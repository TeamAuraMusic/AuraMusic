#!/usr/bin/env python3
"""Capture focus, Projectivy audio state, and card-surface cadence during a real track."""
import argparse
import json
import re
import shlex
import time
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from pathlib import Path
from tv_adb import (
    DEFAULT_ADB_EXECUTABLE,
    DEFAULT_ADB_SERVER_PORT,
    is_allowed_target,
    run_adb,
)

ADB_EXECUTABLE = DEFAULT_ADB_EXECUTABLE
ADB_SERVER_PORT = DEFAULT_ADB_SERVER_PORT

def adb(serial: str, *args: str, timeout: int = 30) -> str:
    return run_adb(
        serial, *args, executable=ADB_EXECUTABLE, server_port=ADB_SERVER_PORT, timeout=timeout,
    ).replace("\r", "")


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat()


def median(values: list[float]) -> float | None:
    if not values:
        return None
    values = sorted(values)
    middle = len(values) // 2
    return values[middle] if len(values) % 2 else (values[middle - 1] + values[middle]) / 2


def main() -> None:
    global ADB_EXECUTABLE, ADB_SERVER_PORT
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True, help="Explicit ADB serial; no device fallback")
    parser.add_argument("--adb", type=Path, default=ADB_EXECUTABLE, help="Windows ADB executable")
    parser.add_argument("--adb-port", type=int, default=ADB_SERVER_PORT)
    parser.add_argument("--allow-physical", action="store_true", help="Permit non-emulator serials explicitly")
    parser.add_argument("--title", required=True, help="Exact focused Projectivy program title")
    parser.add_argument("--duration-seconds", required=True, type=float, help="Expected source track duration")
    parser.add_argument("--interval-seconds", type=float, default=30)
    parser.add_argument("--post-end-seconds", type=float, default=10)
    parser.add_argument(
        "--playback-started-before-test-seconds", type=float, default=0,
        help="Measured offset when audio started before this sampler (from device player lifecycle logs)",
    )
    parser.add_argument("--evidence-dir", required=True, type=Path)
    args = parser.parse_args()
    if not is_allowed_target(args.serial, args.allow_physical):
        parser.error("serial is outside the emulator allowlist; pass --allow-physical only for the gated Chromecast phase")
    ADB_EXECUTABLE, ADB_SERVER_PORT = args.adb, args.adb_port
    args.evidence_dir.mkdir(parents=True, exist_ok=True)

    started = time.monotonic()
    samples: list[dict] = []
    last_sample = 0.0
    end_after = max(
        0,
        args.duration_seconds + args.post_end_seconds - args.playback_started_before_test_seconds,
    )

    while last_sample <= end_after:
        sample_started = time.monotonic()
        elapsed = sample_started - started
        sample_started_utc = utc_now()
        index = len(samples)
        prefix = args.evidence_dir / f"sample-{index:02d}"

        adb(args.serial, "shell", "uiautomator", "dump", "/sdcard/codex-projectivy-window.xml")
        hierarchy = adb(args.serial, "exec-out", "cat", "/sdcard/codex-projectivy-window.xml")
        (prefix.with_suffix(".xml")).write_text(hierarchy)
        root = ET.fromstring(hierarchy)
        focused_nodes = [
            node.attrib for node in root.iter("node")
            if node.attrib.get("focused") == "true"
        ]
        focused_titles = [
            node.get("text") or node.get("content-desc") or ""
            for node in focused_nodes
        ]
        foreground = adb(args.serial, "shell", "dumpsys", "activity", "activities")
        Path(f"{prefix}-foreground.txt").write_text(foreground)
        projectivy_foreground = bool(re.search(
            r"topResumedActivity=.*com\.spocky\.projengmenu/\.ui\.home\.MainActivity",
            foreground,
        ))

        audio = adb(args.serial, "shell", "dumpsys", "audio")
        audio_observed_elapsed = time.monotonic() - started
        audio_observed_utc = utc_now()
        Path(f"{prefix}-audio.txt").write_text(audio)
        active_audio = [
            line.strip() for line in audio.splitlines()
            if "AudioPlaybackConfiguration" in line
            and "type:android.media.AudioTrack" in line
            and re.search(r"u/pid:10150/\d+ state:started", line)
        ]

        layer_names = adb(args.serial, "shell", "dumpsys", "SurfaceFlinger", "--list").splitlines()
        card_layers = [
            name for name in layer_names
            if "SurfaceView[com.spocky.projengmenu/com.spocky.projengmenu.ui.home.MainActivity](BLAST)#" in name
        ]
        layer_stats = []
        for name in card_layers:
            command = f"dumpsys SurfaceFlinger --latency {shlex.quote(name)}"
            raw = adb(args.serial, "shell", command)
            rows = [
                row.split() for row in raw.splitlines()[1:]
                if len(row.split()) == 3 and row.split()[0].isdigit()
            ]
            timestamps = [int(row[0]) for row in rows if int(row[0]) > 0]
            intervals = [
                (right - left) / 1_000_000
                for left, right in zip(timestamps, timestamps[1:]) if right > left
            ]
            layer_stats.append({
                "name": name,
                "samples": len(timestamps),
                "spanSeconds": (timestamps[-1] - timestamps[0]) / 1_000_000_000 if timestamps else 0,
                "medianIntervalMs": median(intervals),
            })
        video_layers = [
            layer for layer in layer_stats
            if layer["samples"] >= 4 and layer["medianIntervalMs"] is not None
            and 400 <= layer["medianIntervalMs"] <= 600
        ]

        samples.append({
            "index": index,
            "utc": utc_now(),
            "sampleStartedUtc": sample_started_utc,
            "elapsedSeconds": round(elapsed, 3),
            "audioObservedUtc": audio_observed_utc,
            "audioObservedElapsedSeconds": round(audio_observed_elapsed, 3),
            "audioObservedTrackElapsedSeconds": round(
                audio_observed_elapsed + args.playback_started_before_test_seconds, 3
            ),
            "focusedTitles": focused_titles,
            "expectedTitleFocused": args.title in focused_titles,
            "projectivyForeground": projectivy_foreground,
            "activeProjectivyAudioTrackCount": len(active_audio),
            "activeAudioTrackLines": active_audio,
            "videoLayers": layer_stats,
            "twoFpsCardSurfaceObserved": bool(video_layers),
        })
        last_sample = elapsed
        next_sample = min((index + 1) * args.interval_seconds, end_after)
        if elapsed < next_sample:
            time.sleep(next_sample - elapsed)
        elif elapsed >= end_after:
            break

    playback_samples = [
        sample for sample in samples
        if sample["audioObservedTrackElapsedSeconds"] <= args.duration_seconds - 5
    ]
    post_end_samples = [
        sample for sample in samples
        if sample["audioObservedTrackElapsedSeconds"] >= args.duration_seconds + args.post_end_seconds - 2
    ]
    checks = {
        "projectivyStayedForeground": all(sample["projectivyForeground"] for sample in samples),
        "expectedCardStayedFocused": all(sample["expectedTitleFocused"] for sample in samples),
        "audioTrackRemainedStartedThroughExpectedDuration": bool(playback_samples) and all(
            sample["activeProjectivyAudioTrackCount"] > 0 for sample in playback_samples
        ),
        "twoFpsCardSurfaceObservedDuringPlayback": any(
            sample["twoFpsCardSurfaceObserved"] for sample in playback_samples
        ),
        "audioStoppedAfterExpectedTrackEnd": bool(post_end_samples) and all(
            sample["activeProjectivyAudioTrackCount"] == 0 for sample in post_end_samples
        ),
    }
    status = "PASS" if all(checks.values()) else "FAIL_PRODUCT"
    result = {
        "test": "Projectivy focused real-track full-duration continuity",
        "status": status,
        "serial": args.serial,
        "expectedTitle": args.title,
        "expectedDurationSeconds": args.duration_seconds,
        "postEndGraceSeconds": args.post_end_seconds,
        "playbackStartedBeforeSamplerSeconds": args.playback_started_before_test_seconds,
        "startUtc": samples[0]["utc"] if samples else None,
        "endUtc": samples[-1]["utc"] if samples else None,
        "sampleCount": len(samples),
        "samplingIntervalSeconds": args.interval_seconds,
        "checks": checks,
        "samples": samples,
        "limitations": [
            "AudioPlaybackConfiguration verifies the Projectivy output AudioTrack state, not captured audio samples or spectral identity.",
            "A 2 fps SurfaceFlinger presentation cadence plus source-video frame analysis verifies animation delivery; HDMI pixels are not captured by this utility.",
        ],
    }
    destination = args.evidence_dir / "full-track-continuity-result.json"
    destination.write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps({"status": status, "checks": checks, "samples": len(samples), "result": str(destination)}, indent=2))
    if status != "PASS":
        raise SystemExit(1)


if __name__ == "__main__":
    main()
