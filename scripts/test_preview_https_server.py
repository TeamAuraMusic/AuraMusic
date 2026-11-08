#!/usr/bin/env python3
"""Exercise the HTTPS fixture server on loopback with a locally trusted test CA."""

import argparse
import atexit
import hashlib
import json
import socket
import ssl
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path


REPO = Path(__file__).resolve().parents[1]


def request(url: str, context: ssl.SSLContext, method: str = "GET", range_header: str | None = None):
    headers = {"Range": range_header} if range_header else {}
    req = urllib.request.Request(url, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, context=context, timeout=5) as response:
            body = response.read()
            return response.status, response.headers, body
    except urllib.error.HTTPError as error:
        with error:
            return error.code, error.headers, error.read()


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--evidence-dir", required=True, type=Path)
    args = parser.parse_args()
    evidence = args.evidence_dir.resolve()
    evidence.mkdir(parents=True, exist_ok=True)
    started_utc = datetime.now(timezone.utc).isoformat()
    fixture = REPO / "previewprobe/src/main/res/raw/fixture_a.mp4"
    expected = fixture.read_bytes()

    with tempfile.TemporaryDirectory(prefix="auramusic-preview-https-") as temporary:
        root = Path(temporary)
        media_root = root / "media"
        media_root.mkdir()
        (media_root / fixture.name).write_bytes(expected)
        (root / "outside.mp4").write_bytes(b"outside-root")
        cert_dir = root / "cert"
        subprocess.run(
            [str(REPO / "scripts/generate-preview-https-cert.sh"), str(cert_dir), "127.0.0.1"],
            check=True,
            capture_output=True,
            text=True,
        )
        with socket.socket() as listener:
            listener.bind(("127.0.0.1", 0))
            port = listener.getsockname()[1]

        request_log = root / "requests.jsonl"
        stdout_path = root / "server.stdout.log"
        stderr_path = root / "server.stderr.log"
        with stdout_path.open("w") as stdout, stderr_path.open("w") as stderr:
            server = subprocess.Popen(
                [
                    sys.executable, str(REPO / "scripts/preview_https_server.py"),
                    "--root", str(media_root),
                    "--cert", str(cert_dir / "preview-server.crt"),
                    "--key", str(cert_dir / "preview-server.key"),
                    "--log", str(request_log),
                    "--host", "127.0.0.1",
                    "--port", str(port),
                ],
                stdout=stdout,
                stderr=stderr,
            )

            def stop_server() -> None:
                if server.poll() is None:
                    server.terminate()
                try:
                    server.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    server.kill()
                    server.wait(timeout=5)

            atexit.register(stop_server)
            context = ssl.create_default_context(cafile=str(cert_dir / "preview-ca.crt"))
            base = f"https://127.0.0.1:{port}"
            deadline = time.monotonic() + 8
            while True:
                if server.poll() is not None:
                    raise RuntimeError(f"HTTPS fixture server exited with status {server.returncode}")
                try:
                    head_status, head_headers, _ = request(base + "/fixture_a.mp4", context, method="HEAD")
                    break
                except (OSError, ssl.SSLError, urllib.error.URLError):
                    if time.monotonic() >= deadline:
                        raise
                    time.sleep(0.1)

            probe = subprocess.run(
                [
                    "python3", str(REPO / "scripts/https_probe.py"),
                    base + "/fixture_a.mp4", "--ca", str(cert_dir / "preview-ca.crt"),
                    "--range", "bytes=1024-2047",
                ],
                check=True,
                capture_output=True,
                text=True,
            )
            range_probe = json.loads(probe.stdout)
            suffix_status, suffix_headers, suffix_body = request(
                base + "/fixture_a.mp4", context, range_header="bytes=-512"
            )
            invalid_status, invalid_headers, _ = request(
                base + "/fixture_a.mp4", context, range_header="bytes=0-1,3-4"
            )
            missing_status, _, _ = request(base + "/missing.mp4", context)
            traversal_status, _, _ = request(base + "/%2e%2e/outside.mp4", context)
            full_status, full_headers, full_body = request(base + "/fixture_a.mp4", context)

            checks = {
                "loopbackOnly": server.args[server.args.index("--host") + 1] == "127.0.0.1",
                "headStatus200": head_status == 200,
                "headLengthMatchesFixture": int(head_headers.get("Content-Length", -1)) == len(expected),
                "rangeProbeStatus206": range_probe["status"] == 206,
                "rangeContentRangeCorrect": range_probe["contentRange"] == f"bytes 1024-2047/{len(expected)}",
                "rangeBytesRead1024": range_probe["bytesRead"] == 1024,
                "suffixRangeStatus206": suffix_status == 206,
                "suffixBytesExact": suffix_body == expected[-512:],
                "invalidMultipleRangeStatus416": invalid_status == 416,
                "invalidRangeContentRange": invalid_headers.get("Content-Range") == f"bytes */{len(expected)}",
                "missingFileStatus404": missing_status == 404,
                "pathTraversalStatus404": traversal_status == 404,
                "fullGetStatus200": full_status == 200,
                "fullBodyExact": full_body == expected,
                "videoContentType": full_headers.get("Content-Type") == "video/mp4",
            }
            stop_server()

        rows = [json.loads(line) for line in request_log.read_text().splitlines() if line.strip()]
        checks["requestLogCoversSuccessAndFailure"] = {200, 206, 404, 416}.issubset(
            {row["status"] for row in rows}
        )
        checks["requestLogRecordsRangeAndMediaId"] = any(
            row["range"] == "bytes=1024-2047" and row["mediaId"] == "fixture_a"
            for row in rows
        )
        (evidence / "requests.jsonl").write_text(request_log.read_text())
        (evidence / "server.stdout.log").write_text(stdout_path.read_text())
        (evidence / "server.stderr.log").write_text(stderr_path.read_text())

    result = {
        "status": "PASS" if all(checks.values()) else "FAIL_PRODUCT",
        "startedUtc": started_utc,
        "command": ["rtk", "python3", "scripts/test_preview_https_server.py", *sys.argv[1:]],
        "gitCommit": subprocess.run(
            ["git", "-C", str(REPO), "rev-parse", "HEAD"], capture_output=True, text=True, check=True
        ).stdout.strip(),
        "gitDirty": bool(subprocess.run(
            ["git", "-C", str(REPO), "status", "--porcelain"], capture_output=True, text=True, check=True
        ).stdout.strip()),
        "pythonVersion": sys.version.split()[0],
        "scope": "Loopback HTTPS fixture server only; diagnostic CA is trusted by the probe, not by Projectivy/Chromecast.",
        "serverHost": "127.0.0.1",
        "serverPort": port,
        "fixture": "PreviewProbe Program A video",
        "fixtureBytes": len(expected),
        "fixtureSha256": hashlib.sha256(expected).hexdigest(),
        "tlsVerification": "Enabled using generated local CA via SSLContext(cafile); no verification bypass.",
        "checks": checks,
        "requestsRecorded": len(rows),
        "limitations": [
            "Does not verify LAN routing, Chromecast reachability, Projectivy trust of the diagnostic CA, or media rendering.",
            "The ephemeral CA and private key were kept outside the repository and removed with the temporary directory.",
        ],
    }
    (evidence / "result.json").write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps({"status": result["status"], "checks": sum(checks.values()), "total": len(checks), "result": str(evidence / "result.json")}, indent=2))
    if result["status"] != "PASS":
        raise SystemExit(1)


if __name__ == "__main__":
    main()
