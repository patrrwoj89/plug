package com.polishmediahub.app.data.audio

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.polishmediahub.app.model.AudioTrack
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LocalAudioSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) : AudioSource {

    override val id: String = "local_audio"
    override val name: String = "Local music"

    override suspend fun isAvailable(): Boolean = true

    override suspend fun browse(): List<AudioTrack> = withContext(Dispatchers.IO) { loadAudio() }

    override suspend fun search(query: String): List<AudioTrack> =
        browse().filter { it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true) }

    override suspend fun byId(trackId: String): AudioTrack? {
        if (!trackId.startsWith("local_audio:")) return null
        return withContext(Dispatchers.IO) { loadAudio().find { it.id == trackId } }
    }

    override suspend fun resolve(track: AudioTrack): String? = track.streamUrl

    private fun loadAudio(): List<AudioTrack> {
        val tracks = mutableListOf<AudioTrack>()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        context.contentResolver.query(uri, projection, selection, null, null)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val contentUri: Uri = ContentUris.withAppendedId(uri, id)
                tracks += AudioTrack(
                    id = "local_audio:$id",
                    title = cursor.getString(titleCol) ?: "",
                    artist = cursor.getString(artistCol) ?: "",
                    album = cursor.getString(albumCol) ?: "",
                    streamUrl = contentUri.toString(),
                    durationMs = cursor.getLong(durationCol),
                    isLocal = true,
                    sourceId = this@LocalAudioSource.id
                )
            }
        }
        return tracks
    }
}
