#!/usr/bin/env python3
"""Assert animated pink indicator is visible inside a focused Projectivy program card."""
import argparse
import hashlib
import json
import re
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path

import numpy as np


def probe_video(path: Path) -> dict:
    result = subprocess.run(
        ["ffprobe", "-v", "error", "-show_entries", "format=duration,size:stream=codec_name,width,height,avg_frame_rate", "-of", "json", str(path)],
        check=True, capture_output=True, text=True,
    )
    payload = json.loads(result.stdout)
    video = next(stream for stream in payload["streams"] if stream.get("codec_name") == "h264")
    return {
        "durationSeconds": float(payload["format"]["duration"]),
        "bytes": int(payload["format"]["size"]),
        "codec": video["codec_name"],
        "width": int(video["width"]),
        "height": int(video["height"]),
        "frameRate": video["avg_frame_rate"],
        "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
    }


def focused_program(path: Path, title: str, channel_name: str) -> dict:
    root = ET.parse(path).getroot()
    parents = {child: parent for parent in root.iter("node") for child in parent}
    match = next((node for node in root.iter("node")
                  if node.get("focused") == "true" and node.get("content-desc") == title), None)
    ancestors = []
    ancestor = parents.get(match) if match is not None else None
    while ancestor is not None:
        description = ancestor.get("content-desc", "").strip()
        if description:
            ancestors.append(description)
        ancestor = parents.get(ancestor)
    row_match = channel_name in ancestors
    return {
        "title": title,
        "bounds": match.get("bounds") if match is not None else None,
        "focused": match is not None,
        "channelName": channel_name,
        "ancestorContentDescriptions": ancestors,
        "inExpectedChannelRow": row_match,
    }


def classify_outcome(checks: dict, provider_hit: bool) -> str:
    preconditions_observed = checks["projectivyFocused"] and checks["projectivyForeground"]
    if not preconditions_observed:
        return "INCONCLUSIVE"
    if any(not checks[name] for name in (
        "focusedProgramInExpectedChannelRow",
        "pinkIndicatorVisibleInCard",
        "pinkIndicatorAnimatesInCard",
    )):
        return "FAIL_PRODUCT"
    return "PASS" if provider_hit else "INCONCLUSIVE"


def analyze_card(path: Path, roi: tuple[int, int, int, int]) -> dict:
    x, y, width, height = roi
    result = subprocess.run(
        ["ffmpeg", "-v", "error", "-i", str(path), "-vf", f"fps=2,crop={width}:{height}:{x}:{y}", "-an", "-f", "rawvideo", "-pix_fmt", "rgb24", "pipe:1"],
        check=True, capture_output=True,
    )
    frame_bytes = width * height * 3
    frames = [result.stdout[i:i + frame_bytes] for i in range(0, len(result.stdout), frame_bytes) if len(result.stdout[i:i + frame_bytes]) == frame_bytes]
    profiles = []
    pink_counts = []
    for frame in frames:
        rgb = np.frombuffer(frame, dtype=np.uint8).reshape(height, width, 3)
        red, green, blue = rgb[:, :, 0], rgb[:, :, 1], rgb[:, :, 2]
        pink = (red > 150) & (red > green * 1.35) & (blue > 60) & (green < 150)
        pink_counts.append(int(pink.sum()))
        profiles.append(pink.sum(axis=1).astype(np.int32))
    deltas = [int(np.abs(a - b).sum()) for a, b in zip(profiles, profiles[1:])]
    transitions = [delta for delta, a, b in zip(deltas, pink_counts, pink_counts[1:]) if a > 100 and b > 100]
    return {
        "roi": {"x": x, "y": y, "width": width, "height": height},
        "sampleRateFps": 2,
        "samples": len(frames),
        "samplesWithPinkIndicator": sum(count > 100 for count in pink_counts),
        "distinctPinkRowProfiles": len({profile.tobytes() for profile in profiles if profile.sum() > 100}),
        "adjacentVisibleTransitions": len(transitions),
        "transitionsChangingOver100Pixels": sum(delta > 100 for delta in transitions),
        "minimumVisibleProfileDelta": min(transitions, default=0),
        "medianVisibleProfileDelta": float(np.median(transitions)) if transitions else 0,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--video", type=Path, required=True)
    parser.add_argument("--hierarchy", type=Path, required=True)
    parser.add_argument("--foreground", type=Path, required=True)
    parser.add_argument("--provider-logs", type=Path, required=True)
    parser.add_argument("--title", default="Five")
    parser.add_argument("--channel-name", default="AuraMusic")
    parser.add_argument(
        "--roi",
        nargs=4,
        type=int,
        metavar=("X", "Y", "WIDTH", "HEIGHT"),
        default=(230, 720, 130, 55),
        help="capture-pixel region containing the card's animated indicator",
    )
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()

    focused = focused_program(args.hierarchy, args.title, args.channel_name)
    foreground_text = args.foreground.read_text(errors="replace")
    foreground_line = next(
        (line.strip() for line in foreground_text.splitlines() if "topResumedActivity=" in line),
        "",
    )
    provider_logs = args.provider_logs.read_text(errors="replace")
    video = probe_video(args.video)
    card = analyze_card(args.video, tuple(args.roi))
    provider_hit = bool(
        re.search(r"TV preview seekable cache hit .*bytes=\d+", provider_logs)
        or re.search(r"TV preview audio-only open .*bytes=\d+", provider_logs)
    )
    checks = {
        "projectivyFocused": focused["focused"],
        "focusedProgramInExpectedChannelRow": focused["inExpectedChannelRow"],
        "projectivyForeground": "com.spocky.projengmenu/.ui.home.MainActivity" in foreground_line,
        "auraPreviewProviderServedMedia": provider_hit,
        "pinkIndicatorVisibleInCard": card["samplesWithPinkIndicator"] >= 3,
        "pinkIndicatorAnimatesInCard": card["transitionsChangingOver100Pixels"] >= 3,
    }
    outcome = classify_outcome(checks, provider_hit)
    payload = {
        "test": "Projectivy focused AuraMusic program card waveform motion",
        "outcome": outcome,
        "scope": "Focused-card visual assertion plus provider-media correlation; this harness does not capture audio.",
        "focus": focused,
        "foreground": foreground_line,
        "providerMediaServedObserved": provider_hit,
        "capture": {"file": str(args.video), **video},
        "cardMotion": card,
        "checks": checks,
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(payload, indent=2) + "\n")
    print(json.dumps(payload, indent=2))
    if outcome != "PASS":
        raise SystemExit(1)


if __name__ == "__main__":
    main()
