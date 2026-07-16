package com.polishmediahub.app.data.torrent

import android.content.Context
import android.util.Log
import com.polishmediahub.app.BuildConfig
import com.frostwire.jlibtorrent.AlertListener
import com.frostwire.jlibtorrent.Priority
import com.frostwire.jlibtorrent.SessionManager
import com.frostwire.jlibtorrent.TorrentHandle
import com.frostwire.jlibtorrent.TorrentInfo
import com.frostwire.jlibtorrent.alerts.Alert
import com.frostwire.jlibtorrent.alerts.AlertType
import com.frostwire.jlibtorrent.alerts.TorrentFinishedAlert
import com.frostwire.jlibtorrent.alerts.TorrentRemovedAlert
import com.frostwire.jlibtorrent.swig.libtorrent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentEngine @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val session = SessionManager(false)
    private val handles = ConcurrentHashMap<String, TorrentHandle>()
    private val torrentFiles = ConcurrentHashMap<String, TorrentFileInfo>()
    private val saveDir: File by lazy {
        context.getExternalFilesDir(null)?.resolve("torrents")?.also { it.mkdirs() }
            ?: File(context.filesDir, "torrents").also { it.mkdirs() }
    }

    private val _statusFlow = MutableStateFlow<Map<String, TorrentStatus>>(emptyMap())
    val statusFlow: StateFlow<Map<String, TorrentStatus>> = _statusFlow.asStateFlow()

    private val _bufferingProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val bufferingProgress: StateFlow<Map<String, Int>> = _bufferingProgress.asStateFlow()

    private val alertListener = object : AlertListener {
        override fun types(): IntArray? = null
        override fun alert(alert: Alert<*>) {
            try {
                when (alert.type()) {
                    AlertType.ADD_TORRENT -> {
                        val handle = (alert as com.frostwire.jlibtorrent.alerts.AddTorrentAlert).handle()
                        onTorrentAdded(handle)
                    }
                    AlertType.TORRENT_FINISHED -> {
                        val handle = (alert as TorrentFinishedAlert).handle()
                        updateStatus(handle)
                    }
                    AlertType.PIECE_FINISHED,
                    AlertType.STATE_CHANGED,
                    AlertType.TORRENT_CHECKED,
                    AlertType.STATE_UPDATE -> {
                        val handle = tryGetHandle(alert)
                        if (handle != null) {
                            updateStatus(handle)
                            updateBufferingProgress(handle)
                        }
                    }
                    AlertType.TORRENT_REMOVED -> {
                        val handle = (alert as TorrentRemovedAlert).handle()
                        val hash = handle.infoHash().toString()
                        handles.remove(hash)
                        torrentFiles.remove(hash)
                        _statusFlow.value = _statusFlow.value - hash
                        _bufferingProgress.value = _bufferingProgress.value - hash
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.w("TorrentEngine", "alert error: ${e.message}")
            }
        }
    }

    init {
        session.addListener(alertListener)
        session.start()
    }

    fun start() {
        if (!session.isRunning) {
            session.addListener(alertListener)
            session.start()
        }
    }

    fun stop() {
        session.removeListener(alertListener)
        session.stop()
    }

    fun addTorrent(torrentBytes: ByteArray): String {
        start()
        val info = TorrentInfo(torrentBytes)
        DEFAULT_TRACKERS.forEach { info.addTracker(it) }
        session.download(info, saveDir)
        val hash = info.infoHashV1().toString()
        // Try to find handle immediately, fallback to alert
        session.find(info.infoHashV1())?.let { onTorrentAdded(it) }
        return hash
    }

    fun addMagnet(magnetUri: String, timeoutSeconds: Int = 30): String? {
        start()
        val magnetWithTrackers = appendTrackersToMagnet(magnetUri)
        val bytes = session.fetchMagnet(magnetWithTrackers, timeoutSeconds, saveDir) ?: return null
        return addTorrent(bytes)
    }

    private fun appendTrackersToMagnet(magnetUri: String): String {
        if (!magnetUri.startsWith("magnet:", ignoreCase = true)) return magnetUri
        val builder = StringBuilder(magnetUri)
        DEFAULT_TRACKERS.forEach { tracker ->
            builder.append("&tr=").append(URLEncoder.encode(tracker, "UTF-8"))
        }
        return builder.toString()
    }

    fun findHandle(infoHash: String): TorrentHandle? = handles[infoHash]

    fun findFileInfo(infoHash: String): TorrentFileInfo? = torrentFiles[infoHash]

    fun setPiecePriority(infoHash: String, byteStart: Long, byteEnd: Long) {
        val handle = handles[infoHash] ?: return
        val fileInfo = torrentFiles[infoHash] ?: return
        val startPiece = pieceForByte(fileInfo, byteStart)
        val endPiece = pieceForByte(fileInfo, byteEnd)
        for (piece in startPiece..endPiece) {
            if (piece < handle.torrentFile().numPieces()) {
                handle.piecePriority(piece, Priority.SEVEN)
                handle.setPieceDeadline(piece, 0)
            }
        }
    }

    private fun onTorrentAdded(handle: TorrentHandle) {
        val hash = handle.infoHash().toString()
        handles[hash] = handle
        handle.setFlags(libtorrent.getSequential_download())
        configureFilePriorities(handle)
        torrentFiles[hash] = findLargestVideoFile(handle)
        updateStatus(handle)
        updateBufferingProgress(handle)
    }

    private fun configureFilePriorities(handle: TorrentHandle) {
        val info = handle.torrentFile()
        val files = info.files()
        val largest = findLargestVideoFileIndex(files)
        for (i in 0 until files.numFiles()) {
            handle.filePriority(i, if (i == largest) Priority.SEVEN else Priority.IGNORE)
        }
    }

    private fun findLargestVideoFile(handle: TorrentHandle): TorrentFileInfo {
        val files = handle.torrentFile().files()
        val index = findLargestVideoFileIndex(files)
        return TorrentFileInfo(
            index = index,
            path = File(handle.savePath(), files.filePath(index)).absolutePath,
            size = files.fileSize(index),
            offset = files.fileOffset(index),
            pieceLength = files.pieceLength().toLong(),
            name = files.fileName(index)
        )
    }

    private fun findLargestVideoFileIndex(files: com.frostwire.jlibtorrent.FileStorage): Int {
        var bestIndex = 0
        var bestSize = 0L
        for (i in 0 until files.numFiles()) {
            val name = files.fileName(i).lowercase()
            if (name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".webm") ||
                name.endsWith(".avi") || name.endsWith(".mov") || name.endsWith(".m4v")
            ) {
                val size = files.fileSize(i)
                if (size > bestSize) {
                    bestSize = size
                    bestIndex = i
                }
            }
        }
        if (bestSize == 0L) {
            for (i in 0 until files.numFiles()) {
                val size = files.fileSize(i)
                if (size > bestSize) {
                    bestSize = size
                    bestIndex = i
                }
            }
        }
        return bestIndex
    }

    private fun updateStatus(handle: TorrentHandle) {
        val hash = handle.infoHash().toString()
        val status = handle.status()
        val state = when (status.state()) {
            com.frostwire.jlibtorrent.TorrentStatus.State.CHECKING_FILES,
            com.frostwire.jlibtorrent.TorrentStatus.State.CHECKING_RESUME_DATA -> TorrentState.HASHING
            com.frostwire.jlibtorrent.TorrentStatus.State.DOWNLOADING_METADATA -> TorrentState.DOWNLOADING_METADATA
            com.frostwire.jlibtorrent.TorrentStatus.State.DOWNLOADING -> TorrentState.DOWNLOADING
            com.frostwire.jlibtorrent.TorrentStatus.State.FINISHED -> TorrentState.FINISHED
            com.frostwire.jlibtorrent.TorrentStatus.State.SEEDING -> TorrentState.SEEDING
            else -> TorrentState.UNKNOWN
        }
        val info = TorrentStatus(
            infoHash = hash,
            name = status.name().ifBlank { hash },
            progress = status.progress(),
            downloadRateBytesPerSecond = status.downloadPayloadRate(),
            uploadRateBytesPerSecond = status.uploadPayloadRate(),
            state = state,
            numPeers = status.numPeers(),
            numSeeds = status.numSeeds()
        )
        _statusFlow.value = _statusFlow.value + (hash to info)
    }

    private fun updateBufferingProgress(handle: TorrentHandle) {
        val fileInfo = torrentFiles[handle.infoHash().toString()] ?: return
        val piecesNeeded = bufferPiecesCount.coerceAtMost(handle.torrentFile().numPieces())
        var ready = 0
        for (piece in 0 until piecesNeeded) {
            if (handle.havePiece(piece)) ready++
        }
        val percent = if (piecesNeeded > 0) (ready * 100 / piecesNeeded) else 100
        _bufferingProgress.value = _bufferingProgress.value + (handle.infoHash().toString() to percent)
    }

    private fun tryGetHandle(alert: Alert<*>): TorrentHandle? {
        return try {
            when (alert.type()) {
                AlertType.PIECE_FINISHED -> (alert as com.frostwire.jlibtorrent.alerts.PieceFinishedAlert).handle()
                AlertType.STATE_CHANGED -> (alert as com.frostwire.jlibtorrent.alerts.StateChangedAlert).handle()
                AlertType.TORRENT_CHECKED -> (alert as com.frostwire.jlibtorrent.alerts.TorrentCheckedAlert).handle()
                AlertType.STATE_UPDATE -> {
                    val update = alert as com.frostwire.jlibtorrent.alerts.StateUpdateAlert
                    update.status().firstOrNull()?.let { status ->
                        val hash = status.infoHash().toString()
                        handles[hash]
                    }
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        const val bufferPiecesCount = 50

        /**
         * Active Polish and global BitTorrent announce endpoints added to every
         * torrent/magnet as of 2026-07-14. Verified with curl reachability
         * (electro-torrent.pl and devil-torrents.pl redirect to HTTPS;
         * opentrackr/stealth.si/coppersurfer/leechers-paradise are public trackers).
         */
        private val DEFAULT_TRACKERS = listOf(
            "http://electro-torrent.pl",
            "http://devil-torrents.pl",
            "udp://tracker.opentrackr.org:1337/announce",
            "udp://open.stealth.si:80/announce",
            "udp://tracker.coppersurfer.tk:6969/announce",
            "udp://tracker.leechers-paradise.org:6969/announce"
        )

        fun pieceForByte(fileInfo: TorrentFileInfo, bytePos: Long): Int {
            return ((fileInfo.offset + bytePos) / fileInfo.pieceLength).toInt()
        }
    }
}

data class TorrentFileInfo(
    val index: Int,
    val path: String,
    val size: Long,
    val offset: Long,
    val pieceLength: Long,
    val name: String
)

data class TorrentStatus(
    val infoHash: String,
    val name: String,
    val progress: Float,
    val downloadRateBytesPerSecond: Int,
    val uploadRateBytesPerSecond: Int,
    val state: TorrentState,
    val numPeers: Int,
    val numSeeds: Int
)

enum class TorrentState {
    UNKNOWN, HASHING, DOWNLOADING_METADATA, DOWNLOADING, FINISHED, SEEDING
}
