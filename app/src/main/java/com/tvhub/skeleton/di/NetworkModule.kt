package com.tvhub.skeleton.di

import android.content.Context
import com.tvhub.skeleton.BuildConfig
import com.tvhub.skeleton.data.remote.RetryInterceptor
import com.tvhub.skeleton.data.remote.anilist.AniListApi
import com.tvhub.skeleton.data.remote.stremio.StremioApi
import com.tvhub.skeleton.data.remote.tmdb.TmdbApi
import com.tvhub.skeleton.data.remote.trakt.TraktApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        val cacheDir = File(context.cacheDir, "http_cache").apply { mkdirs() }
        return OkHttpClient.Builder()
            .cache(Cache(cacheDir, 50 * 1024 * 1024))
            .addInterceptor(RetryInterceptor(maxRetries = 3))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideTmdbApi(client: OkHttpClient): TmdbApi = Retrofit.Builder()
        .baseUrl("https://api.themoviedb.org/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(TmdbApi::class.java)

    @Provides
    @Singleton
    fun provideAniListApi(client: OkHttpClient): AniListApi = Retrofit.Builder()
        .baseUrl(AniListApi.BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(AniListApi::class.java)

    @Provides
    @Singleton
    fun provideTraktApi(client: OkHttpClient): TraktApi = Retrofit.Builder()
        .baseUrl(TraktApi.BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(TraktApi::class.java)

    @Provides
    @Singleton
    fun provideStremioApi(client: OkHttpClient): StremioApi = Retrofit.Builder()
        .baseUrl("https://stremio.github.io/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(StremioApi::class.java)
}
