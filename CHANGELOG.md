# Changelog

All notable changes to Polish Media Hub are documented in this file.

## [Unreleased]

### Added
- **Multi-User Profiles**
  - New `ProfileEntity`, `ProfileDao` and `ProfileRepository`.
  - Per-profile `WatchedEntity`, `SavedMediaEntity`, `CustomListEntity` and `CustomListItemEntity` with `profileId` foreign keys.
  - `ProfileRepository.currentProfile` exposed as `StateFlow`, persisted in DataStore.
  - `WatchHistoryRepository`, `SavedMediaRepository` and `CustomListsRepository` filter data by the active profile using `flatMapLatest`.
  - Profile switcher in `Sidebar` with avatar / initial display and PIN-locked profile support.
  - `TvLauncherManager` clears and re-publishes `WatchNextPrograms` on every profile switch.
  - Room database migration from v7 to v8 preserving all existing data under `default_profile`.

- **Documentation**
  - Added `FAQ.md`, `CHANGELOG.md`, `PLUGIN_GUIDE.md` and `ADMIN_PANEL.md`.
  - Updated `README.md` with multi-user profiles and documentation index.

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

- **Android TV / Google TV launcher integration**
  - `TvLauncherManager` + `WatchNextHelper` for the **Watch Next** channel.
  - `PreviewChannelHelper` + `RecommendationsWorker` for app-specific recommendations.
  - Deep links `polishmediahub://play/{id}` and `polishmediahub://detail/{id}`.

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

### Changed
- `TvLauncherManager` progress writes throttled to 15 seconds during playback, with a forced write on `onPlaybackStopped`.

### Fixed
- `WebMediaSource.byId()` now fetches a single page directly instead of scraping the whole catalog.
- `CloudstreamSource.categories()` now loads remote indexes instead of returning an empty list.
- `QuickJsEngine` no longer uses `runBlocking` in `httpFetch`.

## Earlier pre-release history

- Initial Android TV skeleton with Hilt, Room and Jetpack Compose.
- Basic Kodi and IPTV source support.
- Settings, Library and Watchlist screens.
