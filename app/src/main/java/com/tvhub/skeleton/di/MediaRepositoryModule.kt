package com.tvhub.skeleton.di

import com.tvhub.skeleton.data.CompositeMediaRepository
import com.tvhub.skeleton.data.MediaRepository
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
