import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import {createGzip} from 'node:zlib';
const root = path.resolve(process.argv[2] || 'docs');
const port = Number(process.argv[3] || 4173);
const types = { '.html':'text/html; charset=utf-8', '.js':'text/javascript', '.css':'text/css', '.json':'application/json', '.jpg':'image/jpeg', '.webp':'image/webp', '.png':'image/png', '.svg':'image/svg+xml', '.woff2':'font/woff2', '.pdf':'application/pdf', '.xlsx':'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', '.apk':'application/vnd.android.package-archive', '.mp4':'video/mp4', '.vtt':'text/vtt; charset=utf-8', '.srt':'application/x-subrip; charset=utf-8' };
http.createServer((req,res) => {
  const relative = decodeURIComponent(new URL(req.url, 'http://localhost').pathname);
  const file = path.resolve(root, '.' + (relative === '/' ? '/index.html' : relative));
  if (!file.startsWith(root + path.sep) || !fs.existsSync(file) || !fs.statSync(file).isFile()) { res.writeHead(404); res.end(); return; }
  // GitHub Pages отдаёт HTML/CSS/JS с gzip. Замер CI использует тот же режим передачи.
  const compressed = /\bgzip\b/.test(req.headers['accept-encoding'] || '') && /\.(html|css|js|json|svg)$/.test(file);
  const size = fs.statSync(file).size;
  const headers = { 'Content-Type': types[path.extname(file)] || 'application/octet-stream', 'Vary':'Accept-Encoding', ...(compressed ? {'Content-Encoding':'gzip'} : {'Accept-Ranges':'bytes','Content-Length':size}) };
  let start = 0, end = size - 1, status = 200;
  if (!compressed && req.headers.range) {
    const parts = /^bytes=(\d*)-(\d*)$/.exec(req.headers.range);
    if (parts && (parts[1] || parts[2])) {
      if (parts[1]) { start = Number(parts[1]); end = parts[2] ? Math.min(Number(parts[2]), end) : end; }
      else { start = Math.max(0, size - Number(parts[2])); }
    }
    if (!parts || !(parts[1] || parts[2]) || (!parts[1] && Number(parts[2]) === 0) || !Number.isSafeInteger(start) || !Number.isSafeInteger(end) || start < 0 || start >= size || end < start) {
      res.writeHead(416, {'Content-Range':`bytes */${size}`}); res.end(); return;
    }
    status = 206;
    headers['Content-Range'] = `bytes ${start}-${end}/${size}`;
    headers['Content-Length'] = end - start + 1;
  }
  res.writeHead(status, headers);
  if (req.method === 'HEAD') { res.end(); return; }
  const stream = fs.createReadStream(file, status === 206 ? {start,end} : undefined);
  if (compressed) stream.pipe(createGzip()).pipe(res);
  else stream.pipe(res);
}).listen(port, '127.0.0.1', () => console.log('Preview: http://127.0.0.1:'+port));
