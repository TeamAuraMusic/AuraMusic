# Android TV preview compatibility

Checked 2026-11-04 on a Chromecast (`sabrina`, Android 14) with Projectivy 4.73, the latest stable release listed in the [Projectivy release history](https://github.com/spocky/miproja1/releases/tag/4.73).

AuraMusic's channel is visible on the Projectivy home screen and contains Quick Picks. The service logged 10 published programs, updated in place on refresh. Focusing a card enlarges its artwork, but the image stays static. AuraMusic currently publishes poster and thumbnail artwork only; it does not set either preview URI. This is expected with the current program metadata, not evidence of a broken Projectivy channel.

![AuraMusic Quick Picks row with a focused card in Projectivy](../pr-evidence/projectivy-home-aura-focused-20261104.png)

Projectivy 4.51 added hover video previews for supported cards and a setting for when to show them ([release notes](https://github.com/spocky/miproja1/releases/tag/4.51)). The 4.73 notes do not mention a change to card previews. Android TV's [preview video API](https://developer.android.com/training/tv/discovery/preview-videos) accepts a publicly accessible HTTPS URI and allows audio or video media. Projectivy's public repository contains configuration and theme tooling, not its launcher implementation, and its release notes do not establish support for `previewAudioUri` specifically. Audio preview compatibility remains unverified.

AuraMusic should not publish its current resolved stream URLs as previews yet. [`AuraVideo.getVideoStreamUrl`](../auravideo/src/main/kotlin/com/auramusic/auravideo/AuraVideo.kt) returns a URL and MIME type without an expiry value, and its fallback can select a video-only stream. A provider duration describes the program; it does not enforce a 15–30 second preview cutoff. Such URLs could expire or play without audio before Projectivy requests them.

The focused-card check used existing provider content while AuraMusic playback was active. No preview URI was present, so it verifies the static focus effect only. No temporary preview URL or playback change was introduced. A separate URI compatibility test is still needed before AuraMusic can safely publish audio or video previews.
