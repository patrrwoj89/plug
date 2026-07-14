package com.tvhub.skeleton.data.remote.debrid

import com.tvhub.skeleton.data.ApiConfigRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

class RealDebridService @Inject constructor(
    private val client: OkHttpClient,
    private val apiConfigRepository: ApiConfigRepository
) : DebridService {

    override val provider: DebridProvider = DebridProvider.REAL_DEBRID

    private val baseUrl = "https://api.real-debrid.com/rest/1.0"
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun token(): String = apiConfigRepository.debridAccessToken.first()

    override suspend fun isAvailable(): Boolean = token().isNotBlank()

    override suspend fun getUserInfo(): DebridUserInfo {
        val request = Request.Builder()
            .url("$baseUrl/user")
            .header("Authorization", "Bearer ${token()}")
            .build()
        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            val body = response.body?.string().orEmpty()
            DebridUserInfo("", false) // TODO parse
        } else {
            DebridUserInfo("", false)
        }
    }

    override suspend fun resolve(videoUrl: String): DebridStreamResult? {
        val body = FormBody.Builder().add("link", videoUrl).build()
        val request = Request.Builder()
            .url("$baseUrl/unrestrict/link")
            .post(body)
            .header("Authorization", "Bearer ${token()}")
            .build()
        client.newCall(request).execute()
        return DebridStreamResult("", "", null)
    }

    override suspend fun addMagnet(magnet: String): DebridTorrentResult {
        val body = FormBody.Builder().add("magnet", magnet).build()
        val request = Request.Builder()
            .url("$baseUrl/torrents/addMagnet")
            .post(body)
            .header("Authorization", "Bearer ${token()}")
            .build()
        client.newCall(request).execute()
        return DebridTorrentResult("", "", "")
    }

    override suspend fun getTorrentFiles(id: String): List<DebridFile> {
        return emptyList()
    }
}
