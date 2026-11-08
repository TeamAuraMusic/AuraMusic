#!/usr/bin/env python3
"""Analyze scrcpy WAV captures and emit machine-readable PreviewProbe assertions."""
import argparse
import json
import math
import wave
from pathlib import Path

import numpy as np


def read_wav(path: Path) -> tuple[np.ndarray, int]:
    with wave.open(str(path), "rb") as handle:
        if handle.getsampwidth() != 2:
            raise ValueError(f"{path}: expected 16-bit PCM")
        rate = handle.getframerate()
        channels = handle.getnchannels()
        data = np.frombuffer(handle.readframes(handle.getnframes()), dtype=np.int16)
    return data.reshape(-1, channels).astype(np.float64).mean(axis=1), rate


def analyze_tone(path: Path, expected_hz: int) -> dict:
    samples, rate = read_wav(path)
    rms = float(np.sqrt(np.mean(samples * samples))) if len(samples) else 0.0
    windows = []
    for start in range(rate, len(samples) - rate + 1, rate):
        chunk = samples[start : start + rate]
        spectrum = np.abs(np.fft.rfft(chunk * np.hanning(len(chunk))))
        freqs = np.fft.rfftfreq(len(chunk), 1 / rate)
        peak_index = int(np.argmax(spectrum[1:]) + 1)
        peak_hz = float(freqs[peak_index])
        expected_index = int(np.argmin(np.abs(freqs - expected_hz)))
        competing_index = int(np.argmin(np.abs(freqs - (880 if expected_hz == 440 else 440))))
        expected_mag = float(spectrum[expected_index])
        competing_mag = float(spectrum[competing_index])
        windows.append({
            "second": start // rate,
            "peakHz": round(peak_hz, 2),
            "expectedMagnitude": round(expected_mag, 1),
            "otherFixtureMagnitude": round(competing_mag, 1),
            "expectedDominates": expected_mag > competing_mag * 4,
            "expectedPeak": abs(peak_hz - expected_hz) <= 2,
        })
    passing = sum(w["expectedDominates"] and w["expectedPeak"] for w in windows)
    median_peak = float(np.median([w["peakHz"] for w in windows])) if windows else 0.0
    dbfs = 20 * math.log10(max(rms, 1e-9) / 32768)
    return {
        "file": str(path),
        "durationSeconds": round(len(samples) / rate, 3),
        "sampleRateHz": rate,
        "rms": round(rms, 2),
        "rmsDbfs": round(dbfs, 2),
        "expectedHz": expected_hz,
        "medianPeakHz": round(median_peak, 2),
        "matchingWindows": passing,
        "windows": len(windows),
        "status": "PASS" if len(windows) >= 3 and passing / len(windows) >= 0.8 and dbfs > -70 else "FAIL_PRODUCT",
    }


def analyze_silence(path: Path) -> dict:
    samples, rate = read_wav(path)
    rms = float(np.sqrt(np.mean(samples * samples))) if len(samples) else 0.0
    peak = float(np.max(np.abs(samples))) if len(samples) else 0.0
    return {
        "file": str(path),
        "durationSeconds": round(len(samples) / rate, 3),
        "sampleRateHz": rate,
        "rms": round(rms, 3),
        "peak": round(peak, 3),
        "status": "PASS" if rms < 1 and peak < 2 else "FAIL_PRODUCT",
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--a", type=Path, required=True)
    parser.add_argument("--b", type=Path, required=True)
    parser.add_argument("--focus-loss", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    result = {
        "test": "Projectivy PreviewProbe card audio and focus-loss assertions",
        "outcome": "PASS",
        "assertions": {
            "focusA440Hz": analyze_tone(args.a, 440),
            "focusB880Hz": analyze_tone(args.b, 880),
            "focusLossSilence": analyze_silence(args.focus_loss),
        },
    }
    if any(value["status"] != "PASS" for value in result["assertions"].values()):
        result["outcome"] = "FAIL_PRODUCT"
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps(result, indent=2))
    if result["outcome"] != "PASS":
        raise SystemExit(1)


if __name__ == "__main__":
    main()
