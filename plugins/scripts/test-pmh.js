#!/usr/bin/env node
/**
 * Smoke tests for the generated PMH QuickJS plugins.
 * Uses a synchronous mock for httpFetch so the plugin code can be exercised
 * without a real QuickJS / Android runtime.
 */

const fs = require('fs');
const path = require('path');
const { spawnSync } = require('child_process');

const rootDir = path.join(__dirname, '..');

const sampleSearchHtml = '<html><body>' +
  '<a href="/film/test-2020" title="Test Title">Test Title</a>' +
  '<a href="/anime/test" title="Test Anime">Test Anime</a>' +
  '</body></html>';

const base64Player = Buffer.from('https://example.com/player').toString('base64');
const sampleDetailHtml = '<html><body>' +
  '<h1>Test Title</h1>' +
  '<div data-iframe="' + base64Player + '">1080p lektor</div>' +
  '<iframe src="https://cda.pl/video/123"></iframe>' +
  '<a href="https://streamtape.com/e/abc">streamtape</a>' +
  '</body></html>';

function mockHttpFetch(url, headers) {
  var u = url.toLowerCase();
  if (u.indexOf('phrase=') >= 0 || u.indexOf('?s=') >= 0 || u.indexOf('search?value=') >= 0 || u.indexOf('anime?search=') >= 0 || u.indexOf('/search/qf/?q=') >= 0) {
    return JSON.stringify({ code: 200, body: sampleSearchHtml, headers: {} });
  }
  if (u.indexOf('/film/test-2020') >= 0 || u.indexOf('/anime/test') >= 0) {
    return JSON.stringify({ code: 200, body: sampleDetailHtml, headers: {} });
  }
  return JSON.stringify({ code: 404, body: '', headers: {}, error: 'Not found in mock' });
}

globalThis.httpFetch = mockHttpFetch;

const files = fs.readdirSync(rootDir).filter(f => f.endsWith('.js') && fs.statSync(path.join(rootDir, f)).isFile());
let failed = 0;
let tested = 0;

for (const file of files) {
  if (['scripts', 'node_modules', 'test', 'dist'].some(d => file.indexOf(d) === 0)) continue;
  const p = path.join(rootDir, file);
  // syntax check
  const syntax = spawnSync('node', ['-c', p], { encoding: 'utf8' });
  if (syntax.status !== 0) {
    console.error(file + ': SYNTAX ERROR');
    console.error(syntax.stderr);
    failed++;
    continue;
  }

  try {
    const mod = require(p);
    tested++;
    if (typeof mod.search !== 'function') throw new Error('search is missing');
    if (typeof mod.byId !== 'function') throw new Error('byId is missing');
    if (typeof mod.resolve !== 'function') throw new Error('resolve is missing');

    const results = mod.search('test');
    if (!Array.isArray(results)) throw new Error('search did not return array');
    if (results.length === 0) {
      console.log(file + ': search returned no results (may be expected for this provider)');
      continue;
    }

    const firstId = results[0].id;
    const details = mod.byId(firstId);
    if (!details || typeof details !== 'object') throw new Error('byId did not return object');

    const stream = mod.resolve(firstId);
    if (!stream || typeof stream !== 'object' || !stream.url) throw new Error('resolve did not return stream url');

    console.log(file + ': OK (' + results.length + ' results, stream=' + stream.url + ')');
  } catch (e) {
    console.error(file + ': RUNTIME ERROR -', e.message);
    failed++;
  }

  delete require.cache[require.resolve(p)];
}

// Validate manifest
const manifestPath = path.join(rootDir, 'manifest.json');
if (fs.existsSync(manifestPath)) {
  try {
    const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
    if (!manifest.id || !Array.isArray(manifest.sources)) throw new Error('invalid manifest');
    manifest.sources.forEach(s => {
      if (s.type !== 'quickjs') throw new Error('source ' + s.id + ' is not quickjs');
      if (typeof s.config.script !== 'string' || s.config.script.length < 100) throw new Error('source ' + s.id + ' missing script');
    });
    console.log('manifest.json: OK (' + manifest.sources.length + ' sources)');
  } catch (e) {
    console.error('manifest.json: ERROR -', e.message);
    failed++;
  }
} else {
  console.error('manifest.json: missing');
  failed++;
}

if (failed > 0) {
  process.exitCode = 1;
}
console.log('\n' + (failed === 0 ? 'All PMH plugin tests passed.' : failed + ' failure(s).'));
