// Build a curated RSS artifact and code-native covers. No platform API or posting.
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const pack = path.join(root, 'growth/social-channels-2026-10-14/pinterest');
const catalog = JSON.parse(fs.readFileSync(path.join(pack, 'catalog.json'), 'utf8'));
const statePath = path.join(pack, 'publication-state.json');
const state = JSON.parse(fs.readFileSync(statePath, 'utf8'));
const args = new Set(process.argv.slice(2));
const xml = value => String(value).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&apos;'}[c]));
const assert = (value, message) => { if (!value) throw new Error(message); };
const readImage = relative => fs.readFileSync(path.join(root, relative));
const imageUri = relative => 'data:image/' + (relative.endsWith('.webp') ? 'webp' : relative.endsWith('.png') ? 'png' : 'jpeg') + ';base64,' + readImage(relative).toString('base64');
const outputPath = item => path.join(root, 'docs/images/pinterest', item.slug + '.jpg');

function verifyExpansion() {
  const proof = state.pilotVerification;
  assert(/^https:\/\/(www\.)?pinterest\.com\/pin\/\d+\/?$/.test(proof.pinUrl || ''), 'Save the real verified pilot Pin URL first');
  assert(proof.checkedAt && Number.isFinite(Date.parse(proof.checkedAt)), 'Save pilot verification time');
  for (const key of ['imageMatchesEnclosure','destinationWorksAndRetainsUtm','duplicateCheckPassed']) {
    assert(proof[key] === true, 'Pilot verification required: ' + key);
  }
}

function validateCatalog() {
  assert(catalog.items.length === 6, 'Only six curated useful pages are in this batch');
  for (const field of ['slug','page','guid','image']) assert(new Set(catalog.items.map(item => item[field])).size === 6, 'Duplicate ' + field);
  assert(state.activeSlugs.length && new Set(state.activeSlugs).size === state.activeSlugs.length, 'Invalid activeSlugs');
  assert(state.activeSlugs.every(slug => catalog.items.some(item => item.slug === slug)), 'Unknown active item');
  if (state.stage === 'pilot') assert(JSON.stringify(state.activeSlugs) === '["tool-accounting-templates"]', 'Pilot must contain one initial item');
  else { assert(state.stage === 'expanded', 'Unknown publication stage'); verifyExpansion(); }
  for (const item of catalog.items) {
    assert(/^[a-z0-9-]+$/.test(item.slug) && /^[a-z0-9-]+\.html$/.test(item.page), 'Unsafe path');
    assert(item.guid === catalog.site + '/' + item.page, 'Stable canonical GUID required');
    const link = new URL(item.link);
    assert(link.origin === catalog.site && link.pathname === '/' + item.page && !link.hash, 'Wrong destination');
    assert(link.searchParams.get('utm_source') === 'pinterest' && link.searchParams.get('utm_medium') === 'organic_social' && link.searchParams.get('utm_campaign') === 'evergreen' && link.searchParams.get('utm_content') === item.slug && [...link.searchParams].length === 4, 'Wrong UTM');
    assert(item.image === catalog.site + '/images/pinterest/' + item.slug + '.jpg', 'Wrong image URL');
    assert(fs.existsSync(path.join(root, 'docs', item.page)), 'Missing article');
    assert(item.title.trim() && item.description.trim() && Number.isFinite(Date.parse(item.feedPublishedAt)), 'Incomplete item');
    assert(crypto.createHash('sha256').update(readImage(item.cover.source)).digest('hex') === item.cover.sourceSha256, 'Original illustration changed: ' + item.slug);
  }
}

function rssText() {
  const active = catalog.items.filter(item => state.activeSlugs.includes(item.slug));
  const latest = Math.max(...active.map(item => Date.parse(item.feedPublishedAt)));
  return '<?xml version="1.0" encoding="UTF-8"?>\n<rss version="2.0">\n<channel>\n' +
    '<title>Каптёрка PRO — инструкции и шаблоны учёта</title>\n<link>' + catalog.site + '/guides.html</link>\n' +
    '<description>Полезные публичные инструкции, шаблоны и советы для учёта инструмента и имущества.</description>\n<language>ru</language>\n' +
    '<lastBuildDate>' + new Date(latest).toUTCString() + '</lastBuildDate>\n' + active.map(item => {
      const length = fs.statSync(outputPath(item)).size;
      return '<item>\n<title>' + xml(item.title) + '</title>\n<description>' + xml(item.description) + '</description>\n' +
        '<link>' + xml(item.link) + '</link>\n<guid isPermaLink="true">' + xml(item.guid) + '</guid>\n' +
        '<pubDate>' + new Date(item.feedPublishedAt).toUTCString() + '</pubDate>\n' +
        '<enclosure url="' + xml(item.image) + '" length="' + length + '" type="image/jpeg" />\n</item>\n';
    }).join('') + '</channel>\n</rss>\n';
}

function coverHtml(item) {
  const c = item.cover;
  const colors = {mint:['#d6ffeb','#00765d'],gold:['#fff0b7','#ac4b08'],blue:['#e0f3ff','#0567bd']};
  const [bg, accent] = colors[c.theme];
  const crop = c.crop;
  let imgCss = 'width:100%;height:100%;object-fit:contain';
  if (crop) {
    const scale = Math.min(880 / crop.width, 580 / crop.height);
    imgCss = `position:absolute;max-width:none;width:${c.layout === 'poster' ? 1080 * scale : 1080 * scale}px;left:${(880-crop.width*scale)/2-crop.left*scale}px;top:${(580-crop.height*scale)/2-crop.top*scale}px`;
  }
  return `<!doctype html><html lang="ru"><meta charset="utf-8"><title>${xml(item.title)}</title><style>
    *{box-sizing:border-box}html,body{margin:0;width:1000px;height:1500px;overflow:hidden}body{font-family:Arial,sans-serif;background:radial-gradient(circle at 95% 0%,#ffffff 0,transparent 43%),${bg};color:#102f2b}
    .brand{position:absolute;top:52px;left:60px;display:flex;align-items:center;gap:18px;font-size:30px;font-weight:700;letter-spacing:2px}.brand img{width:60px;height:60px;border-radius:14px}
    .rule{position:absolute;left:60px;top:133px;width:140px;height:8px;background:${accent}}
    h1{position:absolute;left:60px;top:164px;margin:0;font-size:76px;line-height:1.06;letter-spacing:-2px;max-width:880px}
    .sub{position:absolute;top:440px;left:60px;width:880px;font-size:34px;line-height:1.2;color:${accent};font-weight:600}
    .art{position:absolute;left:60px;top:580px;width:880px;height:580px;overflow:hidden;border:3px solid #ffffff;border-radius:26px;background:#0d1817;box-shadow:0 18px 42px #102f2b24}
    .art img{${imgCss}}.caption{position:absolute;top:1179px;left:60px;font-size:30px;color:#3b534a}
    .benefit{position:absolute;left:60px;top:1240px;width:880px;font-size:31px;line-height:1.22;font-weight:600}
    .cta{position:absolute;left:60px;top:1340px;width:880px;color:#fff;background:${accent};border-radius:18px;padding:22px 24px;font-weight:700;font-size:28px;text-align:center}
    .url{position:absolute;left:60px;bottom:28px;font-size:23px;font-weight:700;letter-spacing:1px}
    </style><body><div class="brand"><img src="${imageUri('docs/app_icon_512.png')}" alt="">КАПТЁРКА PRO</div><div class="rule"></div>
    <h1>${c.title.map(xml).join('<br>')}</h1><div class="sub">${xml(c.subtitle)}</div><div class="art"><img src="${imageUri(c.source)}" alt="${xml(c.caption)}"></div>
    <div class="caption">${xml(c.caption)}</div><div class="benefit">${xml(c.benefit)}</div><div class="cta">${xml(c.cta)}</div><div class="url">kapterka-pro.ru</div></body></html>`;
}

async function renderCovers() {
  const {chromium} = await import('playwright');
  const browser = await chromium.launch({headless:true, executablePath:process.env.CHROME_BIN || undefined,
    args:['--disable-gpu','--renderer-process-limit=1']});
  const page = await browser.newPage({viewport:{width:1000,height:1500},deviceScaleFactor:1});
  const qa = [];
  try {
    fs.mkdirSync(path.join(root,'docs/images/pinterest'),{recursive:true});
    for (const item of catalog.items) {
      await page.setContent(coverHtml(item));
      await page.evaluate(async () => { await document.fonts.ready; await Promise.all([...document.images].map(i => i.decode())); });
      const bounds = await page.evaluate(() => [...document.querySelectorAll('h1,.sub,.caption,.benefit,.cta,.url')].map(e=>{
        const r=e.getBoundingClientRect();return {text:e.textContent,left:r.left,right:r.right,top:r.top,bottom:r.bottom,scrollWidth:e.scrollWidth,clientWidth:e.clientWidth};
      }));
      assert(bounds.every(r=>r.left>=0 && r.right<=1000 && r.top>=0 && r.bottom<=1500 && r.scrollWidth<=r.clientWidth), 'Text clips on ' + item.slug);
      assert(bounds[0].bottom < bounds[1].top && bounds[1].bottom < 560 && bounds[3].bottom < bounds[4].top && bounds[4].bottom < bounds[5].top, 'Text overlaps on ' + item.slug);
      await page.screenshot({path:outputPath(item),type:'jpeg',quality:90});
      qa.push({slug:item.slug,width:1000,height:1500,bytes:fs.statSync(outputPath(item)).size,textBounds:bounds});
    }
    const gallery = '<!doctype html><meta charset="utf-8"><title>Шесть обложек Pinterest — подготовлено</title><style>html,body{margin:0}body{display:grid;grid-template-columns:repeat(3,400px);width:1200px}img{display:block;width:400px;height:600px}</style>' + catalog.items.map(item=>'<img alt="'+xml(item.title)+'" src="'+imageUri('docs/images/pinterest/'+item.slug+'.jpg')+'">').join('');
    await page.setViewportSize({width:1200,height:1200});
    await page.setContent(gallery);
    await page.evaluate(async()=>Promise.all([...document.images].map(i=>i.decode())));
    await page.screenshot({path:path.join(pack,'covers-preview.jpg'),type:'jpeg',quality:90});
    fs.writeFileSync(path.join(pack,'cover-qa.json'),JSON.stringify({rendered:true,generatedWith:'HTML/CSS, existing public illustrations and real educational interface frames',covers:qa},null,2)+'\n');
  } finally {await browser.close();}
}

if (args.has('--activate-rest')) {
  verifyExpansion();
  state.stage = 'expanded';
  state.activeSlugs = catalog.items.map(item => item.slug);
  fs.writeFileSync(statePath,JSON.stringify(state,null,2)+'\n');
}
validateCatalog();
if (args.has('--covers')) await renderCovers();
const expected = rssText();
const feedPath = path.join(root,'docs/pinterest.xml');
if (args.has('--write-feed')) fs.writeFileSync(feedPath,expected);
else if (args.has('--check')) assert(fs.readFileSync(feedPath,'utf8').replace(/\r\n/g,'\n')===expected,'RSS differs from curated catalog/state; regenerate after review');
else if (!args.has('--covers') && !args.has('--activate-rest')) console.log('Use --covers --write-feed, --write-feed, --check, or --activate-rest --write-feed after actual pilot verification.');
console.log('PASS: curated RSS state: '+state.stage+', '+state.activeSlugs.length+' active material(s). No platform actions.');
