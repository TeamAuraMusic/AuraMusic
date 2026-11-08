# Android TV lab

Windows emulator and SDK data live on I: (SSD):

- SDK: `I:\AndroidTVSdk`
- AVD home: `I:\AndroidTVAvd`
- Local HTTPS fixtures/certificates/logs: `I:\temp\AuraMusicPreviewHttps`
- AVDs: `AuraMusicTVApi34`, `AuraMusicTVApi36`

`C:\AndroidTVSdk` and `C:\AndroidTVAvd` are compatibility junctions to I:. Keep them in place for older checkouts. F: contains an old cold-storage AVD backup and remains untouched. The historical `AuraMusicTvRootApi36` image is unverified; do not use it as the standard TV target.

## Control

Run from the implementation worktree:

```bash
scripts/tvctl doctor --json
scripts/tvctl boot
TVCTL_AVD=AuraMusicTVApi34 scripts/tvctl boot
TVCTL_NO_SNAPSHOT=1 scripts/tvctl boot
scripts/tvctl screenshot
scripts/tvctl hierarchy
scripts/tvctl foreground
scripts/tvctl key DOWN
```

`tvctl` uses Windows ADB on port 5038 and allows only emulator serials `emulator-5554` and `emulator-5556`; it never selects a physical device implicitly. `TVCTL_NO_SNAPSHOT=1` cold-boots without removing saved snapshots. Default target is API 36. Run only one AVD at a time.

Audio/video capture commands require scrcpy on Windows:

```bash
scripts/tvctl capture-audio 10 evidence/audio.wav
scripts/tvctl capture-video 10 evidence/video.mp4
```

The Android TV emulator is hosted on Windows. Do not run the Linux emulator binary from WSL. Keep active AVD data on C: or I: SSD; current lab uses I:.

## Local HTTPS fixtures

See [preview_https_test_server.md](preview_https_test_server.md). The diagnostic server serves from I:, logs requests there, and is not started automatically. Projectivy 4.73 rejected the diagnostic CA in the recorded API 36 test; this is not a Chromecast trust solution.
