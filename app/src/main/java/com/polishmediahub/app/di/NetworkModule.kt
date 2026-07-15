package com.polishmediahub.app.di

import android.content.Context
import coil.ImageLoader
import coil.imageLoader
import com.polishmediahub.app.BuildConfig
import com.polishmediahub.app.data.remote.RetryInterceptor
import com.polishmediahub.app.data.remote.debrid.DebridAuthenticator
import com.polishmediahub.app.data.source.CloudflareBypassInterceptor
import com.polishmediahub.app.data.source.MemoryCookieJar
import com.polishmediahub.app.data.remote.anilist.AniListApi
import com.polishmediahub.app.data.remote.stremio.StremioApi
import com.polishmediahub.app.data.remote.tmdb.TmdbApi
import com.polishmediahub.app.data.remote.trakt.TraktApi
import com.polishmediahub.app.data.remote.trakt.TraktAuthenticator
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
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Provides
    @Singleton
    fun provideJson(): Json = json

    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader = context.imageLoader

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        debridAuthenticator: DebridAuthenticator,
        cookieJar: MemoryCookieJar,
        cloudflareBypassInterceptor: CloudflareBypassInterceptor
    ): OkHttpClient {
        val cacheDir = File(context.cacheDir, "http_cache").apply { mkdirs() }
        return OkHttpClient.Builder()
            .cache(Cache(cacheDir, 50 * 1024 * 1024))
            .cookieJar(cookieJar)
            .authenticator(debridAuthenticator)
            .addInterceptor(cloudflareBypassInterceptor)
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
    @Named("trakt")
    fun provideTraktOkHttpClient(
        client: OkHttpClient,
        traktAuthenticator: TraktAuthenticator
    ): OkHttpClient = client.newBuilder()
        .authenticator(traktAuthenticator)
        .build()

    @Provides
    @Singleton
    fun provideTraktApi(@Named("trakt") client: OkHttpClient): TraktApi = Retrofit.Builder()
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
