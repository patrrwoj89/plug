package com.polishmediahub.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.polishmediahub.app.data.audio.AudioRepository
import com.polishmediahub.app.model.AudioTrack
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AudioMiniPlayerViewModel @Inject constructor(
    private val audioRepository: AudioRepository
) : ViewModel() {

    val currentTrack = audioRepository.currentTrack
    val isPlaying = audioRepository.isPlaying

    fun toggle() {
        audioRepository.setPlaying(!audioRepository.isPlaying.value)
    }

    fun stop() {
        audioRepository.stop()
    }

    fun resume(track: AudioTrack) {
        audioRepository.setCurrentTrack(track, true)
    }
}
