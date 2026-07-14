package com.tvhub.skeleton.data.remote.debrid

import android.util.Base64
import com.tvhub.skeleton.data.ApiConfigRepository
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import javax.inject.Inject

enum class DebridProvider(
    val id: String,
    val displayName: String,
    val clientId: String = "",
    val deviceCodeUrl: String = "",
    val tokenUrl: String = "",
    val apiKeyBased: Boolean = false
) {
    REAL_DEBRID(
        id = "real_debrid",
        displayName = "Real-Debrid",
        clientId = "X245A4XAIBGVM",
        deviceCodeUrl = "https://api.real-debrid.com/oauth/v2/device/code?client_id=%s&new_credentials=yes",
        tokenUrl = "https://api.real-debrid.com/oauth/v2/token"
    ),
    TORBOX(
        id = "torbox",
        displayName = "TorBox",
        apiKeyBased = true
    ),
    ALLDEBRID(
        id = "alldebrid",
        displayName = "AllDebrid",
        apiKeyBased = true
    ),
    PREMIUMIZE(
        id = "premiumize",
        displayName = "Premiumize",
        apiKeyBased = true
    )
}

@Serializable
data class DeviceCodeResponse(
    @SerialName("device_code") val deviceCode: String,
    @SerialName("user_code") val userCode: String,
    @SerialName("verification_uri") val verificationUri: String,
    val interval: Int,
    @SerialName("expires_in") val expiresIn: Int
)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Int? = null
)

class DebridOAuthManager @Inject constructor(
    private val client: OkHttpClient,
    private val apiConfigRepository: ApiConfigRepository
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun startDeviceFlow(provider: DebridProvider = DebridProvider.REAL_DEBRID): DeviceCodeResponse {
        val url = provider.deviceCodeUrl.format(URLEncoder.encode(provider.clientId, "UTF-8"))
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw IllegalStateException("Empty device code response")
        return json.decodeFromString(DeviceCodeResponse.serializer(), body)
    }

    suspend fun pollForToken(
        provider: DebridProvider = DebridProvider.REAL_DEBRID,
        deviceCode: String
    ): TokenResponse {
        val credentials = "${provider.clientId}:".toByteArray()
        val basic = "Basic ${Base64.encodeToString(credentials, Base64.NO_WRAP)}"

        while (true) {
            val body = FormBody.Builder()
                .add("client_id", provider.clientId)
                .add("code", deviceCode)
                .add("grant_type", "http://oauth.net/grant_type/device/1.0")
                .build()

            val request = Request.Builder()
                .url(provider.tokenUrl)
                .post(body)
                .header("Authorization", basic)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                return json.decodeFromString(TokenResponse.serializer(), responseBody)
            } else if (response.code == 400 && responseBody.contains("authorization_pending")) {
                delay(5_000)
                continue
            } else {
                throw IllegalStateException("OAuth token error: $responseBody")
            }
        }
    }

    suspend fun authorize(provider: DebridProvider = DebridProvider.REAL_DEBRID): DeviceCodeResponse {
        apiConfigRepository.setDebridProvider(provider.id)
        if (provider.apiKeyBased) {
            throw IllegalStateException("${provider.displayName} requires an API key, not OAuth device flow.")
        }
        return startDeviceFlow(provider)
    }

    suspend fun finishAuthorization(deviceCode: String, provider: DebridProvider = DebridProvider.REAL_DEBRID) {
        if (provider.apiKeyBased) return
        val token = pollForToken(deviceCode = deviceCode)
        apiConfigRepository.setDebridAccessToken(token.accessToken)
        token.refreshToken?.let { apiConfigRepository.setDebridRefreshToken(it) }
    }
}
