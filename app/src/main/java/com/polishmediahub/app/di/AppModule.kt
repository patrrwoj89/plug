package com.polishmediahub.app.di

import android.content.Context
import androidx.room.Room
import com.polishmediahub.app.data.MockDataSource
import com.polishmediahub.app.data.local.HistoryDao
import com.polishmediahub.app.data.local.MediaDatabase
import com.polishmediahub.app.data.local.DownloadDao
import com.polishmediahub.app.data.local.EpgDao
import com.polishmediahub.app.data.local.AudioHistoryDao
import com.polishmediahub.app.data.local.PluginDao
import com.polishmediahub.app.data.local.ProfileDao
import com.polishmediahub.app.data.local.ChannelDao
import com.polishmediahub.app.data.local.SavedMediaDao
import com.polishmediahub.app.data.EncryptedSettingsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideEncryptedSettingsManager(@ApplicationContext context: Context): EncryptedSettingsManager =
        EncryptedSettingsManager(context)

    @Provides
    @Singleton
    fun provideMockDataSource(): MockDataSource = MockDataSource()

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MediaDatabase =
        Room.databaseBuilder(
            context,
            MediaDatabase::class.java,
            "media.db"
        ).addMigrations(*MediaDatabase.MIGRATIONS)
            .build()

    @Provides
    fun provideSavedMediaDao(database: MediaDatabase): SavedMediaDao =
        database.savedMediaDao()

    @Provides
    fun provideHistoryDao(database: MediaDatabase): HistoryDao =
        database.historyDao()

    @Provides
    fun providePluginDao(database: MediaDatabase): PluginDao =
        database.pluginDao()

    @Provides
    fun provideDownloadDao(database: MediaDatabase): DownloadDao =
        database.downloadDao()

    @Provides
    fun provideEpgDao(database: MediaDatabase): EpgDao =
        database.epgDao()

    @Provides
    fun provideProfileDao(database: MediaDatabase): ProfileDao =
        database.profileDao()

    @Provides
    fun provideAudioHistoryDao(database: MediaDatabase): AudioHistoryDao =
        database.audioHistoryDao()

    @Provides
    fun provideChannelDao(database: MediaDatabase): ChannelDao =
        database.channelDao()
}
