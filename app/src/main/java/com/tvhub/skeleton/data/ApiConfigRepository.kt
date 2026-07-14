package com.tvhub.skeleton.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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
    @param:ApplicationContext private val context: Context
) {

    val tmdbApiKey: Flow<String> = context.apiConfigDataStore.data.map { it[KEY_TMDB].orEmpty() }
    val aniListToken: Flow<String> = context.apiConfigDataStore.data.map { it[KEY_ANILIST].orEmpty() }
    val traktClientId: Flow<String> = context.apiConfigDataStore.data.map { it[KEY_TRAKT_ID].orEmpty() }
    val debridApiKey: Flow<String> = context.apiConfigDataStore.data.map { it[KEY_DEBRID].orEmpty() }
    val debridAccessToken: Flow<String> = context.apiConfigDataStore.data.map { it[KEY_DEBRID_ACCESS].orEmpty() }
    val debridRefreshToken: Flow<String> = context.apiConfigDataStore.data.map { it[KEY_DEBRID_REFRESH].orEmpty() }
    val debridProvider: Flow<String> = context.apiConfigDataStore.data.map { it[KEY_DEBRID_PROVIDER].orEmpty() }
    val iptvSourceUrls: Flow<String> = context.apiConfigDataStore.data.map { it[KEY_IPTV].orEmpty() }
    val stremioAddons: Flow<String> = context.apiConfigDataStore.data.map { it[KEY_STREMIO].orEmpty() }
    val kodiUrl: Flow<String> = context.apiConfigDataStore.data.map { it[KEY_KODI_URL].orEmpty() }
    val webSourceConfig: Flow<String> = context.apiConfigDataStore.data.map { it[KEY_WEB_SOURCE_CONFIG].orEmpty() }
    val cloudstreamRepoUrls: Flow<String> = context.apiConfigDataStore.data.map { it[KEY_CLOUDSTREAM_REPOS].orEmpty() }
    val jellyfinUrl: Flow<String> = context.apiConfigDataStore.data.map { it[KEY_JELLYFIN_URL].orEmpty() }
    val jellyfinToken: Flow<String> = context.apiConfigDataStore.data.map { it[KEY_JELLYFIN_TOKEN].orEmpty() }
    val plexUrl: Flow<String> = context.apiConfigDataStore.data.map { it[KEY_PLEX_URL].orEmpty() }
    val plexToken: Flow<String> = context.apiConfigDataStore.data.map { it[KEY_PLEX_TOKEN].orEmpty() }
    val embyUrl: Flow<String> = context.apiConfigDataStore.data.map { it[KEY_EMBY_URL].orEmpty() }
    val embyToken: Flow<String> = context.apiConfigDataStore.data.map { it[KEY_EMBY_TOKEN].orEmpty() }
    val subsonicUrl: Flow<String> = context.apiConfigDataStore.data.map { it[KEY_SUBSONIC_URL].orEmpty() }
    val subsonicUser: Flow<String> = context.apiConfigDataStore.data.map { it[KEY_SUBSONIC_USER].orEmpty() }
    val subsonicPassword: Flow<String> = context.apiConfigDataStore.data.map { it[KEY_SUBSONIC_PASSWORD].orEmpty() }

    suspend fun setTmdbApiKey(value: String) = edit(KEY_TMDB, value)
    suspend fun setAniListToken(value: String) = edit(KEY_ANILIST, value)
    suspend fun setTraktClientId(value: String) = edit(KEY_TRAKT_ID, value)
    suspend fun setDebridApiKey(value: String) = edit(KEY_DEBRID, value)
    suspend fun setDebridAccessToken(value: String) = edit(KEY_DEBRID_ACCESS, value)
    suspend fun setDebridRefreshToken(value: String) = edit(KEY_DEBRID_REFRESH, value)
    suspend fun setDebridProvider(value: String) = edit(KEY_DEBRID_PROVIDER, value)
    suspend fun setIptvSourceUrls(value: String) = edit(KEY_IPTV, value)
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
    suspend fun setSubsonicUrl(value: String) = edit(KEY_SUBSONIC_URL, value)
    suspend fun setSubsonicUser(value: String) = edit(KEY_SUBSONIC_USER, value)
    suspend fun setSubsonicPassword(value: String) = edit(KEY_SUBSONIC_PASSWORD, value)

    private suspend fun edit(key: Preferences.Key<String>, value: String) {
        context.apiConfigDataStore.edit { it[key] = value }
    }

    companion object {
        private val KEY_TMDB = stringPreferencesKey("tmdb_api_key")
        private val KEY_ANILIST = stringPreferencesKey("anilist_token")
        private val KEY_TRAKT_ID = stringPreferencesKey("trakt_client_id")
        private val KEY_DEBRID = stringPreferencesKey("debrid_api_key")
        private val KEY_DEBRID_ACCESS = stringPreferencesKey("debrid_access_token")
        private val KEY_DEBRID_REFRESH = stringPreferencesKey("debrid_refresh_token")
        private val KEY_DEBRID_PROVIDER = stringPreferencesKey("debrid_provider")
        private val KEY_IPTV = stringPreferencesKey("iptv_source_urls")
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
        private val KEY_SUBSONIC_URL = stringPreferencesKey("subsonic_url")
        private val KEY_SUBSONIC_USER = stringPreferencesKey("subsonic_user")
        private val KEY_SUBSONIC_PASSWORD = stringPreferencesKey("subsonic_password")
    }
}
