package com.polishmediahub.app.data.admin

import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkAddressHelper {

    fun getLocalIpAddress(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces()
                ?.toList()
                ?.filter { it.isUp && !it.isLoopback && !it.isVirtual }
                ?.flatMap { it.inetAddresses.toList() }
                ?.filterIsInstance<Inet4Address>()
                ?.firstOrNull { !it.isLoopbackAddress }
                ?.hostAddress
        } catch (_: Exception) {
            null
        }
    }
}
