# Polish Media Hub — Frequently Asked Questions

## Ogólne

### Czy aplikacja jest legalna? Czy zawiera treści?

Nie. Polish Media Hub to **agregator / odtwarzacz mediów** (shell). Nie zawiera żadnych filmów, seriali, kanałów IPTV, torrentów ani scraperów. To Ty dodajesz własne legalne źródła (Kodi, Jellyfin, Plex, Emby, M3U, wtyczki QuickJS itp.), a aplikacja je odtwarza. To Twoja odpowiedzialność, aby używać treści, do których masz prawo. Zobacz [LEGAL_SOURCES.md](LEGAL_SOURCES.md) / [LEGAL_SOURCES.pl.md](LEGAL_SOURCES.pl.md) dla listy legalnych punktów wyjścia.

### Na jakich urządzeniach działa?

Urządzenia Android TV i Google TV (armeabi-v7a / arm64-v8a) z Androidem 6.0+ (`minSdk = 23`). Interfejs jest zoptymalizowany pod nawigację pilotem D-Pad, nie pod ekrany dotykowe telefonów.

### Czy działa na telefonach / tabletach?

UI jest zbudowany dla telewizorów. Może działać na tabletach, ale nie jest zoptymalizowany pod dotyk.

## Pierwsze uruchomienie i onboarding

### Co to jest ekran Essential Addon Setup?

Po pierwszym uruchomieniu profilu kreator **Essential Setup** prosi o wybranie trzech legalnych pakietów startowych:

- **Darmowa Telewizja internetowa** — ładuje publiczne playlisty IPTV M3U i URL XMLTV EPG z `legal_sources.json`.
- **Muzyka i Podcasty** — ładuje przykładowe kanały RSS podcastów i (jeśli skonfigurowano) URL proxy Deezer.
- **Katalogi Publiczne Web** — ładuje oficjalne dodatki Stremio i skonfigurowane crawlery web.

Wybrane pakiety są aplikowane w tle, zapisywane w DataStore, a następnie aplikacja otwiera ekran główny. Możesz pominąć lub zmienić wybór później w **Admin**.

### Jak zresetować pierwszą konfigurację?

Wyczyść dane aplikacji lub odinstaluj / zainstaluj ponownie. Flaga `isFirstLaunch` jest przechowywana w DataStore; nie ma przycisku resetu w aplikacji, aby uniknąć przypadkowej utraty danych.

## Wyszukiwanie

### Jak działa wyszukiwanie głosowe?

Na ekranie **Szukaj** przesuń fokus na przycisk **mikrofonu** obok pola wyszukiwania i naciśnij SELECT. Aplikacja uruchamia systemowy rozpoznawanie mowy skonfigurowane dla języka polskiego (`pl-PL`). Po wypowiedzeniu frazy rozpoznany tekst jest wstawiany do pola wyszukiwania i automatycznie uruchamiane jest wyszukiwanie. Wyszukiwanie głosowe wymaga zainstalowanej aplikacji rozpoznawania mowy Google / systemowej.

## Źródła i treści

### Jak dodać źródła?

1. Otwórz **Admin** w panelu bocznym.
2. Zeskanuj kod QR telefonem lub otwórz `http://<IP_TV>:<port>/admin` w przeglądarce w tej samej sieci.
3. Wklej URL / konfigurację JSON dla wybranego źródła i zapisz.

Szczegółowe opisy endpointów i przykłady JSON znajdziesz w [ADMIN_PANEL.md](ADMIN_PANEL.md) / [ADMIN_PANEL.pl.md](ADMIN_PANEL.pl.md).

### Jakie źródła są obsługiwane?

- Kodi JSON-RPC z DRM, przeglądaniem katalogów wtyczek, wykrywaniem LAN i zdalnym zapisem ustawień.
- Repozytoria Cloudstream (`repo.json` / `plugins.json`) i binarne wtyczki `.cs3` / `.cs4`.
- Rozszerzenia Aniyomi `.apk` ładowane dynamicznie.
- Wtyczki QuickJS `.js`.
- Web scraping przez konfigurację JSON (Jsoup + opcjonalny fallback do QuickJS).
- IPTV M3U + XMLTV EPG.
- Jellyfin, Plex, Emby, Subsonic/Airsonic.
- Dodatki Stremio (tylko legalne/oficjalne).
- TMDB, AniList, Trakt metadata.
- Reaktywny fallback Kitsu dla anime (publiczne API, brak klucza).
- Podcasty RSS, radio internetowe M3U/PLS, proxy Deezer.
- Legalny BitTorrent (`.torrent` / magnet) dla treści, do których masz prawo lub które są wolno rozpowszechnialne.

## MDBList

### Jak dodać MDBList?

MDBList to dostawca list metadanych. Wejdź w **Ustawienia** lub bezprzewodowy panel **Admin**, wpisz swój klucz API MDBList (pobierz go na https://mdblist.com/preferences/#api) i zapisz. Aplikacja wtedy ładuje publiczne listy top, Twoje osobiste listy i obsługuje wyszukiwanie. Elementy MDBList zawierają `tmdbId`, `imdbId` i `traktId`, więc inne źródła mogą je dopasować.

### Gdzie przechowywane są moje klucze API?

Wrażliwe wartości, takie jak MDBList, TMDB, AniList, Trakt, Debrid, tokeny Jellyfin/Plex/Emby czy hasło Subsonic, są szyfrowane algorytmem AES-256-GCM z użyciem sprzętowo chronionego klucza z Android Keystore przed zapisem do DataStore. Nigdy nie są zapisywane jako jawny tekst w kopiach zapasowych. Zwykłe preferencje (motyw, jakość wideo, status EPG itp.) pozostają niezaszyfrowane dla szybkiego dostępu.

## Reaktywny fallback Kitsu dla anime

### Czym jest fallback Kitsu?

Moduł anime korzysta głównie z `AniListMediaRepository` (GraphQL AniList). Gdy AniList zawiedzie z powodu błędu sieci, timeoutu lub HTTP 429, `AnimeRepository` automatycznie przełącza się na `KitsuMediaSource`, które wywołuje publiczne API Kitsu JSON:API (`https://kitsu.io/api/edge`). Przełączenie jest ciche i nie wymaga konfiguracji użytkownika.

### Czy Kitsu wymaga klucza API?

Nie. Kitsu to darmowe, publiczne API i jest wymienione jako źródło startowe w `legal_sources.json`. Nie jest potrzebny klucz API, konto ani ręczna konfiguracja.

### W jaki sposób Kitsu linkuje inne identyfikatory anime?

Zapytania do Kitsu używają `include=mappings`. Mapowania Kitsu z `externalSite` zawierającym `myanimelist` są zapisywane w wynikowym `MediaItem` jako `malId`; mapowania zawierające `anilist` jako `aniListId`. Identyfikatory te pomagają innym wtyczkom i resolverom dopasować strumienie.

## Fallback metadanych Filmweb.pl

### Kiedy aplikacja pobiera metadane z Filmwebu?

Ekran szczegółów w Polish Media Hub najpierw ładuje metadane z TMDB. Jeśli opis z TMDB jest pusty, krótszy niż 50 znaków lub nie zawiera polskich znaków diakrytycznych (ą/ć/ę/ł/ń/ó/ś/ź/ż), `DetailViewModel` uruchamia zadanie w tle na `Dispatchers.IO`, które wyszukuje tytuł na Filmweb.pl przez publiczne API (`www.filmweb.pl/api/v1`) i pobiera polski tytuł, fabułę, plakat, ocenę społeczności oraz liczbę głosów.

### Czy Filmweb wymaga konfiguracji źródła?

Nie. Fallback metadanych Filmweb jest całkowicie automatyczny i nie wymaga klucza API. `FilmwebMediaSource` korzysta z globalnego `OkHttpClient`, więc dzieli `MemoryCookieJar` i `CloudflareBypassInterceptor` z resztą aplikacji.

### Gdzie zapisywane są dane z Filmwebu?

Wzbogacone opisy, plakaty, oceny i adresy URL są buforowane w tabeli `filmweb_cache` w Room (dodanej w wersji v14) przez `FilmwebCacheRepository`. Przy kolejnym otwarciu tego samego tytułu i roku karta szczegółów wyświetla polskie metadane natychmiast, bez ponownego odpytywania API.

### Dlaczego na niektórych kartach szczegółów widzę etykietę „Filmweb: X,X"?

Gdy uda się pobrać dane z Filmwebu, `DetailScreen` dodaje etykietę `Filmweb: X,X` obok wiersza z rokiem / czasem trwania / oceną TMDB. Jeśli etykiety nie ma, oznacza to, że TMDB dostarczył już wystarczający polski opis lub wyszukiwanie Filmweb nie znalazło dopasowania.

## TV na żywo i EPG

### Kiedy aktualizowany jest EPG?

`IptvUpdateWorker` odświeża skonfigurowane playlisty M3U oraz pliki XMLTV EPG co 12 godzin i przy zimnym starcie aplikacji. Aktualizacja wykonuje się tylko w połączeniu bez limitu danych (Wi-Fi / Ethernet), gdy urządzenie jest bezczynne i bateria nie jest na niskim poziomie. Kanały i programy są zapisywane w Room, dzięki czemu ekran **Program TV** otwiera się natychmiast z lokalnej pamięci.

### Gdzie sprawdzić status ostatniej synchronizacji EPG?

Status jest wyświetlany u dołu ekranu **Program TV**, w ekranie **Ustawień** oraz w bezprzewodowym panelu **Admin** (`lastEpgSyncAt`, `lastEpgSyncStatus`, `lastEpgSyncError`).

### Czy mogę użyć NAS / domowego serwera?

Tak. Kodi, Jellyfin, Plex, Emby, Subsonic oraz zwykłe endpointy M3U/XMLTV w sieci LAN są w pełni obsługiwane. Czysty HTTP do `localhost`, `127.0.0.1` i hostów LAN jest włączony w konfiguracji bezpieczeństwa sieci.

### Gdzie znaleźć legalne playlisty M3U / IPTV?

Zobacz sekcję **IPTV / M3U** w [LEGAL_SOURCES.md](LEGAL_SOURCES.md) / [LEGAL_SOURCES.pl.md](LEGAL_SOURCES.pl.md). Plik `legal_sources.json` w aplikacji zawiera także wyselekcjonowane publiczne punkty wyjścia, które są weryfikowane pod kątem aktualnych publicznych źródeł (ostatnia weryfikacja: 15.07.2026) i można je załadować podczas onboarding-u lub z panelu Admin. Zawsze zweryfikuj licencję każdego strumienia lokalnie.

### Czy aplikacja może scrapować dowolną stronę?

Tylko strony, do których scrapowania masz prawo. Szanuj `robots.txt` i regulaminy. Konfiguracja źródła web używa selektorów Jsoup i może fallbackować do QuickJS dla stron dynamicznych.

### Czym są wtyczki Cloudstream / Aniyomi?

Cloudstream i Aniyomi to istniejące ekosystemy wtyczek na Android TV / mobile. Polish Media Hub może ładować ich pliki binarne bezpośrednio do pamięci przez `DexClassLoader` (bez systemowego instalatora):

- `.cs3` / `.cs4` dla Cloudstream.
- `.apk` dla rozszerzeń Aniyomi.

Klasy wtyczek są adaptowane do kontraktu `MediaSource` aplikacji i dzielą globalny `OkHttpClient`, więc dziedziczą `CookieJar`, obsługę `User-Agent`/`Referer` oraz bypass Cloudflare.

## Profile

### Jak utworzyć nowy profil?

UI zarządzania profilami znajduje się na górze **panelu bocznego**. Wybranie aktualnego profilu otwiera picker. API tworzenia / zarządzania profilami udostępnia `ProfileRepository`; przyszła aktualizacja UI doda bezpośredni przycisk „Dodaj profil" w oknie.

### Co to jest `default_profile`?

Po pierwszej instalacji automatycznie tworzy się profil domyślny o nazwie „Default". Cała istniejąca historia oglądania, biblioteka, lista obserwowanych i listy własne są migrowane do tego profilu przy aktualizacji bazy danych.

### Czy profile są chronione PIN-em?

Każdy profil może mieć `isPinLocked = true` i 4-cyfrowy `pinCode`. Po wybraniu zablokowanego profilu wyświetlany jest `PinScreen`. Weryfikacja PIN-u odbywa się przez `ProfileRepository.verifyPin()` i jest obecnie per-profil (globalny PIN dla Settings/Admin nadal istnieje w `PinRepository`).

### Jak działa Kontrola Rodzicielska?

Każda encja `ProfileEntity` przechowuje `maxAgeRating` (np. G, PG, PG-13, R, NC-17, 7/12/16/18) oraz `allowNsfw` (Boolean). Zanim wyniki `search()`, `categories()` lub `featured()` trafią do UI, `ContentFilter` usuwa pozycje, których kategoria wiekowa przekracza limit profilu lub które są oznaczone jako treści dla dorosłych/NSFW. Filtr jest stosowany w `CompositeMediaRepository`, `FederatedMediaRepository` i `PluginMediaSource`.

### Jak zmienić ustawienia Kontroli Rodzicielskiej?

Otwórz **Ustawienia** (chronione PIN-em), przewiń do sekcji **Kontrola Rodzicielska** i wybierz maksymalną kategorię wiekową oraz przełącznik NSFW dla każdego profilu. Zmiany wchodzą w życie natychmiast i są zapisywane w tabeli `profiles` w Room (v12).

### Czy historia oglądania jest współdzielona między profilami?

Nie. Każdy profil ma własne wiersze `WatchedEntity`, `SavedMediaEntity`, `CustomListEntity` i `AudioHistoryEntity`. Przełączenie profilu natychmiast filtruje wiersz „Kontynuuj oglądanie" na Home, Bibliotekę, listę Obserwowanych i listy własne.

### Czy ekran główny Android TV aktualizuje się przy zmianie profilu?

Tak. `TvLauncherManager` nasłuchuje `ProfileRepository.currentProfile`, czyści `WatchNextPrograms` i publikuje ponownie niedokończone treści nowego profilu.

## Nowoczesny panel boczny

### Dlaczego panel boczny znika?

**Nowoczesny panel boczny** celowo zwija się do małej pływającej pigułki podczas przeglądania treści. D-Pad LEFT na skrajnym lewym elemencie go otwiera. Jeśli zostawisz panel otwarty bez interakcji na 1500 ms, automatycznie się zwinie.

### Jak otworzyć panel boczny bez przechodzenia do skrajnego lewego elementu?

Na ekranie głównym możesz nacisnąć **BACK**, gdy panel jest zwinięty. D-Pad LEFT na dowolnym zaznaczonym elemencie w lewej kolumnie również działa.

### Czy fokus w poziomym rzędzie pamięta moją pozycję?

Tak. Poziome `LazyRow` (np. w `CategoryRow` na ekranie głównym) używają `Modifier.focusGroup()` i `focusRestorer()`. Jeśli przewiniesz do 5. kafelka, zjedziesz w dół, a następnie wrócisz w górę, fokus wraca na 5. pozycję zamiast resetować się do pierwszego kafelka.

## BitTorrent

### Czy mogę streamować torrenty?

Tak, ale **tylko legalne treści** (domena publiczna, Creative Commons, obrazy ISO Linuxa, treści, które stworzyłeś lub do których masz prawa). Polish Media Hub używa `jlibtorrent` z pobieraniem sekwencyjnym i lokalnym serwerem proxy HTTP (`TorrentHttpServer`), dzięki czemu ExoPlayer streamuje podczas pobierania kawałków.

### Gdzie trafiają pobrane pliki?

Dane torrent zapisywane są w prywatnym katalogu cache aplikacji. Lokalny proxy HTTP czyta z częściowo pobranego pliku. Aplikacja domyślnie nie seeduje; ustawienia seedowania mogą zostać dodane w przyszłości.

### Jak dodać torrent?

Przejdź do **Torrents** w panelu bocznym, wklej magnet URI lub URL `.torrent` i potwierdź. Engine wykona announce, zbuforuje początek i przekaże URL `http://127.0.0.1:<port>/stream?infoHash=...&file=...` do odtwarzacza.

## Wtyczki QuickJS

### Jak napisać wtyczkę?

Zobacz [PLUGIN_GUIDE.md](PLUGIN_GUIDE.md) / [PLUGIN_GUIDE.pl.md](PLUGIN_GUIDE.pl.md). Wtyczka to plik JavaScript, który udostępnia funkcje takie jak `search(query)`, `categories()`, `featured()` i `resolve(id)`. Runtime udostępnia globalną funkcję `httpFetch(url, headersJson)` zwracającą `{code, body, headers, error}`.

### Czy wtyczka może dodać nagłówki dla ExoPlayera?

Tak. Zwróć obiekt `headers` w `MediaItem`. Odtwarzacz przekaże te nagłówki (np. `User-Agent`, `Referer`) do `DefaultHttpDataSource.Factory` w ExoPlayerze.

### Niektóre pliki MKV/AVI lub dźwięk DTS/AC3 nie odtwarzają się. Co robić?

Włącz opcję **Użyj odtwarzacza LibVLC** w **Ustawieniach > Odtwarzacz** (lub z poziomu bezprzewodowego panelu Admina). Spowoduje to przełączenie `PlayerScreen` na wbudowany w proces aplikacji `UniversalVlcPlayer` oparty na `org.videolan.android:libvlc-all`. LibVLC obsługuje wiele kodeków i kontenerów, których ExoPlayer na Android TV może nie dekodować, w tym dźwięk DTS/AC3 oraz pliki MKV/AVI z torrentów, Kodi i wtyczek webowych.

### Czym wtyczki Cloudstream / Aniyomi różnią się od QuickJS?

QuickJS to JavaScript wykonywany w silniku wbudowanym w aplikację. Wtyczki Cloudstream/Aniyomi to skompilowane pliki DEX/APK ładowane dynamicznie przez `DexClassLoader` i adaptowane refleksyjnie. Oba rodzaje wytwarzają obiekty `MediaItem` i mogą dostarczać nagłówki dla ExoPlayera.

## EPG / Telewizja na żywo

### EPG jest puste, co zrobić?

Dodaj playlistę M3U i URL XMLTV EPG w **Admin** lub podczas **Essential Setup**. Następnie otwórz **EPG**. Parser obsługuje tagi `<channel>`, `<programme>`, `<title>`, `<desc>`, `<date>`, `<category>` i `<icon>`.

### Czy mogę oglądać TV na żywo bez EPG?

Tak, kanały IPTV są również wyświetlane na ekranie **IPTV**. EPG jest opcjonalne i zapewnia widok siatki oś czasu.

## Listy własne

### Czym są listy własne?

Listy własne pozwalają grupować pozycje (filmy, seriale, odcinki) niezależnie od globalnej Biblioteki lub listy Do obejrzenia. Każda lista ma `listId` i nazwę oraz jest przechowywana w `CustomListEntity` z kluczem obcym `profileId`.

### Jak wyświetlić zawartość listy własnej?

Otwórz **Listy własne** w panelu bocznym, zaznacz wybraną listę i naciśnij **SELECT**. Aplikacja przechodzi do `CustomListDetailScreen`, który pokazuje zawartość listy w 5-kolumnowej siatce `LazyVerticalGrid` z kafelkami `MediaCard`.

## Launcher Android TV

### Dlaczego nie widzę Polish Media Hub na ekranie głównym?

Na obsługiwanych launcherach Android TV / Google TV aplikacja publikuje kanał **Watch Next** i kanał rekomendacji **Preview**. Systemowy launcher decyduje, czy i jak wyświetlać kanały. Upewnij się, że aplikacja była otwarta co najmniej raz i `RecommendationsWorker` wykonał się.

### Czy kanał Watch Next respektuje profile?

Tak. Jest powiązany z `WatchHistoryRepository` aktywnego profilu. Zmiana profilu czy stare kafelki i publikuje niedokończone elementy nowego profilu.

## Audio / Napisy / Muzyka

### Dlaczego automatycznie włącza się ścieżka AD (Audiodeskrypcja)?

Nie powinno się tak dziać. ExoPlayer preferuje polskie audio, ale depriorytyzuje ścieżki oznaczone `ROLE_FLAG_DESCRIBES_VIDEO` (Audio Description). Możesz ręcznie przełączać ścieżki audio w panelu odtwarzacza; pełne etykiety, np. „Polski (Lektor)", są wyświetlane, gdy są dostępne.

### Czy mogę załadować zewnętrzne napisy?

Tak. `PlayerScreen` obsługuje URL-e napisów `.vtt` i `.srt`. Język napisów domyślnie to polski, chyba że element multimedialny określa inny. Wtyczki mogą także dostarczyć mapę `subtitleHeaders` w `MediaItem`, dzięki czemu nagłówki autoryzacyjne są przekazywane podczas pobierania pliku napisów. W Ustawieniach możesz zmienić rozmiar, kolor i przesunięcie pionowe napisów; zmiany są stosowane na żywo.

### Skąd biorą się podcasty i radio?

- **Podcasty**: kanały RSS skonfigurowane w **Admin** (`podcastFeeds`) lub załadowane podczas onboarding-u.
- **Radio**: playlisty M3U / PLS. `RadioRepository` współdzieli pole `iptvSourceUrls`, więc każdy URL M3U/PLS może pojawić się w sekcji Muzyka.
- **Deezer**: URL proxy `deezerProxyUrl` podany w Admin. Proxy musi udostępniać endpointy oczekiwane przez `DeezerAudioSource`.

### Czy wyniki Deezer są mieszane z wynikami wideo?

Nie. `DeezerAudioSource` implementuje `AudioSource` i **nie** jest rejestrowany w `SourceRegistry`. Ładuje dane tylko do ekranu **Muzyka**, izolując muzykę od federacji wideo i limitów zapytań.

## Nerd Stats w odtwarzaczu

### Co to jest nakładka Nerd Stats?

Panel w prawym górnym rogu odtwarzacza pokazujący dane diagnostyczne w czasie rzeczywistym: aktualna rozdzielczość + fps, aktywne kodeki wideo/audio, bieżący bitrate oraz pominięte / jank klatki. Włączasz ją w **Ustawienia → Pokaż statystyki ładowania**.

### Czy to wpływa na wydajność?

Statystyki są zbierane przez `AnalyticsListener` ExoPlayera i eksponowane w osobnym `StateFlow`. Rekompozycje dotyczą tylko małego panelu statystyk, więc reszta ekranu `PlayerScreen` nie jest odświeżana.

## Auto-Play Next

### Jak działa nakładka następnego odcinka?

Dla seriali, gdy do końca odcinka pozostaje 15 sekund, pojawia się półprzezroczysta nakładka z metadata następnego odcinka i licznikiem 15 → 0. Możesz nacisnąć **Odtwórz teraz**, aby pominąć napisy końcowe, lub **Anuluj** / **BACK**, aby dokończyć obecny odcinek.

### Czy to działa dla filmów?

Nie, nakładka jest uruchamiana tylko dla `MediaItem.Type.SERIES` / odcinków.

## Synchronizacja Trakt.tv

### Jak działa dwukierunkowa synchronizacja z Trakt?

`TraktSyncWorker` uruchamia się co 6 godzin (a także ręcznie z **Ustawień** lub panelu admina). Pobiera z Trakt historię obejrzanych pozycji i listę do obejrzenia, zapisując je w tabelach Room aktywnego profilu. Pozycje obejrzane lub dodane do listy do obejrzenia lokalnie w trybie offline są wysyłane z powrotem do Trakt. Konflikty są rozwiązywane na podstawie timestampu — nowszy rekord wygrywa. Od czerwca 2026 r. Trakt paginuje `sync/watched` i `sync/watchlist`; aplikacja odczytuje `X-Pagination-Page-Count` i ładuje wszystkie strony, więc duże historie pozostają zsynchronizowane.

### Gdzie wpisać dane logowania do Trakt?

Otwórz **Ustawienia → Synchronizacja Trakt** lub bezprzewodowy **panel Admin**. Wpisz **Trakt client ID** i **client secret** (oba znajdziesz na `trakt.tv/oauth/applications`), a następnie dotknij **Zaloguj się przez Trakt**. Aplikacja wyświetli `user_code` i kod QR, a w tle będzie odpytywać Trakt do momentu autoryzacji. Wpisz kod na stronie aktywacji wyświetlonej na ekranie (lub zeskanuj kod QR telefonem). Po autoryzacji token dostępu i odświeżania są szyfrowane AES-256-GCM w Android Keystore i zapisywane w DataStore.

### Czym jest tryb obrazu w obrazie (Picture-in-Picture)?

Gdy odtwarzasz wideo i naciśniesz przycisk **Home** (lub w inny sposób opuścisz aplikację), odtwarzacz przełącza się w natywny Androidowy Picture-in-Picture (PiP). Ukrywane są wszystkie kontrolki, napisy, nakładka Nerd Stats, karta informacyjna Trybu Kinowego i nakładka następnego odcinka — pozostaje sam strumień wideo. Po powrocie do aplikacji pełny interfejs odtwarzacza jest przywracany.

### Czym jest scrobbling Trakt?

Podczas odtwarzania aplikacja wysyła `/scrobble/start` po rozpoczęciu odtwarzania, `/scrobble/pause` po pauzie i `/scrobble/stop` po zamknięciu odtwarzacza. Każde żądanie zawiera dokładny procent postępu, więc Trakt wie, na jakim etapie jesteś w filmie lub odcinku.

### Czy muszę ponownie parować Trakt po wygaśnięciu tokena dostępowego?

Nie. `TraktAuthenticator` jest podpięty pod dedykowanego klienta `OkHttpClient` dla Trakt. Gdy zapytanie do API Trakt zwróci HTTP 401, odczytuje zaszyfrowany `refresh_token` z DataStore, wywołuje endpoint `/oauth/token` z `grant_type=refresh_token`, szyfruje nowe tokeny i transparentnie ponawia pierwotne żądanie.

## Pomijanie czołówki / końcówki

### Jak działa przycisk "Pomiń czołówkę"?

Jeśli wtyczka lub źródło dostarczy dokładne znaczniki `introStartMs`/`introEndMs` w `MediaItem`, odtwarzacz ich użyje. W przeciwnym razie użyje domyślnych czasów ustawionych w **Ustawienia → Pomijanie czołówki / końcówki**. Gdy głowica wejdzie w segment czołówki, wysuwa się przycisk „Pomiń czołówkę” i przejmuje fokus pilota D-Pad; jego naciśnięcie przesuwa odtwarzanie za czołówkę. Analogicznie dla końcówki/napisów końcowych przycisk wywołuje nakładkę Auto-Play Next, aby uruchomić następny odcinek lub anulować.

### Gdzie zobaczyć status ostatniej synchronizacji?

Ekran **Ustawień** oraz bezprzewodowy **panel Admin** pokazują „Ostatnia synchronizacja Trakt: [data] — [status]”. W przypadku błędu wyświetlana jest także jego treść.

## Tryb Kinowy

### Czym jest Tryb Kinowy?

**Tryb Kinowy** (przełącznik w **Ustawieniach**) to tryb odtwarzania zaprojektowany dla ciemnych pomieszczeń. Gdy wideo jest odtwarzane, nakładki automatycznie chowają się po krótkim czasie bez aktywności, a ekran płynnie przyciemnia się. Po pauzie lub naciśnięciu przycisku pilota interfejs rozjaśnia się ponownie.

### Co pokazuje karta informacyjna przy pauzie?

Po pauzie pod suwakiem postępu pojawia się karta z tytułem, opisem, gatunkami i top 5 aktorami aktualnego filmu lub odcinka. Dane o obsadzie są pobierane w tle z TMDB (lub metadanych Trakt) zaraz po otwarciu odtwarzacza.

## Spoiler Blur

### Dlaczego opis odcinka jest rozmyty?

Gdy **Ustawienia → Rozmycie spoilerów** jest włączone, opisy odcinków, które nie były jeszcze oglądane przez aktywny profil, są rozmywane, aby uniknąć spoilerów. Naciśnij **SELECT** / **D-Pad Center** na opisie, aby go odsłonić na czas seansu.

### Skąd aplikacja wie, że odcinek był oglądany?

Sprawdza `WatchHistoryRepository` dla pozycji odtwarzania większej niż próg dla danego `profileId`.

## Kodi

### Czy aplikacja może automatycznie wykryć mój serwer Kodi?

Tak, jeśli Twoje Kodi ogłasza usługę `_xbmc-jsonrpc._tcp` przez mDNS/Bonjour. `KodiDiscoveryManager` nasłuchuje przez Android NSD i automatycznie aktualizuje URL Kodi w `ApiConfigRepository`.

### Czy mogę wysłać tokeny Real-Debrid / Trakt do wtyczek Kodi?

Tak. Panel Admin zapisuje tokeny debrid / trakt. `KodiMediaSource.setAddonSetting()` wywołuje `Settings.SetSettingValue` przez JSON-RPC i przesyła je do pliku ustawień wybranej wtyczki Kodi.

### Czy Kodi DRM działa?

Tak. Gdy Kodi zwraca strumień z metadanymi DRM, aplikacja tworzy `MediaItem.DrmConfiguration` dla Widevine, PlayReady lub ClearKey przed przekazaniem strumienia do ExoPlayera.

## Cloudflare / Źródła web

### Co jeśli źródło web jest chronione przez Cloudflare?

Aplikacja zawiera **stos obejścia Cloudflare**:

- `MemoryCookieJar` przechowuje ciasteczka per host.
- `CloudflareBypassInterceptor` wykrywa 403/503 i uruchamia headless `WebView` (`HeadlessWebSolver`), aby rozwiązać wyzwanie.
- Po uzyskaniu `cf_clearance` interceptor ponawia oryginalne żądanie.
- Taki sam `User-Agent` i `Referer` są używane w całym łańcuchu.

### Czy potrafi rozpakować spakowany JavaScript?

Tak. `JsUnpacker` dekoduje powszechne skrypty spakowane formatem `eval(function(p,a,c,k,...) )` i wyciąga URL-e strumieni, które następnie przekazywane są do ExoPlayera z właściwymi nagłówkami.

## Panel administracyjny i kod QR

### Kod QR się nie skanuje.

Upewnij się, że telefon i TV są w tej samej sieci Wi-Fi. QR zawiera lokalny IP TV i dynamiczny port `AdminHttpServer` (np. `http://192.168.1.42:8123/admin`). Jeśli IP to `0.0.0.0` lub `127.0.0.1`, adapter sieciowy nie został rozpoznany.

### Czy panel administracyjny jest bezpieczny?

Panel działa celowo na czystym HTTP w lokalnej sieci. Nie należy wystawiać TV bezpośrednio na publicznym internecie. Globalna blokada PIN w **Ustawienia** zabezpiecza także ekran Admin na samym TV.

## Raportowanie awarii

### Co się dzieje, gdy aplikacja ulegnie awarii?

Nieobsługiwane wyjątki są łapane przez `GlobalExceptionHandler`. Ślad stosu jest zapisywany, a `CrashReportActivity` uruchamiana w osobnym procesie `:crashreport`. Zawieszony proces jest ubijany, więc TV nie wraca natychmiast do launchera. Ekran awarii oferuje:

- **Uruchom ponownie aplikację** — czyści log awarii i startuje `MainActivity`.
- **Wyczyść pamięć podręczną i zrestartuj** — usuwa cache aplikacji i zoptymalizowany katalog DEX wtyczek, a następnie restartuje.

### Czy raportowanie awarii zapętli się, jeśli sam ekran awarii ulegnie awarii?

Nie. `GlobalExceptionHandler` nie jest instalowany wewnątrz procesu `:crashreport`, co zapobiega rekurencyjnemu łapaniu wyjątków.

## Development i budowanie

### Jak zbudować ze źródeł?

```bash
export ANDROID_HOME=/path/to/android-sdk
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

### Jak uruchomić testy zrzutów ekranu?

```bash
./gradlew :app:recordPaparazziDebug   # generowanie / aktualizacja baseline'ów
./gradlew :app:verifyPaparazziDebug   # porównanie z baseline'ami
```

## Interfejs / UX

### Dlaczego plakat filmu animuje się podczas otwierania ekranu szczegółów?

Polish Media Hub używa Jetpack Compose **Shared Element Transitions**. `NavHost` jest owinięty w `SharedTransitionLayout`; plakat w siatce (`MediaCard`) i główny plakat na ekranie szczegółów współdzielą ten sam klucz (`poster_<item_id>`). Po naciśnięciu D-Pad Center/SELECT Compose animuje rozmiar i pozycję plakatu między dwoma ekranami zamiast go wygaszać i ponownie pojawiać.

### Gdzie zgłosić błąd?

Otwórz issue w repozytorium z krokami reprodukcji i, jeśli dotyczy, z konfiguracją źródła, którego używasz.
