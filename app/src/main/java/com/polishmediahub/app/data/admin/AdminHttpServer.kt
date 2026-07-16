package com.polishmediahub.app.data.admin

import android.content.Context
import android.util.Log
import com.polishmediahub.app.BuildConfig
import com.polishmediahub.app.data.ApiConfigRepository
import com.polishmediahub.app.data.SettingsRepository
import com.polishmediahub.app.data.plugin.PluginRepository
import com.polishmediahub.app.data.plugin.PluginUpdateWorker
import com.polishmediahub.app.data.remote.cloud.CloudProfileSyncRestore
import com.polishmediahub.app.data.remote.cloud.CloudProfileSyncWorker
import com.polishmediahub.app.data.remote.trakt.TraktSyncWorker
import com.polishmediahub.app.data.source.KodiMediaSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import okhttp3.OkHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminHttpServer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val apiConfigRepository: ApiConfigRepository,
    private val settingsRepository: SettingsRepository,
    private val pluginRepository: PluginRepository,
    private val kodiMediaSource: KodiMediaSource,
    private val okHttpClient: OkHttpClient
) {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val sandboxEngine = SandboxEngine(kodiMediaSource, okHttpClient, json)

    private var serverSocket: ServerSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var port: Int = 0
        private set

    private var pairingToken: String = ""

    fun start(): Int = synchronized(this) {
        serverSocket?.let { return port }
        val socket = ServerSocket(0)
        port = socket.localPort
        if (pairingToken.isBlank()) {
            pairingToken = UUID.randomUUID().toString()
        }
        serverSocket = socket
        scope.launch {
            while (!socket.isClosed) {
                try {
                    val client = socket.accept()
                    scope.launch { handleClient(client) }
                } catch (e: Exception) {
                    if (!socket.isClosed && BuildConfig.DEBUG) Log.w("AdminHttpServer", "accept error: ${e.message}")
                }
            }
        }
        port
    }

    fun stop() = synchronized(this) {
        try {
            serverSocket?.close()
            scope.cancel()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w("AdminHttpServer", "stop failed: ${e.message}", e)
        }
        serverSocket = null
        port = 0
    }

    fun adminUrl(ip: String): String = "http://$ip:$port/admin?token=$pairingToken"

    private fun handleClient(client: Socket) {
        try {
            client.use { socket ->
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val requestLine = reader.readLine() ?: return
                val parts = requestLine.split(" ")
                if (parts.size < 2) return
                val method = parts[0].uppercase(Locale.getDefault())
                val pathWithQuery = parts[1]
                val path = pathWithQuery.substringBefore('?')

                val headers = mutableMapOf<String, String>()
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.isEmpty()) break
                    val colon = line.indexOf(':')
                    if (colon > 0) {
                        headers[line.substring(0, colon).trim().lowercase(Locale.getDefault())] =
                            line.substring(colon + 1).trim()
                    }
                }

                val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
                val body = if (contentLength > 0) {
                    val bodyBuilder = StringBuilder()
                    var remaining = contentLength
                    while (remaining > 0) {
                        val c = reader.read()
                        if (c == -1) break
                        bodyBuilder.append(c.toChar())
                        remaining--
                    }
                    bodyBuilder.toString()
                } else ""

                val out = socket.getOutputStream()
                val clientIp = socket.inetAddress?.hostAddress ?: ""
                val allowedOrigin = if (clientIp.isNotBlank()) "http://$clientIp:$port" else null
                val originHeader = headers["origin"]
                val query = parseQuery(pathWithQuery)
                val token = query["token"]
                if (path.startsWith("/api/")) {
                    if (token != pairingToken) {
                        writeResponse(out, 403, "Forbidden", "text/plain", "Forbidden", allowedOrigin)
                        return@use
                    }
                    if (allowedOrigin != null && originHeader != null && originHeader != allowedOrigin) {
                        writeResponse(out, 403, "Forbidden", "text/plain", "Forbidden", allowedOrigin)
                        return@use
                    }
                }
                when {
                    method == "GET" && path == "/admin" -> serveAdminPage(out, allowedOrigin)
                    method == "POST" && path == "/api/config" -> handleConfigPost(body, out, allowedOrigin)
                    method == "POST" && path == "/api/plugin" -> handlePluginPost(body, out, allowedOrigin)
                    method == "POST" && path == "/api/plugin/test" -> handlePluginTest(query, body, out, allowedOrigin)
                    method == "POST" && path == "/api/plugin/update" -> handlePluginUpdate(out, allowedOrigin)
                    method == "POST" && path == "/api/profile/sync" -> handleProfileSync(out, allowedOrigin)
                    method == "POST" && path == "/api/profile/restore" -> handleProfileRestore(out, allowedOrigin)
                    method == "POST" && path == "/api/trakt/sync" -> handleTraktSync(out, allowedOrigin)
                    method == "GET" && path == "/api/config" -> serveConfig(out, allowedOrigin)
                    method == "GET" && path == "/api/health" -> serveHealth(out, allowedOrigin)
                    else -> writeResponse(out, 404, "Not Found", "text/plain", "Not Found", allowedOrigin)
                }
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w("AdminHttpServer", "client error: ${e.message}")
        }
    }

    private fun serveAdminPage(out: java.io.OutputStream, corsOrigin: String?) {
        val html = ADMIN_HTML
        writeResponse(out, 200, "OK", "text/html; charset=utf-8", html, corsOrigin)
    }

    private fun serveConfig(out: java.io.OutputStream, corsOrigin: String?) {
        runBlocking(Dispatchers.IO) {
            val values = apiConfigRepository.run {
                mapOf(
                    "tmdbApiKey" to tmdbApiKey,
                    "aniListToken" to aniListToken,
                    "traktClientId" to traktClientId,
                    "traktClientSecret" to traktClientSecret,
                    "traktRefreshToken" to traktRefreshToken,
                    "traktAccessToken" to traktAccessToken,
                    "debridApiKey" to debridApiKey,
                    "debridProvider" to debridProvider,
                    "iptvSourceUrls" to iptvSourceUrls,
                    "epgUrl" to epgUrl,
                    "stremioAddons" to stremioAddons,
                    "kodiUrl" to kodiUrl,
                    "webSourceConfig" to webSourceConfig,
                    "cloudstreamRepoUrls" to cloudstreamRepoUrls,
                    "jellyfinUrl" to jellyfinUrl,
                    "jellyfinToken" to jellyfinToken,
                    "plexUrl" to plexUrl,
                    "plexToken" to plexToken,
                    "embyUrl" to embyUrl,
                    "embyToken" to embyToken,
                    "forceTranscode" to forceTranscode,
                    "maxDirectPlayBitrate" to maxDirectPlayBitrate,
                    "subsonicUrl" to subsonicUrl,
                    "subsonicUser" to subsonicUser,
                    "subsonicPassword" to subsonicPassword,
                    "podcastFeeds" to podcastFeeds,
                    "deezerProxyUrl" to deezerProxyUrl,
                    "mdbListApiKey" to mdbListApiKey,
                    "lastEpgSyncAt" to lastEpgSyncAt,
                    "lastEpgSyncStatus" to lastEpgSyncStatus,
                    "lastEpgSyncError" to lastEpgSyncError.map { it ?: "" },
                    "lastTraktSyncAt" to lastTraktSyncAt,
                    "lastTraktSyncStatus" to lastTraktSyncStatus,
                    "lastTraktSyncError" to lastTraktSyncError.map { it ?: "" },
                    "useCloudflareBypass" to useCloudflareBypass,
                    "cloudflareWorkerUrl" to cloudflareWorkerUrl,
                    "cloudflareAuthToken" to cloudflareAuthToken,
                    "homeAssistantUrl" to homeAssistantUrl,
                    "homeAssistantToken" to homeAssistantToken,
                    "homeAssistantWebhookEnabled" to homeAssistantWebhookEnabled,
                    "lastProfileSyncAt" to lastProfileSyncAt,
                    "lastProfileSyncStatus" to lastProfileSyncStatus,
                    "lastProfileSyncError" to lastProfileSyncError.map { it ?: "" },
                    "lastPluginUpdateAt" to lastPluginUpdateAt,
                    "pluginUpdateCount" to pluginUpdateCount
                ) + with(settingsRepository) {
                    mapOf(
                        "autoSkipIntro" to autoSkipIntro,
                        "introEndSeconds" to defaultIntroEndSeconds,
                        "outroDurationSeconds" to defaultOutroDurationSeconds,
                        "useAlternativePlayer" to useAlternativePlayer,
                        "preferredAudioType" to preferredAudioType,
                        "nightModeEnabled" to nightModeEnabled,
                        "dialogueBoostGainmB" to dialogueBoostGainmB,
                        "amoledMode" to amoledMode,
                        "pureBlackSurfaces" to pureBlackSurfaces,
                        "tunneledPlaybackEnabled" to tunneledPlaybackEnabled,
                        "exoplayerParallelConnections" to exoplayerParallelConnections,
                        "exoplayerMinBufferMs" to exoplayerMinBufferMs,
                        "exoplayerMaxBufferMs" to exoplayerMaxBufferMs,
                        "exoplayerBufferForPlaybackMs" to exoplayerBufferForPlaybackMs,
                        "exoplayerBufferForPlaybackAfterRebufferMs" to exoplayerBufferForPlaybackAfterRebufferMs,
                        "exoplayerBackBufferMs" to exoplayerBackBufferMs,
                        "exoplayerInitialAllocationCount" to exoplayerInitialAllocationCount,
                        "exoplayerTargetBufferBytes" to exoplayerTargetBufferBytes,
                        "streamRules" to streamRules,
                        "bingeGroupingEnabled" to bingeGroupingEnabled
                    )
                }
            }.mapValues { (_, flow) -> flow.first().toString() }
            val maskedValues = values.mapValues { (key, value) ->
                if (key in SENSITIVE_KEYS) maskSecret(value) else value
            }
            val jsonObj = JsonObject(maskedValues.mapValues { (_, v) -> JsonPrimitive(v) })
            val json = Json.encodeToString(JsonObject.serializer(), jsonObj)
            writeResponse(out, 200, "OK", "application/json", json, corsOrigin)
        }
    }

    private fun serveHealth(out: java.io.OutputStream, corsOrigin: String?) {
        runBlocking(Dispatchers.IO) {
            val raw = apiConfigRepository.healthStatuses.first()
            val body = raw.ifBlank { "{\"lastCheckAt\":0,\"sources\":[]}" }
            writeResponse(out, 200, "OK", "application/json", body, corsOrigin)
        }
    }

    private fun handleConfigPost(body: String, out: java.io.OutputStream, corsOrigin: String?) {
        val params = parseForm(body)
        runBlocking(Dispatchers.IO) {
            params["tmdbApiKey"]?.let { apiConfigRepository.setTmdbApiKey(it) }
            params["aniListToken"]?.let { apiConfigRepository.setAniListToken(it) }
            params["traktClientId"]?.let { apiConfigRepository.setTraktClientId(it) }
            params["traktClientSecret"]?.let { apiConfigRepository.setTraktClientSecret(it) }
            params["debridApiKey"]?.let { apiConfigRepository.setDebridApiKey(it) }
            params["debridProvider"]?.let { apiConfigRepository.setDebridProvider(it) }
            params["iptvSourceUrls"]?.let { apiConfigRepository.setIptvSourceUrls(it) }
            params["epgUrl"]?.let { apiConfigRepository.setEpgUrl(it) }
            params["stremioAddons"]?.let { apiConfigRepository.setStremioAddons(it) }
            params["kodiUrl"]?.let { apiConfigRepository.setKodiUrl(it) }
            pushAddonSettingsIfKodiConfigured()
            params["webSourceConfig"]?.let { apiConfigRepository.setWebSourceConfig(it) }
            params["cloudstreamRepoUrls"]?.let { apiConfigRepository.setCloudstreamRepoUrls(it) }
            params["jellyfinUrl"]?.let { apiConfigRepository.setJellyfinUrl(it) }
            params["jellyfinToken"]?.let { apiConfigRepository.setJellyfinToken(it) }
            params["plexUrl"]?.let { apiConfigRepository.setPlexUrl(it) }
            params["plexToken"]?.let { apiConfigRepository.setPlexToken(it) }
            params["embyUrl"]?.let { apiConfigRepository.setEmbyUrl(it) }
            params["embyToken"]?.let { apiConfigRepository.setEmbyToken(it) }
            params["forceTranscode"]?.toBooleanStrictOrNull()?.let { apiConfigRepository.setForceTranscode(it) }
            params["maxDirectPlayBitrate"]?.let { apiConfigRepository.setMaxDirectPlayBitrate(it) }
            params["subsonicUrl"]?.let { apiConfigRepository.setSubsonicUrl(it) }
            params["subsonicUser"]?.let { apiConfigRepository.setSubsonicUser(it) }
            params["subsonicPassword"]?.let { apiConfigRepository.setSubsonicPassword(it) }
            params["podcastFeeds"]?.let { apiConfigRepository.setPodcastFeeds(it) }
            params["deezerProxyUrl"]?.let { apiConfigRepository.setDeezerProxyUrl(it) }
            params["mdbListApiKey"]?.let { apiConfigRepository.setMdbListApiKey(it) }
            params["traktAccessToken"]?.let { apiConfigRepository.setTraktAccessToken(it) }
            params["autoSkipIntro"]?.toBooleanStrictOrNull()?.let { settingsRepository.setAutoSkipIntro(it) }
            params["introEndSeconds"]?.toIntOrNull()?.let { settingsRepository.setDefaultIntroEndSeconds(it.coerceIn(1, 600)) }
            params["outroDurationSeconds"]?.toIntOrNull()?.let { settingsRepository.setDefaultOutroDurationSeconds(it.coerceIn(1, 600)) }
            params["useAlternativePlayer"]?.toBooleanStrictOrNull()?.let { settingsRepository.setUseAlternativePlayer(it) }
            params["preferredAudioType"]?.let { settingsRepository.setPreferredAudioType(it) }
            params["nightModeEnabled"]?.toBooleanStrictOrNull()?.let { settingsRepository.setNightModeEnabled(it) }
            params["dialogueBoostGainmB"]?.toIntOrNull()?.let { settingsRepository.setDialogueBoostGainmB(it.coerceIn(0, 3000)) }
            params["amoledMode"]?.toBooleanStrictOrNull()?.let { settingsRepository.setAmoledMode(it) }
            params["pureBlackSurfaces"]?.toBooleanStrictOrNull()?.let { settingsRepository.setPureBlackSurfaces(it) }
            params["tunneledPlaybackEnabled"]?.toBooleanStrictOrNull()?.let { settingsRepository.setTunneledPlaybackEnabled(it) }
            params["exoplayerParallelConnections"]?.toIntOrNull()?.let { settingsRepository.setExoplayerParallelConnections(it) }
            params["exoplayerMinBufferMs"]?.toIntOrNull()?.let { settingsRepository.setExoplayerMinBufferMs(it) }
            params["exoplayerMaxBufferMs"]?.toIntOrNull()?.let { settingsRepository.setExoplayerMaxBufferMs(it) }
            params["exoplayerBufferForPlaybackMs"]?.toIntOrNull()?.let { settingsRepository.setExoplayerBufferForPlaybackMs(it) }
            params["exoplayerBufferForPlaybackAfterRebufferMs"]?.toIntOrNull()?.let { settingsRepository.setExoplayerBufferForPlaybackAfterRebufferMs(it) }
            params["exoplayerBackBufferMs"]?.toIntOrNull()?.let { settingsRepository.setExoplayerBackBufferMs(it) }
            params["exoplayerInitialAllocationCount"]?.toIntOrNull()?.let { settingsRepository.setExoplayerInitialAllocationCount(it) }
            params["exoplayerTargetBufferBytes"]?.toIntOrNull()?.let { settingsRepository.setExoplayerTargetBufferBytes(it) }
            params["streamRules"]?.let { settingsRepository.setStreamRules(it) }
            params["bingeGroupingEnabled"]?.toBooleanStrictOrNull()?.let { settingsRepository.setBingeGroupingEnabled(it) }
            params["useCloudflareBypass"]?.toBooleanStrictOrNull()?.let { apiConfigRepository.setUseCloudflareBypass(it) }
            params["cloudflareWorkerUrl"]?.let { apiConfigRepository.setCloudflareWorkerUrl(it) }
            params["cloudflareAuthToken"]?.let { apiConfigRepository.setCloudflareAuthToken(it) }
            params["homeAssistantUrl"]?.let { apiConfigRepository.setHomeAssistantUrl(it) }
            params["homeAssistantToken"]?.let { apiConfigRepository.setHomeAssistantToken(it) }
            params["homeAssistantWebhookEnabled"]?.toBooleanStrictOrNull()?.let { apiConfigRepository.setHomeAssistantWebhookEnabled(it) }
            pushAddonSettingsIfKodiConfigured()
        }
        writeResponse(out, 200, "OK", "text/plain", "OK", corsOrigin)
    }

    private suspend fun pushAddonSettingsIfKodiConfigured() {
        try {
            val kodiUrl = apiConfigRepository.kodiUrl.first()
            if (kodiUrl.isBlank()) return
            kodiMediaSource.configure(kodiUrl)
            val debrid = apiConfigRepository.debridApiKey.first()
            val debridProvider = apiConfigRepository.debridProvider.first()
            when (debridProvider) {
                "real_debrid" -> {
                    if (debrid.isNotBlank()) {
                        kodiMediaSource.setAddonSetting("plugin.video.fanfilm", "realdebrid_token", debrid)
                    }
                }
                "torbox" -> {
                    if (debrid.isNotBlank()) {
                        kodiMediaSource.setAddonSetting("plugin.video.fanfilm", "torbox_token", debrid)
                        kodiMediaSource.setAddonSetting("plugin.video.fanfilm", "torbox_apikey", debrid)
                    }
                }
            }
            val traktId = apiConfigRepository.traktClientId.first()
            if (traktId.isNotBlank()) {
                kodiMediaSource.setAddonSetting("plugin.video.fanfilm", "trakt_token", traktId)
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w("AdminHttpServer", "pushAddonSettingsIfKodiConfigured failed: ${e.message}", e)
        }
    }

    private fun handleTraktSync(out: java.io.OutputStream, corsOrigin: String?) {
        TraktSyncWorker.startImmediate(context)
        writeResponse(out, 200, "OK", "text/plain", "Trakt sync scheduled", corsOrigin)
    }

    private fun handleProfileSync(out: java.io.OutputStream, corsOrigin: String?) {
        CloudProfileSyncWorker.startBackup(context)
        writeResponse(out, 200, "OK", "text/plain", "Profile cloud sync scheduled", corsOrigin)
    }

    private fun handleProfileRestore(out: java.io.OutputStream, corsOrigin: String?) {
        CloudProfileSyncRestore.restoreIfNeeded(context)
        writeResponse(out, 200, "OK", "text/plain", "Profile restore applied; restart to finish", corsOrigin)
    }

    private fun handlePluginUpdate(out: java.io.OutputStream, corsOrigin: String?) {
        PluginUpdateWorker.startImmediate(context)
        writeResponse(out, 200, "OK", "text/plain", "Plugin update check scheduled", corsOrigin)
    }

    private fun handlePluginTest(
        query: Map<String, String>,
        body: String,
        out: java.io.OutputStream,
        corsOrigin: String?
    ) {
        val format = query["format"]?.lowercase(Locale.getDefault()) ?: "js"
        val params = parseForm(body)
        val code = params["code"]?.ifBlank { null } ?: body
        if (code.isBlank()) {
            writeResponse(
                out,
                400,
                "Bad Request",
                "application/json",
                SandboxResult.error("Missing code").toJson(json),
                corsOrigin
            )
            return
        }

        val result = runBlocking(Dispatchers.IO) {
            try {
                when (format) {
                    "js" -> sandboxEngine.testJs(code)
                    "json" -> sandboxEngine.testJson(code)
                    "python" -> {
                        val kodiUrl = apiConfigRepository.kodiUrl.first()
                        sandboxEngine.testPython(code, kodiUrl)
                    }
                    else -> SandboxResult.error("Unsupported format: $format. Use js, json or python.")
                }
            } catch (e: Exception) {
                SandboxResult.error(e.message ?: "Sandbox error")
            }
        }

        writeResponse(out, 200, "OK", "application/json", result.toJson(json), corsOrigin)
    }

    private fun handlePluginPost(body: String, out: java.io.OutputStream, corsOrigin: String?) {
        val params = parseForm(body)
        val url = params["url"]
        if (url.isNullOrBlank()) {
            writeResponse(out, 400, "Bad Request", "text/plain", "Missing url", corsOrigin)
            return
        }
        runBlocking(Dispatchers.IO) {
            try {
                pluginRepository.addPluginFromUrl(url)
                writeResponse(out, 200, "OK", "text/plain", "OK", corsOrigin)
            } catch (e: Exception) {
                writeResponse(out, 500, "Error", "text/plain", e.message ?: "Error", corsOrigin)
            }
        }
    }

    private fun parseForm(body: String): Map<String, String> {
        if (body.isBlank()) return emptyMap()
        return body.split('&').mapNotNull { pair ->
            val eq = pair.indexOf('=')
            if (eq > 0) {
                val key = URLDecoder.decode(pair.substring(0, eq), "UTF-8")
                val value = URLDecoder.decode(pair.substring(eq + 1), "UTF-8")
                key to value
            } else null
        }.toMap()
    }

    private fun parseQuery(pathWithQuery: String): Map<String, String> {
        val query = pathWithQuery.substringAfter('?', "")
        if (query.isBlank()) return emptyMap()
        return query.split('&').mapNotNull { pair ->
            val eq = pair.indexOf('=')
            if (eq > 0) {
                val key = URLDecoder.decode(pair.substring(0, eq), "UTF-8")
                val value = URLDecoder.decode(pair.substring(eq + 1), "UTF-8")
                key to value
            } else null
        }.toMap()
    }

    private fun writeResponse(
        out: java.io.OutputStream,
        code: Int,
        status: String,
        contentType: String,
        body: String,
        corsOrigin: String? = null
    ) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        val corsHeader = corsOrigin?.let { "Access-Control-Allow-Origin: $it\r\n" } ?: ""
        val headers = "HTTP/1.1 $code $status\r\n" +
            "Content-Type: $contentType\r\n" +
            "Content-Length: ${bytes.size}\r\n" +
            "Connection: close\r\n" +
            corsHeader +
            "\r\n"
        out.write(headers.toByteArray(Charsets.UTF_8))
        out.write(bytes)
        out.flush()
    }

    private fun maskSecret(value: String): String {
        if (value.isBlank()) return value
        return if (value.length <= 8) {
            "*".repeat(value.length)
        } else {
            value.take(4) + "*".repeat(value.length - 8) + value.takeLast(4)
        }
    }

    companion object {
        private val SENSITIVE_KEYS = setOf(
            "tmdbApiKey",
            "aniListToken",
            "traktClientId",
            "traktClientSecret",
            "traktRefreshToken",
            "traktAccessToken",
            "debridApiKey",
            "jellyfinToken",
            "plexToken",
            "embyToken",
            "subsonicPassword",
            "mdbListApiKey",
            "cloudflareAuthToken",
            "homeAssistantToken"
        )

        private val ADMIN_HTML = """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Polish Media Hub - Admin</title>
<style>
body { font-family: -apple-system, BlinkMacSystemFont, sans-serif; background: #111; color: #eee; padding: 1rem; max-width: 720px; margin: 0 auto; }
h1 { color: #4fc3f7; }
label { display: block; margin-top: 1rem; font-size: 0.9rem; color: #bbb; }
textarea, input { width: 100%; background: #222; color: #eee; border: 1px solid #444; padding: 0.5rem; border-radius: 4px; box-sizing: border-box; }
textarea { min-height: 80px; font-family: monospace; }
button { margin-top: 1.5rem; padding: 0.75rem 1.5rem; background: #4fc3f7; color: #000; border: none; border-radius: 4px; cursor: pointer; font-weight: bold; }
button:hover { background: #29b6f6; }
.status { margin-top: 1rem; padding: 0.5rem; border-radius: 4px; display: none; }
.status.ok { background: #1b5e20; display: block; }
.status.err { background: #b71c1c; display: block; }
.health-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 0.5rem; margin-top: 1rem; }
.health-item { display: flex; align-items: center; gap: 0.5rem; background: #222; padding: 0.5rem; border-radius: 4px; }
.status-dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; flex-shrink: 0; }
.status-dot.online { background: #4dff8c; }
.status-dot.offline { background: #ff4d4d; }
.status-dot.unconfigured { background: #808080; }
label .status-dot { margin-left: 0.5rem; }
.CodeMirror { border: 1px solid #444; border-radius: 4px; background: #222; color: #eee; min-height: 160px; font-family: monospace; font-size: 0.9rem; }
</style>
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.16/codemirror.min.css">
<script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.16/codemirror.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.16/mode/javascript/javascript.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.65.16/mode/python/python.min.js"></script>
</head>
<body>
<h1>Polish Media Hub - Admin</h1>
<p>Configure sources and plugins from your phone or computer.</p>
<h2>Source Health</h2>
<div id="healthStatus" class="health-grid"></div>
<p id="epgStatus" class="status"></p>
<form id="configForm">
  <label>Kodi URL <span class="status-dot unconfigured" id="status-kodiUrl"></span></label>
  <input type="text" name="kodiUrl" placeholder="http://192.168.1.10:8080">
  <label>Web Source Config (JSON) <span class="status-dot unconfigured" id="status-webSourceConfig"></span></label>
  <textarea name="webSourceConfig" placeholder='[{"id":"example",...}]'></textarea>
  <label>Cloudstream Repo URLs (one per line) <span class="status-dot unconfigured" id="status-cloudstreamRepoUrls"></span></label>
  <textarea name="cloudstreamRepoUrls"></textarea>
  <label>IPTV / M3U URLs (one per line) <span class="status-dot unconfigured" id="status-iptvSourceUrls"></span></label>
  <textarea name="iptvSourceUrls"></textarea>
  <label>EPG URL <span class="status-dot unconfigured" id="status-epgUrl"></span></label>
  <input type="text" name="epgUrl" placeholder="https://example.com/epg.xml">
  <label>Stremio Addons (one per line) <span class="status-dot unconfigured" id="status-stremioAddons"></span></label>
  <textarea name="stremioAddons"></textarea>
  <label>Jellyfin URL <span class="status-dot unconfigured" id="status-jellyfinUrl"></span></label>
  <input type="text" name="jellyfinUrl">
  <label>Jellyfin Token</label>
  <input type="text" name="jellyfinToken">
  <label>Plex URL <span class="status-dot unconfigured" id="status-plexUrl"></span></label>
  <input type="text" name="plexUrl">
  <label>Plex Token</label>
  <input type="text" name="plexToken">
  <label>Emby URL <span class="status-dot unconfigured" id="status-embyUrl"></span></label>
  <input type="text" name="embyUrl">
  <label>Emby Token</label>
  <input type="text" name="embyToken">
  <label>Force Transcode (true/false)</label>
  <input type="text" name="forceTranscode" placeholder="false">
  <label>Max Direct Play Bitrate (bps)</label>
  <input type="text" name="maxDirectPlayBitrate" placeholder="20000000">
  <label>Subsonic URL <span class="status-dot unconfigured" id="status-subsonicUrl"></span></label>
  <input type="text" name="subsonicUrl">
  <label>Subsonic User</label>
  <input type="text" name="subsonicUser">
  <label>Subsonic Password</label>
  <input type="text" name="subsonicPassword">
  <label>Podcast RSS Feeds (one per line) <span class="status-dot unconfigured" id="status-podcastFeeds"></span></label>
  <textarea name="podcastFeeds"></textarea>
  <label>Deezer Proxy URL <span class="status-dot unconfigured" id="status-deezerProxyUrl"></span></label>
  <input type="text" name="deezerProxyUrl" placeholder="https://your-worker.workers.dev">
  <label>MDBList API Key</label>
  <input type="password" name="mdbListApiKey" placeholder="Get it at mdblist.com/preferences/#api">
  <label>TMDB API Key</label>
  <input type="text" name="tmdbApiKey">
  <label>AniList Token</label>
  <input type="text" name="aniListToken">
  <label>Trakt Client ID</label>
  <input type="text" name="traktClientId">
  <label>Trakt Client Secret</label>
  <input type="password" name="traktClientSecret" placeholder="OAuth secret from trakt.tv/oauth/applications">
  <label>Trakt Access Token</label>
  <input type="password" name="traktAccessToken" placeholder="OAuth Bearer token from Trakt.tv">
  <label>Debrid API Key / Token</label>
  <input type="text" name="debridApiKey">
  <label>Debrid Provider</label>
  <input type="text" name="debridProvider" placeholder="real_debrid or torbox">
  <h3>Skip Intro / Outro</h3>
  <label>Auto Skip Intros and Outros (true/false)</label>
  <input type="text" name="autoSkipIntro" placeholder="true">
  <label>Intro End (seconds)</label>
  <input type="text" name="introEndSeconds" placeholder="90">
  <label>Outro Duration from End (seconds)</label>
  <input type="text" name="outroDurationSeconds" placeholder="120">
  <h3>Player Engine</h3>
  <label>Use LibVLC alternative player (true/false)</label>
  <input type="text" name="useAlternativePlayer" placeholder="false">
  <h3>Premium Audio</h3>
  <label>Preferred Polish Audio</label>
  <input type="text" name="preferredAudioType" placeholder="lector or dubbing">
  <label>Night Mode (true/false)</label>
  <input type="text" name="nightModeEnabled" placeholder="false">
  <label>Dialogue Boost (mB, 0-3000)</label>
  <input type="text" name="dialogueBoostGainmB" placeholder="1000">
  <h3>Appearance &amp; Playback Premium</h3>
  <label>AMOLED Mode (true/false)</label>
  <input type="text" name="amoledMode" placeholder="false">
  <label>Pure Black Surfaces (true/false)</label>
  <input type="text" name="pureBlackSurfaces" placeholder="false">
  <h4>ExoPlayer Native Engine</h4>
  <label>Tunneled Playback (true/false)</label>
  <input type="text" name="tunneledPlaybackEnabled" placeholder="false">
  <label>Parallel Connections (1-16)</label>
  <input type="text" name="exoplayerParallelConnections" placeholder="4">
  <label>Min Buffer (ms, 1000-120000)</label>
  <input type="text" name="exoplayerMinBufferMs" placeholder="5000">
  <label>Max Buffer (ms, 1000-1200000)</label>
  <input type="text" name="exoplayerMaxBufferMs" placeholder="50000">
  <label>Buffer for Playback (ms, 0-60000)</label>
  <input type="text" name="exoplayerBufferForPlaybackMs" placeholder="2500">
  <label>Buffer for Playback After Rebuffer (ms, 0-60000)</label>
  <input type="text" name="exoplayerBufferForPlaybackAfterRebufferMs" placeholder="5000">
  <label>Back Buffer (ms, 0-120000)</label>
  <input type="text" name="exoplayerBackBufferMs" placeholder="0">
  <label>Initial Allocation Count (0-64)</label>
  <input type="text" name="exoplayerInitialAllocationCount" placeholder="0">
  <label>Target Buffer Bytes (0-2GB)</label>
  <input type="text" name="exoplayerTargetBufferBytes" placeholder="-1">
  <h4>Debrid / TorBox Stream Rules (JSON)</h4>
  <textarea name="streamRules" placeholder='{"enabled":true,"sizeMinMb":500,"sizeMaxMb":51200,"resolutions":["1080p","4K"],"preferredEncoders":["HEVC"]}'></textarea>
  <label>Binge Grouping (true/false)</label>
  <input type="text" name="bingeGroupingEnabled" placeholder="true">
  <h3>Cloudflare Edge Offloading</h3>
  <label>Use Cloudflare bypass (true/false)</label>
  <input type="text" name="useCloudflareBypass" placeholder="false">
  <label>Cloudflare Worker URL</label>
  <input type="text" name="cloudflareWorkerUrl" placeholder="https://your-worker.workers.dev">
  <label>Cloudflare Worker Auth Token</label>
  <input type="password" name="cloudflareAuthToken" placeholder="Hub token from worker secret">
  <h3>Smart Home Assistant</h3>
  <label>Send playback webhooks (true/false)</label>
  <input type="text" name="homeAssistantWebhookEnabled" placeholder="true">
  <label>Home Assistant URL</label>
  <input type="text" name="homeAssistantUrl" placeholder="https://homeassistant.local:8123">
  <label>Home Assistant Webhook ID / Token</label>
  <input type="password" name="homeAssistantToken" placeholder="Webhook ID from Home Assistant automation">
  <button type="submit">Save Configuration</button>
  <div id="status" class="status"></div>
</form>
<h2>Profile Cloud Sync</h2>
<p id="profileSyncStatus" class="status"></p>
<button type="button" id="profileSyncBtn">Sync profiles to cloud now</button>
<button type="button" id="profileRestoreBtn">Restore profile backup</button>
<h2>Plugin Updates <span id="pluginUpdateBadge" style="background:#ff4d4d; color:#fff; border-radius:50%; padding:0 6px; font-size:0.75rem; display:none;"></span></h2>
<p id="pluginUpdateStatus" class="status"></p>
<button type="button" id="pluginUpdateBtn">Check for plugin updates</button>
<h2>Trakt Sync</h2>
<p id="traktStatus" class="status"></p>
<button type="button" id="traktSyncBtn">Sync with Trakt now</button>
<h2>Plugin</h2>
<form id="pluginForm">
  <label>Plugin URL</label>
  <input type="text" name="url" placeholder="https://example.com/plugin.json">
  <button type="submit">Add Plugin</button>
</form>
<h2>Developer Console</h2>
<form id="sandboxForm">
  <label>Format</label>
  <div style="display:flex; gap:1rem; flex-wrap:wrap; margin:0.5rem 0;">
    <label><input type="radio" name="sandboxFormat" value="js" checked> JavaScript / QuickJS</label>
    <label><input type="radio" name="sandboxFormat" value="json"> JSON / MediaItem validator</label>
    <label><input type="radio" name="sandboxFormat" value="python"> Python / Kodi RPC</label>
  </div>
  <label>Code</label>
  <textarea id="sandboxCode" name="code" placeholder="// JS: use httpFetch('url') or write QuickJS code&#10;// JSON: paste a MediaItem JSON object&#10;// Python: paste a Kodi test_scraper.py script"></textarea>
  <button type="submit" id="sandboxBtn">Testuj w QuickJS ⚡</button>
  <pre id="sandboxOutput" class="status" style="display:none; white-space:pre-wrap; font-family:monospace; background:#222;"></pre>
</form>
<script>
const API_TOKEN = new URLSearchParams(window.location.search).get('token') || '';
function api(path) { return path + '?token=' + encodeURIComponent(API_TOKEN); }
async function loadConfig() {
  try {
    const res = await fetch(api('/api/config'));
    const cfg = await res.json();
    for (const [key, value] of Object.entries(cfg)) {
      const el = document.querySelector('[name="' + key + '"]');
      if (el) el.value = value;
    }
  } catch (e) { console.error(e); }
}
document.getElementById('configForm').addEventListener('submit', async function(e) {
  e.preventDefault();
  const form = new FormData(this);
  const params = new URLSearchParams(form).toString();
  const status = document.getElementById('status');
  try {
    const res = await fetch(api('/api/config'), { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: params });
    if (res.ok) { status.textContent = 'Saved'; status.className = 'status ok'; }
    else { status.textContent = 'Error'; status.className = 'status err'; }
  } catch (err) { status.textContent = err.message; status.className = 'status err'; }
});
document.getElementById('pluginForm').addEventListener('submit', async function(e) {
  e.preventDefault();
  const form = new FormData(this);
  const params = new URLSearchParams(form).toString();
  try {
    await fetch(api('/api/plugin'), { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: params });
    alert('Plugin added');
  } catch (err) { alert(err.message); }
});
let sandboxEditor = CodeMirror.fromTextArea(document.getElementById('sandboxCode'), {
  lineNumbers: true,
  mode: 'javascript',
  theme: 'default',
  tabSize: 2
});
function setSandboxEditorMode(format) {
  if (format === 'python') sandboxEditor.setOption('mode', 'python');
  else if (format === 'json') sandboxEditor.setOption('mode', { name: 'javascript', json: true });
  else sandboxEditor.setOption('mode', 'javascript');
}
function updateSandboxButtonLabel() {
  const format = document.querySelector('input[name="sandboxFormat"]:checked').value;
  const btn = document.getElementById('sandboxBtn');
  if (format === 'js') { btn.textContent = 'Testuj w QuickJS ⚡'; }
  else if (format === 'json') { btn.textContent = 'Waliduj strukturę JSON 🔍'; }
  else { btn.textContent = 'Uruchom skrypt i debuguj w Kodi 🐍'; }
  setSandboxEditorMode(format);
}
document.querySelectorAll('input[name="sandboxFormat"]').forEach(r => r.addEventListener('change', updateSandboxButtonLabel));
document.getElementById('sandboxForm').addEventListener('submit', async function(e) {
  e.preventDefault();
  const format = document.querySelector('input[name="sandboxFormat"]:checked').value;
  const code = sandboxEditor.getValue();
  const out = document.getElementById('sandboxOutput');
  out.style.display = 'block';
  out.className = 'status';
  out.textContent = 'Running...';
  try {
    const params = new URLSearchParams();
    params.append('code', code);
    const res = await fetch(api('/api/plugin/test') + '&format=' + encodeURIComponent(format), {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: params.toString()
    });
    const result = await res.json();
    out.textContent = JSON.stringify(result, null, 2);
    out.className = 'status ' + (result.success ? 'ok' : 'err');
  } catch (err) {
    out.textContent = err.message;
    out.className = 'status err';
  }
});
async function loadEpgStatus() {
  try {
    const res = await fetch(api('/api/config'));
    const cfg = await res.json();
    const at = cfg.lastEpgSyncAt ? new Date(Number(cfg.lastEpgSyncAt)).toLocaleString() : 'Never';
    const status = cfg.lastEpgSyncStatus || '';
    const error = cfg.lastEpgSyncError;
    const el = document.getElementById('epgStatus');
    el.textContent = 'Last EPG sync: ' + at + ' — ' + status + (error ? ' (' + error + ')' : '');
    el.className = 'status ' + (status === 'success' ? 'ok' : status ? 'err' : '');
    el.style.display = status ? 'block' : 'none';
  } catch (e) { console.error(e); }
}
async function syncProfiles() {
  try {
    const res = await fetch(api('/api/profile/sync'), { method: 'POST' });
    if (res.ok) { alert('Profile sync scheduled'); }
    else { alert('Profile sync failed'); }
  } catch (err) { alert(err.message); }
}
async function restoreProfiles() {
  try {
    const res = await fetch(api('/api/profile/restore'), { method: 'POST' });
    const text = await res.text();
    if (res.ok) { alert(text); }
    else { alert('Profile restore failed: ' + text); }
  } catch (err) { alert(err.message); }
}
async function loadProfileSyncStatus() {
  try {
    const res = await fetch(api('/api/config'));
    const cfg = await res.json();
    const at = cfg.lastProfileSyncAt ? new Date(Number(cfg.lastProfileSyncAt)).toLocaleString() : 'Never';
    const status = cfg.lastProfileSyncStatus || '';
    const error = cfg.lastProfileSyncError;
    const el = document.getElementById('profileSyncStatus');
    el.textContent = 'Last profile sync: ' + at + ' — ' + status + (error ? ' (' + error + ')' : '');
    el.className = 'status ' + (status === 'success' ? 'ok' : status ? 'err' : '');
    el.style.display = status ? 'block' : 'none';
  } catch (e) { console.error(e); }
}
async function checkPluginUpdates() {
  try {
    const res = await fetch(api('/api/plugin/update'), { method: 'POST' });
    if (res.ok) { alert('Plugin update check scheduled'); }
    else { alert('Plugin update check failed'); }
  } catch (err) { alert(err.message); }
}
async function loadPluginUpdateStatus() {
  try {
    const res = await fetch(api('/api/config'));
    const cfg = await res.json();
    const at = cfg.lastPluginUpdateAt ? new Date(Number(cfg.lastPluginUpdateAt)).toLocaleString() : 'Never';
    const count = Number(cfg.pluginUpdateCount) || 0;
    const el = document.getElementById('pluginUpdateStatus');
    el.textContent = 'Last plugin update check: ' + at + (count > 0 ? ' (' + count + ' updates available)' : '');
    el.className = 'status ' + (count > 0 ? 'err' : 'ok');
    el.style.display = 'block';
    const badge = document.getElementById('pluginUpdateBadge');
    if (count > 0) { badge.textContent = count; badge.style.display = 'inline'; }
    else { badge.style.display = 'none'; }
  } catch (e) { console.error(e); }
}
async function syncTrakt() {
  try {
    const res = await fetch(api('/api/trakt/sync'), { method: 'POST' });
    if (res.ok) { alert('Trakt sync scheduled'); }
    else { alert('Trakt sync failed'); }
  } catch (err) { alert(err.message); }
}
async function loadTraktStatus() {
  try {
    const res = await fetch(api('/api/config'));
    const cfg = await res.json();
    const at = cfg.lastTraktSyncAt ? new Date(Number(cfg.lastTraktSyncAt)).toLocaleString() : 'Never';
    const status = cfg.lastTraktSyncStatus || '';
    const error = cfg.lastTraktSyncError;
    const el = document.getElementById('traktStatus');
    el.textContent = 'Last Trakt sync: ' + at + ' — ' + status + (error ? ' (' + error + ')' : '');
    el.className = 'status ' + (status === 'success' ? 'ok' : status ? 'err' : '');
    el.style.display = status ? 'block' : 'none';
  } catch (e) { console.error(e); }
}
document.getElementById('traktSyncBtn').addEventListener('click', syncTrakt);
async function loadHealth() {
  try {
    const res = await fetch(api('/api/health'));
    const data = await res.json();
    const container = document.getElementById('healthStatus');
    if (!data.sources || data.sources.length === 0) {
      container.innerHTML = '<p>No health data yet. Wait for the next background check or open Settings on the TV.</p>';
      return;
    }
    const fieldMap = {
      kodi: 'kodiUrl',
      epg: 'epgUrl',
      jellyfin: 'jellyfinUrl',
      plex: 'plexUrl',
      emby: 'embyUrl',
      subsonic: 'subsonicUrl',
      deezer: 'deezerProxyUrl'
    };
    const grouped = {};
    data.sources.forEach(s => {
      const prefix = s.id.split('_')[0];
      const field = fieldMap[prefix];
      if (field) {
        if (!grouped[field]) grouped[field] = [];
        grouped[field].push(s);
      }
    });
    Object.keys(grouped).forEach(field => {
      const dot = document.getElementById('status-' + field);
      if (dot) {
        const sources = grouped[field];
        if (sources.every(s => s.status === 'UNCONFIGURED')) {
          dot.className = 'status-dot unconfigured';
        } else if (sources.every(s => s.status === 'ONLINE' || s.status === 'UNCONFIGURED')) {
          dot.className = 'status-dot online';
        } else if (sources.some(s => s.status === 'OFFLINE')) {
          dot.className = 'status-dot offline';
        } else {
          dot.className = 'status-dot unconfigured';
        }
      }
    });
    container.innerHTML = data.sources.map(s => {
      const cls = s.status === 'ONLINE' ? 'online' : (s.status === 'OFFLINE' ? 'offline' : 'unconfigured');
      const label = (s.label || s.id) + (s.error ? ' (' + s.error + ')' : '');
      return '<div class="health-item"><span class="status-dot ' + cls + '"></span><span>' + label + '</span></div>';
    }).join('');
  } catch (e) { console.error(e); }
}
document.getElementById('traktSyncBtn').addEventListener('click', syncTrakt);
document.getElementById('profileSyncBtn')?.addEventListener('click', syncProfiles);
document.getElementById('profileRestoreBtn')?.addEventListener('click', restoreProfiles);
document.getElementById('pluginUpdateBtn')?.addEventListener('click', checkPluginUpdates);
updateSandboxButtonLabel();
loadConfig();
loadHealth();
loadEpgStatus();
loadTraktStatus();
loadProfileSyncStatus();
loadPluginUpdateStatus();
</script>
</body>
</html>
        """.trimIndent()
    }
}
