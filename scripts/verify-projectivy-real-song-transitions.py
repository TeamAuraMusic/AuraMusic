#!/usr/bin/env python3
"""Exercise repeated no-click focus changes between two published AuraMusic cards."""

import argparse
import hashlib
import json
import re
import select
import subprocess
import time
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from pathlib import Path
from typing import Callable

import cv2
import numpy as np
from tv_adb import DEFAULT_ADB_EXECUTABLE, DEFAULT_ADB_SERVER_PORT, adb_command, is_allowed_target, run_adb

ADB_EXECUTABLE = DEFAULT_ADB_EXECUTABLE
ADB_SERVER_PORT = DEFAULT_ADB_SERVER_PORT


def adb(serial: str, *args: str, timeout: int = 30, binary: bool = False):
    return run_adb(
        serial, *args, executable=ADB_EXECUTABLE, server_port=ADB_SERVER_PORT,
        timeout=timeout, binary=binary,
    )


def focus_nodes(xml: str) -> list[dict]:
    root = ET.fromstring(xml)
    return [node.attrib for node in root.iter("node") if node.attrib.get("focused") == "true"]


def focused_program_info(xml: str, title: str) -> dict | None:
    root = ET.fromstring(xml)
    parents = {child: parent for parent in root.iter("node") for child in parent}
    node = next((node for node in root.iter("node")
                 if node.attrib.get("focused") == "true"
                 and (node.attrib.get("text") or node.attrib.get("content-desc")) == title), None)
    if node is None:
        return None
    ancestors = []
    parent = parents.get(node)
    while parent is not None:
        description = parent.attrib.get("content-desc", "").strip()
        if description:
            ancestors.append(description)
        parent = parents.get(parent)
    return {"attributes": node.attrib, "ancestorContentDescriptions": ancestors}


def count_preview_provider_opens(log_text: str) -> int:
    pattern = r"TV preview audio-only open request=|TV preview seekable (?:cache hit|prepared open) request="
    return len(re.findall(pattern, log_text))


def analyze_indicator(image: np.ndarray | None, bounds: str | None) -> tuple[int, list[int], dict | None]:
    if image is None or not bounds:
        return 0, [], None
    parsed = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
    if not parsed:
        return 0, [], None
    left, top, right, bottom = map(int, parsed.groups())
    x = max(0, round(left + (right - left) * 0.22))
    y = max(0, round(top + (bottom - top) * 0.64))
    width = min(image.shape[1] - x, round((right - left) * 0.56))
    height = min(image.shape[0] - y, round((bottom - top) * 0.34))
    if width <= 0 or height <= 0:
        return 0, [], None
    crop = image[y:y + height, x:x + width]
    if crop.size == 0:
        return 0, [], None
    blue, green, red = cv2.split(crop)
    pink = (red > 150) & (red > green * 1.35) & (blue > 60) & (green < 150)
    return (
        int(pink.sum()),
        pink.sum(axis=1).astype(int).tolist(),
        {"x": x, "y": y, "width": width, "height": height},
    )


def wait_for_indicator(
    capture: Callable[[], tuple[bytes, np.ndarray | None]],
    bounds: str | None,
    timeout_seconds: float,
    poll_interval_seconds: float = 0.1,
    clock: Callable[[], float] = time.monotonic,
    sleep: Callable[[float], None] = time.sleep,
) -> dict:
    if timeout_seconds < 0 or poll_interval_seconds <= 0:
        raise ValueError("timeout must be non-negative and polling interval must be positive")
    started_at = clock()
    deadline = started_at + timeout_seconds
    poll_count = 0
    while True:
        raw, image = capture()
        poll_count += 1
        pixels, profile, roi = analyze_indicator(image, bounds)
        sampled_at = clock()
        visible = pixels > 100
        if visible or sampled_at >= deadline:
            return {
                "raw": raw,
                "pinkPixels": pixels,
                "profile": profile,
                "roi": roi,
                "visible": visible,
                "pollCount": poll_count,
                "elapsedSeconds": max(0.0, sampled_at - started_at),
            }
        sleep(min(poll_interval_seconds, deadline - sampled_at))


def main() -> None:
    global ADB_EXECUTABLE, ADB_SERVER_PORT
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True, help="Explicit ADB serial; no device fallback")
    parser.add_argument("--adb", type=Path, default=ADB_EXECUTABLE,
                        help="Authoritative Windows ADB executable (default: TVCTL_ADB or I: SDK ADB)")
    parser.add_argument("--adb-port", type=int, default=ADB_SERVER_PORT,
                        help="Windows ADB server port used by tvctl (default: TVCTL_ADB_PORT or 5038)")
    parser.add_argument("--allow-physical", action="store_true",
                        help="Explicitly permit non-emulator serials after emulator acceptance")
    parser.add_argument("--title-a", required=True)
    parser.add_argument("--title-b", required=True)
    parser.add_argument("--channel-name", default="AuraMusic")
    parser.add_argument("--cycles", type=int, default=10, help="A→B→A rounds")
    parser.add_argument("--focus-timeout-seconds", type=float, default=5)
    parser.add_argument("--audio-timeout-seconds", type=float, default=5,
                        help="wait for the focused preview AudioTrack before sampling the transition")
    parser.add_argument("--settle-seconds", type=float, default=0.5)
    parser.add_argument("--indicator-timeout-seconds", type=float, default=1.0,
                        help="bounded time after audio starts for the focused-card waveform to appear")
    parser.add_argument("--evidence-dir", required=True, type=Path)
    args = parser.parse_args()
    if not is_allowed_target(args.serial, args.allow_physical):
        parser.error("serial is outside the emulator allowlist; pass --allow-physical only for the gated Chromecast phase")
    if not args.adb.is_file():
        parser.error(f"Windows ADB executable not found: {args.adb}")
    if not 1 <= args.adb_port <= 65535:
        parser.error("ADB server port must be in 1..65535")
    ADB_EXECUTABLE, ADB_SERVER_PORT = args.adb, args.adb_port
    if (args.cycles < 1 or args.focus_timeout_seconds <= 0 or args.audio_timeout_seconds <= 0
            or args.indicator_timeout_seconds <= 0
            or args.settle_seconds < 0):
        parser.error("cycles and timeouts must be positive; settle cannot be negative")
    evidence = args.evidence_dir
    evidence.mkdir(parents=True, exist_ok=True)
    serial = args.serial

    def package_uid(package: str) -> int:
        listing = adb(serial, "shell", "cmd", "package", "list", "packages", "-U", package)
        match = re.search(rf"(?:^|\s)package:{re.escape(package)}\s+uid:(\d+)", listing)
        if not match:
            raise SystemExit(f"INCONCLUSIVE: could not resolve UID for {package}")
        return int(match.group(1))

    def package_version(package: str) -> dict:
        details = adb(serial, "shell", "dumpsys", "package", package)
        version_name = re.search(r"\bversionName=([^\s]+)", details)
        version_code = re.search(r"\bversionCode=(\d+)", details)
        if not version_name or not version_code:
            raise SystemExit(f"INCONCLUSIVE: could not resolve installed version for {package}")
        return {"package": package, "versionName": version_name.group(1), "versionCode": int(version_code.group(1))}

    repo = Path(__file__).resolve().parent.parent
    git_commit = subprocess.run(
        ["git", "-C", str(repo), "rev-parse", "HEAD"], check=True, capture_output=True, text=True,
    ).stdout.strip()
    git_dirty = bool(subprocess.run(
        ["git", "-C", str(repo), "status", "--porcelain"], check=True, capture_output=True, text=True,
    ).stdout.strip())

    projectivy_uid = package_uid("com.spocky.projengmenu")
    aura_uid = package_uid("com.auramusic.app.debug")

    adb(serial, "shell", "uiautomator", "dump", "/sdcard/codex-projectivy-transition-initial.xml", timeout=45)
    initial_xml = adb(serial, "exec-out", "cat", "/sdcard/codex-projectivy-transition-initial.xml")
    (evidence / "initial-hierarchy.xml").write_text(initial_xml)
    initial_nodes = focus_nodes(initial_xml)
    current = next((node.get("text") or node.get("content-desc") or "" for node in initial_nodes), "")
    if current not in (args.title_a, args.title_b):
        raise SystemExit(f"INCONCLUSIVE: expected initial focus {args.title_a!r} or {args.title_b!r}; found {current!r}")

    metadata = {
        "serial": serial,
        "startedUtc": datetime.now(timezone.utc).isoformat(),
        "model": adb(serial, "shell", "getprop", "ro.product.model").strip(),
        "androidApi": adb(serial, "shell", "getprop", "ro.build.version.sdk").strip(),
        "projectivyPackage": "com.spocky.projengmenu",
        "auraPackage": "com.auramusic.app.debug",
        "projectivyUid": projectivy_uid,
        "auraUid": aura_uid,
        "projectivyVersion": package_version("com.spocky.projengmenu"),
        "auraMusicVersion": package_version("com.auramusic.app.debug"),
        "gitCommit": git_commit,
        "gitWorkingTreeDirty": git_dirty,
        "expectedChannelName": args.channel_name,
        "titleA": args.title_a,
        "titleB": args.title_b,
        "initialFocusedTitle": current,
        "cycles": args.cycles,
        "selectPressed": False,
    }
    (evidence / "run-metadata.json").write_text(json.dumps(metadata, indent=2) + "\n")
    logcat = subprocess.Popen(
        adb_command(
            ADB_EXECUTABLE,
            ADB_SERVER_PORT,
            serial,
            "logcat", "-v", "threadtime", "-s",
            "TvPreviewFocusAccessibilityService:D", "TvPreviewContentProvider:I",
        ),
        stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, bufsize=1,
    )
    samples = []
    try:
        for step in range(args.cycles * 2):
            target = args.title_b if current == args.title_a else args.title_a
            key = "KEYCODE_DPAD_RIGHT" if target == args.title_b else "KEYCODE_DPAD_LEFT"
            prefix = evidence / f"transition-{step + 1:02d}"
            start = time.monotonic()
            adb(serial, "shell", "input", "keyevent", key)
            matched_line = None
            deadline = time.monotonic() + args.focus_timeout_seconds
            while time.monotonic() < deadline:
                ready, _, _ = select.select([logcat.stdout], [], [], max(0, deadline - time.monotonic()))
                if not ready:
                    break
                line = logcat.stdout.readline()
                if not line:
                    break
                with (evidence / "focus-events.log").open("a") as output:
                    output.write(line)
                if (target in line and "TvPreviewFocusAccessibilityService" in line and
                        ("TYPE_VIEW_FOCUSED" in line or "focused-node refresh description=" in line)):
                    matched_line = line.rstrip()
                    break
            event_latency = time.monotonic() - start if matched_line else None
            if args.settle_seconds:
                time.sleep(args.settle_seconds)

            remote_xml = f"/sdcard/codex-projectivy-transition-{step + 1:02d}.xml"
            adb(serial, "shell", "uiautomator", "dump", remote_xml, timeout=45)
            hierarchy = adb(serial, "exec-out", "cat", remote_xml)
            prefix.with_suffix(".xml").write_text(hierarchy)
            focused = focus_nodes(hierarchy)
            focused_titles = [node.get("text") or node.get("content-desc") or "" for node in focused]
            matching_program = focused_program_info(hierarchy, target)
            matching_node = matching_program["attributes"] if matching_program else None
            ancestor_descriptions = matching_program["ancestorContentDescriptions"] if matching_program else []
            row_matched = args.channel_name in ancestor_descriptions
            bounds = matching_node.get("bounds") if matching_node else None

            foreground = adb(serial, "shell", "dumpsys", "activity", "activities")
            (evidence / f"transition-{step + 1:02d}-foreground.txt").write_text(foreground)
            projectivy_foreground = bool(re.search(
                r"topResumedActivity=.*com\.spocky\.projengmenu/\.ui\.home\.MainActivity", foreground
            ))
            audio_deadline = start + args.audio_timeout_seconds
            audio = ""
            audio_started_at = None
            while True:
                audio = adb(serial, "shell", "dumpsys", "audio")
                active = [line.strip() for line in audio.splitlines()
                          if "AudioPlaybackConfiguration" in line
                          and "type:android.media.AudioTrack" in line
                          and re.search(rf"u/pid:{projectivy_uid}/\d+ state:started", line)]
                if active:
                    audio_started_at = time.monotonic()
                    break
                remaining = audio_deadline - time.monotonic()
                if remaining <= 0:
                    break
                time.sleep(min(0.2, remaining))
            (evidence / f"transition-{step + 1:02d}-audio.txt").write_text(audio)
            active_tracks = [line.strip() for line in audio.splitlines()
                             if "AudioPlaybackConfiguration" in line
                             and "type:android.media.AudioTrack" in line
                             and re.search(rf"u/pid:{projectivy_uid}/\d+ state:started", line)]
            active_aura_tracks = [line.strip() for line in audio.splitlines()
                                  if "AudioPlaybackConfiguration" in line
                                  and "type:android.media.AudioTrack" in line
                                  and re.search(rf"u/pid:{aura_uid}/\d+ state:started", line)]

            indicator_wait = wait_for_indicator(
                lambda: (raw := adb(serial, "exec-out", "screencap", "-p", binary=True),
                         cv2.imdecode(np.frombuffer(raw, np.uint8), cv2.IMREAD_COLOR)),
                bounds,
                args.indicator_timeout_seconds if audio_started_at is not None else 0,
            )
            raw = indicator_wait["raw"]
            pink_pixels = indicator_wait["pinkPixels"]
            profile = indicator_wait["profile"]
            roi = indicator_wait["roi"]
            screenshot_name = f"transition-{step + 1:02d}.png"
            (evidence / screenshot_name).write_bytes(raw)

            sample = {
                "step": step + 1,
                "from": current,
                "to": target,
                "key": key,
                "focusEventMatched": matched_line is not None,
                "focusEvent": matched_line,
                "focusLatencySecondsUpperBound": round(event_latency, 3) if event_latency is not None else None,
                "focusedTitles": focused_titles,
                "expectedTitleFocused": matching_node is not None,
                "focusedAncestorContentDescriptions": ancestor_descriptions,
                "focusedProgramInExpectedChannelRow": row_matched,
                "focusedCardBounds": bounds,
                "projectivyForeground": projectivy_foreground,
                "activeProjectivyAudioTrackCount": len(active_tracks),
                "activeProjectivyAudioTracks": active_tracks,
                "projectivyAudioStartupSecondsUpperBound": (
                    round(audio_started_at - start, 3) if audio_started_at is not None else None
                ),
                "activeAuraMusicAudioTracks": active_aura_tracks,
                "pinkIndicatorPixels": pink_pixels,
                "indicatorPollCount": indicator_wait["pollCount"],
                "indicatorVisibleAfterAudio": indicator_wait["visible"] and audio_started_at is not None,
                "indicatorStartupSecondsUpperBound": round(indicator_wait["elapsedSeconds"], 3)
                    if indicator_wait["visible"] and audio_started_at is not None else None,
                "indicatorWaitSeconds": (
                    round(indicator_wait["elapsedSeconds"], 3)
                    if audio_started_at is not None else None
                ),
                "pinkRowProfile": profile,
                "indicatorRegion": roi,
                "screenshot": screenshot_name,
                "screenshotSha256": hashlib.sha256(raw).hexdigest(),
            }
            samples.append(sample)
            current = target if matching_node else current
    finally:
        logcat.terminate()
        try:
            logcat.wait(timeout=3)
        except subprocess.TimeoutExpired:
            logcat.kill()

    provider_logs = adb(serial, "logcat", "-d", "-v", "threadtime", "-s", "TvPreviewContentProvider:I")
    (evidence / "provider-logcat.txt").write_text(provider_logs)
    provider_open_count = count_preview_provider_opens(provider_logs)

    checks = {
        "allRequestedTransitionsCompleted": len(samples) == args.cycles * 2 and all(s["expectedTitleFocused"] for s in samples),
        "projectivyStayedForeground": bool(samples) and all(s["projectivyForeground"] for s in samples),
        "audioStayedActive": bool(samples) and all(s["activeProjectivyAudioTrackCount"] > 0 for s in samples),
        "noMultipleProjectivyTracksObserved": bool(samples) and all(s["activeProjectivyAudioTrackCount"] <= 1 for s in samples),
        "noAuraMusicAudioTrackOverlapObserved": bool(samples) and all(not s["activeAuraMusicAudioTracks"] for s in samples),
        "indicatorVisibleOnEachCard": bool(samples) and all(s["pinkIndicatorPixels"] > 100 for s in samples),
        "programsStayedInExpectedChannelRow": bool(samples) and all(
            s["focusedProgramInExpectedChannelRow"] for s in samples
        ),
        "auraPreviewProviderOpenObserved": provider_open_count > 0,
        "focusEventLatencyObserved": bool(samples) and all(s["focusEventMatched"] for s in samples),
    }
    latencies = [sample["focusLatencySecondsUpperBound"] for sample in samples
                 if sample["focusLatencySecondsUpperBound"] is not None]
    ordered = sorted(latencies)
    p95 = ordered[min(len(ordered) - 1, round((len(ordered) - 1) * 0.95))] if ordered else None
    indicator_latencies = [sample["indicatorStartupSecondsUpperBound"] for sample in samples
                           if sample["indicatorStartupSecondsUpperBound"] is not None]
    ordered_indicator_latencies = sorted(indicator_latencies)
    indicator_p95 = (
        ordered_indicator_latencies[
            min(len(ordered_indicator_latencies) - 1,
                round((len(ordered_indicator_latencies) - 1) * 0.95))
        ] if ordered_indicator_latencies else None
    )
    functional_checks = (
        "allRequestedTransitionsCompleted",
        "projectivyStayedForeground",
        "audioStayedActive",
        "noMultipleProjectivyTracksObserved",
        "noAuraMusicAudioTrackOverlapObserved",
        "indicatorVisibleOnEachCard",
        "programsStayedInExpectedChannelRow",
        "auraPreviewProviderOpenObserved",
    )
    if all(checks[name] for name in functional_checks):
        status = "PASS"
        status_reason = "All observed focus, foreground, playback, overlap, and card-indicator assertions passed."
    elif not checks["allRequestedTransitionsCompleted"] or not checks["projectivyStayedForeground"]:
        status = "INCONCLUSIVE"
        status_reason = "Requested focus/foreground precondition was not observed; input delivery or launcher state is unresolved."
    elif not checks["audioStayedActive"]:
        status = "INCONCLUSIVE"
        status_reason = "Focused-card audio was absent, but this run did not prove the selected media source was reachable."
    elif not checks["auraPreviewProviderOpenObserved"]:
        status = "INCONCLUSIVE"
        status_reason = "No AuraMusic preview-provider open was captured, so the focused audio source is not correlated."
    else:
        status = "FAIL_PRODUCT"
        status_reason = "Focus and media preconditions held, but a playback overlap or card-indicator assertion failed."
    result = {
        "test": "Projectivy repeated real AuraMusic song focus transitions",
        "status": status,
        "scope": "No-click focus transitions, focused title, foreground, active AudioTrack count, and card overlay on each target",
        **metadata,
        "statusReason": status_reason,
        "checks": checks,
        "providerOpenCountObserved": provider_open_count,
        "latencySeconds": {
            "medianUpperBound": float(np.median(latencies)) if latencies else None,
            "p95UpperBound": p95,
            "maxUpperBound": max(latencies) if latencies else None,
            "samples": latencies,
        },
        "indicatorStartupSeconds": {
            "medianUpperBound": float(np.median(indicator_latencies)) if indicator_latencies else None,
            "p95UpperBound": indicator_p95,
            "maxUpperBound": max(indicator_latencies) if indicator_latencies else None,
            "samples": indicator_latencies,
        },
        "samples": samples,
        "limitations": [
            "Active Projectivy AudioTrack state and card focus do not provide a digital capture or spectral identity of each real song.",
            "AudioTrack polling waits up to the configured timeout; very brief sub-sample overlaps may not be observed.",
            "Screenshots are device-composed frames, not HDMI capture.",
        ],
    }
    (evidence / "transition-result.json").write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps({"status": status, "transitions": len(samples), "latencySeconds": result["latencySeconds"],
                      "checks": checks, "result": str(evidence / "transition-result.json")}, indent=2))
    if status != "PASS":
        raise SystemExit(1)


if __name__ == "__main__":
    main()
