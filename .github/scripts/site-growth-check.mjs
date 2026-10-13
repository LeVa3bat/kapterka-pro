import fs from 'node:fs';
import path from 'node:path';
const read = name=>fs.readFileSync(path.join('docs',name),'utf8');
const sitemap = read('sitemap.xml');
if (!read('robots.txt').includes('Sitemap: https://kapterka-pro.ru/sitemap.xml')) throw new Error('robots sitemap missing');
const urls = [...sitemap.matchAll(/<url>\s*<loc>([^<]+)<\/loc>\s*<lastmod>([^<]+)<\/lastmod>/g)];
const seen = new Set();
for (const [,url,lastmod] of urls) {
  if (seen.has(url)) throw new Error('Duplicate sitemap URL: '+url); seen.add(url);
  if (!url.startsWith('https://kapterka-pro.ru/') || !/^\d{4}-\d{2}-\d{2}$/.test(lastmod)) throw new Error('Invalid sitemap metadata');
  const file = new URL(url).pathname.slice(1) || 'index.html', source = read(file);
  const canonical = /<link\b[^>]*rel="canonical"[^>]*href="([^"]+)"/.exec(source)?.[1];
  if (canonical !== url) throw new Error('Canonical mismatch: '+file);
  const modified = /"dateModified":\s*"([^"]+)"/.exec(source)?.[1];
  if (modified && modified !== lastmod) throw new Error('Sitemap/dateModified mismatch: '+file+' ('+lastmod+' / '+modified+')');
}
for (const file of fs.readdirSync('docs').filter(name=>name.endsWith('.html'))) {
  for (const [,ref] of read(file).matchAll(/(?:href|src)=["']([^"']+)["']/g)) {
    if (/^(https?:|mailto:|tel:|data:|javascript:|#)/.test(ref)) continue;
    const target = ref.split(/[?#]/)[0]; if (!target) continue;
    const full = path.resolve('docs', target === '/' ? 'index.html' : target.replace(/^\//,''));
    if (!full.startsWith(path.resolve('docs')+path.sep) || !fs.existsSync(full)) throw new Error('Broken public reference: '+file+' -> '+ref);
  }
}
for (const file of ['workshop-blank.xlsx','workshop-example.xlsx','workshop-blank.pdf','workshop-example.pdf','inventory-checklist.pdf']) {
  const data = fs.readFileSync('docs/downloads/'+file);
  if (!data.subarray(0,4).equals(Buffer.from(file.endsWith('.pdf')?'%PDF':'PK\x03\x04'))) throw new Error('Invalid download: '+file);
}
const index = read('index.html');
for (const src of ['app.js','auth-config.js','auth-v2.js']) if (!new RegExp('<script[^>]*src="'+src.replace('.','\\.')+'[^>]*defer').test(index)) throw new Error('Blocking script: '+src);
if (fs.statSync('docs/site.bundle.min.css').size > 170000) throw new Error('Homepage CSS above 170 KB budget');
for (const file of fs.readdirSync('docs/screens').filter(name=>name.endsWith('-preview.webp'))) if (fs.statSync('docs/screens/'+file).size > 60000) throw new Error('Gallery preview above 60 KB budget: '+file);
console.log('PASS: sitemap/robots/canonical/dates, public links, download types, performance resource budgets.');
