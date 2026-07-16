package com.polishmediahub.app.data.iptv

import android.content.Context
import android.util.Log
import com.polishmediahub.app.BuildConfig
import com.polishmediahub.app.data.local.EpgDao
import com.polishmediahub.app.data.local.EpgEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import com.polishmediahub.app.data.local.EpgChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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

    fun observeForChannels(channelIds: List<String>, from: Long, to: Long): Flow<Map<String, List<EpgEntity>>> {
        return epgDao.observeForChannels(channelIds, from, to)
            .map { entries -> entries.groupBy { it.channelId } }
    }

    fun observeDistinctChannels(from: Long, to: Long): Flow<List<EpgChannel>> {
        return epgDao.observeDistinctChannels(from, to)
    }

    fun observeTimelineByChannels(
        channels: Flow<List<EpgChannel>>,
        from: Long,
        to: Long
    ): Flow<List<ChannelWithPrograms>> {
        return channels.flatMapLatest { channelList ->
            val ids = channelList.map { it.channelId }
            if (ids.isEmpty()) {
                flowOf(emptyList())
            } else {
                observeForChannels(ids, from, to).map { programsByChannel ->
                    channelList.map { ch ->
                        ChannelWithPrograms(
                            channelId = ch.channelId,
                            channelName = ch.channelName ?: ch.channelId,
                            programs = programsByChannel[ch.channelId] ?: emptyList()
                        )
                    }
                }
            }
        }
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
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w("EpgRepository", "loadEpg failed for $xmltvUrl: ${e.message}", e)
            throw e
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

data class ChannelWithPrograms(
    val channelId: String,
    val channelName: String,
    val programs: List<EpgEntity>
)
