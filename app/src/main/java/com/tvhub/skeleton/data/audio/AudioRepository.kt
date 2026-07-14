package com.tvhub.skeleton.data.audio

import com.tvhub.skeleton.model.AudioTrack
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRepository @Inject constructor(
    private val sources: Set<@JvmSuppressWildcards AudioSource>
) {

    suspend fun browse(): List<AudioTrack> =
        sources.filter { it.isAvailable() }.flatMap { it.browse() }

    suspend fun search(query: String): List<AudioTrack> =
        sources.filter { it.isAvailable() }.flatMap { it.search(query) }

    suspend fun resolve(track: AudioTrack): String? {
        val source = sources.find { it.id == track.sourceId } ?: return track.streamUrl
        return source.resolve(track)
    }
}
