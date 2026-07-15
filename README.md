# Polish Media Hub

A modern Android TV / Google TV media hub built with Jetpack Compose for TV. It aggregates movies, series, live IPTV channels, music, podcasts and internet radio from user-supplied, legal sources through a unified, D-Pad friendly interface.

The app is **personal-use only** and does **not** ship any pre-bundled pirated content, trackers, unauthorized IPTV playlists or unlicensed scrapers. All streams, torrents and media sources must be added by the user and used in compliance with local law and the source's Terms of Service.

*Polish version / Wersja polska: [`README.pl.md`](README.pl.md)*

## Features

### Core playback & sources

- **TV-first UI** built with Jetpack Compose Foundation and Material3, D-Pad focus handling, scale/glow outlines and the **Modern Sidebar**.
- **Federated media sources** aggregated through `FederatedMediaRepository` and `SourceRegistry`:
  - **Kodi** JSON-RPC integration with DRM stream support, plugin directory browsing (`Files.GetDirectory`), automatic LAN discovery (`KodiDiscoveryManager`) and remote setting updates (`Settings.SetSettingValue`).
  - **Cloudstream** plugin-style repositories and **Aniyomi** `.apk` extensions loaded dynamically via `DexClassLoader`.
  - **QuickJS plugins** (`.js`) with built-in `httpFetch` network bridge, headers and async evaluation.
  - **Web scraping** configuration via JSON with validation and dynamic fallback to QuickJS.
  - **IPTV/M3U** with XMLTV EPG support, local Room cache, background refresh by `IptvUpdateWorker` and a professional **EPG Timeline Grid**.
  - **Jellyfin, Plex, Emby, Subsonic/Airsonic, Stremio, AniList, TMDB, Trakt, MDBList, podcasts (RSS)**, internet radio and Deezer proxy.
  - **MDBList integration** (`MdbListMediaSource`): public top lists, user lists, media search and cross-ID lookup by imdb/tmdb/trakt/tvdb; every item carries `tmdbId`, `imdbId` and `traktId` for matching with other sources.
  - **Kitsu anime fallback** (`KitsuMediaSource`, `AnimeRepository`): when AniList GraphQL fails (network, timeout, 429), the app silently falls back to Kitsu JSON:API. Cross-linked `malId` and `aniListId` are parsed from `include=mappings` to keep stream resolver compatibility.
- **BitTorrent streaming** via `jlibtorrent` with sequential download, local HTTP proxy and buffering UI.
- **Trakt.tv two-way sync** (`TraktSyncWorker`) every 6 hours: pulls watched history and watchlist from Trakt into Room, pushes local Room history/watchlist back to Trakt, with timestamp conflict resolution. ExoPlayer playback sends `/scrobble/start`, `/scrobble/pause` and `/scrobble/stop` with exact percent progress.
- **Music & audio**:
  - Native podcast RSS parser (`PodcastRssParser`) with iTunes tags and enclosure audio URLs.
  - M3U/PLS internet radio repository (`RadioRepository`).
  - Deezer proxy integration through an isolated `DeezerAudioSource`.
  - Per-profile audio history in Room.

### TV UI / UX

- **Modern Sidebar Scaffold**: floating collapsed pill, Haze blur overlay drawer, no layout jitter, auto-collapse after 1500 ms of inactivity, D-Pad LEFT opens the menu from the leftmost item; menu items are grouped into Discover, Library, Multimedia, Downloads and System sections, and PIN-locked profiles show `Icons.Default.Lock` with a localized content description.
- **Essential Addon Setup** onboarding screen for first-launch profiles: one-click loading of legal starter packages (IPTV/EPG, music/podcasts, public web catalogs) via `EssentialSetupScreen` and `EssentialSetupViewModel`.
- **Library, Watchlist and Custom Lists** screens use a 5-column `LazyVerticalGrid` (`CustomListDetailScreen` for the contents of a single custom list).
- **D-Pad focus restoration** in horizontal `LazyRow`s (`CategoryRow`) via `Modifier.focusGroup()` + `focusRestorer()`, so focus returns to the last viewed item when moving between rows.
- **Voice Search** in `SearchScreen`: D-Pad Mic button launches `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` in Polish (`pl-PL`) and inserts the recognized query into the search field.
- **Auto-Play Next** overlay for series: 15-second countdown with next-episode metadata, "Play now" / "Cancel" D-Pad buttons, BACK behaves like Cancel.
- **Spoiler Blur**: unwatched episode plot descriptions are blurred on the detail screen and can be revealed with D-Pad Center/SELECT.
- **Subtitle settings in player**: size, color and vertical offset stored in DataStore and applied live to `SubtitleView`.
- **Nerd Stats Overlay**: optional real-time panel with resolution, fps, active codecs, current bitrate and dropped/jank frames.
- **Rounded media and avatar clipping**: `MediaCard` and `WideCard` clip `AsyncImage` with `RoundedCornerShape(Radius.md)`; profile avatars in `Sidebar` and `CollapsedSidebarPill` use `ContentScale.Crop` and `fillMaxSize()` inside circular containers.

### Player & media

- **ExoPlayer / Media3** with HLS, DASH and progressive stream detection.
- **DRM playback**: `MediaItem` carries `drmLicenseUrl`, `drmScheme` and `drmHeaders`; ExoPlayer is configured with the correct `DrmConfiguration` for Widevine/PlayReady/ClearKey.
- **Polish audio / subtitle support**: ExoPlayer prefers `pl` tracks, deprioritizes Audio Description and exposes full language labels in player controls.
- **External subtitles**: `.vtt` and `.srt` URLs.
- **Cinema Dimming Mode**: when enabled, overlays auto-hide while playing and the screen smoothly dims; on pause an animated info card below the slider shows title, description, genres and top cast members fetched from TMDB/Trakt.
- **Stream headers**: `User-Agent`, `Referer`, `Cookie` and custom headers forwarded to `DefaultHttpDataSource.Factory`.

### Network & anti-bot

- **Cloudflare bypass**: headless `WebView` solver (`HeadlessWebSolver`), `MemoryCookieJar`, `CloudflareBypassInterceptor` and a native P.A.C.K.E.R. unpacker (`JsUnpacker`).
- **CDA decoder** for extracting `data-video-id` from `cda.pl` links.
- Global `OkHttpClient` shared across repositories and plugin code.

### Admin & configuration

- **Wireless admin panel** served by a local HTTP server with QR code (ZXing) for easy source configuration from a phone or computer; the panel and Settings show the last EPG sync timestamp and status.
- **Background EPG/IPTV updater** (`IptvUpdateWorker`) runs every 12 hours (and on cold start) on `Dispatchers.IO` with unmetered/idle/battery-not-low constraints, caches channels and EPG in Room, so the live-TV screen opens instantly.
- **Background Trakt.tv sync** (`TraktSyncWorker`) runs every 6 hours with network/battery constraints. You can also trigger an immediate sync from `Settings` or the admin panel.
- **First-launch onboarding** lets new users pick legal starter source packages, including an MDBList starter package in `legal_sources.json`.

### Android TV integration

- **Watch Next** channel for resume playback.
- **Preview/recommendations channel** updated daily by `WorkManager`.
- **Global search** with a dedicated `SearchActivity` and `SearchRecentSuggestionsProvider`.

### Security, stability & profiles

- **Multi-user profiles** with per-profile history, library, watchlist, custom lists and audio history; profile switcher in the sidebar and optional PIN lock per profile.
- **Parental Control** per profile: `maxAgeRating` and `allowNsfw` flags stored in `ProfileEntity` (Room v12). `ContentFilter` removes items whose age rating exceeds the profile cap or that are flagged as adult/NSFW before they reach `search()`, `categories()` and `featured()` results in `CompositeMediaRepository`, `FederatedMediaRepository` and `PluginMediaSource`. Managed in the PIN-protected **Parental Control** section of `SettingsScreen`.
- **PIN lock** for Settings and Admin screens.
- **Download support** for audio and video via `WorkManager`.
- **Encrypted sensitive settings** (`EncryptedSettingsManager`): API keys, OAuth tokens and passwords (TMDB, AniList, Trakt, Debrid, Jellyfin/Plex/Emby tokens, Subsonic password, MDBList key) are encrypted with AES-256-GCM using a hardware-backed key from the Android Keystore before being stored in DataStore. Plain preferences (dark theme, quality, EPG status, etc.) remain unencrypted.
- **Global Crash Report Center**: uncaught exceptions are caught by `GlobalExceptionHandler`, a stack trace is saved and `CrashReportActivity` (in a separate `:crashreport` process) offers restart or "clear cache & restart" without returning to the Android launcher.
- **Screenshot / Compose Preview tests** with Paparazzi and instrumented D-Pad tests.

## Documentation

- [`README.md`](README.md) — this overview (English).
- [`README.pl.md`](README.pl.md) — overview in Polish.
- [`FAQ.md`](FAQ.md) / [`FAQ.pl.md`](FAQ.pl.md) — frequently asked questions.
- [`CHANGELOG.md`](CHANGELOG.md) / [`CHANGELOG.pl.md`](CHANGELOG.pl.md) — release history and latest changes.
- [`PLUGIN_GUIDE.md`](PLUGIN_GUIDE.md) / [`PLUGIN_GUIDE.pl.md`](PLUGIN_GUIDE.pl.md) — how to write and adapt QuickJS, Cloudstream and Aniyomi plugins.
- [`ADMIN_PANEL.md`](ADMIN_PANEL.md) / [`ADMIN_PANEL.pl.md`](ADMIN_PANEL.pl.md) — wireless configuration via the local web admin panel and QR code.
- [`LEGAL_SOURCES.md`](LEGAL_SOURCES.md) / [`LEGAL_SOURCES.pl.md`](LEGAL_SOURCES.pl.md) — curated legal/public/self-hostable sources.

## Tech stack

- Android Gradle Plugin 9.0.0, Gradle 9.1.0, Kotlin 2.3.0
- Jetpack Compose BOM 2026.06.01 (`compose.material3`, `compose.ui`, `compose.foundation`)
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
- `dev.chrisbanes.haze:haze-android` for the frosted-glass sidebar
- `minSdk = 23`, `targetSdk = 36`

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
│   ├── remote/           # TMDB, Trakt, AniList, Jellyfin, Plex, Emby, Subsonic, Stremio, IPTV, Deezer, podcast/radio
│   ├── source/           # Kodi, Web, Cloudstream, FederatedMediaRepository, SourceRegistry, GlobalExceptionHandler
│   ├── plugin/           # QuickJsEngine, PluginManifest, PluginRepository, DynamicPluginLoader, ReflectiveMediaSource
│   ├── audio/            # AudioSource, DeezerAudioSource, PodcastSource, RadioRepository, AudioHistoryRepository
│   ├── torrent/          # TorrentEngine, TorrentHttpServer, TorrentMediaSource
│   ├── iptv/             # EPG parser/repository
│   ├── tv/               # TvLauncherManager, WatchNextHelper, RecommendationsWorker
│   ├── legal/            # LegalSourcesRepository / legal_sources.json sample sources
│   └── ProfileRepository.kt
├── di/                   # Hilt modules
├── model/                # MediaItem, Category, AudioTrack and shared data classes
├── navigation/           # Screen sealed class, TVNavHost, TVApp
├── search/               # Android TV search provider / activity
├── ui/
│   ├── components/       # TVCard, FocusableSurface, ModernSidebarBlurPanel, Sidebar, etc.
│   ├── screens/          # Home, Search, Detail, Player, Settings, Library, Watchlist, Admin, EPG, Torrents, Music, Downloads, Custom Lists, CustomListDetail, EssentialSetup, CrashReportActivity
│   ├── theme/            # AppColor, AppTypography, Spacing, Radius, TVHubTheme
│   └── viewmodel/        # Hilt ViewModels
├── MainActivity.kt
├── CrashReportActivity.kt
├── TVHubApplication.kt
└── ...
```

## Configuration

### First launch

The first time a profile is created, the app shows the **Essential Setup** screen. Pick the legal starter packages you want:

- **Free Internet TV** — public IPTV M3U playlists + XMLTV EPG.
- **Music & Podcasts** — podcast RSS feeds and Deezer proxy.
- **Public Web Catalogs** — official Stremio add-ons and configured web source crawlers.

Selected sources are loaded on `Dispatchers.IO`, saved to DataStore and Room, and the app navigates to Home.

### Admin panel

Most sources are configured through the **Admin** screen or the wireless web panel:

1. Open **Admin** on the TV.
2. Scan the QR code with your phone or open `http://<TV_IP>:<port>/admin` in a browser.
3. Paste your legal source URLs / config JSON and save.

Supported configurable sources:

- **IPTV**: M3U playlists and XMLTV EPG URLs.
- **Kodi**: JSON-RPC endpoint with optional automatic LAN discovery.
- **Cloudstream / Aniyomi**: repository index or binary plugin files loaded dynamically.
- **Web**: JSON config with selectors for title, description, poster, stream link.
- **QuickJS plugins**: manifest URL or `.js` plugin text.
- **Jellyfin / Plex / Emby / Subsonic**: server URL + token/user credentials.
- **Stremio**: addon URLs.
- **TMDB / AniList / Trakt**: API keys / tokens.
- **Podcasts**: RSS feed URLs.
- **Deezer**: proxy URL.
- **Debrid**: OAuth link.

Sample legal starter sources can be loaded from the Admin panel via **Load legal sample sources** or during first-launch onboarding.

## Multi-User Profiles

The app supports multiple household profiles stored in Room:

- Each profile has `id`, `name`, `avatarUrl`, `isPinLocked` and `pinCode`.
- `WatchedEntity`, `SavedMediaEntity`, `CustomListEntity` and `AudioHistoryEntity` carry a `profileId` foreign key.
- `ProfileRepository` exposes `currentProfile` as a `StateFlow` and persists the selected profile in DataStore.
- `WatchHistoryRepository`, `SavedMediaRepository`, `CustomListsRepository` and `AudioHistoryRepository` automatically filter reads/writes by the active profile using `flatMapLatest`.
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

## Dynamic binary plugin loading

Cloudstream (`.cs3` / `.cs4`) and Aniyomi (`.apk`) plugins can be loaded without an Android system installer:

- `PluginRepository` fetches the binary to `context.cacheDir/plugins/`.
- `DynamicPluginLoader` creates an optimized DEX output directory under `codeCacheDir/plugins_dex` and instantiates `DexClassLoader` with the app class loader as parent.
- Loaded classes are adapted through `ReflectiveMediaSource` to the app's `MediaSource` contract.
- Plugin results feed `PluginMediaSource` and appear in Search, Home and the federated dashboard.
- Cleanup removes optimized DEX files when a plugin is disabled or removed.

## Polish audio / subtitle handling

- `DefaultTrackSelector` prefers `pl` for audio and text tracks.
- If multiple Polish audio tracks exist (Lektor, Dubbing, Audio Description), the AD role is deprioritized.
- Full track labels are extracted from Media3 and shown in `PlayerControls` so the user can switch precisely with the D-Pad.
- Subtitle size, color and vertical offset are configurable in Settings and applied live.

## Android TV launcher integration

- `TvLauncherManager` writes to `TvContractCompat.WatchNextPrograms` and a dedicated `PreviewChannel`.
- `RecommendationsWorker` runs once per day (or on app start) to refresh featured content on the home screen.
- Clicking a launcher tile opens `MainActivity` via deep links (`polishmediahub://play/{id}` or `polishmediahub://detail/{id}`) and resumes playback from the saved position.

## Crash reporting

Unhandled exceptions are caught by `GlobalExceptionHandler`:

- The stack trace is saved to `filesDir/last_crash.txt`.
- `CrashReportActivity` is started in a separate `:crashreport` process so the failing process can be killed without losing the report.
- The user can choose **Restart app** or **Clear cache and restart** (which removes `cacheDir` contents and the optimized `plugins_dex` directory).
- The crash reporter does not install the exception handler in its own process, preventing crash loops.

## Quality & lint

The project maintains a zero-lint-warning baseline:

- `./gradlew :app:lintDebug` reports `No issues found.`
- `app/lint.xml` suppresses only pre-existing, informational dependency-version and icon-asset warnings.
- All code warnings introduced by the audit were fixed (`ModifierParameter`, `PrivateResource`, `UseKtx`, `PluralsCandidate`, `FrequentlyChangingValue`, `SetJavaScriptEnabled`, `RedundantLabel`, `IntentFilterUniqueDataAttributes`, `DefaultUncaughtExceptionDelegation`, etc.).
- Migrated deprecated Haze `hazeChild` calls to `hazeEffect` to keep the build warning-free.
- Silent empty `catch` blocks in critical paths now log warnings via `Log.w(...)`.

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
