#!/usr/bin/env python3
"""Analyze scrcpy WAV captures and emit machine-readable PreviewProbe assertions."""
import argparse
import json
import math
import wave
from pathlib import Path

import numpy as np

MIN_TONE_WINDOWS = 3
MIN_SILENCE_SECONDS = 3.0
TRANSITION_WINDOW_SECONDS = 0.5
MIN_TRANSITION_STABLE_WINDOWS = 3


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
    duration_seconds = len(samples) / rate
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
    if len(windows) < MIN_TONE_WINDOWS:
        status = "INCONCLUSIVE"
    else:
        status = "PASS" if passing / len(windows) >= 0.8 and dbfs > -70 else "FAIL_PRODUCT"
    return {
        "file": str(path),
        "durationSeconds": round(duration_seconds, 3),
        "sampleRateHz": rate,
        "rms": round(rms, 2),
        "rmsDbfs": round(dbfs, 2),
        "expectedHz": expected_hz,
        "medianPeakHz": round(median_peak, 2),
        "matchingWindows": passing,
        "windows": len(windows),
        "status": status,
    }


def analyze_silence(path: Path) -> dict:
    samples, rate = read_wav(path)
    rms = float(np.sqrt(np.mean(samples * samples))) if len(samples) else 0.0
    peak = float(np.max(np.abs(samples))) if len(samples) else 0.0
    duration_seconds = len(samples) / rate
    if duration_seconds < MIN_SILENCE_SECONDS:
        status = "INCONCLUSIVE"
    else:
        status = "PASS" if rms < 1 and peak < 2 else "FAIL_PRODUCT"
    return {
        "file": str(path),
        "durationSeconds": round(duration_seconds, 3),
        "sampleRateHz": rate,
        "rms": round(rms, 3),
        "peak": round(peak, 3),
        "status": status,
    }


def analyze_tone_transition(path: Path, first_hz: int = 440, second_hz: int = 880) -> dict:
    samples, rate = read_wav(path)
    window_size = max(1, round(rate * TRANSITION_WINDOW_SECONDS))
    windows = []
    for start in range(0, len(samples) - window_size + 1, window_size):
        chunk = samples[start : start + window_size]
        rms = float(np.sqrt(np.mean(chunk * chunk))) if len(chunk) else 0.0
        dbfs = 20 * math.log10(max(rms, 1e-9) / 32768)
        spectrum = np.abs(np.fft.rfft(chunk * np.hanning(len(chunk))))
        frequencies = np.fft.rfftfreq(len(chunk), 1 / rate)
        peak_index = int(np.argmax(spectrum[1:]) + 1) if len(spectrum) > 1 else 0
        peak_hz = float(frequencies[peak_index]) if len(frequencies) else 0.0
        first_index = int(np.argmin(np.abs(frequencies - first_hz)))
        second_index = int(np.argmin(np.abs(frequencies - second_hz)))
        first_magnitude = float(spectrum[first_index])
        second_magnitude = float(spectrum[second_index])

        if dbfs <= -70:
            state = "SILENCE"
        elif abs(peak_hz - first_hz) <= 3 and first_magnitude > second_magnitude * 4:
            state = "FIRST"
        elif abs(peak_hz - second_hz) <= 3 and second_magnitude > first_magnitude * 4:
            state = "SECOND"
        elif min(first_magnitude, second_magnitude) > 0 and max(first_magnitude, second_magnitude) <= min(
            first_magnitude, second_magnitude
        ) * 4:
            state = "MIXED"
        else:
            state = "OTHER"
        windows.append({
            "startSeconds": round(start / rate, 3),
            "state": state,
            "peakHz": round(peak_hz, 2),
            "rmsDbfs": round(dbfs, 2),
            "firstMagnitude": round(first_magnitude, 1),
            "secondMagnitude": round(second_magnitude, 1),
        })

    first_start = next((index for index, item in enumerate(windows) if item["state"] == "FIRST"), None)
    second_start = next((index for index, item in enumerate(windows)
                         if first_start is not None and index > first_start and item["state"] == "SECOND"), None)
    if first_start is None or second_start is None:
        status = "INCONCLUSIVE"
        reason = "Could not observe both expected tones in order"
        first_stable = second_stable = 0
        boundary_states = []
    else:
        first_end = first_start
        while first_end + 1 < second_start and windows[first_end + 1]["state"] == "FIRST":
            first_end += 1
        second_end = second_start
        while second_end + 1 < len(windows) and windows[second_end + 1]["state"] == "SECOND":
            second_end += 1
        first_stable = first_end - first_start + 1
        second_stable = second_end - second_start + 1
        boundary_states = [item["state"] for item in windows[first_end + 1 : second_start]]
        stale_first_after_second = any(item["state"] == "FIRST" for item in windows[second_start + 1 :])
        mixed_count = boundary_states.count("MIXED")
        silence_count = boundary_states.count("SILENCE")
        other_count = boundary_states.count("OTHER")

        if stale_first_after_second or mixed_count > 1:
            status = "FAIL_PRODUCT"
            reason = "Stale first tone or sustained mixed-tone overlap observed"
        elif first_stable < MIN_TRANSITION_STABLE_WINDOWS or second_stable < MIN_TRANSITION_STABLE_WINDOWS:
            status = "INCONCLUSIVE"
            reason = "Expected tone was not sustained for enough windows"
        elif mixed_count or silence_count > 1 or other_count:
            status = "INCONCLUSIVE"
            reason = "Transition boundary is mixed, interrupted, or contains unexpected audio"
        else:
            status = "PASS"
            reason = "Observed sustained first then second tone with no mixed-tone window"

    return {
        "file": str(path),
        "durationSeconds": round(len(samples) / rate, 3),
        "sampleRateHz": rate,
        "firstHz": first_hz,
        "secondHz": second_hz,
        "windowSeconds": TRANSITION_WINDOW_SECONDS,
        "minimumStableWindowsPerTone": MIN_TRANSITION_STABLE_WINDOWS,
        "firstStableWindows": first_stable,
        "secondStableWindows": second_stable,
        "boundaryStates": boundary_states,
        "status": status,
        "reason": reason,
        "windows": windows,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--a", type=Path, help="Standalone A capture (440 Hz)")
    parser.add_argument("--b", type=Path, help="Standalone B capture (880 Hz)")
    parser.add_argument("--focus-loss", type=Path, help="Standalone silence capture after focus leaves")
    parser.add_argument("--transition", type=Path,
                        help="Optional continuous A→B WAV capture; mixed or ambiguous handoff does not pass")
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    separate_captures = (args.a, args.b, args.focus_loss)
    if any(separate_captures) and not all(separate_captures):
        parser.error("--a, --b, and --focus-loss must be supplied together")
    if not args.transition and not all(separate_captures):
        parser.error("provide either all of --a/--b/--focus-loss or a --transition capture")
    assertions = {}
    if all(separate_captures):
        assertions.update({
            "focusA440Hz": analyze_tone(args.a, 440),
            "focusB880Hz": analyze_tone(args.b, 880),
            "focusLossSilence": analyze_silence(args.focus_loss),
        })
    if args.transition:
        assertions["focusAToBNoOverlap"] = analyze_tone_transition(args.transition)
    result = {
        "test": "Projectivy PreviewProbe card audio and focus-loss assertions",
        "outcome": "PASS",
        "assertions": assertions,
    }
    statuses = {value["status"] for value in result["assertions"].values()}
    if "FAIL_PRODUCT" in statuses:
        result["outcome"] = "FAIL_PRODUCT"
    elif "INCONCLUSIVE" in statuses:
        result["outcome"] = "INCONCLUSIVE"
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps(result, indent=2))
    if result["outcome"] != "PASS":
        raise SystemExit(1)


if __name__ == "__main__":
    main()
