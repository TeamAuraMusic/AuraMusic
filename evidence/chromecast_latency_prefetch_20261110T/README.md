# Chromecast focus-prefetch verification

Current candidate installed in place on Chromecast `sabrina`; Projectivy remained foreground. No app data or launcher defaults changed.

- Build and TV unit suite: **PASS**, 73 tests / 16 suites.
- Projectivy restart and real-song preview recovery: **PASS**. `Proud Mary` focused without Select; provider opened local media; Projectivy owned the AudioTrack; original artwork and waveform visible. Pixel comparison found changing pixels in all 15 adjacent pairs across 16 captured frames.
- Focus cleanup: **PASS**. Moving from `Proud Mary` to Preview A switched to one Projectivy track; moving outside preview rows left zero started Projectivy or AuraMusic tracks.
- Focus prefetch: **PASS** for request ordering on this warm-cache run. AuraMusic opened the selected local audio URI and completed prefetch 432 ms before Projectivy requested it. External signed URLs are excluded.
- Cold-cache latency improvement: **INCONCLUSIVE**. This card's cache was warm; no cold file was removed to manufacture a test.
- Synthetic A/B transitions: ordered 440/880 Hz, no mixed-tone windows, with a measured silent boundary of about 1.28 s A→B and 1.26 s B→A. This remains a handoff-latency limitation. [`Audio boundary analysis`](previewprobe-audio-boundaries.json).

Machine assertions: [`result.json`](result.json). Raw build log: [`gradle-final.log`](gradle-final.log). The larger screenshot, logs, and frame bundle are retained in this evidence directory and `../chromecast_projectivy_restart_current_20261110T/`.
