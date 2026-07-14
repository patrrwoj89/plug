package com.tvhub.skeleton.di

import com.tvhub.skeleton.data.remote.debrid.DebridService
import com.tvhub.skeleton.data.remote.debrid.RealDebridService
import com.tvhub.skeleton.data.remote.debrid.TorBoxService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class DebridModule {

    @Binds
    @IntoSet
    abstract fun bindRealDebridService(service: RealDebridService): DebridService

    @Binds
    @IntoSet
    abstract fun bindTorBoxService(service: TorBoxService): DebridService
}
