package com.polishmediahub.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.apiConfigDataStore: DataStore<Preferences> by preferencesDataStore(name = "api_config")

@Singleton
class ApiConfigRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val encryptedSettingsManager: EncryptedSettingsManager
) {

    val tmdbApiKey: Flow<String> = stringFlow(KEY_TMDB)
    val aniListToken: Flow<String> = stringFlow(KEY_ANILIST)
    val traktClientId: Flow<String> = stringFlow(KEY_TRAKT_ID)
    val traktClientSecret: Flow<String> = stringFlow(KEY_TRAKT_SECRET)
    val traktAccessToken: Flow<String> = stringFlow(KEY_TRAKT_ACCESS)
    val traktRefreshToken: Flow<String> = stringFlow(KEY_TRAKT_REFRESH)
    val debridApiKey: Flow<String> = stringFlow(KEY_DEBRID)
    val debridAccessToken: Flow<String> = stringFlow(KEY_DEBRID_ACCESS)
    val debridRefreshToken: Flow<String> = stringFlow(KEY_DEBRID_REFRESH)
    val debridProvider: Flow<String> = plainStringFlow(KEY_DEBRID_PROVIDER)
    val iptvSourceUrls: Flow<String> = plainStringFlow(KEY_IPTV)
    val epgUrl: Flow<String> = plainStringFlow(KEY_EPG)
    val stremioAddons: Flow<String> = plainStringFlow(KEY_STREMIO)
    val kodiUrl: Flow<String> = plainStringFlow(KEY_KODI_URL)
    val webSourceConfig: Flow<String> = plainStringFlow(KEY_WEB_SOURCE_CONFIG)
    val cloudstreamRepoUrls: Flow<String> = plainStringFlow(KEY_CLOUDSTREAM_REPOS)
    val jellyfinUrl: Flow<String> = plainStringFlow(KEY_JELLYFIN_URL)
    val jellyfinToken: Flow<String> = stringFlow(KEY_JELLYFIN_TOKEN)
    val plexUrl: Flow<String> = plainStringFlow(KEY_PLEX_URL)
    val plexToken: Flow<String> = stringFlow(KEY_PLEX_TOKEN)
    val embyUrl: Flow<String> = plainStringFlow(KEY_EMBY_URL)
    val embyToken: Flow<String> = stringFlow(KEY_EMBY_TOKEN)
    val forceTranscode: Flow<Boolean> = context.apiConfigDataStore.data.map { it[KEY_FORCE_TRANSCODE] ?: false }
    val maxDirectPlayBitrate: Flow<String> = plainStringFlow(KEY_MAX_DIRECT_PLAY_BITRATE)
    val subsonicUrl: Flow<String> = plainStringFlow(KEY_SUBSONIC_URL)
    val subsonicUser: Flow<String> = plainStringFlow(KEY_SUBSONIC_USER)
    val subsonicPassword: Flow<String> = stringFlow(KEY_SUBSONIC_PASSWORD)
    val podcastFeeds: Flow<String> = plainStringFlow(KEY_PODCAST_FEEDS)
    val deezerProxyUrl: Flow<String> = plainStringFlow(KEY_DEEZER_PROXY_URL)
    val mdbListApiKey: Flow<String> = stringFlow(KEY_MDBLIST)
    val lastEpgSyncAt: Flow<Long> = context.apiConfigDataStore.data.map { it[KEY_LAST_EPG_SYNC_AT] ?: 0L }
    val lastEpgSyncStatus: Flow<String> = plainStringFlow(KEY_LAST_EPG_SYNC_STATUS)
    val lastEpgSyncError: Flow<String?> = context.apiConfigDataStore.data.map { it[KEY_LAST_EPG_SYNC_ERROR] }
    val lastTraktSyncAt: Flow<Long> = context.apiConfigDataStore.data.map { it[KEY_LAST_TRAKT_SYNC_AT] ?: 0L }
    val lastTraktSyncStatus: Flow<String> = plainStringFlow(KEY_LAST_TRAKT_SYNC_STATUS)
    val lastTraktSyncError: Flow<String?> = context.apiConfigDataStore.data.map { it[KEY_LAST_TRAKT_SYNC_ERROR] }

    val healthStatuses: Flow<String> = plainStringFlow(KEY_HEALTH_STATUSES)

    suspend fun setTmdbApiKey(value: String) = edit(KEY_TMDB, value)
    suspend fun setAniListToken(value: String) = edit(KEY_ANILIST, value)
    suspend fun setTraktClientId(value: String) = edit(KEY_TRAKT_ID, value)
    suspend fun setTraktClientSecret(value: String) = edit(KEY_TRAKT_SECRET, value)
    suspend fun setTraktAccessToken(value: String) = edit(KEY_TRAKT_ACCESS, value)
    suspend fun setTraktRefreshToken(value: String) = edit(KEY_TRAKT_REFRESH, value)
    suspend fun setDebridApiKey(value: String) = edit(KEY_DEBRID, value)
    suspend fun setDebridAccessToken(value: String) = edit(KEY_DEBRID_ACCESS, value)
    suspend fun setDebridRefreshToken(value: String) = edit(KEY_DEBRID_REFRESH, value)
    suspend fun setDebridProvider(value: String) = edit(KEY_DEBRID_PROVIDER, value)
    suspend fun setIptvSourceUrls(value: String) = edit(KEY_IPTV, value)
    suspend fun setEpgUrl(value: String) = edit(KEY_EPG, value)
    suspend fun setStremioAddons(value: String) = edit(KEY_STREMIO, value)
    suspend fun setKodiUrl(value: String) = edit(KEY_KODI_URL, value)
    suspend fun setWebSourceConfig(value: String) = edit(KEY_WEB_SOURCE_CONFIG, value)
    suspend fun setCloudstreamRepoUrls(value: String) = edit(KEY_CLOUDSTREAM_REPOS, value)
    suspend fun setJellyfinUrl(value: String) = edit(KEY_JELLYFIN_URL, value)
    suspend fun setJellyfinToken(value: String) = edit(KEY_JELLYFIN_TOKEN, value)
    suspend fun setPlexUrl(value: String) = edit(KEY_PLEX_URL, value)
    suspend fun setPlexToken(value: String) = edit(KEY_PLEX_TOKEN, value)
    suspend fun setEmbyUrl(value: String) = edit(KEY_EMBY_URL, value)
    suspend fun setEmbyToken(value: String) = edit(KEY_EMBY_TOKEN, value)
    suspend fun setForceTranscode(value: Boolean) = edit(KEY_FORCE_TRANSCODE, value)
    suspend fun setMaxDirectPlayBitrate(value: String) = edit(KEY_MAX_DIRECT_PLAY_BITRATE, value)
    suspend fun setSubsonicUrl(value: String) = edit(KEY_SUBSONIC_URL, value)
    suspend fun setSubsonicUser(value: String) = edit(KEY_SUBSONIC_USER, value)
    suspend fun setSubsonicPassword(value: String) = edit(KEY_SUBSONIC_PASSWORD, value)
    suspend fun setPodcastFeeds(value: String) = edit(KEY_PODCAST_FEEDS, value)
    suspend fun setDeezerProxyUrl(value: String) = edit(KEY_DEEZER_PROXY_URL, value)
    suspend fun setMdbListApiKey(value: String) = edit(KEY_MDBLIST, value)

    suspend fun setLastTraktSync(timestamp: Long, status: String, error: String? = null) {
        context.apiConfigDataStore.edit {
            it[KEY_LAST_TRAKT_SYNC_AT] = timestamp
            it[KEY_LAST_TRAKT_SYNC_STATUS] = status
            if (error != null) {
                it[KEY_LAST_TRAKT_SYNC_ERROR] = error
            } else {
                it.remove(KEY_LAST_TRAKT_SYNC_ERROR)
            }
        }
    }

    suspend fun setLastEpgSync(timestamp: Long, status: String, error: String? = null) {
        context.apiConfigDataStore.edit {
            it[KEY_LAST_EPG_SYNC_AT] = timestamp
            it[KEY_LAST_EPG_SYNC_STATUS] = status
            if (error != null) {
                it[KEY_LAST_EPG_SYNC_ERROR] = error
            } else {
                it.remove(KEY_LAST_EPG_SYNC_ERROR)
            }
        }
    }

    private fun plainStringFlow(key: Preferences.Key<String>): Flow<String> =
        context.apiConfigDataStore.data.map { it[key].orEmpty() }

    private fun stringFlow(key: Preferences.Key<String>): Flow<String> =
        context.apiConfigDataStore.data.map { decrypt(it[key]).orEmpty() }

    private fun decrypt(value: String?): String? = if (value.isNullOrBlank()) {
        value
    } else {
        encryptedSettingsManager.decrypt(value) ?: value
    }

    private suspend fun edit(key: Preferences.Key<String>, value: String) {
        val stored = if (key in SENSITIVE_STRING_KEYS && value.isNotBlank()) {
            encryptedSettingsManager.encrypt(value) ?: value
        } else {
            value
        }
        context.apiConfigDataStore.edit { it[key] = stored }
    }

    private suspend fun edit(key: Preferences.Key<Boolean>, value: Boolean) {
        context.apiConfigDataStore.edit { it[key] = value }
    }

    suspend fun setHealthStatuses(value: String) = edit(KEY_HEALTH_STATUSES, value)

    companion object {
        private val KEY_TMDB = stringPreferencesKey("tmdb_api_key")
        private val KEY_ANILIST = stringPreferencesKey("anilist_token")
        private val KEY_TRAKT_ID = stringPreferencesKey("trakt_client_id")
        private val KEY_TRAKT_SECRET = stringPreferencesKey("trakt_client_secret")
        private val KEY_TRAKT_ACCESS = stringPreferencesKey("trakt_access_token")
        private val KEY_TRAKT_REFRESH = stringPreferencesKey("trakt_refresh_token")
        private val KEY_DEBRID = stringPreferencesKey("debrid_api_key")
        private val KEY_DEBRID_ACCESS = stringPreferencesKey("debrid_access_token")
        private val KEY_DEBRID_REFRESH = stringPreferencesKey("debrid_refresh_token")
        private val KEY_DEBRID_PROVIDER = stringPreferencesKey("debrid_provider")
        private val KEY_IPTV = stringPreferencesKey("iptv_source_urls")
        private val KEY_EPG = stringPreferencesKey("epg_url")
        private val KEY_STREMIO = stringPreferencesKey("stremio_addons")
        private val KEY_KODI_URL = stringPreferencesKey("kodi_url")
        private val KEY_WEB_SOURCE_CONFIG = stringPreferencesKey("web_source_config")
        private val KEY_CLOUDSTREAM_REPOS = stringPreferencesKey("cloudstream_repo_urls")
        private val KEY_JELLYFIN_URL = stringPreferencesKey("jellyfin_url")
        private val KEY_JELLYFIN_TOKEN = stringPreferencesKey("jellyfin_token")
        private val KEY_PLEX_URL = stringPreferencesKey("plex_url")
        private val KEY_PLEX_TOKEN = stringPreferencesKey("plex_token")
        private val KEY_EMBY_URL = stringPreferencesKey("emby_url")
        private val KEY_EMBY_TOKEN = stringPreferencesKey("emby_token")
        private val KEY_FORCE_TRANSCODE = booleanPreferencesKey("force_transcode")
        private val KEY_MAX_DIRECT_PLAY_BITRATE = stringPreferencesKey("max_direct_play_bitrate")
        private val KEY_SUBSONIC_URL = stringPreferencesKey("subsonic_url")
        private val KEY_SUBSONIC_USER = stringPreferencesKey("subsonic_user")
        private val KEY_SUBSONIC_PASSWORD = stringPreferencesKey("subsonic_password")
        private val KEY_PODCAST_FEEDS = stringPreferencesKey("podcast_feeds")
        private val KEY_DEEZER_PROXY_URL = stringPreferencesKey("deezer_proxy_url")
        private val KEY_MDBLIST = stringPreferencesKey("mdblist_api_key")
        private val KEY_LAST_EPG_SYNC_AT = longPreferencesKey("last_epg_sync_at")
        private val KEY_LAST_EPG_SYNC_STATUS = stringPreferencesKey("last_epg_sync_status")
        private val KEY_LAST_EPG_SYNC_ERROR = stringPreferencesKey("last_epg_sync_error")
        private val KEY_LAST_TRAKT_SYNC_AT = longPreferencesKey("last_trakt_sync_at")
        private val KEY_LAST_TRAKT_SYNC_STATUS = stringPreferencesKey("last_trakt_sync_status")
        private val KEY_LAST_TRAKT_SYNC_ERROR = stringPreferencesKey("last_trakt_sync_error")
        private val KEY_HEALTH_STATUSES = stringPreferencesKey("health_statuses")

        private val SENSITIVE_STRING_KEYS = setOf(
            KEY_TMDB,
            KEY_ANILIST,
            KEY_TRAKT_ID,
            KEY_TRAKT_SECRET,
            KEY_TRAKT_ACCESS,
            KEY_TRAKT_REFRESH,
            KEY_DEBRID,
            KEY_DEBRID_ACCESS,
            KEY_DEBRID_REFRESH,
            KEY_JELLYFIN_TOKEN,
            KEY_PLEX_TOKEN,
            KEY_EMBY_TOKEN,
            KEY_SUBSONIC_PASSWORD,
            KEY_MDBLIST
        )
    }
}
