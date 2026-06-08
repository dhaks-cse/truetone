"""
backend/models/classifier.py
Wraps the Random Forest model: train, save, load, predict.
"""
import os
import joblib
import numpy as np
from sklearn.ensemble import RandomForestClassifier
from sklearn.preprocessing import LabelEncoder

MODEL_PATH   = os.path.join(os.path.dirname(__file__), 'rf_model.pkl')
ENCODER_PATH = os.path.join(os.path.dirname(__file__), 'label_encoder.pkl')


def train(X: np.ndarray, y_raw: list, n_estimators: int = 200):
    """
    Train a Random Forest on feature matrix X and string labels y_raw.
    Saves model + encoder to disk.
    Returns (model, encoder, accuracy_on_full_set).
    """
    le = LabelEncoder()
    y  = le.fit_transform(y_raw)        # 'FAKE'→0, 'REAL'→1 (alphabetical)

    clf = RandomForestClassifier(
        n_estimators=n_estimators,
        max_depth=None,
        min_samples_split=4,
        random_state=42,
        n_jobs=-1,
    )
    clf.fit(X, y)

    joblib.dump(clf, MODEL_PATH)
    joblib.dump(le,  ENCODER_PATH)

    train_acc = clf.score(X, y)
    return clf, le, train_acc


def load():
    """Load model + encoder from disk. Returns (model, encoder)."""
    if not os.path.exists(MODEL_PATH):
        raise FileNotFoundError(
            "Model not found. Run  python scripts/train.py  first."
        )
    clf = joblib.load(MODEL_PATH)
    le  = joblib.load(ENCODER_PATH)
    return clf, le


def predict(features: np.ndarray):
    """
    Returns (label: str, confidence: float).
    confidence = probability of the predicted class.
    """
    clf, le = load()
    feat_2d = features.reshape(1, -1)
    proba   = clf.predict_proba(feat_2d)[0]
    class_idx   = int(np.argmax(proba))
    confidence  = float(proba[class_idx])
    label       = le.inverse_transform([class_idx])[0]   # 'REAL' or 'FAKE'
    return label, confidence
