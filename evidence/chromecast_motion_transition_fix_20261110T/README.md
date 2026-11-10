# Chromecast Projectivy card motion investigation

## Current candidate

- Build: `:app:assembleFossTvArmeabiDebug`, successful.
- APK SHA-256: `13aa5b3a05f4513cfc8b1d14dce85e8968c488e38a9aedd0e6d2e23fa598992a`.
- Pulled installed APK hash matched the candidate hash (`installed-final.sha256`).
- Installed in place on Chromecast `sabrina` (Android 14/API 34), Projectivy 4.73/code 98. No app data cleared.
- AuraMusic provider exposed 10 programs. Projectivy stayed foreground. Ten DPAD-only Angie/Africa transitions generated 10 focus events; final hierarchy focused Angie. Service logged 9 new-window replacements and no errors.
- AuraMusic suppresses the old overlay immediately on a different Projectivy focus event, updates cached artwork, and creates a fresh overlay window for the new program.
- Isolation run temporarily disabled only AuraMusic's accessibility overlay; Projectivy's native horizontal row slide remained. The exact accessibility-service list was restored and verified after the recording.

## Result

Overall `INCONCLUSIVE` for the user's precise “old rendered image must not cross onto the next card” observation. The recordings show Projectivy's own horizontal row and preview transition, including its Surface presentation. They do not reliably separate the Projectivy Surface pixels from AuraMusic's accessibility overlay at card boundaries. AuraMusic now changes its own overlay immediately; the remaining native row animation also occurs with that overlay disabled.

The 950 ms hide-and-reveal experiment was rejected and removed from the current candidate; it created an unnecessary waveform gap. Its logs and captures remain local and are excluded from this focused evidence bundle.

## Evidence

- [`screenrecord-ten-transitions.mp4`](screenrecord-ten-transitions.mp4) and [`recreated-window-ten-logcat.txt`](recreated-window-ten-logcat.txt): ten no-click focus inputs, service events and final focus.
- [`screenrecord-recreated-window.mp4`](screenrecord-recreated-window.mp4): one Angie/Africa round trip after replacing the overlay window per program.
- [`screenrecord-no-overlay.mp4`](screenrecord-no-overlay.mp4): Projectivy behavior with only AuraMusic's overlay accessibility service temporarily disabled.
- [`accessibility-services-before.txt`](accessibility-services-before.txt) and [`accessibility-services-after.txt`](accessibility-services-after.txt): same service list before/after isolation.
- [`result.json`](result.json): machine-readable assertions and limits.

Commands: build with `./gradlew :app:assembleFossTvArmeabiDebug`; install with Windows ADB and explicit Chromecast serial; navigate to AuraMusic row; use DPAD only. Do not press Select.
