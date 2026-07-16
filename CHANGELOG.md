# Changelog

All notable changes to Polish Media Hub are documented in this file.

## [Unreleased]

### Added

#### Centered D-Pad focus scrolling, full spoiler blur and player quick settings
- **Centered TV focus scrolling** (`TvBringIntoViewSpec`)
  - Added `TvBringIntoViewSpec` implementing `BringIntoViewSpec` with a centered-child layout and a smooth `SpringSpec` animation.
  - Wrapped `LazyRow` in `CategoryRow` and `LazyVerticalGrid`/`LazyColumn` in `LibraryScreen`, `WatchlistScreen`, `CustomListDetailScreen` and `CustomListsScreen` with `TvBringIntoViewProvider`, so D-Pad focus stays in the center of the screen instead of hitting the edges.
- **Full spoiler blur for unwatched episodes** (`DetailScreen`, `DetailViewModel`)
  - When `spoilerBlurEnabled` is on and the episode is not in `WatchHistoryRepository` for the current `profileId`, both the poster (`AsyncImage` with `Modifier.blur(16.dp)`) and the title are hidden.
  - The original title is replaced by `stringResource(R.string.episode_title, ...)` (or `spoiler_hidden_title` for items without an episode number).
  - D-Pad Center/SELECT on the poster/title tile reveals title, poster and description for the current session.
- **Player quick settings overlay** (`PlayerQuickSettingsOverlay`, `PlayerControls`, `PlayerScreen`, `UniversalVlcPlayer`, `PlayerViewModel`, `PlayerViewModelTest`)
  - Added a dedicated gear icon (`Icons.Default.Settings`) in `PlayerControls`.
  - Opens a bottom `PlayerQuickSettingsOverlay` with toggles for Night mode (`LoudnessEnhancer`), audio preference (`Lektor` / `Dubbing`) and player engine (`ExoPlayer` / `LibVLC`).
  - Overlay state is subscribed with `collectAsStateWithLifecycle()` and closing it returns focus to the progress `Slider` via `FocusRequester`.
  - `PlayerViewModel` exposes `toggleNightModeEnabled()`, `cyclePreferredAudioType()` and `toggleUseAlternativePlayer()` which write to `SettingsRepository`.
  - Unguarded `Log` calls in `PlayerViewModel` and `UniversalVlcPlayer` are now wrapped with `if (BuildConfig.DEBUG)` so audio session parameters and URLs are never logged in release builds.
  - Extended `PlayerViewModelTest` with MockK tests verifying the three quick-setting toggles call the correct `SettingsRepository` setters.

#### Smart Home, In-App PiP & OLED protection (final code freeze)
- **Home Assistant Smart Cinema webhooks** (`ApiConfigRepository`, `SettingsScreen`, `AdminHttpServer`, `HomeAssistantWebhookClient`, `PlayerViewModel`, `PlayerViewModelTest`)
  - Encrypted DataStore preferences `homeAssistantUrl`, `homeAssistantToken`, `homeAssistantWebhookEnabled` in `ApiConfigRepository`.
  - `SettingsScreen` section with toggle, Home Assistant URL input and masked webhook token.
  - Wireless admin panel `/api/config` exposes the same three fields with the token masked.
  - `OkHttpHomeAssistantWebhookClient` sends `POST {url}/api/webhook/{token}` with JSON `{"event":"play|pause|stop","profile":"...","media":"..."}` on `Dispatchers.IO` using the global `OkHttpClient`.
  - `PlayerViewModel` triggers webhooks from `setIsPlaying` (play/pause) and `finishPlayback` (stop) for both ExoPlayer and LibVLC paths.
  - Webhook URL and token are never logged to Logcat in release builds (`BuildConfig.DEBUG == false`).
  - `PlayerViewModelTest` verifies `play`, `pause` and `stop` webhook calls with MockK.
- **In-App Video Picture-in-Picture on HomeScreen** (`VideoPipManager`, `InAppVideoPipOverlay`, `PlayerScreen`, `TVNavHost`, `MainActivity`)
  - Activity-scoped `VideoPipManager` singleton keeps the active `ExoPlayer` instance and `MediaItem` across navigation.
  - Pressing BACK from `PlayerScreen` preserves the player, exits the screen and slides up a rounded mini-player in the bottom-right corner of `HomeScreen`.
  - Video and audio continue without interruption; the mini-player uses a small `PlayerView` bound to the same `ExoPlayer`.
  - D-Pad Center/SELECT on the mini-player navigates back to `PlayerScreen`, which re-attaches the same player.
  - Stop button in the mini-player fully releases the player (`VideoPipManager.stopAndRelease()`).
  - `MainActivity.onDestroy()` calls `videoPipManager.stopAndRelease()` to prevent leaks when the app is killed.
  - `HomeScreen` hides the audio mini-player while the video PiP is active so the two bottom bars do not overlap.
  - `collectAsStateWithLifecycle()` is used for all video-PiP state subscriptions (Zasada 4).
- **OLED Burn-In Saver** (`OledBurnInSaver`, `TVNavHost`)
  - Root-level D-Pad idle listener in `TVNavHost` resets a 5-minute (300 000 ms) timer on every key press.
  - If the timer expires and no video is playing (`PlayerViewModel.isPlaying` and `VideoPipManager.isInPipMode` are false), the saver overlays the screen with an 85% dim, a minimalist digital clock and slowly moving movie posters pulled from Coil's disk/memory cache.
  - Any D-Pad press immediately closes the overlay and restores full brightness.
  - Posters and clock use `collectAsStateWithLifecycle()` and `LocalLocale` to satisfy Compose lint.
- **Final code hardening & freeze**
  - Audited new workers, HTTP servers and player components for `release()` / `finally` correctness.
  - Removed temporary `Log.d` debug entries from `HomeViewModel` and left only `BuildConfig.DEBUG`-guarded logging elsewhere.
  - Verified `./gradlew clean test :app:compileDebugKotlin :app:lintDebug` reports `No issues found`.

#### UI / UX
- **Background Source Health-Check Engine** (`HealthCheckWorker`, `HealthCheckEngine`, `HealthStatus`, `SourceHealth`)
  - `CoroutineWorker` runs every 4 hours on `Dispatchers.IO` and checks reachability of all configured sources (Kodi, IPTV M3U, EPG, Plex, Jellyfin, Emby, Subsonic, Deezer proxy, podcast feeds, Stremio addons, Cloudstream repos and web source base URLs).
  - Uses source-specific lightweight endpoints (`JSONRPC.Version`, `/System/Info/Public`, `/identity`, `/rest/ping.view`, etc.) with a 3-second timeout.
  - Per-source status (`ONLINE`, `OFFLINE`, `UNCONFIGURED`) is stored as JSON in DataStore and displayed as colored dots in `SettingsScreen` (`SourceHealthSection`) and in the wireless admin panel.
  - All HTTP connections and the dedicated `OkHttpClient` dispatcher/connection pool are cleaned up in `finally`; no URLs or credentials are logged in release builds.
- **Shared Element Transitions for movie posters** (`TVNavHost`, `TVCard`, `DetailScreen`)
  - `NavHost` is wrapped in a `SharedTransitionLayout` to provide a `SharedTransitionScope` to every screen.
  - `MediaCard` and the detail main poster use `Modifier.sharedElement` with `rememberSharedContentState(key = "poster_${item.id}")` and the `AnimatedVisibilityScope` from `composable` destinations so the poster animates smoothly from the grid/row to the detail screen and back during D-Pad navigation.
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

#### Android TV / Google TV integration
- **Per-Profile Watch Next & Dynamic Preview Channels** (`TvLauncherManager`, `RecommendationsWorker`, `WatchNextHelper`, `TvLauncherManagerTest`)
  - `TvLauncherManager` now observes `ProfileRepository.currentProfile` and, on every profile switch, clears all system launcher tiles on `Dispatchers.IO`, then repopulates them with data scoped to the active profile.
  - Unfinished resume sessions are pulled from `WatchHistoryRepository` and watchlist items from `SavedMediaRepository` for the current `profileId`.
  - All candidates pass through `ContentFilter` with the active profile's `maxAgeRating` and `allowNsfw` limits; items without a rating are rejected for child profiles (fail-closed).
  - Watch Next row entries are published as `WatchNextProgram` with type `CONTINUE` for resume positions and `WATCHLIST` for queued items.
  - Preview/recommendations channel is rebuilt from `FederatedMediaRepository.featured()` (MDBList, Kitsu, home-cloud sources), filtered to movies/series/episodes and re-filtered by `ContentFilter`.
  - `RecommendationsWorker` repeat interval changed from 24 h to 12 h and constrained to `NetworkType.CONNECTED` + `requiresBatteryNotLow`; it uses `ExistingPeriodicWorkPolicy.UPDATE` so the new interval replaces old schedules.
  - `TVHubApplication` now calls `tvLauncherManager.start()` after scheduling workers; the manager no longer auto-starts in `init`, which improves testability.
  - All `ContentResolver` cursor operations use `.use`, helper delete/insert calls run on `Dispatchers.IO`, and `ContentFilter` rejects unallowed entries before any IPC occurs.
  - No `profileId` or media metadata is logged in release builds (`BuildConfig.DEBUG == false`).
  - Added `TvLauncherManagerTest` (JUnit) verifying `isUnfinished`, `buildWatchNextItems`, `buildWatchlistItems` and `buildPreviewItems` filtering for child and adult profiles.

#### Cloud crash reporting
- **Secured Cloud Crash Reporting Engine** (`CrashReportActivity`, `CrashReportSanitizer`, `GlobalExceptionHandler`, `cloudflare-resolver/index.ts`)
  - Cloudflare Worker now exposes `POST /report-error` endpoint protected by `X-Hub-Token`; it appends a timestamp and emits the sanitized stacktrace to Cloudflare Observability logs (and optionally to a bound `CRASH_REPORTS` KV namespace).
  - `CrashReportActivity` adds a D-Pad focused **Send report to cloud** `TvButton` that reads `last_crash.txt`, redacts API keys, OAuth tokens and premium credentials via `CrashReportSanitizer`, and posts the result to the Worker's `/report-error` endpoint.
  - `GlobalExceptionHandler` passes the current `cloudflareWorkerUrl` and `cloudflareAuthToken` to `CrashReportActivity` via intent extras so the `:crashreport` process does not need to access DataStore.
  - `CrashReportSanitizer` runs offline on the device before any network call, replacing detected secrets with `****`.
  - A lightweight, one-shot `OkHttpClient` is used for the upload; the response body and the dispatcher/connection pool are explicitly closed/cleaned up to prevent resource leaks in the `:crashreport` process.
  - No Cloudflare auth token, worker URL or crash report metadata is logged to Logcat in release builds (`BuildConfig.DEBUG == false`).
  - Added `CrashReportSanitizerTest` (JUnit) verifying masking of API keys, query parameters, JSON values, `Authorization` headers and source-specific credentials.

#### Developer Console / Plugin Sandbox
- **Live JS / JSON / Python sandbox in the wireless admin panel** (`SandboxEngine`, `SandboxResult`, `AdminHttpServer`, `KodiMediaSource`, `QuickJsEngine`)
  - New protected endpoint `POST /api/plugin/test?token=...&format=js|json|python`.
  - **JS mode**: evaluates QuickJS snippets in `SandboxEngine` using `QuickJsEngine`; `httpFetch` / `httpFetchText` globals are available, and `QuickJsEngine.evaluateWithError` surfaces syntax/runtime errors to the UI.
  - **JSON mode**: parses the pasted payload with `kotlinx.serialization.json.Json.parseToJsonElement` and tries to map it to the production `MediaItem` model, returning `MediaItem structure valid` with a compact preview or a validation error.
  - **Python mode**: base64-encodes the pasted `.py` script, sends it to the configured Kodi instance via `Files.WriteFile` into `special://home/addons/plugin.video.fanfilm/test_scraper.py`, then invokes `XBMC.RunScript` and returns the JSON-RPC result. Python is executed by Kodi, not on the Android TV.
  - Wireless admin page now has a **Developer Console** section with a CodeMirror editor (loaded from cdnjs) supporting line numbers, syntax highlighting and mode switching for JS/JSON/Python, plus a dynamic action button label (`Testuj w QuickJS`, `Waliduj strukturę JSON`, `Uruchom skrypt i debuguj w Kodi`).
  - `SandboxEngineTest` (JUnit) verifies JSON validation (valid, malformed, missing required fields) and Python RPC orchestration (success, `Files.WriteFile` error, empty `XBMC.RunScript` response).
  - ProGuard keep rules added for `com.polishmediahub.app.data.admin.**` and `com.polishmediahub.app.data.plugin.**`.

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
- **Smart Audio Track Selector & Native Loudness Enhancer** (`PlayerScreen`, `PlayerViewModel`, `UniversalVlcPlayer`, `SettingsScreen`, `AdminHttpServer`)
  - New DataStore preferences `preferredAudioType` (Polish Lektor / Dubbing), `nightModeEnabled` and `dialogueBoostGainmB` (0–3000 mB).
  - ExoPlayer `DefaultTrackSelector` now scores Polish (`pl`/`pol`) audio tracks; Dubbing mode prefers multichannel codecs (`5.1`, `E-AC3`, `DTS`), Lektor mode prefers stereo/mono tracks flagged as lektor; `ROLE_FLAG_DESCRIBES_VIDEO` (Audio Description) always gets the lowest priority.
  - The same scoring is mirrored to `UniversalVlcPlayer` (`mediaPlayer.audioTracks` / `setAudioTrack`) for full player engine parity.
  - Native `android.media.audiofx.LoudnessEnhancer` is bound to the ExoPlayer `audioSessionId`; when Night Mode is enabled it applies `setTargetGain(dialogueBoostGainmB)` to flatten dynamic range and boost quiet dialogue. The effect is released on player close, pause and LibVLC switch.
  - Added "Premium Audio" section to `SettingsScreen` and the wireless admin panel (toggle, slider, selector) with `collectAsStateWithLifecycle()`.
  - Audio session IDs are logged only in `BuildConfig.DEBUG`; no audio ID leaks in release builds.
- **Home poster pre-fetch & offline image cache** (`HomePreFetchWorker`, `NetworkModule`, `TVCard`, `AsyncImage`)
  - `HomePreFetchWorker` is a `@HiltWorker` `CoroutineWorker` that runs every 12 hours with `NetworkType.UNMETERED`, `requiresDeviceIdle` and `requiresBatteryNotLow` constraints, and is also triggered immediately after a successful Trakt sync.
  - It queries `MediaRepository.featured()` and all home `Category` lists, collects unique `posterUrl` and `backdropUrl` values, then calls `ImageLoader.execute(ImageRequest.Builder(...).data(url).build())` to warm Coil's persistent disk cache.
  - `TVHubApplication` implements `ImageLoaderFactory` and builds a singleton `ImageLoader` with a 100 MB `DiskCache` (`File(cacheDir, "image_cache")`), `diskCachePolicy(ENABLED)` and `memoryCachePolicy(ENABLED)`.
  - Every `AsyncImage` in `TVCard`, `HomeScreen`, `DetailScreen`, `PlayerScreen`, `EpgScreen`, `ModernSidebarBlurPanel` and `Sidebar` now uses an explicit `ImageRequest` with `diskCachePolicy(ENABLED)` and `memoryCachePolicy(ENABLED)` so posters render instantly from cache.
  - URL logging inside the worker is blocked in release builds (`BuildConfig.DEBUG == false`).
  - Added `HomePreFetchWorkerTest` with MockK, verifying URL extraction and `ImageLoader.execute()` invocations.

#### Sources
- **Cloudflare Edge Offloading Engine** (`cloudflare-resolver` Worker + `WebMediaSource` integration)
  - New `cloudflare-resolver/` TypeScript/Wrangler subproject deployed as a Cloudflare Worker on `https://*.workers.dev/resolve`.
  - The Worker accepts a `?url=` query parameter and optional `?headers=` JSON, validates `X-Hub-Token` against a Worker secret (`HUB_TOKEN`), fetches the target page, and extracts a stream URL using the same P.A.C.K.E.R unpacker, CDA decoder and media-regex logic as the Android app.
  - `WebMediaSource` now reads `useCloudflareBypass`, `cloudflareWorkerUrl` and `cloudflareAuthToken` from `ApiConfigRepository` (encrypted DataStore).
  - When Cloudflare bypass is enabled, `WebMediaSource.resolve()` calls the Worker first; on any network error, timeout, 5xx or 403 it transparently falls back to the local Kotlin resolver (`JsUnpacker`, `CdaDecoder`, `QuickJsEngine`).
  - Resolved stream headers returned by the Worker are merged into `MediaItem.headers` in `resolveItem()` so ExoPlayer / LibVLC receive the correct `Referer` / `User-Agent`.
  - Configuration UI added to `SettingsScreen` and the wireless `AdminHttpServer` QR panel (toggle, Worker URL, masked auth token); sensitive token is masked in `serveConfig`.
  - Added `WebMediaSourceTest` with MockWebServer verifying Worker routing, `X-Hub-Token` header, header merging and local fallback behavior.
- **MDBList integration** (`MdbListMediaSource`, `MdbListApi` models)
  - New federated `MediaSource` bound into `SourceRegistry`.
  - Loads public top lists (`/lists/top`), authenticated user lists (`/lists/user`) and list items (`/lists/{id}/items`).
  - Media search via `/search/any` and cross-ID lookup via `/{provider}/{type}/{id}` (imdb/tmdb/trakt/tvdb).
  - Every `MediaItem` carries `tmdbId`, `imdbId` and `traktId` for matching with Stremio, Plex, Jellyfin, Trakt, etc.
  - Uses the global `OkHttpClient` and Kotlinx Serialization `@Serializable` models.
- **MDBList starter package** in `legal_sources.json` and `LegalSourcesRepository` (`MdbListStarter`, `MdbListStarterEntry`).
- **MDBList API key** configuration in `ApiConfigRepository`, `SettingsScreen` (masked input) and `AdminHttpServer` QR admin panel.
- **Kitsu anime fallback** (`KitsuMediaSource`, `AnimeRepository`)
  - New `MediaSource` using `https://kitsu.io/api/edge` and Kotlinx Serialization `@Serializable` models; Hilt-injected global `OkHttpClient`.
  - `AnimeRepository` wraps `AniListMediaRepository` and `KitsuMediaSource`; if AniList returns empty (network error, timeout, 429), it silently falls back to Kitsu.
  - Maps Kitsu anime to `MediaItem` with `malId` and `aniListId` parsed from `include=mappings` relationships.
  - Added a `kitsu` entry to `legal_sources.json`.
- **Filmweb.pl metadata fallback** (`FilmwebMediaSource`, `FederatedMediaRepository`, `FilmwebCacheRepository`, Room v14)
  - New `FilmwebMediaSource` implementing `MediaSource` and using the global `OkHttpClient` with `MemoryCookieJar` and `CloudflareBypassInterceptor`.
  - Searches `www.filmweb.pl/api/v1/films/search` and `/live/search`, then fetches title, Polish plot, poster, rating and vote count from `/film/{id}/info`, `/description`, `/preview` and `/rating`.
  - `FederatedMediaRepository.enrichWithFilmweb(item)` is called from `DetailViewModel` on `Dispatchers.IO` when the TMDB description is empty, too short or lacks Polish diacritics.
  - Enriched metadata (description, poster, `filmwebRating`, `filmwebVoteCount`, `filmwebUrl`) is cached in the new `filmweb_cache` Room table via `FilmwebCacheRepository` for instant future detail loads.
  - `DetailScreen` displays a `Filmweb: X.X` label next to the year/duration/rating row when Filmweb data is available.

#### Security
- **Encrypted sensitive settings** (`EncryptedSettingsManager`, `ApiConfigRepository`)
  - AES-256-GCM encryption using a hardware-backed key generated in the Android Keystore (`AndroidKeyStore`).
  - A random 12-byte IV is generated per encryption, prepended to the ciphertext and Base64-encoded.
  - Sensitive values (TMDB, AniList, Trakt, Debrid, Jellyfin/Plex/Emby tokens, Subsonic password, MDBList API key) are encrypted before being written to DataStore and decrypted on read. Plain preferences (dark theme, quality, EPG sync status, etc.) remain unencrypted.
- **Parental Control System**
  - `ProfileEntity` extended with `maxAgeRating` (e.g. G, PG, PG-13, R, NC-17, 7/12/16/18) and `allowNsfw` boolean.
  - Room v12 migration (`migrationFromV11toV12`) adds `maxAgeRating` and `allowNsfw` columns to the `profiles` table with a safe fallback default.
  - New `ContentFilter` utility filters `MediaItem` lists and categories by active profile; applied in `CompositeMediaRepository`, `FederatedMediaRepository` and `PluginMediaSource` for `search()`, `categories()` and `featured()`.
  - PIN-protected **Parental Control** section in `SettingsScreen` lets the admin set the maximum age rating and NSFW allowance per household profile.

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

#### Trakt.tv two-way sync engine (`TraktSyncWorker`, `TraktMediaRepository`)
- `TraktSyncWorker` is a `@HiltWorker` scheduled every 6 hours via `WorkManager` with `NetworkType.CONNECTED` and `requiresBatteryNotLow` constraints.
- Pulls watched movies/shows/episodes from `/sync/watched` and the watchlist from `/sync/watchlist` into per-profile Room tables.
- Pushes local `WatchHistoryRepository` and `SavedMediaRepository` changes back to Trakt using `/sync/history` and `/sync/watchlist`; timestamp-based conflict resolution keeps the newer record.
- `PlayerViewModel` sends `/scrobble/start`, `/scrobble/pause` and `/scrobble/stop` requests with exact percent progress on ExoPlayer state changes.
- Last sync timestamp/status/error are exposed through `ApiConfigRepository` and shown in `SettingsScreen` and `AdminHttpServer`.

#### Cinema Dimming Mode (`PlayerScreen`, `PlayerViewModel`)
- `cinemaMode` toggle in `SettingsRepository` / `SettingsScreen` and the wireless admin panel.
- When `isPlaying == true` the player auto-hides overlays after a short inactivity delay and dims the entire screen with an animated black overlay.
- On pause or D-Pad interaction the overlay fades back in and an info card below the slider shows the title, description, genres and top 5 cast members fetched in the background from TMDB / Trakt metadata.
- `TmdbMediaRepository.credits(...)` queries `/movie/{id}/credits` and `/tv/{id}/credits`.

#### Trakt.tv Device Code OAuth (`TraktAuthManager`, `TraktPairingViewModel`, `TraktPairingSection`)
- Replaces manual token pasting with the official device-code flow (`/oauth/device/code` and `/oauth/device/token`).
- User enters their Trakt client ID and secret once; the app requests a device code and displays the `user_code`, expiry countdown and a QR code linking to the activation URL.
- Polling runs on `Dispatchers.IO` every `interval` seconds until the user authorizes the device.
- The resulting `access_token` and `refresh_token` are encrypted with AES-256-GCM in Android Keystore and saved to `ApiConfigRepository`.
- Pairing wizard is integrated in both `SettingsScreen` and `AdminScreen`.

#### Native Picture-in-Picture Mode (`MainActivity`, `PlayerScreen`, `PlayerViewModel`)
- `MainActivity` declares `android:supportsPictureInPicture="true"` with the required `configChanges`.
- `onUserLeaveHint()` enters PiP when the player is currently playing.
- `onPictureInPictureModeChanged()` propagates the PiP state to `PlayerViewModel`.
- `PlayerScreen` hides controls, subtitles, Nerd Stats, Cinema info card and Next Episode overlay when `isInPipMode == true`, leaving only the clean video stream.
- Returning to fullscreen restores the full control interface.

#### Trakt.tv Token Refresh (`TraktAuthenticator`, `NetworkModule`)
- New `okhttp3.Authenticator` intercepts HTTP 401 responses from Trakt API endpoints.
- It reads the encrypted `refresh_token` from `ApiConfigRepository`, synchronously calls `/oauth/token` with `grant_type=refresh_token`, and encrypts the new `access_token` / `refresh_token` pair back to DataStore.
- The original request is retried with the fresh bearer token, keeping background sync and scrobbling alive without re-pairing.
- `NetworkModule` provides a dedicated `@Named("trakt")` `OkHttpClient` with `TraktAuthenticator` for `TraktApi`; the global `OkHttpClient` keeps the `DebridAuthenticator`.

#### Smart Skip Intro/Outro (`PlayerScreen`, `PlayerViewModel`, `SettingsRepository`)
- `MediaItem` extended with optional `introStartMs`, `introEndMs`, `outroStartMs`, `outroEndMs`; plugins (Stremio/Kodi) can supply exact segment timestamps.
- Fallback defaults are configurable in `SettingsScreen` / Admin panel: intro ends at N seconds, outro starts N seconds before the end.
- During playback a semi-transparent `TvButton` slides in and auto-focuses when the playhead enters an intro or outro segment.
- **Skip intro** seeks the player to the intro end timestamp.
- **Skip credits/outro** immediately triggers the existing Auto-Play Next overlay so the user can play the next episode or cancel.
- Toggle and default durations are persisted in `SettingsRepository` and exposed through `SettingsViewModel`.

#### LibVLC alternative player engine (`UniversalVlcPlayer`)
- New `org.videolan.android:libvlc-all:4.0.0-eap17` dependency.
- `UniversalVlcPlayer` Composable wraps `VLCVideoLayout`, `LibVLC`, `MediaPlayer` and `Media` directly in the app process (no external player installation required).
- `useAlternativePlayer` toggle in `SettingsRepository` / DataStore, exposed in `SettingsScreen` and `AdminHttpServer` web panel.
- `PlayerScreen` conditionally renders `UniversalVlcPlayer` instead of `PlayerContent` when the toggle is enabled.
- HTTP `User-Agent` and per-stream `mediaItem.headers` are injected through `LibVLC` options; a default `PolishMediaHub` User-Agent is added when none is supplied.
- External subtitles (`subtitleUrl`) are attached via `Media.addSlave`; `subtitleHeaders` are passed as per-media `:http-header-fields` options and the first text track is selected on `MediaPlayer.Event.Playing`.
- A 500 ms position loop reads `mediaPlayer.time`/`length` and feeds `PlayerViewModel.updatePosition(...)`, keeping Trakt scrobbling, Room watch history, Auto-Play Next and Smart Skip Intro/Outro functional on the LibVLC engine.
- Solves ExoPlayer decoding issues with DTS/AC3 audio and MKV/AVI containers from torrents, Kodi and web plugins.

#### Final Phase: Player Intelligence, Cloud Profile Sync & Background Updates

- **Black-frame intro/outro detection** (`BlackFrameDetector`, `FrameSampler`, `AudioLevelMonitor`)
  - Pure, unit-testable state machine that detects black (<0.05 luma) and quiet (<-40 dB) frame sequences longer than 1500 ms.
  - Triggers inside the first 10% (intro) or last 15% (outro) of the video duration and feeds `SkipIntroState` in `PlayerViewModel`.
  - ExoPlayer path uses `FrameSampler` (`PixelCopy` on API 24+ or `TextureView.bitmap`) plus `Visualizer` via `AudioLevelMonitor`; LibVLC fallback reports full luma and 0 dB.
  - Explicit `introStartMs`/`introEndMs`/`outroStartMs`/`outroEndMs` markers always take precedence.
  - All native listeners, visualizers and bitmaps are released in `DisposableEffect`/`release()` (Zasada 5).

- **Cloud profile sync** (`CloudProfileSyncWorker`, `CloudProfileSyncClient`, `CloudProfileSyncRestore`, `cloudflare-resolver/index.ts`)
  - `CloudProfileSyncWorker` runs every 12 hours and on demand, zips `media.db` + `media.db-shm` + `media.db-wal` on `Dispatchers.IO` and uploads it to `POST /api/sync-profiles`.
  - `GET /api/sync-profiles` downloads the latest backup; `CloudProfileSyncRestore` applies the staged files before Room opens on the next cold start.
  - Cloudflare Worker endpoint is protected by `X-Hub-Token` and stores the zip in a bound `PROFILE_BACKUPS` KV namespace.
  - No worker URL or auth token is logged to Logcat in release builds (Zasada 6); ProGuard keep rules added for `com.polishmediahub.app.data.remote.cloud.**`.
  - Added `BlackFrameDetectorTest` (JUnit) and `CloudProfileSyncWorkerTest` (JUnit/MockK) verifying the detection logic and upload/retry behavior.
  - Added `ProfileSyncMigrationTest` (instrumented) verifying that profile and watch-history data survive Room database restore/reopen.

- **Background plugin/source updater** (`PluginUpdateWorker`)
  - Runs every 24 hours when the device is idle and battery is not low.
  - Polls plugin manifests and repository indexes (`checkUpdates`/`syncIndexes`); if newer versions are found, clears the `plugins_dex` optimized cache so updated binaries are loaded next time.
  - Stores update count and timestamp in `ApiConfigRepository` / DataStore and shows them as a red badge in `SettingsScreen` and in `AdminHttpServer`.

- **Home audio mini-player** (`AudioMiniPlayer`, `AudioMiniPlayerViewModel`, `AudioRepository`)
  - Slides up at the bottom of `HomeScreen` when `AudioRepository.currentTrack` is active.
  - Shows cover, title, artist and D-Pad Pause/Stop buttons; tapping the bar navigates to `PlayerScreen`.
  - State is collected with `collectAsStateWithLifecycle()` (Zasada 4).
  - `MusicViewModel` and `PlayerViewModel` update `AudioRepository` so the mini-player stays in sync across screens.

- **Settings / Admin panel additions**
  - SettingsScreen sections "Profile Cloud Sync" and "Plugin & Source Updates" with manual triggers, last sync timestamp and status.
  - `AdminHttpServer` endpoints `/api/profile/sync`, `/api/profile/restore` and `/api/plugin/update`.

### Changed
- `TVNavHost` waits for the `isFirstLaunch` value before choosing the `NavHost` start destination, avoiding graph resets.
- `TvLauncherManager` progress writes throttled to 15 seconds during playback, with a forced write on `onPlaybackStopped`.
- `PlayerScreen` now drives subtitle styling and Nerd Stats overlay reactively from dedicated `StateFlow`s.
- `PlayerControls` signature extended with `cinemaMode` and `cinemaInfo` to support the Cinema Dimming info card.
- `AdminHttpServer` now accepts `/api/trakt/sync` and exposes `traktClientSecret`, `traktRefreshToken`, `traktAccessToken`, `lastTraktSyncAt/Status/Error`.
- `ApiConfigRepository` extended with encrypted `traktClientSecret` and `traktRefreshToken` keys.
- `PlayerContent` signature extended with `onUpdatePosition`, `onSkipIntro`, `onSkipOutro`, `onSeekHandled`, `skipIntroState`, `pendingSeekToMs` and `forceAutoPlayOverlay`.
- `NextEpisodeOverlay` accepts a nullable `nextEpisode` and exits early when null.
- `AdminHttpServer` now saves and exposes `autoSkipIntro`, `introEndSeconds` and `outroDurationSeconds` from `SettingsRepository`.
- `MediaItem` extended with `subtitleHeaders` map for per-subtitle authorization headers.
- `QuickJsMediaSource` maps the `subtitleHeaders` field from JS plugins to `MediaItem`.
- `NextEpisodeOverlay` visibility changed from `private` to `internal` so it can be reused by `UniversalVlcPlayer`.
- **Dependency audit (2026-07-15)**
  - `libvlc` updated to `4.0.0-eap24` (latest LibVLC Android EAP on Maven Central as of 2026-07-15, https://central.sonatype.com/artifact/org.videolan.android/libvlc-all/4.0.0-eap24).
  - `haze` updated to `1.7.2` (latest stable, https://github.com/chrisbanes/haze/releases/tag/1.7.2).
  - `zxing` updated to `3.5.4` (latest stable, https://github.com/zxing/zxing/releases).
  - `retrofit` updated to `2.12.0` (latest stable, https://github.com/square/retrofit/releases/tag/2.12.0).
  - `kotlinx-coroutines-test` updated to `1.10.2` (latest stable, https://github.com/Kotlin/kotlinx.coroutines/releases/tag/1.10.2).
  - `appcompat` updated to `1.7.1` (latest stable, https://developer.android.com/jetpack/androidx/releases/appcompat#1.7.1).
  - `navigation-compose` updated to `2.9.8` (latest stable, https://developer.android.com/jetpack/androidx/releases/navigation#2.9.8).

### Fixed

- **TorBox token injection into Kodi FanFilm**
  - `AdminHttpServer.pushAddonSettingsIfKodiConfigured()` now pushes the configured `debridApiKey` to `plugin.video.fanfilm` as both `torbox_token` and `torbox_apikey` when `debridProvider == "torbox"`, mirroring the existing Real-Debrid support.
- **LibVLC player parity with ExoPlayer**
  - `UniversalVlcPlayer` now applies Polish audio/subtitle preferences by scanning `mediaPlayer.getTracks(Audio/Text)` and selecting the best `pl`/`pol` track while deprioritizing Audio Description tracks.
  - Subtitle styling (size, color, vertical offset) from `SettingsRepository` is passed to the LibVLC engine via `--sub-text-scale`, `--freetype-color`, `--freetype-opacity` and `--sub-margin` options.
  - `Nerd Stats Overlay` and `Cinema Dimming Mode` (animated dim overlay + `PlayerControls`/`CinemaInfoCard`) now render over the `VLCVideoLayout` identically to the ExoPlayer path.
- **Admin panel credential leak and CORS hardening**
  - `AdminHttpServer.serveConfig()` masks all decrypted sensitive keys (TMDB, AniList, Trakt, Debrid, Jellyfin/Plex/Emby, Subsonic password, MDBList) before sending JSON, showing only the first 4 and last 4 characters.
  - Wildcard `Access-Control-Allow-Origin: *` is removed; the server now allows cross-origin requests only from the authorized admin IP (`http://<clientIp>:<port>`), rejecting mismatched origins with HTTP 403.
- **Parental Control "fail closed"**
  - `ContentFilter` now rejects any `MediaItem` that lacks a declared age rating when the active profile has `maxAgeRating` set, hiding such content from child profiles instead of allowing it through.
- **Trakt token refresh diagnostics**
  - `TraktAuthenticator` logs `Log.e("TraktAuthenticator", "Krytyczny błąd automatycznego odświeżania sesji OAuth Trakt: ...")` before returning `null` on refresh failure.
- **Dead code cleanup**
  - Removed the unused `CastManager` skeleton and the entire `com.polishmediahub.app.data.cast` package.

- **D-Pad focus restoration on vertical grid screens**
  - `LibraryScreen`, `WatchlistScreen` and `CustomListDetailScreen` now apply `Modifier.focusGroup()` + `focusRestorer()` to their `LazyVerticalGrid`, so focus returns to the last viewed tile after navigating back from the detail screen.
- **Hero banner on Home**
  - `HomeScreen` now uses a dedicated `HeroFeaturedBanner` with a cinematic backdrop, left and bottom black gradients, title, year, TMDB/Filmweb rating badges and a focused D-Pad `TvButton` "Play now ▷".
- **LibVLC runtime diagnostics**
  - Replaced two silent empty `catch {}` blocks in `UniversalVlcPlayer` with explicit `Log.e("UniversalVlcPlayer", ...)` logging so playback errors are visible in logcat.
- **Admin panel plugin endpoint security**
  - `AdminHttpServer` now generates a unique per-process pairing token at startup and embeds it in the QR/admin URL (`/admin?token=...`).
  - All `/api/*` endpoints (including `/api/plugin` GET/POST and `/api/config` GET/POST) reject requests with a missing or invalid `?token=...` with HTTP 403 Forbidden, closing the LAN RCE vector for unauthorized plugin installation.
  - The admin HTML fetches all `/api/*` URLs with the token read from the page query string.

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
- **Trakt.tv API audit — pagination for `sync/watched` and `sync/watchlist`**
  - Added `page` and `limit` query parameters (max 250) to `/sync/watched/movies`, `/sync/watched/shows` and `/sync/watchlist/movies,shows`.
  - `TraktMediaRepository` now reads `X-Pagination-Page-Count` response headers and loops through all pages, so Trakt two-way sync works after the June 30, 2026 pagination enforcement.
  - Updated code references to the current Trakt documentation home at `https://docs.trakt.tv` (the old Apiary docs are deprecated as of June 2026).
  - Added `Modifier.focusGroup()` and `focusRestorer()` to `CategoryRow` horizontal `LazyRow`s so D-Pad focus returns to the last viewed item when moving up/down between rows.
  - Localized hard-coded strings in `AdminScreen`, `EpgScreen` and `DownloadsScreen`; added missing `contentDescription` for plugin reorder arrows and Sidebar selection state.
  - Fixed `IntentFilterUniqueDataAttributes`, `DefaultUncaughtExceptionDelegation`, `UseKtx`, `ModifierParameter`, `PluralsCandidate`, `FrequentlyChangingValue`, `SetJavaScriptEnabled`, `RedundantLabel` and other lint warnings.
  - Added `app/lint.xml` baseline for pre-existing dependency-version and icon-asset warnings; `lintDebug` now reports `No issues found.`
- `WebMediaSource.byId()` now fetches a single page directly instead of scraping the whole catalog.
- `CloudstreamSource.categories()` now loads remote indexes instead of returning an empty list.
- `QuickJsEngine` no longer uses `runBlocking` in `httpFetch`.
- `PlayerViewModel` `AnalyticsListener` overrides updated for ExoPlayer API signatures.
- `AnimeRepository.categories()` now falls back to Kitsu when AniList returns empty categories.
- `ContentFilter` age/NSFW logic simplified: `allowNsfw` blocks adult-flagged items; `maxAgeRating` blocks items with a recognized rating level above the cap.
- **Starter source URL audit (2026-07-15)**
  - Removed dead `LegalStream` IPTV/EPG samples from `legal_sources.json` (repository deprecated, URLs return 404).
  - Replaced Stremio samples with working official add-ons: `Cinemeta` (`https://v3-cinemeta.strem.io/manifest.json`) and `WatchHub` (`https://watchhub-us.strem.io/manifest.json`).
  - Replaced dead NASA `NASAcast_Podcast.rss` sample with the current NASA podcast feed `https://www.nasa.gov/feeds/podcasts/houston-we-have-a-podcast`.
  - Added a working Pluto TV EPG sample `https://i.mjh.nz/PlutoTV/us.xml` for IPTV/EPG onboarding.
  - Verified all remaining starter URLs return HTTP 200 as of 2026-07-15; live stream availability still depends on the broadcaster and must be verified locally.

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
