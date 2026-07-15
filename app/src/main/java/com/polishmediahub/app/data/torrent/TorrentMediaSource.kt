package com.polishmediahub.app.data.torrent

import android.content.Context
import com.polishmediahub.app.data.MediaRepository
import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentMediaSource @Inject constructor(
    private val torrentEngine: TorrentEngine,
    private val httpServer: TorrentHttpServer,
    @param:ApplicationContext private val context: Context
) : MediaRepository {

    private val knownItems = mutableMapOf<String, MediaItem>()
    private val saveDir: File by lazy {
        context.getExternalFilesDir(null)?.resolve("torrents")?.also { it.mkdirs() }
            ?: File(context.filesDir, "torrents").also { it.mkdirs() }
    }

    val statusFlow: StateFlow<Map<String, TorrentStatus>> = torrentEngine.statusFlow
    val bufferingProgress: StateFlow<Map<String, Int>> = torrentEngine.bufferingProgress

    fun configure() {
        torrentEngine.start()
        httpServer.start()
    }

    fun addMagnet(magnetUri: String): String {
        configure()
        val infoHash = torrentEngine.addMagnet(magnetUri, 60) ?: ""
        if (infoHash.isNotBlank()) {
            knownItems[infoHash] = MediaItem(
                id = "magnet:$infoHash",
                title = "Torrent $infoHash",
                type = MediaItem.Type.MOVIE,
                videoUrl = magnetUri
            )
        }
        return infoHash
    }

    fun addTorrent(torrentBytes: ByteArray): String {
        configure()
        val infoHash = torrentEngine.addTorrent(torrentBytes)
        knownItems[infoHash] = MediaItem(
            id = "torrent:$infoHash",
            title = "Torrent $infoHash",
            type = MediaItem.Type.MOVIE,
            videoUrl = ""
        )
        return infoHash
    }

    fun createMediaItem(infoHash: String, name: String, videoUrl: String): MediaItem {
        val item = MediaItem(
            id = if (videoUrl.startsWith("magnet:")) "magnet:$infoHash" else "torrent:$infoHash",
            title = name.ifBlank { "Torrent $infoHash" },
            type = MediaItem.Type.MOVIE,
            videoUrl = videoUrl
        )
        knownItems[item.id] = item
        return item
    }

    override suspend fun featured(): List<MediaItem> = knownItems.values.toList()

    override suspend fun categories(): List<Category> = emptyList()

    override suspend fun search(query: String): List<MediaItem> =
        knownItems.values.filter { it.title.contains(query, ignoreCase = true) }

    override suspend fun byId(id: String): MediaItem? = knownItems[id]

    override suspend fun resolve(mediaItem: MediaItem): String? {
        val id = mediaItem.id
        val infoHash = when {
            id.startsWith("magnet:") -> id.removePrefix("magnet:")
            id.startsWith("torrent:") -> id.removePrefix("torrent:")
            else -> return mediaItem.videoUrl
        }
        if (infoHash.isBlank()) return null
        configure()

        // Ensure magnet is added if not already
        val sourceUrl = mediaItem.videoUrl
        if (!sourceUrl.isNullOrBlank() && sourceUrl.startsWith("magnet:") && torrentEngine.findHandle(infoHash) == null) {
            torrentEngine.addMagnet(sourceUrl, 60)
        }

        val fileInfo = torrentEngine.findFileInfo(infoHash)
        return if (fileInfo != null) {
            // Prioritize first pieces for start-up buffering
            torrentEngine.setPiecePriority(infoHash, 0, TorrentEngine.bufferPiecesCount * fileInfo.pieceLength)
            httpServer.resolveUrl(infoHash, fileInfo.index)
        } else null
    }

    fun observeStatus(infoHash: String, onUpdate: (TorrentStatus) -> Unit) {
        // Kept for compatibility; flow-based observation preferred
    }
}
