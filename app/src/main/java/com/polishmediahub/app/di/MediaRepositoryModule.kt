package com.polishmediahub.app.di

import com.polishmediahub.app.data.CompositeMediaRepository
import com.polishmediahub.app.data.MediaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMediaRepository(
        repository: CompositeMediaRepository
    ): MediaRepository
}
