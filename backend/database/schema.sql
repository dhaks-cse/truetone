-- TrueTone SQLite Schema
-- File: backend/database/schema.sql

CREATE TABLE IF NOT EXISTS predictions (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    filename    TEXT    NOT NULL,
    result      TEXT    NOT NULL,        -- 'REAL' or 'FAKE'
    confidence  REAL    NOT NULL,        -- 0.0 to 1.0
    created_at  TEXT    NOT NULL DEFAULT (datetime('now','localtime'))
);

CREATE TABLE IF NOT EXISTS model_info (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    model_name   TEXT NOT NULL,
    accuracy     REAL,
    trained_at   TEXT NOT NULL DEFAULT (datetime('now','localtime')),
    num_samples  INTEGER
);
