# Polish Media Hub — Audyt legalnych źródeł

Ten dokument zawiera listę **legalnych, publicznie dostępnych lub możliwych do samodzielnego hostowania źródeł**, które można używać z aplikacją do testów i użytku osobistego. Celowo **nie zawiera** trackerów pirackich, nieautoryzowanych playlist IPTV ani nielegalnych scraperów.

*English version: [`LEGAL_SOURCES.md`](LEGAL_SOURCES.md)*

## BitTorrent / jlibtorrent

Używaj wyłącznie do treści, do których masz prawo je udostępniać lub pobierać.

| Źródło | URL | Typ | Uwagi |
|--------|-----|-----|-------|
| Internet Archive | https://archive.org/details/bittorrent | Domena publiczna / CC | Największa legalna biblioteka torrentów |
| Ubuntu ISO torrents | https://ubuntu.com/download | Open-source | Obrazy desktop/serwer |
| Debian CD torrents | https://www.debian.org/CD/torrent-cd/ | Open-source | Nośniki instalacyjne |
| Linux Tracker | https://linuxtracker.org | Obrazy Linux | Publiczny tracker dla dystrybucji Linux |
| VODO | https://vodo.net | Indie filmy CC | Głównie filmy na licencji Creative Commons |
| Jamendo music | https://www.jamendo.com/start | Muzyka CC | Posiada także oficjalne API |

## IPTV / M3U (darmowe i legalne)

| Źródło | URL | Typ | Uwagi |
|--------|-----|-----|-------|
| IPTV-org (public links) | https://iptv-org.github.io/iptv/index.m3u | Globalne kanały publiczne | Zweryfikuj licencję każdego kanału lokalnie |

| RW1986 IPTV | https://github.com/RW1986/IPTV | Pluto/Plex/STIRR | Wyselekcjonowane darmowe legalne strumienie |
| NASA TV | https://nasa.gov/multimedia/nasatv/index.html | Domena publiczna | Oficjalne transmisje na żywo |
| TVP Info (Polska) | https://www.tvp.info | Polska TV publiczna | Wymaga polskiego IP / oficjalna aplikacja |

## EPG / XMLTV (darmowe i legalne)

Parser EPG obsługuje standardowe pliki XMLTV z tagami `<channel>`, `<programme>`, `<title>`, `<desc>`, `<date>`, `<category>` i `<icon>`. Używaj tylko danych EPG, do których masz prawo.

| Źródło | URL | Uwagi |
|--------|-----|-------|
| XMLTV.org | https://xmltv.org | Informacje o formacie XMLTV i przykładowe parsery |
| IPTV-org EPG | https://github.com/iptv-org/epg | Społecznościowe listingi EPG; zweryfikuj prawa kanałów lokalnie |
| Pluto TV EPG (MJH) | https://i.mjh.nz/PlutoTV/us.xml | Społecznościowy przewodnik Pluto TV; zweryfikuj zgodność ze swoim M3U |
| Oficjalne EPG nadawców | np. TVP, BBC iPlayer, Pluto TV | Używaj tylko oficjalnych / legalnych feedów |

Dla polskich kanałów sprawdź, czy nadawca udostępnia publiczny feed EPG, lub użyj samodzielnie hostowanego źródła XMLTV.

## Stremio (oficjalne / legalne dodatki)

| Dodatek | Źródło | Uwagi |
|---------|--------|-------|
| Cinemeta | https://v3-cinemeta.strem.io/manifest.json | Oficjalne metadane / katalogi |
| WatchHub | https://watchhub-us.strem.io/manifest.json | Oficjalny agregator where-to-watch |
| OpenSubtitles | oficjalny dodatek | Tylko napisy; konto wymagane dla niektórych funkcji |
| Public Domain Torrents | społeczność (zweryfikuj) | Filmy w domenie publicznej |

## Źródła Kodi (oficjalne repozytorium)

| Dodatek | Repo / URL | Uwagi |
|---------|------------|-------|
| YouTube | oficjalne repo Kodi | Publiczne filmy |
| Jellyfin for Kodi | https://github.com/jellyfin/jellyfin-kodi | Osobisty serwer multimediów |
| Plex for Kodi | https://github.com/plexinc/plex-for-kodi | Osobisty serwer Plex |
| Pluto TV | oficjalne repo Kodi | Darmowa TV z reklamami |
| Archive.org | dodatki społeczności | Treści w domenie publicznej |

## MDBList

MDBList (https://mdblist.com) to serwis agregujący metadane, który pozwala użytkownikom tworzyć, przeglądać i wyszukiwać publiczne listy multimediów z wykorzystaniem cross-linkowanych identyfikatorów (IMDb, TMDB, Trakt, TVDB, MyAnimeList). Aplikacja ładuje publiczne listy top oraz listy uwierzytelnionego użytkownika. Musisz uzyskać własny darmowy klucz API na https://mdblist.com/preferences/#api; aplikacja nie dostarcza kluczy ani treści.

| Źródło | URL | Uwagi |
|--------|-----|-------|
| MDBList | https://mdblist.com | Publiczne listy użytkowników; wymagany klucz API |
| Przykład publicznej listy MDBList | `https://api.mdblist.com/lists/top?apikey=<twój_klucz>` | Publiczne listy top |

## Metadane anime Kitsu

Kitsu (https://kitsu.io) to darmowe, publiczne API JSON:API dla metadanych anime i mangi. Aplikacja używa `https://kitsu.io/api/edge` jako reaktywnego fallbacku, gdy AniList jest niedostępny, z parametrem `include=mappings` w celu zachowania cross-linków do identyfikatorów MyAnimeList i AniList. Nie jest wymagany klucz API.

| Źródło | URL | Uwagi |
|--------|-----|-------|
| Kitsu API | https://kitsu.io/api/edge | Publiczne metadane anime/mangi; darmowe, brak klucza |
| Endpoint mapowań Kitsu | `https://kitsu.io/api/edge/anime/{id}/mappings` | Rekordy mapowań zewnętrznych identyfikatorów |

## Muzyka / Audio / Podcasty

| Źródło | URL | Uwagi |
|--------|-----|-------|
| Jamendo API | https://developer.jamendo.com | Muzyka CC, oficjalne API, OAuth2 |
| Free Music Archive | https://freemusicarchive.org | Muzyka CC |
| ccMixter | http://ccmixter.org | Remiksy CC |
| Musopen | https://musopen.org | Klasyczna muzyka w domenie publicznej |
| Internet Archive audio | https://archive.org/details/audio | Domena publiczna / CC |
| SomaFM | https://somafm.com | Darmowe radio internetowe |
| Radio Paradise | https://www.radioparadise.com | Darmowe radio internetowe |
| NASA: Houston We Have a Podcast | https://www.nasa.gov/feeds/podcasts/houston-we-have-a-podcast | Podcasty w domenie publicznej |
| Legal podcast RSS | własny feed źródła | Używaj tylko feedów, do których masz prawo |

### Proxy Deezer

`DeezerAudioSource` używa podanego przez użytkownika `deezerProxyUrl` (np. Cloudflare Worker). Proxy musi udostępniać endpointy oczekiwane przez `DeezerAudioSource` i być zgodne z regulaminem Deezer. Aplikacja nie zawiera żadnych kluczy Deezer ani nie omija DRM.

## Anime (oficjalne serwisy)

| Serwis | Uwagi |
|--------|-------|
| Crunchyroll | Subskrypcja; wewnętrzne API używane przez oficjalne aplikacje (brak publicznego udokumentowanego API) |
| HIDIVE | Subskrypcja |
| ADN / Wakanim (region-specific) | Subskrypcja |

Używanie nieoficjalnych klientów dla tych serwisów jest niezgodne z ich regulaminem i nie jest obsługiwane.

## Polskie wideo w domenie publicznej / legalne

| Źródło | URL | Uwagi |
|--------|-----|-------|
| Ninateka (Filmoteka Narodowa) | https://ninateka.pl | Polskie filmy, dokumenty, teatr; ograniczenia geo |
| Repozytorium Cyfrowe FN | https://repozytorium.fn.org.pl | Przedwojenne polskie filmy, kroniki |

## Polskie audio / napisy

Aplikacja automatycznie preferuje polskie audio (`pl`) i polskie napisy (`pl`) oraz pokazuje pełne etykiety językowe w UI odtwarzacza (np. „Polski (Lektor)", „Polski (Dubbing)"). Gdy występuje więcej niż jedna polska ścieżka audio, rola Audiodeskrypcji / accessibility jest depriorytyzowana, aby nie wybrać jej automatycznie.

Dla własnych treści możesz:

- Dostarczyć wielojęzyczne strumienie HLS/DASH i pozwolić ExoPlayerowi wybrać polską ścieżkę.
- Załadować zewnętrzne napisy SRT/VTT w odtwarzaczu.
- Użyć legalnych źródeł polskiego dubbingu, takich jak oficjalne serwisy VOD (Netflix/Prime/Player.pl/HBO Max wymagają własnych aplikacji / subskrypcji).
- Dla anime preferuj legalne serwisy z polskimi napisami lub oryginalnym audio + polskimi napisami (Crunchyroll, HIDIVE itp.).

## Integracja z launcherami Android TV / Google TV

Aplikacja publikuje dwa kanały systemowe na obsługiwanych launcherach:

- **Watch Next** — kontynuacja niedokończonych treści z `WatchHistoryRepository`.
- **Preview / Recommendations** — polecane treści odświeżane codziennie przez `WorkManager`.

Tylko treści dodane przez użytkownika przez legalne źródła pojawią się na launcherze. Aplikacja nie wysyła żadnych treści bez skonfigurowanych przez użytkownika źródeł.

## Legalne pakiety startowe (`legal_sources.json`)

Aplikacja zawiera `app/src/main/assets/legal_sources.json` z wyselekcjonowanymi punktami wyjścia niepodlegającymi prawom autorskim. Kreator **Essential Setup** i przycisk **Load legal sample sources** w panelu Admin mogą je załadować automatycznie:

- **Darmowa Telewizja Internetowa** — publiczne playlisty IPTV M3U i URL XMLTV EPG.
- **Muzyka i Podcasty** — przykładowe kanały RSS podcastów i placeholder `deezerProxyUrl`.
- **Katalogi Publiczne Web** — oficjalne dodatki Stremio (YouTube, TED) i pusta lista `webSources` do własnych crawlerów.
- **MDBList** — przykładowy wpis publicznej listy oraz oficjalny URL do wygenerowania własnego klucza API.

Te przykładowe źródła mają charakter wyłącznie punktu wyjścia; Ty jesteś odpowiedzialny za zweryfikowanie legalności każdego źródła w Twoim regionie.

## Polecenia testowe / CI

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
./gradlew :app:assembleDebugAndroidTest
./gradlew :app:connectedDebugAndroidTest   # wymaga emulatora lub urządzenia Android TV
./gradlew :app:recordPaparazziDebug       # generowanie / aktualizacja baseline'ów zrzutów ekranu
```

## Web scraping / JSON / JS źródła

Scrapuj tylko strony, które na to wyraźnie pozwalają (sprawdź `robots.txt` i regulamin) lub używaj publicznych API.

| Źródło | URL | Uwagi |
|--------|-----|-------|
| Archive.org Metadata API | https://archive.org/services/docs/api | Metadane domeny publicznej / CC |
| Open Library | https://openlibrary.org/developers/api | Metadane książek |
| NASA APIs | https://api.nasa.gov | Metadane obrazów / wideo |
| TVMaze | https://www.tvmaze.com/api | Metadane seriali (CC-BY-SA) |
| TMDB | https://developer.themoviedb.org | API metadanych (darmowy klucz, wymagana atrybucja) |

## Uwagi

- Aplikacja **nie zawiera** wstępnie skonfigurowanych pirackich źródeł.
- Dodawanie nieautoryzowanych strumieni, torrentów lub scraperów jest odpowiedzialnością użytkownika i może naruszać obowiązujące prawo.
- Dla treści self-hosted użyj Jellyfin, Plex, Emby, Kodi, Subsonic/Airsonic lub własnych endpointów M3U/JSON.
- Wsparcie BitTorrent przeznaczone jest do treści legalnie rozpowszechnialnych (np. domena publiczna, Creative Commons, obrazy ISO Linuxa, treści, do których masz prawa).
- Wtyczki QuickJS i web scraping mogą uzyskiwać dostęp tylko do skonfigurowanych przez Ciebie źródeł; szanuj `robots.txt` i regulaminy każdej strony.
- Dane EPG muszą pochodzić z legalnego źródła, do którego masz prawo.
- Wtyczki Cloudstream / Aniyomi i proxy Deezer są legalne tylko wtedy, gdy dane źródłowe i samo proxy są legalne dla Twojego użytku.
