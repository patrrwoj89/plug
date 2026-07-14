package com.tvhub.skeleton.data

import com.tvhub.skeleton.data.remote.anilist.AniListMediaRepository
import com.tvhub.skeleton.data.remote.iptv.IptvRepository
import com.tvhub.skeleton.data.remote.stremio.StremioRepository
import com.tvhub.skeleton.data.remote.tmdb.TmdbMediaRepository
import com.tvhub.skeleton.data.remote.trakt.TraktMediaRepository
import com.tvhub.skeleton.model.Category
import com.tvhub.skeleton.model.MediaItem
import javax.inject.Inject

class CompositeMediaRepository @Inject constructor(
    private val mockMediaRepository: MockMediaRepository,
    private val tmdbMediaRepository: TmdbMediaRepository,
    private val aniListMediaRepository: AniListMediaRepository,
    private val traktMediaRepository: TraktMediaRepository,
    private val iptvRepository: IptvRepository,
    private val stremioRepository: StremioRepository
) : MediaRepository {

    private val repositories: List<MediaRepository> = listOf(
        mockMediaRepository,
        tmdbMediaRepository,
        aniListMediaRepository,
        traktMediaRepository,
        iptvRepository,
        stremioRepository
    )

    override suspend fun featured(): List<MediaItem> =
        repositories.flatMap { it.featured() }

    override suspend fun categories(): List<Category> =
        repositories.flatMap { it.categories() }

    override suspend fun search(query: String): List<MediaItem> =
        repositories.flatMap { it.search(query) }

    override suspend fun byId(id: String): MediaItem? =
        repositories.firstNotNullOfOrNull { it.byId(id) }
}
