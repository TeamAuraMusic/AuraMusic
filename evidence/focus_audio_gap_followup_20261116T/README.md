# Focused-card audio gap follow-up

The candidate removes AuraMusic's 120 ms debounce before asynchronous selected-card prefetch. Unit tests (73 tests) and the FOSS TV ARMv7 debug APK build passed.

## Chromecast result

Projectivy 4.73 was foreground on a Chromecast running Android 14/API 34. Test cards were at the bottom of the Projectivy home screen. The user's disabled channels/categories were left as configured; no launcher settings or owner data were changed. Projectivy accessibility was enabled before and after testing (`accessibility-before.txt`; final `device-metadata.txt` shows it enabled and bound).

Captured PreviewProbe device output contains the expected 440 Hz A tone followed by the 880 Hz B tone with no mixed-tone overlap. The old and new synthetic captures measured:

- Before: 1.28 s silent A→B boundary.
- After: 1.06 s silent A→B boundary.
- Difference between these two samples: 0.22 s (17.2%).

This is **not evidence that the code change caused the difference**. PreviewProbe is a separate publisher/package. AuraMusic's focus service reads only AuraMusic-owned TV provider channels, so focusing PreviewProbe does not call the changed AuraMusic prefetch path. The current Projectivy view shows the PreviewProbe row, with no AuraMusic song row visible; see [`current-home-previewprobe-view.png`](chromecast-clean-transition-20261116T/current-home-previewprobe-view.png). I left the user's channel configuration untouched and did not open AuraMusic. Therefore the code change's real-song handoff impact remains **INCONCLUSIVE**. The numeric samples use the same 20 ms window, 10 ms hop, normalized RMS `<0.0001` silence threshold, and 8× tone-dominance rule, but they are separate runs and not a controlled attribution test.

A separate 5.888 s device-output capture after focus left the test row was digital silence (RMS and peak both zero). The analyzer's detailed assertion is [`corrected-audio-assertions.json`](chromecast-clean-transition-20261116T/corrected-audio-assertions.json); the fine-grained comparison is [`gap-comparison.json`](chromecast-clean-transition-20261116T/gap-comparison.json). Raw captures, device metadata, action timestamps, and Projectivy/provider logs are in [`chromecast-clean-transition-20261116T/`](chromecast-clean-transition-20261116T/).

The change removes up to 120 ms of app-added debounce when an AuraMusic card is identified; Projectivy still controls its own playback startup. No claim of reduced real-song latency or gapless playback is made. B→A, repeated transitions, and real AuraMusic song playback were not measured in this follow-up. The still screenshots do not establish whether card video identity/animation remains correct throughout navigation; this PR changes audio prefetch timing only. Cropped screenshots exclude unrelated personal home-screen rows.

Cleanup removed only the stale test instrumentation package `com.auramusic.app.debug.test`. AuraMusic, Projectivy, and PreviewProbe remain installed.

See [`result.json`](result.json) for machine-readable outcomes and the APK hash.
