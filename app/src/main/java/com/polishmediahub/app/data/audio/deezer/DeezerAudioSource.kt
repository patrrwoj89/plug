package com.polishmediahub.app.data.audio.deezer

import com.polishmediahub.app.data.ApiConfigRepository
import com.polishmediahub.app.data.audio.AudioSource
import com.polishmediahub.app.data.source.HeadlessWebSolver
import com.polishmediahub.app.model.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeezerAudioSource @Inject constructor(
    private val apiConfigRepository: ApiConfigRepository,
    private val okHttpClient: OkHttpClient,
    private val json: Json
) : AudioSource {

    override val id: String = "deezer"
    override val name: String = "Deezer"

    private companion object {
        private const val MAX_RETRIES = 3
    }

    private suspend fun baseUrl(): String = apiConfigRepository.deezerProxyUrl.first().removeSuffix("/")

    override suspend fun isAvailable(): Boolean {
        val url = baseUrl()
        return url.isNotBlank() && url.toHttpUrlOrNull() != null
    }

    override suspend fun browse(): List<AudioTrack> = withContext(Dispatchers.IO) {
        try {
            val base = baseUrl()
            getChart().map { it.toAudioTrack(base) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun search(query: String): List<AudioTrack> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val base = baseUrl()
            searchTracks(query).map { it.toAudioTrack(base) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun byId(trackId: String): AudioTrack? = withContext(Dispatchers.IO) {
        if (!trackId.startsWith("deezer:track:")) return@withContext null
        val deezerId = trackId.removePrefix("deezer:track:").toLongOrNull() ?: return@withContext null
        try {
            val base = baseUrl()
            getTrack(deezerId).toAudioTrack(base)
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun resolve(track: AudioTrack): String? = withContext(Dispatchers.IO) {
        if (track.sourceId != id) return@withContext track.streamUrl
        if (!track.streamUrl.isNullOrBlank()) return@withContext track.streamUrl

        val trackId = track.id.removePrefix("deezer:track:").toLongOrNull() ?: return@withContext null
        try {
            getTrack(trackId).preview.ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getChart(): List<DeezerTrack> = fetchWithRetries {
        val response = get("/chart/0/tracks")
        json.decodeFromString(DeezerChartResponse.serializer(), response).data
    }

    suspend fun searchTracks(query: String): List<DeezerTrack> = fetchWithRetries {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        val response = get("/search?q=$encoded")
        json.decodeFromString(DeezerSearchResponse.serializer(), response).data
    }

    suspend fun getAlbum(albumId: Long): DeezerAlbumDetail = fetchWithRetries {
        val response = get("/album/$albumId")
        json.decodeFromString(DeezerAlbumDetail.serializer(), response)
    }

    suspend fun getTrack(trackId: Long): DeezerTrack = fetchWithRetries {
        val response = get("/track/$trackId")
        json.decodeFromString(DeezerTrack.serializer(), response)
    }

    private suspend fun <T> fetchWithRetries(block: suspend () -> T): T {
        var lastError: Exception? = null
        repeat(MAX_RETRIES) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastError = e
                if (attempt < MAX_RETRIES - 1) {
                    delay(500L * (attempt + 1))
                }
            }
        }
        throw lastError ?: Exception("Deezer proxy request failed")
    }

    private suspend fun get(path: String): String {
        val base = baseUrl().ifBlank { throw IllegalStateException("Deezer proxy URL not configured") }
        val url = base.removeSuffix("/") + "/" + path.removePrefix("/")
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", HeadlessWebSolver.BROWSER_USER_AGENT)
            .header("Accept", "application/json")
            .build()
        return okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
            response.body?.string() ?: throw Exception("Empty response")
        }
    }

    private fun DeezerTrack.toAudioTrack(base: String): AudioTrack = AudioTrack(
        id = "deezer:track:$id",
        title = title,
        artist = artist.name,
        album = album.title,
        coverUrl = album.cover ?: artist.picture,
        streamUrl = preview,
        durationMs = duration * 1000L,
        sourceId = this@DeezerAudioSource.id,
        headers = mapOf(
            "User-Agent" to HeadlessWebSolver.BROWSER_USER_AGENT,
            "Referer" to "$base/"
        )
    )
}
