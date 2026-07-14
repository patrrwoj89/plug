package com.polishmediahub.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
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
        val torrentDir = File(filesDir, "torrents").apply { mkdirs() }
        torrentMediaSource.configure(torrentDir)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
