package com.tvhub.skeleton.data.remote.debrid

import com.tvhub.skeleton.data.ApiConfigRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.ListSerializer
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
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private suspend fun token(): String = apiConfigRepository.debridAccessToken.first()

    override suspend fun isAvailable(): Boolean = token().isNotBlank()

    override suspend fun getUserInfo(): DebridUserInfo {
        val request = Request.Builder()
            .url("$baseUrl/user")
            .header("Authorization", "Bearer ${token()}")
            .build()
        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            response.body?.string()?.let { body ->
                try {
                    val user = json.decodeFromString(RealDebridUser.serializer(), body)
                    DebridUserInfo(user.username, user.premium > 0 || user.type == "premium")
                } catch (_: Exception) {
                    DebridUserInfo("", false)
                }
            } ?: DebridUserInfo("", false)
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
        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            response.body?.string()?.let { responseBody ->
                try {
                    val result = json.decodeFromString(RealDebridUnrestrictedLink.serializer(), responseBody)
                    DebridStreamResult(url = result.download, name = result.filename, quality = null)
                } catch (_: Exception) {
                    null
                }
            }
        } else {
            null
        }
    }

    override suspend fun addMagnet(magnet: String): DebridTorrentResult {
        val body = FormBody.Builder().add("magnet", magnet).build()
        val request = Request.Builder()
            .url("$baseUrl/torrents/addMagnet")
            .post(body)
            .header("Authorization", "Bearer ${token()}")
            .build()
        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            response.body?.string()?.let { responseBody ->
                try {
                    val result = json.decodeFromString(RealDebridTorrent.serializer(), responseBody)
                    DebridTorrentResult(id = result.id, uri = "", status = result.status)
                } catch (_: Exception) {
                    DebridTorrentResult("", "", "")
                }
            } ?: DebridTorrentResult("", "", "")
        } else {
            DebridTorrentResult("", "", "")
        }
    }

    override suspend fun getTorrentFiles(id: String): List<DebridFile> {
        val request = Request.Builder()
            .url("$baseUrl/torrents/info/$id")
            .header("Authorization", "Bearer ${token()}")
            .build()
        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            response.body?.string()?.let { body ->
                try {
                    val result = json.decodeFromString(RealDebridTorrent.serializer(), body)
                    result.files?.map { file ->
                        DebridFile(
                            id = file.id.toString(),
                            path = file.path,
                            bytes = file.bytes,
                            url = null
                        )
                    } ?: emptyList()
                } catch (_: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        } else {
            emptyList()
        }
    }

    suspend fun selectFiles(id: String, fileIds: String) {
        val body = FormBody.Builder().add("files", fileIds).build()
        val request = Request.Builder()
            .url("$baseUrl/torrents/selectFiles/$id")
            .post(body)
            .header("Authorization", "Bearer ${token()}")
            .build()
        client.newCall(request).execute()
    }

    suspend fun deleteTorrent(id: String) {
        val request = Request.Builder()
            .url("$baseUrl/torrents/delete/$id")
            .delete()
            .header("Authorization", "Bearer ${token()}")
            .build()
        client.newCall(request).execute()
    }

    suspend fun getTorrents(limit: Int = 100): List<RealDebridTorrent> {
        val request = Request.Builder()
            .url("$baseUrl/torrents?limit=$limit")
            .header("Authorization", "Bearer ${token()}")
            .build()
        val response = client.newCall(request).execute()
        return if (response.isSuccessful) {
            response.body?.string()?.let { body ->
                try {
                    json.decodeFromString(ListSerializer(RealDebridTorrent.serializer()), body)
                } catch (_: Exception) {
                    emptyList()
                }
            } ?: emptyList()
        } else {
            emptyList()
        }
    }
}
