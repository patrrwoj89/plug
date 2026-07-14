package com.polishmediahub.app.data.remote.anilist

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST

interface AniListApi {

    @POST("")
    suspend fun query(@Body body: AniListGraphQlRequest): AniListGraphQlResponse

    companion object {
        const val BASE_URL = "https://graphql.anilist.co/"
    }
}

@Serializable
data class AniListGraphQlRequest(
    val query: String,
    val variables: Map<String, String>? = null
)

@Serializable
data class AniListGraphQlResponse(
    val data: AniListData? = null,
    val errors: List<AniListError>? = null
)

@Serializable
data class AniListData(
    val Page: AniListPage? = null,
    val Media: AniListMedia? = null
)

@Serializable
data class AniListPage(
    val media: List<AniListMedia>? = null
)

@Serializable
data class AniListMedia(
    val id: Int,
    @SerialName("idMal") val idMal: Int? = null,
    val title: AniListTitle? = null,
    val description: String? = null,
    val coverImage: AniListCoverImage? = null,
    val bannerImage: String? = null,
    val episodes: Int? = null,
    val format: String? = null,
    val seasonYear: Int? = null,
    val averageScore: Int? = null
)

@Serializable
data class AniListTitle(
    val romaji: String? = null,
    val english: String? = null,
    val native: String? = null
)

@Serializable
data class AniListCoverImage(
    val large: String? = null,
    val extraLarge: String? = null
)

@Serializable
data class AniListError(
    val message: String
)
