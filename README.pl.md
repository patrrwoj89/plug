# Polish Media Hub

Nowoczesny hub multimedialny dla Android TV / Google TV, zbudowany w Jetpack Compose for TV. Agreguje filmy, seriale, kanały IPTV, muzykę, podcasty i radio internetowe z legalnych źródeł dostarczonych przez użytkownika poprzez ujednolicony interfejs przyjazny dla pilota D-Pad.

Aplikacja jest przeznaczona **wyłącznie do użytku osobistego** i **nie zawiera w sobie** żadnych pirackich treści, trackerów, nieautoryzowanych playlist IPTV ani nielegalnych scraperów. Wszystkie strumienie, torrenty i źródła multimediów muszą zostać dodane przez użytkownika i używane zgodnie z obowiązującym prawem oraz regulaminem wybranego źródła.

*English version: [`README.md`](README.md)*

## Funkcje

### Podstawowe źródła i odtwarzanie

- **Interfejs TV-first** oparty na Jetpack Compose Foundation i Material3, obsługa focusu pilotem D-Pad, efekty powiększenia / podświetlenia oraz nowoczesny **panel boczny**.
- **Sfera źródeł mediów** agregowana przez `FederatedMediaRepository` i `SourceRegistry`:
  - **Kodi** — integracja JSON-RPC ze wsparciem DRM, przeglądaniem katalogów wtyczek (`Files.GetDirectory`), automatycznym wykrywaniem serwera w LAN (`KodiDiscoveryManager`) i zdalnym zapisem ustawień (`Settings.SetSettingValue`).
  - **Cloudstream** — repozytoria i wtyczki oraz **Aniyomi** rozszerzenia `.apk` ładowane dynamicznie przez `DexClassLoader`.
  - **Wtyczki QuickJS** (`.js`) z globalnym mostkiem sieciowym `httpFetch`, nagłówkami i asynchroniczną ewaluacją.
  - **Web scraping** przez konfigurację JSON z walidacją i dynamicznym fallbackiem do QuickJS.
  - **Silnik Offloadingu Chmury Cloudflare** (`cloudflare-resolver/`): opcjonalny Worker TypeScript/Wrangler (`https://*.workers.dev/resolve`) wykonuje ciężkie wyciąganie linków web (rozpakowywanie P.A.C.K.E.R, dekoder CDA, regex URL mediów) w chmurze. `WebMediaSource.resolve()` najpierw odpytuje Workera, gdy opcja jest włączona, i transparentnie wraca do lokalnego resolvera Kotlina przy błędach sieci, kodach 5xx lub 403. Zwrócone nagłówki strumienia są scalane do `MediaItem.headers`.
  - **IPTV/M3U** z obsługą XMLTV EPG, lokalnym cache Room, tłem odświeżaniem przez `IptvUpdateWorker` oraz profesjonalną **siatką EPG Timeline Grid**.
  - **Jellyfin, Plex, Emby, Subsonic/Airsonic, Stremio, AniList, TMDB, Trakt, MDBList, podcasty (RSS)**, radio internetowe i proxy Deezer.
  - **Integracja MDBList** (`MdbListMediaSource`): publiczne listy top, listy użytkownika, wyszukiwanie mediów i lookup po identyfikatorach imdb/tmdb/trakt/tvdb; każdy element zawiera `tmdbId`, `imdbId` i `traktId`, co ułatwia dopasowanie do innych źródeł.
  - **Reaktywny fallback Kitsu dla anime** (`KitsuMediaSource`, `AnimeRepository`): gdy AniList GraphQL zawiedzie (błąd sieci, timeout, 429), aplikacja automatycznie i cicho przełącza się na Kitsu JSON:API. Z `include=mappings` parsowane są powiązane `malId` oraz `aniListId`, aby zachować zgodność z resolverami strumieni.
  - **Fallback metadanych Filmweb.pl** (`FilmwebMediaSource`, `FederatedMediaRepository`): gdy szczegóły z TMDB nie zawierają opisu, zawierają zbyt krótki opis lub brak polskich znaków diakrytycznych, aplikacja pobiera polski tytuł, fabułę, plakat i społecznościową ocenę z publicznego API Filmweb (`www.filmweb.pl/api/v1`). Wyniki są zapisywane w lokalnej bazie Room v14, dzięki czemu przyszłe wejścia na kartę szczegółów są natychmiastowe, a obok oceny TMDB wyświetlana jest etykieta `Filmweb: X.X`.
- **Strumieniowanie BitTorrent** przez `jlibtorrent` z pobieraniem sekwencyjnym, lokalnym serwerem HTTP i UI buforowania.
- **Dwukierunkowa synchronizacja z Trakt.tv** (`TraktSyncWorker`) co 6 godzin: pobiera historię obejrzanych i listę do obejrzenia z Trakt do Room, wysyła lokalną historię/listę do obejrzenia z Room z powrotem do Trakt, z rozwiązywaniem konfliktów na podstawie timestampu. Odtwarzacz ExoPlayer wysyła `/scrobble/start`, `/scrobble/pause` i `/scrobble/stop` z dokładnym procentem postępu.
- **Muzyka i audio**:
  - Natywny parser RSS podcastów (`PodcastRssParser`) z tagami iTunes i URL-ami audio z `<enclosure>`.
  - Repozytorium radia internetowego M3U/PLS (`RadioRepository`).
  - Integracja proxy Deezer przez izolowany `DeezerAudioSource`.
  - Historia odsłuchu audio w Room na poziomie profilu.

### Interfejs TV / UX

- **Nowoczesny panel boczny** — pływająca pigułka, overlay z efektem rozmycia Haze, brak drgania layoutu, automatyczne zwijanie po 1500 ms, otwieranie D-Pad LEFT; pozycje menu pogrupowane w sekcje Odkrywaj, Biblioteka, Multimedia, Pobrane i System, a zablokowane profile wyświetlają ikonę `Icons.Default.Lock` z lokalizowanym opisem.
- **Ekran pierwszej konfiguracji** (Essential Addon Setup) dla nowych profili: jednym kliknięciem ładuje legalne pakiety startowe (IPTV/EPG, muzyka/podcasty, katalogi web, MDBList) przez `EssentialSetupScreen` i `EssentialSetupViewModel`.
- **Biblioteka, Do obejrzenia i Listy własne** wyświetlają siatkę 5 kolumn (`CustomListDetailScreen` dla zawartości pojedynczej listy własnej) z `Modifier.focusGroup()` + `focusRestorer()`, więc fokus pilota wraca do ostatnio oglądanego kafelka po powrocie z ekranu szczegółów.
- **Przywracanie fokusu D-Pada** w poziomych `LazyRow` (`CategoryRow`) przez `Modifier.focusGroup()` + `focusRestorer()` — fokus wraca do ostatnio oglądanego kafelka przy przechodzeniu między rzędami.
- **Wyszukiwanie głosowe** w `SearchScreen`: przycisk D-Pad z mikrofonem uruchamia `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` w języku polskim (`pl-PL`) i wstawia rozpoznaną frazę do pola wyszukiwania.
- **Nakładka Auto-Play Next** dla seriali: 15-sekundowe odliczanie, metadata następnego odcinka, przyciski „Odtwórz teraz” i „Anuluj”, BACK działa jak Anuluj.
- **Płynne animacje przejść współdzielonych elementów** (Shared Element Transitions) dla plakatów filmów (`TVNavHost`, `TVCard`, `DetailScreen`, `SharedTransitionLayout`): `NavHost` jest owinięty w `SharedTransitionLayout`; `MediaCard` i główny plakat w `DetailScreen` współdzielą ten sam klucz dzięki `Modifier.sharedElement`, dzięki czemu plakaty płynnie animują się z siatki/rzędu do ekranu szczegółów i z powrotem podczas nawigacji pilotem D-Pad.
- **Hero Featured Banner** na `HomeScreen`: kinowe tło z lewym i dolnym gradientem wygaszania do czerni, tytuł, rok, odznaki oceny TMDB/Filmweb oraz skupiony przycisk pilota D-Pad „Odtwórz teraz ▷" `TvButton`.
- **Spoiler Blur** — opisy nieobejrzanych odcinków rozmyte `Modifier.blur(16.dp)`, odkrywane D-Pad Center/SELECT.
- **Ustawienia napisów w locie**: rozmiar, kolor i przesunięcie pionowe zapisywane w DataStore i aplikane na żywo w `SubtitleView`.
- **Nakładka Nerd Stats**: panel diagnostyczny w prawym górnym rogu (rozdzielczość, fps, kodeki, bitrate, pominięte klatki).
- **Zaokrąglenie okładek i awatarów**: `MediaCard` i `WideCard` przycinają `AsyncImage` za pomocą `RoundedCornerShape(Radius.md)`; awatary profili w `Sidebar` i `CollapsedSidebarPill` używają `ContentScale.Crop` i `fillMaxSize()` w okrągłych kontenerach.
- **Wyprzedzające pobieranie plakatów i dyskowy cache Coil** (`HomePreFetchWorker`, `ImageLoaderFactory`, `TVCard`): zadanie `WorkManager` uruchamiane co 12 godzin rozgrzewa URL-e plakatów i tła ekranu głównego w 100 MB trwałym cache dyskowym Coil przy niezlimitowanym Wi-Fi, gdy urządzenie jest bezczynne. Każdy `AsyncImage` używa jawnego `ImageRequest` z `diskCachePolicy(ENABLED)` i `memoryCachePolicy(ENABLED)`, więc siatka główna renderuje się natychmiast nawet offline.

### Odtwarzacz i media

- **ExoPlayer / Media3** z autodetekcją HLS, DASH i progresywnych strumieni.
- **Odtwarzanie DRM**: `MediaItem` przenosi `drmLicenseUrl`, `drmScheme` i `drmHeaders`; ExoPlayer konfiguruje `DrmConfiguration` dla Widevine/PlayReady/ClearKey.
- **Polskie audio / napisy** z inteligentnym wyborem ścieżek i normalizacją głośności:
  - ExoPlayer preferuje utwory `pl`, depriorytyzuje Audiodeskrypcję i pokazuje pełne etykiety językowe.
  - Nowe ustawienia w DataStore: `preferredAudioType` (Polski Lektor / Dubbing), `nightModeEnabled` oraz `dialogueBoostGainmB` (0–3000 mB).
  - Tryb Dubbing preferuje ścieżki wielokanałowe (`5.1`, `E-AC3`, `DTS`); tryb Lektor preferuje stereo/mono z flagą lektorską. `ROLE_FLAG_DESCRIBES_VIDEO` zawsze ma najniższy priorytet.
  - Natywny efekt `android.media.audiofx.LoudnessEnhancer` jest wiązany z `audioSessionId` ExoPlayera; gdy Tryb Nocny jest włączony, aplikuje skonfigurowane wzmocnienie, spłaszczając dynamikę i podbijając ciche dialogi. Efekt jest zwalniany przy zamykaniu playera, pauzie i przełączaniu na LibVLC.
  - Ta sama logika wyboru została przeniesiona do alternatywnego odtwarzacza **LibVLC** (`UniversalVlcPlayer`) dla pełnej parytetowości silników.
- **Napisy zewnętrzne**: pliki `.vtt` i `.srt`.
- **Inteligentny Tryb Kinowy**: po włączeniu nakładki automatycznie ukrywają się podczas odtwarzania, a ekran płynnie przyciemnia się; po pauzie nakładka rozjaśnia się i pod suwakiem pojawia się karta informacyjna z tytułem, opisem, gatunkami i głównymi aktorami pobranymi w tle z metadanych TMDB / Trakt.
- **Logowanie Trakt.tv kodem urządzenia** (`TraktAuthManager`, `TraktPairingSection`): aplikacja na Android TV prosi o kod urządzenia, wyświetla `user_code` i kod QR z adresem aktywacji, a następnie odpytuje `oauth/device/token` do momentu autoryzacji. Tokeny dostępowe i odświeżania są szyfrowane AES-256-GCM w Android Keystore.
- **Natywny tryb obrazu w obrazie** (`MainActivity`, `PlayerScreen`): gdy odtwarzanie jest aktywne i użytkownik opuszcza aplikację, odtwarzacz przechodzi w PiP; ekran odtwarzacza ukrywa kontrolki, napisy, Nerd Stats, kartę kinową i nakładkę następnego odcinka, pozostawiając sam strumień wideo. Powrót do pełnego ekranu przywraca pełny interfejs.
- **Inteligentne pomijanie czołówki i końcówki** (`PlayerScreen`, `PlayerViewModel`): wtyczki mogą dostarczać dokładne znaczniki czasowe `introStartMs/introEndMs/outroStartMs/outroEndMs` w `MediaItem`; w przeciwnym razie odtwarzacz używa konfigurowalnych domyślnych długości czołówki i końcówki. Półprzezroczysty przycisk obsługiwany pilotem D-Pad wysuwa się podczas segmentu, aby pominąć czołówkę lub wywołać nakładkę Auto-Play Next.
- **Alternatywny silnik LibVLC** (`UniversalVlcPlayer`): wbudowany silnik `org.videolan.android:libvlc-all` działający bezpośrednio w procesie aplikacji. Włącz go w Ustawieniach / panelu Admina, aby ominąć problemy ExoPlayera z dekodowaniem dźwięku DTS/AC3 oraz kontenerów MKV/AVI z torrentów, Kodi i wtyczek webowych. Przekazuje `mediaItem.headers`, dodaje domyślny `User-Agent`, dołącza napisy zewnętrzne przez `Media.addSlave` wraz z `subtitleHeaders` i odpytuje `mediaPlayer.time`, aby scrobbling Trakt, historia Room, nakładka Auto-Play Next i Smart Skip Intro/Outro działały także na silniku LibVLC. Powiela również logikę polskiego doboru ścieżki dźwiękowej i napisów (preferuje `pl`/`pol`, depriorytetyzuje audiodeskrypcję), stosuje ustawienia rozmiaru/koloru/przesunięcia napisów przez opcje LibVLC oraz wyświetla nakładkę Nerd Stats i Tryb Kinowy nad `VLCVideoLayout`.
- **Nagłówki strumieni**: `User-Agent`, `Referer`, `Cookie` i niestandardowe nagłówki przekazywane do `DefaultHttpDataSource.Factory` (ExoPlayer) lub wstrzykiwane do opcji `LibVLC` (silnik LibVLC).

### Sieć i ochrona przed botami

- **Obejście Cloudflare**: headless `WebView` (`HeadlessWebSolver`), `MemoryCookieJar`, `CloudflareBypassInterceptor` oraz natywny dekoder P.A.C.K.E.R. (`JsUnpacker`).
- **Silnik Offloadingu Chmury Cloudflare** (`cloudflare-resolver/`): opcjonalny Cloudflare Worker uruchamiający tę samą logikę rozpakowywania/dekodowania na brzegu sieci, odciążając procesor telewizora. Konfigurowany z `Ustawienia → Offloading Chmury Cloudflare` lub bezprzewodowego panelu admina.
- **Dekoder CDA** (`CdaDecoder`) dla ekstrakcji `data-video-id` z `cda.pl`.
- Globalny `OkHttpClient` współdzielony przez repozytoria i kod wtyczek.

### Admin i konfiguracja

- **Bezprzewodowy panel administracyjny** serwowany lokalnie z kodem QR (ZXing) do łatwej konfiguracji źródeł z telefonu lub komputera. Unikalny token parowania generowany na sesję jest osadzony w URL admina (`/admin?token=...`), a wszystkie endpointy `/api/*` odrzucają brakujący lub niepoprawny `?token=...` oraz nieoczekiwane nagłówki `Origin` kodem HTTP 403, blokując nieautoryzowaną instalację wtyczek przez LAN. Endpoint `/api/config` maskuje odszyfrowane klucze API i hasła w odpowiedzi JSON, pokazując tylko pierwsze 4 i ostatnie 4 znaki.
- **Silnik monitorowania zdrowia źródeł w tle** (`HealthCheckWorker`) uruchamia się co 4 godziny (i przy zimnym starcie) na `Dispatchers.IO` z ograniczeniami sieci/baterii. Sprawdza dostępność Kodi (`JSONRPC.Version`), Jellyfin/Emby (`/System/Info/Public`), Plex (`/identity`), Subsonic (`/rest/ping.view`), URL-i IPTV/EPG, endpointy Stremio/Cloudstream/web oraz proxy Deezer z timeoutem 3 sekund, zapisuje status każdego źródła (`ONLINE`/`OFFLINE`/`UNCONFIGURED`) jako JSON w DataStore i wyświetla kolorowe kropki w `Ustawieniach` oraz bezprzewodowym panelu admina.
- **Tło aktualizowanie EPG/IPTV** (`IptvUpdateWorker`) uruchamia się co 12 godzin (i przy zimnym starcie) na `Dispatchers.IO` z ograniczeniami unmetered/idle/battery-not-low, zapisuje kanały i programy do Room, dzięki czemu ekran telewizji na żywo otwiera się natychmiast.
- **Parowanie Trakt.tv kodem urządzenia**: w `Ustawienia → Synchronizacja Trakt` lub w panelu admina wpisz **Trakt client ID** i **client secret**, dotknij **Zaloguj się przez Trakt**, a następnie zeskanuj kod QR lub wpisz wyświetlony `user_code` na stronie aktywacji na telefonie.
- **Tło synchronizacji Trakt.tv** (`TraktSyncWorker`) uruchamia się co 6 godzin z ograniczeniami sieci/baterii. Pobiera historię obejrzanych i listę do obejrzenia z Trakt do Room oraz wysyła lokalne zmiany z powrotem do Trakt. Od czerwca 2026 r. Trakt wymusza paginację na `sync/watched` i `sync/watchlist`; repozytorium odczytuje `X-Pagination-Page-Count` i ładuje wszystkie strony (max 250 na stronę). Można też uruchomić natychmiastową synchronizację z poziomu Ustawień lub panelu admina.
- **Automatyczne odświeżanie tokenów Trakt** (`TraktAuthenticator`): gdy zapytanie do API Trakt zwróci 401, dedykowany klient `OkHttpClient` odświeża `access_token` za pomocą zaszyfrowanego `refresh_token`, zapisuje nowe tokeny w Android Keystore i transparentnie ponawia pierwotne żądanie.
- **Ustawienia pomijania czołówki / końcówki**: przełącznik oraz domyślne czasy trwania można zmienić w `Ustawienia` lub w bezprzewodowym panelu admina.
- **Ustawienia offloadingu Cloudflare**: przełącznik, URL Workera oraz maskowany token autoryzacyjny można ustawić w `Ustawienia → Offloading Chmury Cloudflare` lub w bezprzewodowym panelu admina.
- **Onboarding pierwszego uruchomienia** pozwala nowym użytkownikom wybrać legalne pakiety startowe.

### Integracja z Android TV

- **Wiersz Watch Next per profil** (`TvLauncherManager`, `WatchNextHelper`)
  - Wiersz launchera jest powiązany z `ProfileRepository.currentProfile`; przy każdej zmianie profilu systemowe `WatchNextPrograms` są czyszczone i ponownie publikowane wyłącznie z niedokończonymi seansami oraz listą do obejrzenia bieżącego użytkownika.
  - Wszystkie pozycje przepuszczane są przez `ContentFilter` z limitami `maxAgeRating` i `allowNsfw` aktywnego profilu; brak zadeklarowanego wieku powoduje odrzucenie dla profili z ograniczeniem wiekowym (tryb fail-closed).
  - Pozycje wznowienia używają typu `WATCH_NEXT_TYPE_CONTINUE`; pozycje z listy do obejrzenia używają `WATCH_NEXT_TYPE_WATCHLIST`.
- **Kanał rekomendacji / Preview per profil** (`TvLauncherManager`, `RecommendationsWorker`)
  - Kanał budowany jest z `FederatedMediaRepository.featured()` (MDBList, Kitsu i chmury domowe), filtrowany do filmów/seriali/odcinków i ponownie filtrowany przez kontrolę rodzicielską aktywnego profilu.
  - `RecommendationsWorker` odświeża kanał co 12 godzin przy `NetworkType.CONNECTED` i `requiresBatteryNotLow`.
  - Wszystkie operacje `ContentResolver` wykonują się na `Dispatchers.IO`, kursory używają `.use`, a `ContentFilter` odrzuca niedozwolone pozycje przed jakimkolwiek IPC z systemowym launcherem.
- **Globalne wyszukiwanie Android TV** przez `SearchActivity` i `SearchRecentSuggestionsProvider`.

### Bezpieczeństwo, stabilność i profile

- **Wielu użytkowników / profile** z per-profilową historią, biblioteką, listami obserwowanych, listami własnymi i historią audio; przełącznik profili w panelu bocznym i opcjonalny PIN na profil.
- **Kontrola Rodzicielska** dla każdego profilu: pola `maxAgeRating` i `allowNsfw` w encji `ProfileEntity` (baza Room v12). `ContentFilter` działa w trybie „fail-closed”: pozycje bez zadeklarowanego ograniczenia wiekowego są ukrywane, gdy aktywny profil ma ustawiony limit wieku; pozycje z kategorią powyżej limitu lub oznaczone jako treści dla dorosłych/NSFW są usuwane z wyników `search()`, `categories()` i `featured()` w `CompositeMediaRepository`, `FederatedMediaRepository` i `PluginMediaSource`. Zarządzana w chronionej PIN-em sekcji **Kontrola Rodzicielska** w `SettingsScreen`.
- **Blokada PIN** dla Ustawień i ekranu Admin.
- **Pobieranie** audio i wideo przez `WorkManager`.
- **Szyfrowanie wrażliwych ustawień** (`EncryptedSettingsManager`): klucze API, tokeny OAuth i hasła (TMDB, AniList, Trakt, Debrid, tokeny Jellyfin/Plex/Emby, hasło Subsonic, klucz MDBList) są szyfrowane algorytmem AES-256-GCM z wykorzystaniem sprzętowo chronionego klucza z Android Keystore przed zapisem do DataStore. Zwykłe preferencje (motyw, jakość, status EPG itp.) pozostają w jawnej postaci.
- **Globalny ekran awarii**: nieobsługiwane wyjątki są łapane przez `GlobalExceptionHandler`, zapisywane do pliku i uruchamiana jest `CrashReportActivity` w osobnym procesie `:crashreport`, która pozwala na restart, wyczyszczenie pamięci podręcznej i restart lub bezpieczne wysłanie raportu do chmury (Cloudflare Worker `POST /report-error` z autoryzacją `X-Hub-Token`, po offline-owej anonimizacji przez `CrashReportSanitizer`) — bez nagłego powrotu do launchera Android TV.
- **Testy zrzutów ekranu** z Paparazzi i testy instrumentacyjne D-Pad.

## Dokumentacja

- [`README.md`](README.md) / [`README.pl.md`](README.pl.md) — przegląd projektu.
- [`FAQ.md`](FAQ.md) / [`FAQ.pl.md`](FAQ.pl.md) — często zadawane pytania.
- [`CHANGELOG.md`](CHANGELOG.md) / [`CHANGELOG.pl.md`](CHANGELOG.pl.md) — historia zmian.
- [`PLUGIN_GUIDE.md`](PLUGIN_GUIDE.md) / [`PLUGIN_GUIDE.pl.md`](PLUGIN_GUIDE.pl.md) — tworzenie i adaptacja wtyczek QuickJS, Cloudstream i Aniyomi.
- [`ADMIN_PANEL.md`](ADMIN_PANEL.md) / [`ADMIN_PANEL.pl.md`](ADMIN_PANEL.pl.md) — konfiguracja przez lokalny panel web i QR.
- [`LEGAL_SOURCES.md`](LEGAL_SOURCES.md) / [`LEGAL_SOURCES.pl.md`](LEGAL_SOURCES.pl.md) — lista legalnych / publicznych / self-hostowalnych źródeł.

## Tech stack

- Android Gradle Plugin 9.0.0, Gradle 9.1.0, Kotlin 2.3.0
- Jetpack Compose BOM 2026.06.01 (`compose.material3`, `compose.ui`, `compose.foundation`)
- Hilt 2.60.1 + `androidx.hilt` + `hilt-navigation-compose`
- ExoPlayer / Media3 1.10.1
- Room 2.8.4, DataStore 1.2.1, WorkManager 2.11.2
- OkHttp 4.12.0, Retrofit 2.12.0, Jsoup 1.22.2
- Kotlinx Serialization 1.8.0
- Coil 2.7.0
- jlibtorrent 2.0.12.9 (FrostWire Maven)
- LibVLC 4.0.0-eap24 (`org.videolan.android:libvlc-all`) jako alternatywny silnik odtwarzacza w procesie aplikacji
- QuickJS wrapper 3.2.3 (`wang.harlon.quickjs:wrapper-android`)
- ZXing 3.5.4 dla kodów QR
- `androidx.tvprovider:tvprovider:1.1.0` dla kanałów Android TV
- Paparazzi 2.0.0-alpha05 dla testów zrzutów ekranu
- `dev.chrisbanes.haze:haze-android` 1.7.2 dla matowego szkła panelu bocznego
- `minSdk = 23`, `targetSdk = 36`

## Budowanie

```bash
export ANDROID_HOME=/path/to/android-sdk
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

Testy instrumentacyjne (wymagają emulatora lub urządzenia Android TV):

```bash
./gradlew :app:connectedDebugAndroidTest
```

APK powstaje w `app/build/outputs/apk/debug/app-debug.apk`.

## Struktura projektu

```
app/src/main/java/com/polishmediahub/app/
├── data/                 # Repozytoria, źródła danych, persistencja
│   ├── local/            # Encje/DAO Room (MediaDatabase, ProfileEntity itd.)
│   ├── remote/           # TMDB, Trakt, AniList, Jellyfin, Plex, Emby, Subsonic, Stremio, IPTV, Deezer, podcasty, radio, MDBList
│   ├── source/           # Kodi, Web, Cloudstream, FederatedMediaRepository, SourceRegistry, GlobalExceptionHandler
│   ├── plugin/           # QuickJsEngine, PluginManifest, PluginRepository, DynamicPluginLoader, ReflectiveMediaSource
│   ├── audio/            # AudioSource, DeezerAudioSource, PodcastSource, RadioRepository, AudioHistoryRepository
│   ├── torrent/          # TorrentEngine, TorrentHttpServer, TorrentMediaSource
│   ├── iptv/             # Parser/repozytorium EPG
│   ├── tv/               # TvLauncherManager, WatchNextHelper, RecommendationsWorker
│   ├── legal/            # LegalSourcesRepository / legal_sources.json
│   └── ProfileRepository.kt
├── di/                   # Moduły Hilt
├── model/                # MediaItem, Category, AudioTrack i współdzielone klasy danych
├── navigation/           # Klasa Screen, TVNavHost, TVApp
├── search/               # Dostawca wyszukiwania / SearchActivity
├── ui/
│   ├── components/       # TVCard, FocusableSurface, ModernSidebarBlurPanel, Sidebar itd.
│   ├── screens/          # Home, Search, Detail, Player, Settings, Library, Watchlist, Admin, EPG, Torrents, Music, Downloads, Custom Lists, CustomListDetail, EssentialSetup, CrashReportActivity
│   ├── theme/            # AppColor, AppTypography, Spacing, Radius, TVHubTheme
│   └── viewmodel/        # ViewModele Hilt
├── MainActivity.kt
├── CrashReportActivity.kt
├── TVHubApplication.kt
└── ...
```

## Konfiguracja

### Pierwsze uruchomienie

Przy pierwszym starcie profilu aplikacja pokazuje ekran **Essential Setup**. Wybierz pakiety startowe:

- **Darmowa Telewizja Internetowa** — publiczne playlisty M3U + XMLTV EPG (przykładowe adresy URL zweryfikowane i zaktualizowane 15.07.2026).
- **Muzyka i Podcasty** — kanały RSS podcastów i proxy Deezer.
- **Katalogi Publiczne Web** — oficjalne dodatki Stremio i skonfigurowane crawlery web.

Wybrane pakiety są ładowane na `Dispatchers.IO`, zapisywane w DataStore/Room, a następnie aplikacja przechodzi do ekranu głównego.

### Panel administracyjny

Większość źródeł konfiguruje się przez ekran **Admin** lub bezprzewodowy panel web:

1. Otwórz **Admin** w panelu bocznym.
2. Zeskanuj kod QR telefonem lub otwórz `http://<IP_TV>:<port>/admin` w przeglądarce.
3. Wklej URL / konfigurację JSON dla wybranego źródła i zapisz.

Wspierane źródła:

- **IPTV**: playlisty M3U i URL-e XMLTV EPG.
- **Kodi**: endpoint JSON-RPC z opcjonalnym automatycznym wykrywaniem w LAN.
- **Cloudstream / Aniyomi**: repozytoria indeksów lub pliki binarne wtyczek ładowane dynamicznie.
- **Web**: konfiguracja JSON z selektorami tytułu, opisu, plakatu, linku do strumienia.
- **Wtyczki QuickJS**: URL manifestu lub tekst wtyczki `.js`.
- **Jellyfin / Plex / Emby / Subsonic**: URL serwera + token/dane logowania.
- **Stremio**: URL-e dodatków.
- **TMDB / AniList / Trakt**: klucze API / tokeny.
- **Podcasty**: URL-e kanałów RSS.
- **Deezer**: URL proxy.
- **Debrid**: link OAuth.

Przykładowe legalne źródła startowe można załadować z panelu Admin przez przycisk **Load legal sample sources** lub podczas onboarding-u.

## Profile użytkowników

Aplikacja obsługuje wiele profili domowników przechowywanych w Room:

- Każdy profil ma `id`, `name`, `avatarUrl`, `isPinLocked` i `pinCode`.
- `WatchedEntity`, `SavedMediaEntity`, `CustomListEntity` i `AudioHistoryEntity` posiadają klucz obcy `profileId`.
- `ProfileRepository` udostępnia `currentProfile` jako `StateFlow` i zapisuje wybrany profil w DataStore.
- Repozytoria historii, biblioteki, list obserwowanych, list własnych i audio automatycznie filtrują dane po aktywnym profilu przez `flatMapLatest`.
- Górna część `Sidebar` pokazuje aktywny profil (avatar lub inicjał), otwiera picker profili i pyta o PIN przy przełączaniu na zablokowany profil.
- `TvLauncherManager` czyści wiersz Android TV **Watch Next** przy przełączaniu profilu i publikuje ponownie historię nowego profilu.

## Siatka EPG Timeline Grid

Ekran przewodnika TV (`EpgScreen` / `EpgViewModel`) renderuje profesjonalną dwukierunkowo przewijaną oś czasu:

- Kanały przewijają się pionowo; bloki czasowe poziomo.
- Lewa kolumna kanałów jest zamrożona i pokazuje numer, nazwę i logo.
- Górna belka pokazuje czas w blokach 30-minutowych.
- Kafelki programów mają szerokość proporcjonalną do czasu trwania.
- Focus D-Pad płynnie przewija oś czasu i pokazuje panel szczegółów z tytułem, rokiem, gatunkiem, opisem i postępem.
- Czerwony pionowy marker wskazuje aktualny czas, a widok automatycznie pozycjonuje się na "teraz" przy wejściu.
- `LazyColumn` i `LazyRow` zapewniają płynność przy dużych zbiorach EPG.

## Strumieniowanie BitTorrent

`TorrentEngine` używa `jlibtorrent`:

- Włączone pobieranie sekwencyjne dla wybranego pliku wideo.
- Priorytetowe kawałki z deadline'ami dla szybkiego startu ExoPlayera.
- Lokalny serwer proxy `TorrentHttpServer` z obsługą `Range: bytes=`.
- `TorrentInputStream` blokuje do momentu dostępności żądanych kawałków.
- `TorrentMediaSource.resolve()` zwraca URL `http://127.0.0.1:<port>/stream?infoHash=...&file=...` dla ExoPlayera.
- Postęp buforowania (`TorrentStatus`, procent) dostępny przez `StateFlow`.

## Wtyczki QuickJS

Wtyczki to pliki `.js` ewaluowane w dzielonym kontekście `QuickJsEngine`:

- Globalna funkcja `httpFetch(url, headersJson)` zwraca JSON: `{code, body, headers, error}`.
- `evaluate()` działa na `Dispatchers.IO`, aby uniknąć ANR.
- Singleton z cyklem życia Hilt i jawnym `dispose()` dla czyszczenia natywnego kontekstu.
- `QuickJsMediaSource.mapToMediaItem` przekazuje opcjonalne nagłówki HTTP (`User-Agent`, `Referer`) do fabryki `DefaultHttpDataSource.Factory` w ExoPlayerze.

## Dynamiczne ładowanie wtyczek binarnych

Wtyczki Cloudstream (`.cs3` / `.cs4`) i Aniyomi (`.apk`) mogą być ładowane bez systemowego instalatora:

- `PluginRepository` pobiera plik binarny do `context.cacheDir/plugins/`.
- `DynamicPluginLoader` tworzy katalog zoptymalizowanego DEX pod `codeCacheDir/plugins_dex` i tworzy `DexClassLoader` z `context.classLoader` jako rodzicem.
- Załadowane klasy są adaptowane przez `ReflectiveMediaSource` do kontraktu `MediaSource` aplikacji.
- Wyniki wtyczek zasilają `PluginMediaSource` i pojawiają się w Wyszukiwarce, na Home i w dashboardzie.
- Przy wyłączaniu / usuwaniu wtyczki czyszczone są pliki DEX.

## Polskie audio / napisy

- `DefaultTrackSelector` preferuje ścieżki audio i tekstowe `pl`.
- Jeśli występują wielokrotne polskie ścieżki audio (Lektor, Dubbing, Audiodeskrypcja), rola AD jest depriorytyzowana.
- Pełne etykiety językowe są wyciągane z Media3 i pokazywane w `PlayerControls`, więc użytkownik może precyzyjnie przełączać ścieżki D-Padem.
- Rozmiar, kolor i przesunięcie pionowe napisów można zmieniać w Ustawieniach i są stosowane na żywo.

## Integracja z launchery Android TV

- `TvLauncherManager` zapisuje do `TvContractCompat.WatchNextPrograms` i dedykowanego kanału `PreviewChannel`.
- `RecommendationsWorker` uruchamia się raz dziennie (lub przy starcie) i odświeża polecane treści na ekranie głównym.
- Kliknięcie kafelka launchera otwiera `MainActivity` przez deep linki (`polishmediahub://play/{id}` lub `polishmediahub://detail/{id}`) i wznawia odtwarzanie z zapisanego miejsca.

## Raportowanie awarii

Nieobsługiwane wyjątki są łapane przez `GlobalExceptionHandler`:

- Ślad stosu zapisywany jest do `filesDir/last_crash.txt`.
- Uruchamiana jest `CrashReportActivity` w osobnym procesie `:crashreport`, dzięki czemu zawieszony proces można ubić bez utraty raportu.
- Ekran awarii oferuje trzy opcje:
  - **Uruchom ponownie aplikację** — czyści log awarii i uruchamia `MainActivity`.
  - **Wyczyść pamięć podręczną i zrestartuj** — usuwa zawartość `cacheDir` oraz katalog `codeCacheDir/plugins_dex`, a następnie restartuje aplikację.
  - **Wyślij raport do chmurze** — odczytuje lokalny log `last_crash.txt`, uruchamia offline'owy `CrashReportSanitizer` (redaguje klucze API, tokeny OAuth i dane premium), a następnie przesyła zanonimizowany ślad stosu na endpoint `POST /report-error` Cloudflare Workera z nagłówkiem `X-Hub-Token` pobranym z `ApiConfigRepository`. Do wysyłki używany jest lekki, jednorazowy `OkHttpClient`; ciało odpowiedzi oraz dispatcher i pula połączeń są jawnie zamykane/czyszczone w bloku `finally`.
- `GlobalExceptionHandler` przekazuje adres Workera i token autoryzacyjny do `:crashreport` przez extra intentu, więc proces raportowania nie musi odczytywać DataStore.
- Handler nie jest instalowany wewnątrz procesu `:crashreport`, co zapobiega zapętleniu awarii.

## Jakość i lint

Projekt utrzymuje zerową liczbę ostrzeżeń lintera:

- `./gradlew :app:lintDebug` zwraca `No issues found.`
- `app/lint.xml` wycisza wyłącznie pre-existing, informacyjne ostrzeżenia o wersjach zależności i zasobach ikon.
- Wszystkie ostrzeżenia kodu wprowadzone przez audyt zostały naprawione (`ModifierParameter`, `PrivateResource`, `UseKtx`, `PluralsCandidate`, `FrequentlyChangingValue`, `SetJavaScriptEnabled`, `RedundantLabel`, `IntentFilterUniqueDataAttributes`, `DefaultUncaughtExceptionDelegation` i inne).
- Zdeprecjonowane wywołania `hazeChild` zostały zamienione na `hazeEffect`, aby build pozostał bez ostrzeżeń.
- Puste bloki `catch` w krytycznych ścieżkach zostały zastąpione logowaniem przez `Log.w(...)`.

## Testowanie

- Testy jednostkowe: `./gradlew :app:testDebugUnitTest`
- Testy zrzutów ekranu / record: `./gradlew :app:recordPaparazziDebug`
- Lint: `./gradlew :app:lintDebug`
- Testy instrumentacyjne UI / D-Pad: `./gradlew :app:connectedDebugAndroidTest`

CI (GitHub Actions) buduje, sprawdza lint i uruchamia testy jednostkowe przy każdym push-u; zadanie emulatorowe jest gotowe do przyszłych testów instrumentacyjnych.

## Prawidłowe / odpowiedzialne użytkowanie

- Projekt jest przeznaczony wyłącznie do **osobistego, legalnego użytku multimediów**.
- Nie dodawaj pirackich strumieni, nieautoryzowanych playlist IPTV, torrentów z materiałami chronionymi prawem, do których nie masz praw, ani scraperów łamiących regulamin strony.
- Maintainerzy nie dostarczają ani nie rekomendują żadnych konkretnych źródeł treści.
- Zobacz [LEGAL_SOURCES.md](LEGAL_SOURCES.md) / [LEGAL_SOURCES.pl.md](LEGAL_SOURCES.pl.md) dla listy legalnych / publicznych / self-hostowalnych punktów wyjścia.

## Licencja

MIT License — zobacz [LICENSE](LICENSE).
