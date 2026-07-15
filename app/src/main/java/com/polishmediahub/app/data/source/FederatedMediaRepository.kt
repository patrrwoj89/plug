package com.polishmediahub.app.data.source

import android.util.Log
import com.polishmediahub.app.data.ApiConfigRepository
import com.polishmediahub.app.data.MediaRepository
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FederatedMediaRepository @Inject constructor(
    private val registry: SourceRegistry,
    private val kodiMediaSource: KodiMediaSource,
    private val webMediaSource: WebMediaSource,
    private val cloudstreamSource: CloudstreamSource,
    private val apiConfigRepository: ApiConfigRepository
) : MediaRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var configured = false

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

            mutex.withLock { configured = true }
        } catch (e: Exception) {
            android.util.Log.w("FederatedMediaRepository", "applyConfigs failed: ${e.message}")
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
        return registry.featuredAll()
    }

    override suspend fun categories(): List<Category> {
        ensureConfigured()
        return registry.categoriesAll()
    }

    override suspend fun search(query: String): List<MediaItem> {
        ensureConfigured()
        return registry.searchAll(query).values.flatten()
    }

    override suspend fun byId(id: String): MediaItem? {
        ensureConfigured()
        for (source in registry.all.filter { it.isAvailable() }) {
            try {
                source.byId(id)?.let { return it }
            } catch (e: Exception) {
                android.util.Log.w("FederatedMediaRepository", "byId failed for ${source.id}: ${e.message}")
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
            android.util.Log.w("FederatedMediaRepository", "resolve failed for ${source.id}: ${e.message}")
            null
        }
    }

    override suspend fun resolveItem(mediaItem: MediaItem): MediaItem {
        ensureConfigured()
        val prefix = mediaItem.id.substringBefore(":", missingDelimiterValue = "")
        val source = registry.source(prefix) ?: registry.all.find { mediaItem.id.startsWith("${it.id}:") }
            ?: return mediaItem
        return try {
            source.resolveItem(mediaItem)
        } catch (e: Exception) {
            android.util.Log.w("FederatedMediaRepository", "resolveItem failed for ${source.id}: ${e.message}")
            mediaItem
        }
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
            Log.w("FederatedMediaRepository", "reportProgress failed for ${source.id}: ${e.message}")
        }
    }
}
