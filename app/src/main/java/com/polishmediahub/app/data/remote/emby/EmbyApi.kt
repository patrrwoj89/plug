package com.polishmediahub.app.data.remote.emby

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface EmbyApi {

    @GET("Users")
    suspend fun getUsers(@Header("X-Emby-Token") token: String): List<EmbyUser>

    @GET("Items")
    suspend fun getItems(
        @Header("X-Emby-Token") token: String,
        @Query("userId") userId: String,
        @Query("includeItemTypes") includeItemTypes: String = "Movie,Series,Episode",
        @Query("recursive") recursive: Boolean = true
    ): EmbyItemsResponse

    @GET("Items/{id}")
    suspend fun getItem(
        @Header("X-Emby-Token") token: String,
        @Path("id") id: String,
        @Query("userId") userId: String
    ): EmbyItem

    @POST("Sessions/Playing/Progress")
    suspend fun reportProgress(
        @Header("X-Emby-Token") token: String,
        @Body info: PlaybackProgressInfo
    )

    companion object {
        const val DEFAULT_BASE_URL = "https://demo.emby.media/"
    }
}

@Serializable
data class EmbyUser(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String
)

@Serializable
data class EmbyItemsResponse(
    @SerialName("Items") val items: List<EmbyItem> = emptyList()
)

@Serializable
data class EmbyItem(
    @SerialName("Id") val id: String,
    @SerialName("Name") val name: String,
    @SerialName("Overview") val overview: String? = null,
    @SerialName("Type") val type: String,
    @SerialName("PrimaryImageTag") val primaryImageTag: String? = null,
    @SerialName("BackdropImageTags") val backdropImageTags: List<String>? = null,
    @SerialName("ProductionYear") val productionYear: Int? = null,
    @SerialName("Bitrate") val bitrate: Long? = null
)

@Serializable
data class PlaybackProgressInfo(
    @SerialName("ItemId") val itemId: String,
    @SerialName("PositionTicks") val positionTicks: Long,
    @SerialName("IsPaused") val isPaused: Boolean = false,
    @SerialName("IsStopped") val isStopped: Boolean = false
)
