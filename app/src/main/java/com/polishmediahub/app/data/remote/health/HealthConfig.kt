package com.polishmediahub.app.data.remote.health

data class HealthConfig(
    val kodiUrl: String = "",
    val iptvUrls: List<String> = emptyList(),
    val epgUrl: String = "",
    val stremioAddons: List<String> = emptyList(),
    val cloudstreamRepos: List<String> = emptyList(),
    val webSourceConfig: String = "",
    val jellyfinUrl: String = "",
    val jellyfinToken: String = "",
    val plexUrl: String = "",
    val plexToken: String = "",
    val embyUrl: String = "",
    val embyToken: String = "",
    val subsonicUrl: String = "",
    val subsonicUser: String = "",
    val subsonicPassword: String = "",
    val podcastFeeds: List<String> = emptyList(),
    val deezerProxyUrl: String = ""
)
