<div align="center">
  <img src="https://raw.githubusercontent.com/TeamAuraMusic/AuraMusic/main/app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png" width="140" height="140" alt="AuraMusic logo"/>
  <h1>Team AuraMusic</h1>
  <p><strong>We build open-source software for listening to music on your own terms.</strong></p>
  <p>
    <a href="https://github.com/TeamAuraMusic/AuraMusic/releases"><img src="https://img.shields.io/github/v/release/TeamAuraMusic/AuraMusic?style=for-the-badge&label=Latest%20release" alt="Latest release"></a>
    <a href="https://github.com/TeamAuraMusic/AuraMusic/stargazers"><img src="https://img.shields.io/github/stars/TeamAuraMusic/AuraMusic?style=for-the-badge&label=Stars&color=gold" alt="Stars"></a>
    <a href="https://github.com/TeamAuraMusic/AuraMusic/network/members"><img src="https://img.shields.io/github/forks/TeamAuraMusic/AuraMusic?style=for-the-badge&label=Forks&color=blue" alt="Forks"></a>
    <a href="https://github.com/TeamAuraMusic/AuraMusic/blob/main/LICENSE"><img src="https://img.shields.io/github/license/TeamAuraMusic/AuraMusic?style=for-the-badge&label=License" alt="License"></a>
  </p>
  <p>
    <a href="https://www.auramusic.site/">Website</a> •
    <a href="https://discord.gg/H6Nvy6Fs7Z">Discord</a> •
    <a href="https://t.me/AuraMusicUpdates">Telegram</a> •
    <a href="https://hosted.weblate.org/projects/auramusic/">Weblate</a>
  </p>
</div>

---

## Who we are

We're a small, volunteer-run team shipping free and open-source media software. Our
flagship project, **AuraMusic**, is a YouTube Music client for Android with real
audio-engineering features — real-time equalizer, normalization, crossfade, tempo and
pitch control — plus a native Android TV / Google TV app. Everything we publish is GPL-3.0.

We build things we wanted to exist: no ads, no account walls, no proprietary sync layer,
and a FOSS build that works without Google Play Services.

## Our projects

| Project | Description | Stack | License |
| --- | --- | --- | --- |
| [AuraMusic](https://github.com/TeamAuraMusic/AuraMusic) | YouTube Music client for phones and Android TV / Google TV | Kotlin, Compose | GPL-3.0 |
| [AuraMusicServer](https://github.com/TeamAuraMusic/AuraMusicServer) | WebSocket backend for the Listen Together sync feature | Go | GPL-3.0 |
| [AuraMusicCanvasServer](https://github.com/TeamAuraMusic/AuraMusicCanvasServer) | API that serves looping canvas visuals for songs, albums and artists | JavaScript | GPL-3.0 |
| [AuraMusicSite](https://github.com/TeamAuraMusic/AuraMusicSite) | The official landing page for AuraMusic | TypeScript, Next.js | — |

### AuraMusic — the app

<img src="https://raw.githubusercontent.com/TeamAuraMusic/AuraMusic/main/fastlane/metadata/android/en-US/images/phoneScreenshots/1.jpg" width="24%" alt="AuraMusic home" />
<img src="https://raw.githubusercontent.com/TeamAuraMusic/AuraMusic/main/fastlane/metadata/android/en-US/images/phoneScreenshots/2.jpg" width="24%" alt="AuraMusic player" />
<img src="https://raw.githubusercontent.com/TeamAuraMusic/AuraMusic/main/fastlane/metadata/android/en-US/images/phoneScreenshots/3.jpg" width="24%" alt="AuraMusic lyrics" />
<img src="https://raw.githubusercontent.com/TeamAuraMusic/AuraMusic/main/fastlane/metadata/android/en-US/images/phoneScreenshots/4.jpg" width="24%" alt="AuraMusic search" />

**Playback & streaming**
- Stream and cache any song or video from YouTube Music, with offline playback and background service
- Search songs, albums, artists, videos and playlists; sign in with a YouTube Music account and sync your library both ways
- Video playback with subtitles, SponsorBlock skipping and AutoMix handling
- Audio quality selection, audio offload, and seamless gapless playback

**Audio**
- Equalizer with custom presets, audio normalization, skip silence
- Crossfade with adjustable duration, tempo and pitch adjustment
- Sleep timer and alarm clock

**Lyrics**
- Live synchronized, word-by-word lyrics
- Seven provider backends: Kugou, LRCLib, Rush, BetterLyrics, Musixmatch, SimpMusic and Paxsenix
- Lyrics translation, including an optional bring-your-own-model AI translation mode

**Beyond playback**
- **Listen Together** — synchronized group listening over WebSockets
- **Voice control** — offline wake-word detection ("Hey Aura") with TTS feedback
- **Google Cast** — send audio to Chromecast, speakers and Android TV
- **AuraCanvas** — experimental looping visuals behind album art
- Shazam-style music recognition, Last.fm scrobbling, Discord Rich Presence
- Home screen widget, backups, listening stats, and a yearly recap

**Android TV / Google TV**
- Native Leanback app with D-pad–optimized navigation
- A TV-native UI, video playback, in-app screensaver, and a recommendation channel

**Build flavors:** `foss` (no Google Play Services, F-Droid compatible) and `gms`
(with Cast). Mobile and TV UI variants, in universal and per-ABI builds.

<a href="https://github.com/TeamAuraMusic/AuraMusic/releases/latest/download/AuraMusic.apk">
  <img src="https://github.com/machiav3lli/oandbackupx/blob/034b226cea5c1b30eb4f6a6f313e4dadcbb0ece4/badge_github.png" alt="Download AuraMusic" height="82">
</a>

<p>Full feature list, build instructions and screenshots live in the <a href="https://github.com/TeamAuraMusic/AuraMusic">AuraMusic repository</a>.</p>

## How we build

AuraMusic is a Gradle multi-module project in Kotlin, on Jetpack Compose and Material 3.

- **Language / UI** — Kotlin 2.3, Jetpack Compose, Material 3 (dynamic color, light/dark/black themes)
- **Audio** — Media3 / ExoPlayer, native C++ (CMake) DSP for the audio processing path
- **Architecture** — MVVM with Hilt, Room, DataStore, coroutines and Flow
- **Networking** — Ktor, kotlinx.serialization, Jsoup
- **Build** — AGP 9, Gradle 9.4, JDK 21, compileSdk 36, minSdk 23
- **CI/CD** — GitHub Actions matrix builds (7 APK variants per push) plus automated releases
- **Localization** — 1,000+ strings, translated through Weblate

Modules are split by concern: `innertube` (YouTube Music API client), `auravideo`
(video streaming), plus one module per integration (`kugou`, `lrclib`, `rush`,
`betterlyrics`, `musixmatch`, `simpmusic`, `paxsenix`, `lastfm`, `kizzy`, `shazamkit`).

## Contributing

We welcome pull requests from anyone.

- **Code** — read [`README.md`](https://github.com/TeamAuraMusic/AuraMusic#readme) for build instructions, then open a PR. Bug reports and feature requests go to the [issue tracker](https://github.com/TeamAuraMusic/AuraMusic/issues).
- **Translations** — all 15 languages are community-maintained on [Weblate](https://hosted.weblate.org/projects/auramusic/). No coding required.

<p align="center">
  <a href="https://github.com/TeamAuraMusic/AuraMusic/graphs/contributors">
    <img src="https://contrib.rocks/image?repo=TeamAuraMusic/AuraMusic" alt="AuraMusic contributors">
  </a>
</p>

Thanks to everyone who has filed an issue, translated a string, or sent a patch.

## Standing on shoulders

AuraMusic would not exist without the open-source community:

- **[InnerTune](https://github.com/z-huang)** — Zion Huang, Malopieds
- **[OuterTune](https://github.com/DD3Boh)** — Davide Garberi, Michael Zh
- **[Metrolist](https://github.com/MetrolistGroup/Metrolist)** — the project AuraMusic is based on

And the projects that power individual features: [Kizzy](https://github.com/dead8309/Kizzy)
(Discord Rich Presence), [Rush](https://github.com/shub39/Rush) (lyrics),
[Better Lyrics](https://better-lyrics.boidu.dev), [SimpMusic](https://github.com/maxrave-dev/SimpMusic),
[MusicRecognizer](https://github.com/aleksey-saenko/MusicRecognizer),
[Flow](https://github.com/A-EDev/Flow), [Vosk](https://github.com/alphacep/vosk-api),
[Monochrome](https://github.com/monochrome-music/monochrome), and
[SponsorBlock](https://github.com/ajayyy/SponsorBlock).

## Community

<table>
<tr>
<td align="center">
  <a href="https://discord.gg/H6Nvy6Fs7Z"><img src="https://logotyp.us/file/discord.svg" alt="Discord" width="46" height="46"></a><br>
  <a href="https://discord.gg/H6Nvy6Fs7Z">Discord</a>
</td>
<td align="center">
  <a href="https://t.me/AuraMusicUpdates"><img src="https://upload.wikimedia.org/wikipedia/commons/8/82/Telegram_logo.svg" alt="Telegram" width="46" height="46"></a><br>
  <a href="https://t.me/AuraMusicUpdates">Telegram</a>
</td>
<td align="center">
  <a href="https://hosted.weblate.org/projects/auramusic/"><img src="https://hosted.weblate.org/widgets/auramusic/horizontal-auto.svg" alt="Translation status" height="32"></a><br>
  <a href="https://hosted.weblate.org/projects/auramusic/">Translate on Weblate</a>
</td>
</tr>
</table>

## Support us

AuraMusic is free and always will be. If it earns your support:

<a href="https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=franklinfinyange%40gmail.com">
  <img src="https://www.paypalobjects.com/webstatic/i/logo/rebrand/ppcom.svg" alt="PayPal" height="52">
</a>
&nbsp;&nbsp;
<a href="https://liberapay.com/chila254/donate">
  <img src="https://liberapay.com/assets/widgets/donate.svg" alt="Donate using Liberapay" height="52">
</a>
&nbsp;&nbsp;
<a href="https://ko-fi.com/chila254">
  <img src="https://img.shields.io/badge/Ko--fi-FF5E5B?style=for-the-badge&logo=kofi&logoColor=white" alt="Support on Ko-fi">
</a>

## License & disclaimer

All AuraMusic projects are released under the
[GNU General Public License v3.0](https://github.com/TeamAuraMusic/AuraMusic/blob/main/LICENSE).

These projects are not affiliated with, funded by, authorized by, or endorsed by YouTube,
Google LLC, Spotify AB, or any of their affiliates. All trademarks and intellectual property
remain with their respective owners.

<div align="center">
  <sub>Made with ❤️ by <a href="https://github.com/chila254">chila254</a> and the AuraMusic contributors</sub>
</div>