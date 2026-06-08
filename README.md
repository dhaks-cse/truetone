# TrueTone — EchoShield AI
**Detect whether a voice recording is real or AI-generated.**

---

## Repository Structure

```
TrueTone/
├── backend/
│   ├── app.py                        ← Flask API server
│   ├── truetone.db                   ← SQLite DB (auto-created)
│   ├── uploads/                      ← Uploaded audio (auto-created)
│   ├── models/
│   │   ├── classifier.py             ← Random Forest wrapper
│   │   ├── rf_model.pkl              ← Trained model (after training)
│   │   └── label_encoder.pkl         ← Label encoder (after training)
│   ├── utils/
│   │   └── feature_extractor.py      ← MFCC extraction
│   └── database/
│       ├── db.py                     ← SQLite helpers
│       └── schema.sql                ← Table definitions
├── frontend/
│   └── src/
│       └── TrueToneApp.java          ← Java Swing GUI
├── dataset/
│   ├── real/                         ← Put real voice WAV files here
│   └── fake/                         ← Put AI/cloned voice WAV files here
├── scripts/
│   ├── train.py                      ← Train the model
│   └── generate_sample_data.py       ← Generate test data (no real data needed)
├── requirements.txt
└── README.md
```

---

## Prerequisites

| Tool | Version | Check command |
|------|---------|---------------|
| Python | 3.10+ | `python3 --version` |
| pip | latest | `pip3 --version` |
| Java JDK | 17+ | `java -version` |
| javac | 17+ | `javac -version` |

Install Java on macOS if missing:
```bash
brew install openjdk@17
```

---

## Step 1 — Create the Python virtual environment

```bash
cd TrueTone
python3 -m venv venv
source venv/bin/activate
pip install --upgrade pip
pip install -r requirements.txt
```

> Every time you open a new terminal, run `source venv/bin/activate` before any Python command.

---

## Step 2 — Add audio data

**Option A — Use your own recordings (recommended for final project)**

- Put real human voice recordings (WAV/MP3) inside `dataset/real/`
- Put AI-generated / cloned voice recordings inside `dataset/fake/`
- Minimum recommended: **30 files per class**

**Option B — Generate synthetic data (quick test)**

```bash
python scripts/generate_sample_data.py
```

This creates 20 synthetic WAV files in each folder so you can verify the whole pipeline works.

---

## Step 3 — Train the model

```bash
python scripts/train.py
```

Expected output:
```
Scanning dataset…
  REAL samples : 20
  FAKE samples : 20
  Total        : 40

  [1/40] real_001.wav → REAL
  ...
Cross-val accuracy (5-fold): 87.50%
Training final model on full dataset…
Training accuracy : 100.00%

✅  Model saved to backend/models/rf_model.pkl
    You can now start the Flask server.
```

---

## Step 4 — Start the Flask backend

Open a **new terminal tab**, activate the venv, then:

```bash
cd TrueTone
source venv/bin/activate
cd backend
python app.py
```

Expected output:
```
TrueTone API running on http://localhost:5000
 * Running on http://127.0.0.1:5000
```

Leave this terminal running. Do not close it.

---

## Step 5 — Compile the Java frontend

Open a **third terminal tab**:

```bash
cd TrueTone/frontend
```

Download the JSON library (one-time only):
```bash
curl -L -o json.jar "https://search.maven.org/remotecontent?filepath=org/json/json/20240303/json-20240303.jar"
```

Compile:
```bash
cd src
javac -cp .:../json.jar TrueToneApp.java
```

---

## Step 6 — Run the Java GUI

```bash
# Still inside frontend/src/
java -cp .:../json.jar TrueToneApp
```

The TrueTone window opens. You are ready.

---

## How to use the GUI

| Step | Action |
|------|--------|
| 1 | Click **Select Audio File** and choose a WAV/MP3 file |
| 2 | Click **Analyze Voice** |
| 3 | Result shows **REAL** (green) or **FAKE** (red) with confidence % |
| 4 | Click **Waveform** to see the audio waveform |
| 5 | Click **Spectrogram** to see the mel spectrogram |
| 6 | Click **History** in the sidebar → **Refresh** to see all past predictions |

---

## API Endpoints (for reference / testing)

```
GET  /health                  → { "status": "ok" }
POST /predict                 → upload field name: "audio"
GET  /history                 → last 100 predictions
GET  /waveform/<prediction_id>
GET  /spectrogram/<prediction_id>
```

Test with curl:
```bash
curl -F "audio=@/path/to/voice.wav" http://localhost:5000/predict
curl http://localhost:5000/history
```

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `ModuleNotFoundError: librosa` | Run `pip install -r requirements.txt` with venv active |
| `Model not found` error | Run `python scripts/train.py` first |
| `Connection refused` in Java GUI | Make sure Flask is running (`python app.py`) |
| `javac: command not found` | Install JDK: `brew install openjdk@17` |
| Low accuracy | Add more audio samples (50+ per class recommended) |
| `soundfile` error on MP3 | Install ffmpeg: `brew install ffmpeg` |

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | Java 17, Swing |
| Backend | Python 3, Flask 3 |
| ML | Scikit-Learn, Random Forest |
| Features | Librosa, MFCC (40 coefficients + deltas) |
| Database | SQLite |
| Visualization | Custom Java2D rendering |

---

## Project Info

- **Project**: TrueTone (EchoShield AI)
- **Description**: Voice authenticity detection using MFCC feature extraction and Random Forest classification
- **Render**: https://truetone-2967.onrender.com/
