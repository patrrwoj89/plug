package com.polishmediahub.app.data.remote.stremio

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

interface StremioApi {

    @GET
    suspend fun getManifest(@Url manifestUrl: String): StremioManifest

    @GET
    suspend fun getCatalog(@Url url: String): StremioCatalogResponse

    @GET
    suspend fun getMeta(@Url url: String): StremioMetaResponse

    @GET
    suspend fun getStreams(@Url url: String): StremioStreamsResponse
}

@Serializable
data class StremioManifest(
    val id: String,
    val name: String,
    val version: String,
    val description: String? = null,
    val types: List<String>,
    val catalogs: List<StremioCatalogDefinition>? = null,
    val resources: List<StremioResourceDefinition>? = null,
    val behaviorHints: StremioBehaviorHints? = null
)

@Serializable
data class StremioCatalogDefinition(
    val type: String,
    val id: String,
    val name: String? = null,
    @SerialName("extra") val extraParams: List<StremioExtraParam>? = null
)

@Serializable
data class StremioExtraParam(
    val name: String,
    val isRequired: Boolean = false
)

@Serializable
data class StremioResourceDefinition(
    val name: String,
    val types: List<String>? = null,
    @SerialName("idPrefixes") val idPrefixes: List<String>? = null
)

@Serializable
data class StremioBehaviorHints(
    @SerialName("configurable") val configurable: Boolean = false,
    @SerialName("configurationRequired") val configurationRequired: Boolean = false
)

@Serializable
data class StremioCatalogResponse(
    val metas: List<StremioMetaPreview> = emptyList()
)

@Serializable
data class StremioMetaPreview(
    val id: String,
    val type: String,
    val name: String,
    @SerialName("poster") val poster: String? = null,
    @SerialName("posterShape") val posterShape: String? = null,
    val genres: List<String>? = null,
    @SerialName("imdbRating") val imdbRating: String? = null,
    val year: String? = null
)

@Serializable
data class StremioMetaResponse(
    val meta: StremioMeta? = null
)

@Serializable
data class StremioMeta(
    val id: String,
    val type: String,
    val name: String,
    val description: String? = null,
    val year: String? = null,
    @SerialName("poster") val poster: String? = null,
    @SerialName("posterShape") val posterShape: String? = null,
    @SerialName("background") val background: String? = null,
    @SerialName("imdbRating") val imdbRating: String? = null,
    val genres: List<String>? = null,
    val videos: List<StremioVideo>? = null,
    val runtime: String? = null,
    val releaseInfo: String? = null
)

@Serializable
data class StremioVideo(
    val id: String,
    val title: String? = null,
    @SerialName("released") val released: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val overview: String? = null,
    @SerialName("thumbnail") val thumbnail: String? = null
)

@Serializable
data class StremioStreamsResponse(
    val streams: List<StremioStream> = emptyList()
)

@Serializable
data class StremioStream(
    val name: String? = null,
    val title: String? = null,
    val url: String? = null,
    @SerialName("infoHash") val infoHash: String? = null,
    @SerialName("ytId") val ytId: String? = null,
    val externalUrl: String? = null,
    @SerialName("androidTvUrl") val androidTvUrl: String? = null,
    @SerialName("behaviorHints") val behaviorHints: StremioStreamHints? = null
)

@Serializable
data class StremioStreamHints(
    @SerialName("notWebReady") val notWebReady: Boolean = false,
    @SerialName("bingeGroup") val bingeGroup: String? = null,
    @SerialName("filename") val filename: String? = null
)
