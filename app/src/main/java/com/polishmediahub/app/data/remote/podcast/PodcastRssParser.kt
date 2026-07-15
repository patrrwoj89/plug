package com.polishmediahub.app.data.remote.podcast

import android.util.Xml
import com.polishmediahub.app.model.AudioTrack
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

object PodcastRssParser {

    private const val ITUNES_NS = "http://www.itunes.com/dtds/podcast-1.0.dtd"

    fun parse(xml: String, sourceId: String = "podcast"): List<AudioTrack> {
        val parser = Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            setInput(StringReader(xml))
        }

        var channelTitle = "Podcast"
        var channelImage: String? = null
        val tracks = mutableListOf<AudioTrack>()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "channel") {
                val channel = parseChannel(parser)
                channelTitle = channel.title
                channelImage = channel.image
            } else if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                tracks += parseItem(parser, channelTitle, channelImage, sourceId)
            }
            eventType = parser.next()
        }
        return tracks
    }

    private data class ChannelInfo(val title: String, val image: String?)

    private fun parseChannel(parser: XmlPullParser): ChannelInfo {
        var title = "Podcast"
        var image: String? = null
        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && parser.name == "channel")) {
            if (eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "title" -> title = readText(parser).ifBlank { title }
                    "image" -> if (parser.namespace == ITUNES_NS) {
                        image = parser.getAttributeValue(null, "href") ?: image
                    }
                    "author" -> if (parser.namespace == ITUNES_NS) {
                        // author text ignored; not needed at channel level
                        readText(parser)
                    }
                }
            }
            eventType = parser.next()
        }
        return ChannelInfo(title, image)
    }

    private fun parseItem(
        parser: XmlPullParser,
        channelTitle: String,
        channelImage: String?,
        sourceId: String
    ): AudioTrack {
        var title = "Untitled"
        var guid = ""
        var description = ""
        var durationText = ""
        var itemImage: String? = null
        var author: String? = null
        var pubDate = ""
        var enclosureUrl: String? = null
        var enclosureType = ""

        var eventType = parser.next()
        while (!(eventType == XmlPullParser.END_TAG && parser.name == "item")) {
            if (eventType == XmlPullParser.START_TAG) {
                val name = parser.name
                val ns = parser.namespace
                when {
                    name == "title" && ns.isNullOrBlank() -> title = readText(parser)
                    name == "guid" && ns.isNullOrBlank() -> guid = readText(parser)
                    name == "description" && ns.isNullOrBlank() -> description = readText(parser)
                    name == "pubDate" && ns.isNullOrBlank() -> pubDate = readText(parser)
                    name == "duration" && ns == ITUNES_NS -> durationText = readText(parser)
                    name == "image" && ns == ITUNES_NS -> {
                        itemImage = parser.getAttributeValue(null, "href") ?: itemImage
                        consumeTag(parser)
                    }
                    name == "author" && ns == ITUNES_NS -> author = readText(parser)
                    name == "enclosure" && ns.isNullOrBlank() -> {
                        val url = parser.getAttributeValue(null, "url")
                        val type = parser.getAttributeValue(null, "type") ?: ""
                        if (url != null && (enclosureUrl == null || type.startsWith("audio"))) {
                            enclosureUrl = url
                            enclosureType = type
                        }
                        consumeTag(parser)
                    }
                    else -> consumeTag(parser)
                }
            }
            eventType = parser.next()
        }

        val id = if (guid.isNotBlank()) "podcast:$guid" else "podcast:${(title + enclosureUrl).hashCode()}"
        return AudioTrack(
            id = id,
            title = title,
            artist = author ?: channelTitle,
            album = channelTitle,
            coverUrl = itemImage ?: channelImage,
            streamUrl = enclosureUrl,
            durationMs = parseDuration(durationText),
            isLive = false,
            sourceId = sourceId,
            description = description
        )
    }

    private fun readText(parser: XmlPullParser): String {
        val result = StringBuilder()
        var eventType = parser.next()
        while (eventType != XmlPullParser.END_TAG) {
            if (eventType == XmlPullParser.TEXT || eventType == XmlPullParser.CDSECT) {
                parser.text?.let { result.append(it) }
            }
            eventType = parser.next()
        }
        return result.toString().trim()
    }

    private fun consumeTag(parser: XmlPullParser) {
        if (parser.isEmptyElementTag) return
        var eventType = parser.next()
        var depth = 1
        while (depth > 0 && eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) depth++
            else if (eventType == XmlPullParser.END_TAG) depth--
            eventType = parser.next()
        }
    }

    private fun parseDuration(duration: String): Long {
        val parts = duration.split(":", limit = 3).mapNotNull { it.toLongOrNull() }
        return when (parts.size) {
            3 -> ((parts[0] * 3600) + (parts[1] * 60) + parts[2]) * 1000
            2 -> ((parts[0] * 60) + parts[1]) * 1000
            1 -> parts[0] * 1000
            else -> 0L
        }
    }
}
