const http = require('http');
const { URL } = require('url');
const { handler } = require('./yandex-cloud-function.js');

const PORT = Number(process.env.PORT || 8080);
const MAX_BODY_BYTES = 1024 * 1024;

function queryObject(searchParams) {
  const out = {};
  for (const [key, value] of searchParams.entries()) {
    if (!(key in out)) out[key] = value;
  }
  return out;
}

const server = http.createServer((req, res) => {
  const chunks = [];
  let total = 0;
  let rejected = false;

  req.on('data', (chunk) => {
    if (rejected) return;
    total += chunk.length;
    if (total > MAX_BODY_BYTES) {
      rejected = true;
      res.statusCode = 413;
      res.setHeader('Content-Type', 'application/json; charset=utf-8');
      res.end(JSON.stringify({ ok: false, error: 'PAYLOAD_TOO_LARGE' }));
      req.destroy();
      return;
    }
    chunks.push(chunk);
  });

  req.on('end', async () => {
    if (rejected) return;
    try {
      const origin = 'http://' + (req.headers.host || 'localhost');
      const url = new URL(req.url || '/', origin);
      const event = {
        httpMethod: String(req.method || 'GET').toUpperCase(),
        queryStringParameters: queryObject(url.searchParams),
        headers: req.headers,
        body: Buffer.concat(chunks).toString('utf8'),
        isBase64Encoded: false,
        requestContext: {
          identity: {
            sourceIp: req.socket.remoteAddress || ''
          },
          http: {
            sourceIp: req.socket.remoteAddress || ''
          }
        }
      };

      const result = await handler(event);
      res.statusCode = Number(result?.statusCode || 500);
      for (const [name, value] of Object.entries(result?.headers || {})) {
        if (value !== undefined && value !== null) res.setHeader(name, String(value));
      }
      res.end(String(result?.body || ''));
    } catch (error) {
      console.error('HTTP adapter error:', error?.message || error);
      if (!res.headersSent) {
        res.statusCode = 500;
        res.setHeader('Content-Type', 'application/json; charset=utf-8');
      }
      res.end(JSON.stringify({ ok: false, error: 'INTERNAL_ERROR' }));
    }
  });

  req.on('error', (error) => {
    console.error('HTTP request error:', error?.message || error);
    if (!res.headersSent) {
      res.statusCode = 400;
      res.setHeader('Content-Type', 'application/json; charset=utf-8');
    }
    if (!res.writableEnded) {
      res.end(JSON.stringify({ ok: false, error: 'BAD_REQUEST' }));
    }
  });
});

server.listen(PORT, '0.0.0.0', () => {
  console.log('Kapterka backend listening on port ' + PORT);
});
