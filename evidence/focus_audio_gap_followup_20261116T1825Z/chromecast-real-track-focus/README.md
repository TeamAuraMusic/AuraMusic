# Chromecast real-track focus capture

Captured 2026-11-16, 19:23–19:25 Europe/Oslo. Device: Chromecast (`sabrina`), Android 14/API 34, Projectivy 4.73. AuraMusic debug package was version 3.2.0/code 29; pulled installed APK SHA-256 matched the PR candidate `5292bd803e3ca40b1d95f17ec6674c1dc3448839e0bb00fbcb30e70fc653c266`. Source branch commit: `af90f8cd12b920e4ec498e8d1a14db33cb612fd2`.

In Projectivy Home, DPAD focused the two adjacent published AuraMusic song cards without Select. Screenshots show the original artwork and animated pink bars inside each focused card. Projectivy remained top-resumed. Captured output WAVs were non-silent for each selected card. Moving focus outside AuraMusic programs hid the indicator; the following 5.78-second loopback capture measured -91 dBFS mean and peak (near-silence, not exact digital zero).

AuraMusic's focus service logged prefetch-ready at 2266 ms on the first card and 1275 ms on the next. These are preparation completion times, not audio-start latency. The captures establish real-card focus playback/visual behavior on this device; they do not provide a controlled before/after gap measurement or audio-source identity comparison. Results therefore do not establish how much this PR's 120 ms debounce removal changed perceived gap.

Projectivy Accessibility was enabled and bound after the test. Existing channel/category filters were preserved. No channel/category settings changed.

See `assertions.json`, screenshots, WAV captures, and `projectivy-focus-log.txt`.

A screen-only scrcpy clip of the focused “Nocturne for the Abyss” card is `card-animation-b.mp4`. At 2 fps, 13/13 ROI samples inside that card contain the pink indicator; all 12 adjacent sample transitions change more than 100 pixels. See `animation-analysis.json`. Real-track audio loopback captures stay local and are intentionally excluded from the public PR commit; `assertions.json` records their measured RMS/peak levels and local paths.

A later synchronized DPAD A/B capture is separately marked **INCONCLUSIVE** in `live-transition-audio-result.json`: UI focus changed and Projectivy logged an active playback state/provider opens, but scrcpy loopback stayed at -91 dBFS for the full 13.824 seconds. Android reported music stream volume index 25. This cannot distinguish an audio capture/route problem from playback failure, so no transition gap is asserted. The quiet sample remains local and is not part of the public PR commit.
