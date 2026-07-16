package com.polishmediahub.app.data.audio

import com.polishmediahub.app.model.AudioTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRepository @Inject constructor(
    private val sources: Set<@JvmSuppressWildcards AudioSource>
) {

    private val cache = mutableMapOf<String, AudioTrack>()

    private val _currentTrack = MutableStateFlow<AudioTrack?>(null)
    val currentTrack: StateFlow<AudioTrack?> = _currentTrack

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

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

    /**
     * Updates the global mini-player state. UI components must use [collectAsStateWithLifecycle]
     * when collecting [currentTrack] and [isPlaying] (Zasada 4).
     */
    fun setCurrentTrack(track: AudioTrack?, playing: Boolean = true) {
        _currentTrack.value = track
        _isPlaying.value = track != null && playing
    }

    fun setPlaying(playing: Boolean) {
        _isPlaying.value = playing && _currentTrack.value != null
    }

    fun stop() {
        _currentTrack.value = null
        _isPlaying.value = false
    }
}
