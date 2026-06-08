"""
scripts/generate_sample_data.py
────────────────────────────────────────────────────────────────────
Generates synthetic WAV files so you can train and test the model
WITHOUT a real dataset. Replace these files with genuine real/fake
voice recordings for production use.

Usage (from project root):
    python scripts/generate_sample_data.py
"""
import os
import sys
import numpy as np
import soundfile as sf

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

REAL_DIR = os.path.join(os.path.dirname(__file__), '..', 'dataset', 'real')
FAKE_DIR = os.path.join(os.path.dirname(__file__), '..', 'dataset', 'fake')
SR       = 22050
DUR      = 5          # seconds
N_EACH   = 20         # files per class


def sine_wave(freq, sr, dur, amp=0.5):
    t = np.linspace(0, dur, int(sr * dur), endpoint=False)
    return (amp * np.sin(2 * np.pi * freq * t)).astype(np.float32)


def human_like_voice(sr, dur):
    """Multi-harmonic + noise + pitch jitter → mimics a real voice."""
    t   = np.linspace(0, dur, int(sr * dur), endpoint=False)
    f0  = np.random.uniform(100, 200)          # fundamental
    sig = np.zeros_like(t)
    for h in range(1, 8):                      # harmonics
        amp = 0.5 / h + np.random.normal(0, 0.02)
        sig += amp * np.sin(2 * np.pi * f0 * h * t)
    sig += np.random.normal(0, 0.02, size=len(t))   # breath noise
    sig /= np.max(np.abs(sig) + 1e-6)
    return (sig * 0.8).astype(np.float32)


def ai_like_voice(sr, dur):
    """Cleaner tones + subtle periodicity artifacts → mimics TTS."""
    t   = np.linspace(0, dur, int(sr * dur), endpoint=False)
    f0  = np.random.uniform(120, 220)
    sig = np.zeros_like(t)
    for h in range(1, 6):
        sig += (0.6 / h) * np.sin(2 * np.pi * f0 * h * t)
    # Very slight noise — TTS is typically cleaner
    sig += np.random.normal(0, 0.005, size=len(t))
    # Slight AM modulation (artifact pattern)
    mod  = 1 + 0.1 * np.sin(2 * np.pi * 3 * t)
    sig  = sig * mod
    sig /= np.max(np.abs(sig) + 1e-6)
    return (sig * 0.8).astype(np.float32)


def main():
    os.makedirs(REAL_DIR, exist_ok=True)
    os.makedirs(FAKE_DIR, exist_ok=True)

    print(f"Generating {N_EACH} REAL samples…")
    for i in range(N_EACH):
        data = human_like_voice(SR, DUR)
        path = os.path.join(REAL_DIR, f"real_{i+1:03d}.wav")
        sf.write(path, data, SR)
        print(f"  {path}")

    print(f"\nGenerating {N_EACH} FAKE samples…")
    for i in range(N_EACH):
        data = ai_like_voice(SR, DUR)
        path = os.path.join(FAKE_DIR, f"fake_{i+1:03d}.wav")
        sf.write(path, data, SR)
        print(f"  {path}")

    print(f"\n✅  Done. {N_EACH*2} files created.")
    print("    Now run:  python scripts/train.py")


if __name__ == '__main__':
    main()
