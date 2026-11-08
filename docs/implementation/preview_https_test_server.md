# Projectivy local HTTPS media test

This diagnostic host serves the deterministic PreviewProbe A/B MP4s from the Windows C: SSD. It supports TLS, `HEAD`, single byte ranges, bounded chunked file reads, MIME types, status/Range logs, client disconnects, and server TLS errors. It does not alter AuraMusic production preview URIs.

## Start

Use the current Windows LAN IPv4 address for `LAN_IP`; the address observed in this run was `192.168.3.9`.

```bash
scripts/generate-preview-https-cert.sh /mnt/i/temp/AuraMusicPreviewHttps LAN_IP
powershell.exe -NoProfile -ExecutionPolicy Bypass \
  -File "$(wslpath -w scripts/start-preview-https.ps1)" \
  -RepoRoot "$(wslpath -w "$PWD")" \
  -Root 'I:\temp\AuraMusicPreviewHttps'
```

The generator creates a diagnostic CA and a 30-day leaf certificate with IP SANs for emulator gateway `10.0.2.2` and the supplied LAN IP. Keep the generated private keys local; do not commit them. Windows Python 3.12 is used by the host startup script. The server binds all Windows IPv4 interfaces on port 8443 while running.

Verify TLS and a partial response from Windows:

```powershell
& 'C:\Users\nos\AppData\Local\Programs\Python\Python312\python.exe' `
  'I:\temp\AuraMusicPreviewHttps\https_probe.py' `
  'https://192.168.3.9:8443/fixture_a.mp4' `
  --ca 'I:\temp\AuraMusicPreviewHttps\preview-ca.crt' `
  --range 'bytes=0-1023'
```

Expected: JSON `status:206`, `contentRange:bytes 0-1023/<size>`, `contentType:video/mp4`, and 1024 bytes read. Logs append to `I:\temp\AuraMusicPreviewHttps\requests.jsonl`; stdout/stderr go to `server.stdout.log` and `server.stderr.log`.

## Loopback regression test

Run the local server/probe integration without exposing a listener to the LAN or using an Android device:

```bash
rtk python3 scripts/test_preview_https_server.py \
  --evidence-dir evidence/<run-id>/https-loopback
```

The harness binds only `127.0.0.1`, creates a temporary CA and leaf certificate with a loopback SAN, and verifies TLS through that CA file. It checks `HEAD`, full `GET`, exact and suffix `Range`, invalid multi-range, missing file, path traversal, MIME type, and request logging. Temporary keys stay outside the repository and are discarded after the run. **PASS** here proves the loopback fixture server and client only; it says nothing about Projectivy trusting the diagnostic CA, LAN routing, or Chromecast playback.

## Exercise Projectivy

Install the PreviewProbe debug APK. Its publisher accepts deterministic mutation arguments. Pass `previewprobe_publish=true` to publish on launch; omit it to open the app and use the button. `previewprobe_programs` is an ordered comma-separated subset of A-H; omitted means default A-H, and an empty string removes all probe programs. Stable IDs are retained and each supplied position is written as a descending TvProvider weight. Projectivy may choose its own display order, so inspect provider metadata and the actual row separately.

```bash
scripts/tvctl install previewprobe/build/outputs/apk/debug/previewprobe-debug.apk
/mnt/i/AndroidTVSdk/platform-tools/adb.exe -P 5038 -s emulator-5554 shell \
  am start -n com.auramusic.previewprobe/.MainActivity \
  --es preview_https_base https://10.0.2.2:8443/ \
  --ez previewprobe_publish true --es previewprobe_programs A,B
```

Repeat with `--es previewprobe_programs B,A,C` to update, add C, and change weights; use `--es previewprobe_programs B` to remove A/C; use `--es previewprobe_programs ''` to remove all. Capture `PreviewProbe` logcat and query `content://android.media.tv/preview_program` after each operation to assert stable IDs, set membership, and weights. This publisher is deterministic test infrastructure; it does not prove Projectivy has refreshed its visible row until the home screen is inspected.

The HTTPS mode uses fresh A/B provider IDs and prunes its previous content-URI rows, so Projectivy must resolve the test HTTPS URIs. C remains on its local TvInput experiment URI. Focus A/B in Projectivy without selecting them. Projectivy must remain foreground. Inspect output audio, the focused card, HTTP request log, and logcat; a foreground card alone does not prove the URL loaded.

## Current measured result

Host-side TLS and HTTP Range work. Projectivy 4.73 on the stock Android TV API 36 image rejected this local CA with `SSLHandshakeException: CertPathValidatorException: Trust anchor for certification path not found`; its security settings had no CA install flow and the image does not permit `adb root`. No Projectivy HTTP request reached the server and focus audio was silent. See `evidence/https_projectivy_attempt_20261106T171100Z/assertions.json`.

This diagnostic CA is not a Chromecast trust solution. A physical test needs a publicly trusted certificate for a hostname that resolves to the Windows host on the LAN, or another trust path demonstrated on an unmodified Projectivy installation. Do not disable TLS verification. Stop the server after the test with:

```powershell
Stop-Process -Id (Get-Content 'I:\temp\AuraMusicPreviewHttps\server.pid')
```
