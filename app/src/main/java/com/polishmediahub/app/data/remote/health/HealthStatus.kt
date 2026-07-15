package com.polishmediahub.app.data.remote.health

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HealthStatus(
    @SerialName("lastCheckAt")
    val lastCheckAt: Long = 0L,
    @SerialName("sources")
    val sources: List<SourceHealth> = emptyList()
)

@Serializable
data class SourceHealth(
    @SerialName("id")
    val id: String,
    @SerialName("label")
    val label: String,
    @SerialName("url")
    val url: String,
    @SerialName("status")
    val status: String,
    @SerialName("checkedAt")
    val checkedAt: Long = 0L,
    @SerialName("error")
    val error: String? = null
) {
    companion object {
        const val ONLINE = "ONLINE"
        const val OFFLINE = "OFFLINE"
        const val UNCONFIGURED = "UNCONFIGURED"
    }
}
