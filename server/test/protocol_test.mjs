import WebSocket from 'ws';
import { randomUUID } from 'node:crypto';

const URL = process.env.WS ?? 'ws://localhost:8080';
const ok = (label, cond) => console.log(`${cond ? 'PASS' : 'FAIL'} - ${label}`);

function client() {
  const ws = new WebSocket(URL);
  const pending = new Map();
  ws.on('message', (raw) => {
    const msg = JSON.parse(raw);
    if (msg.id && pending.has(msg.id)) { pending.get(msg.id)(msg); pending.delete(msg.id); }
  });
  const req = (type, data) => new Promise((resolve) => {
    const id = randomUUID();
    pending.set(id, resolve);
    ws.send(JSON.stringify({ type, id, data }));
  });
  return { ws, req, open: new Promise((r) => ws.on('open', r)) };
}

const a = client();
await a.open;

const bad = await a.req('auth', { token: 'nope' });
ok('invalid token rejected', bad.type === 'auth.err');

const unauth = await a.req('sync.pull', {});
ok('pull before auth blocked', unauth.type === 'error' && unauth.data.code === 'unauthorized');

const userA = (await a.req('auth.register', { deviceName: 'A' })).data;
const habitId = randomUUID();
await a.req('habit.put', { id: habitId, name: 'Baca', scheduleType: 'weekdays', scheduleDays: '1,3,5', targetType: 'timer', targetValue: 1800 });

const frz = await a.req('freeze.put', { habitId, date: '2026-10-14' });
ok('freeze put', frz.type === 'sync.ack' && frz.data.rows[0].id === `${habitId}:2026-10-14`);
const unfrz = await a.req('freeze.delete', { id: `${habitId}:2026-10-14` });
ok('freeze delete tombstone', unfrz.data.rows[0].deleted === 1);

await a.req('checkin.put', { habitId, date: '2026-10-13', value: 1, duration: 1800 });
const delByDate = await a.req('checkin.delete', { habitId, date: '2026-10-13' });
ok('checkin delete by habit+date', delByDate.type === 'sync.ack' && delByDate.data.rows[0].deleted === 1);

const b = client();
await b.open;
const userB = (await b.req('auth.register', { deviceName: 'B' })).data;
const stateB = await b.req('sync.pull', { since: 0 });
ok('user isolation', userB.userId !== userA.userId && stateB.data.habits.length === 0);

a.ws.close();
b.ws.close();
setTimeout(() => process.exit(0), 100);
