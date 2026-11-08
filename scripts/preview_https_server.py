#!/usr/bin/env python3
"""Local HTTPS media fixture server with bounded streaming and Range support."""

import argparse
import json
import mimetypes
import ssl
import threading
import time
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import unquote, urlsplit


def parse_range(value: str, size: int):
    if not value or not value.startswith("bytes=") or "," in value:
        return None
    try:
        first, last = value[6:].split("-", 1)
        if not first:
            suffix = int(last)
            if suffix <= 0:
                return None
            return max(0, size - suffix), size - 1
        start = int(first)
        end = min(int(last), size - 1) if last else size - 1
        if start < 0 or start >= size or end < start:
            return None
        return start, end
    except (ValueError, TypeError):
        return None


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", required=True, type=Path)
    parser.add_argument("--cert", required=True, type=Path)
    parser.add_argument("--key", required=True, type=Path)
    parser.add_argument("--log", required=True, type=Path)
    parser.add_argument("--host", default="0.0.0.0")
    parser.add_argument("--port", type=int, default=8443)
    args = parser.parse_args()
    root = args.root.resolve(strict=True)
    args.log.parent.mkdir(parents=True, exist_ok=True)
    log_lock = threading.Lock()

    def record(row):
        row["time"] = datetime.now(timezone.utc).isoformat()
        with log_lock, args.log.open("a", encoding="utf-8") as log:
            log.write(json.dumps(row, separators=(",", ":")) + "\n")
            log.flush()

    class Handler(BaseHTTPRequestHandler):
        protocol_version = "HTTP/1.1"

        def log_message(self, _format, *_args):
            pass

        def do_HEAD(self):
            self.serve_media(head=True)

        def do_GET(self):
            self.serve_media(head=False)

        def serve_media(self, head):
            started = time.monotonic()
            parsed = urlsplit(self.path)
            relative = Path(unquote(parsed.path.lstrip("/")))
            candidate = (root / relative).resolve()
            row = {
                "client": self.client_address[0], "method": self.command,
                "path": parsed.path, "mediaId": candidate.stem,
                "range": self.headers.get("Range"), "status": 500,
                "contentType": None, "bytesSent": 0, "disconnect": False,
            }
            try:
                if not candidate.is_relative_to(root) or not candidate.is_file():
                    row["status"] = 404
                    self.send_error(404)
                    return
                size = candidate.stat().st_size
                selected = parse_range(row["range"], size)
                if row["range"] and selected is None:
                    row["status"] = 416
                    self.send_response(416)
                    self.send_header("Content-Range", f"bytes */{size}")
                    self.send_header("Content-Length", "0")
                    self.end_headers()
                    return
                start, end = selected if selected else (0, size - 1)
                length = max(0, end - start + 1)
                status = 206 if selected else 200
                content_type = mimetypes.guess_type(candidate.name)[0] or "application/octet-stream"
                row["status"] = status
                row["contentType"] = content_type
                self.send_response(status)
                self.send_header("Content-Type", content_type)
                self.send_header("Content-Length", str(length))
                self.send_header("Accept-Ranges", "bytes")
                self.send_header("Cache-Control", "no-store")
                if selected:
                    self.send_header("Content-Range", f"bytes {start}-{end}/{size}")
                self.end_headers()
                if not head:
                    with candidate.open("rb") as media:
                        media.seek(start)
                        remaining = length
                        while remaining:
                            block = media.read(min(256 * 1024, remaining))
                            if not block:
                                break
                            self.wfile.write(block)
                            row["bytesSent"] += len(block)
                            remaining -= len(block)
            except (BrokenPipeError, ConnectionResetError, ssl.SSLError) as exc:
                row["disconnect"] = True
                row["error"] = type(exc).__name__
            except Exception as exc:
                row["error"] = f"{type(exc).__name__}: {exc}"
            finally:
                row["elapsedMs"] = round((time.monotonic() - started) * 1000, 1)
                record(row)

    server = ThreadingHTTPServer((args.host, args.port), Handler)
    server.daemon_threads = True
    context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    context.load_cert_chain(args.cert, args.key)
    server.socket = context.wrap_socket(server.socket, server_side=True)
    print(f"listening=https://{args.host}:{args.port}/ root={root} log={args.log}", flush=True)
    server.serve_forever(poll_interval=0.5)


if __name__ == "__main__":
    main()
