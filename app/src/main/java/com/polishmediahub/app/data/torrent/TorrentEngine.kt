package com.polishmediahub.app.data.torrent

import android.util.Log
import com.frostwire.jlibtorrent.AlertListener
import com.frostwire.jlibtorrent.SessionManager
import com.frostwire.jlibtorrent.TorrentHandle
import com.frostwire.jlibtorrent.TorrentInfo
import com.frostwire.jlibtorrent.alerts.Alert
import com.frostwire.jlibtorrent.alerts.AlertType
import com.frostwire.jlibtorrent.alerts.TorrentFinishedAlert
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentEngine @Inject constructor() {

    private val session = SessionManager(false)
    private val listeners = ConcurrentHashMap<String, (TorrentStatus) -> Unit>()
    private var saveDir: File? = null

    private val alertListener = object : AlertListener {
        override fun types(): IntArray? = null
        override fun alert(alert: Alert<*>) {
            val handle: TorrentHandle = try {
                when (alert.type()) {
                    AlertType.ADD_TORRENT -> (alert as com.frostwire.jlibtorrent.alerts.AddTorrentAlert).handle()
                    AlertType.TORRENT_FINISHED -> (alert as TorrentFinishedAlert).handle()
                    else -> return
                }
            } catch (_: Exception) {
                return
            }
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
            listeners[hash]?.invoke(
                TorrentStatus(
                    infoHash = hash,
                    name = status.name(),
                    progress = status.progress(),
                    downloadRateBytesPerSecond = status.downloadPayloadRate(),
                    uploadRateBytesPerSecond = status.uploadPayloadRate(),
                    state = state,
                    numPeers = status.numPeers(),
                    numSeeds = status.numSeeds()
                )
            )
        }
    }

    fun start(savePath: File) {
        if (session.isRunning) return
        saveDir = savePath
        session.addListener(alertListener)
        session.start()
    }

    fun stop() {
        session.removeListener(alertListener)
        session.stop()
    }

    fun addTorrent(torrentBytes: ByteArray, savePath: File? = saveDir): String {
        val dir = savePath ?: File(System.getProperty("java.io.tmpdir") ?: "/tmp")
        start(dir)
        val info = TorrentInfo(torrentBytes)
        session.download(info, dir)
        val hash = info.infoHashV1().toString()
        return hash
    }

    fun addMagnet(magnetUri: String, savePath: File? = saveDir): String {
        val dir = savePath ?: File(System.getProperty("java.io.tmpdir") ?: "/tmp")
        start(dir)
        val bytes = session.fetchMagnet(magnetUri, 30, dir)
        return if (bytes != null) {
            addTorrent(bytes, dir)
        } else {
            ""
        }
    }

    fun setListener(infoHash: String, listener: (TorrentStatus) -> Unit) {
        listeners[infoHash] = listener
    }

    fun removeListener(infoHash: String) {
        listeners.remove(infoHash)
    }
}

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
