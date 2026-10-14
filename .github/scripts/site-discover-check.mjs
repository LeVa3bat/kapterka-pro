// Verify the concrete preparation for three existing guides. This is a quality
// check; passing it does not imply indexing, Discover impressions, or traffic.
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import {createRequire} from 'node:module';
const require = createRequire(import.meta.url);
const selected = [
  ['uchet-instrumenta-android.html','tool-accounting'],
  ['inventarizaciya-na-android.html','inventory-reconcile'],
  ['peremeshchenie-mezhdu-skladami-android.html','stock-transfer']
];
const origin = 'https://kapterka-pro.ru';
const results = [];
const attributes = tag => Object.fromEntries([...tag.matchAll(/([\w:-]+)="([^"]*)"/g)].map(([,key,value])=>[key,value]));
const webpSize = buffer => {
  assert.equal(buffer.toString('ascii',0,4),'RIFF','Not a RIFF image');
  assert.equal(buffer.toString('ascii',8,12),'WEBP','Not a WebP image');
  for (let offset=12; offset+8<=buffer.length;) {
    const type=buffer.toString('ascii',offset,offset+4), length=buffer.readUInt32LE(offset+4), start=offset+8;
    assert.ok(start+length<=buffer.length,'Truncated WebP chunk');
    if (type==='VP8X') return [buffer.readUIntLE(start+4,3)+1,buffer.readUIntLE(start+7,3)+1];
    if (type==='VP8 ') {
      assert.equal(buffer.toString('hex',start+3,start+6),'9d012a','Invalid VP8 frame');
      return [buffer.readUInt16LE(start+6)&0x3fff,buffer.readUInt16LE(start+8)&0x3fff];
    }
    if (type==='VP8L') {
      assert.equal(buffer[start],0x2f,'Invalid VP8L frame');
      const bits=buffer.readUInt32LE(start+1);
      return [(bits&0x3fff)+1,((bits>>>14)&0x3fff)+1];
    }
    offset=start+length+(length%2);
  }
  throw new Error('No WebP dimensions found');
};

for (const [file,stem] of selected) {
  const html=fs.readFileSync(path.join('docs',file),'utf8');
  const metas=[...html.matchAll(/<meta\b[^>]*>/g)].map(([tag])=>attributes(tag));
  const meta = key => {
    const found=metas.filter(a=>(a.property||a.name)===key);
    assert.equal(found.length,1,`${file}: expected one ${key}`);
    return found[0].content;
  };
  assert.match(meta('robots'),/max-image-preview:large/,`${file}: large image preview missing`);
  assert.doesNotMatch(meta('robots'),/noindex|noimageindex|nosnippet/,`${file}: restrictive robots`);
  const imageUrl=`${origin}/images/discover/${stem}-1200.webp`;
  assert.equal(meta('og:image'),imageUrl,`${file}: representative og:image`);
  assert.equal(meta('og:image:type'),'image/webp');
  assert.equal(meta('og:image:width'),'1200');
  assert.equal(meta('og:image:height'),'675');
  assert.ok(meta('og:image:alt').length>20,`${file}: descriptive preview alt missing`);
  assert.equal(meta('twitter:card'),'summary_large_image');
  assert.equal(meta('twitter:image'),imageUrl);
  assert.equal(meta('twitter:image:alt'),meta('og:image:alt'));
  const canonical=[...html.matchAll(/<link\b[^>]*>/g)].map(([tag])=>attributes(tag)).find(a=>a.rel==='canonical');
  assert.equal(canonical?.href,`${origin}/${file}`);
  const jsonLd=[...html.matchAll(/<script type="application\/ld\+json">\s*([\s\S]*?)\s*<\/script>/g)].map(([,json])=>JSON.parse(json));
  const article=jsonLd.find(value=>value['@type']==='Article');
  assert.ok(article,`${file}: Article schema missing`);
  assert.deepEqual(article.image,{'@type':'ImageObject',url:imageUrl,width:1200,height:675});
  const sitemap=fs.readFileSync('docs/sitemap.xml','utf8');
  const entry=[...sitemap.matchAll(/<url>([\s\S]*?)<\/url>/g)].map(([,xml])=>xml).find(xml=>xml.includes(`<loc>${origin}/${file}</loc>`));
  assert.ok(entry,`${file}: sitemap URL missing`);
  assert.equal(/<lastmod>([^<]+)<\/lastmod>/.exec(entry)?.[1],article.dateModified,`${file}: sitemap/schema date mismatch`);
  const illustration=/<figure class="guide-illustration"[^>]*>([\s\S]*?)<\/figure>/.exec(html)?.[1];
  assert.ok(illustration,`${file}: illustration is not visible in article`);
  const image=attributes(/<img\b[^>]*>/.exec(illustration)?.[0]||'');
  assert.equal(image.src,`images/discover/${stem}-1200.webp`);
  assert.equal(image.srcset,`images/discover/${stem}-600.webp 600w, images/discover/${stem}-1200.webp 1200w`);
  assert.ok(image.sizes,`${file}: responsive sizes missing`);
  assert.equal(image.width,'1200'); assert.equal(image.height,'675');
  assert.equal(image.loading,'lazy'); assert.equal(image.decoding,'async');
  assert.equal(image.alt,meta('og:image:alt'));
  assert.match(illustration,/не экран приложения/,`${file}: distinguish illustration from app UI`);
  const assets=[];
  for (const width of [1200,600]) {
    const name=`images/discover/${stem}-${width}.webp`, buffer=fs.readFileSync(path.join('docs',name));
    const dimensions=webpSize(buffer);
    assert.deepEqual(dimensions,[width,Math.round(width*9/16)],`${name}: incorrect dimensions`);
    assert.ok(buffer.length<=(width===1200?40000:20000),`${name}: exceeds ${width===1200?40:20} KB image budget`);
    if(width===1200) assert.ok(dimensions[0]*dimensions[1]>300000,'Full image below Discover resolution recommendation');
    assets.push({path:name,width:dimensions[0],height:dimensions[1],bytes:buffer.length});
  }
  results.push({page:`${origin}/${file}`,assets});
}
console.log('PASS: three guides have relevant 1200 × 675 previews, robots/schema/meta consistency, responsive visible illustrations, stable dates and small image budgets.');

if (process.argv.includes('--browser')) {
  const {chromium}=require(process.env.PLAYWRIGHT_MODULE_PATH||'playwright');
  const base=process.env.PAGE_BASE_URL||'http://127.0.0.1:4173';
  const evidence=path.resolve(process.env.QA_OUTPUT||'growth/qa/discover');
  fs.mkdirSync(evidence,{recursive:true});
  const browser=await chromium.launch({headless:true,executablePath:process.env.CHROME_BIN||(process.platform==='win32'?'C:/Program Files/Google/Chrome/Application/chrome.exe':'/usr/bin/google-chrome')});
  try {
    for (const [label,viewport] of [['mobile-360',{width:360,height:800}],['mobile-412',{width:412,height:915}],['desktop',{width:1280,height:800}]]) {
      const context=await browser.newContext({viewport,deviceScaleFactor:1});
      await context.route('**/*',route=>new URL(route.request().url()).origin===new URL(base).origin?route.continue():route.abort());
      const page=await context.newPage(), errors=[];
      page.on('pageerror',error=>errors.push(error.message));
      for (const [file,stem] of selected) {
        const response=await page.goto(`${base}/${file}`);
        assert.equal(response.status(),200,`${label}: page HTTP status`);
        const figure=page.locator('.guide-illustration'), image=figure.locator('img');
        await figure.scrollIntoViewIfNeeded();
        await image.evaluate(el=>el.decode());
        const layout=await image.evaluate(el=>({src:el.currentSrc,naturalWidth:el.naturalWidth,naturalHeight:el.naturalHeight,displayWidth:el.getBoundingClientRect().width,overflow:document.documentElement.scrollWidth>innerWidth+1}));
        assert.equal(layout.overflow,false,`${label}: horizontal overflow on ${file}`);
        const expectedWidth=label==='desktop'?1200:600;
        assert.equal(new URL(layout.src).pathname,`/images/discover/${stem}-${expectedWidth}.webp`,`${label}: unexpected image source on ${file}`);
        assert.ok(layout.displayWidth>0&&layout.displayWidth<=viewport.width,`${label}: invalid rendered width`);
        // With width descriptors, naturalWidth is density-corrected by the
        // browser. currentSrc identifies the actual 600/1200 px file above.
        assert.ok(Math.abs(layout.naturalWidth/layout.naturalHeight-16/9)<.015,`${label}: rendered image ratio`);
        for (const width of [600,1200]) {
          const asset=await context.request.get(`${base}/images/discover/${stem}-${width}.webp`);
          assert.equal(asset.status(),200,`${file}: image HTTP status`);
          assert.match(asset.headers()['content-type'],/^image\/webp/);
        }
        await figure.screenshot({path:path.join(evidence,`${label}-${stem}.png`)});
        const selectedBytes=fs.statSync(path.join('docs',new URL(layout.src).pathname.slice(1))).size;
        const item=results.find(result=>result.page===`${origin}/${file}`);
        (item.browser ||= []).push({viewport:label,...layout,selectedAssetWidth:expectedWidth,selectedImageBytes:selectedBytes});
      }
      assert.deepEqual(errors,[],`${label}: browser errors`);
      await context.close();
    }
  } finally {await browser.close();}
  console.log('PASS: 360/412px mobile and desktop, decoded responsive image selection, no horizontal overflow or browser errors, image HTTP 200 and MIME.');
}
if(process.env.DISCOVER_REPORT) {
  fs.mkdirSync(path.dirname(process.env.DISCOVER_REPORT),{recursive:true});
  fs.writeFileSync(process.env.DISCOVER_REPORT,JSON.stringify({checkedAt:new Date().toISOString(),results},null,2)+'\n');
}
