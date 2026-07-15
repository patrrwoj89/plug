package com.polishmediahub.app.data.admin

import android.content.Context
import android.util.Log
import com.polishmediahub.app.data.ApiConfigRepository
import com.polishmediahub.app.data.SettingsRepository
import com.polishmediahub.app.data.plugin.PluginRepository
import com.polishmediahub.app.data.remote.trakt.TraktSyncWorker
import com.polishmediahub.app.data.source.KodiMediaSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminHttpServer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val apiConfigRepository: ApiConfigRepository,
    private val settingsRepository: SettingsRepository,
    private val pluginRepository: PluginRepository,
    private val kodiMediaSource: KodiMediaSource
) {

    private var serverSocket: ServerSocket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var port: Int = 0
        private set

    fun start(): Int = synchronized(this) {
        serverSocket?.let { return port }
        val socket = ServerSocket(0)
        port = socket.localPort
        serverSocket = socket
        scope.launch {
            while (!socket.isClosed) {
                try {
                    val client = socket.accept()
                    scope.launch { handleClient(client) }
                } catch (e: Exception) {
                    if (!socket.isClosed) Log.w("AdminHttpServer", "accept error: ${e.message}")
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
            Log.w("AdminHttpServer", "stop failed: ${e.message}", e)
        }
        serverSocket = null
        port = 0
    }

    fun adminUrl(ip: String): String = "http://$ip:$port/admin"

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
                when {
                    method == "GET" && path == "/admin" -> serveAdminPage(out)
                    method == "POST" && path == "/api/config" -> handleConfigPost(body, out)
                    method == "POST" && path == "/api/plugin" -> handlePluginPost(body, out)
                    method == "POST" && path == "/api/trakt/sync" -> handleTraktSync(out)
                    method == "GET" && path == "/api/config" -> serveConfig(out)
                    else -> writeResponse(out, 404, "Not Found", "text/plain", "Not Found")
                }
            }
        } catch (e: Exception) {
            Log.w("AdminHttpServer", "client error: ${e.message}")
        }
    }

    private fun serveAdminPage(out: java.io.OutputStream) {
        val html = ADMIN_HTML
        writeResponse(out, 200, "OK", "text/html; charset=utf-8", html)
    }

    private fun serveConfig(out: java.io.OutputStream) {
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
                    "lastTraktSyncError" to lastTraktSyncError.map { it ?: "" }
                ) + with(settingsRepository) {
                    mapOf(
                        "autoSkipIntro" to autoSkipIntro,
                        "introEndSeconds" to defaultIntroEndSeconds,
                        "outroDurationSeconds" to defaultOutroDurationSeconds,
                        "useAlternativePlayer" to useAlternativePlayer
                    )
                }
            }.mapValues { (_, flow) -> flow.first().toString() }
            val jsonObj = JsonObject(values.mapValues { (_, v) -> JsonPrimitive(v) })
            val json = Json.encodeToString(JsonObject.serializer(), jsonObj)
            writeResponse(out, 200, "OK", "application/json", json)
        }
    }


    private fun handleConfigPost(body: String, out: java.io.OutputStream) {
        val params = parseForm(body)
        runBlocking(Dispatchers.IO) {
            params["tmdbApiKey"]?.let { apiConfigRepository.setTmdbApiKey(it) }
            params["aniListToken"]?.let { apiConfigRepository.setAniListToken(it) }
            params["traktClientId"]?.let { apiConfigRepository.setTraktClientId(it) }
            params["traktClientSecret"]?.let { apiConfigRepository.setTraktClientSecret(it) }
            params["debridApiKey"]?.let { apiConfigRepository.setDebridApiKey(it) }
            params["debridProvider"]?.let { apiConfigRepository.setDebridProvider(it) }
            params["iptvSourceUrls"]?.let { apiConfigRepository.setIptvSourceUrls(it) }
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
            pushAddonSettingsIfKodiConfigured()
        }
        writeResponse(out, 200, "OK", "text/plain", "OK")
    }

    private suspend fun pushAddonSettingsIfKodiConfigured() {
        try {
            val kodiUrl = apiConfigRepository.kodiUrl.first()
            if (kodiUrl.isBlank()) return
            kodiMediaSource.configure(kodiUrl)
            val debrid = apiConfigRepository.debridApiKey.first()
            if (debrid.isNotBlank()) {
                kodiMediaSource.setAddonSetting("plugin.video.fanfilm", "realdebrid_token", debrid)
            }
            val traktId = apiConfigRepository.traktClientId.first()
            if (traktId.isNotBlank()) {
                kodiMediaSource.setAddonSetting("plugin.video.fanfilm", "trakt_token", traktId)
            }
        } catch (e: Exception) {
            Log.w("AdminHttpServer", "pushAddonSettingsIfKodiConfigured failed: ${e.message}", e)
        }
    }

    private fun handleTraktSync(out: java.io.OutputStream) {
        TraktSyncWorker.startImmediate(context)
        writeResponse(out, 200, "OK", "text/plain", "Trakt sync scheduled")
    }

    private fun handlePluginPost(body: String, out: java.io.OutputStream) {
        val params = parseForm(body)
        val url = params["url"]
        if (url.isNullOrBlank()) {
            writeResponse(out, 400, "Bad Request", "text/plain", "Missing url")
            return
        }
        runBlocking(Dispatchers.IO) {
            try {
                pluginRepository.addPluginFromUrl(url)
                writeResponse(out, 200, "OK", "text/plain", "OK")
            } catch (e: Exception) {
                writeResponse(out, 500, "Error", "text/plain", e.message ?: "Error")
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

    private fun writeResponse(out: java.io.OutputStream, code: Int, status: String, contentType: String, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        val headers = "HTTP/1.1 $code $status\r\n" +
            "Content-Type: $contentType\r\n" +
            "Content-Length: ${bytes.size}\r\n" +
            "Connection: close\r\n" +
            "Access-Control-Allow-Origin: *\r\n\r\n"
        out.write(headers.toByteArray(Charsets.UTF_8))
        out.write(bytes)
        out.flush()
    }

    companion object {
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
</style>
</head>
<body>
<h1>Polish Media Hub - Admin</h1>
<p>Configure sources and plugins from your phone or computer.</p>
<p id="epgStatus" class="status"></p>
<form id="configForm">
  <label>Kodi URL</label>
  <input type="text" name="kodiUrl" placeholder="http://192.168.1.10:8080">
  <label>Web Source Config (JSON)</label>
  <textarea name="webSourceConfig" placeholder='[{"id":"example",...}]'></textarea>
  <label>Cloudstream Repo URLs (one per line)</label>
  <textarea name="cloudstreamRepoUrls"></textarea>
  <label>IPTV / M3U URLs (one per line)</label>
  <textarea name="iptvSourceUrls"></textarea>
  <label>Stremio Addons (one per line)</label>
  <textarea name="stremioAddons"></textarea>
  <label>Jellyfin URL</label>
  <input type="text" name="jellyfinUrl">
  <label>Jellyfin Token</label>
  <input type="text" name="jellyfinToken">
  <label>Plex URL</label>
  <input type="text" name="plexUrl">
  <label>Plex Token</label>
  <input type="text" name="plexToken">
  <label>Emby URL</label>
  <input type="text" name="embyUrl">
  <label>Emby Token</label>
  <input type="text" name="embyToken">
  <label>Force Transcode (true/false)</label>
  <input type="text" name="forceTranscode" placeholder="false">
  <label>Max Direct Play Bitrate (bps)</label>
  <input type="text" name="maxDirectPlayBitrate" placeholder="20000000">
  <label>Subsonic URL</label>
  <input type="text" name="subsonicUrl">
  <label>Subsonic User</label>
  <input type="text" name="subsonicUser">
  <label>Subsonic Password</label>
  <input type="text" name="subsonicPassword">
  <label>Podcast RSS Feeds (one per line)</label>
  <textarea name="podcastFeeds"></textarea>
  <label>Deezer Proxy URL</label>
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
  <button type="submit">Save Configuration</button>
  <div id="status" class="status"></div>
</form>
<h2>Trakt Sync</h2>
<p id="traktStatus" class="status"></p>
<button type="button" id="traktSyncBtn">Sync with Trakt now</button>
<h2>Plugin</h2>
<form id="pluginForm">
  <label>Plugin URL</label>
  <input type="text" name="url" placeholder="https://example.com/plugin.json">
  <button type="submit">Add Plugin</button>
</form>
<script>
async function loadConfig() {
  try {
    const res = await fetch('/api/config');
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
    const res = await fetch('/api/config', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: params });
    if (res.ok) { status.textContent = 'Saved'; status.className = 'status ok'; }
    else { status.textContent = 'Error'; status.className = 'status err'; }
  } catch (err) { status.textContent = err.message; status.className = 'status err'; }
});
document.getElementById('pluginForm').addEventListener('submit', async function(e) {
  e.preventDefault();
  const form = new FormData(this);
  const params = new URLSearchParams(form).toString();
  try {
    await fetch('/api/plugin', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: params });
    alert('Plugin added');
  } catch (err) { alert(err.message); }
});
async function loadEpgStatus() {
  try {
    const res = await fetch('/api/config');
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
async function syncTrakt() {
  try {
    const res = await fetch('/api/trakt/sync', { method: 'POST' });
    if (res.ok) { alert('Trakt sync scheduled'); }
    else { alert('Trakt sync failed'); }
  } catch (err) { alert(err.message); }
}
async function loadTraktStatus() {
  try {
    const res = await fetch('/api/config');
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
loadConfig();
loadEpgStatus();
loadTraktStatus();
</script>
</body>
</html>
        """.trimIndent()
    }
}
