package com.polishmediahub.app.data.audio

import com.polishmediahub.app.data.ApiConfigRepository
import com.polishmediahub.app.model.AudioTrack
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SubsonicSource @Inject constructor(
    private val apiConfigRepository: ApiConfigRepository
) : AudioSource {

    override val id: String = "subsonic"
    override val name: String = "Subsonic/Airsonic"

    private suspend fun url(): String = apiConfigRepository.subsonicUrl.first()
    private suspend fun user(): String = apiConfigRepository.subsonicUser.first()
    private suspend fun password(): String = apiConfigRepository.subsonicPassword.first()

    override suspend fun isAvailable(): Boolean = url().isNotBlank() && user().isNotBlank() && password().isNotBlank()

    override suspend fun browse(): List<AudioTrack> {
        return emptyList()
    }

    override suspend fun search(query: String): List<AudioTrack> = emptyList()

    override suspend fun resolve(track: AudioTrack): String? = track.streamUrl
}
