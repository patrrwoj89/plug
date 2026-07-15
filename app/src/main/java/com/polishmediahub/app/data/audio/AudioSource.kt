package com.polishmediahub.app.data.audio

import com.polishmediahub.app.model.AudioTrack

interface AudioSource {
    val id: String
    val name: String
    suspend fun isAvailable(): Boolean
    suspend fun browse(): List<AudioTrack>
    suspend fun search(query: String): List<AudioTrack>
    suspend fun byId(trackId: String): AudioTrack? = null
    suspend fun resolve(track: AudioTrack): String?
}
