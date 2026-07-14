package com.tvhub.skeleton.data.remote.debrid

import com.tvhub.skeleton.data.ApiConfigRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

class TorBoxService @Inject constructor(
    private val client: OkHttpClient,
    private val apiConfigRepository: ApiConfigRepository
) : DebridService {

    override val provider: DebridProvider = DebridProvider.TORBOX

    private val baseUrl = "https://api.torbox.app"
    private val apiVersion = "v1"
    private val json = Json { ignoreUnknownKeys = true }

    private suspend fun token(): String = apiConfigRepository.debridAccessToken.first()

    override suspend fun isAvailable(): Boolean = token().isNotBlank()

    override suspend fun getUserInfo(): DebridUserInfo {
        val request = Request.Builder()
            .url("$baseUrl/$apiVersion/api/user/me")
            .header("Authorization", "Bearer ${token()}")
            .build()
        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            DebridUserInfo("", false) // TODO parse
        } else {
            DebridUserInfo("", false)
        }
    }

    override suspend fun resolve(videoUrl: String): DebridStreamResult? {
        val body = FormBody.Builder().add("link", videoUrl).build()
        val request = Request.Builder()
            .url("$baseUrl/$apiVersion/api/webdl/createwebdownload")
            .post(body)
            .header("Authorization", "Bearer ${token()}")
            .build()
        client.newCall(request).execute()
        return DebridStreamResult("", "", null)
    }

    override suspend fun addMagnet(magnet: String): DebridTorrentResult {
        val body = FormBody.Builder().add("magnet", magnet).build()
        val request = Request.Builder()
            .url("$baseUrl/$apiVersion/api/torrents/createtorrent")
            .post(body)
            .header("Authorization", "Bearer ${token()}")
            .build()
        client.newCall(request).execute()
        return DebridTorrentResult("", "", "")
    }

    override suspend fun getTorrentFiles(id: String): List<DebridFile> {
        return emptyList()
    }

    suspend fun getTorrentList(): List<TorBoxTorrent> {
        val request = Request.Builder()
            .url("$baseUrl/$apiVersion/api/torrents/mylist")
            .header("Authorization", "Bearer ${token()}")
            .build()
        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            response.body?.string()?.let { body ->
                try {
                    json.decodeFromString(TorBoxTorrentListResponse.serializer(), body).data
                } catch (_: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        } else {
            emptyList()
        }
    }

    suspend fun requestDownloadLink(torrentId: Long, fileId: Long, redirect: Boolean = false): String? {
        val request = Request.Builder()
            .url("$baseUrl/$apiVersion/api/torrents/requestdl?token=${token()}&torrent_id=$torrentId&file_id=$fileId&redirect=$redirect")
            .header("Authorization", "Bearer ${token()}")
            .build()
        val response = client.newCall(request).execute()
        return if (response.isSuccessful) response.body?.string() else null
    }
}

@Serializable
data class TorBoxTorrentListResponse(
    val success: Boolean,
    val data: List<TorBoxTorrent>
)

@Serializable
data class TorBoxTorrent(
    val id: Long,
    @SerialName("torrent_id") val torrentId: Long,
    val hash: String,
    @SerialName("created_at") val createdAt: String,
    val status: String,
    val name: String,
    val files: List<TorBoxFile>? = null
)

@Serializable
data class TorBoxFile(
    val id: Long,
    val name: String,
    val size: Long,
    @SerialName("short_name") val shortName: String? = null
)
