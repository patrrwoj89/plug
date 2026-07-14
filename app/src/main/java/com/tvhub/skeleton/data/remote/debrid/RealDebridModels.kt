package com.tvhub.skeleton.data.remote.debrid

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RealDebridUser(
    val id: Long,
    val email: String,
    val username: String,
    val points: Long,
    val locale: String,
    val avatar: String? = null,
    val type: String,
    val premium: Long,
    val expiration: String? = null
)

@Serializable
data class RealDebridUnrestrictedLink(
    val id: String,
    val filename: String,
    @SerialName("mimeType") val mimeType: String? = null,
    @SerialName("filesize") val filesize: Long = 0,
    val link: String,
    val host: String,
    val chunks: Int = 0,
    val crc: Int = 0,
    val download: String,
    val streamable: Int = 0
)

@Serializable
data class RealDebridTorrent(
    val id: String,
    val filename: String,
    val hash: String,
    val bytes: Long = 0,
    @SerialName("original_bytes") val originalBytes: Long = 0,
    val host: String? = null,
    val split: Int = 0,
    val progress: Double = 0.0,
    val status: String,
    val seeded: Int = 0,
    val speed: Long = 0,
    @SerialName("seeders") val seeders: Int = 0,
    val links: List<String> = emptyList(),
    val files: List<RealDebridTorrentFile>? = null,
    @SerialName("ended") val ended: String? = null
)

@Serializable
data class RealDebridTorrentFile(
    val id: Int,
    val path: String,
    val bytes: Long,
    val selected: Int = 0
)
