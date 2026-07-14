package com.tvhub.skeleton.data.source

import com.tvhub.skeleton.data.ApiConfigRepository
import com.tvhub.skeleton.data.MediaRepository
import com.tvhub.skeleton.model.Category
import com.tvhub.skeleton.model.MediaItem
import kotlinx.coroutines.flow.first
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

    init {
        // Apply saved configurations on creation (synchronously is not possible for suspend,
        // so callers should call configure() explicitly or we rely on first access).
    }

    private suspend fun applyConfigs() {
        val kodiUrl = apiConfigRepository.kodiUrl.first()
        if (kodiUrl.isNotBlank()) kodiMediaSource.configure(kodiUrl)

        val webConfig = apiConfigRepository.webSourceConfig.first()
        if (webConfig.isNotBlank()) webMediaSource.configure(webConfig)

        val cloudstreamRepos = apiConfigRepository.cloudstreamRepoUrls.first()
        if (cloudstreamRepos.isNotBlank()) cloudstreamSource.configure(cloudstreamRepos)
    }

    override suspend fun featured(): List<MediaItem> {
        applyConfigs()
        return registry.featuredAll()
    }

    override suspend fun categories(): List<Category> {
        applyConfigs()
        return registry.categoriesAll()
    }

    override suspend fun search(query: String): List<MediaItem> {
        applyConfigs()
        return registry.searchAll(query).values.flatten()
    }

    override suspend fun byId(id: String): MediaItem? {
        applyConfigs()
        return registry.all.firstNotNullOfOrNull { it.byId(id) }
    }
}
