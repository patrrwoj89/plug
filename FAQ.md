# Polish Media Hub — Frequently Asked Questions

## General

### Is the app legal? Does it ship content?

No. Polish Media Hub is a **media aggregator/player shell**. It does not ship any movies, series, IPTV channels, torrents or scrapers. You add your own legal sources (Kodi, Jellyfin, Plex, Emby, M3U, QuickJS plugins, etc.) and the app plays them. It is your responsibility to use content you have the right to access. See [LEGAL_SOURCES.md](LEGAL_SOURCES.md) for a curated list of legal starting points.

### On which devices does it run?

Android TV and Google TV devices (armeabi-v7a / arm64-v8a) running Android 6.0+ (`minSdk = 23`). It is optimized for remote/D-Pad navigation, not touch phones.

### Does it work on phones / tablets?

The UI is built for TV form factors. It may run on tablets but is not optimized for touch.

## Sources & Content

### How do I add sources?

1. Open **Admin** in the sidebar.
2. Scan the QR code with your phone, or open `http://<TV_IP>:<port>/admin` in a browser on the same network.
3. Paste the URL / JSON config for the source you want and save.

See [ADMIN_PANEL.md](ADMIN_PANEL.md) for detailed endpoint descriptions and JSON examples.

### What sources are supported?

- Kodi JSON-RPC
- Cloudstream repositories (`repo.json` / `plugins.json`)
- QuickJS `.js` plugins
- Web scraping via JSON config (with Jsoup + optional QuickJS fallback)
- IPTV M3U + XMLTV EPG
- Jellyfin, Plex, Emby, Subsonic/Airsonic
- Stremio add-ons (legal/official only)
- TMDB, AniList, Trakt metadata
- Podcasts RSS
- Legal BitTorrent (`.torrent` / magnet) for content you own or that is freely distributable

### Can I use my NAS / home server?

Yes. Kodi, Jellyfin, Plex, Emby, Subsonic and plain M3U/XMLTV endpoints on your LAN are fully supported. Cleartext HTTP to `localhost`, `127.0.0.1` and LAN hosts is enabled in the network security config.

### Where can I find legal M3U / IPTV playlists?

See the **IPTV / M3U** and **EPG / XMLTV** sections of [LEGAL_SOURCES.md](LEGAL_SOURCES.md). Always verify that a stream is licensed for your region.

### Can the app scrape any website?

Only sites you have the right to scrape. Respect `robots.txt` and Terms of Service. The web source config uses Jsoup selectors and can fall back to QuickJS for dynamic pages.

## Profiles

### How do I create a new profile?

The profile management UI is available from the top of the **Sidebar**. Selecting the current profile opens the profile picker. Profile creation/management APIs are exposed by `ProfileRepository`; a future UI update will add an explicit "Add profile" button in the dialog.

### What is the `default_profile`?

When you first install the app, a default profile called "Default" is created automatically. All existing watch history, library, watchlist and custom lists are migrated to this profile when the database upgrades to schema v8.

### Are profiles protected by a PIN?

Each profile can have `isPinLocked = true` and a 4-digit `pinCode`. When a locked profile is selected, `PinScreen` is shown. PIN verification is handled by `ProfileRepository.verifyPin()` and is currently per-profile (the global Settings/Admin PIN still exists in `PinRepository`).

### Is watch history shared between profiles?

No. Each profile has its own `WatchedEntity`, `SavedMediaEntity` and `CustomListEntity` rows. Switching profiles immediately filters the Home "Continue watching" row, Library, Watchlist and Custom Lists.

### Does the Android TV home screen update when I switch profiles?

Yes. `TvLauncherManager` listens to `ProfileRepository.currentProfile`, clears `WatchNextPrograms`, and re-publishes the new profile's unfinished content.

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

## EPG / Live TV

### The EPG is empty, what do I do?

Add an M3U playlist and an XMLTV EPG URL in **Admin**. Then open **EPG**. The parser supports `<channel>`, `<programme>`, `<title>`, `<desc>`, `<date>`, `<category>` and `<icon>`.

### Can I watch live TV without EPG?

Yes, IPTV channels are also listed in the **IPTV** screen. EPG is optional and provides the timeline grid view.

## Android TV Launcher

### Why do I not see Polish Media Hub on the home screen?

On supported Android TV / Google TV launchers the app publishes a **Watch Next** channel and a **Preview** recommendations channel. The system launcher decides whether and how to display channels. Make sure the app has been opened at least once and `RecommendationsWorker` has run.

### Does the Watch Next channel respect profiles?

Yes. It is tied to the active profile's `WatchHistoryRepository`. Switching a profile clears the old tiles and publishes the new profile's unfinished items.

## Audio / Subtitles

### Why does a Polish AD (Audiodeskrypcja) track auto-select?

It shouldn't. ExoPlayer prefers Polish audio but deprioritizes tracks flagged as `ROLE_FLAG_DESCRIBES_VIDEO` (Audio Description). You can also manually switch audio tracks in the player controls; full labels like "Polski (Lektor)" are shown when available.

### Can I load external subtitles?

Yes. `PlayerScreen` supports `.vtt` and `.srt` subtitle URLs. The subtitle language defaults to Polish unless the media item specifies another language.

## Admin Panel & QR Code

### The QR code does not scan.

Make sure your phone and the TV are on the same Wi-Fi network. The QR encodes the TV's local IP and the dynamic port of `AdminHttpServer` (e.g. `http://192.168.1.42:8123/admin`). If the IP is `0.0.0.0` or `127.0.0.1`, the network adapter could not be determined.

### Is the admin panel secure?

The panel listens only on the local network interface. It does not use TLS, so it should only be used on a trusted home network. The **Settings / Admin** screens can be protected by the global PIN.

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
