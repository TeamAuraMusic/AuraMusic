# AuraMusic Projectivy channel previews

## Current status (2026-11-08)

The earlier 2026-11-05 report below is a historical baseline; later device runs supersede its “visual acceptance is not met” conclusion.

- On Chromecast 4K (`sabrina`, Android 14/API 34) with Projectivy 4.73, AuraMusic real-song cards have been verified to start audio and show an animated pink equalizer over the original artwork on no-click focus. The indicator is an AuraMusic AccessibilityService overlay positioned within the Projectivy card; Projectivy's native preview surface was not the source of that animation. The service must stay enabled.
- Current installed build at the last authorized device check: APK SHA-256 `1a517f04ed5707692eadbdbc62bb376fba209ac8b00f3fb07db3aa589ee2e779`. It passed the recorded 20-transition A/B focus run and focus-loss/return run. A same-song device audio capture correlated with its source AAC at 0.998729. Evidence paths and limits are recorded in the execution plan below and its linked evidence directories.
- Latest local-only build adds a focused 500 ms playback-state refresh for the reproducible 1/20 stale-indicator race, preserves the previous complete audio cache during replacement/failure, bounds distinct full-song downloads to two at once, covers LRU eviction, pins cache files while provider opens them, reruns eviction after pins release, sends identity encoding on sequential/range resume requests, cancels shared preparation when its last provider-open waiter leaves, and recovers safely across concurrent acquisition/cancellation. Standalone and FOSS TV APKs were assembled; full test suites pass 46 tests each. Neither APK has been installed or run on a device. Standalone APK SHA-256 `af8e6243421f4f95d7f0680f900433fc3b9a288706bd6f72405631249f450ec1`; FOSS APK SHA-256 `f50ec06e79114d23e560bc4b33b6f995c79b437a652548fda6f0475e87a2e7fa`. Do not treat either as the installed or device-verified candidate.
- Local HTTPS server behavior passed 17 loopback checks. Projectivy trust, LAN access, and Chromecast use of this server remain unverified; public DNS/certificate provisioning is unresolved.
- Mission is still in progress. Lifecycle/update/failure coverage, exact-latest-build device retest, emulator compatibility, and broad performance remain open. Chromecast use is paused per the user's request; do not touch it without renewed direction.
- Latest local candidate also scopes the accessibility waveform to the focused card's actual AuraMusic channel row. Projectivy hierarchy evidence shows `AuraMusic` as an ancestor of the focused program card; the overlay now requires that row match as well as a published title. Standalone/FOSS builds and suites pass 50 tests each (14 suites). Current APK hashes: `8a4a15b392e48c95aab2c72efdb7584de256b16ee36eb3da9ed6b859017804bc` / `03f234ab28c1c44beab28e337083588f6c4ee8c0f572789c95c6bc53d409a0e9`. Evidence: [`result.json`](../evidence/focus_row_scope_20261108T043431Z/result.json). Not installed or run on device; runtime hierarchy matching remains unverified on this candidate.
- The existing motion verifier now asserts the same row ancestry and passes against the archived Chromecast capture (14/14 visible animation samples). This confirms the verifier against prior evidence only; latest APK still needs device validation.
- Duplicate titles are now handled without guessing artwork: if all matching cards share the same artwork URI, it is reused; if artwork differs or any entry lacks it, the overlay leaves Projectivy's original art untouched. The latest Standalone/FOSS builds pass 53 tests each (14 suites). APK hashes: `837b4df355066842856a1f9fcdbd5a237a7196256fcaf08437e4e5716ab6c1e2` / `d53953ccaaa6b2ee976967e1d0bc482c2c2cd0baf25406005b8495e61d17df7f`. Evidence: [`result.json`](../evidence/focus_identity_20261108T044247Z/result.json). Not installed or device-tested.
- Latest local-only fix routes source resolution in both preview-generation paths through the two-job gate, so expiring URLs are obtained only after a permit is available. Added a regression that holds one permit and proves a second resolver cannot run early. Standalone and FOSS TV builds pass 47 tests (13 suites each). APK SHA-256: Standalone `6cf1be7231230860118cbeb63fb268aba4d3001b8174b2ba5a413b14a102f03c`; FOSS `1fe7f8963fec2f6af639c53918a330d207f084972528d0fd72415493e97669f6`. Evidence: [`result.json`](../evidence/source_refresh_gate_20261108T042247Z/result.json) and [`updated-build-test.log`](../evidence/source_refresh_gate_20261108T042247Z/updated-build-test.log). Neither APK was installed or device-tested; Chromecast and emulator remained untouched.
- Latest local build handles an authoritative empty queue plus Quick Picks snapshot by removing stale channel programs in one TvProvider batch; unreadable snapshots preserve existing cards. Standalone/FOSS builds and tests pass 58 tests across 15 suites each. APK hashes: `01520ff6d1c8cb27c8d4ce7c167154a7495a98259a4083b73c27d36b320d6ca1` / `f468cfbe687fe4ae8a2b8c0be3de57745a55d36b8b0bb1578b1b01aef31013b2`. Evidence: [`result.json`](../evidence/empty_refresh_reconciliation_20261108T044941Z/result.json). Not device-tested.
- The live A/B verifier now uses the same Windows ADB executable and port as `tvctl`, defaults to an emulator-only serial allowlist, and requires `--allow-physical` before it can target a Chromecast. Six harness tests pass; this does not replace the exact-candidate device run. Evidence: [`result.json`](../evidence/live_transition_verifier_20261108T045829Z/result.json).
- PreviewProbe now supports deterministic add/update/reorder/remove/clear publications through launch extras. Its argument parser passes six unit tests (default, clear, ordering/case, duplicate, unknown, empty entry); latest APK hash `d68bf235d11d7af99ad4f15df8aca7c399231271ef2f6fbe46359887ec1b06ac`. Evidence: [`PreviewProbe mutation unit/build result`](../evidence/previewprobe_mutation_unit_20261108T052020Z/result.json). AuraMusic Standalone and FOSS TV suites pass 58/58 each; hashes `01520ff6d1c8cb27c8d4ce7c167154a7495a98259a4083b73c27d36b320d6ca1` / `f468cfbe687fe4ae8a2b8c0be3de57745a55d36b8b0bb1578b1b01aef31013b2`, with [evidence](../evidence/current_tv_build_tests_20261108T051631Z/result.json). Projectivy provider mutation and visible reorder have not been run on a device.
- Local HTTPS host loopback harness passes 17/17 checks with TLS verification enabled, including range handling and request diagnostics. This proves host server behavior only. Evidence: [`result.json`](../evidence/preview_https_loopback_20261108T052000Z/result.json).
- Latest local-only build now normalizes non-`audio/mp4` real sources to audio-only AAC/MP4 using Media3 Transformer; output policy requires one AAC track/no video before atomic caching. Standalone/FOSS each pass 59 tests across 15 suites. APK hashes `5dbe7df38ba22df2ecf2e28725ca4b9f76be284b6d0b53fbe56f568c17d9387f` / `4836ca47271a717dbc78772f0cf457fdfe6bd8888a998a03af11bcc396749e76`; [build/test evidence](../evidence/audio_only_normalization_policy_20261108T054138Z/result.json). Not device-tested: runtime conversion, Projectivy playback, and startup latency remain unverified. The prior 88.445-second cold full-video path and failed fragmented-pipe experiment remain historical evidence in the [source-format audit](implementation/PROJECTIVY_PREVIEW_EXECUTION_PLAN.md).

## Historical device baseline (2026-11-05)

The following sections record the 2026-11-05 Chromecast baseline only. Later implementation and device results are in [the execution plan](implementation/PROJECTIVY_PREVIEW_EXECUTION_PLAN.md); do not read this snapshot's conclusions as current status.

Checked on a Chromecast with Google TV 4K (`sabrina`, Android 14 / API 34), Projectivy Launcher 4.73, and AuraMusic FOSS TV Universal Debug 3.2.0 (`versionCode=29`). The AuraMusic row is at the bottom of this Projectivy home layout. Tests used D-pad focus only; no card was clicked.

### Publication at the historical baseline

AuraMusic publishes up to 10 Quick Picks / queue tracks to its existing `Continue Listening` PreviewChannel. Each program keeps its stable track ID and click intent. It resolves audio-only streams through the existing AuraVideo/NewPipe extractor and reuses a still-valid audio URL while its `expire` query value is more than two minutes away.

The current candidate writes the same resolved audio URI to both `previewVideoUri` and `previewAudioUri`. Projectivy 4.73 did not play when only `previewAudioUri` was set; Android's preview guide documents audio or video through `setPreviewVideoUri()`. Projectivy 4.51 release notes describe hover video previews "where supported" and the `never` / `always` / `only when no media is currently playing` policy ([release notes](https://github.com/spocky/miproja1/releases/tag/4.51), [Android TV preview guide](https://developer.android.com/training/tv/discovery/preview-videos)). The policy was set to `Always` on this device.

The official [Projectivy repository README](https://github.com/spocky/miproja1#about-this-repo--projectivy-launcher-special-device-configuration) describes device configuration files and issue tracking; it does not publish the launcher implementation. AuraMusic therefore cannot patch Projectivy's focused-card renderer in this repository.

For this device check, the installed Projectivy 4.73 APK (`versionCode=98`) was pulled read-only and inspected with JADX 1.5.6. Its focused channel-card handler reads `preview_video_uri`, starts Media3 playback into a card `SurfaceView` after a one-second focus delay, and tears it down when focus leaves. The card's separate focus effect scales the poster to 1.05 over three seconds. Although the provider projection includes `preview_audio_uri`, the focused-card path does not read it and contains no audio visualizer. This matches the device result: an audio-only URI plays sound but gives the video surface no frames, so it appears black; the remaining visible change is the scale-up. AuraMusic can publish a real video URI, but it cannot make Projectivy draw sound bars from audio metadata.

Projectivy's [official wallpaper-provider plugin](https://github.com/spocky/projectivy-plugin-wallpaper-provider) can react to `ProgramCardFocused` and return an animated drawable, Lottie, or video as a launcher wallpaper ([event model](https://github.com/spocky/projectivy-plugin-wallpaper-provider/blob/main/api/src/main/java/tv/projectivy/plugin/wallpaperprovider/api/Event.kt), [wallpaper types](https://github.com/spocky/projectivy-plugin-wallpaper-provider/blob/main/api/src/main/java/tv/projectivy/plugin/wallpaperprovider/api/Wallpaper.kt)). This changes the launcher background, not the focused card. The event includes title, program type, artwork URI, and aspect ratio, but no channel or package identity, so a wallpaper plugin cannot reliably animate only AuraMusic's row. It is a separate background-level experiment, not a card-rendering workaround.

### Focus-only device results at the historical baseline

All tests below used only D-pad focus and waits. AuraMusic playback was paused or force-stopped; no `PreviewProgram` was clicked. The provider refreshed the existing 10 records (`0 added, 10 updated, 0 removed`). `adb install -r` preserved app data: `firstInstallTime` stayed `2026-10-20 09:57:57`.

| Published preview metadata | Projectivy result |
| --- | --- |
| Audio URL in `previewAudioUri` only | No Projectivy audio track started. |
| Audio URL in `previewVideoUri` only | Projectivy started a 44.1 kHz `AudioTrack` on focus. D-pad right started a different track; moving up stopped it. The focused card showed a black preview surface. |
| Same audio URL in both fields | After a fresh Projectivy launch, “Knee Socks” started audio; its per-track GIF changed in 17,978 pixels in a 485×310 crop over two seconds. D-pad right to “Labyrinthine” started a new Projectivy track, but that focused card stayed static (zero changed pixels over two seconds). Moving focus up stopped playback (zero active AudioFlinger tracks). |
| Local silent H.264 `previewVideoUri` plus song URL in `previewAudioUri` | No song audio track started. The GIF could animate, but this did not provide combined song audio and visualization. |
| Muxed 360p H.264 MP4 in both preview fields | Resolver returned a single `video/mp4` stream. Projectivy started a 44.1 kHz audio track owned by Projectivy, but the focused card remained black; no usable video appeared. Tested on “Labyrinthine” with focus only. |
| Local 10-second H.264/AAC `android.resource` URI in both fields | A controlled probe targeted “Messy” (`Y03n19YRXD4`). Projectivy 4.73 initialized 320×180 H.264 and 44.1 kHz AAC decoders, then released the player at the 10-second end. The focused card stayed on static poster art (zero changed pixels in a 500×370 crop over two seconds), and AudioFlinger showed zero active tracks during capture. This verifies URI parsing and bounded clip completion, but not audible output or visible video rendering. |
| Animated GIF through AuraMusic `FileProvider` as poster and thumbnail | A fresh provider ID forced Projectivy to fetch a 240×135, six-frame GIF. AuraMusic granted read access to HOME handlers; ActivityManager showed a grant for Projectivy UID 10150. Projectivy still logged a `SecurityException` opening the non-exported AndroidX `FileProvider`, and the card showed image-load error placeholders. The test was reverted; do not publish app-private `FileProvider` URIs as launcher artwork. [Device capture](../pr-evidence/projectivy-fileprovider-gif-read-denied-20261105.png). |
| Separate real video URI plus separate audio URI | A temporary first-card probe using AuraVideo's existing video resolver did not finish publishing within 15 seconds; the app process sampled at about 248% CPU. It was stopped and reverted before any Projectivy preview result. This is inconclusive about launcher video rendering, but the eager per-card video-resolution approach is too costly for the publish path. |

The verified result is focus audio that changes with the card and stops on focus loss for direct audio URLs. A 43-second focus-only check continued playing; `durationMillis` does not bound Projectivy playback. The URI is the full song, so this is not yet a short preview. The focused preview surface stays static for audio and the tested muxed H.264 stream; a local muxed resource decoded but did not replace the static poster. Animated poster experiments did not render consistently across cards and were removed from the candidate patch. [Focused bottom-row card screenshot](../pr-evidence/projectivy-aura-h264-hover-no-fetch-20261104.png).

The clean final build was installed with `adb install -r`; `firstInstallTime` stayed `2026-10-20 09:57:57` and the saved AuraMusic MediaSession remained paused. With AuraMusic force-stopped, D-pad down focused “Paint Me Silver” and Projectivy UID 10150 started a 44.1 kHz AudioTrack. D-pad right changed focus to “Everlong (Acoustic Version)” and started a different AudioTrack. D-pad up to “Button Mapper” left no started Projectivy AudioTrack. No card was clicked. Two focused-card captures differed by zero pixels in the card crop over two seconds: audio works, visual animation does not. [Clean-build focus capture](../pr-evidence/projectivy-clean-final-audio-focus-20261105.png).

The final publisher log updated the existing 10 programs without accumulating rows (`0 added, 10 updated, 0 removed` on the steady refresh). Preview URLs remain complete songs; a 43-second focus-only check showed `durationMillis` does not bound Projectivy playback. The app has no verified 15–30 second limit here, so these are focus-playable songs, not short snippets. A clean source build cannot make the cards animate while Projectivy renders the audio preview surface black/static.

### Interaction with AuraMusic playback at the historical baseline

With Projectivy's preview policy set to `Always`, a separate controlled check started AuraMusic playback, then focused a channel card without clicking it. Projectivy and AuraMusic both had active AudioTracks at the same time (UIDs 10150 and 10014). Projectivy does not suppress its preview when AuraMusic is already playing under this policy. Sending an explicit pause to AuraMusic while the card was focused, then moving focus away, left AuraMusic paused and stopped Projectivy's preview; AudioFlinger reported zero active tracks. The candidate clears AuraMusic's saved audio-focus-resume flag on an explicit user pause.

Projectivy also offers an “only when no media is currently playing” hover-preview policy. It is the appropriate setting to avoid overlapping AuraMusic playback. ADB could not read or change Projectivy's private setting (`run-as` reports the package is not debuggable; no matching Android system setting exists). No launcher settings were changed during this work.

Projectivy owns the preview player and correctly stops it after focus loss in these checks. AuraMusic's MediaSession stayed paused; no click-launch behavior was included. A standard `TvInputService` can render on an Android TV surface, but Projectivy's support for app-owned `input` preview URIs is unverified. It remains a candidate for a separate compatibility probe, not a demonstrated workaround.

### Refresh and cost at the historical baseline

The `feat/tv-periodic-refresh` follow-up schedules a unique four-hour WorkManager refresh from TV activity startup and device boot. It uses the same publisher as playback events, updates stable program IDs, and does not start AuraMusic's media-playback foreground service. WorkManager timing is inexact; a force-stopped app's job may wait until the app is opened.

On-device publish logs reported `0 added, 10 updated, 0 removed`, confirming repeated refreshes do not accumulate duplicate rows. The final app install kept `firstInstallTime=2026-10-20 09:57:57`; login and app data were preserved. After a cold Projectivy restart, focusing “Labyrinthine” started a 44.1 kHz AudioTrack owned by Projectivy (UID 10150), not AuraMusic. Moving focus up paused/stopped it; AudioFlinger then reported zero active tracks. Preview URLs without a recognized expiry are re-resolved instead of reused indefinitely. The final candidate also re-resolves cached URLs unless their query MIME identifies audio, preventing reuse of the H.264 video experiment as an audio preview.

The FOSS TV unit suite was forced to execute after adding URI-policy coverage. The FOSS TV and GMS TV Universal debug assemblies passed, as did the FOSS Mobile Universal debug assembly for shared playback code. The app was reinstalled with `adb install -r`; account data stayed intact. The temporary GIF and local MP4 probes were removed from source before the final TV build.

```sh
./gradlew :app:testFossTvUniversalDebugUnitTest --rerun-tasks --console=plain
./gradlew :app:assembleFossTvUniversalDebug --console=plain
./gradlew :app:assembleFossMobileUniversalDebug --console=plain
./gradlew :app:assembleGmsTvUniversalDebug --console=plain
```

### Remaining work recorded at the historical baseline

- Verify URI replacement directly from TvProvider on a debuggable build; shell queries are permission-restricted on Chromecast. The refreshed source validates audio MIME before publication, and Projectivy started the resulting focus stream.
- Projectivy provides no demonstrated 15–30 second playback bound for a direct stream URI. A short preview needs launcher playback limits or a bounded media source it supports.
- The device's current Projectivy `Always` policy allows overlapping AuraMusic playback and card previews; change it to “only when no media is currently playing” in Projectivy settings to avoid overlap.
- Reliable per-card soundbar/video preview needs Projectivy renderer support or a separately verified app-owned TV input path. Visual acceptance is not met.

### AAC-only source normalization test

`TvPreviewAudioTranscoderInstrumentedTest` exercises production Media3 conversion on Android: an eight-second deterministic 440 Hz MP3 fixture must become a seekable MP4 with one AAC audio track, samples through the full duration, and decoded PCM dominated by the original tone. Fixture lives in the `androidTestTv` source set. Package with:

```sh
./gradlew :app:assembleStandaloneTvUniversalDebugAndroidTest --console=plain
./gradlew :app:assembleFossTvUniversalDebugAndroidTest --console=plain
```

Run against the C: API 36 emulator through its Windows ADB server. The explicit serial prevents this test from landing on the connected Chromecast:

```sh
ADB_SERVER_SOCKET=tcp:127.0.0.1:5038 ./gradlew :app:connectedStandaloneTvUniversalDebugAndroidTest --serial emulator-5554 --console=plain
ADB_SERVER_SOCKET=tcp:127.0.0.1:5038 ./gradlew :app:connectedFossTvUniversalDebugAndroidTest --serial emulator-5554 --console=plain
```

The fixture and test class are present in both Standalone and FOSS instrumentation APKs; the source tone was independently measured at 439.981 Hz. The test has not been installed or run. This is build evidence only; Android codec behavior and real non-AAC conversion remain runtime verification gates. Evidence: `evidence/transcoder_test_both_flavors_20261108T061223Z/`.
