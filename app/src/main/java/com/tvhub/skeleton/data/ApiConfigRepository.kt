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
    }
}
