# Changelog

Wszystkie istotne zmiany w Polish Media Hub są dokumentowane w tym pliku.

## [Unreleased]

### Dodano

#### UI / UX
- **Nowoczesny panel boczny** (`ModernSidebarBlurPanel`, `Sidebar`)
  - Zwinięta pływająca pigułka z awatarem / sekcją.
  - Overlay z efektem rozmycia Haze, który nie zmienia szerokości okna z treścią.
  - Automatyczne zwijanie po `1500 ms` braku aktywności.
  - D-Pad LEFT na skrajnym lewym elemencie otwiera panel.
- **Ekran pierwszej konfiguracji** (`EssentialSetupScreen`, `EssentialSetupViewModel`)
  - Flaga `isFirstLaunch` w `SettingsRepository` / DataStore.
  - Trzy wybieralne pakiety startowe: Darmowa Telewizja Internetowa, Muzyka i Podcasty, Katalogi Publiczne Web.
  - Asynchroniczne ładowanie `Dispatchers.IO` przykładowych źródeł z `LegalSourcesRepository` do `ApiConfigRepository`, a następnie nawigacja do `Home`.
- **Nakładka Auto-Play Next** (`PlayerScreen`, `PlayerViewModel`)
  - 15-sekundowe odliczanie przed końcem odcinka serialu.
  - Tło następnego odcinka, tytuł, sezon/odcinek i animowany licznik.
  - Przyciski D-Pad „Odtwórz teraz” i „Anuluj”; BACK działa jak Anuluj.
- **Spoiler Blur** (`DetailScreen`, `DetailViewModel`, `SettingsRepository`)
  - Przełącznik `spoilerBlurEnabled` w Ustawieniach.
  - Opisy nieobejrzanych odcinków są rozmywane `Modifier.blur(16.dp)`.
  - D-Pad Center/SELECT odsłania opis na czas seansu.

#### Odtwarzacz
- Konfiguracja napisów w locie (`SettingsScreen`, `PlayerScreen`, `PlayerViewModel`)
  - Rozmiar (`14sp`, `18sp`, `24sp`, `32sp`), kolor (Biały / Żółty / Szary) i przesunięcie pionowe zapisywane w DataStore.
  - Aplikowane na żywo do `SubtitleView` w ExoPlayerze.
- **Nakładka Nerd Stats** (`PlayerScreen`, `PlayerViewModel`)
  - Przełącznik `showLoadingStats` w Ustawieniach.
  - Panel w czasie rzeczywistym: rozdzielczość wideo + fps, aktywne kodeki wideo/audio, bieżący bitrate, pominięte klatki.
  - Izolowany `StateFlow`, aby uniknąć pełnoekranowych rekompozycji.
- **Odtwarzanie strumieni DRM** (`KodiMediaSource`, `PlayerScreen`, `MediaItem`)
  - `MediaItem` przenosi `drmLicenseUrl`, `drmScheme` i `drmHeaders`.
  - ExoPlayer konfiguruje `MediaItem.DrmConfiguration` dla Widevine, PlayReady i ClearKey.

#### Audio
- **Natywny parser RSS podcastów** (`PodcastRssParser`)
  - Bazuje na `XmlPullParser`; wyciąga `<title>`, `<description>`, `<pubDate>`, `<itunes:image>`, `<itunes:duration>` i `<enclosure>`.
  - Całe parsowanie na `Dispatchers.IO`.
- **Radio internetowe / M3U audio** (`RadioRepository`)
  - Parsuje playlisty `.m3u`, `.m3u8` i `.pls`.
  - Mapuje stacje na `AudioTrack` z `isLive = true` i `MediaItem.Type.AUDIO`.
- **Moduł audio Deezer** (`DeezerAudioSource`)
  - Izolowany `AudioSource` (nie `MediaSource`, nie w `SourceRegistry`) chroniący warstwę federacji wideo.
  - Globalny `OkHttpClient` przez Hilt, z `MemoryCookieJar` i `CloudflareBypassInterceptor`.
  - Modele `@Serializable`: `DeezerTrack`, `DeezerArtist`, `DeezerAlbumRef`, `DeezerAlbumDetail`.
  - Pętle retry na `Dispatchers.IO` cicho zwracają puste listy przy awarii proxy.
  - Pole `preview` mapowane na `streamUrl` z nagłówkami ExoPlayera.
- **Historia audio** (`AudioHistoryEntity`, `AudioHistoryDao`, `AudioHistoryRepository`)
  - Tabela Room filtrowana po aktywnym `profileId` z `ProfileRepository`.
  - `PlayerViewModel` zapisuje postęp podcastu i rozwiązuje utwory audio przez `AudioRepository`.

#### Kodi
- Rozszerzenia `KodiMediaSource`
  - Parsowanie metadanych DRM z odpowiedzi `Files.PrepareDownload` / `Player.Open`.
  - `setAddonSetting(addonId, settingId, value)` wywołujące `Settings.SetSettingValue` do przesyłania tokenów Real-Debrid / Trakt do wtyczek Kodi.
  - `getPluginDirectory(directoryPath)` przez `Files.GetDirectory`, mapujące elementy na `MediaItem`.
- **Wykrywanie Kodi w LAN** (`KodiDiscoveryManager`)
  - Nasłuchiwanie Android NSD dla usługi `_xbmc-jsonrpc._tcp`.
  - Automatyczna aktualizacja URL Kodi w `ApiConfigRepository` po wykryciu serwera.
- Uprawnienia `ACCESS_WIFI_STATE` i `CHANGE_WIFI_MULTICAST_STATE` w manifeście.

#### Wtyczki i dynamiczne ładowanie
- **Dynamiczne ładowanie wtyczek binarnych** (`DynamicPluginLoader`, `ReflectiveMediaSource`, `PluginRepository`)
  - Wtyczki Cloudstream (`.cs3` / `.cs4`) i Aniyomi (`.apk`) ładowane przez `DexClassLoader`.
  - Katalog zoptymalizowanego DEX pod `codeCacheDir/plugins_dex`.
  - Modele dla `index.min.json`, `repo.json`, `plugins.json`, `AniyomiExtension` i `CloudstreamPluginMetadata`.
  - Adapter refleksyjny dla klas wtyczek nieimplementujących bezpośrednio `MediaSource`.
  - Wstrzykiwanie globalnego `OkHttpClient` i przekazywanie nagłówków do `DefaultHttpDataSource.Factory`.
  - Czyszczenie plików DEX przy wyłączaniu / usuwaniu wtyczki.

#### Sieć / anti-bot
- **Stos obejścia Cloudflare** (`MemoryCookieJar`, `HeadlessWebSolver`, `CloudflareBypassInterceptor`)
  - In-memory `CookieJar` synchronizowany per host.
  - Headless `WebView` z Chromecast User-Agent, JS/DOM storage, czekający na `cf_clearance`.
  - Interceptor obsługujący 403/503, nagłówki `User-Agent`/`Referer` i `X-Set-Referer`.
- **Natywny dekoder P.A.C.K.E.R.** (`JsUnpacker`) dla spakowanych skryptów `eval(function(p,a,c,k,...) )`.
- **Dekoder CDA** (`CdaDecoder`) dla ekstrakcji `data-video-id` z `cda.pl`.
- Integracja `WebMediaSource` i `NetworkModule`: `OkHttpClient` używa cookie jar i bypass interceptor; ścieżki CDA i packed JS wstrzykują nagłówki do `MediaItem` dla ExoPlayera.

#### Raportowanie awarii i stabilność
- **Globalny ekran awarii** (`GlobalExceptionHandler`, `CrashReportActivity`)
  - `Thread.UncaughtExceptionHandler` zapisuje `Log.getStackTraceString(throwable)` do `filesDir/last_crash.txt`.
  - Uruchamia `CrashReportActivity` w osobnym procesie `:crashreport`.
  - Ubija zawieszony proces przez `Process.killProcess` + `System.exit(10)`.
  - UI ekranu awarii z ciemnym motywem, czerwoną ikoną ostrzeżenia, przewijanym panelem stacktrace i dwoma przyciskami D-Pad:
    - **Uruchom ponownie aplikację**
    - **Wyczyść pamięć podręczną i zrestartuj** (usuwa `cacheDir` i `codeCacheDir/plugins_dex`)
  - Handler nie jest instalowany w procesie `:crashreport`, aby zapobiec rekurencji.
- `TVHubApplication.onCreate()` rejestruje handler i pomija ciężką inicjalizację w procesie `:crashreport`.

#### Profile i dane
- **Profile użytkowników**
  - `ProfileEntity`, `ProfileDao`, `ProfileRepository`.
  - Encje `WatchedEntity`, `SavedMediaEntity`, `CustomListEntity` i `AudioHistoryEntity` z kluczem obcym `profileId`.
  - `ProfileRepository.currentProfile` jako `StateFlow`, zapis w DataStore.
  - Repozytoria filtrują dane po aktywnym profilu przez `flatMapLatest`.
  - Przełącznik profili w `Sidebar` z awatarem / inicjałem i obsługą PIN.
  - `TvLauncherManager` czyści i ponownie publikuje `WatchNextPrograms` przy przełączaniu profilu.
  - Migracje Room zachowujące istniejące dane pod `default_profile`.

#### Integracja Android TV
- `TvLauncherManager` + `WatchNextHelper` dla kanału **Watch Next**.
- `PreviewChannelHelper` + `RecommendationsWorker` dla kanału rekomendacji.
- Deep linki `polishmediahub://play/{id}` i `polishmediahub://detail/{id}`.
- Globalne wyszukiwanie `SearchActivity` i `SearchRecentSuggestionsProvider`.

#### Legalne źródła startowe
- `LegalSourcesRepository` czyta `assets/legal_sources.json`.
  - `podcastFeeds`, `deezerProxy` i `webSources`.
  - Przykładowe źródła obejmują publiczne IPTV, EPG, dodatki Stremio, kanały RSS podcastów i radio internetowe.

### Zmieniono
- `TVNavHost` czeka na wartość `isFirstLaunch` przed wyborem startu `NavHost`, co zapobiega resetowaniu grafu nawigacji.
- `TvLauncherManager` zapisuje postęp co 15 sekund podczas odtwarzania, z wymuszonym zapisem przy `onPlaybackStopped`.
- `PlayerScreen` reaktywnie pobiera styl napisów i nakładkę Nerd Stats z dedykowanych `StateFlow`.

### Naprawiono
- **Poprawki jakościowe z audytu Claude Sonnet 5**
  - `LibraryScreen` i `WatchlistScreen`: uszkodzony układ `LazyColumn` + `Modifier.fillParentMaxWidth(0.25f)` zastąpiono 5-kolumnową `LazyVerticalGrid` z odstępami `Spacing.md`.
  - Kafelki list własnych w `CustomListsScreen` przekierowują do nowego ekranu `CustomListDetailScreen` (siatka 5 kolumn + `CustomListDetailViewModel`).
  - Wszystkie 22 puste bloki `catch` w kluczowych plikach (`PlayerViewModel`, `AdminHttpServer`, `TorrentHttpServer`, `MediaDatabase`, `DynamicPluginLoader`, `PluginRepository`, `TvLauncherManager`, `WatchNextHelper`) zastąpiono logowaniem `Log.w(...)`.
  - Usunięto martwe zależności `androidx.tv.material` i `androidx.tv.foundation` z `app/build.gradle.kts` i `libs.versions.toml`; poprawiono README.
  - Do poziomych `LazyRow` w `CategoryRow` dodano `Modifier.focusGroup()` i `focusRestorer()`, więc fokus D-Pada wraca do ostatnio oglądanego kafelka przy przejściu między rzędami.
  - Zlokalizowano teksty w `AdminScreen`, `EpgScreen` i `DownloadsScreen`; dodano brakujące `contentDescription` dla strzałek reorder wtyczek i stanu zaznaczenia w `Sidebar`.
  - Naprawiono ostrzeżenia lint: `IntentFilterUniqueDataAttributes`, `DefaultUncaughtExceptionDelegation`, `UseKtx`, `ModifierParameter`, `PluralsCandidate`, `FrequentlyChangingValue`, `SetJavaScriptEnabled`, `RedundantLabel` i inne.
  - Dodano bazę `app/lint.xml` dla pre-existing ostrzeżeń o wersjach zależności i zasobów ikon; `lintDebug` zwraca teraz `No issues found.`
- `WebMediaSource.byId()` pobiera teraz pojedynczą stronę bezparsując całego katalogu.
- `CloudstreamSource.categories()` ładuje zdalne indeksy zamiast zwracać pustą listę.
- `QuickJsEngine` nie używa już `runBlocking` w `httpFetch`.
- `PlayerViewModel` posiada zaktualizowane override'y `AnalyticsListener` zgodnie z API ExoPlayera.

## [1.0.0-alpha] — 2026-07-14

### Dodano
- **Architektura federowanych źródeł mediów**
  - Kontrakt `MediaSource` i `SourceRegistry`.
  - `KodiMediaSource` z JSON-RPC i dekodowaniem ścieżek `image://`.
  - `CloudstreamSource` z ładowaniem `repo.json` / `plugins.json`.
  - `WebMediaSource` ze scrapowaniem Jsoup, walidacją i fallbackiem do QuickJS.
  - `QuickJsEngine` + `QuickJsMediaSource` dla wtyczek `.js`.
  - `FederatedMediaRepository` z reaktywnym cachem w pamięci i `resolve()`.

- **Integracja QuickJS**
  - Globalny mostek sieciowy `httpFetch(url, headersJson)` zwracający JSON `{code, body, headers, error}`.
  - Singleton Hilt z jawnym `dispose()` / `close()` dla czyszczenia kontekstu natywnego.
  - Nagłówki wtyczek (`User-Agent`, `Referer`) przekazywane do ExoPlayera.

- **Strumieniowanie BitTorrent**
  - `TorrentEngine` oparty na `jlibtorrent` z pobieraniem sekwencyjnym i deadline'ami kawałków.
  - Lokalny proxy `TorrentHttpServer` z obsługą `Range: bytes=`.
  - `TorrentInputStream` i postęp buforowania UI przez `StateFlow`.

- **Siatka EPG Timeline Grid**
  - Dwukierunkowo przewijana siatka z zamrożoną kolumną kanałów.
  - Nagłówek 30-minutowy, kafelki proporcjonalne do czasu trwania, marker aktualnego czasu.
  - Focus D-Pad i panel szczegółów z tytułem/rok/gatunkiem/opisem/postępem.

- **Bezprzewodowy panel administracyjny**
  - `AdminHttpServer` serwowany z TV z dynamicznym portem.
  - Generowanie kodu QR przez ZXing.
  - Formularze web dla Kodi, Web, Cloudstream, IPTV/M3U, Jellyfin, Plex, Emby, Subsonic, TMDB, AniList, Trakt, Debrid i podcastów.

- **Bezpieczeństwo i stabilność**
  - `network_security_config.xml` i `usesCleartextTraffic` dla zaufanych HTTP lokalnych/LAN.
  - `TorrentHttpServer` i `AdminHttpServer` z dynamicznymi portami i bezpieczeństwem cyklu życia `ServerSocket`.
  - `RetryInterceptor` z obsługą HTTP 429 `Retry-After`.
  - `DebridAuthenticator` dla odświeżania tokenów Real-Debrid przy 401.
  - `TorBoxService` parsowanie informacji użytkownika.
  - Stabilne migracje Room v1→v7 (bez destruktywnego fallback-u).
  - Usunięto martwy `TmdbMediaRepository.kt` i przebazowano pakiet testowy `com.tvhub.skeleton` → `com.polishmediahub.app`.

- **Doświadczenie odtwarzacza**
  - Preferencja polskiego audio/napisów (`pl`).
  - Depriorytyzacja roli Audio Description.
  - Pełne etykiety ścieżek w `PlayerControls` (np. "Polski (Lektor)", "Polski (Dubbing)").
  - Detekcja strumieni HLS, DASH i MP4.
  - Wsparcie napisów VTT/SRT.

- **UI / UX**
  - Jetpack Compose for TV, `FocusableSurface`, `TVCard`, `WideCard`, nawigacja boczna.
  - Ekrany Search, Home, Detail, Player, Library, Watchlist, Settings, Admin, EPG, Torrents, Music, Downloads i Custom Lists.
  - Blokada PIN dla Settings i Admin.
  - Testy zrzutów ekranu Paparazzi i instrumentacyjne testy D-Pad.

## Wcześniejsza historia pre-release

- Początkowy szkielet Android TV z Hilt, Room i Jetpack Compose.
- Podstawowe wsparcie źródeł Kodi i IPTV.
- Ekrany Settings, Library i Watchlist.
