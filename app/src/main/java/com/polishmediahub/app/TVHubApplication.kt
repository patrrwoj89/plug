package com.polishmediahub.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.polishmediahub.app.data.source.GlobalExceptionHandler
import com.polishmediahub.app.data.tv.RecommendationsWorker
import com.polishmediahub.app.data.torrent.TorrentMediaSource
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class TVHubApplication : Application(), Configuration.Provider {

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
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
