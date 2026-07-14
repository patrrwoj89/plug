package com.polishmediahub.app.di

import com.polishmediahub.app.data.plugin.PluginMediaSource
import com.polishmediahub.app.data.remote.emby.EmbyMediaSource
import com.polishmediahub.app.data.remote.jellyfin.JellyfinMediaSource
import com.polishmediahub.app.data.remote.plex.PlexMediaSource
import com.polishmediahub.app.data.source.CloudstreamSource
import com.polishmediahub.app.data.source.KodiMediaSource
import com.polishmediahub.app.data.source.MediaSource
import com.polishmediahub.app.data.source.WebMediaSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class MediaSourceModule {

    @Binds
    @IntoSet
    abstract fun bindKodiMediaSource(source: KodiMediaSource): MediaSource

    @Binds
    @IntoSet
    abstract fun bindWebMediaSource(source: WebMediaSource): MediaSource

    @Binds
    @IntoSet
    abstract fun bindCloudstreamSource(source: CloudstreamSource): MediaSource

    @Binds
    @IntoSet
    abstract fun bindPluginMediaSource(source: PluginMediaSource): MediaSource

    @Binds
    @IntoSet
    abstract fun bindJellyfinMediaSource(source: JellyfinMediaSource): MediaSource

    @Binds
    @IntoSet
    abstract fun bindPlexMediaSource(source: PlexMediaSource): MediaSource

    @Binds
    @IntoSet
    abstract fun bindEmbyMediaSource(source: EmbyMediaSource): MediaSource
}
