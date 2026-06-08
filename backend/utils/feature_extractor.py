"""
backend/utils/feature_extractor.py
Extracts MFCC + delta features from an audio file.
Returns a flat numpy array ready for the classifier.
"""
import librosa
import numpy as np


# ── tuneable constants ────────────────────────────────────────────────────────
SAMPLE_RATE   = 22050
DURATION      = 5          # seconds – clips/pads every file to this length
N_MFCC        = 40         # number of MFCC coefficients
# ─────────────────────────────────────────────────────────────────────────────


def _load_audio(path: str) -> np.ndarray:
    """Load audio, resample, and force mono. Pad or trim to DURATION seconds."""
    y, sr = librosa.load(path, sr=SAMPLE_RATE, mono=True, duration=DURATION)
    target_len = SAMPLE_RATE * DURATION
    if len(y) < target_len:
        y = np.pad(y, (0, target_len - len(y)))
    else:
        y = y[:target_len]
    return y


def extract_features(path: str) -> np.ndarray:
    """
    Returns a 1-D numpy array of shape (N_MFCC * 3,) containing:
      - mean of each MFCC coefficient        (N_MFCC values)
      - mean of each delta-MFCC coefficient  (N_MFCC values)
      - mean of each delta2-MFCC coefficient (N_MFCC values)
    """
    y = _load_audio(path)

    mfcc   = librosa.feature.mfcc(y=y, sr=SAMPLE_RATE, n_mfcc=N_MFCC)
    delta  = librosa.feature.delta(mfcc)
    delta2 = librosa.feature.delta(mfcc, order=2)

    # mean across time axis → each has shape (N_MFCC,)
    feat = np.concatenate([
        np.mean(mfcc,   axis=1),
        np.mean(delta,  axis=1),
        np.mean(delta2, axis=1),
    ])
    return feat          # shape: (120,)


def extract_waveform_data(path: str, max_points: int = 2000) -> list:
    """Return downsampled waveform amplitudes for the frontend chart."""
    y = _load_audio(path)
    step = max(1, len(y) // max_points)
    return y[::step].tolist()


def extract_spectrogram_data(path: str) -> dict:
    """
    Return mel-spectrogram data (dB) as a 2-D list + axis info.
    Kept small enough to send as JSON.
    """
    y = _load_audio(path)
    S = librosa.feature.melspectrogram(y=y, sr=SAMPLE_RATE, n_mels=64)
    S_db = librosa.power_to_db(S, ref=np.max)

    # Downsample time axis to ≤200 frames for reasonable JSON size
    max_frames = 200
    if S_db.shape[1] > max_frames:
        step = S_db.shape[1] // max_frames
        S_db = S_db[:, ::step]

    return {
        "data":       S_db.tolist(),
        "n_mels":     S_db.shape[0],
        "n_frames":   S_db.shape[1],
        "sample_rate": SAMPLE_RATE,
        "duration":   DURATION,
    }
