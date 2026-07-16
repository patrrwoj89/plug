package com.polishmediahub.app.data.legal

import android.content.Context
import android.util.Log
import com.polishmediahub.app.BuildConfig
import com.polishmediahub.app.data.source.WebSourceConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LegalSourcesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val json = Json { ignoreUnknownKeys = true }

    fun load(): LegalSources? {
        return try {
            context.assets.open("legal_sources.json").bufferedReader().use {
                json.decodeFromString(LegalSources.serializer(), it.readText())
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w("LegalSourcesRepository", "Failed to load legal_sources.json: ${e.message}", e)
            null
        }
    }
}

@Serializable
data class LegalSources(
    val iptv: List<SourceEntry> = emptyList(),
    val epg: List<SourceEntry> = emptyList(),
    @SerialName("torrentExamples") val torrentExamples: List<SourceEntry> = emptyList(),
    @SerialName("stremioAddons") val stremioAddons: List<SourceEntry> = emptyList(),
    val music: List<SourceEntry> = emptyList(),
    val podcastFeeds: List<SourceEntry> = emptyList(),
    @SerialName("deezerProxy") val deezerProxy: SourceEntry? = null,
    val webSources: List<WebSourceConfig> = emptyList(),
    @SerialName("mdbList") val mdbList: MdbListStarter? = null,
    @SerialName("kitsu") val kitsu: SourceEntry? = null,
    val polish: PolishSources? = null
)

@Serializable
data class SourceEntry(
    val name: String,
    val url: String = "",
    val magnet: String = "",
    val license: String = ""
)

@Serializable
data class MdbListStarter(
    @SerialName("apiKeyUrl") val apiKeyUrl: String = "",
    @SerialName("starterLists") val starterLists: List<MdbListStarterEntry> = emptyList()
)

@Serializable
data class MdbListStarterEntry(
    val id: Int,
    val name: String = "",
    val description: String = ""
)

@Serializable
data class PolishSources(
    val note: String = ""
)
