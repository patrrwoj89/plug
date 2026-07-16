package com.polishmediahub.app.data.source

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.polishmediahub.app.BuildConfig
import com.polishmediahub.app.data.ApiConfigRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KodiDiscoveryManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val apiConfigRepository: ApiConfigRepository,
    private val kodiMediaSource: KodiMediaSource
) {

    private val nsdManager: NsdManager? = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    fun startDiscovery() {
        if (nsdManager == null) {
            if (BuildConfig.DEBUG) Log.w("KodiDiscoveryManager", "NsdManager not available")
            return
        }
        stopDiscovery()
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                if (BuildConfig.DEBUG) Log.d("KodiDiscoveryManager", "Discovery started: $regType")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        if (BuildConfig.DEBUG) Log.w("KodiDiscoveryManager", "Resolve failed: $errorCode")
                    }

                    override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                        val host = serviceInfo.host?.hostAddress ?: return
                        val port = serviceInfo.port
                        val url = "http://$host:${port}"
                        if (BuildConfig.DEBUG) Log.i("KodiDiscoveryManager", "Kodi resolved at $url")
                        scope.launch(Dispatchers.IO) {
                            try {
                                apiConfigRepository.setKodiUrl(url)
                                kodiMediaSource.configure(url)
                            } catch (e: Exception) {
                                if (BuildConfig.DEBUG) Log.w("KodiDiscoveryManager", "Failed to store Kodi URL: ${e.message}")
                            }
                        }
                    }
                })
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                if (BuildConfig.DEBUG) Log.w("KodiDiscoveryManager", "Discovery start failed: $errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                if (BuildConfig.DEBUG) Log.w("KodiDiscoveryManager", "Discovery stop failed: $errorCode")
            }
        }
        discoveryListener = listener
        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w("KodiDiscoveryManager", "discoverServices failed: ${e.message}")
        }
    }

    fun stopDiscovery() {
        discoveryListener?.let { listener ->
            try {
                nsdManager?.stopServiceDiscovery(listener)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.w("KodiDiscoveryManager", "stopServiceDiscovery failed: ${e.message}")
            }
        }
        discoveryListener = null
    }

    companion object {
        private const val SERVICE_TYPE = "_xbmc-jsonrpc._tcp"
    }
}
