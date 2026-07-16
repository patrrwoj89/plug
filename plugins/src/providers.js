module.exports = [
  {
    id: 'pl-filman',
    provider: 'filman',
    name: 'Filman.cc',
    base: 'https://filman.cc',
    searchUrl: "BASE + '/wyszukiwarka?phrase=' + encodeURIComponent(query)",
    candidateFilter: `function(href) {
      var h = href.toLowerCase();
      if (!/\\/(film|serial|m|s)\\//.test(h) && !/-online/.test(h)) return false;
      if (/\\/(tag|category|page|autor|news)\\//.test(h)) return false;
      return true;
    }`,
    typeFromHref: `function(href) {
      return /\\/(serial|s)\\//i.test(href) ? 'SERIES' : 'MOVIE';
    }`,
    hostRegex: 'cda\\.pl|vider|streamtape|dood|vk\\.com|ok\\.ru|upstream|mixdrop|voe|filemoon|lulu',
    dataSrc: false,
    iframeDataSrc: false,
    seriesPathsRe: '/\\/(serial|s)\\//i',
    episodeExtra: 'function(href, text, ep){ return false; }'
  },
  {
    id: 'pl-ekino',
    provider: 'ekino',
    name: 'Ekino-TV.pl',
    base: 'https://ekino-tv.pl',
    searchUrl: "BASE + '/search/qf/?q=' + encodeURIComponent(query)",
    candidateFilter: `function(href) {
      return /\\/(watch|film|serial|movie)\\//i.test(href);
    }`,
    typeFromHref: `function(href) {
      return /\\/(serial|tv)\\//i.test(href) ? 'SERIES' : 'MOVIE';
    }`,
    hostRegex: 'cda\\.pl|vider|streamtape|dood|vk\\.com|ok\\.ru|upstream|mixdrop|voe|filemoon|lulu',
    dataSrc: false,
    iframeDataSrc: false,
    seriesPathsRe: '/\\/(serial|tv|film|movie)\\//i',
    episodeExtra: 'function(href, text, ep){ return false; }'
  },
  {
    id: 'pl-zaluknij',
    provider: 'zaluknij',
    name: 'Zaluknij.cc',
    base: 'https://zaluknij.cc',
    searchUrl: "BASE + '/?s=' + encodeURIComponent(query)",
    candidateFilter: `function(href) {
      if (!/zaluknij\\.cc\\//i.test(href) && href.charAt(0) !== '/') return false;
      if (/\\/(tag|category|page|author)\\//i.test(href)) return false;
      return true;
    }`,
    typeFromHref: `function(href) {
      return /\\/(serial|seriale|s)\\//i.test(href) || /-sezon-/.test(href) ? 'SERIES' : 'MOVIE';
    }`,
    hostRegex: 'cda\\.pl|vider|streamtape|dood|vk\\.com|ok\\.ru|upstream|mixdrop|voe|filemoon|lulu',
    dataSrc: true,
    iframeDataSrc: false,
    seriesPathsRe: '/\\/(serial|seriale|s)\\//i',
    episodeExtra: 'function(href, text, ep){ return false; }'
  },
  {
    id: 'pl-desu',
    provider: 'desu',
    name: 'Desu-Online.pl',
    base: 'https://desu-online.pl',
    searchUrl: "BASE + '/?s=' + encodeURIComponent(query)",
    candidateFilter: `function(href) {
      if (!/desu-online\\.pl\\//i.test(href) && href.charAt(0) !== '/') return false;
      if (/\\/(tag|category|page|gatunek)\\//i.test(href)) return false;
      return true;
    }`,
    typeFromHref: "function(href){ return 'SERIES'; }",
    hostRegex: 'cda\\.pl|vider|streamtape|dood|vk\\.com|ok\\.ru|sibnet|mp4upload|mixdrop|voe|filemoon',
    dataSrc: false,
    iframeDataSrc: true,
    seriesPathsRe: '/anime|odcinki|desu/i',
    episodeExtra: 'function(href, text, ep){ return false; }'
  },
  {
    id: 'pl-animezone',
    provider: 'animezone',
    name: 'AnimeZone.pl',
    base: 'https://www.animezone.pl',
    searchUrl: "BASE + '/anime?search=' + encodeURIComponent(query)",
    candidateFilter: `function(href) {
      return /\\/(odcinki|anime)\\//i.test(href);
    }`,
    typeFromHref: "function(href){ return 'SERIES'; }",
    hostRegex: 'cda\\.pl|vider|streamtape|dood|vk\\.com|ok\\.ru|sibnet|mp4upload|mixdrop|voe|filemoon',
    dataSrc: false,
    iframeDataSrc: true,
    seriesPathsRe: '/anime|odcinki/i',
    episodeExtra: 'function(href, text, ep){ return new RegExp("/" + ep + "(/|$)").test(href); }'
  },
  {
    id: 'pl-ogladajanime',
    provider: 'ogladajanime',
    name: 'OgladajAnime.pl',
    base: 'https://ogladajanime.pl',
    searchUrl: "BASE + '/search?value=' + encodeURIComponent(query)",
    candidateFilter: `function(href) {
      return /\\/(anime|odcinek)\\//i.test(href);
    }`,
    typeFromHref: "function(href){ return 'SERIES'; }",
    hostRegex: 'cda\\.pl|vider|streamtape|dood|vk\\.com|ok\\.ru|sibnet|mp4upload|mixdrop|voe|filemoon',
    dataSrc: true,
    iframeDataSrc: false,
    seriesPathsRe: '/anime|odcinek/i',
    episodeExtra: 'function(href, text, ep){ return false; }'
  },
  {
    id: 'pl-naszeanime',
    provider: 'naszeanime',
    name: 'NaszeAnime.pl',
    base: 'https://naszeanime.pl',
    searchUrl: "BASE + '/?s=' + encodeURIComponent(query)",
    candidateFilter: `function(href) {
      if (!/naszeanime\\.pl\\//i.test(href) && href.charAt(0) !== '/') return false;
      if (/\\/(tag|category|page|gatunek)\\//i.test(href)) return false;
      return true;
    }`,
    typeFromHref: "function(href){ return 'SERIES'; }",
    hostRegex: 'cda\\.pl|vider|streamtape|dood|vk\\.com|ok\\.ru|sibnet|mp4upload|mixdrop|voe|filemoon',
    dataSrc: true,
    iframeDataSrc: false,
    seriesPathsRe: '/anime|odcinki|naszeanime/i',
    episodeExtra: 'function(href, text, ep){ return false; }'
  }
];
