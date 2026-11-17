# Chromecast Projectivy PreviewProbe A/B focus evidence

Captured 2026-11-16, 20:11–20:13 Europe/Oslo. Chromecast `sabrina`, Android 14/API 34; Projectivy 4.73. AuraMusic 3.2.0 debug candidate was installed and its pulled APK hash matches the locally built artifact (`049b320b…57643f73f`).

From the Projectivy home screen, DPAD focus moved from AuraMusic “All Summer Long” up to Preview A, right to Preview B, left to Preview A, then down back to the AuraMusic song card. No Select/Enter was sent. UIAutomator confirms the focused labels and metadata. Projectivy stayed foreground. Projectivy Accessibility and AuraMusic’s focus service were enabled and bound before and after. User channel/category filters were left as found.

`scrcpy 4.1` captured screen and device playback for each focused fixture. FFT analysis of the captured output found a stable 440 Hz tone for A and 880 Hz for B, matching the deterministic fixture metadata. Clips are cropped to the PreviewProbe row; their audio is synthetic.

The pixel-level visual result is **INCONCLUSIVE**. The captured A/B tile pixels appear black or show a neighboring image instead of a definitive selected-frame identity. Projectivy renders preview video through a separate SurfaceView, which scrcpy/screencap can omit or miscompose on this device. The focus labels are reliable; these pixels do not prove a product failure or visual pass. No visual pass is claimed. This is separate from prior real AuraMusic-card evidence, where the overlay animation was captured inside the artwork.

The complete Android TV unit-test suite passed: 75 tests across 16 suites, no failures/errors/skips. A forced Android TV variant rebuild completed successfully in 5m33s; its APK hash matches the APK pulled from the Chromecast, and both contain the right-neighbor implementation. See `../../focus_audio_gap_followup_20261116T1825Z/chromecast-real-track-focus/README.md` for the real-track evidence and this folder’s `prefetch-events.txt` for measured prewarm events. Preparation-ready durations are not audio-start latency. The exact audible gap reduction remains unmeasured.

Files:

- `result.json`: machine-readable assertions and outcomes.
- `preview-A-row.mp4`, `preview-B-row.mp4`: cropped screen + synthetic device-audio recordings.
- `focus-A.png`, `focus-B.png`: cropped card row and focused title/metadata.
- `focus-A.xml`, `focus-B.xml`, `restored-aura-focus.xml`: UIAutomator hierarchy captures.
- `device-state.txt`, `foreground-activity.txt`, `enabled-accessibility-services.txt`, `accessibility-bound-services.txt`: device/app state and service persistence.
- `chromecast-cleanup-check.txt`: confirms the two temporary remote screenshot/XML files were removed.
- `right-neighbor-prefetch-log.txt`, `prefetch-events.txt`: real AuraMusic prefetch logs.
- `installed-apk-sha256.txt`, `build-and-installed-apk-sha256.txt`, `evidence-sha256.txt`: artifact/evidence hashes.
- `focused-logcat.txt`: small relevant log excerpt around the fixture captures.

Original uncropped recordings remain on the local I: drive at `I:\temp\preview-fixture-capture`; no real-song audio was copied into this evidence folder.
