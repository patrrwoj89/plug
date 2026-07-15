package com.polishmediahub.app.data.audio

import com.polishmediahub.app.model.AudioTrack
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRepository @Inject constructor(
    private val sources: Set<@JvmSuppressWildcards AudioSource>
) {

    private val cache = mutableMapOf<String, AudioTrack>()

    suspend fun browse(): List<AudioTrack> =
        sources.filter { it.isAvailable() }
            .flatMap { it.browse() }
            .also { tracks -> cache.putAll(tracks.associateBy { it.id }) }

    suspend fun search(query: String): List<AudioTrack> =
        sources.filter { it.isAvailable() }
            .flatMap { it.search(query) }
            .also { tracks -> cache.putAll(tracks.associateBy { it.id }) }

    suspend fun byId(id: String): AudioTrack? {
        cache[id]?.let { return it }
        val track = sources.firstNotNullOfOrNull { it.byId(id) }
        track?.let { cache[id] = it }
        return track
    }

    suspend fun resolve(track: AudioTrack): String? {
        val source = sources.find { it.id == track.sourceId } ?: return track.streamUrl
        return source.resolve(track)
    }

    fun cache(track: AudioTrack) {
        cache[track.id] = track
    }
}
