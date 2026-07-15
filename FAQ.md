# Polish Media Hub — Frequently Asked Questions

## General

### Is the app legal? Does it ship content?

No. Polish Media Hub is a **media aggregator/player shell**. It does not ship any movies, series, IPTV channels, torrents or scrapers. You add your own legal sources (Kodi, Jellyfin, Plex, Emby, M3U, QuickJS plugins, etc.) and the app plays them. It is your responsibility to use content you have the right to access. See [LEGAL_SOURCES.md](LEGAL_SOURCES.md) for a curated list of legal starting points.

### On which devices does it run?

Android TV and Google TV devices (armeabi-v7a / arm64-v8a) running Android 6.0+ (`minSdk = 23`). It is optimized for remote/D-Pad navigation, not touch phones.

### Does it work on phones / tablets?

The UI is built for TV form factors. It may run on tablets but is not optimized for touch.

## First launch & onboarding

### What is the Essential Addon Setup screen?

The first time a profile starts the app, the **Essential Setup** wizard asks you to choose three legal starter packages:

- **Free Internet TV** — loads public IPTV M3U playlists and an XMLTV EPG URL from `legal_sources.json`.
- **Music & Podcasts** — loads sample podcast RSS feeds and (if configured) a Deezer proxy URL.
- **Public Web Catalogs** — loads official Stremio add-ons and any configured web source crawlers.

Selected packages are applied on a background thread, saved to DataStore, and then the app opens the Home screen. You can skip or adjust these later in **Admin**.

### How do I reset the first-launch setup?

Clear app data or uninstall/reinstall. The `isFirstLaunch` flag is stored in DataStore and there is no in-app reset button to avoid accidental data loss.

## Sources & Content

### How do I add sources?

1. Open **Admin** in the sidebar.
2. Scan the QR code with your phone, or open `http://<TV_IP>:<port>/admin` in a browser on the same network.
3. Paste the URL / JSON config for the source you want and save.

See [ADMIN_PANEL.md](ADMIN_PANEL.md) for detailed endpoint descriptions and JSON examples.

### What sources are supported?

- Kodi JSON-RPC with DRM, plugin directories, LAN discovery and remote setting sync.
- Cloudstream repositories (`repo.json` / `plugins.json`) and binary `.cs3` / `.cs4` plugins.
- Aniyomi `.apk` extensions loaded dynamically.
- QuickJS `.js` plugins.
- Web scraping via JSON config (with Jsoup + optional QuickJS fallback).
- IPTV M3U + XMLTV EPG.
- Jellyfin, Plex, Emby, Subsonic/Airsonic.
- Stremio add-ons (legal/official only).
- TMDB, AniList, Trakt metadata.
- Podcasts RSS, internet radio M3U/PLS, Deezer proxy.
- Legal BitTorrent (`.torrent` / magnet) for content you own or that is freely distributable.

### Can I use my NAS / home server?

Yes. Kodi, Jellyfin, Plex, Emby, Subsonic and plain M3U/XMLTV endpoints on your LAN are fully supported. Cleartext HTTP to `localhost`, `127.0.0.1` and LAN hosts is enabled in the network security config.

### Where can I find legal M3U / IPTV playlists?

See the **IPTV / M3U** section of [LEGAL_SOURCES.md](LEGAL_SOURCES.md). The app's `legal_sources.json` also ships curated public starting points that can be loaded during onboarding or from the Admin panel.

### Can the app scrape any website?

Only sites you have the right to scrape. Respect `robots.txt` and Terms of Service. The web source config uses Jsoup selectors and can fall back to QuickJS for dynamic pages.

### What are Cloudstream / Aniyomi plugins?

Cloudstream and Aniyomi are existing Android TV/mobile plugin ecosystems. Polish Media Hub can load their binaries directly into RAM via `DexClassLoader` (no system installer needed):

- `.cs3` / `.cs4` Cloudstream plugins.
- `.apk` Aniyomi extensions.

Plugin classes are adapted to the app's `MediaSource` contract and share the global `OkHttpClient` so they inherit `CookieJar`, `User-Agent`/`Referer` handling and the Cloudflare bypass.

## Profiles

### How do I create a new profile?

The profile management UI is available from the top of the **Sidebar**. Selecting the current profile opens the profile picker. Profile creation/management APIs are exposed by `ProfileRepository`; a future UI update will add an explicit "Add profile" button in the dialog.

### What is the `default_profile`?

When you first install the app, a default profile called "Default" is created automatically. All existing watch history, library, watchlist, custom lists and audio history are migrated to this profile when the database upgrades.

### Are profiles protected by a PIN?

Each profile can have `isPinLocked = true` and a 4-digit `pinCode`. When a locked profile is selected, `PinScreen` is shown. PIN verification is handled by `ProfileRepository.verifyPin()` and is currently per-profile (the global Settings/Admin PIN still exists in `PinRepository`).

### Is watch history shared between profiles?

No. Each profile has its own `WatchedEntity`, `SavedMediaEntity`, `CustomListEntity` and `AudioHistoryEntity` rows. Switching profiles immediately filters the Home "Continue watching" row, Library, Watchlist, Custom Lists and Music history.

### Does the Android TV home screen update when I switch profiles?

Yes. `TvLauncherManager` listens to `ProfileRepository.currentProfile`, clears `WatchNextPrograms`, and re-publishes the new profile's unfinished content.

## Modern Sidebar

### Why does the sidebar disappear?

The **Modern Sidebar** intentionally collapses to a small floating pill when you are browsing content. D-Pad LEFT on the leftmost item re-opens it. If you leave the sidebar open without interacting for 1500 ms, it auto-collapses again.

### How do I open the sidebar without reaching the leftmost item?

On the Home screen you can press **BACK** while the sidebar is collapsed. D-Pad LEFT on any focused item in the leftmost column also works.

## BitTorrent

### Can I stream torrents?

Yes, for **legal content only** (public domain, Creative Commons, Linux ISOs, content you created/own the rights to). Polish Media Hub uses `jlibtorrent` with sequential download and a local HTTP proxy (`TorrentHttpServer`) so ExoPlayer can stream while pieces download.

### Where do the downloaded files go?

Torrent data is saved to the app's private cache directory. The local HTTP proxy reads from the partially downloaded file. The app does not seed by default; seeding settings can be added in the future.

### How do I add a torrent?

Go to **Torrents** in the sidebar, paste a magnet URI or `.torrent` URL, and confirm. The engine will announce, buffer the beginning, and pass a `http://127.0.0.1:<port>/stream?infoHash=...&file=...` URL to the player.

## QuickJS Plugins

### How do I write a plugin?

See [PLUGIN_GUIDE.md](PLUGIN_GUIDE.md). A plugin is a JavaScript file that exposes functions such as `search(query)`, `categories()`, `featured()` and `resolve(id)`. The runtime provides a global `httpFetch(url, headersJson)` returning `{code, body, headers, error}`.

### Can a plugin add headers for ExoPlayer?

Yes. Return a `headers` map in the `MediaItem` object. The player will forward those headers (for example `User-Agent`, `Referer`) to ExoPlayer's `DefaultHttpDataSource.Factory`.

### How are Cloudstream / Aniyomi plugins different from QuickJS plugins?

QuickJS plugins are JavaScript executed inside the app's QuickJS engine. Cloudstream/Aniyomi plugins are Android DEX/APK files loaded dynamically via `DexClassLoader` and adapted with reflection. Both result in `MediaItem` objects and can supply headers for ExoPlayer.

## EPG / Live TV

### The EPG is empty, what do I do?

Add an M3U playlist and an XMLTV EPG URL in **Admin** or during **Essential Setup**. Then open **EPG**. The parser supports `<channel>`, `<programme>`, `<title>`, `<desc>`, `<date>`, `<category>` and `<icon>`.

### Can I watch live TV without EPG?

Yes, IPTV channels are also listed in the **IPTV** screen. EPG is optional and provides the timeline grid view.

## Android TV Launcher

### Why do I not see Polish Media Hub on the home screen?

On supported Android TV / Google TV launchers the app publishes a **Watch Next** channel and a **Preview** recommendations channel. The system launcher decides whether and how to display channels. Make sure the app has been opened at least once and `RecommendationsWorker` has run.

### Does the Watch Next channel respect profiles?

Yes. It is tied to the active profile's `WatchHistoryRepository`. Switching a profile clears the old tiles and publishes the new profile's unfinished items.

## Audio / Subtitles / Music

### Why does a Polish AD (Audiodeskrypcja) track auto-select?

It shouldn't. ExoPlayer prefers Polish audio but deprioritizes tracks flagged as `ROLE_FLAG_DESCRIBES_VIDEO` (Audio Description). You can also manually switch audio tracks in the player controls; full labels like "Polski (Lektor)" are shown when available.

### Can I load external subtitles?

Yes. `PlayerScreen` supports `.vtt` and `.srt` subtitle URLs. The subtitle language defaults to Polish unless the media item specifies another language. You can change subtitle size, color and vertical offset in Settings and they are applied live.

### Where do podcasts and radio come from?

- **Podcasts**: RSS feeds configured in **Admin** (`podcastFeeds`) or loaded during onboarding.
- **Radio**: M3U / PLS playlists. The `RadioRepository` shares the same `iptvSourceUrls` field for playlist URLs, so any M3U/PLS URL can appear in Music.
- **Deezer**: a user-provided `deezerProxyUrl` in Admin. The proxy must expose the endpoints expected by `DeezerAudioSource`.

### Is Deezer audio mixed with video results?

No. `DeezerAudioSource` implements `AudioSource` and is **not** registered in `SourceRegistry`. It only feeds the **Music** screen, keeping music requests isolated from video federation and rate limiting.

## Player Nerd Stats

### What is the Nerd Stats overlay?

A panel in the top-right corner of the player that shows real-time diagnostic data: current video resolution + fps, active video and audio codecs, current download bitrate and dropped/jank frames. Toggle it in **Settings → Show loading stats**.

### Does it affect performance?

The stats are collected through an ExoPlayer `AnalyticsListener` and exposed in a separate `StateFlow`. Only the small stats panel recomposes, so the rest of `PlayerScreen` is not affected.

## Auto-Play Next

### How does the next-episode overlay work?

For series, when 15 seconds remain in the current episode a semi-transparent overlay appears with the next episode's metadata and a 15 → 0 countdown. You can press **Play now** to skip the credits or **Cancel** / **BACK** to finish the current episode.

### Does it work for movies?

No, the overlay is only triggered for `MediaItem.Type.SERIES` / episodes.

## Spoiler Blur

### Why is the episode description blurred?

If **Settings → Spoiler blur** is enabled, descriptions of episodes that have not been watched by the active profile are blurred to avoid spoilers. Press **SELECT** / **D-Pad Center** on the description to reveal it for the current session.

### How does it know an episode was watched?

It checks `WatchHistoryRepository` for a playback position greater than the threshold for that `profileId`.

## Kodi

### Can the app discover my Kodi server automatically?

Yes, if your Kodi instance advertises `_xbmc-jsonrpc._tcp` via mDNS/Bonjour. `KodiDiscoveryManager` listens on Android NSD and automatically updates the Kodi URL in `ApiConfigRepository`.

### Can I send Real-Debrid / Trakt tokens to Kodi plugins?

Yes. The Admin panel stores your debrid / trakt tokens. `KodiMediaSource.setAddonSetting()` calls `Settings.SetSettingValue` over JSON-RPC to push them into the selected Kodi add-on's settings file.

### Does Kodi DRM work?

Yes. When Kodi returns a stream with DRM metadata, the app creates a `MediaItem.DrmConfiguration` for Widevine, PlayReady or ClearKey before handing the stream to ExoPlayer.

## Cloudflare / Web sources

### What if a web source is protected by Cloudflare?

The app includes a **Cloudflare bypass stack**:

- `MemoryCookieJar` stores cookies per host.
- `CloudflareBypassInterceptor` detects 403/503 and launches a headless `WebView` (`HeadlessWebSolver`) to solve the challenge.
- Once `cf_clearance` is obtained, the interceptor retries the original request.
- The same `User-Agent` and `Referer` are used throughout the request chain.

### Can it unpack packed JavaScript?

Yes. `JsUnpacker` decodes common `eval(function(p,a,c,k,...) )` P.A.C.K.E.R. packed scripts and extracts stream URLs, which are then passed to ExoPlayer with the correct headers.

## Admin Panel & QR Code

### The QR code does not scan.

Make sure your phone and the TV are on the same Wi-Fi network. The QR encodes the TV's local IP and the dynamic port of `AdminHttpServer` (e.g. `http://192.168.1.42:8123/admin`). If the IP is `0.0.0.0` or `127.0.0.1`, the network adapter could not be determined.

### Is the admin panel secure?

The panel listens only on the local network interface. It does not use TLS, so it should only be used on a trusted home network. The **Settings / Admin** screens can be protected by the global PIN.

## Crash reporter

### What happens if the app crashes?

Unhandled exceptions are caught by `GlobalExceptionHandler`. The stack trace is saved and `CrashReportActivity` is started in a separate `:crashreport` process. The failing process is killed, so the TV does not immediately return to the launcher. The crash screen offers:

- **Restart app** — clears the crash log and starts `MainActivity`.
- **Clear cache and restart** — deletes the app cache and the optimized DEX plugin directory, then restarts. This is useful if a corrupted plugin or cached file causes a crash loop.

### Will the crash reporter loop if it crashes?

No. `GlobalExceptionHandler` is not installed inside the `:crashreport` process, preventing recursive crash handling.

## Development & Building

### How do I build from source?

```bash
export ANDROID_HOME=/path/to/android-sdk
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

### How do I run snapshot tests?

```bash
./gradlew :app:recordPaparazziDebug   # generate/update baselines
./gradlew :app:verifyPaparazziDebug   # compare with baselines
```

### Where can I report a bug?

Open an issue in the repository with reproduction steps and, if relevant, the source config you are using.
