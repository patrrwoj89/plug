# Polish Media Hub — Plugin Development Guide

Plugins let you add new sources to Polish Media Hub without modifying the Android app. The app supports three plugin formats:

1. **QuickJS plugins** — JavaScript files executed by the embedded **QuickJS** engine.
2. **Cloudstream plugins** — binary `.cs3` / `.cs4` DEX files.
3. **Aniyomi extensions** — Android `.apk` packages loaded dynamically.

*Polska wersja: [`PLUGIN_GUIDE.pl.md`](PLUGIN_GUIDE.pl.md)*

## QuickJS plugins

QuickJS plugins are the easiest to write and require no Android tooling.

### Supported source types

The app recognizes plugins through `PluginManifest` entries with `"type": "quickjs"`. The manifest can be loaded from a URL in the Admin panel or bundled as a raw script. The runtime class is `QuickJsMediaSource`.

### Required JavaScript functions

Your plugin must expose one or more of the following top-level functions. Missing functions simply mean that feature is unavailable for the source.

```js
function featured() {
  return [ /* MediaItem objects */ ];
}

function categories() {
  return [ /* MediaItem objects */ ];
}

function search(query) {
  return [ /* MediaItem objects */ ];
}

function byId(id) {
  return { /* single MediaItem object */ };
}

function resolve(idOrMediaItemId) {
  return "https://...";
  // or return { url: "https://..." };
  // or return { url: "https://...", headers: { "User-Agent": "..." } };
}
```

All functions are invoked from Kotlin coroutines on `Dispatchers.IO`, so network calls inside `httpFetch` are synchronous and block the JS thread only.

### Global `httpFetch(url, headers)`

The engine registers a global JavaScript function that performs an HTTP request through OkHttp and returns a JSON string:

```js
var response = httpFetch("https://example.com/page");
// or with headers (JavaScript object):
var response = httpFetch(
  "https://example.com/page",
  { "User-Agent": "PolishMediaHub/1.0", "Referer": "https://example.com" }
);

var r = JSON.parse(response);
console.log(r.code);     // HTTP status code
console.log(r.body);     // response body as string
console.log(r.headers);  // response headers object
console.log(r.error);    // error message, if any
```

`headers` can be a JavaScript object or a JSON string. Only string header values are forwarded to OkHttp.

Use `r.code` to detect 403/429, read cookies from `r.headers["Set-Cookie"]`, and implement login/session handling inside the plugin.

### MediaItem object fields

`QuickJsMediaSource.mapToMediaItem` converts JavaScript objects/Maps to the app's `MediaItem`. Use these field names:

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `id` | String | yes | Unique ID. Prefix with your source namespace, e.g. `myplugin:1234`. |
| `title` | String | yes | Display title. |
| `type` | String | no | `MOVIE`, `SERIES`, `EPISODE`, `CHANNEL`, `MUSIC`, `PODCAST`, `TORRENT`, `AUDIO`. Defaults to `MOVIE`. |
| `description` | String | no | Plot / summary. |
| `posterUrl` | String | no | URL to poster image. |
| `backdropUrl` | String | no | URL to backdrop image. |
| `year` | Number / String | no | Release year. |
| `season` | Number | no | For series episodes. |
| `episode` | Number | no | For series episodes. |
| `videoUrl` or `url` | String | no | Direct stream or manifest URL. If present, the active player engine (ExoPlayer or LibVLC) uses it directly. |
| `headers` | Object | no | HTTP headers forwarded to the active player engine, e.g. `{ "User-Agent": "...", "Referer": "..." }`. |
| `subtitleUrl` | String | no | External subtitle file (`.vtt` or `.srt`). |
| `subtitleHeaders` | Object | no | Per-subtitle HTTP headers (e.g. `Authorization`) used when fetching `subtitleUrl`. |
| `subtitleLanguage` | String | no | Language code, defaults to `pl`. |
| `drmLicenseUrl` | String | no | DRM license server URL. |
| `drmScheme` | String | no | `WIDEVINE`, `PLAYREADY` or `CLEARKEY`. |
| `drmHeaders` | Object | no | Headers sent to the DRM license server. |

### Stream and subtitle formats

The default ExoPlayer engine auto-detects the stream type from the URL extension and MIME type:

- `.m3u8` or `application/x-mpegurl` → HLS
- `.mpd` or `application/dash+xml` → DASH
- `.mp4`, `.mkv`, `.webm`, etc. → progressive

Subtitles can be `.vtt` or `.srt`. Provide `subtitleLanguage` to match the user's locale. If you need DTS/AC3 audio or legacy MKV/AVI containers that ExoPlayer cannot decode, the user can enable the built-in LibVLC player from Settings/Admin.

### Minimal example QuickJS plugin

```js
function search(query) {
  var resp = httpFetch("https://example.com/search?q=" + encodeURIComponent(query));
  var r = JSON.parse(resp);
  if (r.code !== 200) return [];

  var items = [];
  items.push({
    id: "myplugin:123",
    title: "Example Movie",
    type: "MOVIE",
    posterUrl: "https://example.com/poster.jpg",
    videoUrl: null // resolved later
  });
  return items;
}

function byId(id) {
  var resp = httpFetch("https://example.com/item/" + id.replace("myplugin:", ""));
  var r = JSON.parse(resp);

  return {
    id: id,
    title: "Example Movie",
    description: "A movie from example.com",
    posterUrl: "https://example.com/poster.jpg",
    year: 2024
  };
}

function resolve(id) {
  var realId = id.replace("myplugin:", "");
  var resp = httpFetch("https://example.com/stream/" + realId, {
    "User-Agent": "Mozilla/5.0",
    "Referer": "https://example.com"
  });
  var r = JSON.parse(resp);
  var json = JSON.parse(r.body);
  return {
    url: json.streamUrl,
    headers: {
      "User-Agent": "Mozilla/5.0",
      "Referer": "https://example.com"
    }
  };
}
```

### QuickJS plugin manifest

A plugin is usually distributed as a `PluginManifest` JSON plus one or more `.js` files. The repository ships bundled QuickJS scrapers in `plugins/manifest.json` and individual `plugins/*.js` files.

```json
{
  "id": "myplugin",
  "name": "My Example Plugin",
  "version": "1.0.0",
  "description": "Example plugin for Polish Media Hub",
  "sources": [
    {
      "type": "quickjs",
      "id": "quickjs:myplugin",
      "name": "My Example Source",
      "enabled": true,
      "config": {
        "scriptUrl": "https://example.com/plugins/myplugin/plugin.js"
      }
    }
  ]
}
```

`config.scriptUrl` fetches the `.js` file over HTTPS. You can also embed the script directly in `config.script`. Both work; embedding keeps the plugin self-contained and is used by the bundled scrapers.

You can also paste the raw script directly in the Admin panel. See [ADMIN_PANEL.md](ADMIN_PANEL.md).

## Cloudstream / Aniyomi binary plugins

Cloudstream `.cs3` / `.cs4` files and Aniyomi `.apk` extensions can be loaded without installing them through the Android system installer. The app uses `DexClassLoader` in an isolated directory under `context.codeCacheDir/plugins_dex`.

### Repository indexes

Cloudstream repositories are `repo.json` entry points that link to `plugins.json`:

```json
{
  "name": "Example Repo",
  "url": "https://example.com/plugins.json"
}
```

`plugins.json` is a list of plugin metadata:

```json
[
  {
    "name": "Example Plugin",
    "version": "1.0.0",
    "url": "https://example.com/plugin.cs4",
    "fileSize": 123456,
    "authors": ["author"],
    "tvTypes": ["movie", "tvshow"]
  }
]
```

Aniyomi repositories are `index.min.json` files. The app also ships a default Polish Aniyomi repo URL in `legal_sources.json` (`aniyomiRepo`) and loads it automatically:

```json
[
  {
    "name": "Aniyomi: Example",
    "pkg": "eu.example.extension",
    "apk": "extension.apk",
    "lang": "pl",
    "code": 1,
    "version": "1.0.0",
    "nsfw": 0,
    "sources": [
      {
        "name": "ExampleSource",
        "lang": "pl",
        "id": "1234567890123456789",
        "baseUrl": "https://example.com"
      }
    ]
  }
]
```

`PluginRepository` resolves relative `apk` filenames against the repository base URL and derives a best-guess `mainClass` (`{pkg}.{SourceName}`) for `DexClassLoader`.

### Loading and adaptation

`DynamicPluginLoader` loads the binary, then `ReflectiveMediaSource` adapts plugin classes that do not implement the app's `MediaSource` directly. The adapter uses reflection to find common methods such as `search`, `getMainPage`, `load`, `getVideoExtractor`, etc.

### Network sharing

Loaded plugins receive the global `OkHttpClient` through constructor injection if they accept a constructor parameter of type `okhttp3.OkHttpClient` or `android.content.Context`. This means Cloudflare bypass, `CookieJar`, `User-Agent`/`Referer` handling and retries are shared.

### ExoPlayer headers

Any HTTP headers returned by a binary plugin are attached to `MediaItem.headers` and forwarded to `DefaultHttpDataSource.Factory`.

### Cleanup

When a binary plugin is disabled or removed, `PluginRepository` deletes the optimized DEX files from `codeCacheDir/plugins_dex` and removes the cached APK/DEX file from `cacheDir/plugins/`.

## Adapting existing scripts

### QuickJS

1. Replace the network call helper (e.g. `fetch`, `request`) with `httpFetch(url, headersJson)`.
2. The return value from `httpFetch` is a JSON string; parse it with `JSON.parse`.
3. Make sure `search`, `byId` and `resolve` are top-level functions.
4. Use the exact `MediaItem` field names listed above.
5. Add `headers` to the resolved URL or to each item if the stream requires `User-Agent` / `Referer`.

### Cloudstream / Aniyomi

1. Provide a reachable `repo.json` / `plugins.json` or `index.min.json`.
2. Make sure the binary file is accessible via HTTPS and is not obfuscated beyond `DexClassLoader` reflection.
3. If the plugin exposes a `MainAPI` class, `ReflectiveMediaSource` will detect common method names and adapt them.

## Debugging

- Plugin errors are caught in `QuickJsMediaSource.call()`, `DynamicPluginLoader` and `ReflectiveMediaSource` so the app keeps working.
- Use `adb logcat` to see JavaScript exceptions, DEX loading errors and `console.log` output if the QuickJS wrapper prints it.
- Test `httpFetch` URLs in a browser/curl first to verify headers, cookies and response bodies.
- For dynamic pages, consider using `httpFetch`, parsing the returned HTML in JS, or resolving the video URL in `resolve()`.
- If a Cloudstream/Aniyomi plugin fails to load, check `logcat` for `ClassNotFoundException` or missing constructor errors.

## Security & legal notes

- A plugin can only access the network URLs you configure. It cannot read local files outside the app's sandbox.
- Plugins are JavaScript or compiled code; only install plugins from sources you trust.
- Respect each website's `robots.txt` and Terms of Service. Polish Media Hub is for personal, legal media only.
