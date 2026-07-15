# Polish Media Hub — Admin Panel

The built-in **Admin Panel** lets you configure all sources from a phone, tablet or computer on the same local network as the TV. You do not have to type long URLs with the on-screen keyboard.

*Polska wersja: [`ADMIN_PANEL.pl.md`](ADMIN_PANEL.pl.md)*

## Opening the admin panel

1. In the TV app, open **Admin** from the sidebar.
2. The screen shows:
   - a QR code,
   - the full URL (e.g. `http://192.168.1.42:8123/admin`),
   - short instructions.
3. Scan the QR with your phone camera or any QR reader, or type the URL into a browser on the same Wi-Fi network.

## How the server works

- `AdminHttpServer` is a lightweight `ServerSocket` based server running in a `Dispatchers.IO` coroutine.
- It is started when the **Admin** screen is opened and stopped when the screen is destroyed.
- The port is allocated dynamically (`ServerSocket(0)`) to avoid conflicts with other apps or system services.
- Server errors are logged with `Log.w(...)` instead of being silently swallowed, so admin interactions can be debugged.
- The server binds to the local network IP discovered by `NetworkAddressHelper` (usually Wi-Fi or ethernet).
- Cleartext HTTP to the TV IP is permitted by the network security config because the traffic stays on the LAN.

## Endpoints

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/admin` | Serves the responsive HTML admin page. |
| GET | `/api/config` | Returns the current `ApiConfigRepository` values as JSON. |
| POST | `/api/config` | Receives form-encoded values and saves them into `ApiConfigRepository` (DataStore). |
| POST | `/api/plugin` | Receives a plugin manifest / script URL and stores it through `PluginRepository`. |

## Configurable sources (POST `/api/config`)

Send form fields matching the keys supported by `ApiConfigRepository`. The admin web page already uses these names:

| Field | Description |
|-------|-------------|
| `kodiUrl` | Kodi JSON-RPC endpoint, e.g. `http://192.168.1.10:8080/jsonrpc` |
| `webSourceConfig` | JSON array of `WebSourceConfig` objects (see example below) |
| `cloudstreamRepoUrls` | Cloudstream / Aniyomi repository URLs, one per line |
| `iptvSourceUrls` | IPTV M3U/M3U8 playlist URLs, one per line |
| `stremioAddons` | Stremio add-on base URLs, one per line |
| `jellyfinUrl` / `jellyfinToken` | Jellyfin server URL and API token |
| `plexUrl` / `plexToken` | Plex server URL and token |
| `embyUrl` / `embyToken` | Emby server URL and API token |
| `forceTranscode` | `true` or `false` |
| `maxDirectPlayBitrate` | Max bitrate in bps, e.g. `20000000` |
| `subsonicUrl` / `subsonicUser` / `subsonicPassword` | Subsonic / Airsonic credentials |
| `podcastFeeds` | Podcast RSS feed URLs, one per line |
| `deezerProxyUrl` | Deezer proxy URL (e.g. `https://your-worker.workers.dev`) |
| `tmdbApiKey` | TMDB API key |
| `aniListToken` | AniList access token |
| `traktClientId` | Trakt client ID |
| `debridApiKey` / `debridProvider` | Debrid token and provider (`real_debrid`, `torbox`) |

Only the fields you actually use need to be present. Empty strings are ignored.

### Full JSON example (for direct API use)

```json
{
  "tmdbApiKey": "your-tmdb-key",
  "aniListToken": "your-anilist-token",
  "traktClientId": "your-trakt-client-id",
  "debridApiKey": "your-debrid-token",
  "debridProvider": "real_debrid",
  "iptvSourceUrls": "https://example.com/playlist.m3u\nhttps://example.com/playlist.m3u8",
  "stremioAddons": "https://addon.youtube.com/stremio/\nhttps://addon.ted.com/stremio/",
  "kodiUrl": "http://192.168.1.10:8080/jsonrpc",
  "webSourceConfig": "[{ \"id\": \"example\", \"name\": \"Example Web Source\", \"baseUrl\": \"https://example.com\", \"catalogUrl\": \"https://example.com/catalog\", \"itemSelector\": \"a.movie\", \"titleSelector\": \"h2\", \"descriptionSelector\": \".desc\", \"posterSelector\": \"img\", \"posterAttribute\": \"src\", \"linkSelector\": \"a.movie\", \"headers\": { \"User-Agent\": \"Mozilla/5.0\" } }]",
  "cloudstreamRepoUrls": "https://example.com/repo.json\nhttps://example.com/index.min.json",
  "jellyfinUrl": "http://192.168.1.10:8096",
  "jellyfinToken": "your-token",
  "plexUrl": "http://192.168.1.10:32400",
  "plexToken": "your-plex-token",
  "embyUrl": "http://192.168.1.10:8096",
  "embyToken": "your-emby-token",
  "subsonicUrl": "http://192.168.1.10:4040",
  "subsonicUser": "admin",
  "subsonicPassword": "secret",
  "podcastFeeds": "https://example.com/feed.xml\nhttps://nasa.gov/rss/dyn/NASAcast_Podcast.rss",
  "deezerProxyUrl": "https://your-worker.workers.dev"
}
```

Note: the actual HTTP endpoint expects `application/x-www-form-urlencoded` form fields, not raw JSON. The JSON example above is shown for clarity.

## Kodi remote settings sync

When you save a Debrid or Trakt token in the admin panel, the app attempts to push it to a configured Kodi instance:

- `Settings.SetSettingValue` is called over JSON-RPC for `plugin.video.fanfilm`.
- `realdebrid_token` and `trakt_token` settings are updated automatically.
- This requires `kodiUrl` to be configured and reachable.

## Adding a plugin (POST `/api/plugin`)

You can add a plugin by submitting a `url` form field:

```
POST /api/plugin
Content-Type: application/x-www-form-urlencoded

url=https://example.com/plugins/myplugin/manifest.json
```

The manifest can be a `PluginManifest` (for QuickJS plugins), a Cloudstream `repo.json` / `plugins.json` or an Aniyomi `index.min.json`. Binary plugins (`.cs3`, `.cs4`, `.apk`) can also be referenced by their direct download URL; `PluginRepository` will fetch and load them via `DynamicPluginLoader`.

### QuickJS manifest example

```json
{
  "id": "myplugin",
  "name": "My Plugin",
  "version": "1.0.0",
  "description": "Optional description",
  "sources": [
    {
      "type": "quickjs",
      "id": "quickjs:myplugin",
      "name": "My QuickJS Source",
      "enabled": true,
      "config": {
        "scriptUrl": "https://example.com/plugins/myplugin/plugin.js"
      }
    }
  ]
}
```

### Raw QuickJS script

A manifest or plugin entry can also contain a raw `script` field instead of `scriptUrl`. The raw script is passed to `QuickJsMediaSource.configure(script)`.

See [PLUGIN_GUIDE.md](PLUGIN_GUIDE.md) for the JavaScript API and binary plugin details.

## Reactive updates

`ApiConfigRepository` and `PluginRepository` expose `Flow` values. As soon as you save config from the web panel:

- the TV UI updates,
- `FederatedMediaRepository` rebuilds active sources,
- `RecommendationsWorker` can be triggered to refresh launcher recommendations,
- Kodi tokens are pushed to the configured Kodi add-on.

## First-launch onboarding

New profiles see the **Essential Addon Setup** screen. You can pre-configure `legal_sources.json` (in `app/src/main/assets/`) with the same keys; the onboarding wizard loads selected packages into `ApiConfigRepository`. This is useful for OEM builds or shared household setups.

## Troubleshooting

| Problem | Likely cause | Fix |
|---------|--------------|-----|
| QR code leads to `ERR_CONNECTION_REFUSED` | The TV and phone are on different networks, or the server stopped. | Re-open **Admin**; verify both devices are on the same Wi-Fi and the URL uses the TV's LAN IP. |
| The URL contains `0.0.0.0` or `127.0.0.1` | `NetworkAddressHelper` could not find a non-loopback interface. | Connect the TV to Wi-Fi / ethernet and disable any VPN that tunnels all traffic. |
| Config saves but UI does not change | The repository `Flow` may not be collected by the screen. | Navigate away and back to the screen; restart the app. |
| Plugin does not appear in Search/Home | The plugin may have thrown during evaluation or loading. | Check `logcat` for QuickJS or DEX loading errors; verify URLs return valid data. |
| Kodi settings are not pushed | Kodi is offline or the add-on ID differs. | Verify `kodiUrl` and that the target add-on uses `realdebrid_token` / `trakt_token`. |

## Security notes

- The panel is intentionally simple and runs on HTTP on your local network. Do not expose the TV to the public internet.
- The global PIN lock in **Settings** also guards the Admin screen on the TV itself.
- Cleartext traffic is allowed only for `localhost`, `127.0.0.1` and general LAN hosts in `network_security_config.xml`.
