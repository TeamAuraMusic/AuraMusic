# AuraMusic Projectivy channel previews

## Current status (2026-11-08)

Latest local candidate was installed on API 36 Android TV and tested with Projectivy 4.73. Standalone/FOSS each pass 67 JVM tests; both AndroidTest suites pass 5 tests. No-click focus on two real tracks produced audible Projectivy output and the animated waveform over original artwork; focus loss stopped output; return restarted it; a full 186.12-second track played to its natural end; Select opened AuraMusic with the selected song. Ten 5-second A/B transitions passed. Cold focus-to-audio was 6.99 seconds; warm median 1.59 seconds. Evidence: [API 36 real-song run](../evidence/projectivy_real_song_api36_20261108/result.json). Overall mission remains incomplete: Android 14 Chromecast parity is now verified; isolated channel mutations, failure/recovery, longitudinal performance and trusted HTTPS remain open. API 34 emulator attempt failed at infrastructure: [failure evidence](../evidence/tv_instrumentation_20261108T122954Z/result.json).

### Projectivy 4.73 card-rendering boundary

- `previewVideoUri`: verified to render H.264 video inside a focused program card. PreviewProbe A/B displayed their red/blue video and moving marker on the launcher home screen without Select.
- Audio-only content in `previewVideoUri`: Projectivy plays it on focus, but its card video surface has no frames and appears blank. `previewAudioUri` alone is not consumed by the tested focused-card path.
- AuraMusic production path: publish the real full-length AAC track through the seekable local content provider; draw original artwork plus animated bars in an AuraMusic `TYPE_ACCESSIBILITY_OVERLAY` bounded to the focused Projectivy card. Tested on two real AuraMusic tracks. Overlay requires the user-granted AccessibilityService.
- `TvInputService`: Android registered PreviewProbe's service and a separate Android `TvView` explicitly created a session, set a surface, tuned, rendered timestamped frames and emitted 440 Hz. When only Projectivy was foreground/focusing program cards, `dumpsys tv_input` showed no session and the service had no Projectivy callbacks. **Projectivy 4.73 did not request this service/surface.** Stop pursuing it as a card-rendering path unless a later Projectivy version changes behavior. Evidence: [`Projectivy focus TvInput state`](../evidence/tvinput-experiment/projectivy_focus_tv_input.txt), [`explicit TvView callback trace`](../evidence/tvinput-experiment/tvview_live_callbacks.txt), [`rendered TvView capture`](../evidence/tvinput-experiment/tvview_live.png).
- The wallpaper-provider API changes the launcher background and does not draw inside program cards; it is not used for this feature.

### Chromecast card-layout setting (2026-11-10)

On Projectivy 4.73, keep **Appearance → Cards → Focused card → Focused card size** at `1` to prevent the AuraMusic artwork/waveform overlay from growing across neighboring program cards. The slider was at maximum even with `Zoom effect when focused` and AuraMusic's `Sliding row` disabled; Projectivy exposed a 556×268 selected node against 384×216 neighboring cards. At size `1`, ten no-click real-song transitions stayed at 384×216 and passed the card-motion, focus, foreground, and audio-track checks. This Projectivy preference is global to focused cards, so it changes other launcher rows too. Evidence: [card-motion diagnosis](../evidence/chromecast_motion_current_20261110T/README.md) and [ten-transition result](../evidence/chromecast_focused_size1_real_song_transitions_20261110T/transition-result.json).

The earlier 2026-11-05 report below is a historical baseline; later device runs supersede its “visual acceptance is not met” conclusion.

- On Chromecast 4K (`sabrina`, Android 14/API 34) with Projectivy 4.73, AuraMusic real-song cards have been verified to start audio and show an animated pink equalizer over the original artwork on no-click focus. The indicator is an AuraMusic AccessibilityService overlay positioned within the Projectivy card; Projectivy's native preview surface was not the source of that animation. The service must stay enabled.
- Current installed build at the last authorized device check: APK SHA-256 `1a517f04ed5707692eadbdbc62bb376fba209ac8b00f3fb07db3aa589ee2e779`. It passed the recorded 20-transition A/B focus run and focus-loss/return run. A same-song device audio capture correlated with its source AAC at 0.998729. Evidence paths and limits are recorded in the execution plan below and its linked evidence directories.
- **Latest uninstalled candidate (2026-11-08):** Publisher now explicitly selects the audio-only provider for supported audio MIME types and generated video only for unsupported sources. Standalone/FOSS builds pass 134 tests across 30 flavor suites (67/15 per flavor). APK SHA-256: `668fa52e1f22178938eeccb5d2c2dc553270a7546e6bd3d73485c81fe533c605` / `3aec83e285ca3e472da530e350a372a62df40beba384eeed04a042ed360d5c00`. One-shot source refresh on HTTP 401/403/404/410 is covered over sequential and parallel local HTTP transfers. AndroidTest APKs include waveform rendering, AAC conversion, click-intent contract, and provider MIME tests; hashes `a931d3aa303529e0ac2a7ccdb0ce9107e17979c1b847b80ab9af6cbec5c1249d` / `f5d0abcd7fba339ece1d0293d59ef66200b6f328d5eb8399c8dd70ce10413348`. Package inspection confirms all four classes and the runner. APKs are not installed and instrumentation is unexecuted; Projectivy behavior on this candidate remains unverified. [App build/test evidence](../evidence/publisher_uri_mode_20261108T084813Z/result.json); [instrumentation APK evidence](../evidence/provider_mime_instrumentation_20261108T121251Z/result.json).
- Rebuilt the same candidate and confirmed all four APK files exist with identical hashes; both flavor suites pass again (65 tests each). [Artifact rebuild evidence](../evidence/offline_candidate_rebuild_20261108T081146Z/result.json).
- Added cancellation coverage for a Projectivy preview read closing mid-download; both flavor gate suites pass 6/6. [Test evidence](../evidence/preview_source_refresh_cancellation_test_20261108T082944Z/result.json). Test-only change; APK hashes unchanged.
- Local HTTPS server behavior passed 17 loopback checks. Projectivy trust, LAN access, and Chromecast use of this server remain unverified; public DNS/certificate provisioning is unresolved.
- Mission remains in progress. The exact candidate is verified on Chromecast Android 14/API 34. Reboot and Projectivy restart preserve the channel and restore focused preview. Remaining: isolated channel update/reorder/removal convergence, end-to-end failure recovery, longitudinal resource profiling, and publicly trusted HTTPS.
- Scenario-by-scenario evidence and latest-candidate status: [acceptance matrix](implementation/PROJECTIVY_PREVIEW_ACCEPTANCE_MATRIX.md).
- Latest local candidate also scopes the accessibility waveform to the focused card's actual AuraMusic channel row. Projectivy hierarchy evidence shows `AuraMusic` as an ancestor of the focused program card; the overlay now requires that row match as well as a published title. Standalone/FOSS builds and suites pass 50 tests each (14 suites). Current APK hashes: `8a4a15b392e48c95aab2c72efdb7584de256b16ee36eb3da9ed6b859017804bc` / `03f234ab28c1c44beab28e337083588f6c4ee8c0f572789c95c6bc53d409a0e9`. Evidence: [`result.json`](../evidence/focus_row_scope_20261108T043431Z/result.json). Not installed or run on device; runtime hierarchy matching remains unverified on this candidate.
- The existing motion verifier now asserts the same row ancestry and passes against the archived Chromecast capture (14/14 visible animation samples). This confirms the verifier against prior evidence only; latest APK still needs device validation.
- Duplicate titles are now handled without guessing artwork: if all matching cards share the same artwork URI, it is reused; if artwork differs or any entry lacks it, the overlay leaves Projectivy's original art untouched. The latest Standalone/FOSS builds pass 53 tests each (14 suites). APK hashes: `837b4df355066842856a1f9fcdbd5a237a7196256fcaf08437e4e5716ab6c1e2` / `d53953ccaaa6b2ee976967e1d0bc482c2c2cd0baf25406005b8495e61d17df7f`. Evidence: [`result.json`](../evidence/focus_identity_20261108T044247Z/result.json). Not installed or device-tested.
- Latest local-only fix routes source resolution in both preview-generation paths through the two-job gate, so expiring URLs are obtained only after a permit is available. Added a regression that holds one permit and proves a second resolver cannot run early. Standalone and FOSS TV builds pass 47 tests (13 suites each). APK SHA-256: Standalone `6cf1be7231230860118cbeb63fb268aba4d3001b8174b2ba5a413b14a102f03c`; FOSS `1fe7f8963fec2f6af639c53918a330d207f084972528d0fd72415493e97669f6`. Evidence: [`result.json`](../evidence/source_refresh_gate_20261108T042247Z/result.json) and [`updated-build-test.log`](../evidence/source_refresh_gate_20261108T042247Z/updated-build-test.log). Neither APK was installed or device-tested; Chromecast and emulator remained untouched.
- Latest local build handles an authoritative empty queue plus Quick Picks snapshot by removing stale channel programs in one TvProvider batch; unreadable snapshots preserve existing cards. Standalone/FOSS builds and tests pass 58 tests across 15 suites each. APK hashes: `01520ff6d1c8cb27c8d4ce7c167154a7495a98259a4083b73c27d36b320d6ca1` / `f468cfbe687fe4ae8a2b8c0be3de57745a55d36b8b0bb1578b1b01aef31013b2`. Evidence: [`result.json`](../evidence/empty_refresh_reconciliation_20261108T044941Z/result.json). Not device-tested.
- The live A/B verifier now uses the same Windows ADB executable and port as `tvctl`, defaults to an emulator-only serial allowlist, and requires `--allow-physical` before it can target a Chromecast. Six harness tests pass; this does not replace the exact-candidate device run. Evidence: [`result.json`](../evidence/live_transition_verifier_20261108T045829Z/result.json).
- PreviewProbe now supports deterministic add/update/reorder/remove/clear publications through launch extras. Its argument parser passes six unit tests (default, clear, ordering/case, duplicate, unknown, empty entry); latest APK hash `d68bf235d11d7af99ad4f15df8aca7c399231271ef2f6fbe46359887ec1b06ac`. Evidence: [`PreviewProbe mutation unit/build result`](../evidence/previewprobe_mutation_unit_20261108T052020Z/result.json). AuraMusic Standalone and FOSS TV suites pass 58/58 each; hashes `01520ff6d1c8cb27c8d4ce7c167154a7495a98259a4083b73c27d36b320d6ca1` / `f468cfbe687fe4ae8a2b8c0be3de57745a55d36b8b0bb1578b1b01aef31013b2`, with [evidence](../evidence/current_tv_build_tests_20261108T051631Z/result.json). Projectivy provider mutation and visible reorder have not been run on a device.
- Local HTTPS host loopback harness passes 17/17 checks with TLS verification enabled, including range handling and request diagnostics. This proves host server behavior only. Evidence: [`result.json`](../evidence/preview_https_loopback_20261108T052000Z/result.json).
- Latest local-only build now normalizes non-`audio/mp4` real sources to audio-only AAC/MP4 using Media3 Transformer; output policy requires one AAC track/no video before atomic caching. Standalone/FOSS each pass 59 tests across 15 suites. APK hashes `5dbe7df38ba22df2ecf2e28725ca4b9f76be284b6d0b53fbe56f568c17d9387f` / `4836ca47271a717dbc78772f0cf457fdfe6bd8888a998a03af11bcc396749e76`; [build/test evidence](../evidence/audio_only_normalization_policy_20261108T054138Z/result.json). Not device-tested: runtime conversion, Projectivy playback, and startup latency remain unverified. The prior 88.445-second cold full-video path and failed fragmented-pipe experiment remain historical evidence in the [source-format audit](implementation/PROJECTIVY_PREVIEW_EXECUTION_PLAN.md).
- Python test discovery now passes 38 tests, including fake-ADB end-to-end checks for `tvctl test`, emulator serial routing, physical-device rejection, instrumentation result classification, and evidence generation. Current evidence: [`tvctl_instrumentation_runner_verified_20261108T075924Z`](../evidence/tvctl_instrumentation_runner_verified_20261108T075924Z/). All device verifiers use the configured Windows ADB binary, server port, and explicit serial; physical serials require `--allow-physical`. The PreviewProbe audio analyzer can check a continuous A→B WAV for ordered 440/880 Hz audio and mixed-tone overlap, and reports missing or malformed WAVs as `FAIL_TEST_INFRA`. Existing transition WAVs are inconclusive; they do not establish a clean handoff. Latest evidence and status are in the [execution plan](implementation/PROJECTIVY_PREVIEW_EXECUTION_PLAN.md).

### Capture an A→B PreviewProbe audio transition

Use the emulator, with PreviewProbe's A/B channel enabled and A focused on Projectivy's home screen. Keep A and B adjacent in the row. This records one continuous output-audio capture, waits three seconds on A, then moves right to B without Select. `tvctl` enforces its emulator-only serial allowlist.

```sh
evidence_dir="evidence/previewprobe_transition_$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$evidence_dir"
TVCTL_SERIAL=emulator-5554 scripts/tvctl capture-audio 8 "$evidence_dir/a-to-b.wav" &
capture_pid=$!
sleep 3
TVCTL_SERIAL=emulator-5554 scripts/tvctl key RIGHT
wait "$capture_pid"
python3 scripts/verify-preview-audio.py \
  --transition "$evidence_dir/a-to-b.wav" \
  --output "$evidence_dir/audio-assertions.json"
```

The analyzer uses non-overlapping 500 ms windows. It requires at least three stable windows of each tone. A clean ordered handoff with no mixed-tone window passes; a single ambiguous boundary window is `INCONCLUSIVE`; sustained overlap or A audio returning after B is `FAIL_PRODUCT`. Capture with too little audio to show both tones is `INCONCLUSIVE`. A separate focus-loss capture can still be checked with `--a`, `--b`, and `--focus-loss`.

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

### Run instrumented Android TV tests with tvctl

Build the matching app and AndroidTest APKs, then run on an already-booted allowlisted emulator:

```sh
./gradlew :app:assembleStandaloneTvUniversalDebug :app:assembleStandaloneTvUniversalDebugAndroidTest --console=plain
TVCTL_SERIAL=emulator-5554 scripts/tvctl test standalone
./gradlew :app:assembleFossTvUniversalDebug :app:assembleFossTvUniversalDebugAndroidTest --console=plain
TVCTL_SERIAL=emulator-5554 scripts/tvctl test foss
```

`tvctl test` does not boot a device. By default it rejects physical serials and accepts only the emulator allowlist. For an explicitly authorized physical-device test, set the exact serial and add `--allow-physical`, for example `TVCTL_SERIAL=192.168.3.2:5555 scripts/tvctl test foss FocusedCardWaveformViewInstrumentedTest --allow-physical`. It checks Android TV/Leanback and Projectivy, installs the selected flavor pair, runs AndroidJUnitRunner, and saves machine-readable results plus instrumentation output and device logs under `evidence/tv_instrumentation_*`. Use the optional class name to run only `FocusedCardWaveformViewInstrumentedTest` or `TvPreviewAudioTranscoderInstrumentedTest` or `TvRecommendationClickIntentInstrumentedTest`. Fake-ADB tests validate command routing and result capture.

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


## Current verified physical Chromecast candidate (2026-11-08)

The AuraMusic 3.2.0/code 29 Standalone candidate was installed in place on Chromecast `sabrina` (Android 14/API 34) and verified by pulling its installed APK: SHA-256 `11e4939da4ad29152dcbe1c4007a564fb2f06d1484abd8cd1c1aa9833122c57e`. Projectivy 4.73 displayed focused real AuraMusic cards with original art and the pink moving waveform. DPAD-only focus started actual songs; two tracks have synchronized H.264/AAC output captures. Twenty A/B transitions, 0.119-second focus-loss stop, same-card return, 65-second continuous hold, and the Select deep link passed. See [`machine result`](../evidence/chromecast_api34_latest_candidate_20261108/result.json).

Overall status remains **INCOMPLETE**. The latest optimized APK (`c2beed295b6a43506cea5cc2ee470808bc730af655818e4d8fdbf17f981afc8e`) passes 67 JVM tests and one focused Chromecast view test. A 73.4-second baseline before the drawing change found Projectivy CPU median 42.3% and AuraMusic 13.3%, with stable cache. The connected AndroidTest run removed AuraMusic; Android had no backup set. The candidate was reinstalled and the accessibility grant re-enabled, but the prior local Quick Picks history/channel is gone, so real-song Projectivy verification of this optimization is inconclusive. See [optimization result](../evidence/waveform_render_optimization_20261108/result.json). Remaining: recover/rebuild Quick Picks, compare post-change CPU, complete failure/recovery and long-run cache tests, and verify AuraMusic-specific channel mutation.
