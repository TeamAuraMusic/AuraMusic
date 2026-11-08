#!/usr/bin/env python3
"""Verify no-click Projectivy focus loss stops preview, and returning focus restarts it."""

import argparse
import json
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from pathlib import Path

import cv2
import numpy as np


ADB = "/home/nos/android-sdk/platform-tools/adb"
PROJECTIVY = "com.spocky.projengmenu"
AURA = "com.auramusic.app.debug"
SERVICE = "com.auramusic.app.tv.TvPreviewFocusAccessibilityService"


def adb(serial: str, *args: str, binary: bool = False, timeout: int = 45):
    result = subprocess.run(
        [ADB, "-s", serial, *args], check=True, capture_output=True, timeout=timeout
    )
    return result.stdout if binary else result.stdout.decode(errors="replace")


def focused_node(xml: str) -> dict | None:
    root = ET.fromstring(xml)
    for node in root.iter("node"):
        if node.attrib.get("focused") == "true":
            return node.attrib
    return None


def stage(serial: str, evidence: Path, name: str) -> tuple[str, str, str]:
    remote = f"/sdcard/codex-projectivy-focus-return-{name}.xml"
    xml = read_hierarchy(serial, remote)
    (evidence / f"{name}.xml").write_text(xml)
    png = adb(serial, "exec-out", "screencap", "-p", binary=True)
    (evidence / f"{name}.png").write_bytes(png)
    foreground = adb(serial, "shell", "dumpsys", "activity", "activities")
    (evidence / f"{name}-foreground.txt").write_text(foreground)
    return xml, png, foreground


def read_hierarchy(serial: str, remote: str) -> str:
    adb(serial, "shell", "uiautomator", "dump", remote)
    return adb(serial, "exec-out", "cat", remote)


def title(node: dict | None) -> str:
    return ((node or {}).get("text") or (node or {}).get("content-desc") or "").strip()


def projectivy_tracks(serial: str, uid: int) -> list[str]:
    raw = adb(serial, "shell", "dumpsys", "audio")
    return [
        line.strip() for line in raw.splitlines()
        if "AudioPlaybackConfiguration" in line
        and "type:android.media.AudioTrack" in line
        and "state:started" in line
        and f"u/pid:{uid}/" in line
    ]


def indicator_pixels(png: bytes, bounds: str | None) -> tuple[int, list[int]]:
    if not bounds:
        return 0, []
    match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
    image = cv2.imdecode(np.frombuffer(png, np.uint8), cv2.IMREAD_COLOR)
    if match is None or image is None:
        return 0, []
    left, top, right, bottom = map(int, match.groups())
    x = max(0, round(left + (right - left) * 0.22))
    y = max(0, round(top + (bottom - top) * 0.64))
    width = min(image.shape[1] - x, round((right - left) * 0.56))
    height = min(image.shape[0] - y, round((bottom - top) * 0.34))
    crop = image[y:y + height, x:x + width]
    if crop.size == 0:
        return 0, []
    blue, green, red = cv2.split(crop)
    pink = (red > 150) & (red > green * 1.35) & (blue > 60) & (green < 150)
    return int(pink.sum()), pink.sum(axis=1).astype(int).tolist()


def is_projectivy_foreground(raw: str) -> bool:
    return bool(re.search(r"topResumedActivity=.*com\.spocky\.projengmenu/\.ui\.home\.MainActivity", raw))


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True, help="Explicit ADB serial; no device fallback")
    parser.add_argument("--title", required=True, help="Exact focused AuraMusic card title")
    parser.add_argument("--evidence-dir", required=True, type=Path)
    parser.add_argument("--focus-timeout-seconds", type=float, default=8.0)
    parser.add_argument("--audio-timeout-seconds", type=float, default=8.0)
    args = parser.parse_args()
    evidence = args.evidence_dir
    evidence.mkdir(parents=True, exist_ok=True)

    uid_text = adb(args.serial, "shell", "cmd", "package", "list", "packages", "-U", PROJECTIVY)
    uid_match = re.search(rf"package:{re.escape(PROJECTIVY)}\s+uid:(\d+)", uid_text)
    if uid_match is None:
        raise SystemExit("INCONCLUSIVE: cannot resolve Projectivy UID")
    projectivy_uid = int(uid_match.group(1))
    metadata = {
        "serial": args.serial,
        "startedUtc": datetime.now(timezone.utc).isoformat(),
        "model": adb(args.serial, "shell", "getprop", "ro.product.model").strip(),
        "device": adb(args.serial, "shell", "getprop", "ro.product.device").strip(),
        "androidApi": adb(args.serial, "shell", "getprop", "ro.build.version.sdk").strip(),
        "projectivyUid": projectivy_uid,
        "expectedTitle": args.title,
        "selectPressed": False,
        "accessibilityServices": adb(args.serial, "shell", "settings", "get", "secure", "enabled_accessibility_services").strip(),
    }
    (evidence / "run-metadata.json").write_text(json.dumps(metadata, indent=2, ensure_ascii=False) + "\n")

    before_xml, before_png, before_fg = stage(args.serial, evidence, "before")
    before_node = focused_node(before_xml)
    initial_title = title(before_node)
    before_audio = projectivy_tracks(args.serial, projectivy_uid)
    before_pixels, before_profile = indicator_pixels(before_png, (before_node or {}).get("bounds"))
    result = {
        "test": "Projectivy AuraMusic focus-loss stop and return restart",
        "status": "INCONCLUSIVE",
        "scope": "No-click focus loss and return; exact title, Projectivy foreground, AudioTrack state, and focused-card indicator pixels.",
        **metadata,
        "initialFocusedTitle": initial_title,
        "checks": {},
        "samples": {},
        "limitations": [
            "AudioTrack state does not capture acoustic samples or prove song identity.",
            "The capture uses device screenshots, not HDMI capture.",
        ],
    }

    if initial_title != args.title or not is_projectivy_foreground(before_fg) or not before_audio:
        result["statusReason"] = "Starting focus/audio/foreground precondition was not established."
        result["samples"]["before"] = {"focusedTitle": initial_title, "audioTrackCount": len(before_audio), "pinkPixels": before_pixels}
        (evidence / "result.json").write_text(json.dumps(result, indent=2, ensure_ascii=False) + "\n")
        print(json.dumps({"status": result["status"], "reason": result["statusReason"], "result": str(evidence / "result.json")}, indent=2))
        return

    loss_started = time.monotonic()
    adb(args.serial, "shell", "input", "keyevent", "KEYCODE_DPAD_UP")
    loss_title = initial_title
    loss_xml = ""
    loss_poll_path = f"/sdcard/codex-projectivy-focus-return-loss-poll.xml"
    while time.monotonic() - loss_started < args.focus_timeout_seconds:
        time.sleep(0.4)
        loss_xml = read_hierarchy(args.serial, loss_poll_path)
        loss_title = title(focused_node(loss_xml))
        if loss_title and loss_title != args.title:
            break
    if loss_title == args.title or not loss_title:
        loss_xml, _, loss_fg = stage(args.serial, evidence, "focus-loss")
        result["statusReason"] = "DPAD focus change was not observed; input delivery or launcher focus is unresolved."
        result["samples"]["before"] = {"focusedTitle": initial_title, "audioTrackCount": len(before_audio), "pinkPixels": before_pixels}
        result["samples"]["focusLoss"] = {"focusedTitle": loss_title, "projectivyForeground": is_projectivy_foreground(loss_fg)}
        (evidence / "result.json").write_text(json.dumps(result, indent=2, ensure_ascii=False) + "\n")
        print(json.dumps({"status": result["status"], "reason": result["statusReason"], "result": str(evidence / "result.json")}, indent=2))
        return

    stop_started = time.monotonic()
    stop_elapsed = None
    stop_tracks = projectivy_tracks(args.serial, projectivy_uid)
    while stop_tracks and time.monotonic() - stop_started < args.audio_timeout_seconds:
        time.sleep(0.25)
        stop_tracks = projectivy_tracks(args.serial, projectivy_uid)
    if not stop_tracks:
        stop_elapsed = round(time.monotonic() - stop_started, 3)
    _, loss_png, loss_fg = stage(args.serial, evidence, "focus-loss")
    loss_logs = adb(args.serial, "logcat", "-d", "-v", "threadtime", "-s", "TvPreviewFocusAccessibilityService:D")
    (evidence / "focus-service-logcat.txt").write_text(loss_logs)

    return_started = time.monotonic()
    adb(args.serial, "shell", "input", "keyevent", "KEYCODE_DPAD_DOWN")
    return_title = ""
    return_xml = return_fg = ""
    while time.monotonic() - return_started < args.focus_timeout_seconds:
        time.sleep(0.4)
        return_xml = read_hierarchy(args.serial, "/sdcard/codex-projectivy-focus-return-back-poll.xml")
        return_title = title(focused_node(return_xml))
        if return_title == args.title:
            break
    if return_title == args.title:
        audio_deadline = return_started + args.audio_timeout_seconds
        return_tracks = projectivy_tracks(args.serial, projectivy_uid)
        while not return_tracks and time.monotonic() < audio_deadline:
            time.sleep(0.25)
            return_tracks = projectivy_tracks(args.serial, projectivy_uid)
    else:
        return_tracks = []
    return_xml, return_png, return_fg = stage(args.serial, evidence, "return")
    return_node = focused_node(return_xml)
    return_pixels, return_profile = indicator_pixels(return_png, (return_node or {}).get("bounds"))

    for frame_index in range(5):
        time.sleep(0.25)
        frame = adb(args.serial, "exec-out", "screencap", "-p", binary=True)
        (evidence / f"wave-frame-{frame_index:02d}.png").write_bytes(frame)
        _, profile = indicator_pixels(frame, (return_node or {}).get("bounds"))
        (evidence / f"wave-frame-{frame_index:02d}.json").write_text(json.dumps({"pinkRowProfile": profile}) + "\n")

    no_overlap = True
    audio_after_return = adb(args.serial, "shell", "dumpsys", "audio")
    aura_uid_text = adb(args.serial, "shell", "cmd", "package", "list", "packages", "-U", AURA)
    aura_uid_match = re.search(rf"package:{re.escape(AURA)}\s+uid:(\d+)", aura_uid_text)
    if aura_uid_match:
        no_overlap = not any(
            "AudioPlaybackConfiguration" in line and "type:android.media.AudioTrack" in line
            and "state:started" in line and f"u/pid:{int(aura_uid_match.group(1))}/" in line
            for line in audio_after_return.splitlines()
        )
    profiles = [json.loads((evidence / f"wave-frame-{i:02d}.json").read_text())["pinkRowProfile"] for i in range(5)]
    distinct_profiles = len({tuple(profile) for profile in profiles if profile})
    checks = {
        "initialTitleFocused": initial_title == args.title,
        "projectivyForegroundBefore": is_projectivy_foreground(before_fg),
        "indicatorVisibleBefore": before_pixels > 100,
        "focusMovedOutsideCard": loss_title != args.title,
        "projectivyForegroundOnFocusLoss": is_projectivy_foreground(loss_fg),
        "audioStoppedOnFocusLoss": not stop_tracks,
        "audioStopSecondsUpperBound": stop_elapsed,
        "focusReturnedToSameTitle": return_title == args.title,
        "projectivyForegroundAfterReturn": is_projectivy_foreground(return_fg),
        "audioRestarted": bool(return_tracks),
        "noAuraMusicTrackOverlap": no_overlap,
        "indicatorVisibleAfterReturn": return_pixels > 100,
        "waveProfilesSampled": distinct_profiles,
    }
    result["checks"] = checks
    result["samples"] = {
        "before": {"focusedTitle": initial_title, "audioTrackCount": len(before_audio), "pinkPixels": before_pixels, "waveProfile": before_profile},
        "focusLoss": {"focusedTitle": loss_title, "projectivyAudioTrackCount": len(stop_tracks), "stopSecondsUpperBound": stop_elapsed},
        "return": {"focusedTitle": return_title, "projectivyAudioTrackCount": len(return_tracks), "pinkPixels": return_pixels, "distinctWaveProfiles": distinct_profiles},
    }
    if not checks["focusMovedOutsideCard"] or not checks["focusReturnedToSameTitle"]:
        result["status"] = "INCONCLUSIVE"
        result["statusReason"] = "Focus movement could not be established in both directions."
    elif not checks["audioStoppedOnFocusLoss"]:
        result["status"] = "FAIL_PRODUCT"
        result["statusReason"] = "Projectivy audio remained active after focus left the AuraMusic card."
    elif not checks["audioRestarted"]:
        result["status"] = "INCONCLUSIVE"
        result["statusReason"] = "Audio did not restart, but media-source availability was not independently verified."
    elif not checks["indicatorVisibleAfterReturn"] or distinct_profiles < 2:
        result["status"] = "FAIL_PRODUCT"
        result["statusReason"] = "The focused-card animated indicator was missing or static after return."
    elif not no_overlap:
        result["status"] = "FAIL_PRODUCT"
        result["statusReason"] = "AuraMusic and Projectivy AudioTracks overlapped after focus returned."
    else:
        result["status"] = "PASS"
        result["statusReason"] = "Focus loss stopped preview; returning to the same card restarted preview audio and animated in-card wave."
    (evidence / "result.json").write_text(json.dumps(result, indent=2, ensure_ascii=False) + "\n")
    print(json.dumps({"status": result["status"], "checks": checks, "result": str(evidence / "result.json")}, indent=2, ensure_ascii=False))
    if result["status"] == "FAIL_PRODUCT":
        raise SystemExit(1)


if __name__ == "__main__":
    main()
