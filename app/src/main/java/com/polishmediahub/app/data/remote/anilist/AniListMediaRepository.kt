package com.polishmediahub.app.data.remote.anilist

import com.polishmediahub.app.data.ApiConfigRepository
import com.polishmediahub.app.data.MediaRepository
import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
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
                        bannerImage episodes format seasonYear averageScore genres
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

    override suspend fun search(query: String): List<MediaItem> = search(query = query)

    suspend fun search(
        query: String,
        page: Int = 1,
        perPage: Int = 20,
        sort: String = "TRENDING_DESC",
        format: String? = null
    ): List<MediaItem> = try {
        val formatFilter = format?.let { ", format: $it" }.orEmpty()
        val response = api.query(
            AniListGraphQlRequest(
                query = """
                    query(${'$'}search: String) { Page(page: $page, perPage: $perPage) { media(search: ${'$'}search, type: ANIME$formatFilter, sort: $sort) {
                        id title { english romaji native } description coverImage { large extraLarge }
                        bannerImage episodes format seasonYear averageScore genres
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
                        bannerImage episodes format seasonYear averageScore genres
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
        genres = genres,
        type = MediaItem.Type.SERIES
    )
}
