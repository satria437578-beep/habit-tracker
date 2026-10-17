import { randomUUID, randomBytes } from 'node:crypto';

const HABIT_FIELDS = [
  'id', 'name', 'category', 'schedule_type', 'schedule_days', 'times_per_week',
  'target_type', 'target_value', 'unit', 'reminder', 'archived', 'sort_order',
];
const CHECKIN_FIELDS = ['habit_id', 'date', 'value', 'duration', 'note'];
const FREEZE_FIELDS = ['habit_id', 'date'];
const TASK_FIELDS = [
  'id', 'title', 'notes', 'date', 'time', 'duration', 'priority',
  'important', 'urgent', 'done', 'sort_order',
];

const TABLES = {
  habits: { fields: HABIT_FIELDS, pk: 'id' },
  checkins: { fields: CHECKIN_FIELDS, pk: 'id' },
  freezes: { fields: FREEZE_FIELDS, pk: 'id' },
  tasks: { fields: TASK_FIELDS, pk: 'id' },
};

const ENTITY_TABLES = { habit: 'habits', checkin: 'checkins', freeze: 'freezes', task: 'tasks' };

const today = () => new Date().toISOString().slice(0, 10);
const now = () => Date.now();

export class Store {
  constructor(db) {
    this.db = db;
  }

  register(name = 'device') {
    const token = randomBytes(24).toString('hex');
    const info = this.db.prepare('INSERT INTO users (token, created_at) VALUES (?, ?)').run(token, now());
    const userId = Number(info.lastInsertRowid);
    const deviceId = randomUUID();
    this.touchDevice(deviceId, userId, name);
    return { token, userId, deviceId };
  }

  userByToken(token) {
    return this.db.prepare('SELECT * FROM users WHERE token = ?').get(token) ?? null;
  }

  touchDevice(id, userId, name = 'device') {
    const t = now();
    this.db.prepare(
      `INSERT INTO devices (id, user_id, name, created_at, last_seen) VALUES (?, ?, ?, ?, ?)
       ON CONFLICT(id) DO UPDATE SET last_seen = excluded.last_seen, name = excluded.name`
    ).run(id, userId, name, t, t);
    return id;
  }

  pull(userId, since = 0) {
    const out = { since: 0, habits: [], checkins: [], freezes: [], tasks: [] };
    for (const table of Object.keys(TABLES)) {
      out[table] = this.db
        .prepare(`SELECT * FROM ${table} WHERE user_id = ? AND updated_at > ? ORDER BY updated_at`)
        .all(userId, since);
    }
    out.since = this.#maxUpdated(userId);
    return out;
  }

  list(userId, entity) {
    const table = ENTITY_TABLES[entity];
    if (!table) throw new Error(`unknown entity: ${entity}`);
    return this.db
      .prepare(`SELECT * FROM ${table} WHERE user_id = ? AND deleted = 0 ORDER BY updated_at DESC`)
      .all(userId);
  }

  get(userId, entity, id) {
    const table = ENTITY_TABLES[entity];
    if (!table) throw new Error(`unknown entity: ${entity}`);
    return this.#get(table, userId, id);
  }

  #maxUpdated(userId) {
    let max = 0;
    for (const table of Object.keys(TABLES)) {
      const row = this.db.prepare(`SELECT MAX(updated_at) AS m FROM ${table} WHERE user_id = ?`).get(userId);
      if (row?.m && row.m > max) max = row.m;
    }
    return max;
  }

  apply(userId, type, data = {}) {
    const [entity, action] = type.split('.');
    const table = { habit: 'habits', checkin: 'checkins', freeze: 'freezes', task: 'tasks' }[entity];
    if (!table) throw new Error(`unknown entity: ${entity}`);
    const t = now();

    if (action === 'delete') {
      const id = this.#resolveId(table, data);
      if (!id) throw new Error('delete requires id');
      this.db.prepare(`UPDATE ${table} SET deleted = 1, updated_at = ? WHERE id = ? AND user_id = ?`)
        .run(t, id, userId);
      return this.#get(table, userId, id);
    }

    if (action !== 'put') throw new Error(`unknown action: ${action}`);
    const rows = this.#upsert(table, userId, data, t);
    return rows;
  }

  #resolveId(table, data) {
    return data.id ?? (data.habitId ? `${data.habitId}:${data.date ?? today()}` : null);
  }

  #upsert(table, userId, data, t) {
    const { fields, pk } = TABLES[table];
    const idSeed = data.habitId ?? data.habit_id;
    const id = data.id
      ?? (idSeed ? `${idSeed}:${data.date ?? today()}` : randomUUID());

    const values = {
      id,
      name: data.name,
      category: data.category ?? null,
      schedule_type: data.scheduleType ?? data.schedule_type ?? undefined,
      schedule_days: data.scheduleDays ?? data.schedule_days,
      times_per_week: data.timesPerWeek ?? data.times_per_week,
      target_type: data.targetType ?? data.target_type,
      target_value: data.targetValue ?? data.target_value,
      unit: data.unit,
      reminder: data.reminder,
      archived: data.archived === undefined ? undefined : (data.archived ? 1 : 0),
      sort_order: data.sortOrder ?? data.sort_order,
      habit_id: idSeed,
      date: data.date,
      value: data.value,
      duration: data.duration,
      note: data.note,
      title: data.title,
      notes: data.notes,
      time: data.time,
      priority: data.priority,
      important: data.important === undefined ? undefined : (data.important ? 1 : 0),
      urgent: data.urgent === undefined ? undefined : (data.urgent ? 1 : 0),
      done: data.done === undefined ? undefined : (data.done ? 1 : 0),
    };

    const existing = this.#get(table, userId, id);
    const cols = fields.filter((f) => values[f] !== undefined);
    if (existing) {
      const sets = cols.map((c) => `${c} = ?`).join(', ');
      this.db.prepare(`UPDATE ${table} SET ${sets}, deleted = 0, updated_at = ? WHERE id = ? AND user_id = ?`)
        .run(...cols.map((c) => values[c]), t, id, userId);
    } else {
      const insertCols = [...cols, 'id', 'user_id', 'deleted', 'updated_at'];
      const params = cols.map((c) => values[c]).concat([id, userId, 0, t]);
      const ph = insertCols.map(() => '?').join(', ');
      this.db.prepare(`INSERT INTO ${table} (${insertCols.join(', ')}) VALUES (${ph})`).run(...params);
    }
    return this.#get(table, userId, id);
  }

  #get(table, userId, id) {
    return this.db.prepare(`SELECT * FROM ${table} WHERE id = ? AND user_id = ?`).get(id, userId) ?? null;
  }
}

export { today };
