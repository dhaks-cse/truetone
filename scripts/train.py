"""
scripts/train.py
──────────────────────────────────────────────────────────────────
Scans dataset/real/ and dataset/fake/, extracts MFCC features,
trains a Random Forest, and saves the model to backend/models/.

Usage (run from project root):
    python scripts/train.py
"""
import os
import sys
import numpy as np
from sklearn.model_selection import cross_val_score

# ── make sure we can import from backend/ ────────────────────────────────────
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from backend.utils.feature_extractor import extract_features
from backend.models.classifier       import train
from backend.database.db             import init_db, save_model_info

# ── dataset paths ─────────────────────────────────────────────────────────────
BASE_DIR    = os.path.join(os.path.dirname(__file__), '..')
REAL_DIR    = os.path.join(BASE_DIR, 'dataset', 'real')
FAKE_DIR    = os.path.join(BASE_DIR, 'dataset', 'fake')
AUDIO_EXTS  = {'.wav', '.mp3', '.flac', '.ogg', '.m4a'}


def collect_files(folder: str, label: str):
    files = []
    for fname in os.listdir(folder):
        if os.path.splitext(fname)[1].lower() in AUDIO_EXTS:
            files.append((os.path.join(folder, fname), label))
    return files


def main():
    init_db()
    print("Scanning dataset…")

    real_files = collect_files(REAL_DIR, 'REAL')
    fake_files = collect_files(FAKE_DIR, 'FAKE')
    all_files  = real_files + fake_files

    if len(all_files) == 0:
        print("\n[ERROR] No audio files found in dataset/real/ or dataset/fake/")
        print("Add .wav files to those folders, then re-run this script.")
        sys.exit(1)

    print(f"  REAL samples : {len(real_files)}")
    print(f"  FAKE samples : {len(fake_files)}")
    print(f"  Total        : {len(all_files)}\n")

    X_list, y_list = [], []
    for i, (path, label) in enumerate(all_files, 1):
        try:
            feat = extract_features(path)
            X_list.append(feat)
            y_list.append(label)
            print(f"  [{i}/{len(all_files)}] {os.path.basename(path)} → {label}")
        except Exception as e:
            print(f"  [SKIP] {os.path.basename(path)} — {e}")

    X = np.array(X_list)
    print(f"\nFeature matrix shape: {X.shape}")

    # Cross-validation (5-fold) for honest accuracy estimate
    from sklearn.ensemble import RandomForestClassifier
    from sklearn.preprocessing import LabelEncoder
    le   = LabelEncoder()
    y_enc = le.fit_transform(y_list)
    clf_cv = RandomForestClassifier(n_estimators=200, random_state=42, n_jobs=-1)
    if len(set(y_list)) > 1 and len(y_list) >= 5:
        cv_scores = cross_val_score(clf_cv, X, y_enc, cv=5)
        cv_acc    = float(np.mean(cv_scores))
        print(f"Cross-val accuracy (5-fold): {cv_acc:.2%}")
    else:
        cv_acc = 0.0
        print("Not enough data for cross-validation (need ≥5 files per class)")

    print("\nTraining final model on full dataset…")
    model, encoder, train_acc = train(X, y_list)
    print(f"Training accuracy : {train_acc:.2%}")

    save_model_info('RandomForest_MFCC', cv_acc if cv_acc else train_acc, len(y_list))
    print("\n✅  Model saved to backend/models/rf_model.pkl")
    print("    You can now start the Flask server.")


if __name__ == '__main__':
    main()
