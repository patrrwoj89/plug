package com.polishmediahub.app.model

import androidx.compose.runtime.Immutable

@Immutable
data class MediaItem(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val description: String = "",
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val year: String = "",
    val duration: String = "",
    val rating: String = "",
    val bitrate: Long? = null,
    val videoUrl: String? = null,
    val genres: List<String> = emptyList(),
    val season: Int? = null,
    val episode: Int? = null,
    val tvgId: String? = null,
    val channelNumber: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val subtitleUrl: String? = null,
    val subtitleLanguage: String? = "pl",
    val drmLicenseUrl: String? = null,
    val drmScheme: String? = null,
    val drmHeaders: Map<String, String> = emptyMap(),
    val isLive: Boolean = false,
    val type: Type = Type.MOVIE,
    val tmdbId: Int? = null,
    val imdbId: String? = null,
    val traktId: Int? = null,
    val malId: Int? = null,
    val aniListId: Int? = null,
    val ageRating: String? = null,
    val isAdult: Boolean = false
) {
    enum class Type { MOVIE, SERIES, EPISODE, CHANNEL, AUDIO }
}

@Immutable
data class Category(
    val id: String,
    val name: String,
    val items: List<MediaItem>
)
