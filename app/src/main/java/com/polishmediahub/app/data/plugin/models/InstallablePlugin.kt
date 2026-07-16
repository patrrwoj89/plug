package com.polishmediahub.app.data.plugin.models

sealed class InstallablePlugin {
    abstract val id: String
    abstract val name: String
    abstract val url: String
    abstract val iconUrl: String?
    abstract val description: String?
    abstract val version: String
    abstract val fileSize: Long?

    data class Cloudstream(
        override val id: String,
        override val name: String,
        override val url: String,
        override val iconUrl: String? = null,
        override val description: String? = null,
        override val version: String = "",
        override val fileSize: Long? = null,
        val language: String? = null,
        val tvTypes: List<String> = emptyList(),
        val authors: List<String> = emptyList()
    ) : InstallablePlugin()

    data class Aniyomi(
        override val id: String,
        override val name: String,
        override val url: String,
        override val iconUrl: String? = null,
        override val description: String? = null,
        override val version: String = "",
        override val fileSize: Long? = null,
        val pkg: String = "",
        val lang: String = "en",
        val nsfw: Boolean = false,
        val mainClass: String = ""
    ) : InstallablePlugin()
}
