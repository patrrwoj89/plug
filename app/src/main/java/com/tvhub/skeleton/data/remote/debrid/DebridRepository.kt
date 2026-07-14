package com.tvhub.skeleton.data.remote.debrid

import com.tvhub.skeleton.data.ApiConfigRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Debrid services (Real-Debrid, TorBox, AllDebrid, Premiumize, etc.) allow
 * users to cache/stream files hosted elsewhere. This repository exposes a generic
 * abstraction and uses OAuth device flow (QR code) or API key for authentication.
 */
interface DebridService {
    val provider: DebridProvider
    suspend fun isAvailable(): Boolean
    suspend fun getUserInfo(): DebridUserInfo
    suspend fun resolve(videoUrl: String): DebridStreamResult?
    suspend fun addMagnet(magnet: String): DebridTorrentResult
    suspend fun getTorrentFiles(id: String): List<DebridFile>
}

data class DebridUserInfo(val username: String, val premium: Boolean)
data class DebridStreamResult(val url: String, val name: String, val quality: String?)
data class DebridTorrentResult(val id: String, val uri: String, val status: String)
data class DebridFile(val id: String, val path: String, val bytes: Long, val url: String?)

class DebridRepository @Inject constructor(
    private val services: Set<@JvmSuppressWildcards DebridService>,
    private val apiConfigRepository: ApiConfigRepository
) {
    private val serviceMap = services.associateBy { it.provider }

    fun service(provider: DebridProvider): DebridService? = serviceMap[provider]

    suspend fun currentService(): DebridService? {
        val id = apiConfigRepository.debridProvider.first()
        val provider = DebridProvider.entries.find { it.id == id } ?: return null
        return serviceMap[provider]
    }
}
