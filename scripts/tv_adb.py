"""Shared, explicitly targeted ADB configuration for Android TV verifiers."""

import os
import subprocess
from pathlib import Path

DEFAULT_ADB_EXECUTABLE = Path(
    os.environ.get("TVCTL_ADB", "/mnt/i/AndroidTVSdk/platform-tools/adb.exe")
)
DEFAULT_ADB_SERVER_PORT = int(os.environ.get("TVCTL_ADB_PORT", "5038"))
EMULATOR_SERIAL_ALLOWLIST = frozenset({"emulator-5554", "emulator-5556"})


def is_allowed_target(serial: str, allow_physical: bool = False) -> bool:
    return serial in EMULATOR_SERIAL_ALLOWLIST or allow_physical


def adb_command(executable: Path, server_port: int, serial: str, *args: str) -> list[str]:
    return [str(executable), "-P", str(server_port), "-s", serial, *args]


def run_adb(
    serial: str,
    *args: str,
    executable: Path = DEFAULT_ADB_EXECUTABLE,
    server_port: int = DEFAULT_ADB_SERVER_PORT,
    timeout: int = 30,
    binary: bool = False,
):
    result = subprocess.run(
        adb_command(executable, server_port, serial, *args),
        check=True,
        capture_output=True,
        text=not binary,
        errors="replace" if not binary else None,
        timeout=timeout,
    )
    return result.stdout
