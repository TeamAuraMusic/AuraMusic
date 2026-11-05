# AuraMusic Projectivy channel previews

Checked 2026-11-05 on a Chromecast with Google TV 4K (`sabrina`, Android 14 / API 34), Projectivy Launcher 4.73, and AuraMusic FOSS TV Universal Debug 3.2.0 (`versionCode=29`). The AuraMusic row is at the bottom of this Projectivy home layout. Tests used D-pad focus only; no card was clicked.

## Current publication

AuraMusic publishes up to 10 Quick Picks / queue tracks to its existing `Continue Listening` PreviewChannel. Each program keeps its stable track ID and click intent. It resolves audio-only streams through the existing AuraVideo/NewPipe extractor and reuses a still-valid audio URL while its `expire` query value is more than two minutes away.

The current candidate writes the same resolved audio URI to both `previewVideoUri` and `previewAudioUri`. Projectivy 4.73 did not play when only `previewAudioUri` was set; Android's preview guide documents audio or video through `setPreviewVideoUri()`. Projectivy 4.51 release notes describe hover video previews "where supported" and the `never` / `always` / `only when no media is currently playing` policy ([release notes](https://github.com/spocky/miproja1/releases/tag/4.51), [Android TV preview guide](https://developer.android.com/training/tv/discovery/preview-videos)). The policy was set to `Always` on this device.

The official [Projectivy repository README](https://github.com/spocky/miproja1#about-this-repo--projectivy-launcher-special-device-configuration) describes device configuration files and issue tracking; it does not publish the launcher implementation. AuraMusic therefore cannot patch Projectivy's focused-card renderer in this repository.

For this device check, the installed Projectivy 4.73 APK (`versionCode=98`) was pulled read-only and inspected with JADX 1.5.6. Its focused channel-card handler reads `preview_video_uri`, starts Media3 playback into a card `SurfaceView` after a one-second focus delay, and tears it down when focus leaves. The card's separate focus effect scales the poster to 1.05 over three seconds. Although the provider projection includes `preview_audio_uri`, the focused-card path does not read it and contains no audio visualizer. This matches the device result: an audio-only URI plays sound but gives the video surface no frames, so it appears black; the remaining visible change is the scale-up. AuraMusic can publish a real video URI, but it cannot make Projectivy draw sound bars from audio metadata.

## Focus-only device results

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

## Interaction with AuraMusic playback

With Projectivy's preview policy set to `Always`, a separate controlled check started AuraMusic playback, then focused a channel card without clicking it. Projectivy and AuraMusic both had active AudioTracks at the same time (UIDs 10150 and 10014). Projectivy does not suppress its preview when AuraMusic is already playing under this policy. Sending an explicit pause to AuraMusic while the card was focused, then moving focus away, left AuraMusic paused and stopped Projectivy's preview; AudioFlinger reported zero active tracks. The candidate clears AuraMusic's saved audio-focus-resume flag on an explicit user pause.

Projectivy also offers an “only when no media is currently playing” hover-preview policy. It is the appropriate setting to avoid overlapping AuraMusic playback. ADB could not read or change Projectivy's private setting (`run-as` reports the package is not debuggable; no matching Android system setting exists). No launcher settings were changed during this work.

Projectivy owns the preview player and correctly stops it after focus loss in these checks. AuraMusic's MediaSession stayed paused; no click-launch behavior was included. A standard `TvInputService` can render on an Android TV surface, but Projectivy's support for app-owned `input` preview URIs is unverified. It remains a candidate for a separate compatibility probe, not a demonstrated workaround.

## Refresh and cost

The `feat/tv-periodic-refresh` follow-up schedules a unique four-hour WorkManager refresh from TV activity startup and device boot. It uses the same publisher as playback events, updates stable program IDs, and does not start AuraMusic's media-playback foreground service. WorkManager timing is inexact; a force-stopped app's job may wait until the app is opened.

On-device publish logs reported `0 added, 10 updated, 0 removed`, confirming repeated refreshes do not accumulate duplicate rows. The final app install kept `firstInstallTime=2026-10-20 09:57:57`; login and app data were preserved. After a cold Projectivy restart, focusing “Labyrinthine” started a 44.1 kHz AudioTrack owned by Projectivy (UID 10150), not AuraMusic. Moving focus up paused/stopped it; AudioFlinger then reported zero active tracks. Preview URLs without a recognized expiry are re-resolved instead of reused indefinitely. The final candidate also re-resolves cached URLs unless their query MIME identifies audio, preventing reuse of the H.264 video experiment as an audio preview.

The FOSS TV unit suite was forced to execute after adding URI-policy coverage. The FOSS TV and GMS TV Universal debug assemblies passed, as did the FOSS Mobile Universal debug assembly for shared playback code. The app was reinstalled with `adb install -r`; account data stayed intact. The temporary GIF and local MP4 probes were removed from source before the final TV build.

```sh
./gradlew :app:testFossTvUniversalDebugUnitTest --rerun-tasks --console=plain
./gradlew :app:assembleFossTvUniversalDebug --console=plain
./gradlew :app:assembleFossMobileUniversalDebug --console=plain
./gradlew :app:assembleGmsTvUniversalDebug --console=plain
```

## Remaining work

- Verify URI replacement directly from TvProvider on a debuggable build; shell queries are permission-restricted on Chromecast. The refreshed source validates audio MIME before publication, and Projectivy started the resulting focus stream.
- Projectivy provides no demonstrated 15–30 second playback bound for a direct stream URI. A short preview needs launcher playback limits or a bounded media source it supports.
- The device's current Projectivy `Always` policy allows overlapping AuraMusic playback and card previews; change it to “only when no media is currently playing” in Projectivy settings to avoid overlap.
- Reliable per-card soundbar/video preview needs Projectivy renderer support or a separately verified app-owned TV input path. Visual acceptance is not met.
