package com.polishmediahub.app.data.audio.deezer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeezerTrack(
    val id: Long,
    val title: String,
    val link: String = "",
    val preview: String = "",
    val duration: Int = 0,
    val artist: DeezerArtist = DeezerArtist(),
    val album: DeezerAlbumRef = DeezerAlbumRef()
)

@Serializable
data class DeezerArtist(
    val id: Long = 0,
    val name: String = "",
    val picture: String? = null
)

@Serializable
data class DeezerAlbumRef(
    val id: Long = 0,
    val title: String = "",
    @SerialName("cover") val cover: String? = null
)

@Serializable
data class DeezerAlbumDetail(
    val id: Long,
    val title: String,
    val cover: String? = null,
    val tracks: DeezerTrackList = DeezerTrackList()
)

@Serializable
data class DeezerTrackList(
    @SerialName("data") val data: List<DeezerTrack> = emptyList()
)

@Serializable
data class DeezerChartResponse(
    @SerialName("data") val data: List<DeezerTrack> = emptyList()
)

@Serializable
data class DeezerSearchResponse(
    @SerialName("data") val data: List<DeezerTrack> = emptyList()
)
