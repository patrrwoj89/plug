package com.tvhub.skeleton.data.plugin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PluginManifest(
    val id: String,
    val name: String,
    val version: String,
    val description: String? = null,
    val sources: List<PluginSource> = emptyList()
)

@Serializable
data class PluginSource(
    val type: String,
    val id: String,
    val name: String,
    val enabled: Boolean = true,
    val config: Map<String, String> = emptyMap()
)
