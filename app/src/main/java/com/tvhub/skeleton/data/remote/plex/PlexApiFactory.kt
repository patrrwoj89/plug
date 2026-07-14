package com.tvhub.skeleton.data.remote.plex

import com.tvhub.skeleton.data.ApiConfigRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Inject

class PlexApiFactory @Inject constructor(
    private val client: OkHttpClient,
    private val apiConfigRepository: ApiConfigRepository
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun token(): String = apiConfigRepository.plexToken.first()
    suspend fun serverUrl(): String = apiConfigRepository.plexUrl.first().trim().ifBlank { PlexApi.DEFAULT_BASE_URL }

    suspend fun create(): PlexApi {
        val baseUrl = serverUrl()
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val plexClient = client.newBuilder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .build()
        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(plexClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(PlexApi::class.java)
    }
}
