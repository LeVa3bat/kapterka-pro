// Branded promotional artwork, not app screenshots. The existing official icon
// is embedded from its unchanged source and scaled proportionally, never redrawn.
// Optional authoring dependency only: npm install --no-save --package-lock=false sharp@0.35.5
// Run: node tools/growth/build-catalog-art.mjs
// Or set SHARP_MODULE_PATH to an existing sharp module. CI does not require sharp.
import fs from 'node:fs/promises';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {createRequire} from 'node:module';
const require = createRequire(import.meta.url);
const sharp = require(process.env.SHARP_MODULE_PATH || 'sharp');
const output = path.resolve('growth/promotion-2026-10-14/catalogs/assets');
const originalIcon = await fs.readFile('docs/app_icon_512.png');
const iconSha256 = createHash('sha256').update(originalIcon).digest('hex');
const expectedIcon = '3b52892775ce1d48e13d60da7a01bf64a24c30f68136ab242e1d60c3c5da8489';
if(iconSha256 !== expectedIcon) throw new Error('Official icon source changed; review the current icon before regenerating artwork');
const sourceIcon = 'data:image/png;base64,' + originalIcon.toString('base64');
await fs.mkdir(output,{recursive:true});

function scene(width,height) {
  const large = height > 500;
  const margin = large ? 76 : 62;
  const brandY = large ? 106 : 83;
  const titleY = large ? 254 : 198;
  const titleSize = large ? 64 : 53;
  const leading = large ? 78 : 64;
  const introY = titleY + leading + (large ? 64 : 50);
  const offlineY = introY + (large ? 52 : 43);
  const iconSize = large ? 326 : 266;
  const iconX = large ? 778 : 687;
  const iconY = large ? 166 : 113;
  const cx = iconX+iconSize/2, cy = iconY+iconSize/2;
  return `<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">
  <defs>
    <linearGradient id="backdrop" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#0a1610"/><stop offset="1" stop-color="#10261a"/></linearGradient>
    <radialGradient id="iconGlow"><stop stop-color="#3c724e" stop-opacity=".72"/><stop offset=".66" stop-color="#284c33" stop-opacity=".24"/><stop offset="1" stop-color="#0b1a11" stop-opacity="0"/></radialGradient>
    <linearGradient id="divider" x1="0" y1="0" x2="0" y2="1"><stop stop-color="#80c79d" stop-opacity="0"/><stop offset=".5" stop-color="#80c79d" stop-opacity=".32"/><stop offset="1" stop-color="#80c79d" stop-opacity="0"/></linearGradient>
    <linearGradient id="rim" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#77b18c" stop-opacity=".7"/><stop offset=".46" stop-color="#77b18c" stop-opacity=".08"/><stop offset="1" stop-color="#77b18c" stop-opacity=".45"/></linearGradient>
  </defs>
  <rect width="${width}" height="${height}" fill="url(#backdrop)"/>
  <path d="M${width*.615} 0H${width}V${height}H${width*.57}z" fill="#08150e" opacity=".62"/>
  <ellipse cx="${cx}" cy="${cy}" rx="${large?355:280}" ry="${large?325:260}" fill="url(#iconGlow)"/>
  <g stroke="#71a17f" stroke-width="1" opacity=".10">
    ${Array.from({length:8},(_,i)=>`<path d="M${width*.62+i*58} 0V${height}"/>`).join('')}
    ${Array.from({length:10},(_,i)=>`<path d="M${width*.62} ${i*58}H${width}"/>`).join('')}
  </g>
  <path d="M${width*.615} 0L${width*.57} ${height}" stroke="url(#divider)" stroke-width="1"/>
  <rect x="${margin}" y="${brandY-23}" width="5" height="26" rx="2.5" fill="#88d4a7"/>
  <text x="${margin+19}" y="${brandY}" font-family="Arial, sans-serif" font-size="${large?25:23}" font-weight="700" letter-spacing=".6" fill="#d8e8da">Каптёрка <tspan fill="#8dcc9f">PRO</tspan></text>
  <text x="${margin}" y="${titleY}" font-family="Arial, sans-serif" font-size="${titleSize}" font-weight="700" letter-spacing="-1.4" fill="#f0f3e8">Учёт имущества</text>
  <text x="${margin}" y="${titleY+leading}" font-family="Arial, sans-serif" font-size="${titleSize}" font-weight="700" letter-spacing="-1.4" fill="#f0f3e8">на <tspan fill="#91d5a9">Android</tspan></text>
  <text x="${margin+2}" y="${introY}" font-family="Arial, sans-serif" font-size="${large?25:21}" font-weight="400" fill="#adc2b1">Приход · выдача · остатки</text>
  <g transform="translate(${margin+2} ${offlineY})">
    <circle cx="6" cy="-5" r="4" fill="#88d4a7"/>
    <text x="20" y="0" font-family="Arial, sans-serif" font-size="${large?19:16}" font-weight="400" fill="#d0dfd1">Работает офлайн</text>
  </g>
  <rect x="${iconX-17}" y="${iconY-17}" width="${iconSize+34}" height="${iconSize+34}" rx="24" fill="#0b1b12" stroke="url(#rim)" stroke-width="1.2"/>
  <image x="${iconX}" y="${iconY}" width="${iconSize}" height="${iconSize}" xlink:href="${sourceIcon}" preserveAspectRatio="xMidYMid meet"/>
  <path d="M${margin} ${height-60}H${width-margin}" stroke="#7caa89" stroke-opacity=".20" stroke-width="1"/>
  <text x="${margin+2}" y="${height-29}" font-family="Arial, sans-serif" font-size="${large?16:14}" font-weight="400" letter-spacing=".3" fill="#94ad9a">kapterka-pro.ru</text>
  <text x="${width-margin}" y="${height-29}" text-anchor="end" font-family="Arial, sans-serif" font-size="${large?14:12}" font-weight="400" letter-spacing=".9" fill="#94ad9a">СКЛАД · ИНСТРУМЕНТ · ИМУЩЕСТВО</text>
  </svg>`;
}

const assets=[];
for(const [name,width,height,platform] of [['uptodown-featured-ru',1024,500,'Uptodown'],['a2is-promo-ru',1200,675,'A2IS']]) {
  const svg=scene(width,height);
  const svgPath=path.join(output,name+'.svg'), pngPath=path.join(output,name+'.png');
  await fs.writeFile(svgPath,svg);
  await sharp(Buffer.from(svg)).png({compressionLevel:9,effort:10}).toFile(pngPath);
  const bytes=await fs.readFile(pngPath), metadata=await sharp(bytes).metadata();
  if(metadata.width!==width||metadata.height!==height) throw new Error('Artwork dimensions mismatch');
  assets.push({platform,language:'ru',file:path.relative(process.cwd(),pngPath).replaceAll('\\','/'),editableSource:path.relative(process.cwd(),svgPath).replaceAll('\\','/'),width,height,bytes:bytes.length,sha256:createHash('sha256').update(bytes).digest('hex'),classification:'promotional_artwork_not_app_screenshot',publicationStatus:'prepared_not_uploaded'});
  console.log(`${name}.png: ${width} × ${height}, ${bytes.length} bytes`);
}
// The publisher form requires a 200px square icon. Compose the unchanged
// source in SVG so the submitted size follows the existing brand geometry.
const organizationSvg = `<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="200" height="200" viewBox="0 0 200 200"><image width="200" height="200" preserveAspectRatio="xMidYMid meet" xlink:href="${sourceIcon}"/></svg>`;
await fs.writeFile(path.join(output,'uptodown-organization-icon.svg'),organizationSvg);
await sharp(Buffer.from(organizationSvg)).png().toFile(path.join(output,'uptodown-organization-icon.png'));
const organizationIcon = await fs.readFile(path.join(output,'uptodown-organization-icon.png'));
assets.push({platform:'Uptodown',language:null,file:'growth/promotion-2026-10-14/catalogs/assets/uptodown-organization-icon.png',editableSource:'growth/promotion-2026-10-14/catalogs/assets/uptodown-organization-icon.svg',width:200,height:200,bytes:organizationIcon.length,sha256:createHash('sha256').update(organizationIcon).digest('hex'),classification:'publisher_icon_composition_of_existing_official_icon',publicationStatus:'prepared_not_uploaded'});
await fs.writeFile(path.join(output,'art-assets.json'),JSON.stringify({createdOn:'2026-10-14',generator:'tools/growth/build-catalog-art.mjs',authoringDependency:'sharp 0.35.5',originalIcon:{file:'docs/app_icon_512.png',sha256:iconSha256,modified:false,displayTreatment:'proportional scaling within artwork; original file unmodified'},newStockImagesUsed:false,newAiImagesUsed:false,artworkIsNotAppScreenshot:true,copyClaims:['Учёт имущества на Android','Приход · выдача · остатки','Работает офлайн'],notes:['No price, free unlimited access or approval claims are displayed.','Store upload and moderation status must be recorded separately by the submitting agent.'],assets},null,2)+'\n');
