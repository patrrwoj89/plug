// Polish Media Hub QuickJS plugin: {{NAME}}
// Auto-generated from plugin-template.js — do not edit by hand.

var BASE = '{{BASE}}';
var UA = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36';
var PROVIDER = '{{PROVIDER}}';
var TAG = '[{{PROVIDER}}]';

var HOST_REGEX = '{{HOST_REGEX}}';
var SERIES_PATHS_RE = {{SERIES_PATHS_RE}};
var DATA_SRC = {{DATA_SRC}};
var IFRAME_DATA_SRC = {{IFRAME_DATA_SRC}};

var CANDIDATE_FILTER = {{CANDIDATE_FILTER}};
var TYPE_FROM_HREF = {{TYPE_FROM_HREF}};
var EPISODE_EXTRA = {{EPISODE_EXTRA}};

function search(query) {
  if (!query) return [];
  var want = normalize(query);
  try {
    var searchUrl = {{SEARCH_URL}};
    var html = httpFetchText(searchUrl, { 'User-Agent': UA, 'Referer': BASE + '/' });
    if (!html) return [];
    var links = htmlTags(html, 'a');
    var results = [];
    var seen = {};
    for (var i = 0; i < links.length; i++) {
      var el = links[i];
      var href = (el.attrs.href || '').trim();
      var text = el.text;
      var title = (el.attrs.title || text).trim();
      if (!href || !title) continue;
      if (!CANDIDATE_FILTER(href)) continue;
      var absUrl = absoluteUrl(BASE, href);
      if (!absUrl) continue;
      if (!hostMatches(BASE, absUrl)) continue;
      var slug = stripBase(BASE, absUrl);
      if (!slug || slug === '/') continue;
      if (seen[slug]) continue;
      seen[slug] = true;
      var normTitle = normalize(title);
      var score = 0;
      if (want && normTitle.indexOf(want) >= 0) score += 3;
      if (want && title.toLowerCase().indexOf(want) >= 0) score += 1;
      if (score <= 0) continue;
      results.push({
        item: {
          id: makeId(PROVIDER, slug, TYPE_FROM_HREF(href)),
          title: title,
          type: TYPE_FROM_HREF(href),
          year: extractYear(title),
          posterUrl: null
        },
        score: score
      });
    }
    results.sort(function(a, b) { return b.score - a.score; });
    var out = [];
    for (var i = 0; i < results.length && i < 15; i++) {
      out.push(results[i].item);
    }
    return out;
  } catch (e) {
    log(TAG, 'search error', e && e.message ? e.message : e);
    return [];
  }
}

function byId(id) {
  try {
    var parsed = parseId(PROVIDER, id);
    if (!parsed) return null;
    var url = absoluteUrl(BASE, parsed.slug);
    if (!url) return null;
    var html = httpFetchText(url, { 'User-Agent': UA, 'Referer': BASE + '/' });
    if (!html) return null;
    var title = extractTitle(html) || parsed.slug;
    var poster = extractPoster(html);
    var item = {
      id: id,
      title: title,
      type: parsed.type.toUpperCase(),
      description: '',
      year: extractYear(title),
      posterUrl: poster,
      headers: { 'Referer': BASE + '/', 'User-Agent': UA }
    };
    var player = resolvePlayer(html, parsed, PROVIDER, {
      hostRegex: HOST_REGEX,
      dataSrc: DATA_SRC,
      iframeDataSrc: IFRAME_DATA_SRC,
      seriesPathsRe: SERIES_PATHS_RE,
      episodeExtra: EPISODE_EXTRA
    });
    if (player) {
      item.videoUrl = player.url;
      item.headers = player.headers;
    }
    return item;
  } catch (e) {
    log(TAG, 'byId error', e && e.message ? e.message : e);
    return null;
  }
}

function resolve(id) {
  try {
    var parsed = parseId(PROVIDER, id);
    if (!parsed) return null;
    var url = absoluteUrl(BASE, parsed.slug);
    if (!url) return null;
    var html = httpFetchText(url, { 'User-Agent': UA, 'Referer': BASE + '/' });
    if (!html) return null;
    var player = resolvePlayer(html, parsed, PROVIDER, {
      hostRegex: HOST_REGEX,
      dataSrc: DATA_SRC,
      iframeDataSrc: IFRAME_DATA_SRC,
      seriesPathsRe: SERIES_PATHS_RE,
      episodeExtra: EPISODE_EXTRA
    });
    return player ? { url: player.url, headers: player.headers } : null;
  } catch (e) {
    log(TAG, 'resolve error', e && e.message ? e.message : e);
    return null;
  }
}
