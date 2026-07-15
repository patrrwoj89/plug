# Changelog

Wszystkie istotne zmiany w Polish Media Hub są dokumentowane w tym pliku.

## [Unreleased]

### Dodano

#### UI / UX
- **Płynne animacje przejść współdzielonych elementów** dla plakatów filmów (`TVNavHost`, `TVCard`, `DetailScreen`)
  - `NavHost` jest owinięty w `SharedTransitionLayout`, który dostarcza `SharedTransitionScope` dla każdego ekranu.
  - `MediaCard` i główny plakat na ekranie szczegółów używają `Modifier.sharedElement` z `rememberSharedContentState(key = "poster_${item.id}")` oraz `AnimatedVisibilityScope` z docelowych `composable` — plakat płynnie animuje się z siatki/rzędu do ekranu szczegółów i z powrotem podczas nawigacji D-Padem.
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

#### Wyszukiwanie i TV na żywo
- **Wyszukiwanie głosowe** (`SearchScreen`, `SearchViewModel`)
  - D-Pad przycisk `TvIconButton` z ikoną mikrofonu obok pola wyszukiwania.
  - Uruchamia `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` z `LANGUAGE_MODEL_FREE_FORM` i językiem polskim (`pl-PL`).
  - Rozpoznany tekst jest wstawiany do pola wyszukiwania i wywoływane jest `SearchViewModel.submitSearch(query)`.
- **Tło aktualizowanie cache EPG/IPTV** (`IptvUpdateWorker`)
  - `CoroutineWorker` uruchamiany co 12 godzin i przy zimnym starcie przez `WorkManager`.
  - Pobiera skonfigurowane playlisty M3U oraz pliki XMLTV EPG, parsuje je i zapisuje kanały w nowej tabeli `ChannelEntity` oraz programy w istniejącej tabeli `EpgEntity` w Room.
  - `Constraints`: `NetworkType.UNMETERED`, `requiresDeviceIdle`, `requiresBatteryNotLow`.
  - `EpgViewModel` i `EpgTimelineGrid` ładują kanały i programy wyłącznie z lokalnego cache Room; ekran telewizji na żywo otwiera się natychmiast bez pobierania dużych plików XML.
  - Data i status ostatniej synchronizacji są eksponowane przez `ApiConfigRepository` / DataStore i wyświetlane w `AdminHttpServer` oraz `SettingsScreen`.

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

#### Źródła
- **Integracja MDBList** (`MdbListMediaSource`, modele `MdbListApi`)
  - Nowe federowane źródło `MediaSource` podpięte do `SourceRegistry`.
  - Ładuje publiczne listy top (`/lists/top`), listy użytkownika (`/lists/user`) i elementy list (`/lists/{id}/items`).
  - Wyszukiwanie mediów przez `/search/any` oraz lookup po identyfikatorach przez `/{provider}/{type}/{id}` (imdb/tmdb/trakt/tvdb).
  - Każdy `MediaItem` zawiera `tmdbId`, `imdbId` i `traktId` dla dopasowania do Stremio, Plex, Jellyfin, Trakt itp.
  - Używa globalnego `OkHttpClient` i modeli `@Serializable` z Kotlinx Serialization.
- **Pakiet startowy MDBList** w `legal_sources.json` i `LegalSourcesRepository` (`MdbListStarter`, `MdbListStarterEntry`).
- **Klucz API MDBList** w `ApiConfigRepository`, `SettingsScreen` (pole maskowane) oraz panelu QR `AdminHttpServer`.
- **Reaktywny fallback Kitsu dla anime** (`KitsuMediaSource`, `AnimeRepository`)
  - Nowe źródło `MediaSource` korzystające z `https://kitsu.io/api/edge` i modeli `@Serializable` z Kotlinx Serialization; globalny `OkHttpClient` wstrzykiwany przez Hilt.
  - `AnimeRepository` opakowuje `AniListMediaRepository` i `KitsuMediaSource`; gdy AniList zwróci pusty wynik (błąd sieci, timeout, 429), aplikacja automatycznie przełącza się na Kitsu.
  - Mapowanie anime Kitsu na `MediaItem` z polami `malId` i `aniListId` sparsowanymi z relacji `include=mappings`.
  - Dodano wpis `kitsu` do `legal_sources.json`.
- **Fallback metadanych Filmweb.pl** (`FilmwebMediaSource`, `FederatedMediaRepository`, `FilmwebCacheRepository`, Room v14)
  - Nowe `FilmwebMediaSource` implementujące `MediaSource`, korzystające z globalnego `OkHttpClient` z `MemoryCookieJar` i `CloudflareBypassInterceptor`.
  - Wyszukiwanie przez `www.filmweb.pl/api/v1/films/search` oraz `/live/search`, a następnie pobieranie tytułu, polskiego opisu, plakatu, oceny i liczby głosów z endpointów `/film/{id}/info`, `/description`, `/preview` i `/rating`.
  - `FederatedMediaRepository.enrichWithFilmweb(item)` jest wywoływane z `DetailViewModel` na `Dispatchers.IO`, gdy opis z TMDB jest pusty, zbyt krótki lub nie zawiera polskich znaków diakrytycznych.
  - Wzbogacone metadane (opis, plakat, `filmwebRating`, `filmwebVoteCount`, `filmwebUrl`) są zapisywane w nowej tabeli `filmweb_cache` w Room za pośrednictwem `FilmwebCacheRepository`, co przyspiesza kolejne otwarcia karty szczegółów.
  - `DetailScreen` wyświetla etykietę `Filmweb: X.X` obok roku/czasu trwania/oceny, gdy dane Filmweb są dostępne.

#### Bezpieczeństwo
- **Szyfrowanie wrażliwych ustawień** (`EncryptedSettingsManager`, `ApiConfigRepository`)
  - Szyfrowanie AES-256-GCM przy użyciu sprzętowo chronionego klucza generowanego w Android Keystore (`AndroidKeyStore`).
  - Dla każdego szyfrowania generowany jest losowy 12-bajtowy IV, dołączany do szyfrogramu i kodowany Base64.
  - Wrażliwe wartości (TMDB, AniList, Trakt, Debrid, tokeny Jellyfin/Plex/Emby, hasło Subsonic, klucz MDBList) są szyfrowane przed zapisem do DataStore i odszyfrowywane przy odczycie. Zwykłe preferencje (motyw, jakość, status synchronizacji EPG itp.) pozostaję w jawnej postaci.
- **System Kontroli Rodzicielskiej**
  - Encja `ProfileEntity` rozszerzona o pola `maxAgeRating` (np. G, PG, PG-13, R, NC-17, 7/12/16/18) i `allowNsfw` typu Boolean.
  - Migracja bazy Room do v12 (`migrationFromV11toV12`) bezpiecznie dodaje kolumny `maxAgeRating` i `allowNsfw` do tabeli `profiles` z domyślnymi wartościami.
  - Nowa klasa pomocnicza `ContentFilter` filtruje listy `MediaItem` i kategorie według aktywnego profilu; zastosowana w `CompositeMediaRepository`, `FederatedMediaRepository` oraz `PluginMediaSource` w metodach `search()`, `categories()` i `featured()`.
  - Sekcja **Kontrola Rodzicielska** w `SettingsScreen` (chroniona PIN-em) umożliwia ustawienie maksymalnej kategorii wiekowej i zezwolenia na NSFW dla każdego profilu domownika.

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

#### Dwukierunkowa synchronizacja z Trakt.tv (`TraktSyncWorker`, `TraktMediaRepository`)
- `TraktSyncWorker` to `@HiltWorker` uruchamiany co 6 godzin przez `WorkManager` z ograniczeniami `NetworkType.CONNECTED` oraz `requiresBatteryNotLow`.
- Pobiera obejrzane filmy/seriale/odcinki z `/sync/watched` oraz listę do obejrzenia z `/sync/watchlist` i zapisuje je w per-profilowych tabelach Room.
- Wysyła lokalną historię (`WatchHistoryRepository`) i listę do obejrzenia (`SavedMediaRepository`) z powrotem do Trakt przez `/sync/history` i `/sync/watchlist`; konflikty są rozwiązywane na podstawie timestampu — nowszy rekord wygrywa.
- `PlayerViewModel` wysyła żądania `/scrobble/start`, `/scrobble/pause` i `/scrobble/stop` z dokładnym procentem postępu przy zmianach stanu ExoPlayera.
- Timestamp/status/błąd ostatniej synchronizacji są eksponowane przez `ApiConfigRepository` i wyświetlane w `SettingsScreen` oraz `AdminHttpServer`.

#### Inteligentny Tryb Kinowy (`PlayerScreen`, `PlayerViewModel`)
- Przełącznik `cinemaMode` w `SettingsRepository` / `SettingsScreen` oraz bezprzewodowym panelu admina.
- Gdy `isPlaying == true`, odtwarzacz automatycznie chowa nakładki po krótkim czasie bez aktywności i przyciemnia cały ekran animowanym czarnym overlayem.
- Po pauzie lub interakcji pilotem nakładka płynnie rozjaśnia się, a pod suwakiem pojawia się karta informacyjna z tytułem, opisem, gatunkami i top 5 aktorami pobranymi w tle z metadanych TMDB / Trakt.
- `TmdbMediaRepository.credits(...)` odpytuje endpointy `/movie/{id}/credits` oraz `/tv/{id}/credits`.

#### Logowanie Trakt.tv kodem urządzenia (`TraktAuthManager`, `TraktPairingViewModel`, `TraktPairingSection`)
- Zastępuje ręczne wklejanie tokenów oficjalnym przepływem device-code (`/oauth/device/code` i `/oauth/device/token`).
- Użytkownik wpisuje raz `client ID` i `client secret` Trakt; aplikacja żąda kodu urządzenia, wyświetla `user_code`, odlicza czas ważności i pokazuje kod QR z adresem aktywacji.
- Polling odbywa się na `Dispatchers.IO` co `interval` sekund do momentu autoryzacji urządzenia.
- Uzyskane `access_token` i `refresh_token` są szyfrowane AES-256-GCM w Android Keystore i zapisywane w `ApiConfigRepository`.
- Kreator parowania zintegrowany w `SettingsScreen` oraz `AdminScreen`.

#### Natywny tryb Picture-in-Picture (`MainActivity`, `PlayerScreen`, `PlayerViewModel`)
- `MainActivity` deklaruje `android:supportsPictureInPicture="true"` z wymaganymi `configChanges`.
- `onUserLeaveHint()` wchodzi w tryb PiP, gdy odtwarzacz jest aktualnie w stanie odtwarzania.
- `onPictureInPictureModeChanged()` propaguje stan PiP do `PlayerViewModel`.
- `PlayerScreen` ukrywa kontrolki, napisy, nakładkę Nerd Stats, kartę informacyjną Trybu Kinowego i nakładkę następnego odcinka, gdy `isInPipMode == true`, pozostawiając sam strumień wideo.
- Powrót do pełnego ekranu przywraca pełny interfejs sterowania.

#### Automatyczne odświeżanie tokenów Trakt (`TraktAuthenticator`, `NetworkModule`)
- Nowy `okhttp3.Authenticator` przechwytuje odpowiedzi HTTP 401 z API Trakt.
- Wczytuje zaszyfrowany `refresh_token` z `ApiConfigRepository`, synchronicznie wywołuje `/oauth/token` z `grant_type=refresh_token` i szyfruje nową parę `access_token` / `refresh_token` z powrotem do DataStore.
- Pierwotne żądanie jest ponawiane z nowym tokenem Bearer, dzięki czemu synchronizacja w tle i scrobbling działają bez ponownego parowania.
- `NetworkModule` dostarcza dedykowanego klienta `@Named("trakt")` `OkHttpClient` z `TraktAuthenticator` dla `TraktApi`; globalny `OkHttpClient` zachowuje `DebridAuthenticator`.

#### Inteligentne pomijanie czołówki i końcówki (`PlayerScreen`, `PlayerViewModel`, `SettingsRepository`)
- `MediaItem` rozszerzono o opcjonalne `introStartMs`, `introEndMs`, `outroStartMs`, `outroEndMs`; wtyczki (Stremio/Kodi) mogą podać dokładne segmenty.
- Domyślne wartości zastępcze są konfigurowalne w `SettingsScreen` / panelu admina: koniec czołówki po N sekundach, początek końcówki na N sekund przed końcem.
- Podczas odtwarzania półprzezroczysty przycisk `TvButton` wysuwa się i automatycznie przejmuje fokus, gdy głowica znajdzie się w segmencie czołówki lub końcówki.
- **Pomiń czołówkę** przesuwa odtwarzacz na koniec czołówki.
- **Pomiń napisy końcowe** natychmiast wywołuje istniejącą nakładkę Auto-Play Next, umożliwiając odtworzenie następnego odcinka lub anulowanie.
- Przełącznik i domyślne czasy trwania są zapisywane w `SettingsRepository` i udostępniane przez `SettingsViewModel`.

#### Alternatywny silnik odtwarzacza LibVLC (`UniversalVlcPlayer`)
- Nowa zależność `org.videolan.android:libvlc-all:4.0.0-eap17`.
- Komponent `UniversalVlcPlayer` otacia `VLCVideoLayout`, `LibVLC`, `MediaPlayer` i `Media` bezpośrednio w procesie aplikacji (bez instalowania zewnętrznego odtwarzacza).
- Przełącznik `useAlternativePlayer` w `SettingsRepository` / DataStore, udostępniony w `SettingsScreen` i panelu webowym `AdminHttpServer`.
- `PlayerScreen` warunkowo renderuje `UniversalVlcPlayer` zamiast `PlayerContent`, gdy przełącznik jest włączony.
- Nagłówek HTTP `User-Agent` oraz `mediaItem.headers` są wstrzykiwane do opcji `LibVLC`; dodawany jest domyślny `User-Agent` `PolishMediaHub`, gdy źródło go nie przekazuje.
- Zewnętrzne napisy (`subtitleUrl`) są dołączane przez `Media.addSlave`; `subtitleHeaders` przekazywane są jako per-mediowe opcje `:http-header-fields`, a pierwsza ścieżka tekstowa wybierana jest po zdarzeniu `MediaPlayer.Event.Playing`.
- Pętla pozycji co 500 ms odczytuje `mediaPlayer.time`/`length` i przekazuje do `PlayerViewModel.updatePosition(...)`, dzięki czemu scrobbling Trakt, historia profilu w Room, nakładka Auto-Play Next i przycisk Smart Skip Intro/Outro działają także na silniku LibVLC.
- Rozwiązuje problemy dekodowania ExoPlayera z dźwiękiem DTS/AC3 oraz kontenerami MKV/AVI z torrentów, Kodi i wtyczek webowych.

### Zmieniono
- `TVNavHost` czeka na wartość `isFirstLaunch` przed wyborem startu `NavHost`, co zapobiega resetowaniu grafu nawigacji.
- `TvLauncherManager` zapisuje postęp co 15 sekund podczas odtwarzania, z wymuszonym zapisem przy `onPlaybackStopped`.
- `PlayerScreen` reaktywnie pobiera styl napisów i nakładkę Nerd Stats z dedykowanych `StateFlow`.
- Sygnatura `PlayerControls` rozszerzona o parametry `cinemaMode` i `cinemaInfo` dla karty informacyjnej Trybu Kinowego.
- `AdminHttpServer` obsługuje teraz `/api/trakt/sync` i eksponuje `traktClientSecret`, `traktRefreshToken`, `traktAccessToken`, `lastTraktSyncAt/Status/Error`.
- `ApiConfigRepository` rozszerzona o szyfrowane pola `traktClientSecret` i `traktRefreshToken`.
- Sygnatura `PlayerContent` rozszerzona o `onUpdatePosition`, `onSkipIntro`, `onSkipOutro`, `onSeekHandled`, `skipIntroState`, `pendingSeekToMs` i `forceAutoPlayOverlay`.
- `NextEpisodeOverlay` przyjmuje nullable `nextEpisode` i kończy się natychmiast, gdy wartość jest null.
- `AdminHttpServer` zapisuje i eksponuje `autoSkipIntro`, `introEndSeconds` oraz `outroDurationSeconds` z `SettingsRepository`.
- `MediaItem` rozszerzono o mapę `subtitleHeaders` dla autoryzacyjnych nagłówków napisów.
- `QuickJsMediaSource` mapuje pole `subtitleHeaders` z wtyczek JS na `MediaItem`.
- Widoczność `NextEpisodeOverlay` zmieniono z `private` na `internal`, aby mogła być używana przez `UniversalVlcPlayer`.
- **Audyt zależności (15.07.2026)**
  - `libvlc` zaktualizowano do `4.0.0-eap24` (najnowszy LibVLC Android EAP na Maven Central na 15.07.2026, https://central.sonatype.com/artifact/org.videolan.android/libvlc-all/4.0.0-eap24).
  - `haze` zaktualizowano do `1.7.2` (najnowsza stabilna wersja, https://github.com/chrisbanes/haze/releases/tag/1.7.2).
  - `zxing` zaktualizowano do `3.5.4` (najnowsza stabilna wersja, https://github.com/zxing/zxing/releases).
  - `retrofit` zaktualizowano do `2.12.0` (najnowsza stabilna wersja, https://github.com/square/retrofit/releases/tag/2.12.0).
  - `kotlinx-coroutines-test` zaktualizowano do `1.10.2` (najnowsza stabilna wersja, https://github.com/Kotlin/kotlinx.coroutines/releases/tag/1.10.2).
  - `appcompat` zaktualizowano do `1.7.1` (najnowsza stabilna wersja, https://developer.android.com/jetpack/androidx/releases/appcompat#1.7.1).
  - `navigation-compose` zaktualizowano do `2.9.8` (najnowsza stabilna wersja, https://developer.android.com/jetpack/androidx/releases/navigation#2.9.8).

### Naprawiono

- **Dopracowanie wizualne i struktura menu bocznego**
  - `MediaCard` i `WideCard` (`TVCard`) przycinają teraz `AsyncImage` za pomocą `RoundedCornerShape(Radius.md)`, eliminując ostre rogi plakatów.
  - Awatary profili w `Sidebar` i `CollapsedSidebarPill` używają `ContentScale.Crop` i `fillMaxSize()` w okrągłych kontenerach, zapobiegając zniekształceniom i pustym przestrzeniom.
  - Menu boczne zostało pogrupowane w sekcje: Odkrywaj, Biblioteka, Multimedia, Pobrane i System.
  - Zablokowane profile wyświetlają `Icons.Default.Lock` z lokalizowanym `contentDescription` zamiast emotki `"🔒"`.
  - Zdeprecjonowane wywołania `hazeChild` zostały zamienione na `hazeEffect` w `ModernSidebarBlurPanel`.

- **Poprawki jakościowe z audytu Claude Sonnet 5**
  - `LibraryScreen` i `WatchlistScreen`: uszkodzony układ `LazyColumn` + `Modifier.fillParentMaxWidth(0.25f)` zastąpiono 5-kolumnową `LazyVerticalGrid` z odstępami `Spacing.md`.
  - Kafelki list własnych w `CustomListsScreen` przekierowują do nowego ekranu `CustomListDetailScreen` (siatka 5 kolumn + `CustomListDetailViewModel`).
  - Wszystkie 22 puste bloki `catch` w kluczowych plikach (`PlayerViewModel`, `AdminHttpServer`, `TorrentHttpServer`, `MediaDatabase`, `DynamicPluginLoader`, `PluginRepository`, `TvLauncherManager`, `WatchNextHelper`) zastąpiono logowaniem `Log.w(...)`.
  - Usunięto martwe zależności `androidx.tv.material` i `androidx.tv.foundation` z `app/build.gradle.kts` i `libs.versions.toml`; poprawiono README.
  - Do poziomych `LazyRow` w `CategoryRow` dodano `Modifier.focusGroup()` i `focusRestorer()`, więc fokus D-Pada wraca do ostatnio oglądanego kafelka przy przejściu między rzędami.
  - Zlokalizowano teksty w `AdminScreen`, `EpgScreen` i `DownloadsScreen`; dodano brakujące `contentDescription` dla strzałek reorder wtyczek i stanu zaznaczenia w `Sidebar`.
  - Naprawiono ostrzeżenia lint: `IntentFilterUniqueDataAttributes`, `DefaultUncaughtExceptionDelegation`, `UseKtx`, `ModifierParameter`, `PluralsCandidate`, `FrequentlyChangingValue`, `SetJavaScriptEnabled`, `RedundantLabel` i inne.
  - Dodano bazę `app/lint.xml` dla pre-existing ostrzeżeń o wersjach zależności i zasobów ikon; `lintDebug` zwraca teraz `No issues found.`
- **Audyt Trakt.tv — paginacja `sync/watched` i `sync/watchlist`**
  - Dodano parametry `page` i `limit` (max 250) do `/sync/watched/movies`, `/sync/watched/shows` oraz `/sync/watchlist/movies,shows`.
  - `TraktMediaRepository` odczytuje nagłówki `X-Pagination-Page-Count` i przechodzi przez wszystkie strony, dzięki czemu dwukierunkowa synchronizacja działa po wymuszeniu paginacji 30 czerwca 2026 r.
  - Zaktualizowano odniesienia do aktualnej dokumentacji Trakt pod `https://docs.trakt.tv` (stara dokumentacja Apiary jest zdeprecjonowana od czerwca 2026).
- `WebMediaSource.byId()` pobiera teraz pojedynczą stronę bezparsując całego katalogu.
- `CloudstreamSource.categories()` ładuje zdalne indeksy zamiast zwracać pustą listę.
- `QuickJsEngine` nie używa już `runBlocking` w `httpFetch`.
- `PlayerViewModel` posiada zaktualizowane override'y `AnalyticsListener` zgodnie z API ExoPlayera.
- `AnimeRepository.categories()` teraz fallbackuje do Kitsu, gdy AniList zwraca puste kategorie.
- Logika `ContentFilter` dla wieku/NSFW została uproszczona: `allowNsfw` blokuje pozycje oznaczone jako treści dla dorosłych, a `maxAgeRating` blokuje pozycje o rozpoznanym poziomie wiekowym wyższym od limitu.
- **Audyt adresów źródeł startowych (15.07.2026)**
  - Usunięto martwe próbki IPTV/EPG `LegalStream` z `legal_sources.json` (repozytorium zdeprecjonowane, adresy zwracają 404).
  - Zamieniono przykłady Stremio na działające oficjalne dodatki: `Cinemeta` (`https://v3-cinemeta.strem.io/manifest.json`) i `WatchHub` (`https://watchhub-us.strem.io/manifest.json`).
  - Zamieniono martwy feed NASA `NASAcast_Podcast.rss` na aktualny podcast NASA `https://www.nasa.gov/feeds/podcasts/houston-we-have-a-podcast`.
  - Dodano działającą przykładową EPG Pluto TV `https://i.mjh.nz/PlutoTV/us.xml` dla onboarding IPTV/EPG.
  - Zweryfikowano, że wszystkie pozostałe przykładowe adresy zwracają HTTP 200 na dzień 15.07.2026; dostępność transmisji na żywo nadal zależy od nadawcy i powinna być weryfikowana lokalnie.

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
