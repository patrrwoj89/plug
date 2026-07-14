package com.polishmediahub.app.data.audio

import com.polishmediahub.app.data.ApiConfigRepository
import com.polishmediahub.app.model.AudioTrack
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Inject

class SubsonicSource @Inject constructor(
    private val apiConfigRepository: ApiConfigRepository,
    private val okHttpClient: OkHttpClient,
    private val json: Json
) : AudioSource {

    override val id: String = "subsonic"
    override val name: String = "Subsonic/Airsonic"

    private suspend fun url(): String = apiConfigRepository.subsonicUrl.first().removeSuffix("/")
    private suspend fun user(): String = apiConfigRepository.subsonicUser.first()
    private suspend fun password(): String = apiConfigRepository.subsonicPassword.first()

    private suspend fun api(): SubsonicApi {
        val base = url().ifBlank { SubsonicApi.DEFAULT_BASE_URL }
        return Retrofit.Builder()
            .baseUrl("$base/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(SubsonicApi::class.java)
    }

    override suspend fun isAvailable(): Boolean = url().isNotBlank() && user().isNotBlank() && password().isNotBlank()

    override suspend fun browse(): List<AudioTrack> = try {
        val api = api()
        val response = api.getArtists(user(), password()).subsonicResponse
        val artists = response?.artists?.indexes?.flatMap { it.artist } ?: emptyList()
        artists.map { artist ->
            AudioTrack(
                id = "subsonic:artist:${artist.id}",
                title = artist.name,
                artist = "",
                album = "",
                sourceId = id,
                coverUrl = coverUrl(artist.coverArt)
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun browseDirectory(directoryId: String): List<AudioTrack> = try {
        val api = api()
        if (directoryId.startsWith("subsonic:artist:")) {
            val realId = directoryId.removePrefix("subsonic:artist:")
            val dir = api.getMusicDirectory(realId, user(), password()).subsonicResponse?.directory
            dir?.child?.map { child -> child.toTrack() } ?: emptyList()
        } else {
            val dir = api.getMusicDirectory(directoryId, user(), password()).subsonicResponse?.directory
            dir?.child?.map { child -> child.toTrack() } ?: emptyList()
        }
    } catch (_: Exception) {
        emptyList()
    }

    suspend fun browseAlbum(albumId: String): List<AudioTrack> = try {
        val api = api()
        val realId = albumId.removePrefix("subsonic:album:")
        val album = api.getAlbum(realId, user(), password()).subsonicResponse?.album
        album?.song?.map { it.toTrack(album.name, album.artist) } ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    override suspend fun search(query: String): List<AudioTrack> = emptyList()

    override suspend fun resolve(track: AudioTrack): String? {
        val base = url().ifBlank { return null }
        val songId = track.id.removePrefix("subsonic:song:")
        return "$base/rest/stream.view?id=$songId&u=${user()}&p=${password()}&v=1.16.1&c=PolishMediaHub"
    }

    private suspend fun SubsonicChild.toTrack(albumName: String = "", artistName: String = ""): AudioTrack {
        val cover = coverUrl(coverArt)
        return if (isDir || type == "musicDirectory" || type == "artist" || type == "album") {
            AudioTrack(
                id = if (isDir) "subsonic:dir:$id" else "subsonic:album:$id",
                title = title,
                artist = artistName,
                album = albumName,
                sourceId = id,
                coverUrl = cover
            )
        } else {
            AudioTrack(
                id = "subsonic:song:$id",
                title = title,
                artist = artist,
                album = album,
                sourceId = id,
                durationMs = duration * 1000L,
                coverUrl = cover,
                streamUrl = resolveFromId(id)
            )
        }
    }

    private suspend fun resolveFromId(songId: String): String? {
        val base = url().ifBlank { return null }
        return "$base/rest/stream.view?id=$songId&u=${user()}&p=${password()}&v=1.16.1&c=PolishMediaHub"
    }

    private suspend fun coverUrl(coverArt: String?): String? {
        if (coverArt == null) return null
        val base = url().ifBlank { return null }
        return "$base/rest/getCoverArt.view?id=$coverArt&u=${user()}&p=${password()}&v=1.16.1&c=PolishMediaHub"
    }
}
