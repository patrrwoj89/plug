package com.polishmediahub.app.data.torrent

import com.polishmediahub.app.data.MediaRepository
import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TorrentMediaSource @Inject constructor(
    private val torrentEngine: TorrentEngine
) : MediaRepository {

    private var saveDir: File? = null
    private val knownItems = mutableMapOf<String, MediaItem>()

    fun configure(saveDir: File) {
        this.saveDir = saveDir
        torrentEngine.start(saveDir)
    }

    fun addMagnet(magnetUri: String): String {
        val dir = saveDir ?: File(System.getProperty("java.io.tmpdir") ?: "/tmp")
        return torrentEngine.addMagnet(magnetUri, dir)
    }

    fun addTorrent(torrentBytes: ByteArray): String {
        val dir = saveDir ?: File(System.getProperty("java.io.tmpdir") ?: "/tmp")
        return torrentEngine.addTorrent(torrentBytes, dir)
    }

    override suspend fun featured(): List<MediaItem> = knownItems.values.toList()

    override suspend fun categories(): List<Category> = emptyList()

    override suspend fun search(query: String): List<MediaItem> =
        knownItems.values.filter { it.title.contains(query, ignoreCase = true) }

    override suspend fun byId(id: String): MediaItem? = knownItems[id]

    fun observeStatus(infoHash: String, onUpdate: (TorrentStatus) -> Unit) {
        torrentEngine.setListener(infoHash) { status ->
            val item = MediaItem(
                id = status.infoHash,
                title = status.name.ifBlank { status.infoHash },
                type = MediaItem.Type.MOVIE
            )
            knownItems[status.infoHash] = item
            onUpdate(status)
        }
    }
}
