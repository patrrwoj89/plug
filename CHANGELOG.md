# Changelog

All notable changes to Polish Media Hub are documented in this file.

## [Unreleased]

### Added

#### UI / UX
- **Modern Sidebar Scaffold** (`ModernSidebarBlurPanel`, `Sidebar`)
  - Collapsed floating pill with active profile / section info.
  - Haze blur overlay drawer that does not resize the content window.
  - Auto-collapse after `1500 ms` of inactivity.
  - D-Pad LEFT on the leftmost grid item opens the sidebar.
- **Essential Addon Setup** onboarding (`EssentialSetupScreen`, `EssentialSetupViewModel`)
  - First-launch flag `isFirstLaunch` in `SettingsRepository` / DataStore.
  - Three selectable legal starter packages: Free Internet TV, Music & Podcasts, Public Web Catalogs.
  - Asynchronous `Dispatchers.IO` loading of sample sources from `LegalSourcesRepository` into `ApiConfigRepository`, then navigation to `Home`.
- **Auto-Play Next Overlay** (`PlayerScreen`, `PlayerViewModel`)
  - 15-second countdown when a series episode is about to end.
  - Shows next-episode backdrop, title, season/episode and animated timer.
  - D-Pad buttons "Play now" and "Cancel"; BACK acts like Cancel.
- **Spoiler Blur** (`DetailScreen`, `DetailViewModel`, `SettingsRepository`)
  - `spoilerBlurEnabled` toggle in Settings.
  - Unwatched episode descriptions are blurred with `Modifier.blur(16.dp)`.
  - D-Pad Center/SELECT reveals the description for the current session.

#### Search & live TV
- **Voice Search** (`SearchScreen`, `SearchViewModel`)
  - D-Pad Mic `TvIconButton` next to the search field.
  - Launches `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` with `LANGUAGE_MODEL_FREE_FORM` and Polish (`pl-PL`).
  - Recognized text is inserted into the search field and `SearchViewModel.submitSearch(query)` is called.
- **Background EPG/IPTV cache updater** (`IptvUpdateWorker`)
  - `CoroutineWorker` running every 12 hours and on cold start via `WorkManager`.
  - Fetches configured M3U playlists and XMLTV EPG, parses them and stores channels in the new `ChannelEntity` Room table and programs in the existing `EpgEntity` table.
  - `Constraints`: `NetworkType.UNMETERED`, `requiresDeviceIdle`, `requiresBatteryNotLow`.
  - `EpgViewModel` and `EpgTimelineGrid` now load channels/programs from local Room cache only; the live-TV screen opens instantly without downloading large XML files on entry.
  - Last sync timestamp and status are exposed through `ApiConfigRepository` / DataStore and displayed in `AdminHttpServer` and `SettingsScreen`.

#### Player
- Live subtitle configuration (`SettingsScreen`, `PlayerScreen`, `PlayerViewModel`)
  - Size (`14sp`, `18sp`, `24sp`, `32sp`), color (White / Yellow / Gray) and vertical offset stored in DataStore.
  - Applied to ExoPlayer `SubtitleView` in real time.
- **Nerd Stats Overlay** (`PlayerScreen`, `PlayerViewModel`)
  - `showLoadingStats` toggle in Settings.
  - Real-time panel with video resolution + fps, active video/audio codecs, current bitrate, dropped/jank frames.
  - Isolated `StateFlow` to avoid full-screen recompositions.
- **DRM stream playback** (`KodiMediaSource`, `PlayerScreen`, `MediaItem`)
  - `MediaItem` carries `drmLicenseUrl`, `drmScheme` and `drmHeaders`.
  - ExoPlayer configured with `MediaItem.DrmConfiguration` for Widevine, PlayReady and ClearKey.

#### Sources
- **MDBList integration** (`MdbListMediaSource`, `MdbListApi` models)
  - New federated `MediaSource` bound into `SourceRegistry`.
  - Loads public top lists (`/lists/top`), authenticated user lists (`/lists/user`) and list items (`/lists/{id}/items`).
  - Media search via `/search/any` and cross-ID lookup via `/{provider}/{type}/{id}` (imdb/tmdb/trakt/tvdb).
  - Every `MediaItem` carries `tmdbId`, `imdbId` and `traktId` for matching with Stremio, Plex, Jellyfin, Trakt, etc.
  - Uses the global `OkHttpClient` and Kotlinx Serialization `@Serializable` models.
- **MDBList starter package** in `legal_sources.json` and `LegalSourcesRepository` (`MdbListStarter`, `MdbListStarterEntry`).
- **MDBList API key** configuration in `ApiConfigRepository`, `SettingsScreen` (masked input) and `AdminHttpServer` QR admin panel.

#### Security
- **Encrypted sensitive settings** (`EncryptedSettingsManager`, `ApiConfigRepository`)
  - AES-256-GCM encryption using a hardware-backed key generated in the Android Keystore (`AndroidKeyStore`).
  - A random 12-byte IV is generated per encryption, prepended to the ciphertext and Base64-encoded.
  - Sensitive values (TMDB, AniList, Trakt, Debrid, Jellyfin/Plex/Emby tokens, Subsonic password, MDBList API key) are encrypted before being written to DataStore and decrypted on read. Plain preferences (dark theme, quality, EPG sync status, etc.) remain unencrypted.

#### Audio
- **Native podcast RSS parser** (`PodcastRssParser`)
  - Uses Android `XmlPullParser`; extracts `<title>`, `<description>`, `<pubDate>`, `<itunes:image>`, `<itunes:duration>` and `<enclosure>` audio URLs.
  - Full parsing on `Dispatchers.IO`.
- **Internet radio / M3U audio** (`RadioRepository`)
  - Parses `.m3u`, `.m3u8` and `.pls` playlists.
  - Maps stations to `AudioTrack` with `isLive = true` and `MediaItem.Type.AUDIO`.
- **Deezer audio module** (`DeezerAudioSource`)
  - Isolated `AudioSource` (not `MediaSource`, not in `SourceRegistry`) to protect the video federation layer.
  - Hilt-injected global `OkHttpClient` with `MemoryCookieJar` and `CloudflareBypassInterceptor`.
  - `@Serializable` models (`DeezerTrack`, `DeezerArtist`, `DeezerAlbumRef`, `DeezerAlbumDetail`).
  - `Dispatchers.IO` retry loops that silently return empty results on proxy failure.
  - Maps `preview` field to `streamUrl` with ExoPlayer headers.
- **Audio history** (`AudioHistoryEntity`, `AudioHistoryDao`, `AudioHistoryRepository`)
  - Room table filtered by active `profileId` from `ProfileRepository`.
  - `PlayerViewModel` saves playback position for podcasts and resolves audio items via `AudioRepository`.

#### Kodi
- `KodiMediaSource` extensions
  - DRM metadata parsing from `Files.PrepareDownload` / `Player.Open` responses.
  - `setAddonSetting(addonId, settingId, value)` calling `Settings.SetSettingValue` to push Real-Debrid / Trakt tokens to Kodi plugins.
  - `getPluginDirectory(directoryPath)` via `Files.GetDirectory` mapped to `MediaItem` folders/items.
- **Kodi LAN discovery** (`KodiDiscoveryManager`)
  - Android NSD listener for `_xbmc-jsonrpc._tcp`.
  - Auto-updates `KodiMediaSource` and `ApiConfigRepository` with discovered IP/port.
- Manifest permissions `ACCESS_WIFI_STATE` and `CHANGE_WIFI_MULTICAST_STATE`.

#### Plugins & dynamic loading
- **Dynamic binary plugin loading** (`DynamicPluginLoader`, `ReflectiveMediaSource`, `PluginRepository`)
  - Cloudstream (`.cs3` / `.cs4`) and Aniyomi (`.apk`) plugins loaded via `DexClassLoader`.
  - Optimized DEX output path under `codeCacheDir/plugins_dex`.
  - Models for `index.min.json`, `repo.json`, `plugins.json`, `AniyomiExtension` and `CloudstreamPluginMetadata`.
  - Reflexive adapter for plugin classes not implementing the app's `MediaSource` directly.
  - Network injection: loaded plugins use the global `OkHttpClient` and pass headers to ExoPlayer's `DefaultHttpDataSource.Factory`.
  - Cleanup of optimized DEX files on disable/remove.

#### Network / anti-bot
- **Cloudflare bypass stack** (`MemoryCookieJar`, `HeadlessWebSolver`, `CloudflareBypassInterceptor`)
  - In-memory `CookieJar` synchronized per host.
  - Headless `WebView` solver with Chromecast User-Agent, JS/DOM storage, waits for `cf_clearance`.
  - Interceptor handling 403/503, `User-Agent`/`Referer` headers and `X-Set-Referer`.
- **Native P.A.C.K.E.R. unpacker** (`JsUnpacker`) for `eval(function(p,a,c,k,...) )` packed scripts.
- **CDA decoder** (`CdaDecoder`) for `data-video-id` extraction.
- `WebMediaSource` and `NetworkModule` integration: `OkHttpClient` uses the cookie jar + bypass interceptor; CDA and packed JS paths inject headers into `MediaItem` for ExoPlayer.

#### Crash reporting & stability
- **Global Crash Report Center** (`GlobalExceptionHandler`, `CrashReportActivity`)
  - `Thread.UncaughtExceptionHandler` writes `Log.getStackTraceString(throwable)` to `filesDir/last_crash.txt`.
  - Launches `CrashReportActivity` in a separate `:crashreport` process.
  - Kills the failing process with `Process.killProcess` + `System.exit(10)`.
  - Crash reporter UI with dark theme, red warning icon, scrollable stack trace and two D-Pad buttons:
    - **Restart app**
    - **Clear cache and restart** (deletes `cacheDir` contents and `codeCacheDir/plugins_dex`).
  - Handler is not installed inside the `:crashreport` process to prevent recursion.
- `TVHubApplication.onCreate()` registers the handler and skips heavy init in the crash-report process.

#### Profiles & data
- **Multi-User Profiles**
  - `ProfileEntity`, `ProfileDao`, `ProfileRepository`.
  - Per-profile `WatchedEntity`, `SavedMediaEntity`, `CustomListEntity` and `AudioHistoryEntity` with `profileId` foreign keys.
  - `ProfileRepository.currentProfile` exposed as `StateFlow`, persisted in DataStore.
  - Repositories filter reads/writes by active profile using `flatMapLatest`.
  - Profile switcher in `Sidebar` with avatar / initial display and PIN-locked profile support.
  - `TvLauncherManager` clears and re-publishes `WatchNextPrograms` on every profile switch.
  - Room database migrations preserving existing data under `default_profile`.

#### Android TV integration
- `TvLauncherManager` + `WatchNextHelper` for the **Watch Next** channel.
- `PreviewChannelHelper` + `RecommendationsWorker` for app-specific recommendations.
- Deep links `polishmediahub://play/{id}` and `polishmediahub://detail/{id}`.
- Global search `SearchActivity` and `SearchRecentSuggestionsProvider`.

#### Legal starter sources
- `LegalSourcesRepository` reads `assets/legal_sources.json`.
- Added `podcastFeeds`, `deezerProxy` and `webSources` sections.
- Sample sources include public IPTV, EPG, Stremio add-ons, podcast RSS feeds and internet radio.

### Changed
- `TVNavHost` waits for the `isFirstLaunch` value before choosing the `NavHost` start destination, avoiding graph resets.
- `TvLauncherManager` progress writes throttled to 15 seconds during playback, with a forced write on `onPlaybackStopped`.
- `PlayerScreen` now drives subtitle styling and Nerd Stats overlay reactively from dedicated `StateFlow`s.

### Fixed

- **Visual polish and sidebar UX**
  - `MediaCard` and `WideCard` (`TVCard`) now clip `AsyncImage` with `RoundedCornerShape(Radius.md)` to eliminate sharp poster/hero corners.
  - Profile avatars in `Sidebar` and `CollapsedSidebarPill` use `ContentScale.Crop` and `fillMaxSize()` inside circular containers to avoid distortion or empty space.
  - Sidebar menu reorganized into grouped sections: Discover, Library, Multimedia, Downloads and System.
  - PIN-locked profiles display `Icons.Default.Lock` with a localized `contentDescription` instead of the `"🔒"` emoji.
  - Migrated deprecated Haze `hazeChild` calls to `hazeEffect` in `ModernSidebarBlurPanel`.

- **Claude Sonnet 5 audit quality fixes**
  - `LibraryScreen` and `WatchlistScreen` replaced the broken `LazyColumn` + `Modifier.fillParentMaxWidth(0.25f)` layout with a 5-column `LazyVerticalGrid` using `Spacing.md` spacing.
  - `CustomListsScreen` list tiles now navigate to a new `CustomListDetailScreen` (5-column grid + `CustomListDetailViewModel`).
  - All 22 silent empty `catch` blocks in critical files (`PlayerViewModel`, `AdminHttpServer`, `TorrentHttpServer`, `MediaDatabase`, `DynamicPluginLoader`, `PluginRepository`, `TvLauncherManager`, `WatchNextHelper`) now log warnings with `Log.w(...)`.
  - Removed dead `androidx.tv.material` and `androidx.tv.foundation` dependencies from `app/build.gradle.kts` and `libs.versions.toml`; corrected README claims about Jetpack Compose TV material3.
  - Added `Modifier.focusGroup()` and `focusRestorer()` to `CategoryRow` horizontal `LazyRow`s so D-Pad focus returns to the last viewed item when moving up/down between rows.
  - Localized hard-coded strings in `AdminScreen`, `EpgScreen` and `DownloadsScreen`; added missing `contentDescription` for plugin reorder arrows and Sidebar selection state.
  - Fixed `IntentFilterUniqueDataAttributes`, `DefaultUncaughtExceptionDelegation`, `UseKtx`, `ModifierParameter`, `PluralsCandidate`, `FrequentlyChangingValue`, `SetJavaScriptEnabled`, `RedundantLabel` and other lint warnings.
  - Added `app/lint.xml` baseline for pre-existing dependency-version and icon-asset warnings; `lintDebug` now reports `No issues found.`
- `WebMediaSource.byId()` now fetches a single page directly instead of scraping the whole catalog.
- `CloudstreamSource.categories()` now loads remote indexes instead of returning an empty list.
- `QuickJsEngine` no longer uses `runBlocking` in `httpFetch`.
- `PlayerViewModel` `AnalyticsListener` overrides updated for ExoPlayer API signatures.

## [1.0.0-alpha] — 2026-07-14

### Added
- **Federated media source architecture**
  - `MediaSource` contract and `SourceRegistry`.
  - `KodiMediaSource` with JSON-RPC and `image://` path decoding.
  - `CloudstreamSource` with `repo.json` / `plugins.json` loading.
  - `WebMediaSource` with Jsoup scraping, validation, and QuickJS fallback.
  - `QuickJsEngine` + `QuickJsMediaSource` for `.js` plugins.
  - `FederatedMediaRepository` with reactive in-memory caching and `resolve()`.

- **QuickJS integration**
  - Global `httpFetch(url, headersJson)` network bridge returning JSON `{code, body, headers, error}`.
  - Constructor-injected singleton lifecycle with explicit `dispose()` / `close()` for native context cleanup.
  - Plugin headers (`User-Agent`, `Referer`) forwarded to ExoPlayer.

- **BitTorrent streaming**
  - `TorrentEngine` based on `jlibtorrent` with sequential download and piece deadlines.
  - `TorrentHttpServer` local proxy with `Range: bytes=` support.
  - `TorrentInputStream` and UI buffering progress via `StateFlow`.

- **EPG Timeline Grid**
  - Two-directional scrolling grid with frozen channel column.
  - 30-minute time header, duration-proportional programme tiles, current-time marker.
  - D-Pad focus, detail panel with title/year/category/description/progress.

- **Wireless Admin Panel**
  - `AdminHttpServer` served from the TV with dynamic port.
  - ZXing QR code generation for easy phone pairing.
  - Web forms for Kodi, Web, Cloudstream, IPTV/M3U, Jellyfin, Plex, Emby, Subsonic, TMDB, AniList, Trakt, Debrid and podcasts.

- **Security & stability**
  - `network_security_config.xml` and `usesCleartextTraffic` for trusted local/LAN HTTP.
  - `TorrentHttpServer` and `AdminHttpServer` use dynamic ports and `ServerSocket` lifecycle safety.
  - `RetryInterceptor` with HTTP 429 `Retry-After` handling.
  - `DebridAuthenticator` refresh-token flow for Real-Debrid 401 responses.
  - `TorBoxService` user info parsing.
  - Stable Room migrations covering v1→v7 (no destructive fallback).
  - Removed dead `TmdbMediaRepository.kt` and rebased test skeleton package `com.tvhub.skeleton` → `com.polishmediahub.app`.

- **Player experience**
  - Polish audio/subtitle preference (`pl`).
  - Audio Description role deprioritization.
  - Full track labels in `PlayerControls` (e.g. "Polski (Lektor)", "Polski (Dubbing)").
  - HLS, DASH and MP4 stream detection.
  - VTT/SRT subtitle support.

- **UI / UX**
  - Jetpack Compose for TV, `FocusableSurface`, `TVCard`, `WideCard`, sidebar navigation.
  - Search, Home, Detail, Player, Library, Watchlist, Settings, Admin, EPG, Torrents, Music, Downloads and Custom Lists screens.
  - PIN lock for Settings and Admin.
  - Paparazzi snapshot tests and instrumented D-Pad tests.

## Earlier pre-release history

- Initial Android TV skeleton with Hilt, Room and Jetpack Compose.
- Basic Kodi and IPTV source support.
- Settings, Library and Watchlist screens.
