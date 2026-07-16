package com.polishmediahub.app.data

import android.util.Log
import com.polishmediahub.app.BuildConfig
import com.polishmediahub.app.data.ContentFilter
import com.polishmediahub.app.data.ProfileRepository
import com.polishmediahub.app.data.remote.anime.AnimeRepository
import com.polishmediahub.app.data.remote.debrid.StreamRules
import com.polishmediahub.app.data.remote.debrid.StreamRulesEngine
import com.polishmediahub.app.data.remote.iptv.IptvRepository
import com.polishmediahub.app.data.remote.stremio.StremioRepository
import com.polishmediahub.app.data.remote.tmdb.TmdbMediaRepository
import com.polishmediahub.app.data.remote.trakt.TraktMediaRepository
import com.polishmediahub.app.data.torrent.TorrentMediaSource
import com.polishmediahub.app.data.source.FederatedMediaRepository
import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import com.polishmediahub.app.model.PlaybackState
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import javax.inject.Inject

class CompositeMediaRepository @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository,
    private val mockMediaRepository: MockMediaRepository,
    private val tmdbMediaRepository: TmdbMediaRepository,
    private val animeRepository: AnimeRepository,
    private val traktMediaRepository: TraktMediaRepository,
    private val iptvRepository: IptvRepository,
    private val stremioRepository: StremioRepository,
    private val torrentMediaSource: TorrentMediaSource,
    private val federatedMediaRepository: FederatedMediaRepository
) : MediaRepository {

    private val repositories: List<MediaRepository> = buildList {
        if (BuildConfig.DEBUG) add(mockMediaRepository)
        add(tmdbMediaRepository)
        add(animeRepository)
        add(traktMediaRepository)
        add(iptvRepository)
        add(stremioRepository)
        add(torrentMediaSource)
        add(federatedMediaRepository)
    }

    private suspend fun profile() = profileRepository.currentProfile.first()

    override suspend fun featured(): List<MediaItem> =
        ContentFilter.filter(repositories.flatMap { repo -> repo.safeCall { featured() } ?: emptyList() }, profile())

    override suspend fun categories(): List<Category> =
        ContentFilter.filterCategories(repositories.flatMap { repo -> repo.safeCall { categories() } ?: emptyList() }, profile())

    override suspend fun search(query: String): List<MediaItem> {
        val results = ContentFilter.filter(repositories.flatMap { repo -> repo.safeCall { search(query) } ?: emptyList() }, profile())
        return applyStreamRules(results)
    }

    override suspend fun byId(id: String): MediaItem? =
        applyStreamRules(
            repositories.firstNotNullOfOrNull { repo -> repo.safeCall { byId(id) } }
                ?.takeIf { ContentFilter.isAllowed(it, profile()) }?.let { listOf(it) } ?: emptyList()
        ).firstOrNull()

    override suspend fun resolve(mediaItem: MediaItem): String? = when {
        mediaItem.id.startsWith("magnet:") || mediaItem.id.startsWith("torrent:") ->
            torrentMediaSource.safeCall { resolve(mediaItem) }
        else ->
            repositories.firstNotNullOfOrNull { repo -> repo.safeCall { resolve(mediaItem) } }
    } ?: mediaItem.videoUrl

    override suspend fun resolveItem(mediaItem: MediaItem): MediaItem {
        if (mediaItem.id.startsWith("magnet:") || mediaItem.id.startsWith("torrent:")) {
            val url = torrentMediaSource.safeCall { resolve(mediaItem) }
            return if (!url.isNullOrBlank() && url != mediaItem.videoUrl) mediaItem.copy(videoUrl = url) else mediaItem
        }
        for (repo in repositories) {
            try {
                val resolved = repo.resolveItem(mediaItem)
                if (resolved != mediaItem && ContentFilter.isAllowed(resolved, profile())) return resolved
            } catch (e: Exception) {
                Log.w("CompositeRepo", "resolveItem failed for ${repo::class.java.simpleName}: ${e.message}")
            }
        }
        return mediaItem
    }

    override suspend fun reportProgress(
        mediaItem: MediaItem,
        positionMs: Long,
        durationMs: Long,
        state: PlaybackState
    ) {
        for (repo in repositories) {
            try {
                repo.reportProgress(mediaItem, positionMs, durationMs, state)
            } catch (e: Exception) {
                Log.w("CompositeRepo", "reportProgress failed for ${repo::class.java.simpleName}: ${e.message}")
            }
        }
    }

    private suspend fun applyStreamRules(items: List<MediaItem>): List<MediaItem> {
        return try {
            val json = settingsRepository.streamRules.first()
            val rules = if (json.isNotBlank()) {
                Json.decodeFromString(StreamRules.serializer(), json)
            } else {
                StreamRules()
            }
            StreamRulesEngine.apply(items, rules)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w("CompositeRepo", "applyStreamRules failed: ${e.message}", e)
            items
        }
    }

    private suspend inline fun <T> MediaRepository.safeCall(block: suspend MediaRepository.() -> T): T? {
        return try {
            block()
        } catch (e: Exception) {
            Log.w("CompositeRepo", "${this::class.java.simpleName} failed: ${e.message}")
            null
        }
    }
}
