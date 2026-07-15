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

## Search

### How does voice search work?

On the **Search** screen, move focus to the **microphone** button next to the search field and press SELECT. The app launches the system speech recognizer configured for Polish (`pl-PL`). Once you speak, the recognized text is inserted into the search field and a search is started automatically. Voice search requires a Google / system speech recognizer app on the device.

## Sources & Content

### How do I add sources?

1. Open **Admin** in the sidebar.
2. Scan the QR code with your phone, or open `http://<TV_IP>:<port>/admin?token=<token>` in a browser on the same network. The token is unique per server run and is embedded in the QR/URL.
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
- Kitsu anime fallback (public API, no key needed).
- Podcasts RSS, internet radio M3U/PLS, Deezer proxy.
- Legal BitTorrent (`.torrent` / magnet) for content you own or that is freely distributable.

### How do I check whether my sources are online?

The app runs a background **Source Health-Check** every 4 hours. It sends a lightweight request to each configured source (Kodi, Jellyfin, Plex, Emby, Subsonic, IPTV/EPG URLs, Stremio, Cloudstream, web sources and the Deezer proxy) with a 3-second timeout. Results are shown as colored dots on the **Settings → Source Health** screen and in the wireless admin panel: **green** = online, **red** = offline/error, **gray** = not configured. You can also tap **Check source health now** in Settings to trigger an immediate check.

## MDBList

### How do I add MDBList?

MDBList is a metadata list provider. Go to **Settings** or the wireless **Admin** panel, enter your MDBList API key (get it at https://mdblist.com/preferences/#api) and save. The app then loads public top lists, your personal lists and supports search. MDBList items carry `tmdbId`, `imdbId` and `traktId` so other sources can match them.

### Where are my API keys stored?

Sensitive values such as MDBList, TMDB, AniList, Trakt, Debrid, Jellyfin/Plex/Emby tokens and the Subsonic password are encrypted with AES-256-GCM using a hardware-backed key from the Android Keystore before being written to DataStore. They are never included in system backups as plain text. When the wireless Admin panel returns configuration JSON, all decrypted API keys and passwords are masked to only the first 4 and last 4 characters (e.g. `A1B2***********C3D4`) so they cannot be captured by other devices on the LAN. Ordinary preferences (dark theme, video quality, EPG sync status, etc.) remain unencrypted for quick access.

## Kitsu anime fallback

### What is Kitsu fallback?

The anime module primarily uses `AniListMediaRepository` (AniList GraphQL). If AniList fails due to a network error, timeout or HTTP 429, `AnimeRepository` automatically switches to `KitsuMediaSource`, which calls the public Kitsu JSON:API (`https://kitsu.io/api/edge`). The switch is silent and does not require any user configuration.

### Does Kitsu need an API key?

No. Kitsu is a free, public API and is listed as a starter source in `legal_sources.json`. No API key, account or manual setup is required.

### How does Kitsu link to other anime IDs?

Kitsu responses are requested with `include=mappings`. Kitsu mappings with `externalSite` containing `myanimelist` are stored on the resulting `MediaItem` as `malId`; mappings containing `anilist` are stored as `aniListId`. These IDs help other plugins and resolvers find matching streams.

## Filmweb.pl metadata fallback

### When does the app fetch Filmweb metadata?

The Polish Media Hub detail screen first loads metadata from TMDB. If the TMDB description is empty, shorter than 50 characters, or does not contain Polish diacritics (ą/ć/ę/ł/ń/ó/ś/ź/ż), `DetailViewModel` launches a background `Dispatchers.IO` task that searches Filmweb.pl via its public API (`www.filmweb.pl/api/v1`) and fetches the Polish title, plot, poster, community rating and vote count.

### Is Filmweb a source I need to configure?

No. Filmweb metadata fallback is automatic and does not require an API key. It is implemented by `FilmwebMediaSource` using the global `OkHttpClient`, so it shares `MemoryCookieJar` and `CloudflareBypassInterceptor` with the rest of the app.

### Where is the Filmweb data stored?

Enriched descriptions, posters, ratings and URLs are cached in the `filmweb_cache` Room table (added in Room v14) through `FilmwebCacheRepository`. The next time the same title and year is opened, the detail screen shows the cached Polish metadata instantly.

### Why do I see a "Filmweb: X.X" label on some detail screens?

When Filmweb data is successfully fetched, `DetailScreen` adds a `Filmweb: X.X` label next to the year / duration / TMDB rating row. If the label is missing, either TMDB already provided a sufficient Polish description or the Filmweb search did not find a match.

## Live TV & EPG

### When does the EPG update?

`IptvUpdateWorker` refreshes configured M3U playlists and XMLTV EPG files every 12 hours and also on a cold app start. The update runs only on an unmetered (Wi-Fi / Ethernet) connection while the device is idle and the battery is not low. Channels and programs are cached in Room, so the **TV Guide** screen opens instantly from local storage.

### Where can I see the last EPG sync status?

The status is shown at the bottom of the **TV Guide** screen, on the **Settings** screen and in the wireless **Admin** panel (`lastEpgSyncAt`, `lastEpgSyncStatus` and `lastEpgSyncError`).

### Can I use my NAS / home server?

Yes. Kodi, Jellyfin, Plex, Emby, Subsonic and plain M3U/XMLTV endpoints on your LAN are fully supported. Cleartext HTTP to `localhost`, `127.0.0.1` and LAN hosts is enabled in the network security config.

### Where can I find legal M3U / IPTV playlists?

See the **IPTV / M3U** section of [LEGAL_SOURCES.md](LEGAL_SOURCES.md). The app's `legal_sources.json` also ships curated public starting points that are audited against current public sources (last check: 2026-07-15) and can be loaded during onboarding or from the Admin panel. Always verify each stream's license locally.

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

### How does Parental Control work?

Each `ProfileEntity` stores `maxAgeRating` (e.g. G, PG, PG-13, R, NC-17, 7/12/16/18) and `allowNsfw` (Boolean). Before any `search()`, `categories()` or `featured()` results are returned to the UI, `ContentFilter` removes items whose age rating is higher than the profile's limit or that are flagged as adult/NSFW. Items with no declared age rating are also hidden when a profile has `maxAgeRating` set (fail-closed). Filtering is applied in `CompositeMediaRepository`, `FederatedMediaRepository` and `PluginMediaSource`.

### How do I change Parental Control settings?

Open **Settings** (which is PIN-protected), scroll to the **Parental Control** section and select a maximum age rating and the NSFW toggle for each profile. Changes take effect immediately and are stored in the Room `profiles` table (v12).

### Is watch history shared between profiles?

No. Each profile has its own `WatchedEntity`, `SavedMediaEntity`, `CustomListEntity` and `AudioHistoryEntity` rows. Switching profiles immediately filters the Home "Continue watching" row, Library, Watchlist, Custom Lists and Music history.

### Does the Android TV home screen update when I switch profiles?

Yes. `TvLauncherManager` listens to `ProfileRepository.currentProfile`, clears `WatchNextPrograms`, and re-publishes the new profile's unfinished content.

## Modern Sidebar

### Why does the sidebar disappear?

The **Modern Sidebar** intentionally collapses to a small floating pill when you are browsing content. D-Pad LEFT on the leftmost item re-opens it. If you leave the sidebar open without interacting for 1500 ms, it auto-collapses again.

### How do I open the sidebar without reaching the leftmost item?

On the Home screen you can press **BACK** while the sidebar is collapsed. D-Pad LEFT on any focused item in the leftmost column also works.

### Does the horizontal row focus remember my position?

Yes. Horizontal `LazyRow`s (e.g. in `CategoryRow` on the Home screen) use `Modifier.focusGroup()` and `focusRestorer()`. If you scroll to the 5th item, move down, and then move back up, focus returns to the 5th item instead of resetting to the first tile.

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

### Some MKV/AVI files or DTS/AC3 audio won't play. What can I do?

Enable **Use LibVLC player** in **Settings > Player** (or from the wireless Admin panel). This switches `PlayerScreen` to the in-process `UniversalVlcPlayer` built on `org.videolan.android:libvlc-all`. LibVLC handles many codecs and containers that ExoPlayer may fail to decode on Android TV, including DTS/AC3 audio and MKV/AVI sources from torrents, Kodi and web plugins. The LibVLC player also mirrors the Polish audio/subtitle preference logic (prefers `pl`/`pol`, deprioritizes Audio Description), applies subtitle size/color/vertical-offset settings through LibVLC options, and supports the Nerd Stats Overlay and Cinema Dimming Mode overlays.

### How are Cloudstream / Aniyomi plugins different from QuickJS plugins?

QuickJS plugins are JavaScript executed inside the app's QuickJS engine. Cloudstream/Aniyomi plugins are Android DEX/APK files loaded dynamically via `DexClassLoader` and adapted with reflection. Both result in `MediaItem` objects and can supply headers for ExoPlayer.

## EPG / Live TV

### The EPG is empty, what do I do?

Add an M3U playlist and an XMLTV EPG URL in **Admin** or during **Essential Setup**. Then open **EPG**. The parser supports `<channel>`, `<programme>`, `<title>`, `<desc>`, `<date>`, `<category>` and `<icon>`.

### Can I watch live TV without EPG?

Yes, IPTV channels are also listed in the **IPTV** screen. EPG is optional and provides the timeline grid view.

## Custom Lists

### What are custom lists?

Custom lists let you group media items (movies, series, episodes) independently from the global Library or Watchlist. Each list has a `listId` and a name and is stored in `CustomListEntity` with a `profileId` foreign key.

### How do I view the contents of a custom list?

Open **Custom Lists** in the sidebar, highlight the list you want and press **SELECT**. The app navigates to `CustomListDetailScreen`, which shows the list's contents as a 5-column `LazyVerticalGrid` of `MediaCard` tiles.

## Android TV Launcher

### Why do I not see Polish Media Hub on the home screen?

On supported Android TV / Google TV launchers the app publishes a **Watch Next** channel and a **Preview** recommendations channel. The system launcher decides whether and how to display channels. Make sure the app has been opened at least once and `RecommendationsWorker` has run.

### Does the Watch Next channel respect profiles?

Yes. It is tied to the active profile's `WatchHistoryRepository`. Switching a profile clears the old tiles and publishes the new profile's unfinished items.

## Audio / Subtitles / Music

### Why does a Polish AD (Audiodeskrypcja) track auto-select?

It shouldn't. ExoPlayer prefers Polish audio but deprioritizes tracks flagged as `ROLE_FLAG_DESCRIBES_VIDEO` (Audio Description). You can also manually switch audio tracks in the player controls; full labels like "Polski (Lektor)" are shown when available.

### Can I load external subtitles?

Yes. `PlayerScreen` supports `.vtt` and `.srt` subtitle URLs. The subtitle language defaults to Polish unless the media item specifies another language. Plugins can also supply a `subtitleHeaders` map in `MediaItem` so authorization headers are forwarded when downloading the subtitle file. You can change subtitle size, color and vertical offset in Settings and they are applied live.

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

## Trakt.tv sync

### How does two-way Trakt sync work?

`TraktSyncWorker` runs every 6 hours (and can be triggered manually from **Settings** or the admin panel). It pulls your watched history and watchlist from Trakt and writes them to the active profile's Room tables. Any items watched or added to the watchlist locally while offline are pushed back to Trakt. Conflicts are resolved by timestamp: the newer record wins. Trakt paginates `sync/watched` and `sync/watchlist` since June 2026; the app follows `X-Pagination-Page-Count` and loads every page so large watch histories stay in sync.

### Where do I enter my Trakt credentials?

Open **Settings → Trakt Sync** or the wireless **Admin** panel. Enter your **Trakt client ID** and **client secret** (both from `trakt.tv/oauth/applications`), then tap **Log in with Trakt**. The app will request a device code, display the `user_code` and a QR code, and poll Trakt in the background. Type the code on the activation URL shown on screen (or scan the QR code with your phone). Once you authorize the device, the access and refresh tokens are encrypted with AES-256-GCM in the Android Keystore and saved to DataStore.

### What is Picture-in-Picture mode?

When you are playing a video and press the **Home** button (or otherwise leave the app), the player switches to Android's native Picture-in-Picture (PiP) window. All controls, subtitles, the Nerd Stats overlay, Cinema info card and Next Episode overlay are hidden, leaving only the video stream. When you return to the app the full player interface is restored.

### What is Trakt scrobbling?

During playback the app sends `/scrobble/start` when the video starts playing, `/scrobble/pause` when you pause, and `/scrobble/stop` when the player closes. Each request includes the exact percent progress, so Trakt knows how far you are in a movie or episode.

### Will I have to re-pair Trakt when the access token expires?

No. `TraktAuthenticator` is installed on the dedicated Trakt `OkHttpClient`. If a Trakt API call returns HTTP 401, it reads the encrypted `refresh_token` from DataStore, calls Trakt's `/oauth/token` endpoint with `grant_type=refresh_token`, encrypts the new tokens, and retries the original request automatically.

## Skip Intro / Outro

### How does the skip intro button work?

If a plugin or source provides exact `introStartMs`/`introEndMs` timestamps in `MediaItem`, the player uses them. Otherwise it falls back to the default durations set in **Settings → Skip Intro / Outro**. When the playhead enters the intro segment, a "Skip intro" button slides in and grabs D-Pad focus; pressing it seeks past the intro. The same happens for the outro/credits, where the button triggers the Auto-Play Next overlay so you can start the next episode or cancel.

### Where can I see the last sync status?

The **Settings** screen and the wireless **Admin** panel show "Last Trakt sync: [date] — [status]". If a sync fails, the error message is displayed there.

## Cinema Dimming Mode

### What is Cinema Mode?

**Cinema Mode** (toggle in **Settings**) is a playback experience designed for dark rooms. While the video is playing the player overlays automatically hide after a short inactivity delay and the screen smoothly dims. When you pause or press any D-Pad button the interface brightens again.

### What is the info card shown on pause?

When paused, a card appears below the progress slider showing the title, description, genres and the top 5 cast members for the current movie or episode. Cast data is fetched in the background from TMDB (or Trakt metadata) as soon as the player opens.

## Spoiler Blur

### Why is the episode description blurred?

If **Settings → Spoiler blur** is enabled, descriptions of episodes that have not been watched by the active profile are blurred to avoid spoilers. Press **SELECT** / **D-Pad Center** on the description to reveal it for the current session.

### How does it know an episode was watched?

It checks `WatchHistoryRepository` for a playback position greater than the threshold for that `profileId`.

## Kodi

### Can the app discover my Kodi server automatically?

Yes, if your Kodi instance advertises `_xbmc-jsonrpc._tcp` via mDNS/Bonjour. `KodiDiscoveryManager` listens on Android NSD and automatically updates the Kodi URL in `ApiConfigRepository`.

### Can I send Real-Debrid / Trakt / TorBox tokens to Kodi plugins?

Yes. The Admin panel stores your debrid / trakt tokens. When the configured debrid provider is `real_debrid`, `KodiMediaSource.setAddonSetting()` pushes `realdebrid_token` to `plugin.video.fanfilm`. When the provider is `torbox`, it pushes the same API key as both `torbox_token` and `torbox_apikey` for backward compatibility. Trakt client ID is pushed as `trakt_token`.

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

The panel listens only on the local network interface. It does not use TLS, so it should only be used on a trusted home network. Since this build, `AdminHttpServer` generates a unique per-process pairing token and embeds it in the QR/admin URL (`/admin?token=...`). All `/api/*` endpoints (including `/api/plugin` and `/api/config`) reject missing or invalid tokens with HTTP 403, so an attacker on the LAN cannot install plugins without scanning the QR code while the admin screen is open. The **Settings / Admin** screens can also be protected by the global PIN.

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

## UI / UX

### Why does the movie poster animate when I open the details screen?

Polish Media Hub uses Jetpack Compose **Shared Element Transitions**. The `NavHost` is wrapped in a `SharedTransitionLayout`; the poster in the grid (`MediaCard`) and the main poster on the detail screen share the same key (`poster_<item_id>`). When you press D-Pad Center/SELECT, Compose animates the poster's size and position between the two screens instead of fading it out and in.

### Why does focus jump to the top of the Library / Watchlist grid after I press BACK?

It shouldn't. `LibraryScreen`, `WatchlistScreen` and `CustomListDetailScreen` attach `Modifier.focusGroup()` + `focusRestorer()` to their `LazyVerticalGrid`. If focus ever resets to the header instead of the last tile, make sure you are running the latest build and navigation returned through the same back stack entry.

### What is the large banner at the top of Home?

The **Hero Featured Banner** highlights the first featured item. It shows a cinematic backdrop with dark gradients, the title, year, TMDB/Filmweb rating badges and a focused "Play now ▷" button. Press D-Pad Center/SELECT on the button to open the detail screen.

### Where can I report a bug?

Open an issue in the repository with reproduction steps and, if relevant, the source config you are using.
