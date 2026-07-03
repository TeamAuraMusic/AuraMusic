# AuraMusic — Interview Presentation Guide

> Prepared for the GiveDirectly Senior Software Engineer interview.

---

## Presentation Structure (10-15 minutes)

### Slide 1: What is AuraMusic?
- Open-source Android music player with YouTube Music integration
- Three build variants: Mobile FOSS, Mobile GMS (with Google Cast), Android TV
- 150K+ downloads, active user base, Weblate translations in 20+ languages
- Key differentiator: combines local library + YouTube Music streaming + video playback in one app

### Slide 2: Architecture Overview

```
+-------------------------------------------------+
|                    UI Layer                       |
|  Jetpack Compose (Mobile)  |  Compose TV (D-pad) |
+-------------------------------------------------+
|                 ViewModel Layer                   |
|  HomeViewModel | PlayerConnection | SearchVM     |
+-------------------------------------------------+
|                Data / Service Layer               |
|  MusicService (Media3) | KizzyRPC (Discord)      |
|  YouTube InnerTube API  | LastFM Scrobbling       |
+-------------------------------------------------+
|                Infrastructure                     |
|  Room DB | DataStore | Coil Images | Ktor HTTP    |
+-------------------------------------------------+
```

- **MVVM architecture** with Hilt dependency injection
- **Shared codebase** across mobile and TV via product flavors (FOSS/GMS)
- **Single Activity** with Compose navigation (mobile) and lightweight in-memory navigator (TV)
- **Media3/ExoPlayer** for playback with custom video stream merging

### Slide 3: Key Technical Challenges Solved

| Challenge | Solution | Impact |
|-----------|----------|--------|
| YouTube serves video and audio as separate streams | MergingMediaSource combines video-only + audio-only streams for 1080p+ playback | Users get HD video quality |
| TV has no touchscreen | Custom D-pad focus management, JS-injected WebView for login, BringIntoViewRequester for scroll sync | Full TV experience with remote control |
| Real-time lyrics synchronization | Multiple provider fallback chain (LRCLIB, BetterLyrics, Rush, SimpMusic) with word-level rendering | Lyrics work even when primary provider is down |
| Discord Rich Presence | WebSocket-based RPC with heartbeat, reconnection, image proxy uploads | Live "Listening to" status on Discord |
| Voice commands | VOSK offline speech recognition + intent parsing for 30+ commands | Hands-free music control |
| Sponsor integration | SponsorBlock API for auto-skipping sponsor segments | Better listening experience |

### Slide 4: What I Learned
- **End-to-end ownership**: From architecture decisions to debugging production issues on real devices
- **Cross-platform consistency**: Shared business logic across mobile and TV with different UI paradigms
- **Resilience**: Multi-provider fallbacks, reconnection strategies, graceful degradation
- **User empathy**: Building for people who use the app daily — every crash or missing feature affects real users

### Slide 5: Connection to GiveDirectly
- Built systems that must work reliably across diverse devices and network conditions — similar to delivering payments in hard-to-reach areas
- Experience with real-time data flows (Discord RPC, lyrics sync) translates to monitoring payment pipelines
- Understanding that infrastructure failures have human cost — a broken music player is annoying, a broken payment system is devastating
- The lean, high-ownership engineering approach at AuraMusic mirrors GiveDirectly's startup-like team

---

## Questions You Will Be Asked

### Q1: "Walk me through the architecture"

**Answer:**
"AuraMusic uses MVVM with Hilt for dependency injection. The data flow is: UI observes ViewModels, which expose StateFlows backed by Room database and network APIs. The MusicService runs as a foreground service managing ExoPlayer, and it is the single source of truth for playback state. For YouTube integration, we use InnerTube API endpoints directly rather than the official API, which gives us more control. The TV variant shares all business logic through product flavors — only the UI layer is different. Compose for mobile, and a custom Compose TV variant with D-pad focus management."

### Q2: "How does video playback work?"

**Answer:**
"YouTube serves high-quality video as separate video-only and audio-only streams. A single stream maxes out at 720p. To get 1080p+, we fetch both streams independently and merge them using ExoPlayer's MergingMediaSource. The flow is: detect if the song has a video version, search YouTube for the matching video, extract stream URLs from the YouTube player, create merged media sources, and replace the audio-only item in the queue. We cache search results to avoid redundant lookups. The tricky part is handling transitions — when a song auto-advances, we need to cancel the previous video fetch and start a new one without interrupting audio playback."

### Q3: "How do you handle the TV variant?"

**Answer:**
"Three product flavors: FOSS Mobile, GMS Mobile, and FOSS TV. The TV variant shares the same ViewModels, database, and service layer. The difference is the UI — instead of touch-based Compose, we use a custom TV Compose variant with D-pad navigation. Every focusable element has explicit focus handling: BringIntoViewRequester for scroll sync, FocusRequester for programmatic focus, and onPreviewKeyEvent for D-pad routing. The TV also has its own lightweight navigator instead of Navigation Compose, since TV screens are simpler. One interesting challenge was the Google login — WebView D-pad navigation required JavaScript injection to simulate focus traversal between form fields."

### Q4: "What was the hardest bug you fixed?"

**Answer:**
"The Discord RPC WebSocket lifecycle. When the user paused music, we called close() which permanently killed the coroutine scope. When they resumed, the WebSocket could never reconnect because launch {} calls in connect() would not execute on a dead scope. The fix was preserving the scope across close/reconnect cycles. But the deeper issue was that Discord Gateway uses different close codes (4000 for unknown error, 4004 for expired token, 4014 for disallowed intents) and we were treating them all the same. Adding proper close code handling with different reconnection strategies made the integration significantly more robust."

### Q5: "How do you handle offline or poor connectivity?"

**Answer:**
"Several layers: Room database caches all library data locally. YouTube stream URLs are fetched on-demand but we cache search results. Lyrics have a four-provider fallback chain — if one fails, we try the next. The connectivity observer watches network state and triggers retry logic for failed operations. For the TV variant specifically, we had to handle cases where the TV has intermittent WiFi — the recommendation service retries on boot, and the Discord RPC reconnects with exponential backoff. The key insight is that 'offline' on a TV often means 'intermittent' rather than 'completely disconnected'."

### Q6: "What would you improve about the codebase?"

**Answer:**
"Test coverage — we have minimal unit tests, especially for the Kizzy RPC library and video stream handling. I would also add structured error reporting instead of just logging. The Discord integration could use a centralized manager with proper state management instead of scattering lifecycle logic across MusicService. And the TV login flow using JavaScript injection into a WebView is fragile — if Discord changes their client storage, it breaks silently. A proper OAuth flow would be more reliable."

### Q7: "How does this connect to GiveDirectly's work?"

**Answer:**
"The core engineering challenges are similar: building systems that must work reliably across diverse devices and network conditions. AuraMusic runs on cheap Android tablets in Africa, high-end TVs, and phones with spotty connections. That is the same constraint as delivering payments through different channels to different devices. The multi-provider fallback pattern I built for lyrics is directly analogous to having multiple payment rails. And the TV variant experience — building UIs for non-touchscreen devices with limited input methods — maps to building tools for field workers who might be using basic Android devices."

### Q8: "Tell me about a time you had to make a tradeoff."

**Answer:**
"The video playback system. We could have used YouTube's official embed, which is simpler but caps at 720p and shows YouTube branding. Instead, we built a custom merge pipeline that fetches raw streams. This gave us 1080p+ quality and full UI control, but introduced complexity: we had to handle video/audio sync, manage two concurrent network requests, deal with stream URL expiry, and handle the black screen during transitions. The tradeoff was worth it because video quality is the number one user complaint in music apps, but it meant maintaining a fragile integration that breaks when YouTube changes their player internals."

### Q9: "How do you prioritize what to work on?"

**Answer:**
"I triage by impact times effort. Critical bugs get fixed immediately — crashes, data loss, broken playback. Then I look at what unblocks the most users. When video playback was broken for auto-advance (songs playing audio but showing black screen), that affected every user watching music videos, so it was top priority. For features, I focus on what existing users are requesting most. The TV variant was a big bet — I estimated it would expand our user base significantly, and it did. For a project like GiveDirectly, I would apply the same logic: payment reliability first, then features that help field workers operate more efficiently."

### Q10: "What is your development setup?"

**Answer:**
"I use Android Studio with the project set up for three product flavors. For the Discord integration, I run a local WebSocket proxy to test without spamming my actual Discord status. For YouTube integration, I test against both authenticated and unauthenticated states. The CI pipeline runs on GitHub Actions — it builds all three variants, signs them, and can create releases. I also use Logcat extensively for debugging service-level issues since MusicService runs in a separate process."

---

## Key Numbers to Remember

| Metric | Value |
|--------|-------|
| Downloads | 150K+ |
| Languages | 20+ (Weblate) |
| Build variants | 3 (FOSS Mobile, GMS Mobile, FOSS TV) |
| Lyrics providers | 4 (LRCLIB, BetterLyrics, Rush, SimpMusic) |
| Voice commands | 30+ |
| Video quality | Up to 1080p+ via stream merging |
| Min SDK | 23 (Android 6.0) |
| Target SDK | 36 |
| Language | Kotlin, Jetpack Compose |

---

## Tips for the Presentation

1. **Lead with impact, not code** — "This app serves 150K+ users across 20 languages" before "It uses MVVM with Hilt"
2. **Demo if possible** — Have it running on your tablet. Show the TV UI if you can mirror it
3. **Be honest about AI** — "I used AI as a development accelerator for boilerplate and exploration, but all architecture decisions and debugging were hands-on"
4. **Connect to GiveDirectly** — Every technical answer should end with "and that maps to GiveDirectly because..."
5. **Admit gaps gracefully** — "I am not a Kotlin language expert, but I understand the patterns and can reason about the system design"
6. **Ask questions back** — "How does your team handle deployment reliability?" or "What is the biggest technical challenge your Payments team is facing?"

---

## Quick Reference: What You Built

| Component | What It Does |
|-----------|-------------|
| **MusicService** | Foreground service managing ExoPlayer, notifications, Discord RPC, scrobbling |
| **HomeViewModel** | Aggregates local DB + YouTube API data for the home screen |
| **PlayerConnection** | Bridges UI to MusicService, exposes playback state as StateFlows |
| **DiscordRPC / KizzyRPC** | WebSocket-based Discord Rich Presence with heartbeat, reconnect, image proxy |
| **Video Merge Pipeline** | Fetches separate video+audio streams from YouTube, merges via MergingMediaSource |
| **TV Compose UI** | Custom D-pad navigation, focus management, lightweight navigator |
| **Voice Commands** | VOSK offline speech to intent parsing to player actions |
| **Lyrics System** | Multi-provider fallback chain with word-level rendering |
| **SponsorBlock** | Auto-skip sponsor/intro/outro segments via SponsorBlock API |
| **InnerTube Integration** | Direct YouTube Music API access for search, browse, streaming |
