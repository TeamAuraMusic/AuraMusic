#!/usr/bin/env python3
"""Run packaged AuraMusic TV instrumentation tests on an explicit emulator."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

from tv_adb import adb_command, is_allowed_target


ROOT = Path(__file__).resolve().parents[1]
FLAVORS = {
    "standalone": (
        ROOT / "app/build/outputs/apk/standaloneTvUniversal/debug/app-standalone-tv-universal-debug.apk",
        ROOT / "app/build/outputs/apk/androidTest/standaloneTvUniversal/debug/app-standalone-tv-universal-debug-androidTest.apk",
    ),
    "foss": (
        ROOT / "app/build/outputs/apk/fossTvUniversal/debug/app-foss-tv-universal-debug.apk",
        ROOT / "app/build/outputs/apk/androidTest/fossTvUniversal/debug/app-foss-tv-universal-debug-androidTest.apk",
    ),
}
TEST_PACKAGE = "com.auramusic.app.debug.test"
RUNNER = "androidx.test.runner.AndroidJUnitRunner"
ALLOWED_CLASSES = frozenset(
    {
        "com.auramusic.app.tv.FocusedCardWaveformViewInstrumentedTest",
        "com.auramusic.app.tv.TvPreviewAudioTranscoderInstrumentedTest",
        "com.auramusic.app.tv.TvRecommendationClickIntentInstrumentedTest",
        "com.auramusic.app.tv.TvPreviewContentProviderInstrumentedTest",
    }
)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def classify_instrumentation(return_code: int, output: str) -> tuple[str, int | None]:
    if "FAILURES!!!" in output:
        return "FAIL_PRODUCT", None
    if "INSTRUMENTATION_FAILED" in output:
        return "FAIL_TEST_INFRA", None
    match = re.search(r"\bOK\s*\((\d+)\s+tests?\)", output)
    if return_code != 0 or match is None:
        return "FAIL_TEST_INFRA", None
    count = int(match.group(1))
    if count < 1:
        return "INCONCLUSIVE", count
    return "PASS", count


def adb_run(adb: Path, port: int, serial: str, *args: str, timeout: int = 45) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        adb_command(adb, port, serial, *args),
        capture_output=True,
        text=True,
        errors="replace",
        timeout=timeout,
        check=False,
    )


def windows_path(path: Path) -> str:
    result = subprocess.run(["wslpath", "-w", str(path)], capture_output=True, text=True, check=False)
    if result.returncode != 0:
        raise RuntimeError(f"wslpath failed for {path}: {result.stderr.strip()}")
    return result.stdout.strip()


def make_evidence_dir(output_root: Path) -> Path:
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    evidence = output_root / f"tv_instrumentation_{stamp}"
    suffix = 1
    while evidence.exists():
        evidence = output_root / f"tv_instrumentation_{stamp}_{suffix}"
        suffix += 1
    evidence.mkdir(parents=True)
    return evidence


def git_metadata() -> tuple[str, str]:
    commit = subprocess.run(
        ["git", "rev-parse", "HEAD"], cwd=ROOT, capture_output=True, text=True, check=False
    )
    status = subprocess.run(
        ["git", "status", "--short", "--untracked-files=no"],
        cwd=ROOT,
        capture_output=True,
        text=True,
        check=False,
    )
    return commit.stdout.strip() if commit.returncode == 0 else "unknown", status.stdout.strip()


def parser() -> argparse.ArgumentParser:
    result = argparse.ArgumentParser(description=__doc__)
    result.add_argument("--serial", required=True, help="Allowlisted emulator serial, for example emulator-5554")
    result.add_argument("--adb", type=Path, required=True, help="Authoritative Windows ADB executable")
    result.add_argument("--adb-port", type=int, default=5038)
    result.add_argument("--output", type=Path, default=ROOT / "evidence")
    result.add_argument("--flavor", choices=sorted(FLAVORS), default="standalone")
    result.add_argument("--class", dest="test_class", choices=sorted(ALLOWED_CLASSES))
    result.add_argument("--app-apk", type=Path)
    result.add_argument("--test-apk", type=Path)
    return result


def main(argv: list[str] | None = None) -> int:
    args = parser().parse_args(argv)
    if not is_allowed_target(args.serial):
        parser().error(f"refusing non-allowlisted device serial: {args.serial}")
    if args.adb_port < 1 or args.adb_port > 65535:
        parser().error("ADB server port must be in 1..65535")

    default_app, default_test = FLAVORS[args.flavor]
    app_apk = (args.app_apk or default_app).resolve()
    test_apk = (args.test_apk or default_test).resolve()
    for label, apk in (("app", app_apk), ("instrumentation", test_apk)):
        if not apk.is_file():
            parser().error(f"{label} APK not found: {apk}")
    if not args.adb.is_file():
        parser().error(f"ADB executable not found: {args.adb}")

    evidence = make_evidence_dir(args.output.resolve())
    commands: list[list[str]] = []
    git_commit, tracked_status = git_metadata()

    def run(*adb_args: str, timeout: int = 45) -> subprocess.CompletedProcess[str]:
        command = adb_command(args.adb, args.adb_port, args.serial, *adb_args)
        commands.append(command)
        return adb_run(args.adb, args.adb_port, args.serial, *adb_args, timeout=timeout)

    def read(*adb_args: str) -> str:
        command_result = run(*adb_args)
        if command_result.returncode != 0:
            raise RuntimeError(
                f"ADB command failed ({command_result.returncode}): {' '.join(adb_args)}\n"
                f"{command_result.stderr.strip()}"
            )
        return command_result.stdout.strip()

    metadata: dict[str, str] = {}
    result: dict[str, object] = {
        "created_utc": datetime.now(timezone.utc).isoformat(),
        "git_commit": git_commit,
        "tracked_worktree_status": tracked_status,
        "serial": args.serial,
        "flavor": args.flavor,
        "app_apk": {"path": str(app_apk), "sha256": sha256(app_apk)},
        "test_apk": {"path": str(test_apk), "sha256": sha256(test_apk)},
        "command": "am instrument -w -r",
        "test_class": args.test_class,
        "device_accessed": True,
        "instrumentation_executed": False,
    }

    try:
        state = read("get-state")
        if state != "device":
            raise RuntimeError(f"emulator is not ready: ADB state is {state!r}")
        metadata = {
            "model": read("shell", "getprop", "ro.product.model"),
            "api": read("shell", "getprop", "ro.build.version.sdk"),
            "fingerprint": read("shell", "getprop", "ro.build.fingerprint"),
            "boot_completed": read("shell", "getprop", "sys.boot_completed"),
            "features": read("shell", "pm", "list", "features"),
        }
        metadata["projectivy_version"] = ""
        if "android.hardware.type.television" not in metadata["features"] or "android.software.leanback" not in metadata["features"]:
            result.update(status="BLOCKED_ENVIRONMENT", reason="selected emulator image does not expose Android TV and Leanback features")
        elif metadata["boot_completed"] != "1":
            result.update(status="BLOCKED_ENVIRONMENT", reason="selected emulator has not completed boot")
        else:
            projectivy = read("shell", "dumpsys", "package", "com.spocky.projengmenu")
            version = re.search(r"versionName=([^\s]+)", projectivy)
            if version is None:
                result.update(status="BLOCKED_ENVIRONMENT", reason="Projectivy is not installed on selected emulator")
            else:
                metadata["projectivy_version"] = version.group(1)
                result["device"] = metadata
                installs = []
                for apk in (app_apk, test_apk):
                    install_result = run("install", "-r", windows_path(apk), timeout=180)
                    installs.append({"apk": apk.name, "return_code": install_result.returncode, "output": install_result.stdout.strip(), "error": install_result.stderr.strip()})
                    if install_result.returncode != 0:
                        raise RuntimeError(f"APK installation failed: {apk.name}: {install_result.stderr.strip()}")
                result["installs"] = installs
                installed_app = read("shell", "dumpsys", "package", "com.auramusic.app.debug")
                app_version = re.search(r"versionName=([^\s]+)", installed_app)
                metadata["auramusic_version"] = app_version.group(1) if app_version else "unknown"
                test_args = ["shell", "am", "instrument", "-w", "-r"]
                if args.test_class:
                    test_args += ["-e", "class", args.test_class]
                test_args.append(f"{TEST_PACKAGE}/{RUNNER}")
                result["instrumentation_executed"] = True
                execution = run(*test_args, timeout=600)
                output = execution.stdout + execution.stderr
                (evidence / "instrumentation.txt").write_text(output)
                status, count = classify_instrumentation(execution.returncode, output)
                result.update(status=status, test_count=count, instrumentation_exit_code=execution.returncode)
                for name, capture_args in (
                    ("logcat.txt", ("logcat", "-d", "-t", "3000")),
                    ("audioflinger.txt", ("shell", "dumpsys", "media.audio_flinger")),
                    ("foreground.txt", ("shell", "dumpsys", "activity", "activities")),
                ):
                    capture = run(*capture_args, timeout=90)
                    (evidence / name).write_text(capture.stdout + capture.stderr)
        result.setdefault("device", metadata)
    except (OSError, RuntimeError, subprocess.TimeoutExpired) as error:
        result.update(status="FAIL_TEST_INFRA", error=str(error))
    result["adb_commands"] = commands
    result["evidence_dir"] = str(evidence)
    (evidence / "result.json").write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps(result, indent=2))
    return 0 if result.get("status") == "PASS" else 1


if __name__ == "__main__":
    sys.exit(main())
