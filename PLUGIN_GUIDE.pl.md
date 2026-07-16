# Polish Media Hub — Przewodnik tworzenia wtyczek

Wtyczki pozwalają dodawać nowe źródła do Polish Media Hub bez modyfikowania aplikacji Android. Aplikacja obsługuje trzy formaty wtyczek:

1. **Wtyczki QuickJS** — pliki JavaScript wykonywane przez wbudowany silnik **QuickJS**.
2. **Wtyczki Cloudstream** — binarne pliki DEX `.cs3` / `.cs4`.
3. **Rozszerzenia Aniyomi** — pakiety Android `.apk` ładowane dynamicznie.

*English version: [`PLUGIN_GUIDE.md`](PLUGIN_GUIDE.md)*

## Wtyczki QuickJS

Wtyczki QuickJS są najłatwiejsze do napisania i nie wymagają narzędzi Android.

### Obsługiwane typy źródeł

Aplikacja rozpoznaje wtyczki przez wpisy `PluginManifest` z `"type": "quickjs"`. Manifest można załadować z URL w panelu Admin lub wkleić jako raw script. Runtime to klasa `QuickJsMediaSource`.

### Wymagane funkcje JavaScript

Wtyczka musi udostępniać co najmniej jedną z poniższych funkcji najwyższego poziomu. Brakujące funkcje oznaczają po prostu niedostępność danej funkcji dla tego źródła.

```js
function featured() {
  return [ /* obiekty MediaItem */ ];
}

function categories() {
  return [ /* obiekty MediaItem */ ];
}

function search(query) {
  return [ /* obiekty MediaItem */ ];
}

function byId(id) {
  return { /* pojedynczy obiekt MediaItem */ };
}

function resolve(idOrMediaItemId) {
  return "https://...";
  // lub return { url: "https://..." };
  // lub return { url: "https://...", headers: { "User-Agent": "..." } };
}
```

Wszystkie funkcje są wywoływane z korutyn Kotlin na `Dispatchers.IO`, więc wywołania sieciowe wewnątrz `httpFetch` są synchroniczne i blokują tylko wątek JS.

### Globalna funkcja `httpFetch(url, headers)`

Silnik rejestruje globalną funkcję JS wykonującą żądanie HTTP przez OkHttp i zwracającą łańcuch JSON:

```js
var response = httpFetch("https://example.com/page");
// lub z nagłówkami (obiekt JS):
var response = httpFetch(
  "https://example.com/page",
  { "User-Agent": "PolishMediaHub/1.0", "Referer": "https://example.com" }
);

var r = JSON.parse(response);
console.log(r.code);     // kod HTTP
console.log(r.body);     // ciało odpowiedzi jako string
console.log(r.headers);  // obiekt nagłówków odpowiedzi
console.log(r.error);    // komunikat błędu, jeśli wystąpił
```

`headers` może być obiektem JavaScript lub łańcuchem JSON. Do OkHttp przekazywane są tylko wartości tekstowe.

Użyj `r.code` do wykrywania 403/429, odczytaj ciasteczka z `r.headers["Set-Cookie"]` i zaimplementuj logowanie / zarządzanie sesją wewnątrz wtyczki.

### Pola obiektu MediaItem

`QuickJsMediaSource.mapToMediaItem` konwertuje obiekty JS / Mapy na `MediaItem` aplikacji. Używaj tych nazw pól:

| Pole | Typ | Wymagane | Uwagi |
|------|-----|----------|-------|
| `id` | String | tak | Unikalne ID. Prefiks przestrzeni nazw, np. `myplugin:1234`. |
| `title` | String | tak | Wyświetlany tytuł. |
| `type` | String | nie | `MOVIE`, `SERIES`, `EPISODE`, `CHANNEL`, `MUSIC`, `PODCAST`, `TORRENT`, `AUDIO`. Domyślnie `MOVIE`. |
| `description` | String | nie | Fabuła / streszczenie. |
| `posterUrl` | String | nie | URL plakatu. |
| `backdropUrl` | String | nie | URL tła. |
| `year` | Number / String | nie | Rok wydania. |
| `season` | Number | nie | Dla odcinków seriali. |
| `episode` | Number | nie | Dla odcinków seriali. |
| `videoUrl` lub `url` | String | nie | Bezpośredni URL strumienia / manifestu. Jeśli obecny, aktywny silnik odtwarzacza (ExoPlayer lub LibVLC) używa go bezpośrednio. |
| `headers` | Object | nie | Nagłówki HTTP przekazywane do aktywnego silnika odtwarzacza, np. `{ "User-Agent": "...", "Referer": "..." }`. |
| `subtitleUrl` | String | nie | Zewnętrzny plik napisów (`.vtt` lub `.srt`). |
| `subtitleHeaders` | Object | nie | Per-ścieżka HTTP nagłówki (np. `Authorization`) używane przy pobieraniu `subtitleUrl`. |
| `subtitleLanguage` | String | nie | Kod języka, domyślnie `pl`. |
| `drmLicenseUrl` | String | nie | URL serwera licencji DRM. |
| `drmScheme` | String | nie | `WIDEVINE`, `PLAYREADY` lub `CLEARKEY`. |
| `drmHeaders` | Object | nie | Nagłówki wysyłane do serwera licencji DRM. |

### Formaty strumieni i napisów

Domyślny silnik ExoPlayer automatycznie wykrywa typ strumienia z rozszerzenia URL i MIME type:

- `.m3u8` lub `application/x-mpegurl` → HLS
- `.mpd` lub `application/dash+xml` → DASH
- `.mp4`, `.mkv`, `.webm` itp. → progresywny

Napisy mogą być `.vtt` lub `.srt`. Podaj `subtitleLanguage`, aby dopasować język użytkownika. Jeśli potrzebujesz dźwięku DTS/AC3 lub starszych kontenerów MKV/AVI, których ExoPlayer nie dekoduje, użytkownik może włączyć wbudowany silnik LibVLC w Ustawieniach / panelu Admina.

### Minimalny przykład wtyczki QuickJS

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
    videoUrl: null // rozwiązany później
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

### Manifest wtyczki QuickJS

Wtyczka zazwyczaj dystrybuowana jest jako JSON `PluginManifest` plus jeden lub więcej plików `.js`. Repozytorium zawiera gotowe scrapery QuickJS w `plugins/manifest.json` oraz osobne pliki `plugins/*.js`.

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

`config.scriptUrl` pobiera plik `.js` przez HTTPS. Możesz też osadzić skrypt bezpośrednio w `config.script`. Oba sposoby działają; osadzenie sprawia, że wtyczka jest samowystarczalna i jest używane przez dołączone scrapery.

Raw script możesz także wkleić bezpośrednio w panelu Admin. Zobacz [ADMIN_PANEL.md](ADMIN_PANEL.md) / [ADMIN_PANEL.pl.md](ADMIN_PANEL.pl.md).

## Wtyczki binarne Cloudstream / Aniyomi

Pliki Cloudstream `.cs3` / `.cs4` i pakiety Aniyomi `.apk` mogą być ładowane bez instalowania ich przez systemowy installer Android. Aplikacja używa `DexClassLoader` w izolowanym katalogu pod `context.codeCacheDir/plugins_dex`.

### Indeksy repozytoriów

Repozytoria Cloudstream mają punkt wejścia `repo.json` wskazujący na `plugins.json`:

```json
{
  "name": "Example Repo",
  "url": "https://example.com/plugins.json"
}
```

`plugins.json` to lista metadanych wtyczek:

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

Repozytoria Aniyomi to pliki `index.min.json`. Aplikacja dostarcza też domyślny polski URL repozytorium Aniyomi w `legal_sources.json` (`aniyomiRepo`) i ładuje go automatycznie:

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

`PluginRepository` rozwiązuje względne nazwy `apk` względem bazy URL repozytorium i wyprowadza najlepszy `mainClass` (`{pkg}.{SourceName}`) dla `DexClassLoader`.

### Ładowanie i adaptacja

`DynamicPluginLoader` ładuje plik binarny, a następnie `ReflectiveMediaSource` adaptuje klasy wtyczek, które nie implementują bezpośrednio `MediaSource` aplikacji. Adapter używa refleksji do wykrywania powszechnych metod, takich jak `search`, `getMainPage`, `load`, `getVideoExtractor` itp.

### Współdzielenie sieci

Załadowane wtyczki otrzymują globalny `OkHttpClient` przez wstrzykiwanie konstruktora, jeśli akceptują parametr typu `okhttp3.OkHttpClient` lub `android.content.Context`. Oznacza to, że bypass Cloudflare, `CookieJar`, obsługa `User-Agent`/`Referer` i retry są współdzielone.

### Nagłówki ExoPlayera

Wszystkie nagłówki HTTP zwrócone przez wtyczkę binarną są dołączane do `MediaItem.headers` i przekazywane do `DefaultHttpDataSource.Factory`.

### Czyszczenie pamięci

Gdy wtyczka binarna jest wyłączana lub usuwana, `PluginRepository` usuwa zoptymalizowane pliki DEX z `codeCacheDir/plugins_dex` oraz pobrany plik APK/DEX z `cacheDir/plugins/`.

## Adaptacja istniejących skryptów

### QuickJS

1. Zamień helper wywołań sieciowych (np. `fetch`, `request`) na `httpFetch(url, headersJson)`.
2. Wynik `httpFetch` to łańcuch JSON — sparsuj go przez `JSON.parse`.
3. Upewnij się, że `search`, `byId` i `resolve` są funkcjami najwyższego poziomu.
4. Używaj dokładnych nazw pól `MediaItem` podanych powyżej.
5. Dodaj `headers` do rozwiązanego URL-a lub do każdego elementu, jeśli strumień wymaga `User-Agent` / `Referer`.

### Cloudstream / Aniyomi

1. Dostarcz osiągalne `repo.json` / `plugins.json` lub `index.min.json`.
2. Upewnij się, że plik binarny jest dostępny przez HTTPS i nie jest zbyt zaciemniony dla `DexClassLoader`.
3. Jeśli wtyczka eksponuje klasę `MainAPI`, `ReflectiveMediaSource` wykryje powszechne nazwy metod i je zaadaptuje.

## Debugowanie

- Błędy wtyczek są łapane w `QuickJsMediaSource.call()`, `DynamicPluginLoader` i `ReflectiveMediaSource`, więc aplikacja nadal działa.
- Użyj `adb logcat`, aby zobaczyć wyjątki JavaScript, błędy ładowania DEX i ewentualne wyjście `console.log`, jeśli wrapper QuickJS je drukuje.
- Najpierw przetestuj URL-e `httpFetch` w przeglądarce / curl, aby zweryfikować nagłówki, ciasteczka i ciała odpowiedzi.
- Dla stron dynamicznych użyj `httpFetch`, sparsuj zwrócony HTML w JS lub rozwiąż URL wideo w `resolve()`.
- Jeśli wtyczka Cloudstream/Aniyomi nie ładuje się, sprawdź `logcat` pod kątem `ClassNotFoundException` lub brakującego konstruktora.

## Uwagi bezpieczeństwa i prawne

- Wtyczka może uzyskiwać dostęp tylko do URL-i sieciowych, które skonfigurujesz. Nie może czytać lokalnych plików poza sandboxem aplikacji.
- Wtyczki to kod JavaScript lub skompilowany kod; instaluj je tylko ze źródeł, którym ufasz.
- Szanuj `robots.txt` i regulaminy każdej strony. Polish Media Hub jest przeznaczony wyłącznie do osobistego, legalnego użytku mediów.
