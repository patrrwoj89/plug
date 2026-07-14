package com.tvhub.skeleton.data.remote.debrid

import com.tvhub.skeleton.data.ApiConfigRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Debrid services (Real-Debrid, TorBox, AllDebrid, Premiumize, etc.) allow
 * users to cache/stream files hosted elsewhere. This repository exposes a generic
 * abstraction and uses OAuth device flow (QR code) for authentication.
 */
interface DebridService {
    suspend fun getUserInfo(apiKey: String): DebridUserInfo
    suspend fun unrestrictLink(apiKey: String, url: String): DebridStreamResult
    suspend fun addMagnet(apiKey: String, magnet: String): DebridTorrentResult
    suspend fun getTorrentFiles(apiKey: String, id: String): List<DebridFile>
}

data class DebridUserInfo(val username: String, val premium: Boolean)
data class DebridStreamResult(val url: String, val name: String, val quality: String?)
data class DebridTorrentResult(val id: String, val uri: String, val status: String)
data class DebridFile(val id: String, val path: String, val bytes: Long, val url: String?)

class DebridRepository @Inject constructor(
    private val realDebridService: RealDebridService,
    private val apiConfigRepository: ApiConfigRepository
) {
    /**
     * Returns the currently configured Debrid service if an access token is present.
     */
    fun service(): DebridService? = realDebridService

    suspend fun accessToken(): String = apiConfigRepository.debridAccessToken.first()
    suspend fun refreshToken(): String = apiConfigRepository.debridRefreshToken.first()
}
