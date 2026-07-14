package com.polishmediahub.app.di

import com.polishmediahub.app.data.remote.debrid.DebridService
import com.polishmediahub.app.data.remote.debrid.RealDebridService
import com.polishmediahub.app.data.remote.debrid.TorBoxService
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
