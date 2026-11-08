#!/usr/bin/env python3
"""Verify a local HTTPS media endpoint and optional byte-range response."""

import argparse
import json
import ssl
import urllib.request


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("url")
    parser.add_argument("--ca", required=True)
    parser.add_argument("--range", default="bytes=0-1023")
    args = parser.parse_args()
    context = ssl.create_default_context(cafile=args.ca)
    request = urllib.request.Request(args.url, headers={"Range": args.range})
    with urllib.request.urlopen(request, context=context, timeout=8) as response:
        body = response.read(1024 * 1024)
        result = {
            "status": response.status,
            "contentType": response.headers.get("Content-Type"),
            "contentRange": response.headers.get("Content-Range"),
            "acceptRanges": response.headers.get("Accept-Ranges"),
            "bytesRead": len(body),
        }
    print(json.dumps(result, separators=(",", ":")))


if __name__ == "__main__":
    main()
