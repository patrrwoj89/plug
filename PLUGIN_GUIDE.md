# Polish Media Hub — Plugin Development Guide

Plugins let you add new sources to Polish Media Hub without modifying the Android app. A plugin is a JavaScript file evaluated by the embedded **QuickJS** engine.

## Supported source types

The app recognizes plugins through `PluginManifest` entries with `"type": "quickjs"`. The manifest can be loaded from a URL in the Admin panel or bundled as a raw script. The runtime class is `QuickJsMediaSource`.

## Required JavaScript functions

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
}
```

All functions are invoked from Kotlin coroutines on `Dispatchers.IO`, so network calls inside `httpFetch` are synchronous and block the JS thread only.

## Global `httpFetch(url, headersJson)`

The engine registers a global JavaScript function that performs an HTTP request through OkHttp and returns a JSON string:

```js
var response = httpFetch("https://example.com/page");
// or with headers:
var response = httpFetch(
  "https://example.com/page",
  JSON.stringify({ "User-Agent": "PolishMediaHub/1.0", "Referer": "https://example.com" })
);

var r = JSON.parse(response);
console.log(r.code);     // HTTP status code
console.log(r.body);     // response body as string
console.log(r.headers);  // response headers object
console.log(r.error);    // error message, if any
```

Use `r.code` to detect 403/429, read cookies from `r.headers["Set-Cookie"]`, and implement login/session handling inside the plugin.

## MediaItem object fields

`QuickJsMediaSource.mapToMediaItem` converts JavaScript objects/Maps to the app's `MediaItem`. Use these field names:

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `id` | String | yes | Unique ID. Prefix with your source namespace, e.g. `myplugin:1234`. |
| `title` | String | yes | Display title. |
| `type` | String | no | `MOVIE`, `SERIES`, `EPISODE`, `CHANNEL`, `MUSIC`, `PODCAST`, `TORRENT`. Defaults to `MOVIE`. |
| `description` | String | no | Plot / summary. |
| `posterUrl` | String | no | URL to poster image. |
| `backdropUrl` | String | no | URL to backdrop image. |
| `year` | Number / String | no | Release year. |
| `season` | Number | no | For series episodes. |
| `episode` | Number | no | For series episodes. |
| `videoUrl` or `url` | String | no | Direct stream or manifest URL. If present, ExoPlayer uses it directly. |
| `headers` | Object | no | HTTP headers forwarded to ExoPlayer, e.g. `{ "User-Agent": "...", "Referer": "..." }`. |
| `subtitleUrl` | String | no | External subtitle file (`.vtt` or `.srt`). |
| `subtitleLanguage` | String | no | Language code, defaults to `pl`. |

## Stream and subtitle formats

The player auto-detects the stream type from the URL extension and MIME type:

- `.m3u8` or `application/x-mpegurl` → HLS
- `.mpd` or `application/dash+xml` → DASH
- `.mp4`, `.mkv`, `.webm`, etc. → progressive

Subtitles can be `.vtt` or `.srt`. Provide `subtitleLanguage` to match the user's locale.

## Minimal example plugin

```js
function search(query) {
  var resp = httpFetch("https://example.com/search?q=" + encodeURIComponent(query));
  var r = JSON.parse(resp);
  if (r.code !== 200) return [];

  var doc = new DOMParser().parseFromString(r.body, "text/html");
  // ... or use regex / JSON parsing, depending on the site.

  var items = [];
  // Pseudo-code:
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
  var html = r.body;

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
  var resp = httpFetch("https://example.com/stream/" + realId, JSON.stringify({
    "User-Agent": "Mozilla/5.0",
    "Referer": "https://example.com"
  }));
  var r = JSON.parse(resp);
  // If the page returns the final URL in JSON:
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

## Plugin manifest

A plugin is usually distributed as a `PluginManifest` JSON plus one or more `.js` files:

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

You can also paste the raw script directly in the Admin panel. See [ADMIN_PANEL.md](ADMIN_PANEL.md).

## Adapting existing scripts

If you have a script written for another QuickJS-based TV app, adapt it as follows:

1. Replace the network call helper (e.g. `fetch`, `request`) with `httpFetch(url, headersJson)`.
2. Return JSON string from `httpFetch` must be parsed with `JSON.parse`.
3. Make sure `search`, `byId` and `resolve` are top-level functions.
4. Use the exact `MediaItem` field names listed above.
5. Add `headers` to the resolved URL or to each item if the stream requires `User-Agent` / `Referer`.

## Debugging

- Plugin errors are caught in `QuickJsMediaSource.call()` and the source returns `null`/`emptyList()` so the app keeps working.
- Use `adb logcat` to see JavaScript exceptions and `console.log` output if the QuickJS wrapper prints it.
- Test `httpFetch` URLs in a browser/curl first to verify headers, cookies and response bodies.
- For dynamic pages, consider using `httpFetch`, parsing the returned HTML in JS, or resolving the video URL in `resolve()`.

## Security & legal notes

- A plugin can only access the network URLs you configure. It cannot read local files outside the app's sandbox.
- Plugins are JavaScript code; only install plugins from sources you trust.
- Respect each website's `robots.txt` and Terms of Service. Polish Media Hub is for personal, legal media only.
