package com.tvhub.skeleton.di

import com.tvhub.skeleton.data.MediaRepository
import com.tvhub.skeleton.data.MockMediaRepository
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
        repository: MockMediaRepository
    ): MediaRepository
}
