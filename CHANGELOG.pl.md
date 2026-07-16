# Changelog

Wszystkie istotne zmiany w Polish Media Hub są dokumentowane w tym pliku.

## [Unreleased]

### Dodano

#### Płynne centrowanie focusu, pełne rozmycie spoilerów i szybkie ustawienia playera
- **Centrowanie focusu D-Pada na Android TV** (`TvBringIntoViewSpec`)
  - Dodano `TvBringIntoViewSpec` implementujący `BringIntoViewSpec` z wycentrowanym układem i płynną animacją `SpringSpec`.
  - `LazyRow` w `CategoryRow` oraz `LazyVerticalGrid`/`LazyColumn` w `LibraryScreen`, `WatchlistScreen`, `CustomListDetailScreen` i `CustomListsScreen` zostały owinięte w `TvBringIntoViewProvider`, dzięki czemu focus D-Pada pozostaje na środku ekranu, zamiast uderzać w krawędzie.
- **Pełne rozmycie spoilerów dla nieobejrzanych odcinków** (`DetailScreen`, `DetailViewModel`)
  - Gdy `spoilerBlurEnabled` jest aktywne, a odcinek nie znajduje się w `WatchHistoryRepository` dla bieżącego `profileId`, plakat (`AsyncImage` z `Modifier.blur(16.dp)`) i tytuł są ukrywane.
  - Oryginalny tytuł zastępowany jest `stringResource(R.string.episode_title, ...)` (lub `spoiler_hidden_title`, gdy brak numeru odcinka).
  - Środkowy przycisk D-Pada na kafelku plakatu/tytułu odsłania tytuł, plakat i opis na czas bieżącego seansu.
- **Podręczne menu szybkich ustawień playera** (`PlayerQuickSettingsOverlay`, `PlayerControls`, `PlayerScreen`, `UniversalVlcPlayer`, `PlayerViewModel`, `PlayerViewModelTest`)
  - Dodano przycisk z ikoną zębatki (`Icons.Default.Settings`) w `PlayerControls`.
  - Otwiera dolny `PlayerQuickSettingsOverlay` z przełącznikami trybu nocnego (`LoudnessEnhancer`), preferencji audio (`Lektor` / `Dubbing`) oraz silnika (`ExoPlayer` / `LibVLC`).
  - Stan nakładki subskrybowany jest przez `collectAsStateWithLifecycle()`, a zamknięcie zwraca focus na suwak postępu za pomocą `FocusRequester`.
  - `PlayerViewModel` udostępnia `toggleNightModeEnabled()`, `cyclePreferredAudioType()` i `toggleUseAlternativePlayer()`, które zapisują dane w `SettingsRepository`.
  - Niezabezpieczone wywołania `Log` w `PlayerViewModel` i `UniversalVlcPlayer` zostały opakowane przez `if (BuildConfig.DEBUG)`, więc parametry sesji audio i URL-e nie trafią do Logcat w buildach release.
  - Rozszerzono `PlayerViewModelTest` o testy MockK weryfikujące trzy nowe przełączniki szybkich ustawień.

#### Tuning silnika ExoPlayer, reguły streamów, tryb AMOLED i Binge Grouping
- **Tuning natywnego silnika ExoPlayer** (`ExoPlayerTuningConfig`, `PlayerScreen`, `SettingsScreen`, `NetworkModule`)
  - `ExoPlayerTuningConfig` grupuje odtwarzanie tunelowane, połączenia równoległe, min/max/back buffer, początkową liczbę alokacji i target buffer size.
  - `PlayerScreen` buduje `ExoPlayer` z dostrojonym `DefaultLoadControl` (pre-alokowany `DefaultAllocator`) i `DefaultTrackSelector` z `setTunnelingEnabled`.
  - `NetworkModule` konfiguruje `OkHttpClient.dispatcher.maxRequests` i `maxRequestsPerHost` zgodnie z ustawieniami użytkownika.
  - Nowe ustawienia w `SettingsScreen` udostępniają wszystkie parametry tuningowe za pomocą suwaków i przełączników sterowanych pilotem.
- **Reguły Debrid / TorBox Stream Rules** (`StreamRulesEngine`, `StreamRules`, `CompositeMediaRepository`, `SettingsScreen`)
  - `StreamRulesEngine` analizuje tytuły/podtytuły/opisy źródeł i filtruje po zakresie rozmiaru, rozdzielczości, wymaganych/preferowanych/wykluczonych tagach wideo/audio i enkoderach.
  - Model `@Serializable` `StreamRules` przechowywany jako JSON w DataStore; `CompositeMediaRepository` aplikuje reguły do wyników wyszukiwania i szczegółów.
  - Panel admina `/api/config` przyjmuje i zwraca JSON `streamRules`; `SettingsScreen` udostępnia pole tekstowe do edycji JSON.
- **Tryb AMOLED / Pure Black** (`TVHubTheme`, `MainActivity`, `SettingsScreen`)
  - `SettingsScreen` dodaje przełączniki `AMOLED Mode` i `Pure Black Surfaces`.
  - `MainActivity` zbiera flow ustawień i przekazuje je do `TVHubTheme`, który ustawia tło, surface i surface variant na `#000000` gdy włączone.
  - Dostępne są composition local `LocalAmoledMode` i `LocalPureBlackSurfaces` do dalszej stylizacji kart/plakatów.
- **Binge Grouping** (`PlayerViewModel`, `StreamRulesEngine`)
  - Gdy użytkownik ręcznie wybierze profil źródła podczas oglądania serialu, w pamięci RAM tworzony jest `BingeProfile` na podstawie rozwiązanych metadanych strumienia.
  - `findNextEpisode` automatycznie aplikuje ten sam profil/okno do następnego odcinka, wykorzystując ranking `StreamRulesEngine`.
  - Przełącznik włączenia/wyłączenia w `SettingsScreen`.
- **Testy i hardening**
  - Dodano `StreamRulesEngineTest` z użyciem MockK, weryfikujący wyłączone reguły, filtrowanie po rozmiarze, rozdzielczości, wymaganych/wykluczonych tagach, rankingu preferencji i `maxResults`.
  - Żadne klucze API, hashe, parametry filtrów ani linki źródeł nie są logowane do Logcat w buildach release (`BuildConfig.DEBUG == false`).

#### Natywne Fuzzy Search z odległością Levenshteina
- **LevenshteinEngine** (`app/data/source/LevenshteinEngine.kt`)
  - Pamięciowo zoptymalizowany algorytm z dwiema naprzemiennymi tablicami `IntArray`, progiem `maxDistanceThreshold = 2` i normalizacją `lowercase` + `trim`.
  - Udostępnia helpery `calculateDistance`, `score`, `isFuzzyMatch` i `sort`.
- **Reaktywne wyszukiwanie z debounce** (`SearchViewModel`, `EpgDao`, `ChannelDao`, `IptvRepository`)
  - `SearchViewModel` używa teraz `MutableStateFlow<String>` z `debounce(300L)` i `distinctUntilChanged()`.
  - Końcowe sortowanie trafień odbywa się na `Dispatchers.Default`, chroniąc wątek główny przed przycięciami klatek.
  - `EpgDao` i `ChannelDao` wykorzystują pre-filtry `LIKE %fraza%`; `IptvRepository` sortuje połączone wyniki lokalne + EPG + zdalne przez `LevenshteinEngine.sort`.
- **Testy**
  - `ExampleUnitTest` został usunięty; dodano `LevenshteinEngineTest` weryfikujący odległości dla polskich literówek (`Widźmin` vs `Wiedźmin`, `Filman` vs `Flman`) i kolejność sortowania.

#### Hartowanie produkcyjne i ostateczne zamrożenie kodu
- **Hartowanie silnika Levenshteina** (`LevenshteinEngine`)
  - Dodano normalizację Unicode NFD i usuwanie znaków diakrytycznych, dzięki czemu polskie znaki (`ą`, `ć`, `ę`, `ł` itp.) nie powodują `ArrayIndexOutOfBoundsException`.
  - Poprawiono alokację dwóch tablic `IntArray` na `IntArray(s2.length + 1)`.
- **Hartowanie webhooków Home Assistant** (`HomeAssistantWebhookClient`)
  - Zastąpiono wstrzykiwany globalny `OkHttpClient` dedykowanym klientem z timeoutami connect/write/read ustawionymi na 3 sekundy.
  - Owinięto `.execute()` w `try-catch-finally` i w sekcji `finally` jawne zamknięcie `response.body`, eliminując zawieszone wątki I/O i wycieki zasobów.
  - Adres URL i token webhooka pozostają zamaskowane / nielogowane w buildach release.
- **Parytet Binge Grouping dla LibVLC** (`UniversalVlcPlayer`, `PlayerViewModel`)
  - `PlayerViewModel` aplikuje wyekstrahowany `BingeProfile` do `_preferredAudioType` i `_preferredQuality`, więc zarówno ExoPlayer, jak i LibVLC automatycznie wybierają zapisany profil (`lector`/`dubbing`) i rozdzielczość dla następnego odcinka.
  - `UniversalVlcPlayer` używa `rememberUpdatedState(preferredAudioType)` i `LaunchedEffect`, aby przeładować ścieżkę audio przy zmianie preferencji Binge / Quick-Settings, na równi z reaktywnym wyborem audio ExoPlayera.
- **Audyt logów i hartowanie release**
  - Usunięto tymczasowe `println`, znaczniki `TODO` i osierocone bloki komentarzy.
  - Wszystkie pozostałe wywołania `android.util.Log` / `Log` zostały opakowane przez `if (BuildConfig.DEBUG)`, więc w buildach release do Logcat nie trafiają klucze API, URL-e, tokeny, parametry filtrów ani zapytania użytkownika.
  - Potwierdzono, że wszystkie subskrypcje Flow dla nowych preferencji premium używają `collectAsStateWithLifecycle()`.
- **Weryfikacja końcowa**
  - `./gradlew clean test :app:compileDebugKotlin :app:lintDebug` zwraca `BUILD SUCCESSFUL` i status lintera `No issues found`.

#### Smart Home, wewnętrzny PiP wideo i wygaszacz OLED (zamrożenie kodu)
- **Webhooki Home Assistant Smart Cinema** (`ApiConfigRepository`, `SettingsScreen`, `AdminHttpServer`, `HomeAssistantWebhookClient`, `PlayerViewModel`, `PlayerViewModelTest`)
  - Zaszyfrowane preferencje DataStore `homeAssistantUrl`, `homeAssistantToken`, `homeAssistantWebhookEnabled` w `ApiConfigRepository`.
  - Sekcja w `SettingsScreen` z przełącznikiem, polem adresu Home Assistant i maskowanym tokenem webhooka.
  - Bezprzewodowy panel admina `/api/config` eksponuje te same trzy pola z maskowaniem tokena.
  - `OkHttpHomeAssistantWebhookClient` wysyła `POST {url}/api/webhook/{token}` z JSON `{"event":"play|pause|stop","profile":"...","media":"..."}` na `Dispatchers.IO` za pomocą globalnego `OkHttpClient`.
  - `PlayerViewModel` wyzwala webhooki z `setIsPlaying` (play/pause) i `finishPlayback` (stop) zarówno dla ExoPlayera, jak i LibVLC.
  - Adres i token webhooka nie są logowane do Logcat w buildach release (`BuildConfig.DEBUG == false`).
  - `PlayerViewModelTest` weryfikuje wywołania webhooków `play`, `pause` i `stop` przy użyciu MockK.
- **Wewnętrzny mini-player wideo na HomeScreen** (`VideoPipManager`, `InAppVideoPipOverlay`, `PlayerScreen`, `TVNavHost`, `MainActivity`)
  - Singleton `VideoPipManager` z życiem aktywności przechowuje aktywną instancję `ExoPlayer` i `MediaItem` między ekranami.
  - Wciśnięcie WSTECZ z `PlayerScreen` zachowuje playera, zamyka ekran i wysuwa zaokrąglony mini-odtwarzacz w prawym dolnym rogu `HomeScreen`.
  - Wideo i dźwięk grają bez przerwy; mini-player używa małego `PlayerView` podłączonego do tego samego `ExoPlayer`.
  - Środkowy przycisk D-Pada w mini-playerze przechodzi z powrotem do `PlayerScreen`, które ponownie przyłącza tego samego playera.
  - Przycisk Stop w mini-playerze całkowicie zwalnia playera (`VideoPipManager.stopAndRelease()`).
  - `MainActivity.onDestroy()` wywołuje `videoPipManager.stopAndRelease()`, aby zapobiec wyciekom przy zabijaniu aplikacji.
  - `HomeScreen` ukrywa mini-player audio, gdy aktywny jest wideo-PiP, aby dolne paski się nie nakładały.
  - Subskrypcje stanu wideo-PiP używają `collectAsStateWithLifecycle()` (Zasada 4).
- **Wygaszacz ekranu OLED** (`OledBurnInSaver`, `TVNavHost`)
  - Główny listener zdarzeń D-Pada w `TVNavHost` resetuje 5-minutowy (300 000 ms) licznik bezczynności po każdym wciśnięciu klawisza.
  - Po upływie czasu, gdy żadne wideo nie jest odtwarzane (`PlayerViewModel.isPlaying` i `VideoPipManager.isInPipMode` false), wygaszacz nakłada ekran 85% przyciemnieniem, minimalistycznym cyfrowym zegarem i wolno poruszającymi się okładkami z cache Coil.
  - Dowolne wciśnięcie przycisku pilota natychmiast zamyka wygaszacz i przywraca pełną jasność.
  - Okładki i zegar używają `collectAsStateWithLifecycle()` oraz `LocalLocale`, aby spełnić wymagania lint Compose.
- **Ostateczne utwardzenie kodu i zamrożenie**
  - Przeprowadzono audyt nowych workerów, serwerów HTTP i komponentów odtwarzacza pod kątem poprawności `release()` / `finally`.
  - Usunięto tymczasowe wpisy `Log.d` z `HomeViewModel`; pozostałe logowanie jest chronione przez `BuildConfig.DEBUG`.
  - Zweryfikowano `./gradlew clean test :app:compileDebugKotlin :app:lintDebug` ze statusem `No issues found`.

#### UI / UX
- **Silnik monitorowania zdrowia źródeł w tle** (`HealthCheckWorker`, `HealthCheckEngine`, `HealthStatus`, `SourceHealth`)
  - `CoroutineWorker` uruchamiany co 4 godziny na `Dispatchers.IO` sprawdza dostępność wszystkich skonfigurowanych źródeł (Kodi, IPTV M3U, EPG, Plex, Jellyfin, Emby, Subsonic, proxy Deezer, kanały RSS podcastów, dodatki Stremio, repozytoria Cloudstream i bazowe URL-e źródeł web).
  - Używa lekkich endpointów specyficznych dla źródła (`JSONRPC.Version`, `/System/Info/Public`, `/identity`, `/rest/ping.view` itp.) z timeoutem 3 sekund.
  - Status poszczególnych źródeł (`ONLINE`, `OFFLINE`, `UNCONFIGURED`) jest przechowywany jako JSON w DataStore i wyświetlany jako kolorowe kropki w `SettingsScreen` (`SourceHealthSection`) oraz w bezprzewodowym panelu admina.
  - Wszystkie połączenia HTTP i dedykowany `OkHttpClient` (dispatcher/pula połączeń) są sprzątane w bloku `finally`; w kompilacjach release nie logowane są URL-e ani dane uwierzytelniające.
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

#### Integracja z Android TV / Google TV
- **Per-Profile Watch Next i Dynamiczne Kanały Rekomendacji** (`TvLauncherManager`, `RecommendationsWorker`, `WatchNextHelper`, `TvLauncherManagerTest`)
  - `TvLauncherManager` obserwuje `ProfileRepository.currentProfile` i przy każdej zmianie profilu asynchronicznie (na `Dispatchers.IO`) czyści wszystkie kafelki systemowego pulpitu, a następnie ponownie wypełnia je danymi bieżącego użytkownika.
  - Niedokończone seanse pobierane są z `WatchHistoryRepository`, a pozycje z listy do obejrzenia z `SavedMediaRepository` — wyłącznie dla aktualnego `profileId`.
  - Wszystkie pozycje przepuszczane są przez `ContentFilter` z limitami `maxAgeRating` i `allowNsfw` aktywnego profilu; pozycje bez deklaracji wieku są odrzucane dla profili dziecięcych (tryb fail-closed).
  - Wiersz Watch Next publikuje `WatchNextProgram` z typem `CONTINUE` dla pozycji wznowienia i `WATCHLIST` dla zapisanych pozycji.
  - Kanał rekomendacji/preview budowany jest z `FederatedMediaRepository.featured()` (MDBList, Kitsu, chmury domowe), filtrowany do filmów/seriali/odcinków i ponownie filtrowany przez `ContentFilter`.
  - `RecommendationsWorker` zmienił interwał z 24 h na 12 h, ma ograniczenia `NetworkType.CONNECTED` + `requiresBatteryNotLow` i używa `ExistingPeriodicWorkPolicy.UPDATE`, aby nowy interwał zastąpił stare zaplanowania.
  - `TVHubApplication` wywołuje teraz `tvLauncherManager.start()` po zaplanowaniu workerów; manager nie uruchamia się już automatycznie w `init`, co poprawia testowalność.
  - Wszystkie operacje `ContentResolver` używają `.use`, wywołania helperów delete/insert wykonują się na `Dispatchers.IO`, a `ContentFilter` odrzuca niedozwolone pozycje przed jakimkolwiek IPC.
  - W wersjach produkcyjnych (`BuildConfig.DEBUG == false`) nie logowane są `profileId` ani prywatne metadane filmów.
  - Dodano test `TvLauncherManagerTest` (JUnit) weryfikujący `isUnfinished`, `buildWatchNextItems`, `buildWatchlistItems` oraz `buildPreviewItems` dla profili dziecięcych i dorosłych.

#### Raportowanie awarii w chmurze
- **Zabezpieczony silnik raportowania awarii w chmurze** (`CrashReportActivity`, `CrashReportSanitizer`, `GlobalExceptionHandler`, `cloudflare-resolver/index.ts`)
  - Cloudflare Worker udostępnia endpoint `POST /report-error` chroniony nagłówkiem `X-Hub-Token`; dodaje znacznik czasu i emituje zanonimizowany ślad stosu do logów Observability (opcjonalnie też do powiązanej przestrzeni `CRASH_REPORTS` KV).
  - `CrashReportActivity` dodaje obsługiwany D-Padem przycisk **Wyślij raport do chmury**, który odczytuje `last_crash.txt`, redaguje klucze API, tokeny OAuth i dane premium przez `CrashReportSanitizer`, a następnie wysyła raport na endpoint `/report-error` Workera.
  - `GlobalExceptionHandler` przekazuje do `CrashReportActivity` bieżące `cloudflareWorkerUrl` i `cloudflareAuthToken` przez extra intentu, więc proces `:crashreport` nie musi odczytywać DataStore.
  - `CrashReportSanitizer` działa offline na urządzeniu przed jakimkolwiek wywołaniem sieciowym, zastępując wykryte sekrety przez `****`.
  - Do wysyłki używany jest lekki, jednorazowy `OkHttpClient`; ciało odpowiedzi oraz dispatcher/pula połączeń są jawnie zamykane/czyszczone, aby zapobiec wyciekom zasobów w procesie `:crashreport`.
  - Token autoryzacyjny Cloudflare, adres Workera ani metadane raportu nie są logowane do Logcat w buildach release (`BuildConfig.DEBUG == false`).
  - Dodano test `CrashReportSanitizerTest` (JUnit) weryfikujący maskowanie kluczy API, parametrów zapytania, wartości JSON, nagłówków `Authorization` i danych źródłowych.

#### Konsola deweloperska / Piaskownica wtyczek
- **Żywa piaskownica JS / JSON / Python w bezprzewodowym panelu admina** (`SandboxEngine`, `SandboxResult`, `AdminHttpServer`, `KodiMediaSource`, `QuickJsEngine`)
  - Nowy chroniony endpoint `POST /api/plugin/test?token=...&format=js|json|python`.
  - **Tryb JS**: wykonuje fragmenty QuickJS w `SandboxEngine` za pomocą `QuickJsEngine`; dostępne są globalne funkcje `httpFetch` / `httpFetchText`, a `QuickJsEngine.evaluateWithError` zwraca błędy składni/runtime do UI.
  - **Tryb JSON**: parsuje wklejony payload przez `kotlinx.serialization.json.Json.parseToJsonElement` i próbuje zamapować go na produkcyjny model `MediaItem`, zwracając `MediaItem structure valid` ze skróconym podglądem lub komunikat błędu walidacji.
  - **Tryb Python**: koduje wklejony skrypt `.py` w base64, wysyła do skonfigurowanej instancji Kodi przez `Files.WriteFile` do `special://home/addons/plugin.video.fanfilm/test_scraper.py`, następnie wywołuje `XBMC.RunScript` i zwraca wynik JSON-RPC. Python jest wykonywany przez Kodi, a nie przez Android TV.
  - Strona admina zawiera sekcję **Developer Console** z edytorem CodeMirror (załadowanym z cdnjs): numery linii, kolorowanie składni, przełączanie trybów JS/JSON/Python oraz dynamiczny przycisk akcji (`Testuj w QuickJS`, `Waliduj strukturę JSON`, `Uruchom skrypt i debuguj w Kodi`).
  - `SandboxEngineTest` (JUnit) weryfikuje walidację JSON (poprawny, uszkodzony, brak wymaganych pól) oraz orkiestrację RPC Pythona (sukces, błąd `Files.WriteFile`, pusta odpowiedź `XBMC.RunScript`).
  - Dodano reguły ProGuard `-keep` dla `com.polishmediahub.app.data.admin.**` oraz `com.polishmediahub.app.data.plugin.**`.

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
- **Inteligentny Silnik Wyboru Ścieżek Audio i Natywna Normalizacja Głośności** (`PlayerScreen`, `PlayerViewModel`, `UniversalVlcPlayer`, `SettingsScreen`, `AdminHttpServer`)
  - Nowe preferencje DataStore: `preferredAudioType` (Polski Lektor / Dubbing), `nightModeEnabled` oraz `dialogueBoostGainmB` (0–3000 mB).
  - `DefaultTrackSelector` w ExoPlayerze ocenia ścieżki polskie (`pl`/`pol`); tryb Dubbing preferuje kodeki wielokanałowe (`5.1`, `E-AC3`, `DTS`), tryb Lektor preferuje ścieżki stereo/mono z flagą lektorską; `ROLE_FLAG_DESCRIBES_VIDEO` (Audiodeskrypcja) zawsze otrzymuje najniższy priorytet.
  - Ta sama logika oceny została przeniesiona do `UniversalVlcPlayer` (`mediaPlayer.audioTracks` / `setAudioTrack`) dla pełnej parytetowości silników.
  - Natywny efekt `android.media.audiofx.LoudnessEnhancer` jest wiązany z `audioSessionId` ExoPlayera; gdy Tryb Nocny jest włączony, ustawiana jest `setTargetGain(dialogueBoostGainmB)`, co spłaszcza dynamikę i podbija ciche dialogi. Efekt jest zwalniany przy zamykaniu playera, pauzie i przełączaniu na LibVLC.
  - Dodano sekcję "Premium Audio" w `SettingsScreen` oraz bezprzewodowym panelu admina (przełącznik, suwak, selektor) z użyciem `collectAsStateWithLifecycle()`.
  - ID sesji audio są logowane wyłącznie w `BuildConfig.DEBUG`; w wersji produkcyjnej nie dochodzi do wycieku identyfikatora audio.
- **Wyprzedzające pobieranie plakatów i offline cache obrazów** (`HomePreFetchWorker`, `NetworkModule`, `TVCard`, `AsyncImage`)
  - `HomePreFetchWorker` to `@HiltWorker` `CoroutineWorker`, uruchamiany co 12 godzin z constrainami `NetworkType.UNMETERED`, `requiresDeviceIdle` i `requiresBatteryNotLow`, a także natychmiast po udanej synchronizacji Trakt.
  - Odpytuje `MediaRepository.featured()` oraz listy `Category`, zbiera unikalne `posterUrl` i `backdropUrl`, a następnie wywołuje `ImageLoader.execute(ImageRequest.Builder(...).data(url).build())`, aby rozgrzać trwały dyskowy cache Coil.
  - `TVHubApplication` implementuje `ImageLoaderFactory` i buduje singletonowy `ImageLoader` z 100 MB `DiskCache` (`File(cacheDir, "image_cache")`), `diskCachePolicy(ENABLED)` i `memoryCachePolicy(ENABLED)`.
  - Każdy `AsyncImage` w `TVCard`, `HomeScreen`, `DetailScreen`, `PlayerScreen`, `EpgScreen`, `ModernSidebarBlurPanel` i `Sidebar` używa teraz jawnego `ImageRequest` z `diskCachePolicy(ENABLED)` i `memoryCachePolicy(ENABLED)`, więc plakaty renderują się natychmiast z cache.
  - Logowanie adresów URL wewnątrz Workera jest zablokowane w wersjach produkcyjnych (`BuildConfig.DEBUG == false`).
  - Dodano test jednostkowy `HomePreFetchWorkerTest` z MockK weryfikujący wyciąganie URL-i i wywołania `ImageLoader.execute()`.

#### Źródła
- **Silnik Offloadingu Chmury Cloudflare** (subprojekt `cloudflare-resolver` + integracja `WebMediaSource`)
  - Nowy podprojekt `cloudflare-resolver/` w TypeScript/Wrangler wdrażany jako Cloudflare Worker pod adresem `https://*.workers.dev/resolve`.
  - Worker przyjmuje parametr zapytania `?url=`, opcjonalny `?headers=` w formacie JSON, weryfikuje nagłówek `X-Hub-Token` względem sekretu Workera (`HUB_TOKEN`), pobiera stronę docelową i wyciąga adres strumienia za pomocą tych samych mechanizmów co aplikacja: rozpakowywania P.A.C.K.E.R, dekodera CDA i wyrażeń regularnych na URL-ach mediów.
  - `WebMediaSource` odczytuje teraz `useCloudflareBypass`, `cloudflareWorkerUrl` i `cloudflareAuthToken` z `ApiConfigRepository` (szyfrowany DataStore).
  - Gdy bypass jest aktywny, `WebMediaSource.resolve()` najpierw odpytuje Workera; przy każdym błędzie sieci, timeout, kodzie 5xx lub 403 natychmiast i cicho wraca do lokalnego resolvera Kotlina (`JsUnpacker`, `CdaDecoder`, `QuickJsEngine`).
  - Nagłówki strumienia zwrócone przez Workera są scalane do `MediaItem.headers` w `resolveItem()`, dzięki czemu ExoPlayer / LibVLC otrzymują poprawny `Referer` / `User-Agent`.
  - Panel konfiguracyjny dodany do `SettingsScreen` i bezprzewodowego panelu QR `AdminHttpServer` (przełącznik, URL Workera, maskowany token); wrażliwy token jest maskowany w `serveConfig`.
  - Dodano `WebMediaSourceTest` z MockWebServer weryfikujący kierowanie zapytań do Workera, nagłówek `X-Hub-Token`, scalanie nagłówków i zachowanie fallbacku lokalnego.
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

#### Faza finalna: Inteligencja playera, chmurowa synchronizacja profili i aktualizacje w tle

- **Detekcja czołówki/tyłówki przez czarne klatki** (`BlackFrameDetector`, `FrameSampler`, `AudioLevelMonitor`)
  - Czysta, testowalna jednostkowo maszyna stanu wykrywająca sekwencje czarnych klatek (luma < 0,05) i cichego dźwięku (< -40 dB) trwające powyżej 1500 ms.
  - Aktywuje się w pierwszych 10% (czołówka) lub ostatnich 15% (tyłówka) trwania filmu i aktualizuje `SkipIntroState` w `PlayerViewModel`.
  - Ścieżka ExoPlayer używa `FrameSampler` (`PixelCopy` na API 24+ lub `TextureView.bitmap`) oraz `Visualizer` przez `AudioLevelMonitor`; fallback LibVLC zwraca pełną luminancję i 0 dB.
  - Jawne znaczniki `introStartMs`/`introEndMs`/`outroStartMs`/`outroEndMs` zawsze mają pierwszeństwo.
  - Wszystkie natywne listenery, wizualizery i bitmapy są zwalniane w `DisposableEffect`/`release()` (Zasada 5).

- **Chmurowa synchronizacja profili** (`CloudProfileSyncWorker`, `CloudProfileSyncClient`, `CloudProfileSyncRestore`, `cloudflare-resolver/index.ts`)
  - `CloudProfileSyncWorker` uruchamia się co 12 godzin i na żądanie, pakuje na `Dispatchers.IO` pliki `media.db` + `media.db-shm` + `media.db-wal` do ZIP i wysyła na `POST /api/sync-profiles`.
  - `GET /api/sync-profiles` pobiera ostatnią kopię; `CloudProfileSyncRestore` podstawia rozpakowane pliki przed otwarciem Room przy następnym zimnym starcie.
  - Endpoint Cloudflare Workera chroniony jest nagłówkiem `X-Hub-Token` i przechowuje ZIP w powiązanej przestrzeni KV `PROFILE_BACKUPS`.
  - W wersjach produkcyjnych nie logowane są URL Workera ani token autoryzacyjny (Zasada 6); dodano reguły ProGuard `-keep` dla `com.polishmediahub.app.data.remote.cloud.**`.
  - Dodano testy `BlackFrameDetectorTest` (JUnit) oraz `CloudProfileSyncWorkerTest` (JUnit/MockK) weryfikujące logikę detekcji i wysyłkę/powtórki.
  - Dodano `ProfileSyncMigrationTest` (instrumentalny) sprawdzający, że dane profili i historii oglądania przetrwają przywrócenie bazy Room.

- **Automatyczny aktualizator wtyczek/źródeł w tle** (`PluginUpdateWorker`)
  - Uruchamia się co 24 godziny, gdy urządzenie jest bezczynne i poziom baterii nie jest niski.
  - Odpytuje manifesty wtyczek i indeksy repozytoriów (`checkUpdates`/`syncIndexes`); jeśli wykryto nowsze wersje, czyści zoptymalizowany cache `plugins_dex`, aby przy następnym użyciu załadować zaktualizowane pliki.
  - Liczbę dostępnych aktualizacji i znacznik czasu zapisuje w `ApiConfigRepository` / DataStore i wyświetla jako czerwoną odznakę w `SettingsScreen` oraz w `AdminHttpServer`.

- **Lokalny mini-player audio na ekranie głównym** (`AudioMiniPlayer`, `AudioMiniPlayerViewModel`, `AudioRepository`)
  - Wysuwa się z dołu `HomeScreen`, gdy `AudioRepository.currentTrack` jest aktywny.
  - Wyświetla okładkę, tytuł, wykonawcę i przyciski Pauza/Stop obsługiwane D-Padem; kliknięcie paska przechodzi do `PlayerScreen`.
  - Stan subskrybowany jest przez `collectAsStateWithLifecycle()` (Zasada 4).
  - `MusicViewModel` i `PlayerViewModel` aktualizują `AudioRepository`, dzięki czemu mini-player pozostaje zsynchronizowany między ekranami.

- **Rozszerzenia ekranu ustawień i panelu admina**
  - W `SettingsScreen` sekcje "Synchronizacja profili w chmurze" i "Aktualizacje wtyczek i źródeł" z ręcznym wyzwalaniem, datą ostatniej synchronizacji i statusem.
  - Endpointy `AdminHttpServer`: `/api/profile/sync`, `/api/profile/restore` oraz `/api/plugin/update`.

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

- **Wstrzykiwanie tokenu TorBox do Kodi FanFilm**
  - `AdminHttpServer.pushAddonSettingsIfKodiConfigured()` przekazuje skonfigurowany `debridApiKey` do `plugin.video.fanfilm` pod kluczami `torbox_token` i `torbox_apikey`, gdy `debridProvider == "torbox"`, analogicznie do obsługi Real-Debrid.
- **Parytet odtwarzacza LibVLC z ExoPlayerem**
  - `UniversalVlcPlayer` stosuje preferencje językowe dla dźwięku i napisów, skanując `mediaPlayer.getTracks(Audio/Text)` i wybierając najlepszą ścieżkę `pl`/`pol`, a jednocześnie obniżając priorytet ścieżek z audiodeskrypcją.
  - Stylizacja napisów (rozmiar, kolor, przesunięcie pionowe) z `SettingsRepository` jest przekazywana do silnika LibVLC za pomocą opcji `--sub-text-scale`, `--freetype-color`, `--freetype-opacity` i `--sub-margin`.
  - Nakładka `Nerd Stats Overlay` i `Tryb Kinowy` (animowane przyciemnienie ekranu + `PlayerControls`/`CinemaInfoCard`) renderują się teraz nad `VLCVideoLayout` identycznie jak w ścieżce ExoPlayer.
- **Ochrona wycieku danych logowania i zaostrzenie CORS w panelu Admina**
  - `AdminHttpServer.serveConfig()` maskuje wszystkie odszyfrowane wrażliwe klucze (TMDB, AniList, Trakt, Debrid, Jellyfin/Plex/Emby, hasło Subsonic, MDBList) przed wysłaniem JSON-a, pokazując jedynie pierwsze 4 i ostatnie 4 znaki.
  - Usunięto dziką kartę `Access-Control-Allow-Origin: *`; serwer akceptuje żądania cross-origin wyłącznie z autoryzowanego IP admina (`http://<clientIp>:<port>`), odrzucając niepasujące nagłówki `Origin` kodem HTTP 403.
- **Kontrola rodzicielska w trybie „fail closed”**
  - `ContentFilter` odrzuca teraz każdy `MediaItem` bez zadeklarowanego ograniczenia wiekowego, gdy aktywny profil ma ustawione `maxAgeRating`, ukrywając takie treści przed profilem dziecięcym.
- **Diagnostyka odświeżania tokena Trakt**
  - `TraktAuthenticator` loguje `Log.e("TraktAuthenticator", "Krytyczny błąd automatycznego odświeżania sesji OAuth Trakt: ...")` przed zwróceniem `null` przy niepowodzeniu odświeżania.
- **Porządki w martwym kodzie**
  - Usunięto nieużywany szkielet `CastManager` oraz cały pakiet `com.polishmediahub.app.data.cast`.

- **Przywracanie fokusu D-Pada w pionowych siatkach**
  - `LibraryScreen`, `WatchlistScreen` i `CustomListDetailScreen` mają teraz `Modifier.focusGroup()` + `focusRestorer()` na `LazyVerticalGrid`, więc fokus wraca do ostatnio oglądanego kafelka po powrocie z ekranu szczegółów.
- **Hero banner na Home**
  - `HomeScreen` używa dedykowanego `HeroFeaturedBanner` z kinowym tłem, lewym i dolnym gradientem wygaszania do czerni, tytułem, rokiem, odznakami oceny TMDB/Filmweb i skupionym przyciskiem D-Pad „Odtwórz teraz ▷" `TvButton`.
- **Diagnostyka runtime LibVLC**
  - W `UniversalVlcPlayer` zamieniono dwa puste bloki `catch {}` na jawne logowanie `Log.e("UniversalVlcPlayer", ...)`, dzięki czemu błędy odtwarzania są widoczne w logcat.
- **Bezpieczeństwo endpointu wtyczek w panelu Admina**
  - `AdminHttpServer` generuje teraz unikalny token parowania na sesję i osadza go w URL/QR admina (`/admin?token=...`).
  - Wszystkie endpointy `/api/*` (w tym `/api/plugin` GET/POST i `/api/config` GET/POST) odrzucają brakujący lub niepoprawny `?token=...` kodem HTTP 403 Forbidden, zamykając wektor ataku RCE przez nieautoryzowaną instalację wtyczek z sieci LAN.
  - Wbudowana strona admina odczytuje token z URL i dołącza go do każdego zapytania `/api/*`.

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
