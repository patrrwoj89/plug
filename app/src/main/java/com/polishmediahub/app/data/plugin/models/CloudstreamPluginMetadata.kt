package com.polishmediahub.app.data.plugin.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CloudstreamPluginMetadata(
    val name: String,
    val version: Int = 0,
    val url: String,
    @SerialName("fileSize") val fileSize: Long? = null,
    val authors: List<String>? = null,
    @SerialName("tvTypes") val tvTypes: List<String>? = null,
    val language: String? = null,
    @SerialName("iconUrl") val iconUrl: String? = null,
    val description: String? = null,
    @SerialName("internalName") val internalName: String? = null,
    @SerialName("isAdult") val isAdult: Boolean? = null
)
