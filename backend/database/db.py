"""
backend/database/db.py
Handles all SQLite operations for TrueTone.
"""
import sqlite3
import os

DB_PATH = os.path.join(os.path.dirname(__file__), '..', 'truetone.db')
SCHEMA_PATH = os.path.join(os.path.dirname(__file__), 'schema.sql')


def get_connection():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


def init_db():
    """Create tables if they don't exist."""
    conn = get_connection()
    with open(SCHEMA_PATH, 'r') as f:
        conn.executescript(f.read())
    conn.commit()
    conn.close()


def save_prediction(filename: str, result: str, confidence: float):
    """Insert one prediction record."""
    conn = get_connection()
    conn.execute(
        "INSERT INTO predictions (filename, result, confidence) VALUES (?, ?, ?)",
        (filename, result, round(confidence, 4))
    )
    conn.commit()
    conn.close()


def get_history(limit: int = 50):
    """Return the most recent predictions as a list of dicts."""
    conn = get_connection()
    rows = conn.execute(
        "SELECT id, filename, result, confidence, created_at "
        "FROM predictions ORDER BY id DESC LIMIT ?",
        (limit,)
    ).fetchall()
    conn.close()
    return [dict(r) for r in rows]


def save_model_info(model_name: str, accuracy: float, num_samples: int):
    conn = get_connection()
    conn.execute(
        "INSERT INTO model_info (model_name, accuracy, num_samples) VALUES (?, ?, ?)",
        (model_name, round(accuracy, 4), num_samples)
    )
    conn.commit()
    conn.close()
