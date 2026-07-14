package com.polishmediahub.app.data.iptv

import android.content.Context
import com.polishmediahub.app.data.local.EpgDao
import com.polishmediahub.app.data.local.EpgEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpgRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val client: OkHttpClient,
    private val epgDao: EpgDao
) {

    fun currentProgramForChannel(channelId: String): Flow<List<EpgEntity>> {
        val now = System.currentTimeMillis()
        val day = 24 * 60 * 60 * 1000L
        return epgDao.observeForChannel(channelId, now - day, now + day)
    }

    suspend fun loadEpg(xmltvUrl: String) {
        try {
            val request = Request.Builder().url(xmltvUrl).build()
            client.newCall(request).execute().use { response ->
                response.body?.byteStream()?.use { stream ->
                    val entries = EpgParser.parse(stream)
                    epgDao.upsertAll(entries)
                    epgDao.deleteOlderThan(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L)
                }
            }
        } catch (_: Exception) {
        }
    }

    suspend fun loadEpgFromFile(file: File) {
        file.inputStream().use { stream ->
            val entries = EpgParser.parse(stream)
            epgDao.upsertAll(entries)
            epgDao.deleteOlderThan(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L)
        }
    }
}
