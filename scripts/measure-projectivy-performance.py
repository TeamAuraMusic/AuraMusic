#!/usr/bin/env python3
"""Sample Projectivy/AuraMusic CPU, memory, audio state, and preview-cache size."""

import argparse
import json
import re
import statistics
import subprocess
import time
from datetime import datetime, timezone
from pathlib import Path


ADB = "/home/nos/android-sdk/platform-tools/adb"
PACKAGES = ("com.spocky.projengmenu", "com.auramusic.app.debug")


def adb(serial: str, *args: str, timeout: int = 45) -> str:
    result = subprocess.run(
        [ADB, "-s", serial, *args], capture_output=True, text=True,
        errors="replace", check=True, timeout=timeout,
    )
    return result.stdout


def package_pid(serial: str, package: str) -> int | None:
    result = adb(serial, "shell", "pidof", package).strip().split()
    return int(result[0]) if result else None


def package_uid(serial: str, package: str) -> int:
    output = adb(serial, "shell", "cmd", "package", "list", "packages", "-U", package)
    match = re.search(rf"package:{re.escape(package)}\s+uid:(\d+)", output)
    if not match:
        raise SystemExit(f"INCONCLUSIVE: could not resolve UID for {package}")
    return int(match.group(1))


def cache_sizes(serial: str) -> dict[str, int | None]:
    sizes = {}
    for name in ("tv_preview_audio", "tv_preview_video", "tv_preview_gif"):
        try:
            result = adb(
                serial, "shell", "run-as", "com.auramusic.app.debug", "du", "-sk",
                f"cache/{name}",
            )
        except subprocess.CalledProcessError:
            sizes[name] = None
            continue
        match = re.match(r"(\d+)\s+cache/", result.strip())
        sizes[name] = int(match.group(1)) * 1024 if match else None
    return sizes


def parse_top(raw: str) -> dict[str, dict]:
    lines = raw.splitlines()
    header = None
    rows = {}
    for line in lines:
        fields = line.split()
        if "PID" in fields and "RES" in fields and any("%CPU" in field for field in fields):
            header = []
            for field in fields:
                if field == "S[%CPU]":
                    header.extend(("S", "%CPU"))
                else:
                    header.append(field)
            continue
        if header and fields and fields[0].isdigit():
            row = dict(zip(header, fields))
            rows[row["PID"]] = {
                "cpuPercent": float(row["%CPU"]),
                "resident": row["RES"],
                "state": row.get("S"),
                "command": row.get("ARGS", row.get("CMDLINE", "")),
            }
    return rows


def memory(serial: str, pid: int) -> dict[str, int | None]:
    raw = adb(serial, "shell", "dumpsys", "meminfo", str(pid))
    match = re.search(r"TOTAL PSS:\s*(\d+).*?TOTAL RSS:\s*(\d+)", raw, re.S)
    return {
        "pssKb": int(match.group(1)) if match else None,
        "rssKb": int(match.group(2)) if match else None,
    }


def audio_tracks(serial: str) -> list[str]:
    raw = adb(serial, "shell", "dumpsys", "audio")
    return [
        line.strip() for line in raw.splitlines()
        if "AudioPlaybackConfiguration" in line
        and "type:android.media.AudioTrack" in line
        and "state:started" in line
    ]


def summarize(samples: list[dict], path: str) -> dict:
    values = [sample[path] for sample in samples if sample[path] is not None]
    if not values:
        return {"samples": 0, "median": None, "p95": None, "max": None}
    ordered = sorted(values)
    return {
        "samples": len(values),
        "median": statistics.median(values),
        "p95": ordered[min(len(ordered) - 1, round((len(ordered) - 1) * 0.95))],
        "max": max(values),
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True, help="Explicit ADB serial; no device fallback")
    parser.add_argument("--samples", type=int, default=10)
    parser.add_argument("--interval-seconds", type=float, default=2.0)
    parser.add_argument(
        "--expect-audio", choices=("playing", "stopped"), default="playing",
        help="Expected Projectivy preview AudioTrack state during each sample",
    )
    parser.add_argument("--evidence-dir", required=True, type=Path)
    args = parser.parse_args()
    if args.samples < 1 or args.interval_seconds <= 0:
        parser.error("samples and interval must be positive")

    evidence = args.evidence_dir
    evidence.mkdir(parents=True, exist_ok=True)
    pids = {package: package_pid(args.serial, package) for package in PACKAGES}
    uids = {package: package_uid(args.serial, package) for package in PACKAGES}
    present_pids = [pid for pid in pids.values() if pid is not None]
    if not present_pids:
        raise SystemExit("INCONCLUSIVE: neither target process is running")

    metadata = {
        "serial": args.serial,
        "startedUtc": datetime.now(timezone.utc).isoformat(),
        "model": adb(args.serial, "shell", "getprop", "ro.product.model").strip(),
        "device": adb(args.serial, "shell", "getprop", "ro.product.device").strip(),
        "androidApi": adb(args.serial, "shell", "getprop", "ro.build.version.sdk").strip(),
        "pids": pids,
        "uids": uids,
        "sampleCount": args.samples,
        "intervalSeconds": args.interval_seconds,
        "expectedAudio": args.expect_audio,
        "cacheBytesBefore": cache_sizes(args.serial),
    }
    (evidence / "run-metadata.json").write_text(json.dumps(metadata, indent=2) + "\n")

    samples = []
    for index in range(args.samples):
        current_pids = {package: package_pid(args.serial, package) for package in PACKAGES}
        raw_top = adb(
            args.serial, "shell", "top", "-b", "-n", "1", "-p",
            ",".join(str(pid) for pid in current_pids.values() if pid is not None),
        )
        (evidence / f"sample-{index:02d}-top.txt").write_text(raw_top)
        top = parse_top(raw_top)
        process_samples = {}
        for package, pid in current_pids.items():
            process_samples[package] = {
                "pid": pid,
                **(top.get(str(pid), {}) if pid else {}),
                **(memory(args.serial, pid) if pid else {"pssKb": None, "rssKb": None}),
            }
        tracks = audio_tracks(args.serial)
        sample = {
            "index": index,
            "elapsedSeconds": round(time.monotonic(), 3),
            "processes": process_samples,
            "startedAudioTrackCount": len(tracks),
            "projectivyAudioTrackCount": sum(
                f"u/pid:{uids['com.spocky.projengmenu']}/" in line for line in tracks
            ),
            "auraMusicAudioTrackCount": sum(
                f"u/pid:{uids['com.auramusic.app.debug']}/" in line for line in tracks
            ),
        }
        samples.append(sample)
        (evidence / f"sample-{index:02d}.json").write_text(json.dumps(sample, indent=2) + "\n")
        if index + 1 < args.samples:
            time.sleep(args.interval_seconds)

    cache_after = cache_sizes(args.serial)
    summaries = {}
    for package in PACKAGES:
        summaries[package] = {
            metric: summarize(
                [sample["processes"][package] for sample in samples], metric
            )
            for metric in ("cpuPercent", "pssKb", "rssKb")
        }
    expected_projectivy_tracks = 1 if args.expect_audio == "playing" else 0
    result = {
        "outcome": "PASS" if all(sample["projectivyAudioTrackCount"] == expected_projectivy_tracks
                                  and sample["auraMusicAudioTrackCount"] == 0
                                  for sample in samples) else "INCONCLUSIVE",
        "scope": "Steady-state process resource samples and started AudioTrack identity; no acoustic capture.",
        "metadata": metadata,
        "summaries": summaries,
        "cacheBytesBefore": metadata["cacheBytesBefore"],
        "cacheBytesAfter": cache_after,
        "cacheByteDelta": {
            key: (
                cache_after[key] - metadata["cacheBytesBefore"][key]
                if cache_after.get(key) is not None
                and metadata["cacheBytesBefore"].get(key) is not None
                else None
            )
            for key in set(cache_after) | set(metadata["cacheBytesBefore"])
        },
        "audioTrackAssertions": {
            "expectedProjectivyTrackCountEverySample": all(
                s["projectivyAudioTrackCount"] == expected_projectivy_tracks for s in samples
            ),
            "expectedProjectivyTrackCount": expected_projectivy_tracks,
            "noAuraMusicTrackOverlapEverySample": all(s["auraMusicAudioTrackCount"] == 0 for s in samples),
        },
        "samples": samples,
        "limitations": [
            "CPU readings are short top snapshots, not a controlled benchmark or whole-device power measurement.",
            "AudioTrack state does not establish acoustic song identity.",
        ],
    }
    (evidence / "performance-result.json").write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps({key: result[key] for key in ("outcome", "summaries", "cacheBytesBefore",
                                                   "cacheBytesAfter", "cacheByteDelta",
                                                   "audioTrackAssertions")}, indent=2))


if __name__ == "__main__":
    main()
