# Polish Media Hub — Admin Panel

The built-in **Admin Panel** lets you configure all sources from a phone, tablet or computer on the same local network as the TV. You do not have to type long URLs with the on-screen keyboard.

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
- The server binds to the local network IP discovered by `NetworkAddressHelper` (usually Wi-Fi or ethernet).
- Cleartext HTTP to the TV IP is permitted by the network security config because the traffic stays on the LAN.

## Endpoints

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/admin` | Serves the responsive HTML admin page. |
| GET | `/api/config` | Returns the current `ApiConfigRepository` values as JSON. |
| POST | `/api/config` | Receives JSON and saves it into `ApiConfigRepository` (DataStore). |
| POST | `/api/plugin` | Receives a QuickJS plugin manifest / script and stores it through `PluginRepository`. |

## Configurable sources (POST `/api/config`)

Send a JSON object with the keys supported by `ApiConfigRepository`. The keys map roughly to the UI fields in the admin page:

```json
{
  "kodiUrl": "http://192.168.1.10:8080/jsonrpc",
  "webSources": [
    {
      "name": "Example Web Source",
      "baseUrl": "https://example.com",
      "itemSelector": "a.movie",
      "titleSelector": "h2",
      "descriptionSelector": ".desc",
      "posterSelector": "img",
      "linkSelector": "a.movie",
      "nextPageSelector": ".next",
      "headers": {
        "User-Agent": "Mozilla/5.0"
      }
    }
  ],
  "cloudstreamRepo": "https://example.com/repo.json",
  "m3uUrl": "https://example.com/playlist.m3u",
  "epgUrl": "https://example.com/epg.xml",
  "jellyfinUrl": "http://192.168.1.10:8096",
  "jellyfinToken": "your-token",
  "plexUrl": "http://192.168.1.10:32400",
  "plexToken": "your-plex-token",
  "embyUrl": "http://192.168.1.10:8096",
  "embyToken": "your-emby-token",
  "subsonicUrl": "http://192.168.1.10:4040",
  "subsonicUser": "admin",
  "subsonicPassword": "secret",
  "tmdbApiKey": "your-tmdb-key",
  "aniListToken": "your-anilist-token",
  "traktClientId": "your-trakt-client-id",
  "traktAccessToken": "your-trakt-access-token",
  "podcastRssUrls": ["https://example.com/feed.xml"],
  "debridProvider": "REAL_DEBRID",
  "realDebridAccessToken": "...",
  "realDebridRefreshToken": "..."
}
```

Only the fields you actually use need to be present. Empty strings are ignored.

## Adding a QuickJS plugin (POST `/api/plugin`)

You can add a plugin in two ways:

### 1. By manifest URL

```json
{
  "manifestUrl": "https://example.com/plugins/myplugin/manifest.json"
}
```

The manifest must match the `PluginManifest` format:

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

### 2. By raw script

```json
{
  "id": "myplugin",
  "name": "My Plugin",
  "script": "function search(q) { ... }\nfunction resolve(id) { ... }"
}
```

The raw script is passed to `QuickJsMediaSource.configure(script)`.

See [PLUGIN_GUIDE.md](PLUGIN_GUIDE.md) for the JavaScript API.

## Reactive updates

`ApiConfigRepository` and `PluginRepository` expose `Flow` values. As soon as you save config from the web panel:

- the TV UI updates,
- `FederatedMediaRepository` rebuilds active sources,
- `RecommendationsWorker` can be triggered to refresh launcher recommendations.

## Troubleshooting

| Problem | Likely cause | Fix |
|---------|--------------|-----|
| QR code leads to `ERR_CONNECTION_REFUSED` | The TV and phone are on different networks, or the server stopped. | Re-open **Admin**; verify both devices are on the same Wi-Fi and the URL uses the TV's LAN IP. |
| The URL contains `0.0.0.0` or `127.0.0.1` | `NetworkAddressHelper` could not find a non-loopback interface. | Connect the TV to Wi-Fi / ethernet and disable any VPN that tunnels all traffic. |
| Config saves but UI does not change | The repository `Flow` may not be collected by the screen. | Navigate away and back to the screen; restart the app. |
| Plugin does not appear in Search/Home | The plugin may have thrown during evaluation. | Check `logcat` for QuickJS errors; verify `httpFetch` URLs return valid data. |

## Security notes

- The panel is intentionally simple and runs on HTTP on your local network. Do not expose the TV to the public internet.
- The global PIN lock in **Settings** also guards the Admin screen on the TV itself.
- Cleartext traffic is allowed only for `localhost`, `127.0.0.1` and general LAN hosts in `network_security_config.xml`.
