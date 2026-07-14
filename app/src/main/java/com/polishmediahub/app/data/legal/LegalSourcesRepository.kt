package com.polishmediahub.app.data.legal

import android.content.Context
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
        } catch (_: Exception) {
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
data class PolishSources(
    val note: String = ""
)
