package com.tvhub.skeleton.di

import com.tvhub.skeleton.data.plugin.PluginMediaSource
import com.tvhub.skeleton.data.remote.emby.EmbyMediaSource
import com.tvhub.skeleton.data.remote.jellyfin.JellyfinMediaSource
import com.tvhub.skeleton.data.remote.plex.PlexMediaSource
import com.tvhub.skeleton.data.source.CloudstreamSource
import com.tvhub.skeleton.data.source.KodiMediaSource
import com.tvhub.skeleton.data.source.MediaSource
import com.tvhub.skeleton.data.source.WebMediaSource
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
