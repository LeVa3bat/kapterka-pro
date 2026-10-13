import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import {createGzip} from 'node:zlib';
const root = path.resolve(process.argv[2] || 'docs');
const port = Number(process.argv[3] || 4173);
const types = { '.html':'text/html; charset=utf-8', '.js':'text/javascript', '.css':'text/css', '.json':'application/json', '.jpg':'image/jpeg', '.webp':'image/webp', '.png':'image/png', '.svg':'image/svg+xml', '.woff2':'font/woff2', '.pdf':'application/pdf', '.xlsx':'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', '.apk':'application/vnd.android.package-archive' };
http.createServer((req,res) => {
  const relative = decodeURIComponent(new URL(req.url, 'http://localhost').pathname);
  const file = path.resolve(root, '.' + (relative === '/' ? '/index.html' : relative));
  if (!file.startsWith(root + path.sep) || !fs.existsSync(file) || !fs.statSync(file).isFile()) { res.writeHead(404); res.end(); return; }
  // GitHub Pages отдаёт HTML/CSS/JS с gzip. Замер CI использует тот же режим передачи.
  const compressed = /\bgzip\b/.test(req.headers['accept-encoding'] || '') && /\.(html|css|js|json|svg)$/.test(file);
  res.writeHead(200, { 'Content-Type': types[path.extname(file)] || 'application/octet-stream', 'Vary':'Accept-Encoding', ...(compressed ? {'Content-Encoding':'gzip'} : {}) });
  const stream = fs.createReadStream(file);
  if (compressed) stream.pipe(createGzip()).pipe(res);
  else stream.pipe(res);
}).listen(port, '127.0.0.1', () => console.log('Preview: http://127.0.0.1:'+port));
