"""
backend/app.py
TrueTone Flask API — all routes in one file for simplicity.

Endpoints:
  POST /predict          – upload audio, get prediction
  GET  /history          – get prediction history
  GET  /waveform/<id>    – get waveform data for a prediction
  GET  /spectrogram/<id> – get spectrogram data for a prediction
  GET  /health           – server health check
"""
import os
import uuid
from flask import Flask, request, jsonify
from flask_cors import CORS

from database.db             import init_db, save_prediction, get_history
from utils.feature_extractor import extract_features, extract_waveform_data, extract_spectrogram_data
from models.classifier       import predict

# ── app setup ─────────────────────────────────────────────────────────────────
app         = Flask(__name__)
CORS(app, resources={r"/*": {"origins": "*"}})

UPLOAD_DIR  = os.path.join(os.path.dirname(__file__), 'uploads')
ALLOWED_EXT = {'.wav', '.mp3', '.flac', '.ogg', '.m4a'}
os.makedirs(UPLOAD_DIR, exist_ok=True)

# Keep a tiny in-memory map { prediction_id → saved_file_path } so waveform
# and spectrogram endpoints can re-use the file without touching the DB.
_file_cache: dict[str, str] = {}


def _allowed(filename: str) -> bool:
    return os.path.splitext(filename)[1].lower() in ALLOWED_EXT


# ── routes ────────────────────────────────────────────────────────────────────

@app.route('/health', methods=['GET'])
def health():
    return jsonify({"status": "ok", "service": "TrueTone API"})


@app.route('/predict', methods=['POST'])
def predict_route():
    if 'audio' not in request.files:
        return jsonify({"error": "No audio file in request (field name must be 'audio')"}), 400

    f = request.files['audio']
    if f.filename == '':
        return jsonify({"error": "Empty filename"}), 400
    if not _allowed(f.filename):
        return jsonify({"error": f"Unsupported file type. Allowed: {ALLOWED_EXT}"}), 400

    # Save with unique name to avoid collisions
    ext      = os.path.splitext(f.filename)[1].lower()
    unique   = str(uuid.uuid4())
    savepath = os.path.join(UPLOAD_DIR, f"{unique}{ext}")
    f.save(savepath)

    try:
        features   = extract_features(savepath)
        label, confidence = predict(features)
    except FileNotFoundError as e:
        return jsonify({"error": str(e)}), 500
    except Exception as e:
        return jsonify({"error": f"Feature extraction / prediction failed: {e}"}), 500

    save_prediction(f.filename, label, confidence)
    _file_cache[unique] = savepath

    return jsonify({
        "prediction_id": unique,
        "filename":      f.filename,
        "result":        label,
        "confidence":    round(confidence * 100, 2),   # send as percentage
        "confidence_raw": round(confidence, 4),
    })


@app.route('/history', methods=['GET'])
def history_route():
    rows = get_history(limit=100)
    return jsonify(rows)


@app.route('/waveform/<pred_id>', methods=['GET'])
def waveform_route(pred_id: str):
    path = _file_cache.get(pred_id)
    if not path or not os.path.exists(path):
        return jsonify({"error": "File not found. It may have been cleaned up."}), 404
    try:
        data = extract_waveform_data(path)
        return jsonify({"waveform": data})
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.route('/spectrogram/<pred_id>', methods=['GET'])
def spectrogram_route(pred_id: str):
    path = _file_cache.get(pred_id)
    if not path or not os.path.exists(path):
        return jsonify({"error": "File not found. It may have been cleaned up."}), 404
    try:
        data = extract_spectrogram_data(path)
        return jsonify(data)
    except Exception as e:
        return jsonify({"error": str(e)}), 500


# ── startup ───────────────────────────────────────────────────────────────────
if __name__ == '__main__':
    init_db()
    print("TrueTone API running on http://localhost:5000")
    app.run(debug=True, port=8080)
