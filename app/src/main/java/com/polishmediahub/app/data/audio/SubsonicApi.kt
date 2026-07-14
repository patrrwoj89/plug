package com.polishmediahub.app.data.audio

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface SubsonicApi {

    @GET("rest/ping.view")
    suspend fun ping(
        @Query("u") user: String,
        @Query("p") password: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "PolishMediaHub",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    @GET("rest/getArtists.view")
    suspend fun getArtists(
        @Query("u") user: String,
        @Query("p") password: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "PolishMediaHub",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    @GET("rest/getMusicDirectory.view")
    suspend fun getMusicDirectory(
        @Query("id") id: String,
        @Query("u") user: String,
        @Query("p") password: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "PolishMediaHub",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    @GET("rest/getAlbum.view")
    suspend fun getAlbum(
        @Query("id") id: String,
        @Query("u") user: String,
        @Query("p") password: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "PolishMediaHub",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    companion object {
        const val DEFAULT_BASE_URL = "https://demo.subsonic.org/"
    }
}

@Serializable
data class SubsonicResponse(
    @SerialName("subsonic-response") val subsonicResponse: SubsonicInnerResponse? = null
)

@Serializable
data class SubsonicInnerResponse(
    val status: String = "",
    val artists: SubsonicArtists? = null,
    val directory: SubsonicDirectory? = null,
    val album: SubsonicAlbum? = null
)

@Serializable
data class SubsonicArtists(
    @SerialName("index") val indexes: List<SubsonicIndex> = emptyList()
)

@Serializable
data class SubsonicIndex(
    val name: String = "",
    val artist: List<SubsonicArtist> = emptyList()
)

@Serializable
data class SubsonicArtist(
    val id: String,
    val name: String,
    @SerialName("coverArt") val coverArt: String? = null
)

@Serializable
data class SubsonicDirectory(
    val id: String,
    val name: String,
    val child: List<SubsonicChild> = emptyList()
)

@Serializable
data class SubsonicAlbum(
    val id: String,
    val name: String,
    val artist: String = "",
    val song: List<SubsonicChild> = emptyList()
)

@Serializable
data class SubsonicChild(
    val id: String,
    val parent: String? = null,
    val title: String,
    val artist: String = "",
    val album: String = "",
    val isDir: Boolean = false,
    val type: String? = null,
    @SerialName("coverArt") val coverArt: String? = null,
    val duration: Int = 0
)
