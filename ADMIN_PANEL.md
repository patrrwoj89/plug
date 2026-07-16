# Polish Media Hub — Admin Panel

The built-in **Admin Panel** lets you configure all sources from a phone, tablet or computer on the same local network as the TV. You do not have to type long URLs with the on-screen keyboard.

*Polska wersja: [`ADMIN_PANEL.pl.md`](ADMIN_PANEL.pl.md)*

## Opening the admin panel

1. In the TV app, open **Admin** from the sidebar.
2. The screen shows:
   - a QR code,
   - the full URL (e.g. `http://192.168.1.42:8123/admin?token=abcd1234…`),
   - short instructions.
3. Scan the QR with your phone camera or any QR reader, or type the URL into a browser on the same Wi-Fi network.

The URL contains a unique per-process pairing token. All API endpoints require this token via the `?token=...` query parameter and return `403 Forbidden` if it is missing or incorrect.

## How the server works

- `AdminHttpServer` is a lightweight `ServerSocket` based server running in a `Dispatchers.IO` coroutine.
- It is started when the **Admin** screen is opened and stopped when the screen is destroyed.
- The port is allocated dynamically (`ServerSocket(0)`) to avoid conflicts with other apps or system services.
- On startup the server generates a random UUID pairing token and appends it to the admin URL (`/admin?token=...`).
- All `/api/*` endpoints verify the `?token=...` query parameter; missing/invalid tokens receive HTTP `403 Forbidden`.
- Cross-origin requests are restricted to the IP/port shown in the admin URL. If a browser sends an `Origin` header that does not match `http://<TV_IP>:<port>`, the request is rejected with HTTP `403 Forbidden`; the wildcard CORS header is no longer used.
- The built-in admin HTML reads the token from the page URL and appends it to every `/api/*` fetch, so you do not have to type it manually.
- Server errors are logged with `Log.w(...)` instead of being silently swallowed, so admin interactions can be debugged.
- The server binds to the local network IP discovered by `NetworkAddressHelper` (usually Wi-Fi or ethernet).
- Cleartext HTTP to the TV IP is permitted by the network security config because the traffic stays on the LAN.

## Endpoints

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/admin` | Serves the responsive HTML admin page. |
| GET | `/api/config` | Returns the current `ApiConfigRepository` values as JSON. Sensitive keys are masked (first 4 + last 4 chars). Requires `?token=...`. |
| POST | `/api/config` | Receives form-encoded values and saves them into `ApiConfigRepository` (DataStore). Requires `?token=...`. |
| POST | `/api/plugin` | Receives a plugin manifest / script URL and stores it through `PluginRepository`. Requires `?token=...`. |
| POST | `/api/trakt/sync` | Triggers an immediate Trakt two-way sync (`TraktSyncWorker.startImmediate`). Requires `?token=...`. |
| GET | `/api/health` | Returns the current background source health-check result as JSON. Requires `?token=...`. |

## Source health indicators

The admin page shows a **Source Health** section with a colored dot for each configured source:

- **green** = `ONLINE` (the last background probe received HTTP 200 from the source's lightweight health endpoint within 3 seconds)
- **red** = `OFFLINE` (network error, timeout or non-2xx HTTP response)
- **gray** = `UNCONFIGURED` (empty URL/field)

The same status dots appear next to each source input field. The worker runs every 4 hours, but you can also trigger an immediate check from **Settings → Source Health** on the TV.

## Configurable sources (POST `/api/config`)

Send form fields matching the keys supported by `ApiConfigRepository`. The admin web page already uses these names:

| Field | Description |
|-------|-------------|
| `kodiUrl` | Kodi JSON-RPC endpoint, e.g. `http://192.168.1.10:8080/jsonrpc` |
| `webSourceConfig` | JSON array of `WebSourceConfig` objects (see example below) |
| `cloudstreamRepoUrls` | Cloudstream / Aniyomi repository URLs, one per line |
| `iptvSourceUrls` | IPTV M3U/M3U8 playlist URLs, one per line |
| `epgUrl` | XMLTV EPG URL, e.g. `https://example.com/epg.xml` |
| `stremioAddons` | Stremio add-on base URLs, one per line |
| `jellyfinUrl` / `jellyfinToken` | Jellyfin server URL and API token |
| `plexUrl` / `plexToken` | Plex server URL and token |
| `embyUrl` / `embyToken` | Emby server URL and API token |
| `forceTranscode` | `true` or `false` |
| `maxDirectPlayBitrate` | Max bitrate in bps, e.g. `20000000` |
| `subsonicUrl` / `subsonicUser` / `subsonicPassword` | Subsonic / Airsonic credentials |
| `podcastFeeds` | Podcast RSS feed URLs, one per line |
| `deezerProxyUrl` | Deezer proxy URL (e.g. `https://your-worker.workers.dev`) |
| `mdbListApiKey` | MDBList API key (get it at https://mdblist.com/preferences/#api) |
| `tmdbApiKey` | TMDB API key |
| `aniListToken` | AniList access token |
| `traktClientId` | Trakt client ID |
| `traktClientSecret` | Trakt client secret (required for the device-code OAuth flow) |
| `traktAccessToken` | Trakt OAuth access token (Bearer) for two-way sync and scrobbling |
| `traktRefreshToken` | Trakt OAuth refresh token (populated automatically after device-code pairing) |
| `debridApiKey` / `debridProvider` | Debrid token and provider (`real_debrid`, `torbox`) |
| `lastEpgSyncAt` | Read-only timestamp of the last IPTV/EPG background sync (milliseconds since epoch) |
| `lastEpgSyncStatus` | Read-only status of the last sync: `success` or `error` |
| `lastEpgSyncError` | Read-only error message from the last failed sync, if any |
| `lastTraktSyncAt` | Read-only timestamp of the last Trakt sync (milliseconds since epoch) |
| `lastTraktSyncStatus` | Read-only status of the last Trakt sync: `success` or `error` |
| `lastTraktSyncError` | Read-only error message from the last failed Trakt sync, if any |
| `autoSkipIntro` | `true` or `false` — show skip intro/outro buttons during playback |
| `introEndSeconds` | Default intro end time in seconds (e.g. `90`) |
| `outroDurationSeconds` | Default outro duration measured from the end, in seconds (e.g. `120`) |
| `useAlternativePlayer` | `true` or `false` — use the in-process LibVLC engine instead of ExoPlayer |
| `preferredAudioType` | `lector` or `dubbing` — Polish audio preference |
| `nightModeEnabled` | `true` or `false` — enable the ExoPlayer `LoudnessEnhancer` for dynamic range compression |
| `dialogueBoostGainmB` | Night-mode gain in millibels, `0` to `3000` (default `1000` mB) |
| `useCloudflareBypass` | `true` or `false` — enable the Cloudflare Edge Offloading Worker for web stream extraction |
| `cloudflareWorkerUrl` | Your Worker URL, e.g. `https://your-worker.workers.dev` |
| `cloudflareAuthToken` | Shared `HUB_TOKEN` secret; masked in GET `/api/config` |

Only the fields you actually use need to be present. Empty strings are ignored.

### Full JSON example (for direct API use)

```json
{
  "tmdbApiKey": "your-tmdb-key",
  "aniListToken": "your-anilist-token",
  "traktClientId": "your-trakt-client-id",
  "traktClientSecret": "your-trakt-client-secret",
  "traktAccessToken": "your-trakt-oauth-token",
  "traktRefreshToken": "your-trakt-refresh-token",
  "debridApiKey": "your-debrid-token",
  "debridProvider": "real_debrid",
  "iptvSourceUrls": "https://example.com/playlist.m3u\nhttps://example.com/playlist.m3u8",
  "epgUrl": "https://example.com/epg.xml",
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
  "deezerProxyUrl": "https://your-worker.workers.dev",
  "mdbListApiKey": "your-mdblist-api-key",
  "lastTraktSyncAt": "0",
  "lastTraktSyncStatus": "",
  "lastTraktSyncError": "",
  "autoSkipIntro": "true",
  "introEndSeconds": "90",
  "outroDurationSeconds": "120",
  "useAlternativePlayer": "false",
  "preferredAudioType": "lector",
  "nightModeEnabled": "false",
  "dialogueBoostGainmB": "1000",
  "useCloudflareBypass": "false",
  "cloudflareWorkerUrl": "https://your-worker.workers.dev",
  "cloudflareAuthToken": "your-hub-token"
}
```

Note: the actual HTTP endpoint expects `application/x-www-form-urlencoded` form fields, not raw JSON. The JSON example above is shown for clarity. Sensitive fields (MDBList, TMDB, AniList, Trakt, Debrid, Jellyfin/Plex/Emby tokens and Subsonic password) are encrypted with AES-256-GCM in Android Keystore before being written to DataStore. When `GET /api/config` returns them they are masked to only the first 4 and last 4 characters (e.g. `A1B2***********C3D4`), so raw credentials are never visible to other devices on the LAN. Saving new values still works via `POST /api/config` because the plain value is received and encrypted on the TV. The `cloudflareAuthToken` is also treated as sensitive and masked in `GET /api/config`.

## Kodi remote settings sync

The Trakt **Sync now** button on the admin page posts to `/api/trakt/sync` and starts `TraktSyncWorker` immediately. It requires `traktClientId` and `traktAccessToken` to be set (obtained automatically after device-code pairing).

### Trakt token refresh

A dedicated `@Named("trakt")` `OkHttpClient` installs `TraktAuthenticator`. If any Trakt API call returns HTTP 401, the authenticator reads the encrypted `refresh_token`, synchronously calls `/oauth/token`, encrypts the new `access_token` / `refresh_token` pair, and retries the original request. You do not need to re-pair the device when the access token expires.

When you save a Debrid or Trakt token in the admin panel, the app attempts to push it to a configured Kodi instance:

- `Settings.SetSettingValue` is called over JSON-RPC for `plugin.video.fanfilm`.
- `realdebrid_token` and `trakt_token` settings are updated automatically.
- When `debridProvider` is `torbox`, the same API key is pushed as both `torbox_token` and `torbox_apikey` for backward compatibility.
- This requires `kodiUrl` to be configured and reachable.

## Adding a plugin (POST `/api/plugin`)

You can add a plugin by submitting a `url` form field. The request must include the pairing token from the admin URL as a query parameter:

```
POST /api/plugin?token=YOUR_TOKEN
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
| Kodi settings are not pushed | Kodi is offline or the add-on ID differs. | Verify `kodiUrl` and that the target add-on uses `realdebrid_token` (Real-Debrid), `torbox_token` / `torbox_apikey` (TorBox) and `trakt_token` (Trakt). |

## Security notes

- The panel is intentionally simple and runs on HTTP on your local network. Do not expose the TV to the public internet.
- The global PIN lock in **Settings** also guards the Admin screen on the TV itself.
- Cleartext traffic is allowed only for `localhost`, `127.0.0.1` and general LAN hosts in `network_security_config.xml`.
