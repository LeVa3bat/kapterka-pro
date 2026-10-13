import fs from 'node:fs';
import crypto from 'node:crypto';
import {transform} from 'esbuild';
const files = ['style.css','tabs_and_cabinet.css','site36.css','landing.css'];
const source = files.map(name=>fs.readFileSync('docs/'+name,'utf8').replace(/\r\n/g,'\n')).join('\n');
const digest = crypto.createHash('sha256').update(source).digest('hex');
const {code} = await transform(source,{loader:'css',minify:true,target:['chrome90','safari14']});
const output = '/* Generated from '+files.join(', ')+'. Source SHA-256: '+digest+' */\n'+code;
const target = 'docs/site.bundle.min.css';
if (process.argv.includes('--check')) {
  if (!fs.existsSync(target) || fs.readFileSync(target,'utf8').replace(/\r\n/g,'\n') !== output) throw new Error('CSS bundle is stale: run node .github/scripts/build-site-css.mjs');
} else fs.writeFileSync(target,output);
console.log('PASS: CSS bundle '+Buffer.byteLength(output)+' bytes; source '+Buffer.byteLength(source)+' bytes.');
