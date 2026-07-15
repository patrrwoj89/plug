package com.polishmediahub.app.data.remote.anime

import android.util.Log
import com.polishmediahub.app.model.Category
import com.polishmediahub.app.model.MediaItem
import com.polishmediahub.app.data.source.MediaSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

private const val BASE_URL = "https://kitsu.io/api/edge"

/**
 * Federated [MediaSource] for Kitsu (https://kitsu.io).
 *
 * Loads anime metadata via JSON:API, supports text search and cross-ID lookup.
 * Mappings included with `include=mappings` are parsed for MyAnimeList and
 * AniList identifiers and stored on every [MediaItem] as `malId` and `aniListId`.
 */
@Singleton
class KitsuMediaSource @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json
) : MediaSource {

    override val id: String = "kitsu"
    override val name: String = "Kitsu"
    override val isConfigurable: Boolean = false

    override suspend fun isAvailable(): Boolean = true

    override suspend fun featured(): List<MediaItem> = withContext(Dispatchers.IO) {
        try {
            val response = getList("/anime?sort=popularityRank&page[limit]=20&include=mappings")
            response?.data?.map { it.toMediaItem(response.included) } ?: emptyList()
        } catch (e: Exception) {
            Log.w("KitsuMediaSource", "featured failed: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun categories(): List<Category> = withContext(Dispatchers.IO) {
        try {
            listOf(
                Category(
                    id = "kitsu_trending",
                    name = "Trending Anime",
                    items = getList("/anime?sort=popularityRank&page[limit]=20&include=mappings")?.data
                        ?.map { it.toMediaItem(emptyList()) } ?: emptyList()
                ),
                Category(
                    id = "kitsu_top_rated",
                    name = "Top Rated Anime",
                    items = getList("/anime?sort=-averageRating&page[limit]=20&include=mappings")?.data
                        ?.map { it.toMediaItem(emptyList()) } ?: emptyList()
                ),
                Category(
                    id = "kitsu_movies",
                    name = "Anime Movies",
                    items = getList("/anime?filter[subtype]=movie&sort=popularityRank&page[limit]=20&include=mappings")?.data
                        ?.map { it.toMediaItem(emptyList()) } ?: emptyList()
                )
            )
        } catch (e: Exception) {
            Log.w("KitsuMediaSource", "categories failed: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun search(query: String): List<MediaItem> = withContext(Dispatchers.IO) {
        try {
            if (query.isBlank()) return@withContext emptyList()
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val response = getList("/anime?filter[text]=$encoded&page[limit]=20&include=mappings")
            response?.data?.map { it.toMediaItem(response.included) } ?: emptyList()
        } catch (e: Exception) {
            Log.w("KitsuMediaSource", "search failed: ${e.message}", e)
            emptyList()
        }
    }

    override suspend fun byId(id: String): MediaItem? = withContext(Dispatchers.IO) {
        try {
            val numericId = id.removePrefix("kitsu:").trim()
            if (numericId.isBlank()) return@withContext null
            val response = getSingle("/anime/$numericId?include=mappings") ?: return@withContext null
            response.data.toMediaItem(response.included)
        } catch (e: Exception) {
            Log.w("KitsuMediaSource", "byId failed for $id: ${e.message}", e)
            null
        }
    }

    override suspend fun resolve(mediaItem: MediaItem): String? = null

    private suspend fun getList(path: String): KitsuAnimeListResponse? = request(path)

    private suspend fun getSingle(path: String): KitsuAnimeResponse? = request(path)

    private suspend inline fun <reified T> request(path: String): T? = try {
        val url = if (path.startsWith("http")) path else "$BASE_URL${path}"
        val request = Request.Builder().url(url).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w("KitsuMediaSource", "HTTP ${response.code} for $path")
                return@use null
            }
            val body = response.body?.string()
            if (body.isNullOrBlank()) return@use null
            try {
                json.decodeFromString<T>(body)
            } catch (e: SerializationException) {
                Log.w("KitsuMediaSource", "parse error for $path: ${e.message}")
                null
            }
        }
    } catch (e: Exception) {
        Log.w("KitsuMediaSource", "request error for $path: ${e.message}", e)
        null
    }

    private fun KitsuAnime.toMediaItem(included: List<KitsuIncluded>): MediaItem {
        val mappingIds = relationships.mappings.data.map { it.id }.toSet()
        val mappings = included.filter { it.id in mappingIds && it.type == "mappings" }
        var malId: Int? = null
        var aniListId: Int? = null
        mappings.forEach { mapping ->
            val site = mapping.attributes?.externalSite?.lowercase() ?: return@forEach
            val externalId = mapping.attributes.externalId?.toIntOrNull()
            when {
                site.contains("myanimelist") -> malId = externalId
                site.contains("anilist") -> aniListId = externalId
            }
        }

        val attrs = attributes
        val title = attrs.titles.en?.takeIf { it.isNotBlank() }
            ?: attrs.canonicalTitle.takeIf { it.isNotBlank() }
            ?: attrs.titles.enJp?.takeIf { it.isNotBlank() }
            ?: attrs.titles.jaJp?.takeIf { it.isNotBlank() }
            ?: "Anime ${attrs.slug ?: id}"

        val subtype = attrs.subtype ?: attrs.showType
        val type = if (subtype.equals("movie", ignoreCase = true)) MediaItem.Type.MOVIE else MediaItem.Type.SERIES
        val isAdult = attrs.nsfw || isAdultAgeRating(attrs.ageRating)

        return MediaItem(
            id = "kitsu:$id",
            title = title,
            subtitle = buildString {
                append(subtype?.replaceFirstChar { it.uppercase() } ?: "Anime")
                if ((attrs.episodeCount ?: 0) > 0) append(" • ${attrs.episodeCount} episodes")
                if (!attrs.startDate.isNullOrBlank()) append(" • ${attrs.startDate.take(4)}")
            },
            description = attrs.synopsis?.replace(Regex("<.*?>"), "")?.takeIf { it.isNotBlank() } ?: "",
            posterUrl = attrs.posterImage?.bestUrl(),
            backdropUrl = attrs.coverImage?.bestUrl() ?: attrs.posterImage?.bestUrl(),
            year = attrs.startDate?.take(4).orEmpty(),
            rating = attrs.averageRating?.takeIf { it.isNotBlank() } ?: "",
            type = type,
            malId = malId,
            aniListId = aniListId,
            ageRating = attrs.ageRating,
            isAdult = isAdult
        )
    }

    private fun KitsuImage.bestUrl(): String? {
        return large?.takeIf { it.startsWith("http") }
            ?: medium?.takeIf { it.startsWith("http") }
            ?: small?.takeIf { it.startsWith("http") }
            ?: original?.takeIf { it.startsWith("http") }
            ?: tiny?.takeIf { it.startsWith("http") }
    }

    private fun isAdultAgeRating(rating: String?): Boolean {
        if (rating.isNullOrBlank()) return false
        val upper = rating.uppercase()
        return upper == "R18" || upper == "R18+" || upper.contains("NC-17") || upper == "AO" || upper == "X"
    }
}
