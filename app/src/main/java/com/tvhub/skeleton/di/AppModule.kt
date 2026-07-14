package com.tvhub.skeleton.di

import android.content.Context
import androidx.room.Room
import com.tvhub.skeleton.data.MockDataSource
import com.tvhub.skeleton.data.local.HistoryDao
import com.tvhub.skeleton.data.local.MediaDatabase
import com.tvhub.skeleton.data.local.PluginDao
import com.tvhub.skeleton.data.local.SavedMediaDao
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
    fun provideMockDataSource(): MockDataSource = MockDataSource()

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MediaDatabase =
        Room.databaseBuilder(
            context,
            MediaDatabase::class.java,
            "media.db"
        ).fallbackToDestructiveMigration(true)
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
}
