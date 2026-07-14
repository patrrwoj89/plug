package com.tvhub.skeleton.data.remote.anilist

import com.tvhub.skeleton.data.ApiConfigRepository
import com.tvhub.skeleton.data.MediaRepository
import com.tvhub.skeleton.model.Category
import com.tvhub.skeleton.model.MediaItem
import javax.inject.Inject

class AniListMediaRepository @Inject constructor(
    private val api: AniListApi,
    private val apiConfigRepository: ApiConfigRepository
) : MediaRepository {

    override suspend fun featured(): List<MediaItem> = try {
        val response = api.query(
            AniListGraphQlRequest(
                query = """
                    query { Page(page: 1, perPage: 10) { media(type: ANIME, sort: TRENDING_DESC) {
                        id title { english romaji native } description coverImage { large extraLarge }
                        bannerImage episodes format seasonYear averageScore
                    } } }
                """.trimIndent()
            )
        )
        response.data?.Page?.media?.map { it.toMediaItem() } ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    override suspend fun categories(): List<Category> = try {
        listOf(
            Category(id = "anilist_popular", name = "Popular Anime", items = featured())
        )
    } catch (_: Exception) {
        emptyList()
    }

    override suspend fun search(query: String): List<MediaItem> = try {
        val response = api.query(
            AniListGraphQlRequest(
                query = """
                    query(${'$'}search: String) { Page(page: 1, perPage: 20) { media(search: ${'$'}search, type: ANIME) {
                        id title { english romaji native } description coverImage { large extraLarge }
                        bannerImage episodes format seasonYear averageScore
                    } } }
                """.trimIndent(),
                variables = mapOf("search" to query)
            )
        )
        response.data?.Page?.media?.map { it.toMediaItem() } ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    override suspend fun byId(id: String): MediaItem? = try {
        val numericId = id.removePrefix("anilist:").toIntOrNull() ?: return null
        val response = api.query(
            AniListGraphQlRequest(
                query = """
                    query { Media(id: $numericId) {
                        id title { english romaji native } description coverImage { large extraLarge }
                        bannerImage episodes format seasonYear averageScore
                    } }
                """.trimIndent()
            )
        )
        response.data?.Media?.toMediaItem()
    } catch (_: Exception) {
        null
    }

    private fun AniListMedia.toMediaItem() = MediaItem(
        id = "anilist:$id",
        title = title?.english ?: title?.romaji ?: title?.native ?: "Anime $id",
        subtitle = "${format.orEmpty()} • ${episodes ?: "?"} episodes • ${seasonYear ?: ""}",
        description = description?.replace(Regex("<.*?>"), "").orEmpty(),
        posterUrl = coverImage?.large ?: coverImage?.extraLarge,
        backdropUrl = bannerImage,
        year = seasonYear?.toString().orEmpty(),
        duration = "",
        rating = averageScore?.let { "${it / 10.0}" } ?: "",
        genres = emptyList(),
        type = MediaItem.Type.SERIES
    )
}
