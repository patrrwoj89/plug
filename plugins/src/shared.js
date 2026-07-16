// Shared helpers for Polish Media Hub QuickJS plugins.
// Do NOT use async/await, fetch, cheerio, or module.exports here — the PMH
// runtime exposes only synchronous httpFetch(url, headersObj) returning a JSON string.

function log() {
  try {
    if (typeof console !== 'undefined' && typeof console.log === 'function') {
      console.log.apply(console, arguments);
    }
  } catch (e) {}
}

function httpFetchText(url, headers) {
  var resp;
  try {
    resp = httpFetch(url, headers || {});
  } catch (e) {
    return null;
  }
  if (!resp) return null;
  var r;
  try {
    r = JSON.parse(resp);
  } catch (e) {
    return null;
  }
  if (!r || r.code !== 200) return null;
  return r.body || '';
}

function httpFetchJson(url, headers) {
  var body = httpFetchText(url, headers);
  if (!body) return null;
  try {
    return JSON.parse(body);
  } catch (e) {
    return null;
  }
}

function normalize(s) {
  if (!s) return '';
  return String(s)
    .toLowerCase()
    .replace(/[\u0105]/g, 'a')
    .replace(/[\u0107]/g, 'c')
    .replace(/[\u0119]/g, 'e')
    .replace(/[\u0142]/g, 'l')
    .replace(/[\u0144]/g, 'n')
    .replace(/[\u00f3]/g, 'o')
    .replace(/[\u015b]/g, 's')
    .replace(/[\u017a\u017c]/g, 'z')
    .replace(/[^a-z0-9]+/g, ' ')
    .trim();
}

function detectQuality(text) {
  var t = String(text || '').toLowerCase();
  if (/2160|4k|uhd/.test(t)) return '4K';
  if (/1080/.test(t)) return '1080p';
  if (/720/.test(t)) return '720p';
  if (/480/.test(t)) return '480p';
  if (/cam|ts\b|telesync/.test(t)) return 'CAM';
  return 'HD';
}

function detectLang(text) {
  var t = String(text || '').toLowerCase();
  if (/lektor/.test(t)) return 'Lektor';
  if (/dubbing|dubb/.test(t)) return 'Dubbing';
  if (/napisy|napis|sub/.test(t)) return 'Napisy PL';
  return 'PL';
}

function parseAttrs(tagOpen) {
  var attrs = {};
  var re = /\s([a-zA-Z0-9_:\-]+)(?:=(?:"([^"]*)"|'([^']*)'|([^\s"'>]+)))?/g;
  var m;
  while ((m = re.exec(tagOpen)) !== null) {
    var name = m[1].toLowerCase();
    var val = m[2] !== undefined ? m[2] : (m[3] !== undefined ? m[3] : (m[4] !== undefined ? m[4] : ''));
    attrs[name] = val;
  }
  return attrs;
}

function htmlTags(html, tag) {
  var re = new RegExp('<' + tag + '\\b[^>]*>([\\s\\S]*?)<\\/' + tag + '>', 'gi');
  var out = [];
  var m;
  while ((m = re.exec(html)) !== null) {
    var tagHtml = m[0];
    var inner = m[1] || '';
    var text = inner.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim();
    out.push({ tag: tagHtml, text: text, attrs: parseAttrs(tagHtml) });
  }
  return out;
}

function htmlSelfClosingTags(html, tag) {
  var re = new RegExp('<' + tag + '\\b([^>]*)>', 'gi');
  var out = [];
  var m;
  while ((m = re.exec(html)) !== null) {
    var tagHtml = m[0];
    out.push({ tag: tagHtml, attrs: parseAttrs(tagHtml) });
  }
  return out;
}

function elementsWithAttr(html, attr) {
  var out = [];
  var re = new RegExp('<(\\w+)[^>]*' + attr + '=["\']([^"\']+)["\'][^>]*>([\\s\\S]*?)<\\/\\1>', 'gi');
  var m;
  while ((m = re.exec(html)) !== null) {
    var tagHtml = m[0];
    var text = (m[3] || '').replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim();
    var attrs = parseAttrs(tagHtml);
    out.push({ tagName: m[1], attrs: attrs, text: text, rawAttr: m[2] });
  }
  var re2 = new RegExp('<(\\w+)[^>]*' + attr + '=["\']([^"\']+)["\'][^>]*\\/?>', 'gi');
  while ((m = re2.exec(html)) !== null) {
    var tagHtml2 = m[0];
    var attrs2 = parseAttrs(tagHtml2);
    out.push({ tagName: m[1], attrs: attrs2, text: '', rawAttr: m[2] });
  }
  return out;
}

function base64Decode(input) {
  if (typeof atob === 'function') {
    try {
      return atob(String(input).replace(/[\t\n\r ]+/g, ''));
    } catch (e) {}
  }
  var str = String(input).replace(/[\t\n\r ]+/g, '');
  var pad = str.length % 4 === 0 ? 0 : 4 - (str.length % 4);
  for (var i = 0; i < pad; i++) str += '=';
  var chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=';
  var out = '';
  for (var i = 0; i < str.length; i += 4) {
    var e1 = chars.indexOf(str.charAt(i));
    var e2 = chars.indexOf(str.charAt(i + 1));
    var e3 = chars.indexOf(str.charAt(i + 2));
    var e4 = chars.indexOf(str.charAt(i + 3));
    var c1 = (e1 << 2) | (e2 >> 4);
    var c2 = ((e2 & 15) << 4) | (e3 >> 2);
    var c3 = ((e3 & 3) << 6) | e4;
    out += String.fromCharCode(c1);
    if (e3 !== 64) out += String.fromCharCode(c2);
    if (e4 !== 64) out += String.fromCharCode(c3);
  }
  return out;
}

function absoluteUrl(base, url) {
  if (!url) return null;
  url = String(url).trim();
  if (url.indexOf('//') === 0) url = 'https:' + url;
  if (/^https?:\/\//.test(url)) return url;
  if (url.charAt(0) === '/') return base + url;
  return base + '/' + url;
}

function hostMatches(base, url) {
  function host(u) {
    return u.replace(/^https?:\/\//, '').replace(/^www\./, '').split('/')[0];
  }
  var bh = host(base);
  var uh = host(url);
  return uh === bh || uh === 'www.' + bh || 'www.' + uh === bh;
}

function stripBase(base, url) {
  if (!url) return '';
  url = String(url).trim();
  if (url.indexOf(base) === 0) return url.substring(base.length);
  if (/^https?:\/\//.test(url)) {
    var idx = url.indexOf('/', url.indexOf('://') + 3);
    return idx >= 0 ? url.substring(idx) : '/';
  }
  if (url.charAt(0) === '/') return url;
  return '/' + url;
}

function extractYear(text) {
  var m = String(text || '').match(/\b(19|20)\d{2}\b/);
  return m ? m[0] : '';
}

function extractTitle(html) {
  var h1s = htmlTags(html, 'h1');
  if (h1s.length > 0 && h1s[0].text) return h1s[0].text;
  var titles = htmlTags(html, 'title');
  if (titles.length > 0 && titles[0].text) return titles[0].text.split(/[\|\-]/)[0].trim();
  return '';
}

function extractPoster(html) {
  var imgs = htmlSelfClosingTags(html, 'img');
  for (var i = 0; i < imgs.length; i++) {
    var src = imgs[i].attrs.src || '';
    if (/\.(jpg|jpeg|png|webp)/i.test(src) && (/poster|cover|thumb/i.test(src) || /class="[^"]*(poster|cover|thumb)[^"]*"/i.test(imgs[i].tag))) {
      return src;
    }
  }
  for (var i = 0; i < imgs.length; i++) {
    var src = imgs[i].attrs.src || '';
    if (/\.(jpg|jpeg|png|webp)/i.test(src)) return src;
  }
  return null;
}

function makeId(provider, slug, type, episode) {
  slug = stripBase(BASE, slug).replace(/^\//, '');
  var id = provider + ':' + slug + ':' + (type || 'movie').toLowerCase();
  if (episode) id += ':' + episode;
  return id;
}

function parseId(provider, id) {
  if (typeof id !== 'string') return null;
  var parts = id.split(':');
  if (parts.length < 3) return null;
  if (parts[0] !== provider) return null;
  var type = parts[parts.length - 1];
  var episode = null;
  if (/^\d+$/.test(type)) {
    episode = parseInt(type, 10);
    type = parts[parts.length - 2];
  }
  var slug = parts.slice(1, episode ? parts.length - 2 : parts.length - 1).join('/');
  return { slug: slug, type: type, episode: episode };
}

function qualityRank(label) {
  var q = detectQuality(label);
  if (q === '4K') return 5;
  if (q === '1080p') return 4;
  if (q === '720p') return 3;
  if (q === '480p') return 2;
  if (q === 'CAM') return 0;
  return 1;
}

function extractPlayers(html, base, options) {
  options = options || {};
  var out = [];
  var seen = {};
  function addPlayer(url, label) {
    if (!url) return;
    url = String(url).trim();
    if (url.indexOf('//') === 0) url = 'https:' + url;
    if (!/^https?:\/\//.test(url)) return;
    if (seen[url]) return;
    seen[url] = true;
    out.push({ url: url, label: label || '' });
  }

  // data-iframe (base64 or raw URL)
  var dataIframes = elementsWithAttr(html, 'data-iframe');
  for (var i = 0; i < dataIframes.length; i++) {
    var raw = dataIframes[i].rawAttr;
    if (!raw) continue;
    try {
      var decoded = base64Decode(raw);
      var urlMatch = decoded.match(/https?:\/\/[^"'\\ ]+/);
      if (urlMatch) {
        addPlayer(urlMatch[0], dataIframes[i].text);
        continue;
      }
    } catch (e) {}
    var rawMatch = String(raw).match(/https?:\/\/[^"'\\ ]+/);
    if (rawMatch) addPlayer(rawMatch[0], dataIframes[i].text);
  }

  // iframe src / data-src
  var iframes = htmlSelfClosingTags(html, 'iframe');
  for (var i = 0; i < iframes.length; i++) {
    addPlayer(iframes[i].attrs.src, 'iframe');
    if (options.iframeDataSrc) addPlayer(iframes[i].attrs['data-src'], 'iframe');
  }

  // other elements with data-src
  if (options.dataSrc) {
    var dataSrcs = elementsWithAttr(html, 'data-src');
    for (var i = 0; i < dataSrcs.length; i++) {
      addPlayer(dataSrcs[i].rawAttr, dataSrcs[i].text);
    }
  }

  // <a> links to known hosts
  var hostRe = new RegExp(options.hostRegex, 'i');
  var links = htmlTags(html, 'a');
  for (var i = 0; i < links.length; i++) {
    var href = links[i].attrs.href || '';
    if (hostRe.test(href)) addPlayer(href, links[i].text);
  }

  return out;
}

function findEpisodeLinks(html, episode, extraTest) {
  var ep = episode || 1;
  var links = htmlTags(html, 'a');
  var out = [];
  var rx = new RegExp('(odcinek|episode|ep|\\bodc\\b)[^0-9]*0*' + ep + '(\\b|[^0-9])', 'i');
  for (var i = 0; i < links.length; i++) {
    var href = (links[i].attrs.href || '').trim();
    var text = links[i].text;
    if (!href) continue;
    if (href === '#' || href.indexOf('javascript:') === 0 || href.indexOf('mailto:') === 0) continue;
    if (rx.test(text) || rx.test(href) || (extraTest && extraTest(href, text, ep))) {
      out.push({ href: href, text: text });
    }
  }
  return out;
}

function resolvePlayer(html, slug, provider, options) {
  options = options || {};
  var parsed = typeof slug === 'object' ? slug : parseId(provider, slug);
  if (!parsed) return null;
  var baseUrl = absoluteUrl(BASE, parsed.slug);
  if (!baseUrl) return null;
  var currentHtml = html;
  var referer = baseUrl;

  var shouldFetchEpisode = false;
  if (parsed.episode) {
    shouldFetchEpisode = true;
  } else if (parsed.type === 'series' || parsed.type === 'seriale') {
    if (options.seriesPathsRe && options.seriesPathsRe.test(parsed.slug)) shouldFetchEpisode = true;
  }

  if (shouldFetchEpisode) {
    var epLinks = findEpisodeLinks(currentHtml, parsed.episode || 1, options.episodeExtra);
    if (epLinks.length > 0) {
      var epUrl = absoluteUrl(BASE, epLinks[0].href);
      if (epUrl) {
        var epHtml = httpFetchText(epUrl, { 'User-Agent': UA, 'Referer': baseUrl });
        if (epHtml) {
          currentHtml = epHtml;
          referer = epUrl;
        }
      }
    }
  }

  var players = extractPlayers(currentHtml, BASE, {
    hostRegex: options.hostRegex,
    dataSrc: options.dataSrc,
    iframeDataSrc: options.iframeDataSrc
  });
  if (!players || players.length === 0) return null;
  players.sort(function(a, b) { return qualityRank(b.label) - qualityRank(a.label); });
  var best = players[0];
  return {
    url: best.url,
    headers: { 'Referer': referer, 'User-Agent': UA }
  };
}
