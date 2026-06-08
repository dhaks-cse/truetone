#!/bin/bash
# run_backend.sh — Run from project root: bash run_backend.sh
set -e

echo "=== TrueTone Backend Launcher ==="

# Activate venv
if [ -f "venv/bin/activate" ]; then
    source venv/bin/activate
    echo "✅ Virtual environment activated"
else
    echo "❌ Virtual environment not found."
    echo "   Run:  python3 -m venv venv && source venv/bin/activate && pip install -r requirements.txt"
    exit 1
fi

# Check model
if [ ! -f "backend/models/rf_model.pkl" ]; then
    echo ""
    echo "⚠️  Model not found. Running training first…"
    python scripts/train.py
fi

echo ""
echo "Starting Flask server on http://localhost:5000"
cd backend
python app.py
