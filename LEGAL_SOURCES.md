# Polish Media Hub — Legal Sources Audit

This document lists **legal, publicly available or self-hostable sources** that can be used with the app for testing and personal use. It intentionally does **not** include piracy trackers, unauthorized IPTV playlists, or unlicensed scrapers.

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

## Music / Audio

| Source | URL | Notes |
|--------|-----|-------|
| Jamendo API | https://developer.jamendo.com | CC music, official API, OAuth2 |
| Free Music Archive | https://freemusicarchive.org | CC music |
| ccMixter | http://ccmixter.org | CC remixes |
| Musopen | https://musopen.org | Public domain classical |
| Internet Archive audio | https://archive.org/details/audio | Public domain / CC |
| SomaFM | https://somafm.com | Free internet radio |
| Radio Paradise | https://www.radioparadise.com | Free internet radio |

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

The app prefers Polish audio (`pl`) and Polish subtitles (`pl`) automatically. For custom content you can:

- Provide multi-language HLS/DASH streams and let ExoPlayer select the Polish track.
- Load external SRT/VTT subtitle files in the player (feature stub ready for extension).
- Use legal Polish dubbing sources such as official VOD services (Netflix/Prime/Player.pl/HBO Max require their own apps/subscriptions).

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
