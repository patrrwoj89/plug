package com.polishmediahub.app.data.remote.trakt

import android.util.Log
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
 * OkHttp Authenticator that refreshes the Trakt access token when a request to
 * the Trakt API returns HTTP 401. It uses the official OAuth refresh_token grant
 * and updates the encrypted values in DataStore before retrying the request.
 */
@Singleton
class TraktAuthenticator @Inject constructor(
    private val apiConfigRepository: ApiConfigRepository,
    private val json: Json
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.responseCount >= 3) return null
        if (response.request.header("Authorization") == null) return null
        if (response.request.header("X-Trakt-Refresh") != null) return null
        if (!response.request.url.host.contains("trakt", ignoreCase = true)) return null

        val newAccessToken = refreshAccessToken() ?: return null
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccessToken")
            .header("X-Trakt-Refresh", "1")
            .build()
    }

    private fun refreshAccessToken(): String? = runBlocking {
        val clientId = apiConfigRepository.traktClientId.first()
        val clientSecret = apiConfigRepository.traktClientSecret.first()
        val refreshToken = apiConfigRepository.traktRefreshToken.first()

        if (clientId.isBlank() || clientSecret.isBlank() || refreshToken.isBlank()) {
            return@runBlocking null
        }

        val body = FormBody.Builder()
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .add("refresh_token", refreshToken)
            .add("grant_type", "refresh_token")
            .build()

        val request = Request.Builder()
            .url("${TraktApi.BASE_URL}oauth/token")
            .post(body)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("trakt-api-version", "2")
            .build()

        // Use a fresh client without this authenticator to avoid recursion.
        val refreshClient = OkHttpClient()
        try {
            refreshClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                if (!response.isSuccessful || responseBody.isNullOrBlank()) return@use null

                val token = json.decodeFromString(TraktOAuthTokenResponse.serializer(), responseBody)
                apiConfigRepository.setTraktAccessToken(token.accessToken)
                token.refreshToken?.let { apiConfigRepository.setTraktRefreshToken(it) }
                token.accessToken
            }
        } catch (e: Exception) {
            Log.e("TraktAuthenticator", "Krytyczny błąd automatycznego odświeżania sesji OAuth Trakt: ${e.message}", e)
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
