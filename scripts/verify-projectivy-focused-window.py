#!/usr/bin/env python3
"""Verify a focused AuraMusic card keeps Projectivy audio and its overlay active."""

import argparse
import hashlib
import json
import re
import time
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from pathlib import Path

import cv2
import numpy as np
from tv_adb import (
    DEFAULT_ADB_EXECUTABLE,
    DEFAULT_ADB_SERVER_PORT,
    is_allowed_target,
    run_adb,
)

ADB_EXECUTABLE = DEFAULT_ADB_EXECUTABLE
ADB_SERVER_PORT = DEFAULT_ADB_SERVER_PORT

def adb(serial: str, *args: str, timeout: int = 30, binary: bool = False):
    return run_adb(
        serial, *args, executable=ADB_EXECUTABLE, server_port=ADB_SERVER_PORT,
        timeout=timeout, binary=binary,
    )


def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat()


def main() -> None:
    global ADB_EXECUTABLE, ADB_SERVER_PORT
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True, help="Explicit ADB serial; no device fallback")
    parser.add_argument("--adb", type=Path, default=ADB_EXECUTABLE, help="Windows ADB executable")
    parser.add_argument("--adb-port", type=int, default=ADB_SERVER_PORT)
    parser.add_argument("--allow-physical", action="store_true", help="Permit non-emulator serials explicitly")
    parser.add_argument("--title", required=True, help="Exact focused Projectivy program title")
    parser.add_argument("--seconds", type=float, default=65)
    parser.add_argument("--interval-seconds", type=float, default=5)
    parser.add_argument("--evidence-dir", required=True, type=Path)
    args = parser.parse_args()
    if not is_allowed_target(args.serial, args.allow_physical):
        parser.error("serial is outside the emulator allowlist; pass --allow-physical only for the gated Chromecast phase")
    ADB_EXECUTABLE, ADB_SERVER_PORT = args.adb, args.adb_port
    if args.seconds < 10 or args.interval_seconds <= 0:
        parser.error("seconds must be at least 10 and interval-seconds must be positive")
    args.evidence_dir.mkdir(parents=True, exist_ok=True)

    serial = args.serial
    evidence = args.evidence_dir
    metadata = {
        "serial": serial,
        "startedUtc": utc_now(),
        "title": args.title,
        "requestedSeconds": args.seconds,
        "intervalSeconds": args.interval_seconds,
        "model": adb(serial, "shell", "getprop", "ro.product.model").strip(),
        "device": adb(serial, "shell", "getprop", "ro.product.device").strip(),
        "androidApi": adb(serial, "shell", "getprop", "ro.build.version.sdk").strip(),
        "projectivyPackage": "com.spocky.projengmenu",
        "auraPackage": "com.auramusic.app.debug",
    }
    (evidence / "run-metadata.json").write_text(json.dumps(metadata, indent=2) + "\n")
    (evidence / "programs.txt").write_text(
        adb(serial, "shell", "content", "call", "--uri", "content://com.auramusic.app.debug.tvpreview", "--method", "debugListPrograms")
    )
    samples = []
    started = time.monotonic()
    index = 0
    while True:
        sample_elapsed = time.monotonic() - started
        prefix = evidence / f"sample-{index:02d}"
        hierarchy_path = f"/sdcard/codex-projectivy-focused-window-{index:02d}.xml"
        adb(serial, "shell", "uiautomator", "dump", hierarchy_path, timeout=45)
        hierarchy = adb(serial, "exec-out", "cat", hierarchy_path)
        (prefix.with_suffix(".xml")).write_text(hierarchy)
        root = ET.fromstring(hierarchy)
        focused = []
        card_bounds = None
        for node in root.iter("node"):
            if node.attrib.get("focused") == "true":
                title = node.attrib.get("text") or node.attrib.get("content-desc") or ""
                focused.append(title)
                if title == args.title:
                    card_bounds = node.attrib.get("bounds")

        foreground = adb(serial, "shell", "dumpsys", "activity", "activities")
        (evidence / f"sample-{index:02d}-foreground.txt").write_text(foreground)
        is_foreground = bool(re.search(
            r"topResumedActivity=.*com\.spocky\.projengmenu/\.ui\.home\.MainActivity", foreground
        ))

        audio = adb(serial, "shell", "dumpsys", "audio")
        (evidence / f"sample-{index:02d}-audio.txt").write_text(audio)
        active_tracks = [line.strip() for line in audio.splitlines()
                         if "AudioPlaybackConfiguration" in line
                         and "type:android.media.AudioTrack" in line
                         and re.search(r"u/pid:10150/\d+ state:started", line)]

        raw_screenshot = adb(serial, "exec-out", "screencap", "-p", binary=True)
        screenshot_path = evidence / f"sample-{index:02d}.png"
        screenshot_path.write_bytes(raw_screenshot)
        image = cv2.imdecode(np.frombuffer(raw_screenshot, np.uint8), cv2.IMREAD_COLOR)
        pink_pixels = 0
        profile = []
        roi = None
        if image is not None and card_bounds:
            match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", card_bounds)
            if match:
                left, top, right, bottom = map(int, match.groups())
                x = max(0, round(left + (right - left) * 0.22))
                y = max(0, round(top + (bottom - top) * 0.64))
                width = min(image.shape[1] - x, round((right - left) * 0.56))
                height = min(image.shape[0] - y, round((bottom - top) * 0.34))
                crop = image[y:y + height, x:x + width]
                if crop.size:
                    blue, green, red = cv2.split(crop)
                    pink = (red > 150) & (red > green * 1.35) & (blue > 60) & (green < 150)
                    pink_pixels = int(pink.sum())
                    profile = pink.sum(axis=1).astype(int).tolist()
                    roi = {"x": x, "y": y, "width": width, "height": height}

        samples.append({
            "index": index,
            "elapsedSeconds": round(sample_elapsed, 3),
            "utc": utc_now(),
            "focusedTitles": focused,
            "expectedTitleFocused": args.title in focused,
            "focusedCardBounds": card_bounds,
            "projectivyForeground": is_foreground,
            "activeProjectivyAudioTrackCount": len(active_tracks),
            "activeProjectivyAudioTracks": active_tracks,
            "pinkIndicatorPixels": pink_pixels,
            "pinkRowProfile": profile,
            "indicatorRegion": roi,
            "screenshot": screenshot_path.name,
            "screenshotSha256": hashlib.sha256(raw_screenshot).hexdigest(),
        })
        index += 1
        next_at = index * args.interval_seconds
        if next_at > args.seconds:
            break
        time.sleep(max(0, started + next_at - time.monotonic()))

    end = time.monotonic()
    logs = adb(serial, "logcat", "-d", "-s", "TvPreviewFocusAccessibilityService:D", timeout=45)
    (evidence / "focus-service-logcat.txt").write_text(logs)
    profiles = [sample["pinkRowProfile"] for sample in samples if sample["pinkIndicatorPixels"] > 100]
    transitions = [sum(abs(a - b) for a, b in zip(left, right))
                   for left, right in zip(profiles, profiles[1:])]
    checks = {
        "requestedWindowCaptured": end - started >= args.seconds,
        "expectedCardStayedFocused": bool(samples) and all(s["expectedTitleFocused"] for s in samples),
        "projectivyStayedForeground": bool(samples) and all(s["projectivyForeground"] for s in samples),
        "audioTrackStayedActive": bool(samples) and all(s["activeProjectivyAudioTrackCount"] > 0 for s in samples),
        "indicatorStayedVisible": bool(samples) and len(profiles) == len(samples),
        "indicatorChangedAcrossSamples": len(transitions) >= 2 and any(delta > 100 for delta in transitions),
    }
    status = "PASS" if all(checks.values()) else "FAIL_PRODUCT"
    result = {
        "test": "Projectivy focused AuraMusic active playback window",
        "status": status,
        "scope": "Focus, foreground, Projectivy AudioTrack state, and animated overlay pixels over a bounded window",
        **metadata,
        "actualElapsedSeconds": round(end - started, 3),
        "sampleCount": len(samples),
        "checks": checks,
        "waveformTransitionProfileDeltas": transitions,
        "samples": samples,
        "limitations": [
            "AudioPlaybackConfiguration confirms an active Projectivy output AudioTrack; it does not capture or identify acoustic samples.",
            "ADB screenshots are device-composed frames, not an HDMI capture.",
        ],
    }
    (evidence / "focused-window-result.json").write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps({"status": status, "actualElapsedSeconds": result["actualElapsedSeconds"],
                      "sampleCount": len(samples), "checks": checks,
                      "result": str(evidence / "focused-window-result.json")}, indent=2))
    if status != "PASS":
        raise SystemExit(1)


if __name__ == "__main__":
    main()
