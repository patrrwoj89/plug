package com.polishmediahub.app.data.plugin.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AniyomiExtension(
    val name: String,
    val pkg: String,
    val apk: String,
    val lang: String = "en",
    val code: Long = 0,
    val version: String = "",
    val nsfw: Int = 0,
    val sources: List<AniyomiExtensionSource> = emptyList()
)

@Serializable
data class AniyomiExtensionSource(
    val name: String,
    val lang: String = "",
    val id: String = "",
    @SerialName("baseUrl") val baseUrl: String = ""
)
