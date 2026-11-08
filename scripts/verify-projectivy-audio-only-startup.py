#!/usr/bin/env python3
"""Measure audio-only preview startup on an already-published Projectivy card."""

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


def main() -> None:
    global ADB_EXECUTABLE, ADB_SERVER_PORT
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True, help="Explicit emulator serial; physical targets require opt-in")
    parser.add_argument("--adb", type=Path, default=ADB_EXECUTABLE, help="Windows ADB executable")
    parser.add_argument("--adb-port", type=int, default=ADB_SERVER_PORT)
    parser.add_argument("--allow-physical", action="store_true", help="Permit non-emulator serials explicitly")
    parser.add_argument("--title", required=True)
    parser.add_argument("--key", choices=("LEFT", "RIGHT"), required=True)
    parser.add_argument("--timeout-seconds", type=float, default=60)
    parser.add_argument("--poll-seconds", type=float, default=0.5)
    parser.add_argument("--evidence-dir", required=True, type=Path)
    args = parser.parse_args()
    if not is_allowed_target(args.serial, args.allow_physical):
        parser.error("serial is outside the emulator allowlist; pass --allow-physical only for the gated Chromecast phase")
    ADB_EXECUTABLE, ADB_SERVER_PORT = args.adb, args.adb_port
    args.evidence_dir.mkdir(parents=True, exist_ok=True)
    serial = args.serial

    adb(serial, "shell", "uiautomator", "dump", "/sdcard/audio-only-before.xml", timeout=45)
    before_xml = adb(serial, "exec-out", "cat", "/sdcard/audio-only-before.xml")
    (args.evidence_dir / "before.xml").write_text(before_xml)
    before_root = ET.fromstring(before_xml)
    before_title = next((node.get("text") or node.get("content-desc") or ""
                         for node in before_root.iter("node") if node.get("focused") == "true"), "")
    before_audio = adb(serial, "shell", "dumpsys", "audio")
    (args.evidence_dir / "before-audio.txt").write_text(before_audio)
    active_pattern = re.compile(r"AudioPlaybackConfiguration piid:(\d+).*?u/pid:10150/\d+ state:started")
    baseline_ids = active_pattern.findall(before_audio)

    started_utc = datetime.now(timezone.utc).isoformat()
    started = time.monotonic()
    adb(serial, "shell", "input", "keyevent", f"KEYCODE_DPAD_{args.key}")
    observations = []
    new_audio = None
    audio_text = ""
    while time.monotonic() - started <= args.timeout_seconds:
        audio_text = adb(serial, "shell", "dumpsys", "audio")
        tracks = active_pattern.findall(audio_text)
        observations.append({
            "elapsedSeconds": round(time.monotonic() - started, 3),
            "activeProjectivyTrackIds": tracks,
        })
        new_ids = [track_id for track_id in tracks if track_id not in baseline_ids]
        if new_ids:
            new_audio = new_ids[0]
            break
        time.sleep(args.poll_seconds)

    elapsed = time.monotonic() - started
    (args.evidence_dir / "audio-first-start.txt").write_text(audio_text)
    hierarchy_path = "/sdcard/audio-only-after.xml"
    adb(serial, "shell", "uiautomator", "dump", hierarchy_path, timeout=45)
    hierarchy = adb(serial, "exec-out", "cat", hierarchy_path)
    (args.evidence_dir / "after.xml").write_text(hierarchy)
    root = ET.fromstring(hierarchy)
    focused = [node.attrib for node in root.iter("node") if node.attrib.get("focused") == "true"]
    focused_titles = [node.get("text") or node.get("content-desc") or "" for node in focused]
    matching = next((node for node in focused if (node.get("text") or node.get("content-desc")) == args.title), None)
    foreground = adb(serial, "shell", "dumpsys", "activity", "activities")
    (args.evidence_dir / "after-foreground.txt").write_text(foreground)
    is_foreground = bool(re.search(
        r"topResumedActivity=.*com\.spocky\.projengmenu/\.ui\.home\.MainActivity", foreground
    ))
    image_bytes = adb(serial, "exec-out", "screencap", "-p", binary=True)
    screenshot = args.evidence_dir / "after.png"
    screenshot.write_bytes(image_bytes)
    image = cv2.imdecode(np.frombuffer(image_bytes, np.uint8), cv2.IMREAD_COLOR)
    pink_pixels = 0
    bounds = matching.get("bounds") if matching else None
    if image is not None and bounds:
        parsed = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
        if parsed:
            left, top, right, bottom = map(int, parsed.groups())
            x = max(0, round(left + (right - left) * 0.22))
            y = max(0, round(top + (bottom - top) * 0.64))
            width = min(image.shape[1] - x, round((right - left) * 0.56))
            height = min(image.shape[0] - y, round((bottom - top) * 0.34))
            crop = image[y:y + height, x:x + width]
            if crop.size:
                blue, green, red = cv2.split(crop)
                pink = (red > 150) & (red > green * 1.35) & (blue > 60) & (green < 150)
                pink_pixels = int(pink.sum())
    checks = {
        "targetCardFocused": matching is not None,
        "projectivyStayedForeground": is_foreground,
        "newProjectivyAudioTrackStarted": new_audio is not None,
        "pinkIndicatorVisibleInsideCard": pink_pixels > 100,
    }
    status = "PASS" if all(checks.values()) else ("FAIL_PRODUCT" if matching is not None else "INCONCLUSIVE")
    result = {
        "test": "Projectivy audio-only real-song preview cold focus start",
        "status": status,
        "serial": serial,
        "title": args.title,
        "input": f"DPAD_{args.key}; no Select",
        "startedUtc": started_utc,
        "beforeFocusedTitle": before_title,
        "baselineProjectivyTrackIds": baseline_ids,
        "newProjectivyTrackId": new_audio,
        "audioStartLatencySecondsUpperBound": round(elapsed, 3) if new_audio is not None else None,
        "pollIntervalSeconds": args.poll_seconds,
        "activeTrackObservations": observations,
        "focusedTitlesAfter": focused_titles,
        "projectivyForeground": is_foreground,
        "pinkIndicatorPixels": pink_pixels,
        "checks": checks,
        "screenshot": screenshot.name,
        "screenshotSha256": hashlib.sha256(image_bytes).hexdigest(),
        "limitations": [
            "AudioPlaybackConfiguration identifies Projectivy's started AudioTrack, not an audio sample or song fingerprint.",
            "The run uses a debuggable-only audio-only provider mode; it does not change normal published preview URIs.",
            "The focus overlay is an AccessibilityService window and requires its existing enabled setting.",
        ],
    }
    (args.evidence_dir / "audio-only-startup-result.json").write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps({"status": status, "latencyUpperBoundSeconds": result["audioStartLatencySecondsUpperBound"],
                      "checks": checks, "result": str(args.evidence_dir / "audio-only-startup-result.json")}, indent=2))
    if status != "PASS":
        raise SystemExit(1)


if __name__ == "__main__":
    main()
