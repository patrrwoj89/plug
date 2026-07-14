package com.tvhub.skeleton.di

import com.tvhub.skeleton.data.audio.AudioSource
import com.tvhub.skeleton.data.audio.LocalAudioSource
import com.tvhub.skeleton.data.audio.RadioSource
import com.tvhub.skeleton.data.audio.SubsonicSource
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
}
