package com.polishmediahub.app.di

import com.polishmediahub.app.data.audio.AudioSource
import com.polishmediahub.app.data.audio.LocalAudioSource
import com.polishmediahub.app.data.audio.PodcastSource
import com.polishmediahub.app.data.audio.RadioSource
import com.polishmediahub.app.data.audio.SubsonicSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class AudioSourceModule {

    @Binds
    @IntoSet
    abstract fun bindLocalAudioSource(source: LocalAudioSource): AudioSource

    @Binds
    @IntoSet
    abstract fun bindRadioSource(source: RadioSource): AudioSource

    @Binds
    @IntoSet
    abstract fun bindSubsonicSource(source: SubsonicSource): AudioSource

    @Binds
    @IntoSet
    abstract fun bindPodcastSource(source: PodcastSource): AudioSource
}
