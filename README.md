# Polish Media Hub

A modern Android TV / Google TV media hub built with Jetpack Compose for TV. It aggregates movies, series, live IPTV channels, music and podcasts from user-supplied, legal sources through a unified, D-Pad friendly interface.

The app is **personal-use only** and does **not** ship any pre-bundled pirated content, trackers, unauthorized IPTV playlists or unlicensed scrapers. All streams, torrents and media sources must be added by the user and used in compliance with local law and the source's Terms of Service.

## Features

- **TV-first UI** with `androidx.tv.material3`, D-Pad focus handling, scale/glow outlines and sidebar navigation.
- **Federated media sources**:
  - **Kodi** JSON-RPC integration (movies, TV shows, artwork decoding).
  - **Cloudstream** plugin-style repositories.
  - **QuickJS plugins** (`.js`) with built-in `httpFetch` network bridge, headers and async evaluation.
  - **Web scraping** configuration via JSON with validation and dynamic fallback to QuickJS.
  - **IPTV/M3U** with XMLTV EPG support and a professional **EPG Timeline Grid**.
  - **Jellyfin, Plex, Emby, Subsonic/Airsonic, Stremio, AniList, TMDB, Trakt, podcasts (RSS)** and more.
- **BitTorrent streaming** via `jlibtorrent` with sequential download, local HTTP proxy and buffering UI.
- **EPG Timeline Grid** (Jetpack Compose): bidirectional scrolling, frozen channel column, 30-minute time header, duration-proportional programme tiles, current-time marker, D-Pad focus and top detail panel with progress.
- **Polish audio / subtitle support**: ExoPlayer prefers `pl` tracks, deprioritizes Audio Description, and exposes full language labels in player controls.
- **Wireless admin panel** served by a local HTTP server with QR code (ZXing) for easy source configuration from a phone or computer.
- **Android TV / Google TV launcher integration**:
  - **Watch Next** channel for resume playback.
  - **Preview/recommendations channel** updated daily by `WorkManager`.
- **PIN lock** for Settings and Admin screens.
- **Download support** for audio and video via `WorkManager`.
- **Multi-user profiles** with per-profile history, library, watchlist and custom lists; profile switcher in the sidebar and optional PIN lock per profile.
- **Global Android TV search** with a dedicated `SearchActivity` and `SearchRecentSuggestionsProvider`.
- **Screenshot / Compose Preview tests** with Paparazzi and instrumented D-Pad tests.

## Documentation

- [`README.md`](README.md) — this overview.
- [`FAQ.md`](FAQ.md) — frequently asked questions.
- [`CHANGELOG.md`](CHANGELOG.md) — release history and latest changes.
- [`PLUGIN_GUIDE.md`](PLUGIN_GUIDE.md) — how to write and adapt QuickJS plugins.
- [`ADMIN_PANEL.md`](ADMIN_PANEL.md) — wireless configuration via the local web admin panel and QR code.
- [`LEGAL_SOURCES.md`](LEGAL_SOURCES.md) — curated legal/public/self-hostable sources.

## Tech stack

- Android Gradle Plugin 9.0.0, Gradle 9.1.0, Kotlin 2.3.0
- Jetpack Compose BOM 2026.06.01 (`compose.material3`, `compose.ui`, `tv.foundation`, `tv.material`)
- Hilt 2.60.1 + `androidx.hilt` + `hilt-navigation-compose`
- ExoPlayer / Media3 1.10.1
- Room 2.8.4, DataStore 1.2.1, WorkManager 2.11.2
- OkHttp 4.12.0, Retrofit 2.11.0, Jsoup 1.22.2
- Kotlinx Serialization 1.8.0
- Coil 2.7.0
- jlibtorrent 2.0.12.9 (FrostWire Maven)
- QuickJS wrapper 3.2.3 (`wang.harlon.quickjs:wrapper-android`)
- ZXing 3.5.3 for QR codes
- `androidx.tvprovider:tvprovider:1.1.0` for Android TV channels
- Paparazzi 2.0.0-alpha05 for snapshot tests
- `minSdk = 23`, `targetSdk` set in `app/build.gradle.kts`

## Build

```bash
export ANDROID_HOME=/path/to/android-sdk
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

For instrumented tests (requires an emulator or Android TV device):

```bash
./gradlew :app:connectedDebugAndroidTest
```

APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Project structure

```
app/src/main/java/com/polishmediahub/app/
├── data/                 # Repositories, data sources, persistence
│   ├── local/            # Room entities/DAOs (MediaDatabase, ProfileEntity, etc.)
│   ├── remote/           # TMDB, Trakt, AniList, Jellyfin, Plex, Emby, Subsonic, Stremio, IPTV
│   ├── source/           # Kodi, Web, Cloudstream, FederatedMediaRepository, SourceRegistry
│   ├── plugin/           # QuickJsEngine, QuickJsMediaSource, PluginManifest, PluginRepository
│   ├── torrent/          # TorrentEngine, TorrentHttpServer, TorrentMediaSource
│   ├── iptv/             # EPG parser/repository
│   ├── tv/               # TvLauncherManager, WatchNextHelper, RecommendationsWorker
│   └── ProfileRepository.kt
├── di/                   # Hilt modules
├── model/                # MediaItem, Category and shared data classes
├── navigation/           # Screen sealed class, TVNavHost, TVApp
├── search/               # Android TV search provider / activity
├── ui/
│   ├── components/       # TVCard, FocusableSurface, Sidebar, EmptyState, ErrorState, etc.
│   ├── screens/          # Home, Search, Detail, Player, Settings, Library, Watchlist, Admin, EPG, Torrents, Music, Downloads, Custom Lists
│   ├── theme/            # AppColor, AppTypography, Spacing, Radius, TVHubTheme
│   └── viewmodel/        # Hilt ViewModels
├── MainActivity.kt
└── TVHubApplication.kt
```

## Configuration

Most sources are configured through the **Admin** screen or the wireless web panel:

1. Open **Admin** on the TV.
2. Scan the QR code with your phone or open `http://<TV_IP>:<port>/admin` in a browser.
3. Paste your legal source URLs / config JSON and save.

Supported configurable sources:

- **IPTV**: M3U playlists and XMLTV EPG URLs.
- **Kodi**: JSON-RPC endpoint (`http://host:port/jsonrpc`).
- **Cloudstream**: repository JSON URLs.
- **Web**: JSON config with selectors for title, description, poster, stream link.
- **QuickJS plugins**: manifest URL or `.js` plugin text.
- **Jellyfin / Plex / Emby / Subsonic**: server URL + token/user credentials.
- **Stremio**: addon URLs.
- **TMDB / AniList / Trakt**: API keys / tokens.
- **Podcasts**: RSS feed URLs.
- **Debrid**: OAuth link (stub ready for provider-specific implementation).

Sample legal starter sources can be loaded from the Admin panel via **Load legal sample sources**.

## Multi-User Profiles

The app supports multiple household profiles stored in Room:

- Each profile has `id`, `name`, `avatarUrl`, `isPinLocked` and `pinCode`.
- `WatchedEntity`, `SavedMediaEntity` and `CustomListEntity` carry a `profileId` foreign key.
- `ProfileRepository` exposes `currentProfile` as a `StateFlow` and persists the selected profile in DataStore.
- `WatchHistoryRepository`, `SavedMediaRepository` and `CustomListsRepository` automatically filter reads/writes by the active profile using `flatMapLatest`.
- The top of `Sidebar` shows the active profile (avatar or initial), opens a profile picker, and prompts for the PIN when switching to a locked profile.
- `TvLauncherManager` clears the Android TV **Watch Next** row on profile switch and re-publishes the new profile's history.

## EPG Timeline Grid

The TV Guide screen (`EpgScreen` / `EpgViewModel`) renders a professional two-directional scrolling timeline:

- Channels scroll vertically; time blocks scroll horizontally.
- Left channel column is frozen and shows channel number, name and logo.
- Top header shows time in 30-minute blocks.
- Programme tiles are width-proportional to their duration.
- D-Pad focus smoothly scrolls the timeline and shows a detail panel with title, year, genre, description and progress.
- A red vertical marker indicates the current time and the view auto-positions to "now" on entry.
- `LazyColumn` and `LazyRow` are used to keep performance smooth for large EPG datasets.

## BitTorrent streaming

`TorrentEngine` uses `jlibtorrent` with:

- Sequential download flag enabled for the selected video file.
- First pieces prioritized with deadlines for fast ExoPlayer startup.
- A local `TorrentHttpServer` proxy with `Range: bytes=` support.
- `TorrentInputStream` that blocks until requested pieces are available.
- `TorrentMediaSource.resolve()` returns `http://127.0.0.1:<port>/stream?infoHash=...&file=...` for ExoPlayer.
- UI progress (`TorrentStatus`, buffering percentage) exposed via `StateFlow`.

## QuickJS plugins

Plugins are `.js` files evaluated in a shared `QuickJsEngine`:

- Global `httpFetch(url, headersJson)` returns JSON: `{code, body, headers, error}`.
- `evaluate()` runs on `Dispatchers.IO` to avoid ANRs.
- Constructor-injected singleton lifecycle with explicit `dispose()` to destroy native contexts.
- `QuickJsMediaSource.mapToMediaItem` carries optional HTTP headers (`User-Agent`, `Referer`) through to ExoPlayer's `DefaultHttpDataSource.Factory`.

## Polish audio / subtitle handling

- `DefaultTrackSelector` prefers `pl` for audio and text tracks.
- If multiple Polish audio tracks exist (Lektor, Dubbing, Audio Description), the AD role is deprioritized.
- Full track labels are extracted from Media3 and shown in `PlayerControls` so the user can switch precisely with the D-Pad.

## Android TV launcher integration

- `TvLauncherManager` writes to `TvContractCompat.WatchNextPrograms` and a dedicated `PreviewChannel`.
- `RecommendationsWorker` runs once per day (or on app start) to refresh featured content on the home screen.
- Clicking a launcher tile opens `MainActivity` via deep links (`polishmediahub://play/{id}` or `polishmediahub://detail/{id}`) and resumes playback from the saved position.

## Testing

- Unit tests: `./gradlew :app:testDebugUnitTest`
- Paparazzi snapshot tests / record: `./gradlew :app:recordPaparazziDebug`
- Lint: `./gradlew :app:lintDebug`
- Instrumented UI / D-Pad tests: `./gradlew :app:connectedDebugAndroidTest`

CI (GitHub Actions) builds, lints and runs unit tests on every push; an emulator job is included for future instrumented test runs.

## Legal / responsible use

- This project is for **personal, legal media only**.
- Do not add pirated streams, unauthorized IPTV playlists, copyrighted torrents you do not own the rights to, or scrapers that violate a site's ToS.
- The maintainers do not provide or endorse any specific content sources.
- See [LEGAL_SOURCES.md](LEGAL_SOURCES.md) for a curated list of legal/public/self-hostable starting points.

## License

MIT License — see [LICENSE](LICENSE).
