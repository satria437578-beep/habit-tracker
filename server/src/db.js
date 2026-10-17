import { DatabaseSync } from 'node:sqlite';

export function openDb(file = 'habits.db') {
  const db = new DatabaseSync(file);
  db.exec(`
    PRAGMA journal_mode = WAL;
    PRAGMA foreign_keys = ON;

    CREATE TABLE IF NOT EXISTS users (
      id         INTEGER PRIMARY KEY AUTOINCREMENT,
      token      TEXT NOT NULL UNIQUE,
      created_at INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS devices (
      id         TEXT PRIMARY KEY,
      user_id    INTEGER NOT NULL,
      name       TEXT,
      created_at INTEGER NOT NULL,
      last_seen  INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS habits (
      id              TEXT PRIMARY KEY,
      user_id         INTEGER NOT NULL,
      name            TEXT NOT NULL,
      category        TEXT,
      schedule_type   TEXT NOT NULL DEFAULT 'daily',
      schedule_days   TEXT NOT NULL DEFAULT '',
      times_per_week  INTEGER NOT NULL DEFAULT 0,
      target_type     TEXT NOT NULL DEFAULT 'check',
      target_value    REAL NOT NULL DEFAULT 0,
      unit            TEXT,
      reminder        TEXT,
      archived        INTEGER NOT NULL DEFAULT 0,
      sort_order      INTEGER NOT NULL DEFAULT 0,
      deleted         INTEGER NOT NULL DEFAULT 0,
      updated_at      INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS checkins (
      id         TEXT PRIMARY KEY,
      user_id    INTEGER NOT NULL,
      habit_id   TEXT NOT NULL,
      date       TEXT NOT NULL,
      value      REAL NOT NULL DEFAULT 1,
      duration   INTEGER NOT NULL DEFAULT 0,
      note       TEXT,
      deleted    INTEGER NOT NULL DEFAULT 0,
      updated_at INTEGER NOT NULL
    );
    CREATE UNIQUE INDEX IF NOT EXISTS idx_checkins_habit_date
      ON checkins(habit_id, date);

    CREATE TABLE IF NOT EXISTS freezes (
      id         TEXT PRIMARY KEY,
      user_id    INTEGER NOT NULL,
      habit_id   TEXT NOT NULL,
      date       TEXT NOT NULL,
      deleted    INTEGER NOT NULL DEFAULT 0,
      updated_at INTEGER NOT NULL
    );
    CREATE UNIQUE INDEX IF NOT EXISTS idx_freezes_habit_date
      ON freezes(habit_id, date);

    CREATE TABLE IF NOT EXISTS tasks (
      id         TEXT PRIMARY KEY,
      user_id    INTEGER NOT NULL,
      title      TEXT NOT NULL,
      notes      TEXT,
      date       TEXT,
      time       TEXT,
      duration   INTEGER NOT NULL DEFAULT 0,
      priority   TEXT NOT NULL DEFAULT 'low',
      important  INTEGER NOT NULL DEFAULT 0,
      urgent     INTEGER NOT NULL DEFAULT 0,
      done       INTEGER NOT NULL DEFAULT 0,
      sort_order INTEGER NOT NULL DEFAULT 0,
      deleted    INTEGER NOT NULL DEFAULT 0,
      updated_at INTEGER NOT NULL
    );

    CREATE INDEX IF NOT EXISTS idx_habits_user_ts  ON habits(user_id, updated_at);
    CREATE INDEX IF NOT EXISTS idx_checkins_user_ts ON checkins(user_id, updated_at);
    CREATE INDEX IF NOT EXISTS idx_freezes_user_ts  ON freezes(user_id, updated_at);
    CREATE INDEX IF NOT EXISTS idx_tasks_user_ts    ON tasks(user_id, updated_at);
  `);
  return db;
}
