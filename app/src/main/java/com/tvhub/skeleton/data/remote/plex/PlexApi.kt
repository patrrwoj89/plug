package com.tvhub.skeleton.data.remote.plex

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PlexApi {

    @GET("library/sections")
    suspend fun getLibrarySections(@Query("X-Plex-Token") token: String): PlexMediaContainer

    @GET("library/sections/{sectionId}/all")
    suspend fun getSectionItems(
        @Path("sectionId") sectionId: String,
        @Query("X-Plex-Token") token: String
    ): PlexMediaContainer

    @GET("library/metadata/{id}")
    suspend fun getMetadata(
        @Path("id") id: String,
        @Query("X-Plex-Token") token: String
    ): PlexMediaContainer

    companion object {
        const val DEFAULT_BASE_URL = "https://plex.tv/"
    }
}

@Serializable
data class PlexMediaContainer(
    @SerialName("MediaContainer") val mediaContainer: PlexContainer? = null
)

@Serializable
data class PlexContainer(
    @SerialName("title1") val title: String? = null,
    @SerialName("Directory") val directories: List<PlexDirectory>? = null,
    @SerialName("Metadata") val metadata: List<PlexMetadata>? = null
)

@Serializable
data class PlexDirectory(
    @SerialName("key") val key: String,
    @SerialName("title") val title: String,
    @SerialName("type") val type: String
)

@Serializable
data class PlexMetadata(
    @SerialName("ratingKey") val ratingKey: String,
    @SerialName("title") val title: String,
    @SerialName("summary") val summary: String? = null,
    @SerialName("type") val type: String,
    @SerialName("thumb") val thumb: String? = null,
    @SerialName("year") val year: Int? = null,
    @SerialName("Media") val media: List<PlexMedia>? = null
)

@Serializable
data class PlexMedia(
    @SerialName("Part") val parts: List<PlexPart>? = null
)

@Serializable
data class PlexPart(
    @SerialName("key") val key: String
)
