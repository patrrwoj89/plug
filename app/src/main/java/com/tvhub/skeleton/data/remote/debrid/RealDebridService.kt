package com.tvhub.skeleton.data.remote.debrid

import com.tvhub.skeleton.data.ApiConfigRepository
import kotlinx.coroutines.flow.first
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

class RealDebridService @Inject constructor(
    private val client: OkHttpClient,
    private val apiConfigRepository: ApiConfigRepository
) : DebridService {

    private val baseUrl = "https://api.real-debrid.com/rest/1.0"

    private suspend fun accessToken(): String = apiConfigRepository.debridAccessToken.first()

    override suspend fun getUserInfo(apiKey: String): DebridUserInfo {
        val request = Request.Builder()
            .url("$baseUrl/user")
            .header("Authorization", "Bearer ${accessToken()}")
            .build()
        val response = client.newCall(request).execute()
        // TODO: parse JSON into DebridUserInfo
        return DebridUserInfo("", false)
    }

    override suspend fun unrestrictLink(apiKey: String, url: String): DebridStreamResult {
        val body = FormBody.Builder().add("link", url).build()
        val request = Request.Builder()
            .url("$baseUrl/unrestrict/link")
            .post(body)
            .header("Authorization", "Bearer ${accessToken()}")
            .build()
        client.newCall(request).execute()
        // TODO: parse JSON
        return DebridStreamResult("", "", null)
    }

    override suspend fun addMagnet(apiKey: String, magnet: String): DebridTorrentResult {
        val body = FormBody.Builder().add("magnet", magnet).build()
        val request = Request.Builder()
            .url("$baseUrl/torrents/addMagnet")
            .post(body)
            .header("Authorization", "Bearer ${accessToken()}")
            .build()
        client.newCall(request).execute()
        return DebridTorrentResult("", "", "")
    }

    override suspend fun getTorrentFiles(apiKey: String, id: String): List<DebridFile> {
        return emptyList()
    }
}
