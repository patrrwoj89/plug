package com.tvhub.skeleton.model

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
    val genres: List<String> = emptyList(),
    val type: Type = Type.MOVIE
) {
    enum class Type { MOVIE, SERIES, EPISODE, CHANNEL }
}

@Immutable
data class Category(
    val id: String,
    val name: String,
    val items: List<MediaItem>
)
