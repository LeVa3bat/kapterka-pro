import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';
import {spawnSync} from 'node:child_process';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const source = path.join(root, 'growth/videos');
const target = path.join(root, 'docs/videos');
const check = process.argv.includes('--check');
const ffmpegOption = process.argv.indexOf('--ffmpeg');
const ffmpeg = ffmpegOption >= 0 ? process.argv[ffmpegOption+1] : null;
const manifest = JSON.parse(fs.readFileSync(path.join(source, 'capture-manifest.json'), 'utf8'));
const names = ['issue-check', 'offline-stock', 'report-export'];
const sha = data => crypto.createHash('sha256').update(data).digest('hex');

function time(value) {
  const match = /^(\d{2}):([0-5]\d):([0-5]\d),(\d{3})$/.exec(value);
  if (!match) throw new Error('Invalid subtitle timestamp: '+value);
  return Number(match[1])*3600 + Number(match[2])*60 + Number(match[3]) + Number(match[4])/1000;
}

function toVtt(srt, duration) {
  const blocks = srt.replace(/^\uFEFF/, '').replace(/\r\n?/g, '\n').trim().split(/\n\s*\n/);
  let previousEnd = 0;
  const cues = blocks.map((block, index) => {
    const [number, timing, ...text] = block.split('\n');
    const match = /^(\d{2}:\d{2}:\d{2},\d{3}) --> (\d{2}:\d{2}:\d{2},\d{3})$/.exec(timing ?? '');
    if (number !== String(index+1) || !match || !text.join('').trim()) throw new Error('Invalid subtitle cue '+(index+1));
    const start = time(match[1]), end = time(match[2]);
    if (start < previousEnd || end <= start || end > duration) throw new Error('Subtitle cue exceeds video or overlaps: '+number);
    previousEnd = end;
    return number+'\n'+timing.replaceAll(',', '.')+'\n'+text.map(line => line.replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;')).join('\n');
  });
  if (previousEnd !== duration) throw new Error('Subtitles do not cover the complete video');
  return 'WEBVTT\n\n'+cues.join('\n\n')+'\n';
}

if (!check) fs.mkdirSync(target, {recursive:true});
for (const name of names) {
  const entry = manifest.videoFiles.find(video => video.file === name+'.mp4');
  if (!entry?.decodeVerified || entry.audio !== false || entry.durationSeconds !== 30 || manifest.physicalDeviceRecording !== false) throw new Error('Unverified source video: '+name);
  const mp4 = fs.readFileSync(path.join(source, name+'.mp4'));
  if (sha(mp4) !== entry.sha256) throw new Error('Source video hash mismatch: '+name);
  const srt = fs.readFileSync(path.join(source, name+'.srt'), 'utf8').replace(/\r\n?/g,'\n');
  const vtt = toVtt(srt, entry.durationSeconds);
  for (const [extension, contents] of [['mp4',mp4], ['srt',Buffer.from(srt)], ['vtt',Buffer.from(vtt)]]) {
    const destination = path.join(target, name+'.'+extension);
    if (check) {
      if (!fs.existsSync(destination)) throw new Error('Site asset missing: '+destination);
      const actual=fs.readFileSync(destination);
      const matches=extension==='mp4' ? actual.equals(contents) : actual.toString('utf8').replace(/\r\n?/g,'\n')===contents.toString('utf8');
      if (!matches) throw new Error('Site asset differs from verified source: '+destination);
    } else fs.writeFileSync(destination, contents);
  }
  const poster = path.join(target,name+'-poster.webp');
  if (!check && ffmpeg) {
    const encoded = spawnSync(ffmpeg,['-hide_banner','-loglevel','error','-y','-i',path.join(source,'posters',name+'.png'),'-c:v','libwebp','-lossless','1','-compression_level','6','-q:v','100','-frames:v','1',poster],{encoding:'utf8'});
    if (encoded.status !== 0) throw new Error('Lossless poster conversion failed: '+(encoded.stderr ?? name));
  }
  if (!fs.existsSync(poster)) throw new Error('Poster missing; pass --ffmpeg PATH to make lossless WebP posters: '+name);
  const posterData = fs.readFileSync(poster);
  if (posterData.toString('ascii',0,4) !== 'RIFF' || posterData.toString('ascii',8,12) !== 'WEBP' || posterData.length > 150000) throw new Error('Invalid or oversized WebP poster: '+name);
}
const exportData = fs.readFileSync(path.join(source,manifest.exportFile.file));
if (sha(exportData) !== manifest.exportFile.sha256) throw new Error('Source XLSX hash mismatch');
const exportTarget = path.join(target,manifest.exportFile.file);
if (check) {
  if (!fs.existsSync(exportTarget) || !fs.readFileSync(exportTarget).equals(exportData)) throw new Error('Site XLSX differs from verified source');
} else fs.writeFileSync(exportTarget,exportData);
console.log('PASS: 3 verified MP4 files, synchronized Russian SRT/WebVTT, WebP poster budgets and actual XLSX '+(check?'match':'prepared for')+' the site.');
