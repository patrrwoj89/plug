package com.polishmediahub.app.data.source

import android.util.Log
import com.polishmediahub.app.BuildConfig
import com.polishmediahub.app.data.ApiConfigRepository
import com.polishmediahub.app.data.ContentFilter
import com.polishmediahub.app.data.MediaRepository
import com.polishmediahub.app.data.ProfileRepository
import com.polishmediahub.app.data.remote.filmweb.FilmwebMediaSource
import com.polishmediahub.app.data.remote.mdblist.MdbListMediaSource
import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import com.polishmediahub.app.model.PlaybackState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FederatedMediaRepository @Inject constructor(
    private val registry: SourceRegistry,
    private val kodiMediaSource: KodiMediaSource,
    private val webMediaSource: WebMediaSource,
    private val cloudstreamSource: CloudstreamSource,
    private val mdbListMediaSource: MdbListMediaSource,
    private val filmwebMediaSource: FilmwebMediaSource,
    private val apiConfigRepository: ApiConfigRepository,
    private val profileRepository: ProfileRepository
) : MediaRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var configured = false

    private suspend fun profile() = profileRepository.currentProfile.first()

    init {
        scope.launch {
            applyConfigs()
        }
    }

    private suspend fun applyConfigs() {
        try {
            val kodiUrl = apiConfigRepository.kodiUrl.first()
            if (kodiUrl.isNotBlank()) kodiMediaSource.configure(kodiUrl)

            val webConfig = apiConfigRepository.webSourceConfig.first()
            if (webConfig.isNotBlank()) webMediaSource.configure(webConfig)

            val cloudstreamRepos = apiConfigRepository.cloudstreamRepoUrls.first()
            if (cloudstreamRepos.isNotBlank()) cloudstreamSource.configure(cloudstreamRepos)

            val mdbListKey = apiConfigRepository.mdbListApiKey.first()
            if (mdbListKey.isNotBlank()) mdbListMediaSource.configure(mdbListKey)

            mutex.withLock { configured = true }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.w("FederatedMediaRepository", "applyConfigs failed: ${e.message}")
        }
    }

    private suspend fun ensureConfigured() {
        mutex.withLock {
            if (!configured) {
                applyConfigs()
            }
        }
    }

    override suspend fun featured(): List<MediaItem> {
        ensureConfigured()
        return ContentFilter.filter(registry.featuredAll(), profile())
    }

    override suspend fun categories(): List<Category> {
        ensureConfigured()
        return ContentFilter.filterCategories(registry.categoriesAll(), profile())
    }

    override suspend fun search(query: String): List<MediaItem> {
        ensureConfigured()
        return ContentFilter.filter(registry.searchAll(query).values.flatten(), profile())
    }

    override suspend fun byId(id: String): MediaItem? {
        ensureConfigured()
        for (source in registry.all.filter { it.isAvailable() }) {
            try {
                source.byId(id)
                    ?.takeIf { ContentFilter.isAllowed(it, profile()) }
                    ?.let { return it }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) android.util.Log.w("FederatedMediaRepository", "byId failed for ${source.id}: ${e.message}")
            }
        }
        return null
    }

    override suspend fun resolve(mediaItem: MediaItem): String? {
        ensureConfigured()
        val prefix = mediaItem.id.substringBefore(":", missingDelimiterValue = "")
        val source = registry.source(prefix) ?: registry.all.find { mediaItem.id.startsWith("${it.id}:") }
        source ?: return null
        return try {
            source.resolve(mediaItem)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.w("FederatedMediaRepository", "resolve failed for ${source.id}: ${e.message}")
            null
        }
    }

    override suspend fun resolveItem(mediaItem: MediaItem): MediaItem {
        ensureConfigured()
        val prefix = mediaItem.id.substringBefore(":", missingDelimiterValue = "")
        val source = registry.source(prefix) ?: registry.all.find { mediaItem.id.startsWith("${it.id}:") }
            ?: return mediaItem
        return try {
            val resolved = source.resolveItem(mediaItem)
            if (ContentFilter.isAllowed(resolved, profile())) resolved else mediaItem
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.w("FederatedMediaRepository", "resolveItem failed for ${source.id}: ${e.message}")
            mediaItem
        }
    }

    /**
     * Fetches Polish metadata from Filmweb.pl when the existing description is empty,
     * too short or does not contain Polish diacritics, and falls back to cached data.
     */
    suspend fun enrichWithFilmweb(item: MediaItem): MediaItem {
        if (!shouldFetchFromFilmweb(item)) return item

        return try {
            withContext(Dispatchers.IO) {
                val polish = filmwebMediaSource.fetchPolishMetadata(item.title, item.year) ?: return@withContext item
                item.copy(
                    description = polish.description.takeIf { it.isNotBlank() } ?: item.description,
                    posterUrl = polish.posterUrl ?: item.posterUrl,
                    backdropUrl = polish.backdropUrl ?: item.backdropUrl,
                    filmwebRating = polish.filmwebRating,
                    filmwebVoteCount = polish.filmwebVoteCount,
                    filmwebUrl = polish.filmwebUrl
                )
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.w("FederatedMediaRepository", "enrichWithFilmweb failed: ${e.message}")
            item
        }
    }

    private fun shouldFetchFromFilmweb(item: MediaItem): Boolean {
        if (!item.filmwebRating.isNullOrBlank()) return false
        if (item.description.isBlank() || item.description.length < 50) return true
        val hasPolishDiacritics = item.description.contains(Regex("[ąćęłńóśźżĄĆĘŁŃÓŚŹŻ]"))
        return !hasPolishDiacritics
    }

    override suspend fun reportProgress(
        mediaItem: MediaItem,
        positionMs: Long,
        durationMs: Long,
        state: PlaybackState
    ) {
        ensureConfigured()
        val prefix = mediaItem.id.substringBefore(":", missingDelimiterValue = "")
        val source = registry.source(prefix) ?: registry.all.find { mediaItem.id.startsWith("${it.id}:") }
            ?: return
        if (!source.isAvailable()) return
        try {
            source.reportProgress(mediaItem, positionMs, durationMs, state)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w("FederatedMediaRepository", "reportProgress failed for ${source.id}: ${e.message}")
        }
    }
}
