#!/usr/bin/env python3
"""Verify Projectivy's waveform hides when a focused real AuraMusic preview ends."""

import argparse
import hashlib
import json
import re
import subprocess
import sys
import time
from datetime import datetime, timezone
from pathlib import Path
import xml.etree.ElementTree as ET

import cv2
import numpy as np


ADB = "/home/nos/android-sdk/platform-tools/adb"
PROJECTIVY = "com.spocky.projengmenu"
APP = "com.auramusic.app.debug"
PINK_ROI = (125, 492, 365, 557)


def adb(serial: str, *args: str, binary: bool = False):
    result = subprocess.run([ADB, "-s", serial, *args], check=True, capture_output=True, timeout=30)
    return result.stdout if binary else result.stdout.decode(errors="replace")


def media_track_active(serial: str, uid: int) -> bool:
    raw = adb(serial, "shell", "dumpsys", "audio")
    return any(
        "AudioPlaybackConfiguration" in line
        and "type:android.media.AudioTrack" in line
        and "state:started" in line
        and f"u/pid:{uid}/" in line
        for line in raw.splitlines()
    )


def pink_pixels(png: bytes) -> int:
    image = cv2.imdecode(np.frombuffer(png, np.uint8), cv2.IMREAD_COLOR)
    if image is None:
        return 0
    left, top, right, bottom = PINK_ROI
    crop = image[top:bottom, left:right]
    blue, green, red = cv2.split(crop)
    return int(((red > 150) & (red > green * 1.35) & (blue > 60) & (green < 150)).sum())


def focused_title(xml: str) -> str:
    for node in ET.fromstring(xml).iter("node"):
        if node.attrib.get("focused") == "true":
            return (node.attrib.get("content-desc") or node.attrib.get("text") or "").strip()
    return ""


def package_version(serial: str, package: str) -> dict:
    raw = adb(serial, "shell", "dumpsys", "package", package)
    name = re.search(r"versionName=([^\s]+)", raw)
    code = re.search(r"versionCode=(\d+)", raw)
    return {"versionName": name.group(1) if name else None, "versionCode": int(code.group(1)) if code else None}


def repository_metadata() -> dict:
    root = Path(__file__).resolve().parents[1]
    commit = subprocess.run(["git", "-C", str(root), "rev-parse", "HEAD"], capture_output=True, text=True, check=True).stdout.strip()
    dirty = bool(subprocess.run(["git", "-C", str(root), "status", "--porcelain"], capture_output=True, text=True, check=True).stdout.strip())
    apk = root / "app/build/outputs/apk/standaloneTvUniversal/debug/app-standalone-tv-universal-debug.apk"
    apk_hash = hashlib.sha256(apk.read_bytes()).hexdigest() if apk.exists() else None
    return {"gitCommit": commit, "gitDirtyTree": dirty, "candidateApkSha256": apk_hash}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--title", required=True)
    parser.add_argument("--evidence-dir", required=True, type=Path)
    parser.add_argument("--timeout-seconds", type=int, default=190)
    args = parser.parse_args()
    args.evidence_dir.mkdir(parents=True, exist_ok=True)

    uid_text = adb(args.serial, "shell", "cmd", "package", "list", "packages", "-U", PROJECTIVY)
    match = re.search(rf"package:{re.escape(PROJECTIVY)}\s+uid:(\d+)", uid_text)
    if not match:
        raise SystemExit("INCONCLUSIVE: Projectivy UID unavailable")
    uid = int(match.group(1))
    hierarchy = adb(args.serial, "shell", "uiautomator", "dump", "/sdcard/natural-end.xml")
    hierarchy = adb(args.serial, "exec-out", "cat", "/sdcard/natural-end.xml")
    if focused_title(hierarchy) != args.title:
        raise SystemExit("INCONCLUSIVE: expected focused card title not established")
    if not media_track_active(args.serial, uid):
        raise SystemExit("INCONCLUSIVE: Projectivy AudioTrack was not active at start")

    started = time.monotonic()
    samples = []
    (args.evidence_dir / "before.png").write_bytes(adb(args.serial, "exec-out", "screencap", "-p", binary=True))
    while time.monotonic() - started < args.timeout_seconds:
        active = media_track_active(args.serial, uid)
        sample = {"elapsedSeconds": round(time.monotonic() - started, 2), "audioTrackActive": active}
        if not active:
            sample["pinkPixelsAfterStop"] = pink_pixels(adb(args.serial, "exec-out", "screencap", "-p", binary=True))
            samples.append(sample)
            break
        samples.append(sample)
        time.sleep(1)

    end_hierarchy = adb(args.serial, "shell", "uiautomator", "dump", "/sdcard/natural-end-after.xml")
    end_hierarchy = adb(args.serial, "exec-out", "cat", "/sdcard/natural-end-after.xml")
    end_png = adb(args.serial, "exec-out", "screencap", "-p", binary=True)
    (args.evidence_dir / "after.png").write_bytes(end_png)
    foreground = adb(args.serial, "shell", "dumpsys", "activity", "activities")
    callback_log = adb(args.serial, "logcat", "-d", "-s", "TvPreviewFocusAccessibilityService:I")
    (args.evidence_dir / "playback-callback-log.txt").write_text(callback_log)
    remains_focused = focused_title(end_hierarchy) == args.title
    stopped = bool(samples and not samples[-1]["audioTrackActive"])
    final_pink = pink_pixels(end_png)
    status = "PASS" if stopped and remains_focused and final_pink < 100 else "FAIL_PRODUCT"
    result = {
        "status": status,
        "test": "natural end hides waveform while card remains focused",
        "serial": args.serial,
        "deviceModel": adb(args.serial, "shell", "getprop", "ro.product.model").strip(),
        "deviceCodename": adb(args.serial, "shell", "getprop", "ro.product.device").strip(),
        "androidApi": int(adb(args.serial, "shell", "getprop", "ro.build.version.sdk").strip()),
        "projectivyVersion": package_version(args.serial, PROJECTIVY),
        "auraMusicVersion": package_version(args.serial, APP),
        "projectivyUid": uid,
        "title": args.title,
        "startedUtc": datetime.now(timezone.utc).isoformat(),
        "command": sys.argv,
        **repository_metadata(),
        "timeoutSeconds": args.timeout_seconds,
        "elapsedToAudioStopSeconds": samples[-1]["elapsedSeconds"] if stopped else None,
        "audioStopped": stopped,
        "cardStillFocused": remains_focused,
        "pinkPixelsAfterStop": final_pink,
        "projectivyForeground": bool(re.search(r"topResumedActivity=.*com\.spocky\.projengmenu/\.ui\.home\.MainActivity", foreground)),
        "checks": {"audioNaturallyStopped": stopped, "focusRemainedOnSameCard": remains_focused, "waveHidden": final_pink < 100},
        "samples": samples,
    }
    (args.evidence_dir / "result.json").write_text(json.dumps(result, indent=2, ensure_ascii=False) + "\n")
    print(json.dumps({"status": status, "elapsedToAudioStopSeconds": result["elapsedToAudioStopSeconds"], "pinkPixelsAfterStop": final_pink}, ensure_ascii=False))


if __name__ == "__main__":
    main()
