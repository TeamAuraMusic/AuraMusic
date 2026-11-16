# Focused-card audio gap follow-up

The candidate removes AuraMusic's 120 ms debounce before asynchronous selected-card prefetch. Unit tests (73 tests) and the FOSS TV ARMv7 debug APK build passed.

## Chromecast result

Projectivy 4.73 was foreground on a Chromecast running Android 14/API 34. Test cards were at the bottom of the Projectivy home screen. The user's disabled channels/categories were left as configured; no launcher settings or owner data were changed. Projectivy accessibility was enabled before and after testing (`accessibility-before.txt`; final `device-metadata.txt` shows it enabled and bound).

Captured device output contains the expected 440 Hz A tone followed by the 880 Hz B tone with no overlap. Reanalysis of the existing pre-change WAV and the new WAV using the same 20 ms window, 10 ms hop, normalized RMS `<0.0001` silence threshold, and 8× tone-dominance rule measured:

- Before: 1.28 s silent A→B boundary.
- After: 1.06 s silent A→B boundary.
- Measured reduction: 0.22 s (17.2%), one transition sample.

A separate 5.888 s device-output capture after focus left the test row was digital silence (RMS and peak both zero). The analyzer's detailed assertion is [`corrected-audio-assertions.json`](chromecast-clean-transition-20261116T/corrected-audio-assertions.json); the fine-grained comparison is [`gap-comparison.json`](chromecast-clean-transition-20261116T/gap-comparison.json). Raw captures, device metadata, action timestamps, and Projectivy/provider logs are in [`chromecast-clean-transition-20261116T/`](chromecast-clean-transition-20261116T/).

This is a measured improvement, not gapless playback. Only one post-change A→B transition was captured; B→A and repeated-transition latency were not measured in this follow-up. The still screenshots do not establish whether card video identity/animation remains correct throughout navigation; this PR changes audio prefetch timing only. Cropped screenshots exclude unrelated personal home-screen rows.

Cleanup removed only the stale test instrumentation package `com.auramusic.app.debug.test`. AuraMusic, Projectivy, and PreviewProbe remain installed.

See [`result.json`](result.json) for machine-readable outcomes and the APK hash.
