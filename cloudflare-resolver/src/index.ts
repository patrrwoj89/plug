import { cdaDecode } from './cda';
import { intToToken } from './jsunpacker';

export interface Env {
  HUB_TOKEN: string;
}

interface StreamResult {
  streamUrl: string;
  headers: Record<string, string>;
}

const MEDIA_RE = /(https?:\/\/[^\s"'<>]+\.(?:mp4|m3u8|mpd|webm|mkv|avi|flv|ts))(?:\?[^\s"'<>]*)?/i;
const URL_RE = /https?:\/\/[^\s"'<>]+/i;

export default {
  async fetch(request: Request, env: Env, _ctx: ExecutionContext): Promise<Response> {
    const url = new URL(request.url);

    if (request.method === 'OPTIONS') {
      return new Response(null, {
        status: 204,
        headers: {
          'Access-Control-Allow-Origin': '*',
          'Access-Control-Allow-Methods': 'GET, OPTIONS',
          'Access-Control-Allow-Headers': 'X-Hub-Token, Content-Type'
        }
      });
    }

    if (url.pathname !== '/resolve') {
      return jsonResponse({ error: 'Not Found' }, 404);
    }

    const token = request.headers.get('X-Hub-Token');
    if (!token || token !== env.HUB_TOKEN) {
      return jsonResponse({ error: 'Forbidden' }, 403);
    }

    const target = url.searchParams.get('url');
    if (!target) {
      return jsonResponse({ error: 'Missing url' }, 400);
    }

    const headersParam = url.searchParams.get('headers');
    const customHeaders = parseHeaders(headersParam);

    try {
      const upstream = await fetch(target, {
        headers: {
          ...customHeaders,
          'User-Agent': customHeaders['User-Agent'] || 'Mozilla/5.0'
        },
        redirect: 'follow'
      });

      if (!upstream.ok) {
        return jsonResponse({ error: 'Upstream error', status: upstream.status }, 502);
      }

      const html = await upstream.text();
      const result = extractStreamUrl(html, target);

      if (!result.streamUrl) {
        return jsonResponse({ error: 'No stream found' }, 404);
      }

      return jsonResponse(result, 200, { 'Access-Control-Allow-Origin': '*' });
    } catch (e: any) {
      return jsonResponse({ error: e.message || 'Upstream error' }, 502);
    }
  }
};

function parseHeaders(param: string | null): Record<string, string> {
  if (!param) return {};
  try {
    return JSON.parse(decodeURIComponent(param));
  } catch {
    return {};
  }
}

function extractStreamUrl(html: string, baseUrl: string): StreamResult {
  const lower = html.toLowerCase();
  const base = baseUrl;

  if (lower.includes('cda.pl') || base.includes('cda.pl')) {
    const match = html.match(/data-video-id=["']([^"']+)["']/i);
    if (match && match[1]) {
      const decoded = cdaDecode(match[1].trim());
      if (decoded.startsWith('http')) {
        return { streamUrl: decoded, headers: { Referer: 'https://cda.pl/' } };
      }
    }
  }

  const directMatch = html.match(
    /<(?:video|source)[^>]+src=["']([^"']+\.(?:mp4|m3u8|mpd|webm|mkv|avi|flv|ts))(?:\?[^"']*)?["']/i
  );
  if (directMatch && directMatch[1]) {
    return { streamUrl: resolveUrl(base, directMatch[1]), headers: {} };
  }

  if (html.includes('eval(function(p,a,c,k,')) {
    const unpacked = unpackPacker(html);
    const fromUnpacked = findMediaUrl(unpacked);
    if (fromUnpacked) {
      return { streamUrl: resolveUrl(base, fromUnpacked), headers: {} };
    }
  }

  const fromHtml = findMediaUrl(html);
  if (fromHtml) {
    return { streamUrl: resolveUrl(base, fromHtml), headers: {} };
  }

  return { streamUrl: '', headers: {} };
}

function findMediaUrl(text: string): string | null {
  const m = MEDIA_RE.exec(text);
  if (m) return m[1];
  const m2 = URL_RE.exec(text);
  return m2 ? m2[0] : null;
}

function resolveUrl(base: string, path: string): string {
  if (path.startsWith('http')) return path;
  if (path.startsWith('/')) return base.replace(/\/+$/, '') + path;
  return base.replace(/\/+$/, '') + '/' + path;
}

function unpackPacker(packedJs: string): string {
  const regex =
    /eval\s*\(\s*function\s*\(\s*p\s*,\s*a\s*,\s*c\s*,\s*k\s*,\s*e\s*,\s*[^)]+\s*\)\s*\{[\s\S]*?while\s*\([^)]*\)[\s\S]*?return\s+p[\s\S]*?\}\s*\(\s*(['"])([^'"]*)\1\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*(['"])([^'"]*)\5\.split\(['"]\|['"]\)\s*,\s*\d+\s*,\s*\{\}\s*\)\s*\)/;
  const match = packedJs.match(regex);
  if (!match) return packedJs;

  const packed = match[2];
  const radix = parseInt(match[3], 10);
  const count = parseInt(match[4], 10);
  const dict = match[6].split('|');

  return unpack(packed, radix, count, dict);
}

function unpack(packed: string, radix: number, count: number, keywords: string[]): string {
  if (radix < 2 || radix > 62) return packed;
  let result = packed;
  for (let i = count - 1; i >= 0; i--) {
    const keyword = keywords[i];
    if (!keyword) continue;
    const token = intToToken(i, radix);
    const re = new RegExp(`\\b${escapeRegex(token)}\\b`, 'g');
    result = result.replace(re, keyword);
  }
  return result;
}

function escapeRegex(s: string): string {
  return s.replace(/[-\/\\^$*+?.()|[\]{}]/g, '\\$&');
}

function jsonResponse(body: unknown, status = 200, extraHeaders: Record<string, string> = {}): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      'Content-Type': 'application/json',
      ...extraHeaders
    }
  });
}
