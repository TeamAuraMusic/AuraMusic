#!/usr/bin/env python3
"""Verify no-click Projectivy focus loss stops preview, and returning focus restarts it."""

import argparse
import json
import queue
import re
import subprocess
import threading
import time
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from pathlib import Path

import cv2
import numpy as np
from tv_adb import DEFAULT_ADB_EXECUTABLE, DEFAULT_ADB_SERVER_PORT, adb_command, is_allowed_target, run_adb


ADB = DEFAULT_ADB_EXECUTABLE
ADB_PORT = DEFAULT_ADB_SERVER_PORT
PROJECTIVY = "com.spocky.projengmenu"
AURA = "com.auramusic.app.debug"
SERVICE = "com.auramusic.app.tv.TvPreviewFocusAccessibilityService"


def adb(serial: str, *args: str, binary: bool = False, timeout: int = 45):
    return run_adb(
        serial, *args, executable=ADB, server_port=ADB_PORT, timeout=timeout, binary=binary
    )


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


def bounds_from_focus_log(line: str | None) -> str | None:
    if not line:
        return None
    match = re.search(r"focus indicator shown title=.*?bounds=Rect\((-?\d+), (-?\d+) - (-?\d+), (-?\d+)\)", line)
    if not match:
        return None
    left, top, right, bottom = map(int, match.groups())
    return f"[{left},{top}][{right},{bottom}]"


def active_projectivy_audio(serial: str, uid: int) -> tuple[str, list[str]]:
    raw = adb(serial, "shell", "dumpsys", "audio")
    tracks = [
        line.strip() for line in raw.splitlines()
        if "AudioPlaybackConfiguration" in line
        and "type:android.media.AudioTrack" in line
        and "state:started" in line
        and f"u/pid:{uid}/" in line
    ]
    return raw, tracks


def main() -> None:
    global ADB, ADB_PORT
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True, help="Explicit ADB serial; no device fallback")
    parser.add_argument("--adb", type=Path, default=ADB,
                        help="Authoritative Windows ADB executable (default: TVCTL_ADB or I: SDK ADB)")
    parser.add_argument("--adb-port", type=int, default=ADB_PORT,
                        help="ADB server port used by tvctl (default: TVCTL_ADB_PORT or 5038)")
    parser.add_argument("--allow-physical", action="store_true",
                        help="Explicitly permit non-emulator serials after emulator acceptance")
    parser.add_argument("--title", required=True, help="Exact focused AuraMusic card title")
    parser.add_argument("--evidence-dir", required=True, type=Path)
    parser.add_argument("--focus-timeout-seconds", type=float, default=8.0)
    parser.add_argument("--audio-timeout-seconds", type=float, default=8.0)
    args = parser.parse_args()
    if not is_allowed_target(args.serial, args.allow_physical):
        parser.error("serial is outside the emulator allowlist; physical access is gated")
    if not args.adb.is_file():
        parser.error(f"Windows ADB executable not found: {args.adb}")
    if not 1 <= args.adb_port <= 65535:
        parser.error("ADB server port must be in 1..65535")
    ADB, ADB_PORT = args.adb, args.adb_port
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

    logcat = subprocess.Popen(
        adb_command(
            ADB, ADB_PORT, args.serial, "logcat", "-v", "threadtime", "-T", "1", "-s",
            "TvPreviewFocusAccessibilityService:D", "TvPreviewContentProvider:I",
        ),
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1,
    )
    log_lines: queue.Queue[str] = queue.Queue()
    captured_logs: list[str] = []

    def drain_queued_logs() -> None:
        while True:
            try:
                captured_logs.append(log_lines.get_nowait())
            except queue.Empty:
                return

    def drain_logcat() -> None:
        assert logcat.stdout is not None
        for log_line in logcat.stdout:
            log_lines.put(log_line)

    logcat_reader = threading.Thread(target=drain_logcat, name="focus-return-logcat-reader", daemon=True)
    logcat_reader.start()
    before_xml, before_png, before_fg = stage(args.serial, evidence, "before")
    before_node = focused_node(before_xml)
    initial_title = title(before_node)
    initial_bounds = (before_node or {}).get("bounds")
    initial_program = None
    service_reconnected = False
    deadline = time.monotonic() + args.focus_timeout_seconds
    while time.monotonic() < deadline and initial_program is None:
        try:
            line = log_lines.get(timeout=max(0, deadline - time.monotonic()))
        except queue.Empty:
            break
        captured_logs.append(line)
        if "TV preview focus indicator service connected" in line:
            service_reconnected = True
        if service_reconnected and f"focus indicator shown title={args.title} " in line:
            initial_program = line.rstrip()
    before_png = adb(args.serial, "exec-out", "screencap", "-p", binary=True)
    (evidence / "before.png").write_bytes(before_png)
    before_fg = adb(args.serial, "shell", "dumpsys", "activity", "activities")
    (evidence / "before-foreground.txt").write_text(before_fg)
    before_audio_dump, before_audio = active_projectivy_audio(args.serial, projectivy_uid)
    (evidence / "before-audio.txt").write_text(before_audio_dump)
    before_pixels, before_profile = indicator_pixels(before_png, initial_bounds)
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

    if (initial_title != args.title or not is_projectivy_foreground(before_fg)
            or not before_audio or initial_program is None):
        result["statusReason"] = "Starting focus/audio/foreground precondition was not established."
        result["samples"]["before"] = {"focusedTitle": initial_title, "audioTrackCount": len(before_audio), "pinkPixels": before_pixels}
        (evidence / "focus-service-logcat.txt").write_text("".join(captured_logs))
        (evidence / "result.json").write_text(json.dumps(result, indent=2, ensure_ascii=False) + "\n")
        print(json.dumps({"status": result["status"], "reason": result["statusReason"], "result": str(evidence / "result.json")}, indent=2))
        logcat.terminate()
        return

    drain_queued_logs()
    loss_started = time.monotonic()
    adb(args.serial, "shell", "input", "keyevent", "KEYCODE_DPAD_UP")
    loss_event = None
    loss_indicator_line = None
    deadline = time.monotonic() + args.focus_timeout_seconds
    while time.monotonic() < deadline and loss_indicator_line is None:
        try:
            line = log_lines.get(timeout=max(0, deadline - time.monotonic()))
        except queue.Empty:
            break
        captured_logs.append(line)
        if ("TYPE_VIEW_FOCUSED" in line and "package=com.spocky.projengmenu" in line
                and f"description={args.title}" not in line):
            loss_event = line.rstrip()
        if ("focus indicator hidden reason=" in line and
                ("outside its AuraMusic channel row" in line or "not a published AuraMusic program" in line)):
            loss_indicator_line = line.rstrip()

    stop_elapsed = None
    stop_tracks = projectivy_tracks(args.serial, projectivy_uid)
    while loss_indicator_line and stop_tracks and time.monotonic() < deadline + args.audio_timeout_seconds:
        time.sleep(0.2)
        _, stop_tracks = active_projectivy_audio(args.serial, projectivy_uid)
    if not stop_tracks:
        stop_elapsed = round(time.monotonic() - loss_started, 3)
    loss_png = adb(args.serial, "exec-out", "screencap", "-p", binary=True)
    (evidence / "focus-loss.png").write_bytes(loss_png)
    loss_fg = adb(args.serial, "shell", "dumpsys", "activity", "activities")
    (evidence / "focus-loss-foreground.txt").write_text(loss_fg)
    loss_audio_dump, _ = active_projectivy_audio(args.serial, projectivy_uid)
    (evidence / "focus-loss-audio.txt").write_text(loss_audio_dump)
    loss_pixels, _ = indicator_pixels(loss_png, initial_bounds)

    if loss_indicator_line is None:
        result["statusReason"] = "Live accessibility logs did not confirm focus leaving the AuraMusic program row."
        result["samples"] = {"before": {"focusedTitle": initial_title, "audioTrackCount": len(before_audio), "pinkPixels": before_pixels}}
        result["status"] = "INCONCLUSIVE"
        (evidence / "focus-service-logcat.txt").write_text("".join(captured_logs))
        (evidence / "result.json").write_text(json.dumps(result, indent=2, ensure_ascii=False) + "\n")
        logcat.terminate()
        print(json.dumps({"status": result["status"], "reason": result["statusReason"], "result": str(evidence / "result.json")}, indent=2))
        return

    drain_queued_logs()
    return_started = time.monotonic()
    adb(args.serial, "shell", "input", "keyevent", "KEYCODE_DPAD_DOWN")
    return_event = None
    return_indicator_line = None
    deadline = return_started + args.focus_timeout_seconds
    while time.monotonic() < deadline and (return_event is None or return_indicator_line is None):
        try:
            line = log_lines.get(timeout=max(0, deadline - time.monotonic()))
        except queue.Empty:
            break
        captured_logs.append(line)
        if ("TYPE_VIEW_FOCUSED" in line and "package=com.spocky.projengmenu" in line
                and f"description={args.title}" in line):
            return_event = line.rstrip()
        if f"focus indicator shown title={args.title} " in line:
            return_indicator_line = line.rstrip()
    return_bounds = bounds_from_focus_log(return_indicator_line)
    return_tracks = projectivy_tracks(args.serial, projectivy_uid)
    audio_deadline = time.monotonic() + args.audio_timeout_seconds
    while return_event and return_indicator_line and not return_tracks and time.monotonic() < audio_deadline:
        time.sleep(0.2)
        _, return_tracks = active_projectivy_audio(args.serial, projectivy_uid)
    return_png = adb(args.serial, "exec-out", "screencap", "-p", binary=True)
    (evidence / "return.png").write_bytes(return_png)
    return_fg = adb(args.serial, "shell", "dumpsys", "activity", "activities")
    (evidence / "return-foreground.txt").write_text(return_fg)
    return_audio_dump, _ = active_projectivy_audio(args.serial, projectivy_uid)
    (evidence / "return-audio.txt").write_text(return_audio_dump)
    return_pixels, return_profile = indicator_pixels(return_png, return_bounds)

    for frame_index in range(5):
        time.sleep(0.25)
        frame = adb(args.serial, "exec-out", "screencap", "-p", binary=True)
        (evidence / f"wave-frame-{frame_index:02d}.png").write_bytes(frame)
        _, profile = indicator_pixels(frame, return_bounds)
        (evidence / f"wave-frame-{frame_index:02d}.json").write_text(json.dumps({"pinkRowProfile": profile}) + "\n")

    no_overlap = True
    audio_after_return = return_audio_dump
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
        "focusMovedOutsideCard": loss_event is not None and loss_indicator_line is not None,
        "projectivyForegroundOnFocusLoss": is_projectivy_foreground(loss_fg),
        "audioStoppedOnFocusLoss": not stop_tracks,
        "audioStopSecondsUpperBound": stop_elapsed,
        "indicatorHiddenOnFocusLoss": loss_pixels <= 100,
        "focusReturnedToSameTitle": return_event is not None,
        "returnIndicatorEventObserved": return_indicator_line is not None,
        "projectivyForegroundAfterReturn": is_projectivy_foreground(return_fg),
        "audioRestarted": bool(return_tracks),
        "noAuraMusicTrackOverlap": no_overlap,
        "indicatorVisibleAfterReturn": return_pixels > 100,
        "waveProfilesSampled": distinct_profiles,
    }
    result["checks"] = checks
    result["samples"] = {
        "before": {"focusedTitle": initial_title, "audioTrackCount": len(before_audio), "pinkPixels": before_pixels, "waveProfile": before_profile},
        "focusLoss": {"focusedTitleEvent": loss_event, "indicatorHiddenEvent": loss_indicator_line, "projectivyAudioTrackCount": len(stop_tracks), "stopSecondsUpperBound": stop_elapsed, "pinkPixels": loss_pixels},
        "return": {"focusedTitleEvent": return_event, "indicatorShownEvent": return_indicator_line, "projectivyAudioTrackCount": len(return_tracks), "pinkPixels": return_pixels, "distinctWaveProfiles": distinct_profiles},
    }
    if not checks["focusMovedOutsideCard"] or not checks["focusReturnedToSameTitle"]:
        result["status"] = "INCONCLUSIVE"
        result["statusReason"] = "Focus movement could not be established in both directions."
    elif not checks["audioStoppedOnFocusLoss"]:
        result["status"] = "FAIL_PRODUCT"
        result["statusReason"] = "Projectivy audio remained active after focus left the AuraMusic card."
    elif not checks["indicatorHiddenOnFocusLoss"]:
        result["status"] = "FAIL_PRODUCT"
        result["statusReason"] = "The focused-card indicator remained visible after focus left the AuraMusic card."
    elif not checks["audioRestarted"]:
        result["status"] = "INCONCLUSIVE"
        result["statusReason"] = "Audio did not restart, but media-source availability was not independently verified."
    elif not checks["returnIndicatorEventObserved"] or not checks["indicatorVisibleAfterReturn"] or distinct_profiles < 2:
        result["status"] = "FAIL_PRODUCT"
        result["statusReason"] = "The focused-card animated indicator was missing or static after return."
    elif not no_overlap:
        result["status"] = "FAIL_PRODUCT"
        result["statusReason"] = "AuraMusic and Projectivy AudioTracks overlapped after focus returned."
    else:
        result["status"] = "PASS"
        result["statusReason"] = "Focus loss stopped preview; returning to the same card restarted preview audio and animated in-card wave."
    (evidence / "result.json").write_text(json.dumps(result, indent=2, ensure_ascii=False) + "\n")
    (evidence / "focus-service-logcat.txt").write_text("".join(captured_logs))
    try:
        final_hierarchy = read_hierarchy(args.serial, "/sdcard/codex-projectivy-focus-return-final.xml")
        (evidence / "final-hierarchy.xml").write_text(final_hierarchy)
    finally:
        logcat.terminate()
        logcat.wait(timeout=5)
    print(json.dumps({"status": result["status"], "checks": checks, "result": str(evidence / "result.json")}, indent=2, ensure_ascii=False))
    if result["status"] == "FAIL_PRODUCT":
        raise SystemExit(1)


if __name__ == "__main__":
    main()
