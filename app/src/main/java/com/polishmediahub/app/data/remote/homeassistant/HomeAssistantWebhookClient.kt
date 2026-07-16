package com.polishmediahub.app.data.remote.homeassistant

import com.polishmediahub.app.BuildConfig
import com.polishmediahub.app.data.ApiConfigRepository
import com.polishmediahub.app.data.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

interface HomeAssistantWebhookClient {
    suspend fun send(event: String, mediaTitle: String? = null): Result<Unit>
}

@Singleton
class OkHttpHomeAssistantWebhookClient @Inject constructor(
    private val apiConfigRepository: ApiConfigRepository,
    private val profileRepository: ProfileRepository
) : HomeAssistantWebhookClient {

    private val webhookClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .writeTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    override suspend fun send(event: String, mediaTitle: String?): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val enabled = apiConfigRepository.homeAssistantWebhookEnabled.first()
            if (!enabled) return@withContext Result.success(Unit)

            val baseUrl = apiConfigRepository.homeAssistantUrl.first().removeSuffix("/")
            val token = apiConfigRepository.homeAssistantToken.first()
            if (baseUrl.isBlank() || token.isBlank()) return@withContext Result.success(Unit)

            val url = "$baseUrl/api/webhook/${token.removePrefix("/")}"
            val profileName = profileRepository.currentProfile.first()?.name ?: "unknown"
            val payload = buildString {
                append("{\"event\":\"").append(event.escape()).append("\"")
                append(",\"profile\":\"").append(profileName.escape()).append("\"")
                if (!mediaTitle.isNullOrBlank()) {
                    append(",\"media\":\"").append(mediaTitle.escape()).append("\"")
                }
                append("}")
            }

            val body = payload.toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .header("Content-Type", "application/json")
                .build()

            val response = webhookClient.newCall(request).execute()
            try {
                if (BuildConfig.DEBUG) {
                    android.util.Log.d(
                        TAG,
                        "Home Assistant webhook $event -> ${baseUrl}/api/webhook/**** HTTP ${response.code}"
                    )
                }
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        java.io.IOException("Home Assistant webhook failed: HTTP ${response.code}")
                    )
                }
                Result.success(Unit)
            } finally {
                response.body?.close()
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) android.util.Log.w(TAG, "Home Assistant webhook failed", e)
            Result.failure(e)
        }
    }

    private fun String.escape(): String =
        replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")

    companion object {
        private const val TAG = "HomeAssistantWebhookClient"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
