# Polish Media Hub — Panel administracyjny

Wbudowany **Panel administracyjny** pozwala konfigurować wszystkie źródła z telefonu, tabletu lub komputera w tej samej sieci lokalnej co TV. Nie musisz wpisywać długich URL-i klawiaturą ekranową.

*English version: [`ADMIN_PANEL.md`](ADMIN_PANEL.md)*

## Otwieranie panelu administracyjnego

1. W aplikacji TV otwórz **Admin** w panelu bocznym.
2. Na ekranie zobaczysz:
   - kod QR,
   - pełny URL (np. `http://192.168.1.42:8123/admin?token=abcd1234…`),
   - krótką instrukcję.
3. Zeskanuj QR telefonem / czytnikiem QR lub wpisz URL w przeglądarce w tej samej sieci Wi-Fi.

URL zawiera unikalny token parowania na sesję. Wszystkie endpointy API wymagają tego tokenu w parametrze `?token=...` i zwracają `403 Forbidden`, gdy jest on nieobecny lub nieprawidłowy.

## Jak działa serwer

- `AdminHttpServer` to lekki serwer oparty na `ServerSocket` działający w korutynie `Dispatchers.IO`.
- Jest uruchamiany po otwarciu ekranu **Admin** i zatrzymywany po jego zamknięciu.
- Port przydzielany jest dynamicznie (`ServerSocket(0)`), aby uniknąć konfliktów z innymi aplikacjami lub usługami systemowymi.
- Przy starcie serwer generuje losowy token parowania (UUID) i dołącza go do URL admina (`/admin?token=...`).
- Wszystkie endpointy `/api/*` weryfikują parametr `?token=...`; brakujący lub niepoprawny token otrzymuje HTTP `403 Forbidden`.
- Żądania cross-origin są ograniczone do IP/portu widocznego w URL admina. Jeśli przeglądarka wyśle nagłówek `Origin`, który nie pasuje do `http://<IP_TV>:<port>`, żądanie jest odrzucane kodem HTTP `403 Forbidden`; dzika karta CORS nie jest już używana.
- Wbudowana strona admina odczytuje token z URL i dołącza go do każdego zapytania `/api/*`, więc nie musisz wpisywać go ręcznie.
- Błędy serwera są logowane przez `Log.w(...)` zamiast być cicho połykane, co ułatwia debugowanie interakcji z panelem.
- Serwer binduje się do lokalnego IP sieciowego wykrytego przez `NetworkAddressHelper` (zazwyczaj Wi-Fi lub ethernet).
- Czysty HTTP do IP TV jest dozwolony przez konfigurację bezpieczeństwa sieci, ponieważ ruch pozostaje w LAN.

## Endpointy

| Metoda | Ścieżka | Przeznaczenie |
|--------|---------|---------------|
| GET | `/admin` | Serwuje responsywną stronę administratora. |
| GET | `/api/config` | Zwraca aktualne wartości `ApiConfigRepository` jako JSON. Wrażliwe klucze są maskowane (pierwsze 4 + ostatnie 4 znaki). Wymaga `?token=...`. |
| POST | `/api/config` | Odbiera wartości form-encoded i zapisuje je w `ApiConfigRepository` (DataStore). Wymaga `?token=...`. |
| POST | `/api/plugin` | Odbiera URL manifestu / skryptu wtyczki i zapisuje przez `PluginRepository`. Wymaga `?token=...`. |
| POST | `/api/trakt/sync` | Uruchamia natychmiastową dwukierunkową synchronizację z Trakt (`TraktSyncWorker.startImmediate`). Wymaga `?token=...`. |
| POST | `/api/profile/sync` | Planuje natychmiastową kopię profili w chmurze (`CloudProfileSyncWorker.startBackup`). Wymaga `?token=...`. |
| POST | `/api/profile/restore` | Przywraca oczekującą kopię profili (`CloudProfileSyncRestore.restoreIfNeeded`). Wymaga `?token=...`. |
| POST | `/api/plugin/update` | Planuje natychmiastowe sprawdzenie aktualizacji wtyczek/źródeł (`PluginUpdateWorker.startImmediate`). Wymaga `?token=...`. |
| GET | `/api/health` | Zwraca aktualny wynik monitorowania zdrowia źródeł w formacie JSON. Wymaga `?token=...`. |

## Wskaźniki zdrowia źródeł

Strona admina wyświetla sekcję **Status źródeł** z kolorową kropką przy każdym skonfigurowanym źródle:

- **zielony** = `ONLINE` (ostatnie tła sprawdzenie uzyskało HTTP 200 z lekkiego endpointu zdrowia w ciągu 3 sekund)
- **czerwony** = `OFFLINE` (błąd sieci, timeout lub odpowiedź HTTP inna niż 2xx)
- **szary** = `UNCONFIGURED` (puste URL/pole)

Te same kropki pojawiają się obok pól konfiguracyjnych każdego źródła. Worker uruchamia się co 4 godziny, ale możesz też wymusić natychmiastowe sprawdzenie z poziomu **Ustawienia → Status źródeł** na telewizorze.

## Konfigurowalne źródła (POST `/api/config`)

Wyślij pola formularza odpowiadające kluczom obsługiwanym przez `ApiConfigRepository`. Strona web administratora już używa tych nazw:

| Pole | Opis |
|------|------|
| `kodiUrl` | Endpoint Kodi JSON-RPC, np. `http://192.168.1.10:8080/jsonrpc` |
| `webSourceConfig` | Tablica JSON obiektów `WebSourceConfig` (przykład poniżej) |
| `cloudstreamRepoUrls` | URL-e repozytoriów Cloudstream / Aniyomi, po jednym na linię |
| `iptvSourceUrls` | URL-e playlist IPTV M3U/M3U8, po jednym na linię |
| `epgUrl` | URL XMLTV EPG, np. `https://example.com/epg.xml` |
| `stremioAddons` | URL-e dodatków Stremio, po jednym na linię |
| `jellyfinUrl` / `jellyfinToken` | URL serwera Jellyfin i token API |
| `plexUrl` / `plexToken` | URL serwera Plex i token |
| `embyUrl` / `embyToken` | URL serwera Emby i token API |
| `forceTranscode` | `true` lub `false` |
| `maxDirectPlayBitrate` | Maksymalny bitrate w bps, np. `20000000` |
| `subsonicUrl` / `subsonicUser` / `subsonicPassword` | Dane Subsonic / Airsonic |
| `podcastFeeds` | URL-e kanałów RSS podcastów, po jednym na linię |
| `deezerProxyUrl` | URL proxy Deezer (np. `https://your-worker.workers.dev`) |
| `mdbListApiKey` | Klucz API MDBList (pobierz na https://mdblist.com/preferences/#api) |
| `tmdbApiKey` | Klucz API TMDB |
| `aniListToken` | Token dostępu AniList |
| `traktClientId` | Client ID Trakt |
| `traktClientSecret` | Client secret Trakt (wymagany do przepływu device-code OAuth) |
| `traktAccessToken` | OAuth access token Trakt (Bearer) do dwukierunkowej synchronizacji i scrobblingu |
| `traktRefreshToken` | OAuth refresh token Trakt (uzupełniany automatycznie po parowaniu kodem urządzenia) |
| `debridApiKey` / `debridProvider` | Token Debrid i provider (`real_debrid`, `torbox`) |
| `lastEpgSyncAt` | Tylko do odczytu — znacznik czasu ostatniej synchronizacji EPG/IPTV (ms od epoki) |
| `lastEpgSyncStatus` | Tylko do odczytu — status ostatniej synchronizacji: `success` lub `error` |
| `lastEpgSyncError` | Tylko do odczytu — komunikat błędu z ostatniej nieudanej synchronizacji |
| `lastTraktSyncAt` | Tylko do odczytu — znacznik czasu ostatniej synchronizacji Trakt (ms od epoki) |
| `lastTraktSyncStatus` | Tylko do odczytu — status ostatniej synchronizacji Trakt: `success` lub `error` |
| `lastTraktSyncError` | Tylko do odczytu — komunikat błędu z ostatniej nieudanej synchronizacji Trakt |
| `autoSkipIntro` | `true` lub `false` — pokazuj przyciski pomijania czołówki i końcówki |
| `introEndSeconds` | Domyślny czas końca czołówki w sekundach (np. `90`) |
| `outroDurationSeconds` | Domyślna długość końcówki liczona od końca filmu w sekundach (np. `120`) |
| `useAlternativePlayer` | `true` lub `false` — użyj wbudowanego silnika LibVLC zamiast ExoPlayera |
| `preferredAudioType` | `lector` lub `dubbing` — preferencja polskiego dźwięku |
| `nightModeEnabled` | `true` lub `false` — włącza `LoudnessEnhancer` ExoPlayera do kompresji dynamiki |
| `dialogueBoostGainmB` | Wzmocnienie trybu nocnego w mili-decybelach, `0` do `3000` (domyślnie `1000` mB) |
| `useCloudflareBypass` | `true` lub `false` — włącza Workera Cloudflare do offloadingu wyciągania strumieni web |
| `cloudflareWorkerUrl` | URL Twojego Workera, np. `https://twoj-worker.workers.dev` |
| `cloudflareAuthToken` | Współdzielony sekret `HUB_TOKEN`; maskowany w `GET /api/config` |
| `homeAssistantUrl` | Bazowy adres Home Assistant, np. `https://twoj-dom.local:8123` |
| `homeAssistantToken` | Token webhooka Home Assistant; maskowany w `GET /api/config` |
| `homeAssistantWebhookEnabled` | `true` lub `false` — wysyłaj zdarzenia `play`/`pause`/`stop` do Home Assistant |
| `amoledMode` | `true` lub `false` — wymuś czyste czarne tło `#000000` |
| `pureBlackSurfaces` | `true` lub `false` — ustaw karty/warstwy surface na czystą czerń w trybie AMOLED |
| `tunneledPlaybackEnabled` | `true` lub `false` — włącz odtwarzanie tunelowane ExoPlayera dla 4K/HDR |
| `exoplayerParallelConnections` | Liczba równoległych połączeń HTTP, `1` do `16` (domyślnie `4`) |
| `exoplayerMinBufferMs` | Minimalny czas buforowania w milisekundach (domyślnie `5000`) |
| `exoplayerMaxBufferMs` | Maksymalny czas buforowania w milisekundach (domyślnie `50000`) |
| `exoplayerBufferForPlaybackMs` | Bufor wymagany przed startem odtwarzania (domyślnie `2500`) |
| `exoplayerBufferForPlaybackAfterRebufferMs` | Bufor wymagany po rebufferze (domyślnie `5000`) |
| `exoplayerBackBufferMs` | Back buffer dla błyskawicznego cofania, `0` do `120000` (domyślnie `0`) |
| `exoplayerInitialAllocationCount` | Początkowa liczba segmentów alokatora, `0` do `64` (domyślnie `0`) |
| `exoplayerTargetBufferBytes` | Docelowy rozmiar bufora w bajtach, `0` dla domyślnego (domyślnie `-1`) |
| `streamRules` | Łańcuch JSON z konfiguracją filtrów `StreamRules` |
| `bingeGroupingEnabled` | `true` lub `false` — zapamiętuj wybrany profil źródła dla następnego odcinka |
| `lastProfileSyncAt` | Tylko do odczytu — znacznik czasu ostatniej synchronizacji profili z chmurą (ms od epoki) |
| `lastProfileSyncStatus` | Tylko do odczytu — status: `success`, `error` lub pusty |
| `lastProfileSyncError` | Tylko do odczytu — komunikat błędu z ostatniej nieudanej synchronizacji, jeśli wystąpił |
| `lastPluginUpdateAt` | Tylko do odczytu — znacznik czasu ostatniego sprawdzenia aktualizacji wtyczek (ms od epoki) |
| `lastPluginUpdateCount` | Tylko do odczytu — liczba dostępnych aktualizacji wtyczek/źródeł; `0` czyści odznakę |
| `lastPluginUpdateError` | Tylko do odczytu — komunikat błędu z ostatniego nieudanego sprawdzania, jeśli wystąpił |

Wystarczą tylko pola, których faktycznie używasz. Puste łańcuchy są ignorowane.

### Pełny przykład JSON (do użycia bezpośrednio z API)

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
  "cloudflareWorkerUrl": "https://twoj-worker.workers.dev",
  "cloudflareAuthToken": "twoj-hub-token",
  "homeAssistantUrl": "https://twoj-dom.local:8123",
  "homeAssistantToken": "twoj-webhook-token",
  "homeAssistantWebhookEnabled": "true",
  "amoledMode": "false",
  "pureBlackSurfaces": "false",
  "tunneledPlaybackEnabled": "false",
  "exoplayerParallelConnections": "4",
  "exoplayerMinBufferMs": "5000",
  "exoplayerMaxBufferMs": "50000",
  "exoplayerBufferForPlaybackMs": "2500",
  "exoplayerBufferForPlaybackAfterRebufferMs": "5000",
  "exoplayerBackBufferMs": "0",
  "exoplayerInitialAllocationCount": "0",
  "exoplayerTargetBufferBytes": "-1",
  "streamRules": "{\"enabled\":true,\"sizeMinMb\":500,\"sizeMaxMb\":51200,\"resolutions\":[\"1080p\",\"4K\"],\"preferredEncoders\":[\"HEVC\"]}",
  "bingeGroupingEnabled": "true"
}
```

Uwaga: rzeczywisty endpoint HTTP oczekuje danych `application/x-www-form-urlencoded`, a nie surowego JSON. Powyższy JSON pokazany jest dla przejrzystości. Wrażliwe pola (MDBList, TMDB, AniList, Trakt, Debrid, tokeny Jellyfin/Plex/Emby oraz hasło Subsonic) są szyfrowane algorytmem AES-256-GCM w Android Keystore przed zapisem do DataStore. W odpowiedzi `GET /api/config` zwracane są one w postaci zamaskowanej — widoczne są tylko pierwsze 4 i ostatnie 4 znaki (np. `A1B2***********C3D4`) — więc nie są widoczne dla innych urządzeń w sieci LAN. Zapisanie nowej wartości nadal działa przez `POST /api/config`, ponieważ otrzymuje jawny tekst i szyfruje go na TV. Tokeny `cloudflareAuthToken` i `homeAssistantToken` są również traktowane jako wrażliwe i maskowane w `GET /api/config`.

## Zdalna synchronizacja ustawień Kodi

Przycisk **Zsynchronizuj z Trakt teraz** w panelu web wysyła POST na `/api/trakt/sync` i natychmiast uruchamia `TraktSyncWorker`. Wymaga to ustawionych `traktClientId` i `traktAccessToken` (uzyskanych automatycznie po parowaniu kodem urządzenia).

### Odświeżanie tokenów Trakt

Dedykowany klient `@Named("trakt")` `OkHttpClient` instaluje `TraktAuthenticator`. Jeśli dowolne zapytanie do API Trakt zwróci HTTP 401, authenticator odczytuje zaszyfrowany `refresh_token`, synchronicznie wywołuje `/oauth/token`, szyfruje nową parę `access_token` / `refresh_token` i ponawia pierwotne żądanie. Nie trzeba ponownie parować urządzenia po wygaśnięciu tokena dostępowego.

Gdy zapiszesz token Debrid lub Trakt w panelu administracyjnym, aplikacja próbuje automatycznie przesłać go do skonfigurowanego Kodi:

- Wywoływana jest metoda JSON-RPC `Settings.SetSettingValue` dla `plugin.video.fanfilm`.
- `realdebrid_token` jest przesyłane, gdy `debridProvider` to `real_debrid`.
- `torbox_token` i `torbox_apikey` są przesyłane, gdy `debridProvider` to `torbox`; `torbox_apikey` używa `debridApiKey`, natomiast `torbox_token` preferuje `debridAccessToken`, a w przypadku pustego — `debridApiKey`.
- `trakt_token` jest przesyłane z `traktAccessToken`, a jeśli ten jest pusty — z `traktClientId`.
- Wymaga skonfigurowanego i osiągalnego `kodiUrl`.

## Dodawanie wtyczki (POST `/api/plugin`)

Wtyczkę możesz dodać przesyłając pole formularza `url`. Żądanie musi zawierać token parowania z URL admina jako parametr zapytania:

```
POST /api/plugin?token=TWÓJ_TOKEN
Content-Type: application/x-www-form-urlencoded

url=https://example.com/plugins/myplugin/manifest.json
```

Manifest może być `PluginManifest` (dla QuickJS), `repo.json` / `plugins.json` Cloudstream lub `index.min.json` Aniyomi. Wtyczki binarne (`.cs3`, `.cs4`, `.apk`) mogą być również wskazane bezpośrednim URL-em pobierania; `PluginRepository` pobierze i załaduje je przez `DynamicPluginLoader`.

### Przykładowy manifest QuickJS

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

Manifest lub wpis wtyczki może zawierać także pole `script` zamiast `scriptUrl`. Raw script przekazywany jest do `QuickJsMediaSource.configure(script)`.

Szczegóły API JavaScript oraz ładowania binarnego znajdziesz w [PLUGIN_GUIDE.md](PLUGIN_GUIDE.md) / [PLUGIN_GUIDE.pl.md](PLUGIN_GUIDE.pl.md).

## Konsola deweloperska (POST `/api/plugin/test`)

Panel admina zawiera żywą piaskownicę do testowania fragmentów QuickJS, payloadów JSON `MediaItem` oraz skryptów Pythona wysyłanych do Kodi — bez dotykania UI telewizora.

- Wybierz **JavaScript / QuickJS**, aby wykonać skrypt tym samym silnikiem `QuickJsEngine`, który obsługuje wtyczki. Edytor (CodeMirror załadowany z cdnjs) oferuje numery linii i kolorowanie składni. Dostępne są globalne funkcje `httpFetch(url)` oraz `httpFetchText(url)`.
- Wybierz **JSON / MediaItem validator**, aby wkleić obiekt JSON `MediaItem`. Serwer parsuje go za pomocą `kotlinx.serialization` i zwraca informację, czy da się zamapować na produkcyjny model `MediaItem`; przy sukcesie zwraca skrócony podgląd pól.
- Wybierz **Python / Kodi RPC**, aby wkleić skrypt `.py`. Aplikacja na TV koduje skrypt base64 i wysyła go do Kodi metodą `Files.WriteFile` do katalogu `special://home/addons/plugin.video.fanfilm/test_scraper.py`, a następnie wywołuje `XBMC.RunScript` i zwraca wynik JSON-RPC. Python **nie** jest uruchamiany na telewizorze — wykonuje go instancja Kodi, która zwraca odpowiedź RPC.

Ten sam token parowania chroni endpoint `/api/plugin/test` jak pozostałe `/api/*`. Tymczasowe bufory i strumienie odpowiedzi HTTP są zamykane w blokach `finally`; treści skryptów ani dane wrażliwe nie są logowane w buildach release.

## Reaktywne aktualizacje

`ApiConfigRepository` i `PluginRepository` eksponują `Flow`. Po zapisaniu konfiguracji z panelu web:

- UI TV aktualizuje się automatycznie,
- `FederatedMediaRepository` przebudowuje aktywne źródła,
- `RecommendationsWorker` może zostać wyzwolony do odświeżenia rekomendacji launchera,
- tokeny Kodi są przesyłane do skonfigurowanej instancji Kodi.

## Onboarding pierwszego uruchomienia

Nowe profile widzą ekran **Essential Addon Setup**. Możesz przygotować `legal_sources.json` w `app/src/main/assets/` z tymi samymi kluczami; kreator onboardingu załaduje wybrane pakiety do `ApiConfigRepository`. To przydatne dla buildów OEM lub współdzielonych konfiguracji domowych.

## Rozwiązywanie problemów

| Problem | Prawdopodobna przyczyna | Rozwiązanie |
|---------|------------------------|-------------|
| Kod QR prowadzi do `ERR_CONNECTION_REFUSED` | Telefon i TV są w różnych sieciach lub serwer się zatrzymał. | Ponownie otwórz **Admin**; upewnij się, że oba urządzenia są w tej samej sieci Wi-Fi i URL zawiera lokalne IP TV. |
| URL zawiera `0.0.0.0` lub `127.0.0.1` | `NetworkAddressHelper` nie znalazł interfejsu innego niż loopback. | Podłącz TV do Wi-Fi / ethernet i wyłącz VPN tunelujący cały ruch. |
| Konfiguracja zapisuje się, ale UI się nie zmienia | Repozytorium `Flow` może nie być zbierane przez ekran. | Wyjdź z ekranu i wróć; zrestartuj aplikację. |
| Wtyczka nie pojawia się w Wyszukiwarce / Home | Wtyczka mogła rzucić wyjątek podczas ewaluacji lub ładowania. | Sprawdź `logcat` pod kątem błędów QuickJS lub DEX; zweryfikuj, że URL-e zwracają poprawne dane. |
| Ustawienia Kodi nie są przesyłane | Kodi jest offline lub ID wtyczki się różni. | Sprawdź `kodiUrl` i upewnij się, że docelowa wtyczka używa kluczy `realdebrid_token` (Real-Debrid), `torbox_token` / `torbox_apikey` (TorBox) oraz `trakt_token` (Trakt). |

## Uwagi bezpieczeństwa

- Panel jest celowo prosty i działa na HTTP w lokalnej sieci. Nie wystawiaj TV bezpośrednio na publicznym internecie.
- Globalna blokada PIN w **Ustawienia** chroni także ekran Admin na samym TV.
- Czysty ruch HTTP jest dozwolony tylko dla `localhost`, `127.0.0.1` i hostów LAN w `network_security_config.xml`.
