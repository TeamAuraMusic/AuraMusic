# Android TV preview compatibility

Checked 2026-11-04 on a Chromecast (`sabrina`, Android 14) with Projectivy 4.73, the latest stable release listed in the [Projectivy release history](https://github.com/spocky/miproja1/releases/tag/4.73).

AuraMusic's channel is visible on the Projectivy home screen and contains Quick Picks. The service logged 10 published programs, updated in place on refresh. With the normal build, focusing a card enlarges its artwork. AuraMusic publishes poster and thumbnail artwork only; it does not set either preview URI.

![AuraMusic Quick Picks row with a focused card in Projectivy](../pr-evidence/projectivy-home-aura-focused-20261104.png)

Projectivy 4.51 added hover video previews for supported cards and a setting for when to show them ([release notes](https://github.com/spocky/miproja1/releases/tag/4.51)). The 4.73 notes do not mention a change to card previews. Android TV's [preview video API](https://developer.android.com/training/tv/discovery/preview-videos) accepts a publicly accessible HTTPS URI and allows audio or video media. Projectivy's public repository contains configuration and theme tooling, not its launcher implementation, and its release notes do not establish support for `previewAudioUri` specifically. Audio preview compatibility remains unverified.

## Chromecast focus-preview experiments

Tests ran on 2026-11-04 with AuraMusic 3.2.0 debug, Chromecast (`sabrina`, Android 14), and Projectivy 4.73. The channel had 10 programs. Tests used only D-pad movement and focus waits; no card was selected during preview checks. AuraMusic's media session stayed paused. We resolved audio-only URLs for the visible “Chrysopoeia” and “Lilypad” cards and tried both provider fields:

- `previewAudioUri`: focused each card for about 15 seconds, then moved between them. Projectivy PID 1417 created no audio player or media session; AuraMusic remained paused.
- `previewVideoUri` containing the same audio-only URL: repeated after force-stopping and reopening Projectivy to discard its in-memory card cache. No audio player or media session appeared during focus waits. Audio-only preview behavior is therefore not demonstrated on Projectivy 4.73.
- Muxed `previewVideoUri`: a test-only update published a 360p muxed H.264/AAC URL for the `Chrysopoeia` card, then Projectivy was force-stopped and reopened. D-pad focus on that card started a Projectivy-owned 44.1 kHz `AudioTrack` (UID 10150/PID 23838); moving right to `Lilypad` paused, stopped, and released it; moving left to `Chrysopoeia` started a new track. AuraMusic's own MediaSession remained paused throughout this focus test. No card was selected, so this is hover-triggered preview playback, not app launch.
- The preview card still showed static artwork; no video frame or soundbar animation appeared. Projectivy logs showed both AVC and HEVC decoder activity during the experiment, so the exact URI/codec it consumed is not fully established. Earlier muxed tests also showed `NoSupport [codec.profileLevel, hvc1.2.4.L123.B0, video/hevc]` and decoder reclamation. Audio playback on focus is demonstrated; the visual-preview requirement is not.

The H.264 request is only a resolver preference: `AuraVideo.getVideoStreamSource` may fall back to another codec, returns a sanitized container MIME, and exposes no URL expiry. A provider duration cannot enforce a 15–30 second cutoff. These limits prevent safely publishing the tested direct stream URL as a durable recommendation preview without additional expiry/re-resolution handling. The test-only URI was removed from source and the production build was reinstalled; no app data was cleared.

A separate card-selection check opened AuraMusic to the chosen song. That was a click-launch check and does **not** count as a hover preview. The hover result above came only from D-pad focus changes. Refresh logic explicitly writes null preview URIs in the production build so stale URIs cannot survive reconciliation. Projectivy stops its preview player on focus loss in this test; visual animation/video, freshness/expiry, and a durable preview URI strategy remain unimplemented or unverified.
