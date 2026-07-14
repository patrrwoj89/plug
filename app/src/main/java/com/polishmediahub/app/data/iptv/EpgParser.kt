package com.polishmediahub.app.data.iptv

import com.polishmediahub.app.data.local.EpgEntity
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

object EpgParser {

    private val dateFormat = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)

    fun parse(input: InputStream): List<EpgEntity> {
        val entries = mutableListOf<EpgEntity>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(input, null)

        var channelId: String? = null
        var title = ""
        var desc = ""
        var start = 0L
        var end = 0L
        var icon: String? = null

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "programme" -> {
                            channelId = parser.getAttributeValue(null, "channel")
                            start = parseTime(parser.getAttributeValue(null, "start"))
                            end = parseTime(parser.getAttributeValue(null, "stop"))
                            title = ""
                            desc = ""
                            icon = null
                        }
                        "title" -> title = parser.nextText()
                        "desc" -> desc = parser.nextText()
                        "icon" -> icon = parser.getAttributeValue(null, "src")
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "programme" && channelId != null) {
                        entries.add(
                            EpgEntity(
                                id = UUID.nameUUIDFromBytes("$channelId$start$end".toByteArray()).toString(),
                                channelId = channelId,
                                title = title,
                                description = desc,
                                startTime = start,
                                endTime = end,
                                iconUrl = icon
                            )
                        )
                        channelId = null
                    }
                }
            }
            eventType = parser.next()
        }
        return entries
    }

    private fun parseTime(raw: String?): Long {
        return try {
            dateFormat.parse(raw ?: "")?.time ?: 0L
        } catch (_: Exception) {
            0L
        }
    }
}
