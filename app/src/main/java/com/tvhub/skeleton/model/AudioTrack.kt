package com.tvhub.skeleton.model

import androidx.compose.runtime.Immutable

@Immutable
data class AudioTrack(
    val id: String,
    val title: String,
    val artist: String = "",
    val album: String = "",
    val coverUrl: String? = null,
    val streamUrl: String? = null,
    val durationMs: Long = 0L,
    val isLocal: Boolean = false,
    val sourceId: String = ""
)
