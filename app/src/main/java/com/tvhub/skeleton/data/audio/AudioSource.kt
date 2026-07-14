package com.tvhub.skeleton.data.audio

import com.tvhub.skeleton.model.AudioTrack

interface AudioSource {
    val id: String
    val name: String
    suspend fun isAvailable(): Boolean
    suspend fun browse(): List<AudioTrack>
    suspend fun search(query: String): List<AudioTrack>
    suspend fun resolve(track: AudioTrack): String?
}
