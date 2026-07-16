package com.polishmediahub.app.data.remote.debrid

import com.polishmediahub.app.model.MediaItem
import kotlinx.serialization.Serializable

@Serializable
 data class StreamRules(
    val enabled: Boolean = false,
    val sizeMinMb: Int = 0,
    val sizeMaxMb: Int = 51_200,
    val resolutions: Set<String> = emptySet(),
    val requiredVideoTags: Set<String> = emptySet(),
    val preferredVideoTags: Set<String> = emptySet(),
    val excludedVideoTags: Set<String> = emptySet(),
    val requiredAudioTags: Set<String> = emptySet(),
    val preferredAudioTags: Set<String> = emptySet(),
    val excludedAudioTags: Set<String> = emptySet(),
    val requiredEncoders: Set<String> = emptySet(),
    val preferredEncoders: Set<String> = emptySet(),
    val excludedEncoders: Set<String> = emptySet(),
    val maxResults: Int = 0
)

object StreamRulesEngine {

    private val sizeRegex = Regex("""(\d+(?:[.,]\d+)?)\s*(GB|MB|GiB|MiB|gb|mb|gib|mib)""")
    private val resolutionRegex = Regex("""\b(4k|uhd|2160p|1080p|1080i|720p|720i|480p)\b""")
    private val videoTagRegex = Regex("""\b(hdr10\+|hdr10|hdr|dolby\s*vision|dv|dovi|sdr)\b""")
    private val audioTagRegex = Regex("""\b(atmos|dts[-:]?x|dts[-]?hd|dts|truehd|ddp5\.1|dd\+|eac3|ac3|aac)\b""")
    private val encoderRegex = Regex("""\b(hevc|h\.265|x265|h265|av1|h\.264|x264|h264|mpeg2|vp9)\b""")

    fun apply(items: List<MediaItem>, rules: StreamRules?): List<MediaItem> {
        if (rules == null || !rules.enabled || items.isEmpty()) return items

        val scored = items.map { item ->
            val meta = extractMetadata(item)
            ScoredItem(item, meta, score(item, meta, rules))
        }

        val filtered = scored.filter { it.score >= 0 && passesRules(it.item, it.meta, rules) }

        val sorted = filtered.sortedWith(
            compareByDescending<ScoredItem> { resolutionRank(it.meta.resolutions) }
                .thenByDescending { it.score }
                .thenByDescending { it.meta.sizeMb ?: 0 }
        )

        return if (rules.maxResults > 0) sorted.take(rules.maxResults).map { it.item } else sorted.map { it.item }
    }

    private fun score(item: MediaItem, meta: StreamMetadata, rules: StreamRules): Int {
        var score = 0
        if (rules.preferredVideoTags.isNotEmpty()) {
            score += meta.videoTags.count { it in rules.preferredVideoTags }
        }
        if (rules.preferredAudioTags.isNotEmpty()) {
            score += meta.audioTags.count { it in rules.preferredAudioTags }
        }
        if (rules.preferredEncoders.isNotEmpty()) {
            score += meta.encoders.count { it in rules.preferredEncoders }
        }
        return score
    }

    private fun passesRules(item: MediaItem, meta: StreamMetadata, rules: StreamRules): Boolean {
        if (meta.sizeMb != null) {
            val minOk = rules.sizeMinMb <= 0 || meta.sizeMb >= rules.sizeMinMb
            val maxOk = rules.sizeMaxMb <= 0 || meta.sizeMb <= rules.sizeMaxMb
            if (!minOk || !maxOk) return false
        }

        if (rules.resolutions.isNotEmpty() && meta.resolutions.none { it in rules.resolutions }) {
            return false
        }

        if (rules.requiredVideoTags.isNotEmpty() && !meta.videoTags.containsAll(rules.requiredVideoTags)) return false
        if (rules.requiredAudioTags.isNotEmpty() && !meta.audioTags.containsAll(rules.requiredAudioTags)) return false
        if (rules.requiredEncoders.isNotEmpty() && !meta.encoders.containsAll(rules.requiredEncoders)) return false

        if (meta.videoTags.any { it in rules.excludedVideoTags }) return false
        if (meta.audioTags.any { it in rules.excludedAudioTags }) return false
        if (meta.encoders.any { it in rules.excludedEncoders }) return false

        return true
    }

    private fun extractMetadata(item: MediaItem): StreamMetadata {
        val text = buildString {
            append(item.title)
            if (item.subtitle.isNotBlank()) append(" ").append(item.subtitle)
            if (item.description.isNotBlank()) append(" ").append(item.description)
        }.lowercase()

        val sizeMb = sizeRegex.findAll(text).mapNotNull { match ->
            val value = match.groupValues[1].replace(',', '.').toFloatOrNull() ?: return@mapNotNull null
            val unit = match.groupValues[2].lowercase()
            when {
                unit.startsWith("gb") || unit.startsWith("gib") -> (value * 1024).toInt()
                unit.startsWith("mb") || unit.startsWith("mib") -> value.toInt()
                else -> null
            }
        }.firstOrNull()

        val resolutions = resolutionRegex.findAll(text).map { canonicalResolution(it.groupValues[1]) }.toSet()
        val videoTags = videoTagRegex.findAll(text).map { canonicalVideoTag(it.groupValues[1]) }.toSet()
        val audioTags = audioTagRegex.findAll(text).map { canonicalAudioTag(it.groupValues[1]) }.toSet()
        val encoders = encoderRegex.findAll(text).map { canonicalEncoder(it.groupValues[1]) }.toSet()

        return StreamMetadata(sizeMb, resolutions, videoTags, audioTags, encoders)
    }

    private fun canonicalResolution(token: String): String = when (token.lowercase()) {
        "4k", "uhd", "2160p" -> "4K"
        "1080p", "1080i" -> "1080p"
        "720p", "720i" -> "720p"
        "480p" -> "480p"
        else -> token
    }

    private fun canonicalVideoTag(token: String): String = when (token.lowercase().replace(" ", "")) {
        "hdr10+", "hdr10plus" -> "HDR10+"
        "hdr10" -> "HDR10"
        "hdr" -> "HDR"
        "dolbyvision", "dv", "dovi" -> "DolbyVision"
        "sdr" -> "SDR"
        else -> token
    }

    private fun canonicalAudioTag(token: String): String = when (token.lowercase().replace(Regex("[-:]", RegexOption.IGNORE_CASE), "")) {
        "atmos" -> "Atmos"
        "dtsx", "dts:x" -> "DTS:X"
        "dtshd", "dts-hd" -> "DTS-HD"
        "dts" -> "DTS"
        "truehd" -> "TrueHD"
        "ddp51", "ddp5.1", "dd+" -> "DD+"
        "eac3" -> "E-AC3"
        "ac3" -> "AC3"
        "aac" -> "AAC"
        else -> token
    }

    private fun canonicalEncoder(token: String): String = when (token.lowercase().replace(".", "")) {
        "hevc", "h265", "x265" -> "HEVC"
        "av1" -> "AV1"
        "h264", "x264" -> "H264"
        "mpeg2" -> "MPEG2"
        "vp9" -> "VP9"
        else -> token.uppercase()
    }

    private fun resolutionRank(resolutions: Set<String>): Int {
        return resolutions.maxOfOrNull {
            when (it) {
                "4K" -> 4
                "1080p" -> 3
                "720p" -> 2
                "480p" -> 1
                else -> 0
            }
        } ?: 0
    }

    private data class StreamMetadata(
        val sizeMb: Int?,
        val resolutions: Set<String>,
        val videoTags: Set<String>,
        val audioTags: Set<String>,
        val encoders: Set<String>
    )

    private data class ScoredItem(
        val item: MediaItem,
        val meta: StreamMetadata,
        val score: Int
    )
}
