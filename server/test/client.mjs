import WebSocket from 'ws';
import { randomUUID } from 'node:crypto';

const URL = process.env.WS ?? 'ws://localhost:8080';

function client(name) {
  const ws = new WebSocket(URL);
  const pending = new Map();
  const events = [];
  ws.on('message', (raw) => {
    const msg = JSON.parse(raw);
    if (msg.id && pending.has(msg.id)) {
      pending.get(msg.id)(msg);
      pending.delete(msg.id);
    } else {
      events.push(msg);
    }
  });
  const req = (type, data) => new Promise((resolve) => {
    const id = randomUUID();
    pending.set(id, resolve);
    ws.send(JSON.stringify({ type, id, data }));
  });
  return { ws, req, events, open: new Promise((r) => ws.on('open', r)) };
}

const main = client('main');
const peer = client('peer');
await Promise.all([main.open, peer.open]);

const reg = await main.req('auth.register', { deviceName: 'Pixel' });
console.log('1. register ->', reg.type, reg.data.userId ? 'user=' + reg.data.userId : reg.data);
const token = reg.data.token;

const auth = await peer.req('auth', { token, deviceName: 'Tablet' });
console.log('2. peer auth ->', auth.type);

const habitId = randomUUID();
const put = await main.req('habit.put', {
  id: habitId, name: 'Minum Air', category: 'sehat',
  scheduleType: 'weekly', timesPerWeek: 5, targetType: 'numeric',
  targetValue: 8, unit: 'gelas',
});
console.log('3. habit.put ->', put.type, put.data.rows[0].name, put.data.rows[0].schedule_type);

const ci = await main.req('checkin.put', { habitId, date: '2026-10-15', value: 3, note: 'pagi' });
console.log('4. checkin.put ->', ci.type, ci.data.rows[0].id, 'value=', ci.data.rows[0].value);

await new Promise((r) => setTimeout(r, 200));
console.log('5. peer received push ->', peer.events.map((e) => e.type).join(',') || 'none');

const state = await peer.req('sync.pull', { since: 0 });
console.log('6. peer pull ->', state.data.habits.length, 'habits,', state.data.checkins.length, 'checkins');

const del = await main.req('checkin.delete', { id: `${habitId}:2026-10-15` });
console.log('7. checkin.delete ->', del.type, 'deleted=', del.data.rows[0].deleted);

const delta = await peer.req('sync.pull', { since: state.data.since });
console.log('8. delta pull ->', delta.data.checkins.length, 'changed rows');

const arc = await main.req('habit.put', { id: habitId, archived: true });
console.log('9. archive ->', arc.type, 'archived=', arc.data.rows[0].archived);

main.ws.close();
peer.ws.close();
setTimeout(() => process.exit(0), 100);
