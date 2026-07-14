package com.polishmediahub.app.data.remote.emby

import com.polishmediahub.app.data.ApiConfigRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Inject

class EmbyApiFactory @Inject constructor(
    private val client: OkHttpClient,
    private val apiConfigRepository: ApiConfigRepository
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun token(): String = apiConfigRepository.embyToken.first()
    suspend fun serverUrl(): String = apiConfigRepository.embyUrl.first().trim().ifBlank { EmbyApi.DEFAULT_BASE_URL }

    suspend fun create(): EmbyApi {
        val baseUrl = serverUrl()
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(EmbyApi::class.java)
    }
}
