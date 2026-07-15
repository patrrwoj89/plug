package com.polishmediahub.app.data.remote.debrid

import com.polishmediahub.app.data.ApiConfigRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp Authenticator that refreshes the Debrid access token when a request returns 401.
 * Currently supports the OAuth-style Real-Debrid refresh flow; API-key providers
 * (TorBox, AllDebrid, Premiumize) cannot be refreshed and must be re-configured.
 */
@Singleton
class DebridAuthenticator @Inject constructor(
    private val apiConfigRepository: ApiConfigRepository,
    private val json: Json
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.responseCount >= 3) return null
        if (response.request.header("Authorization") == null) return null

        val newAccessToken = refreshAccessToken() ?: return null
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccessToken")
            .build()
    }

    private fun refreshAccessToken(): String? = runBlocking {
        val providerId = apiConfigRepository.debridProvider.first()
        val provider = DebridProvider.entries.find { it.id == providerId } ?: return@runBlocking null

        when {
            provider.apiKeyBased -> {
                // API-key providers do not support token refresh; fail the request.
                null
            }
            provider == DebridProvider.REAL_DEBRID -> refreshRealDebrid(provider)
            else -> null
        }
    }

    private suspend fun refreshRealDebrid(provider: DebridProvider): String? {
        val refreshToken = apiConfigRepository.debridRefreshToken.first()
        if (refreshToken.isBlank()) return null

        val body = FormBody.Builder()
            .add("client_id", provider.clientId)
            .add("refresh_token", refreshToken)
            .add("grant_type", "refresh_token")
            .build()

        val request = Request.Builder()
            .url(provider.tokenUrl)
            .post(body)
            .build()

        // Use a fresh client without this authenticator to avoid recursion.
        val refreshClient = OkHttpClient()
        return try {
            refreshClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                if (!response.isSuccessful || responseBody.isNullOrBlank()) return@use null

                val token = json.decodeFromString(TokenResponse.serializer(), responseBody)
                apiConfigRepository.setDebridAccessToken(token.accessToken)
                token.refreshToken?.let { apiConfigRepository.setDebridRefreshToken(it) }
                token.accessToken
            }
        } catch (_: Exception) {
            null
        }
    }

    private val Response.responseCount: Int
        get() {
            var count = 1
            var prior = priorResponse
            while (prior != null) {
                count++
                prior = prior.priorResponse
            }
            return count
        }
}
