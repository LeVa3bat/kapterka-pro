// Original vector illustrations for three existing guides. These are diagrams,
// not app screenshots. Regenerate with sharp (local install or SHARP_MODULE_PATH).
// Authoring dependency: sharp 0.35.5. Optional local installation (no manifest or
// lockfile changes): npm install --no-save --package-lock=false sharp@0.35.5
// Then run: node tools/growth/build-discover-images.mjs
// CI serves the checked-in WebP files and does not run this generator.
import fs from 'node:fs/promises';
import path from 'node:path';
import {createRequire} from 'node:module';
const require = createRequire(import.meta.url);
const sharp = require(process.env.SHARP_MODULE_PATH || 'sharp');
const output = path.resolve('docs/images/discover');
await fs.mkdir(output, {recursive: true});

const colors = {bg:'#101e18', panel:'#1b3026', line:'#416252', mint:'#8ae0b6', green:'#549c78', paper:'#eceddf', ink:'#253e32', muted:'#a8c1b0', gold:'#ebb86a', orange:'#c88854'};
const rect = (x,y,w,h,fill,rx=0,extra='') => `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="${rx}" fill="${fill}" ${extra}/>`;
const text = (x,y,value,size=24,fill=colors.paper,weight=600) => `<text x="${x}" y="${y}" fill="${fill}" font-family="Arial, sans-serif" font-size="${size}" font-weight="${weight}">${value}</text>`;
const line = (x1,y1,x2,y2,color=colors.line,width=5,extra='') => `<path d="M${x1} ${y1}L${x2} ${y2}" fill="none" stroke="${color}" stroke-width="${width}" stroke-linecap="round" ${extra}/>`;
const arrow = (x,y,w,color=colors.mint) => `<path d="M${x} ${y}h${w}m-22 -22l22 22-22 22" fill="none" stroke="${color}" stroke-width="9" stroke-linecap="round" stroke-linejoin="round"/>`;
const screwdriver = (x,y,s=1,color=colors.gold) => `<g transform="translate(${x} ${y}) scale(${s})">${rect(-5,0,10,87,'#b9ccbe',3)}${rect(-8,0,16,14,'#d5dfd2',3)}${rect(-17,81,34,73,color,13)}${line(-7,96,-7,139,'#9b682f',4)}${line(7,96,7,139,'#9b682f',4)}</g>`;
const glove = (x,y,s=1) => `<g transform="translate(${x} ${y}) scale(${s})"><path d="M-26 21v-42q0-12 9-12t9 12v28-48q0-12 9-12t9 12v46-38q0-12 9-12t9 12v42-24q0-11 9-11t9 11v45l-8 34h-53l-23-35q-9-14 1-20t23 12z" fill="${colors.paper}"/><path d="M-22 56h48v18h-48z" fill="${colors.green}"/></g>`;
const crate = (x,y,w=132,h=96,color=colors.orange) => `<g>${rect(x,y,w,h,color,8)}${rect(x+7,y+9,w-14,9,'#e2ae77',3)}${rect(x+8,y+h-17,w-16,9,'#8b5c3d',3)}${rect(x+w/2-20,y+34,40,14,colors.ink,4)}</g>`;
const shadow = (cx,cy,rx,ry) => `<ellipse cx="${cx}" cy="${cy}" rx="${rx}" ry="${ry}" fill="#05110c" opacity=".35"/>`;
const shelf = (x,y,w=325,h=300) => `<g>${rect(x,y,w,h,'#213a2c',15)}${rect(x+8,y+8,w-16,h-16,'#15271d',11)}${rect(x+14,y+h/2-2,w-28,14,'#628570',3)}${rect(x+14,y+h-24,w-28,14,'#628570',3)}${rect(x-5,y-8,15,h+22,'#416454',4)}${rect(x+w-10,y-8,15,h+22,'#416454',4)}</g>`;
const base = scene => `<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="675" viewBox="0 0 1200 675"><defs><radialGradient id="glow"><stop stop-color="#305c43"/><stop offset="1" stop-color="${colors.bg}"/></radialGradient></defs>${rect(0,0,1200,675,colors.bg)}<ellipse cx="575" cy="300" rx="635" ry="470" fill="url(#glow)"/>${line(70,551,1130,551,'#365644',2)}${scene}</svg>`;

const tools = base(`
  ${shadow(397,565,309,35)}${shadow(885,566,164,23)}
  ${rect(90,77,495,336,'#284235',22)}${rect(106,93,463,304,'#21372d',14)}
  ${Array.from({length:7},(_,row)=>Array.from({length:11},(_,column)=>`<circle cx="${129+column*41}" cy="${115+row*42}" r="3" fill="#4c6c58"/>`).join('')).join('')}
  <g transform="translate(197 135) rotate(-16)"><path d="M-17 0l0 48 6 10-2 144q2 15 17 15t17-15L19 58l13-15V0l-16 20H0z" fill="#bccabd"/><circle cx="4" cy="194" r="7" fill="#21372d"/></g>
  ${screwdriver(292,132,1.18)}${screwdriver(362,154,.98,colors.mint)}
  <g transform="translate(422 173)">${rect(0,0,93,64,colors.gold,13)}${rect(11,52,48,107,colors.green,11)}${rect(20,136,94,24,colors.ink,8)}${rect(86,15,33,33,'#b4c8b7',5)}${rect(115,24,39,15,'#d5dfd2',3)}${line(20,19,64,19,'#86652f',5)}${line(20,32,64,32,'#86652f',5)}</g>
  ${rect(157,422,452,125,colors.green,15)}${rect(153,404,460,35,colors.mint,10)}${rect(314,385,134,31,colors.ink,11)}${rect(332,395,97,21,colors.bg,6)}${rect(194,432,21,49,colors.paper,5)}${rect(550,432,21,49,colors.paper,5)}${rect(324,472,105,32,colors.ink,7)}
  <g transform="rotate(7 887 310)">${rect(735,93,317,434,colors.paper,20)}${rect(828,74,131,42,'#859a8a',12)}${rect(756,136,275,58,'#d6dfcf',8)}${line(774,158,937,158,'#6d8a75',5)}${line(774,176,869,176,'#91a991',4)}
  ${[240,333,426].map((y,i)=>`${rect(757,y-18,46,46,i===1?colors.gold:colors.green,12)}${text(770,y+15,i===1?'−':'+',31,colors.ink)}${line(827,y,1008,y,'#91a991',6)}${line(827,y+17,938,y+17,'#bdc9b7',4)}`).join('')}</g>
  <path d="M642 236q47-37 66-8l-1-24m1 24-24 2" fill="none" stroke="${colors.mint}" stroke-width="8" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="M703 451q-34 45-75 15l6 26m-6-26 27-7" fill="none" stroke="${colors.gold}" stroke-width="8" stroke-linecap="round" stroke-linejoin="round"/>
`);

const inventory = base(`
  ${shadow(310,554,219,25)}${shadow(862,568,195,23)}
  ${shelf(100,118,389,407)}
  ${Array.from({length:4},(_,i)=>screwdriver(184+i*68,141,.88)).join('')}
  ${Array.from({length:4},(_,i)=>screwdriver(184+i*68,342,.88)).join('')}
  ${rect(438,153,90,72,colors.mint,15)}${text(465,206,'8',49,colors.ink)}
  <g transform="rotate(4 838 321)">${rect(663,92,367,447,colors.paper,22)}${rect(785,73,123,41,'#7f9a85',11)}
  ${text(704,167,'УЧЁТ',22,'#5c7663')}${text(875,167,'ФАКТ',22,'#5c7663')}${text(717,250,'9',70,colors.ink)}${text(892,250,'8',70,colors.ink)}
  ${line(838,171,838,266,'#c0ccb9',3)}${line(698,283,997,283,'#c0ccb9',3)}
  ${rect(698,313,65,58,colors.gold,13)}${text(708,354,'−1',32,colors.ink)}${line(786,327,978,327,'#829a81',6)}${line(786,350,928,350,'#bdc9b7',4)}
  ${[419,474].map(y=>`<path d="M707 ${y-12}l12 12 22-26" fill="none" stroke="${colors.green}" stroke-width="7" stroke-linecap="round" stroke-linejoin="round"/>${line(786,y-7,978,y-7,'#829a81',6)}`).join('')}</g>
  <g transform="translate(564 404) rotate(-16)"><circle r="75" fill="#619679" opacity=".2"/><circle r="75" fill="none" stroke="${colors.mint}" stroke-width="14"/>${line(52,54,127,145,colors.mint,24)}<path d="M-40-21q13-26 40-24" fill="none" stroke="#c0ebd2" stroke-width="6" stroke-linecap="round"/></g>
`);

const transfer = base(`
  ${shadow(274,539,195,22)}${shadow(929,539,195,22)}
  ${text(142,106,'КЛАДОВАЯ',24,colors.muted)}${text(833,106,'УЧАСТОК',24,colors.muted)}
  ${shelf(112,159,326,354)}${shelf(762,159,326,354)}
  ${crate(138,181)}${crate(286,181)}${crate(138,379)}${crate(286,379)}
  ${crate(788,181)}${crate(936,181)}${crate(788,379)}${crate(936,379)}
  ${glove(201,220,.5)}${glove(350,220,.5)}${glove(851,220,.5)}${glove(999,220,.5)}
  ${rect(136,286,275,62,colors.paper,13)}${text(155,330,'20',38,colors.ink)}${arrow(228,311,66,colors.green)}${text(328,330,'14',38,colors.ink)}
  ${rect(786,286,275,62,colors.paper,13)}${text(814,330,'4',38,colors.ink)}${arrow(874,311,66,colors.green)}${text(978,330,'10',38,colors.ink)}
  ${rect(495,219,210,182,'#284b36',25)}${glove(575,290,.75)}${glove(632,294,.75)}
  ${text(549,383,'6 пар',32,colors.paper)}${arrow(450,430,295)}
  ${rect(434,553,332,72,colors.mint,18)}${text(466,601,'ВСЕГО: 24 пары',31,colors.ink)}
`);

for (const [name,svg] of Object.entries({'tool-accounting':tools,'inventory-reconcile':inventory,'stock-transfer':transfer})) {
  for (const width of [1200,600]) {
    const target = path.join(output,`${name}-${width}.webp`);
    await sharp(Buffer.from(svg)).resize({width}).webp({quality:84,effort:6}).toFile(target);
    const {size} = await fs.stat(target);
    console.log(`${path.relative(process.cwd(),target)}: ${width} × ${Math.round(width*9/16)}, ${size} bytes`);
  }
}
