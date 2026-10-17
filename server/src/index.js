import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { WebSocketServer } from 'ws';
import { openDb } from './db.js';
import { Store } from './store.js';

const PUBLIC_DIR = resolve(dirname(fileURLToPath(import.meta.url)), '..', 'public');

const PORT = Number(process.env.PORT ?? 8080);
const store = new Store(openDb(process.env.DB ?? 'habits.db'));

const ENTITY_OP = /^(habit|checkin|freeze|task)\.(put|delete)$/;
const socketsByUser = new Map();

function send(ws, type, id, data) {
  if (ws.readyState === ws.OPEN) ws.send(JSON.stringify({ type, id, data }));
}

function broadcast(userId, exclude, payload) {
  const peers = socketsByUser.get(userId);
  if (!peers) return;
  const msg = JSON.stringify(payload);
  for (const peer of peers) {
    if (peer !== exclude && peer.readyState === peer.OPEN) peer.send(msg);
  }
}

function join(userId, ws) {
  if (!socketsByUser.has(userId)) socketsByUser.set(userId, new Set());
  socketsByUser.get(userId).add(ws);
}

function leave(userId, ws) {
  socketsByUser.get(userId)?.delete(ws);
}

async function serveHttp(req, res) {
  const url = new URL(req.url, 'http://localhost');
  const path = url.pathname;

  if (req.method === 'OPTIONS') {
    cors(res);
    res.writeHead(204);
    return res.end();
  }

  if (path === '/api' || path.startsWith('/api/')) {
    try {
      await serveApi(req, res, url);
    } catch (err) {
      json(res, 500, { error: 'internal', message: err.message });
    }
    return;
  }

  if (req.method === 'GET' && (path === '/' || path === '/index.html')) {
    try {
      const html = await readFile(resolve(PUBLIC_DIR, 'index.html'));
      res.writeHead(200, {
        'content-type': 'text/html; charset=utf-8',
        'cache-control': 'no-store, no-cache, must-revalidate',
        pragma: 'no-cache',
      });
      return res.end(html);
    } catch {
      res.writeHead(500, { 'content-type': 'text/plain' });
      return res.end('index.html tidak ditemukan');
    }
  }
  if (req.method === 'GET' && path === '/health') {
    res.writeHead(200, { 'content-type': 'application/json' });
    return res.end(JSON.stringify({ ok: true, time: Date.now() }));
  }
  res.writeHead(404, { 'content-type': 'text/plain' });
  res.end('not found');
}

/* ---------------- REST API (token: users.token) ---------------- */
const ENTITY_PATHS = { habits: 'habit', checkins: 'checkin', freezes: 'freeze', tasks: 'task' };

function cors(res) {
  res.setHeader('access-control-allow-origin', '*');
  res.setHeader('access-control-allow-methods', 'GET,POST,PUT,PATCH,DELETE,OPTIONS');
  res.setHeader('access-control-allow-headers', 'authorization,content-type,x-auth-token');
  res.setHeader('access-control-max-age', '86400');
}

function json(res, status, obj) {
  cors(res);
  const body = JSON.stringify(obj);
  res.writeHead(status, {
    'content-type': 'application/json; charset=utf-8',
    'content-length': Buffer.byteLength(body),
  });
  res.end(body);
}

function readBody(req, limit = 1_000_000) {
  return new Promise((resolve, reject) => {
    let size = 0;
    const chunks = [];
    req.on('data', (c) => {
      size += c.length;
      if (size > limit) {
        reject(new Error('payload too large'));
        req.destroy();
        return;
      }
      chunks.push(c);
    });
    req.on('end', () => {
      const raw = Buffer.concat(chunks).toString('utf8').trim();
      if (!raw) return resolve({});
      try {
        resolve(JSON.parse(raw));
      } catch {
        reject(new Error('invalid JSON body'));
      }
    });
    req.on('error', reject);
  });
}

function authenticate(req) {
  const header = String(req.headers['authorization'] || '');
  const match = /^Bearer\s+(.+)$/i.exec(header);
  const token = (match ? match[1] : String(req.headers['x-auth-token'] || '')).trim();
  if (!token) return null;
  return store.userByToken(token);
}

function commit(userId, entity, handler) {
  const rows = [handler()].flat().filter(Boolean);
  broadcast(userId, null, { type: 'sync.update', id: null, data: { entity, rows } });
  return rows;
}

const API_ENDPOINTS = [
  'POST   /api/auth/register            -> daftar user baru, dapat token (tanpa auth)',
  'GET    /api/openapi.json             -> skema OpenAPI (tanpa auth)',
  'GET    /api/me                        -> info user dari token',
  'GET    /api/state?since=0             -> seluruh data (habits/checkins/freezes/tasks)',
  'GET    /api/habits                    -> daftar habit',
  'POST   /api/habits                    -> buat habit',
  'GET    /api/habits/:id                -> detail habit',
  'PUT    /api/habits/:id                -> ubah habit',
  'DELETE /api/habits/:id                -> hapus habit',
  'GET    /api/tasks                     -> daftar task',
  'POST   /api/tasks                     -> buat task',
  'GET    /api/tasks/:id                 -> detail task',
  'PUT    /api/tasks/:id                 -> ubah task (judul, jam, tanggal, prioritas, done, ...)',
  'DELETE /api/tasks/:id                 -> hapus task',
  'GET    /api/checkins                  -> daftar check-in',
  'POST   /api/checkins                  -> buat check-in',
  'GET    /api/checkins/:id              -> detail check-in',
  'PUT    /api/checkins/:id              -> ubah check-in',
  'DELETE /api/checkins/:id              -> hapus check-in',
  'DELETE /api/checkins?habitId=&date=  -> hapus check-in per habit+tanggal',
  'GET    /api/freezes                   -> daftar hari libur',
  'POST   /api/freezes                   -> tandai hari libur',
  'DELETE /api/freezes/:id               -> batal hari libur',
];

async function serveApi(req, res, url) {
  const rest = url.pathname.split('/').filter(Boolean).slice(1);

  if (rest[0] === 'auth' && rest[1] === 'register' && req.method === 'POST') {
    const body = await readBody(req);
    const info = store.register(body.deviceName || 'api');
    return json(res, 201, info);
  }
  if (rest[0] === 'openapi.json' && req.method === 'GET') {
    return json(res, 200, openapiDoc());
  }

  const user = authenticate(req);
  if (!user) {
    return json(res, 401, {
      error: 'unauthorized',
      message: 'Kirim header "Authorization: Bearer <token>". Dapatkan token via POST /api/auth/register.',
    });
  }

  if (rest.length === 0 && req.method === 'GET') {
    return json(res, 200, { name: 'habit-api', version: 1, userId: user.id, endpoints: API_ENDPOINTS });
  }
  if (rest[0] === 'me' && req.method === 'GET') {
    return json(res, 200, { userId: user.id });
  }
  if (rest[0] === 'state' && req.method === 'GET') {
    const since = Number(url.searchParams.get('since')) || 0;
    return json(res, 200, store.pull(user.id, since));
  }

  const entity = ENTITY_PATHS[rest[0]];
  if (!entity) {
    return json(res, 404, { error: 'not_found', message: `resource tidak dikenal: ${rest[0] ?? '(kosong)'}` });
  }
  const id = rest[1] ? decodeURIComponent(rest[1]) : null;

  if (req.method === 'GET' && !id) return json(res, 200, { rows: store.list(user.id, entity) });
  if (req.method === 'GET' && id) {
    const row = store.get(user.id, entity, id);
    return row ? json(res, 200, row) : json(res, 404, { error: 'not_found', id });
  }

  if ((req.method === 'POST' && !id) || ((req.method === 'PUT' || req.method === 'PATCH') && id)) {
    const body = await readBody(req);
    if (id) body.id = id;
    const rows = commit(user.id, entity, () => store.apply(user.id, `${entity}.put`, body));
    return json(res, req.method === 'POST' ? 201 : 200, { rows });
  }

  if (req.method === 'DELETE') {
    const body = id ? { id } : { ...Object.fromEntries(url.searchParams), ...(await readBody(req)) };
    const rows = commit(user.id, entity, () => store.apply(user.id, `${entity}.delete`, body));
    return json(res, 200, { rows });
  }

  return json(res, 405, { error: 'method_not_allowed', method: req.method });
}

function openapiDoc() {
  const entitySchema = { type: 'object', additionalProperties: true };
  const collection = (name, entity) => ({
    get: {
      summary: `Daftar ${name}`,
      security: [{ bearerAuth: [] }],
      responses: { 200: { description: 'OK' } },
    },
    post: {
      summary: `Buat ${name}`,
      security: [{ bearerAuth: [] }],
      requestBody: { content: { 'application/json': { schema: entitySchema } } },
      responses: { 201: { description: 'Dibuat' } },
    },
  });
  const item = (name) => ({
    parameters: [{ name: 'id', in: 'path', required: true, schema: { type: 'string' } }],
    get: { summary: `Detail ${name}`, security: [{ bearerAuth: [] }], responses: { 200: { description: 'OK' } } },
    put: {
      summary: `Ubah ${name}`,
      security: [{ bearerAuth: [] }],
      requestBody: { content: { 'application/json': { schema: entitySchema } } },
      responses: { 200: { description: 'OK' } },
    },
    delete: { summary: `Hapus ${name}`, security: [{ bearerAuth: [] }], responses: { 200: { description: 'OK' } } },
  });
  return {
    openapi: '3.0.3',
    info: {
      title: 'Habit Tracker API',
      version: '1.0.0',
      description: 'REST API untuk mengakses dan mengubah habit, task, check-in, dan hari libur. Autentikasi dengan Bearer token dari POST /api/auth/register.',
    },
    servers: [{ url: '/api' }],
    components: {
      securitySchemes: { bearerAuth: { type: 'http', scheme: 'bearer' } },
    },
    security: [{ bearerAuth: [] }],
    paths: {
      '/auth/register': {
        post: {
          summary: 'Daftar user baru dan dapatkan token',
          requestBody: {
            content: { 'application/json': { schema: { type: 'object', properties: { deviceName: { type: 'string' } } } } },
          },
          responses: { 201: { description: 'Berisi token, userId, deviceId' } },
        },
      },
      '/me': { get: { summary: 'Info user', security: [{ bearerAuth: [] }], responses: { 200: { description: 'OK' } } } },
      '/state': {
        get: {
          summary: 'Ambil seluruh data (sync)',
          security: [{ bearerAuth: [] }],
          parameters: [{ name: 'since', in: 'query', schema: { type: 'integer' } }],
          responses: { 200: { description: 'OK' } },
        },
      },
      '/habits': collection('habit', 'habit'),
      '/habits/{id}': item('habit'),
      '/tasks': collection('task', 'task'),
      '/tasks/{id}': item('task'),
      '/checkins': collection('check-in', 'checkin'),
      '/checkins/{id}': item('check-in'),
      '/freezes': collection('hari libur', 'freeze'),
      '/freezes/{id}': item('hari libur'),
    },
  };
}

const httpServer = createServer(serveHttp);
const wss = new WebSocketServer({ server: httpServer });

wss.on('connection', (ws) => {
  ws.isAlive = true;
  ws.userId = null;
  ws.on('pong', () => { ws.isAlive = true; });

  ws.on('message', (raw) => {
    let msg;
    try { msg = JSON.parse(raw); } catch { return send(ws, 'error', null, { code: 'bad_json' }); }
    const { type, id, data } = msg ?? {};
    try {
      handle(ws, String(type), id, data ?? {});
    } catch (err) {
      send(ws, 'error', id, { code: 'internal', message: err.message });
    }
  });

  ws.on('close', () => { if (ws.userId) leave(ws.userId, ws); });
});

function handle(ws, type, id, data) {
  if (type === 'auth.register') {
    const { token, userId, deviceId } = store.register(data.deviceName);
    ws.userId = userId;
    join(userId, ws);
    store.touchDevice(deviceId, userId, data.deviceName);
    return send(ws, 'auth.ok', id, { token, userId, deviceId });
  }

  if (type === 'auth') {
    const user = store.userByToken(data.token);
    if (!user) return send(ws, 'auth.err', id, { code: 'invalid_token' });
    ws.userId = user.id;
    join(user.id, ws);
    if (data.deviceId) store.touchDevice(data.deviceId, user.id, data.deviceName);
    return send(ws, 'auth.ok', id, { userId: user.id, serverTime: Date.now() });
  }

  if (!ws.userId) return send(ws, 'error', id, { code: 'unauthorized' });

  if (type === 'sync.pull') {
    return send(ws, 'sync.state', id, store.pull(ws.userId, Number(data.since) || 0));
  }

  if (type === 'ping') return send(ws, 'pong', id, { t: Date.now() });

  if (ENTITY_OP.test(type)) {
    const rows = store.apply(ws.userId, type, data);
    const [entity] = type.split('.');
    send(ws, 'sync.ack', id, { entity, rows: [rows].flat().filter(Boolean) });
    broadcast(ws.userId, ws, {
      type: 'sync.update',
      id: null,
      data: { entity, rows: [rows].flat().filter(Boolean) },
    });
    return;
  }

  send(ws, 'error', id, { code: 'unknown_type', message: type });
}

const heartbeat = setInterval(() => {
  for (const ws of wss.clients) {
    if (!ws.isAlive) { ws.terminate(); continue; }
    ws.isAlive = false;
    ws.ping();
  }
}, 30000);
wss.on('close', () => clearInterval(heartbeat));

console.log(`habit-server listening on http://localhost:${PORT} (WebSocket di port yang sama)`);
httpServer.listen(PORT);
