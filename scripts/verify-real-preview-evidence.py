#!/usr/bin/env python3
"""Check captured Projectivy playback evidence for two real AuraMusic tracks."""
import argparse
import hashlib
import json
import math
import re
import subprocess
import wave
from pathlib import Path

import numpy as np


def audio_stats(path: Path) -> dict:
    with wave.open(str(path), "rb") as audio:
        channels = audio.getnchannels()
        rate = audio.getframerate()
        samples = np.frombuffer(audio.readframes(audio.getnframes()), dtype=np.int16)
    samples = samples.reshape(-1, channels).astype(np.float64).mean(axis=1)
    rms = float(np.sqrt(np.mean(samples * samples))) if len(samples) else 0.0
    peak = float(np.max(np.abs(samples))) if len(samples) else 0.0
    return {
        "file": str(path),
        "durationSeconds": round(len(samples) / rate, 3),
        "sampleRateHz": rate,
        "rms": round(rms, 3),
        "rmsDbfs": round(20 * math.log10(max(rms, 1e-9) / 32768), 2),
        "peak": round(peak, 3),
        "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
    }


def log_identity(path: Path, title: str) -> dict:
    content = path.read_text(errors="replace")
    escaped = re.escape(title)
    match = re.search(
        rf"TV preview selected program title={escaped} contentIdHash=([0-9a-f]+)",
        content,
    )
    return {"file": str(path), "selectedTitle": title, "contentIdHash": match.group(1) if match else None}


def video_stats(path: Path) -> dict:
    probe = subprocess.run(
        ["ffprobe", "-v", "error", "-show_entries", "stream=codec_type,duration,nb_frames", "-of", "json", str(path)],
        check=True,
        capture_output=True,
        text=True,
    )
    stream = next(s for s in json.loads(probe.stdout)["streams"] if s["codec_type"] == "video")
    # Fixed ROI covers Projectivy card's lower-center playback bars at 1920x1080.
    decoded = subprocess.run(
        ["ffmpeg", "-v", "error", "-i", str(path), "-vf", "crop=130:70:230:720", "-an", "-f", "rawvideo", "-pix_fmt", "rgb24", "pipe:1"],
        check=True,
        capture_output=True,
    ).stdout
    frame_size = 130 * 70 * 3
    frames = [decoded[i : i + frame_size] for i in range(0, len(decoded), frame_size) if len(decoded[i : i + frame_size]) == frame_size]
    hashes = {hashlib.md5(frame).hexdigest() for frame in frames}
    return {
        "file": str(path),
        "durationSeconds": float(stream.get("duration") or 0),
        "frames": int(stream.get("nb_frames") or len(frames)),
        "cardIndicatorRoi": {"x": 230, "y": 720, "width": 130, "height": 70},
        "indicatorFrameCount": len(frames),
        "uniqueIndicatorFrames": len(hashes),
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dance-audio", type=Path, required=True)
    parser.add_argument("--no-grave-audio", type=Path, required=True)
    parser.add_argument("--focus-loss-audio", type=Path, required=True)
    parser.add_argument("--dance-video", type=Path, required=True)
    parser.add_argument("--no-grave-video", type=Path, required=True)
    parser.add_argument("--dance-log", type=Path, required=True)
    parser.add_argument("--no-grave-log", type=Path, required=True)
    parser.add_argument("--no-grave-hierarchy", type=Path, required=True)
    parser.add_argument("--foreground", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()

    audio = {
        "danceTilYouDrop": audio_stats(args.dance_audio),
        "noGraveNoGlory": audio_stats(args.no_grave_audio),
        "focusLoss": audio_stats(args.focus_loss_audio),
    }
    identity = {
        "danceTilYouDrop": log_identity(args.dance_log, "Dance 'Til You Drop"),
        "noGraveNoGlory": log_identity(args.no_grave_log, "No Grave, No Glory"),
    }
    videos = {"danceTilYouDrop": video_stats(args.dance_video), "noGraveNoGlory": video_stats(args.no_grave_video)}
    hierarchy = args.no_grave_hierarchy.read_text(errors="replace")
    foreground = args.foreground.read_text(errors="replace")
    checks = {
        "distinctProgramIdentity": bool(identity["danceTilYouDrop"]["contentIdHash"])
        and bool(identity["noGraveNoGlory"]["contentIdHash"])
        and identity["danceTilYouDrop"]["contentIdHash"] != identity["noGraveNoGlory"]["contentIdHash"],
        "bothTracksProducedDeviceAudio": audio["danceTilYouDrop"]["rms"] > 1 and audio["noGraveNoGlory"]["rms"] > 1,
        "focusLossWasSilent": audio["focusLoss"]["rms"] < 1 and audio["focusLoss"]["peak"] < 2,
        "danceIndicatorChangedInsideCard": videos["danceTilYouDrop"]["uniqueIndicatorFrames"] >= 3,
        "noGraveIndicatorChangedInsideCard": videos["noGraveNoGlory"]["uniqueIndicatorFrames"] >= 3,
        "noGraveCardFocusedInHierarchy": 'content-desc="No Grave, No Glory"' in hierarchy and re.search(r'content-desc="No Grave, No Glory"[^>]*focused="true"', hierarchy) is not None,
        "projectivyForeground": "com.spocky.projengmenu/.ui.home.MainActivity" in foreground,
    }
    result = {
        "test": "AuraMusic real-song Projectivy preview focus and focus-loss evidence",
        "outcome": "PASS" if all(checks.values()) else "INCONCLUSIVE",
        "tracks": identity,
        "audio": audio,
        "video": videos,
        "checks": checks,
        "limitations": [
            "Audio recordings have low captured level on the emulator; nonzero audio and distinct selected program IDs are confirmed.",
            "Captured video lengths reflect available Android screen frames; full 60-120 second continuity is not covered by this result.",
        ],
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps(result, indent=2))
    if result["outcome"] != "PASS":
        raise SystemExit(1)


if __name__ == "__main__":
    main()
