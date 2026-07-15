package com.polishmediahub.app.data

import android.util.Log
import com.polishmediahub.app.data.remote.anilist.AniListMediaRepository
import com.polishmediahub.app.data.remote.iptv.IptvRepository
import com.polishmediahub.app.data.remote.stremio.StremioRepository
import com.polishmediahub.app.data.remote.tmdb.TmdbMediaRepository
import com.polishmediahub.app.data.remote.trakt.TraktMediaRepository
import com.polishmediahub.app.data.torrent.TorrentMediaSource
import com.polishmediahub.app.data.source.FederatedMediaRepository
import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import javax.inject.Inject

class CompositeMediaRepository @Inject constructor(
    private val mockMediaRepository: MockMediaRepository,
    private val tmdbMediaRepository: TmdbMediaRepository,
    private val aniListMediaRepository: AniListMediaRepository,
    private val traktMediaRepository: TraktMediaRepository,
    private val iptvRepository: IptvRepository,
    private val stremioRepository: StremioRepository,
    private val torrentMediaSource: TorrentMediaSource,
    private val federatedMediaRepository: FederatedMediaRepository
) : MediaRepository {

    private val repositories: List<MediaRepository> = listOf(
        mockMediaRepository,
        tmdbMediaRepository,
        aniListMediaRepository,
        traktMediaRepository,
        iptvRepository,
        stremioRepository,
        torrentMediaSource,
        federatedMediaRepository
    )

    override suspend fun featured(): List<MediaItem> =
        repositories.flatMap { repo -> repo.safeCall { featured() } ?: emptyList() }

    override suspend fun categories(): List<Category> =
        repositories.flatMap { repo -> repo.safeCall { categories() } ?: emptyList() }

    override suspend fun search(query: String): List<MediaItem> =
        repositories.flatMap { repo -> repo.safeCall { search(query) } ?: emptyList() }

    override suspend fun byId(id: String): MediaItem? =
        repositories.firstNotNullOfOrNull { repo -> repo.safeCall { byId(id) } }

    override suspend fun resolve(mediaItem: MediaItem): String? = when {
        mediaItem.id.startsWith("magnet:") || mediaItem.id.startsWith("torrent:") ->
            torrentMediaSource.safeCall { resolve(mediaItem) }
        else ->
            repositories.firstNotNullOfOrNull { repo -> repo.safeCall { resolve(mediaItem) } }
    } ?: mediaItem.videoUrl

    private suspend inline fun <T> MediaRepository.safeCall(block: suspend MediaRepository.() -> T): T? {
        return try {
            block()
        } catch (e: Exception) {
            Log.w("CompositeRepo", "${this::class.java.simpleName} failed: ${e.message}")
            null
        }
    }
}
