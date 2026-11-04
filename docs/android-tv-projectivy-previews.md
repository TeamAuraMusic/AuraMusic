# Android TV preview compatibility

Checked 2026-11-04 on a Chromecast (`sabrina`, Android 14) with Projectivy 4.73, the latest stable release listed in the [Projectivy release history](https://github.com/spocky/miproja1/releases/tag/4.73).

AuraMusic's channel is visible on the Projectivy home screen and contains Quick Picks. The service logged 10 published programs, updated in place on refresh. With the normal build, focusing a card enlarges its artwork. AuraMusic publishes poster and thumbnail artwork only; it does not set either preview URI.

![AuraMusic Quick Picks row with a focused card in Projectivy](../pr-evidence/projectivy-home-aura-focused-20261104.png)

Projectivy 4.51 added hover video previews for supported cards and a setting for when to show them ([release notes](https://github.com/spocky/miproja1/releases/tag/4.51)). The 4.73 notes do not mention a change to card previews. Android TV's [preview video API](https://developer.android.com/training/tv/discovery/preview-videos) accepts a publicly accessible HTTPS URI and allows audio or video media. Projectivy's public repository contains configuration and theme tooling, not its launcher implementation, and its release notes do not establish support for `previewAudioUri` specifically. Audio preview compatibility remains unverified.

## Chromecast focus-preview experiments

Tests ran on 2026-11-04 with AuraMusic 3.2.0 debug, Chromecast (`sabrina`, Android 14), and Projectivy 4.73. The channel had 10 programs. Tests used only D-pad movement and focus waits; no card was selected during preview checks. AuraMusic's media session stayed paused. We resolved audio-only URLs for the visible “Chrysopoeia” and “Lilypad” cards and tried both provider fields:

- `previewAudioUri`: focused each card for about 15 seconds, then moved between them. Projectivy PID 1417 created no audio player or media session; AuraMusic remained paused.
- `previewVideoUri` containing the same audio-only URL: repeated after force-stopping and reopening Projectivy to discard its in-memory card cache. No audio player or media session appeared during focus waits. Audio-only preview behavior is therefore not demonstrated on Projectivy 4.73.
- Muxed `previewVideoUri`: Projectivy did initialize Media3, but the focused surface turned black. Logcat showed `NoSupport [codec.profileLevel, hvc1.2.4.L123.B0, video/hevc]` and repeated decoder reclamation. This was not a successful visual or audio preview.

The H.264 request is only a resolver preference: `AuraVideo.getVideoStreamSource` may fall back to another codec, returns a sanitized container MIME, and exposes no URL expiry. A provider duration cannot enforce a 15–30 second cutoff. These limits prevent safely publishing the tested direct stream URL as a durable recommendation preview.

A separate card-selection check opened AuraMusic to the chosen song. That was a click-launch check and does **not** count as a hover preview. All experimental URI publishing was removed from the app source. Refresh logic now explicitly writes null preview URIs so a stale URI cannot survive reconciliation. The test did not clear app data. Preview animation/soundbars, playback stopping on focus loss, freshness/expiry, and a compatible short video preview remain unimplemented or unverified.
