package com.tvhub.skeleton.data.remote.iptv

import com.tvhub.skeleton.model.MediaItem

object M3UParser {

    fun parse(playlist: String): List<MediaItem> {
        val lines = playlist.lines()
        val channels = mutableListOf<MediaItem>()
        var currentExtInf: String? = null

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF:")) {
                currentExtInf = trimmed
            } else if (currentExtInf != null && trimmed.isNotBlank() && !trimmed.startsWith("#")) {
                val name = parseName(currentExtInf)
                val logo = parseAttribute(currentExtInf, "tvg-logo")
                val group = parseAttribute(currentExtInf, "group-title")
                channels.add(
                    MediaItem(
                        id = "iptv:${name}:${trimmed}",
                        title = name,
                        subtitle = group.orEmpty(),
                        description = "IPTV channel from M3U playlist",
                        posterUrl = logo,
                        videoUrl = trimmed,
                        genres = group?.let { listOf(it) } ?: emptyList(),
                        type = MediaItem.Type.CHANNEL
                    )
                )
                currentExtInf = null
            }
        }
        return channels
    }

    private fun parseName(extInf: String): String {
        val afterComma = extInf.substringAfterLast(',', "Unknown")
        return afterComma.trim()
    }

    private fun parseAttribute(extInf: String, key: String): String? {
        val regex = "$key=\"([^\"]*)\"".toRegex()
        return regex.find(extInf)?.groupValues?.get(1)
    }
}
