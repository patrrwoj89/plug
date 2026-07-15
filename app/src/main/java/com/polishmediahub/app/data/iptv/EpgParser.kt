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
        val channelNames = mutableMapOf<String, String>()
        val channelIcons = mutableMapOf<String, String>()
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(input, null)

        var currentChannelId: String? = null
        var programmeChannelId: String? = null
        var title = ""
        var desc = ""
        var year = ""
        var category = ""
        var start = 0L
        var end = 0L
        var icon: String? = null

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "channel" -> {
                            currentChannelId = parser.getAttributeValue(null, "id")
                            channelNames[currentChannelId ?: ""] = ""
                            channelIcons[currentChannelId ?: ""] = ""
                        }
                        "display-name" -> {
                            val text = parser.nextText()
                            if (!currentChannelId.isNullOrBlank() && text.isNotBlank()) {
                                channelNames[currentChannelId] = text.trim()
                            }
                        }
                        "programme" -> {
                            programmeChannelId = parser.getAttributeValue(null, "channel")
                            start = parseTime(parser.getAttributeValue(null, "start"))
                            end = parseTime(parser.getAttributeValue(null, "stop"))
                            title = ""
                            desc = ""
                            year = ""
                            category = ""
                            icon = null
                        }
                        "title" -> title = parser.nextText()
                        "desc" -> desc = parser.nextText()
                        "date" -> year = parser.nextText()
                        "category" -> if (category.isBlank()) category = parser.nextText()
                        "icon" -> {
                            val src = parser.getAttributeValue(null, "src")
                            if (!currentChannelId.isNullOrBlank()) {
                                channelIcons[currentChannelId] = src ?: ""
                            }
                            if (!programmeChannelId.isNullOrBlank()) {
                                icon = src
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "channel" -> currentChannelId = null
                        "programme" -> {
                            val chId = programmeChannelId ?: continue
                            entries.add(
                                EpgEntity(
                                    id = UUID.nameUUIDFromBytes("$chId$start$end".toByteArray()).toString(),
                                    channelId = chId,
                                    channelName = channelNames[chId]?.takeIf { it.isNotBlank() },
                                    title = title,
                                    description = desc,
                                    year = year.takeIf { it.isNotBlank() },
                                    category = category.takeIf { it.isNotBlank() },
                                    startTime = start,
                                    endTime = end,
                                    iconUrl = icon ?: channelIcons[chId]?.takeIf { it.isNotBlank() }
                                )
                            )
                            programmeChannelId = null
                        }
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
