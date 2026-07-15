package com.polishmediahub.app.data.remote.trakt

import com.polishmediahub.app.data.ApiConfigRepository
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class TraktDeviceCodeResponse(
    @SerialName("device_code") val deviceCode: String,
    @SerialName("user_code") val userCode: String,
    @SerialName("verification_url") val verificationUrl: String,
    @SerialName("expires_in") val expiresIn: Int,
    val interval: Int
)

@Serializable
data class TraktOAuthTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Int? = null
)

@Serializable
private data class TraktOAuthError(
    val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null
)

@Singleton
class TraktAuthManager @Inject constructor(
    private val client: OkHttpClient,
    private val apiConfigRepository: ApiConfigRepository
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Step 1 of Trakt.tv device-code flow.
     * https://trakt.docs.apiary.io/#reference/authentication-devices/device-code
     */
    suspend fun startPairing(clientId: String): TraktDeviceCodeResponse {
        val body = FormBody.Builder()
            .add("client_id", clientId)
            .build()

        val request = Request.Builder()
            .url("${TraktApi.BASE_URL}oauth/device/code")
            .post(body)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("trakt-api-version", "2")
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: ""

        if (!response.isSuccessful) {
            throw IllegalStateException("Trakt device code request failed: ${response.code} $responseBody")
        }
        return json.decodeFromString(TraktDeviceCodeResponse.serializer(), responseBody)
    }

    /**
     * Step 2: poll the token endpoint until the user authorizes the device.
     * Throws on expiration, user denial, or other errors.
     */
    suspend fun completePairing(
        clientId: String,
        clientSecret: String,
        deviceCode: String,
        interval: Int
    ): TraktOAuthTokenResponse {
        var currentInterval = (interval * 1000).coerceAtLeast(5000)

        while (true) {
            val body = FormBody.Builder()
                .add("client_id", clientId)
                .add("client_secret", clientSecret)
                .add("code", deviceCode)
                .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                .build()

            val request = Request.Builder()
                .url("${TraktApi.BASE_URL}oauth/device/token")
                .post(body)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("trakt-api-version", "2")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                return json.decodeFromString(TraktOAuthTokenResponse.serializer(), responseBody)
            }

            val error = try {
                json.decodeFromString(TraktOAuthError.serializer(), responseBody).error
            } catch (_: Exception) {
                null
            }

            when (error) {
                "authorization_pending" -> {
                    delay(currentInterval.toLong())
                }
                "slow_down" -> {
                    currentInterval += 5000
                    delay(currentInterval.toLong())
                }
                "expired_token" -> throw IllegalStateException("Trakt device code expired. Please start pairing again.")
                "access_denied" -> throw IllegalStateException("Trakt authorization denied by user.")
                else -> throw IllegalStateException("Trakt token error: $responseBody")
            }
        }
    }

    /**
     * Convenience helper that reads the saved client id/secret, requests a code and
     * returns it. The UI is responsible for polling [completePairing].
     */
    suspend fun startPairingWithSavedCredentials(): TraktDeviceCodeResponse {
        val clientId = apiConfigRepository.traktClientId.first()
        val clientSecret = apiConfigRepository.traktClientSecret.first()
        require(clientId.isNotBlank() && clientSecret.isNotBlank()) { "Trakt client id and secret are required" }
        return startPairing(clientId)
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String?) {
        apiConfigRepository.setTraktAccessToken(accessToken)
        refreshToken?.let { apiConfigRepository.setTraktRefreshToken(it) }
    }
}
