package com.polishmediahub.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.request.CachePolicy
import com.polishmediahub.app.data.remote.cache.HomePreFetchWorker
import com.polishmediahub.app.data.remote.health.HealthCheckWorker
import com.polishmediahub.app.data.remote.iptv.IptvUpdateWorker
import com.polishmediahub.app.data.remote.trakt.TraktSyncWorker
import com.polishmediahub.app.data.source.GlobalExceptionHandler
import com.polishmediahub.app.data.tv.RecommendationsWorker
import com.polishmediahub.app.data.torrent.TorrentMediaSource
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class TVHubApplication : Application(), Configuration.Provider, ImageLoaderFactory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var torrentMediaSource: TorrentMediaSource

    override fun onCreate() {
        super.onCreate()

        // The crash-report process should not schedule workers, configure torrents,
        // or install another crash handler, to avoid recursion and heavy init.
        if (GlobalExceptionHandler.shouldInstallHandler(this)) {
            val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(GlobalExceptionHandler(this, previousHandler))
            File(filesDir, "torrents").apply { mkdirs() }
            torrentMediaSource.configure()
            RecommendationsWorker.schedule(this)
            IptvUpdateWorker.schedule(this)
            IptvUpdateWorker.startImmediate(this)
            TraktSyncWorker.schedule(this)
            HealthCheckWorker.schedule(this)
            HealthCheckWorker.startImmediate(this)
            HomePreFetchWorker.schedule(this)
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "image_cache").apply { mkdirs() })
                    .maxSizeBytes(100 * 1024 * 1024L)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .crossfade(true)
            .build()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
