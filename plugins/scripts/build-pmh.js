#!/usr/bin/env node
/**
 * Build PMH-compatible QuickJS plugins.
 * Reads src/shared.js + src/plugin-template.js + src/providers.js
 * and writes:
 *   - <provider>.js  (top-level search/byId/resolve + module.exports for Node tests)
 *   - manifest.json    (PluginManifest with embedded script strings)
 */

const fs = require('fs');
const path = require('path');

const rootDir = path.join(__dirname, '..');
const srcDir = path.join(rootDir, 'src');
const shared = fs.readFileSync(path.join(srcDir, 'shared.js'), 'utf8');
const template = fs.readFileSync(path.join(srcDir, 'plugin-template.js'), 'utf8');
const providers = require(path.join(srcDir, 'providers.js'));

function generateProviderScript(provider, includeModuleExport) {
  const vars = {
    BASE: provider.base,
    PROVIDER: provider.provider,
    TAG: provider.provider,
    NAME: provider.name,
    SEARCH_URL: provider.searchUrl,
    HOST_REGEX: provider.hostRegex,
    SERIES_PATHS_RE: provider.seriesPathsRe,
    DATA_SRC: String(provider.dataSrc),
    IFRAME_DATA_SRC: String(provider.iframeDataSrc),
    CANDIDATE_FILTER: provider.candidateFilter,
    TYPE_FROM_HREF: provider.typeFromHref,
    EPISODE_EXTRA: provider.episodeExtra
  };

  let code = template;
  for (const [key, value] of Object.entries(vars)) {
    code = code.split('{{' + key + '}}').join(value);
  }

  let out = shared + '\n' + code;
  if (includeModuleExport) {
    out += `\nif (typeof module !== 'undefined' && module.exports) {
  module.exports.search = search;
  module.exports.byId = byId;
  module.exports.resolve = resolve;
}\n`;
  }
  return out;
}

const manifestSources = [];

for (const provider of providers) {
  const script = generateProviderScript(provider, false);
  const scriptWithExport = generateProviderScript(provider, true);

  fs.writeFileSync(path.join(rootDir, provider.provider + '.js'), scriptWithExport, 'utf8');

  manifestSources.push({
    type: 'quickjs',
    id: provider.id,
    name: provider.name,
    enabled: true,
    config: { script: script }
  });

  console.log('Built', provider.provider + '.js');
}

const manifest = {
  id: 'pmh-polish-scrapers',
  name: 'Polish Media Hub — Polish Scraper Plugins',
  version: '3.0.0',
  description: 'QuickJS scrapers for filman.cc, ekino-tv.pl, zaluknij.cc, desu-online.pl, animezone.pl, ogladajanime.pl and naszeanime.pl. Synchronous httpFetch-based, no async/await, no cheerio.',
  sources: manifestSources
};

fs.writeFileSync(path.join(rootDir, 'manifest.json'), JSON.stringify(manifest, null, 2), 'utf8');
console.log('Built manifest.json');
