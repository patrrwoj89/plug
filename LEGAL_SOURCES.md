# Polish Media Hub — Legal Sources Audit

This document lists **legal, publicly available or self-hostable sources** that can be used with the app for testing and personal use. It intentionally does **not** include piracy trackers, unauthorized IPTV playlists, or unlicensed scrapers.

*Polska wersja: [`LEGAL_SOURCES.pl.md`](LEGAL_SOURCES.pl.md)*

## BitTorrent / jlibtorrent

Use only for content you have the right to share or download.

| Source | URL | Type | Notes |
|--------|-----|------|-------|
| Internet Archive | https://archive.org/details/bittorrent | Public domain / CC | Largest legal torrent library |
| Ubuntu ISO torrents | https://ubuntu.com/download | Open-source | Desktop/server images |
| Debian CD torrents | https://www.debian.org/CD/torrent-cd/ | Open-source | Installation media |
| Linux Tracker | https://linuxtracker.org | Linux ISOs | Public tracker for Linux distros |
| VODO | https://vodo.net | Creative Commons indie films | Mostly CC-licensed movies |
| Jamendo music | https://www.jamendo.com/start | CC music | Also has an official API |

## IPTV / M3U (free and legal)

| Source | URL | Type | Notes |
|--------|-----|------|-------|
| IPTV-org (public links) | https://iptv-org.github.io/iptv/index.m3u | Global public channels | Verify each channel license locally |
| LegalStream | https://github.com/JeremyPlease/LegalStream | News / events | Curated legal streams |
| RW1986 IPTV | https://github.com/RW1986/IPTV | Pluto/Plex/STIRR | Curated free legal streams |
| NASA TV | https://nasa.gov/multimedia/nasatv/index.html | Public domain | Official live streams |
| TVP Info (Poland) | https://www.tvp.info | Polish public TV | Requires Polish IP / official app |

## EPG / XMLTV (free and legal)

The EPG parser supports standard XMLTV files with `<channel>`, `<programme>`, `<title>`, `<desc>`, `<date>`, `<category>` and `<icon>` tags. Only use EPG data you have the right to access.

| Source | URL | Notes |
|--------|-----|-------|
| XMLTV.org | https://xmltv.org | Open XMLTV format information and sample parsers |
| IPTV-org EPG | https://github.com/iptv-org/epg | Community EPG listings; verify channel rights locally |
| Official broadcaster EPG | e.g. TVP, BBC iPlayer, Pluto TV | Use only official/legal feeds |

For Polish channels, check whether the broadcaster provides a public EPG feed or use a self-hosted XMLTV source.

## Stremio (official/legal add-ons)

| Add-on | Source | Notes |
|--------|--------|-------|
| YouTube | official Stremio add-on | Public videos |
| TED | official | TED talks |
| OpenSubtitles | official | Subtitles only; account required for some features |
| Public Domain Torrents | community (verify) | Public domain films |

## Kodi sources (official repository)

| Add-on | Repo / URL | Notes |
|--------|------------|-------|
| YouTube | Kodi official repo | Public videos |
| Jellyfin for Kodi | https://github.com/jellyfin/jellyfin-kodi | Personal media server |
| Plex for Kodi | https://github.com/plexinc/plex-for-kodi | Personal Plex server |
| Pluto TV | Kodi official repo | Free ad-supported TV |
| Archive.org | community add-ons | Public domain content |

## MDBList

MDBList (https://mdblist.com) is a metadata aggregation service that lets users create, browse and search public media lists using cross-linked identifiers (IMDb, TMDB, Trakt, TVDB, MyAnimeList). The app loads public top lists and the authenticated user's lists. You must obtain your own free API key from https://mdblist.com/preferences/#api; the app does not ship keys or content.

| Source | URL | Notes |
|--------|-----|-------|
| MDBList | https://mdblist.com | User-generated public lists; API key required |
| MDBList public list example | `https://api.mdblist.com/lists/top?apikey=<your_key>` | Top public lists |

## Kitsu anime metadata

Kitsu (https://kitsu.io) is a free, public JSON:API for anime and manga metadata. The app uses `https://kitsu.io/api/edge` as a reactive fallback when AniList is unavailable, with `include=mappings` to preserve cross-links to MyAnimeList and AniList IDs. No API key is required.

| Source | URL | Notes |
|--------|-----|-------|
| Kitsu API | https://kitsu.io/api/edge | Public anime/manga metadata; free, no key |
| Kitsu mappings endpoint | `https://kitsu.io/api/edge/anime/{id}/mappings` | External ID mapping records |

## Music / Audio / Podcasts

| Source | URL | Notes |
|--------|-----|-------|
| Jamendo API | https://developer.jamendo.com | CC music, official API, OAuth2 |
| Free Music Archive | https://freemusicarchive.org | CC music |
| ccMixter | http://ccmixter.org | CC remixes |
| Musopen | https://musopen.org | Public domain classical |
| Internet Archive audio | https://archive.org/details/audio | Public domain / CC |
| SomaFM | https://somafm.com | Free internet radio |
| Radio Paradise | https://www.radioparadise.com | Free internet radio |
| NASAcast Podcasts | https://www.nasa.gov/nasa-on-the-hub | Public domain podcasts |
| Legal podcast RSS | use source's own feed | Only use feeds you have the right to access |

### Deezer proxy

The `DeezerAudioSource` uses a user-provided `deezerProxyUrl` (for example a Cloudflare Worker). The proxy must expose the endpoints expected by `DeezerAudioSource` and must comply with Deezer's Terms of Service. The app does not contain any Deezer keys or circumvent DRM.

## Anime (official services)

| Service | Notes |
|---------|-------|
| Crunchyroll | Subscription; has internal API used by official apps (no public documented API) |
| HIDIVE | Subscription |
| ADN / Wakanim (region-specific) | Subscription |

Using unofficial clients for these services is against their Terms of Service and is not included.

## Polish public domain / legal video

| Source | URL | Notes |
|--------|-----|-------|
| Ninateka (Filmoteka Narodowa) | https://ninateka.pl | Polish films, documentaries, theatre; geo-restricted |
| Repozytorium Cyfrowe FN | https://repozytorium.fn.org.pl | Pre-war Polish films, newsreels |

## Polish audio / subtitles

The app prefers Polish audio (`pl`) and Polish subtitles (`pl`) automatically and exposes full track labels in the player UI (e.g. "Polski (Lektor)", "Polski (Dubbing)"). If multiple Polish audio tracks are present, the Audio Description / accessibility role is deprioritized so it does not auto-select.

For custom content you can:

- Provide multi-language HLS/DASH streams and let ExoPlayer select the Polish track.
- Load external SRT/VTT subtitle files in the player.
- Use legal Polish dubbing sources such as official VOD services (Netflix/Prime/Player.pl/HBO Max require their own apps/subscriptions).
- For anime, prefer legal services with Polish subtitles or original audio + Polish subtitles (Crunchyroll, HIDIVE, etc.).

## Android TV / Google TV launcher integration

The app publishes two system channels on supported launchers:

- **Watch Next** — resume unfinished content from `WatchHistoryRepository`.
- **Preview / Recommendations** — featured content refreshed daily by `WorkManager`.

Only content the user has added through legal sources will appear on the launcher. No content is pushed without the user's configured sources.

## Legal starter packages (`legal_sources.json`)

The app ships `app/src/main/assets/legal_sources.json` with curated, no-rights-required starting points. The **Essential Setup** wizard and the **Load legal sample sources** button in Admin can load them automatically:

- **Free Internet TV** — public IPTV M3U playlists and an XMLTV EPG URL.
- **Music & Podcasts** — sample podcast RSS feeds and a placeholder `deezerProxyUrl`.
- **Public Web Catalogs** — official Stremio add-ons (YouTube, TED) and an empty `webSources` list for user-defined crawlers.
- **MDBList** — a starter public list entry plus the official API-key URL so users can generate their own key.

These sample sources are intended only as a starting point; you are responsible for verifying that each source is legal in your region.

## Testing / CI commands

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
./gradlew :app:assembleDebugAndroidTest
./gradlew :app:connectedDebugAndroidTest   # requires an emulator or Android TV device
./gradlew :app:recordPaparazziDebug       # generate/update screenshot baselines
```

## Web scraping / JSON / JS sources

Only scrape sites that explicitly allow it (check `robots.txt` and ToS) or use public APIs.

| Source | URL | Notes |
|--------|-----|-------|
| Archive.org Metadata API | https://archive.org/services/docs/api | Public domain / CC metadata |
| Open Library | https://openlibrary.org/developers/api | Books metadata |
| NASA APIs | https://api.nasa.gov | Images/video metadata |
| TVMaze | https://www.tvmaze.com/api | TV show metadata (CC-BY-SA) |
| TMDB | https://developer.themoviedb.org | Metadata API (free key, attribution required) |

## Notes

- The app does **not** ship pre-configured piracy sources.
- Adding unauthorized streams, torrents, or scrapers is the user's responsibility and may violate local law.
- For self-hosted content, use Jellyfin, Plex, Emby, Kodi, Subsonic/Airsonic, or your own M3U/JSON endpoints.
- BitTorrent support is intended for legally distributable content (e.g. public domain, Creative Commons, Linux ISOs, content you own the rights to).
- QuickJS plugins and web scraping can only access sources you configure; respect each source's `robots.txt` and Terms of Service.
- EPG data must be from a legal source you have the right to use.
- Cloudstream / Aniyomi plugins and Deezer proxies are only legal if the source data and proxy service are legal for your use.
