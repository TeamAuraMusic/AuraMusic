# AuraMusic v2.5.0 (Build 22) Release Notes

> [!NOTE]
> This release brings AuraCanvas - dynamic video backdrops for artist headers and album covers, fixes for BetterLyrics TTML fetching and parsing, improved HTML entity decoding in SimpMusic, monochrome layer support for adaptive icons on Android 13+, and updated Discord invite link.

## What's New

### AuraCanvas - Dynamic Video Backdrops
Implemented by @chila254

- feat(canvas): implement AuraCanvas for artist headers and album covers (#712a0d0)
- Improve AuraCanvasOverlay: switch to TextureView, add error handling and fade-in animation (#caa49b3)
- feat(player): AuraCanvas - looping video backdrops behind album art (#b280ca6)
- fix(canvas): make AuraCanvas actually show videos in the player (#677a2fc)

AuraCanvas brings dynamic video backdrops to artist headers and album covers. This feature adds visual depth to the app with looping video backgrounds that enhance the music experience.

### BetterLyrics & SimpMusic Improvements
Implemented by @chila254

- fix(betterlyrics): implement correct TTML fetching and parsing (#4e6cc0d)
- fix(lyrics): remove broken getAllLyrics implementation in BetterLyrics (#91c21de, #9128ae0)
- fix(lyrics): make BetterLyrics actually return lyrics, honour provider priority, and unblock the retry button (#9128ae0)
- Fix HTML entity decoding in SimpMusic lyrics provider (#bcc23c0)

### Adaptive Icons Enhancement
Implemented by @chila254

- feat(icons): Add monochrome layer to all adaptive icons for better dynamic/themed icon consistency on Android 13+ (#4a9f2c9)

### Networking Improvements
Implemented by @chila254

- Changed the client engine to cio (#19902a3)

### Bug Fixes
Implemented by @chila254

- Fix start page playback and speed dial shuffle loading (#3e6a1d3)
- fix(discord): update Discord invite link to https://discord.gg/935CRM8u3 in About section and README (#2722548)

**Full changes in this release**: https://github.com/TeamAuraMusic/AuraMusic/compare/065a222...3e6a1d3

## Translation Contributors

- @Mickaël81 implemented Translated using Weblate (French) (#a9b7750, #192fc32, #8c5838e, #7706e68)
- @SantosSi implemented Translated using Weblate (Portuguese (Portugal)) (#b78d4fc, #f8bbad5)
- @SantosSi implemented Translated using Weblate (Portuguese) (#0d7b201, #f0f5ca4)
- @iamcrizzzgd implemented Translated using Weblate (Spanish) (#4a104db, #377359c, #7afae6a, #c47a4e4, #1b1bfc6)

# AuraMusic v2.4.0 (Build 21) Release Notes

> [!NOTE]
> This release brings true 1080p+ video playback, server-powered Karaoke, PO token + BotGuard playback fixes, a full Enhanced Lyrics rebuild with the beautiful new Monochrome animated style, dramatically sharper thumbnails, and a modernized About screen that now properly credits our translators.

## What's New

### True 1080p+ Video Playback
You can finally select 1080p (and higher) and actually get the quality. AuraMusic now merges YouTube's separate high-resolution video-only streams with premium audio streams when needed.

### Server Karaoke
New dedicated ML karaoke server integration (https://karaoke.auramusic.site/). Works with downloaded songs, has a polished connection UI with progress, "Connected ✓", smart retries, and is hardened against cold starts.

### Reliable Playback with PO Tokens
Full WebView + BotGuard PO token implementation + automatic invalidation/recovery. This eliminates the common "Code: 2000 (IO_UNSPECIFIED)" playback failures.

### Enhanced Lyrics Overhaul
- Rebuilt word-level rendering engine
- Brand new Monochrome animated lyrics + background style
- Instrumental indicators and connected lines now work correctly in enhanced mode
- Many visual and animation polish improvements

### Much Sharper Thumbnails & Artwork
Major upgrade to the thumbnail resizer so it works with every Google CDN host and always requests the highest quality images.

### Modern About Screen & Contributors
- Beautiful 2-column grid with real GitHub avatars
- Liberapay support added
- Translation contributors now proudly listed with profile pictures

## New Contributors (First time in AuraMusic)

- [Mickaël Binos](https://github.com/Mickael81) made their first contribution in AuraMusic with the commit [Added translation using Weblate (French)](https://github.com/TeamAuraMusic/AuraMusic/commit/806caacb6729e93546ebbe4ab091c52929e640c9)

- [ItsMeCrizzzGD](https://github.com/iamcrizzzgd) made their first contribution in AuraMusic with the commit [Translated using Weblate (Spanish)](https://github.com/TeamAuraMusic/AuraMusic/commit/9f624fbb75b731381247af9d298cd597e0f85a39)

- [Silvério Santos](https://github.com/SantosSi) made their first contribution in AuraMusic with the commit [Added translation using Weblate (Portuguese (Portugal))](https://github.com/TeamAuraMusic/AuraMusic/commit/c5659da886b5fc547b3ca26ee9a02702ac2bf9f6)

**Full changes in this release**: https://github.com/TeamAuraMusic/AuraMusic/compare/2e1feb0...66cbc3b

# AuraMusic v2.3.0 (Build 20) Release Notes

> [!NOTE]
> This release introduces hardware integration, enhanced lyrics features, and significant UI improvements including better thumbnail quality and internationalization support.

## What's New

### Hardware Integration & Smart Device Ecosystem
A complete hardware integration system with Bluetooth device support and audio device management:

**Audio Device Integration**
- Added audio device picker style for mini-player
- Implemented Bluetooth profile proxy usage
- Enhanced active hardware flow and dialog layout
- Fixed smart-cast errors in Bluetooth handling

**Alarm Features**
- Added wake-up and snooze alarm functionality
- Integrated with hardware ecosystem for comprehensive device control

### 🎤 Enhanced Lyrics Features
Major lyrics enhancements with visual indicators and improved readability:

**Instrumental & Interval Indicators**
- Added instrumental indicators in enhanced lyrics mode
- Implemented connected lines for better lyrics flow
- Added intro wavy circular progress indicator before first vocal line
- Enhanced lyrics with interval indicators and timing improvements

**Typography & Fonts**
- Added custom font support with Google Fonts integration
- Added Material 3 font icon support
- Improved font application throughout the app

### 🌐 Internationalization Support
- Added Weblate translation badge and link for community-driven translations

## UI/UX Improvements
- Enhanced thumbnail quality for all YouTube videos and streamed songs
- Fixed blurry album and item thumbnails
- Improved UI with smooth transitions and better visuals
- Replaced circular refresh indicator with ContainedLoadingIndicator
- Added font selection and application options
- Moved queue to left in old player design
- Enhanced share as image functionality with background options

## Bug Fixes

### Navigation & UI
- Fixed NPE crash in backToMain navigation
- Fixed TV settings focus restoration when returning from sub-settings
- Fixed video mode persistence issues
- Fixed song click handling that was broken by combinedClickable
- Fixed refresh indicator positioning

### Hardware & Connectivity
- Fixed Bluetooth smart-cast errors
- Resolved hardware integration issues

### Lyrics & Media
- Fixed lyrics provider issues and instrumental indicator support
- Fixed instrumental indicator and connected lines for all lyrics providers
- Reduced first vocal line delay to 1000ms (1 second)

### Build & Compatibility
- Fixed TV APK naming and updater support
- Resolved Android Gradle Plugin API compatibility issues
- Fixed F-Droid Java version compatibility by removing jvmToolchain
- Updated JVM target to Java 21 to fix compilation inconsistency
- Fixed sourceSets API and replaced deprecated buildDir usage

## ⚙️ Build
- Bumped versionCode to **20**
- Version: **2.3.0**

---

**Full Changelog**: https://github.com/TeamAuraMusic/AuraMusic/compare/v2.2.0...v2.3.0

---

# AuraMusic v2.2.0 (Build 19) Release Notes

## What's New

### 🎉 Android TV / Google TV Support
A complete TV-optimized client with D-pad navigation, large controls, and focus management:

**TV Home Screen**
- Personalized Quick Picks (your most played songs)
- Forgotten Favorites (songs you haven't listened to in a while)
- Keep Listening (resume recent playback)
- Similar Recommendations based on current song
- YouTube Home sections (Trending, Moods, Charts, etc.)
- Your YouTube Playlists
- Hero carousel with featured content

**TV Player**
- Full-screen player with large centered controls
- Play/Pause, Skip, Rewind/Fast-forward 10s buttons
- Progress bar with current/time indicators
- Queue sidebar showing upcoming songs
- Sleep timer and lyrics toggle buttons
- Video mode support for music videos
- Marquee scrolling for long titles

**TV Navigation & Focus**
- Custom lightweight navigator with back stack
- Bidirectional navigation: UP from content goes to top bar, DOWN from top bar goes to last focused content
- Per-section focus requesters prevent drift
- Back from sub-settings restores focus to previously selected item
- Smooth focus animations and visual feedback

**TV Settings**
- Appearance: Theme selection, dynamic colors, theme color picker
- Content: Auto-load queue toggle (extends queue automatically)
- Storage: Cache management with clear cache button
- Updater: Real update checking with GitHub API and download links
- About: App version, build info, architecture

**Radio Queue on TV**
- Tapping any song in Quick Picks, Forgotten Favorites, or Keep Listening now loads a YouTube radio queue with related songs
- Matches mobile behavior — no more single-song queues!

### 🎤 Voice Command Improvements
- Added confidence and audio energy filtering to reduce false wake word triggers
- Lowered wake word detection thresholds for maximum sensitivity
- Added AEC (Acoustic Echo Cancellation), NoiseSuppressor, and RMS energy filtering
- Fixed wake word service to stop when starting manual voice session
- Fixed minimum speech length requirements for command mode
- Improved TTS greeting and audio ducking during voice commands
- Fixed microphone loop by stopping wake word service before restart

### 🎨 UI/UX Improvements
- Added sleep timer and lyrics buttons to queue bar in new player design
- Added shuffle button with 4-dot animation to old player design
- Added kebab menu with animations to old player design
- Added gradient to static icon foreground for visual consistency
- Changed dynamic icon background from orange to grey for better visibility
- Fixed default icon background to black when installing
- Moved kebab menu from top area to bottom right
- Added gradient colors to dynamic icon foreground

### 🧩 Widget Redesigns
- Increased compact square widget to 4x4 size
- Modernized music player, compact square, and compact wide widgets
- Added full-cover album art backgrounds
- Added placeholder image to turntable widget album art
- Fixed widget showing 'can't load widget' when service not running
- Fixed widget_wide_play_container to widget_wide_play_pause

### 🐛 Bug Fixes

#### TV
- Fixed TV settings back navigation: focus now restores to previously selected item instead of top nav bar
- Fixed TV lyrics not displaying (improved song change handling)
- Fixed TV lyrics storage — now fetched fresh per song without database persistence
- Fixed TV content settings compilation and Add/Clear queue functionality
- Fixed TV navigation focus issues across Home, Details, Player, and Settings screens
- Fixed TV player white screen on launch
- Fixed TV UP navigation in all screens
- Fixed TV long song titles pushing down icons — added marquee scrolling
- Fixed TV home screen title to "AuraMusic Tv"
- Fixed TV lyrics to be display-only (no click-to-seek, no autoscroll)
- Fixed TV streaming cache and persistent lyrics toggle
- Fixed TV mini-player display and navigation issues
- Fixed TV compilation errors throughout module

#### Mobile
- Fixed ForegroundServiceDidNotStartInTimeException on Android 14+/SDK 36
- Fixed ANR caused by VOSK native cleanup blocking main thread
- Fixed SecurityException when starting microphone FGS from background on Android 14+
- Fixed VOSK detector memory leaks and false wake word triggers
- Fixed mic contention between VOSK wake word and SpeechRecognizer
- Fixed TTS volume muting after voice commands
- Fixed VOSK model download corruption and validation
- Fixed "Hey Aura" / "Hello Aura" not recognizing
- Fixed wake word detection not triggering overlay
- Fixed standalone 'aura' false positives in wake word grammar

### ⚙️ Build
- Bumped versionCode to **19**
- Version: **2.2.0**

---

**Full Changelog**: https://github.com/TeamAuraMusic/AuraMusic/compare/v2.1.0...v2.2.0

---

# AuraMusic v2.1.0 (Build 18) Release Notes

> [!WARNING]
> When hands-free wake word is enabled, it may cause high battery drain, occasional false triggers during playback, and background microphone usage on Android 14+. Use with caution.

## What's New
- Major release with voice command improvements, Google Cast support, and widget redesigns
- Added hands-free "Hey Aura" wake word detection using VOSK offline speech recognition
- Added voice commands with interactive overlay and text-to-speech feedback
- Added Google Cast support for GMS variant
- Redesigned widgets with modern UI and full-cover album art
- Improved voice command accuracy and wake word sensitivity
- Fixed ANR issues and memory leaks in VOSK service

### Hands-Free Wake Word Detection
- Offline wake word detection using VOSK (no internet required)
- Downloads ~40MB English model on first launch
- Added audio filtering (AEC, noise suppression, RMS energy) to reduce false triggers
- Lowered detection thresholds for maximum sensitivity
- Auto-restarts after voice command execution

### Voice Commands
- Interactive overlay with wave animations (Siri/Gemini-like)
- Text-to-Speech feedback with multi-voice selection and audio ducking
- Comprehensive voice command support with 40+ commands
- Automatic volume restoration after voice commands

#### Command Reference
| Category | Commands |
|----------|---------|
| **Playback** | play, pause, next, previous, shuffle on/off, repeat one/all/off |
| **Seek** | skip forward N seconds/minutes, go back N seconds/minutes |
| **Volume** | volume up/down, mute, unmute |
| **Speed** | speed up, slow down, normal speed |
| **Search** | search, play [song/artist] |
| **Downloads** | download this song, download playlist, download album |
| **Lyrics** | show lyrics, hide lyrics, toggle lyrics |
| **Video** | video on/off, toggle video |
| **Media** | like, show queue, clear queue, add to queue |
| **Settings** | dark mode on/off, toggle theme |
| **Navigation** | go home, go library, open search, open settings |

### Google Cast Support (GMS variant only)
- Added Cast device discovery and selection
- Cast picker sheet for easy device selection
- Compatible with Chromecast and smart TVs

### Widget Redesigns
- Modernized compact square, compact wide, and music player widgets
- Increased compact square widget to 4x4 size
- Added full-cover album art backgrounds
- Removed turntable widget

### Old Player Design Enhancements
- Added sleep timer and lyrics buttons to queue bar
- Added shuffle button with 4-dot animation
- Added kebab menu with animations

### Bug Fixes
- Fixed ForegroundServiceDidNotStartInTimeException on Android 14+/SDK 36
- Fixed ANR in VOSK native cleanup blocking main thread
- Fixed security exception when starting microphone FGS from background on Android 14+
- Fixed VOSK detector memory leaks
- Fixed mic contention between wake word and voice recognition
- Fixed TTS volume muting after voice commands
- Fixed widget loading when service not running

---

# AuraMusic v2.0.0 (Build 17) Release Notes

## What's New
- Major release with significant UI/UX improvements and bug fixes
- Added liquid glass customization options with blur radius, corner radius, and opacity controls
- Added Discord and Telegram links to About screen and README
- Improved shuffle button with 4-dot animation
- Fixed video fit mode persistence and loading speed
- Fixed lyrics provider preference to always respect user selection
- Database migrations fixed for seamless upgrades

### Liquid Glass Customization
- Added blur radius, corner radius, and opacity options in Appearance Settings
- Users can now customize the liquid glass effect to their preference

### Social Links
- Added Discord and Telegram links to About screen
- Updated README with socials section

### Shuffle Button Improvements
- Added 4-dot shuffle button with animations to speed dial
- Improved loading indicator size and synchronization with isPlaying
- Track loaded song ID and stop loading when mediaMetadata matches

### Video Playback Improvements
- Fixed video fit mode persistence across app restarts
- Improved video loading speed with sequential subtitle fetching
- Added auto-play on first frame
- Removed unnecessary video toast message after successful load

### Lyrics Improvements
- Fixed Rush lyrics sync by converting duration ms to seconds
- Fixed user lyrics selection to always respect preferred provider
- Refetch lyrics if cached from different provider
- Fixed lyrics provider conflicts and video playback in Speed Dial & Keep Listening

### Database Migrations
- Fixed SpeedDialItem musicVideoType column with manual migration
- Converted 31-32 and 32-33 DB migrations to manual
- Registered DB migrations in Hilt DI module to prevent crash on upgrade
- Fixed duplicate column error with IF NOT EXISTS and column existence checks

### Build Updates
- Updated tinypinyin version to 2.0.1
- Reorganized About screen layout
- Added SpeedDialGridItem playing indicator in center

### Full Changelog (Commits since last release):
- [`69a0b4a`](https://github.com/TeamAuraMusic/AuraMusic/commit/69a0b4a) Update SpeedDialGridItem to show playing indicator in center
- [`2a1244b`](https://github.com/TeamAuraMusic/AuraMusic/commit/2a1244b) Fix SpeedDialGridItem compile error
- [`582e54c`](https://github.com/TeamAuraMusic/AuraMusic/commit/582e54c) Fix video fit mode persistence, improve video loading speed, update shuffle button
- [`f788b6c`](https://github.com/TeamAuraMusic/AuraMusic/commit/f788b6c) Optimize subtitle fetching to run sequentially, add auto-play on first frame
- [`94c6e0f`](https://github.com/TeamAuraMusic/AuraMusic/commit/94c6e0f) Fix: Add missing setValue import for var delegation in HomeScreen
- [`db4bccc`](https://github.com/TeamAuraMusic/AuraMusic/commit/db4bccc) Add 4-dot shuffle button with animations to speed dial
- [`3953e46`](https://github.com/TeamAuraMusic/AuraMusic/commit/3953e46) Fix shuffle button: increase dot spacing and loading indicator size, fix loading sync with isPlaying
- [`a0a32f3`](https://github.com/TeamAuraMusic/AuraMusic/commit/a0a32f3) Fix shuffle button loading: track loaded song ID and stop when mediaMetadata matches
- [`4bdc7db`](https://github.com/TeamAuraMusic/AuraMusic/commit/4bdc7db) Remove video toast message after video loads successfully
- [`db37313`](https://github.com/TeamAuraMusic/AuraMusic/commit/db37313) Add Telegram link to README socials section
- [`2f0fe22`](https://github.com/TeamAuraMusic/AuraMusic/commit/2f0fe22) Add Telegram icon to socials section
- [`67c81d0`](https://github.com/TeamAuraMusic/AuraMusic/commit/67c81d0) Add liquid glass customization options (blur radius, corner radius, opacity) in appearance settings
- [`aa59687`](https://github.com/TeamAuraMusic/AuraMusic/commit/aa59687) Add Discord to socials section in README
- [`1d07170`](https://github.com/TeamAuraMusic/AuraMusic/commit/1d07170) Fix Discord logo URL in README
- [`87b34b9`](https://github.com/TeamAuraMusic/AuraMusic/commit/87b34b9) Update Discord logo URL to working source
- [`95a8b7d`](https://github.com/TeamAuraMusic/AuraMusic/commit/95a8b7d) Add Discord and Telegram links to About screen
- [`db1947a`](https://github.com/TeamAuraMusic/AuraMusic/commit/db1947a) Fix lyrics and video song handling: - Fix Rush lyrics sync by converting duration ms to seconds - Fix user lyrics selection to always respect preferred provider - Fix video song parsing in HomePage to extract musicVideoType
- [`5d92f98`](https://github.com/TeamAuraMusic/AuraMusic/commit/5d92f98) Fix lyrics provider preference: ensure selected provider is always tried first, and refetch if cached from different provider
- [`a320768`](https://github.com/TeamAuraMusic/AuraMusic/commit/a320768) fix: resolve lyrics provider conflicts, video playback in Speed Dial & Keep Listening
- [`d490dd7`](https://github.com/TeamAuraMusic/AuraMusic/commit/d490dd7) fix: replace AutoMigration(32 33) with manual migration for SpeedDialItem musicVideoType column
- [`6dfcd29`](https://github.com/TeamAuraMusic/AuraMusic/commit/6dfcd29) fix: convert 31-32 and 32-33 DB migrations to manual (no 32.json schema exists)
- [`3775ce1`](https://github.com/TeamAuraMusic/AuraMusic/commit/3775ce1) fix: register DB migrations in Hilt DI module to prevent crash on upgrade
- [`68362c0`](https://github.com/TeamAuraMusic/AuraMusic/commit/68362c0) Update tinypinyin version to 2.0.1
- [`91ab496`](https://github.com/TeamAuraMusic/AuraMusic/commit/91ab496) fix: use AutoMigration for 32->33 instead of manual migration
- [`30c72e7`](https://github.com/TeamAuraMusic/AuraMusic/commit/30c72e7) fix: add schema 32.json for auto-migration
- [`6efd681`](https://github.com/TeamAuraMusic/AuraMusic/commit/6efd681) fix: remove MIGRATION_32_33 reference from AppModule
- [`93d1123`](https://github.com/TeamAuraMusic/AuraMusic/commit/93d1123) fix: use IF NOT EXISTS to avoid duplicate column error
- [`d62a3ae`](https://github.com/TeamAuraMusic/AuraMusic/commit/d62a3ae) fix: check column existence before adding
- [`96ed83d`](https://github.com/TeamAuraMusic/AuraMusic/commit/96ed83d) fix: align slider styles in appearance settings and reorganize about screen

---

# AuraMusic v1.0.15 (Build 16) Release Notes

## What's New
- Fixed lyrics provider priority not being respected when user sets provider order
- Improved HomeScreen performance with optimized key parameters
- Fixed duplicate key crash in Moods & Genres section
- This release focuses on fixing known issues and adding new features

### Lyrics Provider Improvements
- Fixed issue where user-set provider priority was not being respected
- Provider order now properly saves and loads from preferences
- Fallback to preferred provider logic works correctly when custom order is not set
- Added proper check for customized provider order vs default order

### Performance Improvements
- Optimized HomeScreen with key parameters to prevent recomposition
- Added derivedStateOf for expensive calculations in LazyGrids
- Improved list rendering performance

### Bug Fixes
- Fixed duplicate key crash in Moods & Genres grid by adding title to item key
- Fixed not being able to save and load provider priority order
- Fixed RushLyrics not showing when set as first priority provider

### Full Changelog (Commits since last release):
- [`37a2eee`](https://github.com/TeamAuraMusic/AuraMusic/commit/37a2eee) Fix: lyrics provider priority not respected and duplicate key crash
- [`a91a918`](https://github.com/TeamAuraMusic/AuraMusic/commit/a91a918) Perf: optimize HomeScreen with key parameters and derivedStateOf
- [`10c6818`](https://github.com/TeamAuraMusic/AuraMusic/commit/10c6818) feat: add playing indicator bars to community playlist thumbnails

---

# AuraMusic v1.0.14 (Build 15) Release Notes

                               ## What's New
      -New real-time audio visualizer with wave animations
      -Improved Listen Together experience and navigation placement
      -More reliable subtitles and caption handling with language preference support
      -Lyrics timing fixes and synchronization improvements
      -Enhanced video playback controls and layout

### Audio Visualizer
> To use this feature enable microphone for the app in the settings if it's not already enabled so that you don't experience crashes

This release introduces real-time audio visualization:
- Added AudioVisualizerView using the Android Visualizer API for live wave rendering
- Rewrote AudioVisualizerSlider with an ocean wave style replacing the traditional progress bar
- Implemented Liquid (Samsung-inspired) notification bar wave slider
- Improved smoothness and visual quality of wave animations

### Listen Together
- Added setting to place Listen Together at the top of the navigation bar
- Added Listen Together card to the Home screen
- Removed redundant top app bar icon and updated setting labe

### Subtitles and Captions
- Added subtitle language preference in player settings
- Enabled captions by default in video mode using VideoLyricsOverlay
- Improved caption fetching reliability with proper request headers
- Added MOBILE/ANDROID client fallback for better subtitle availability
Lyrics
- Improved detection and correction of malformed timestamps in RushLyrics
- Fixed invalid timestamp handling and generated proper line timing
- Resolved issue where all lyrics appeared highlighted

### Video Playback
- Added Fixed width (FIXED_WIDTH) video scaling option
- Improved caption positioning in video mode
- Added caption loading status indicator when unavailable

### Bug Fixes
- Fixed compilation issues in core components
- Resolved duplicate declarations and redundant logic
- Fixed animation easing issues and stability improvements

### Technical Details
**Full Changelog**: https://github.com/TeamAuraMusic/AuraMusic/compare/v1.0.13...v1.0.14

### Build Information
- Version: 1.0.14
- VersionCode: 15

 

---

Full Changelog: https://github.com/TeamAuraMusic/AuraMusic/compare/v1.0.13...v1.0.14

## What's New in This Update

### Native Video Subtitles
This release brings native subtitle rendering to AuraMusic:

1. **Subtitle Rendering**
   - Implemented native ExoPlayer subtitle rendering using PlayerView
   - Fetch YouTube caption tracks automatically when switching to video mode
   - Convert captions to VTT format for compatibility
   - CC button to toggle subtitles on/off

2. **F-Droid Compatibility**
   - Removed Google ML Kit dependency (LanguageDetectionHelper)
   - Added Fastlane metadata for F-Droid submission
   - Fixed workflow YAML indentation

### Liquid Glass Effect Improvements
- Fixed liquid glass effect in dark mode with pure black theme
- Updated appearance settings to show proper toggle UI
- Liquid glass now works correctly in all theme modes

### Video Playback Improvements
- Video songs now start at 0:00 position
- Regular songs preserve current position when switching to video
- Parallel fetching of captions and stream URL for faster loading
- Improved video mode switching performance

### Bug Fixes
- Fixed numerous build errors and compilation issues
- Fixed missing imports for MusicService constants
- Fixed MediaLibrarySessionCallback constant references
- Fixed subtitle track selection
- Fixed caption fetching reliability
- Fixed video autoplay and thumbnail layout issues

---

## Technical Details

### Full Changelog (Commits since last release):
- [`ecd0c54`](https://github.com/TeamAuraMusic/AuraMusic/commit/ecd0c54) Fix: add SEARCH constant import, remove setRendererDisabled
- [`3e14d49`](https://github.com/TeamAuraMusic/AuraMusic/commit/3e14d49) Fix: add missing imports for MusicService constants
- [`2b5df07`](https://github.com/TeamAuraMusic/AuraMusic/commit/2b5df07) Revert: revert optimization changes to fix build
- [`8a4bce2`](https://github.com/TeamAuraMusic/AuraMusic/commit/8a4bce2) Fix: remove unavailable setRendererDisabled method
- [`448cef1`](https://github.com/TeamAuraMusic/AuraMusic/commit/448cef1) Optimize video switching: parallel fetch + start at 0:00 for video songs
- [`ab8fe31`](https://github.com/TeamAuraMusic/AuraMusic/commit/ab8fe31) Fix liquid glass dark mode + improve subtitle selection
- [`5a23693`](https://github.com/TeamAuraMusic/AuraMusic/commit/5a23693) Fix: remove unused SubtitleManager import
- [`202d5ed`](https://github.com/TeamAuraMusic/AuraMusic/commit/202d5ed) F-Droid compatibility: remove ML Kit, add Fastlane metadata
- [`57f9bbf`](https://github.com/TeamAuraMusic/AuraMusic/commit/57f9bbf) Fix workflow: fix YAML indentation issue
- [`fbc250e`](https://github.com/TeamAuraMusic/AuraMusic/commit/fbc250e) Fix build errors: remove SubtitleManager, fix builder chaining
- [`5891215`](https://github.com/TeamAuraMusic/AuraMusic/commit/5891215) Add subtitle support: fetch YouTube captions and attach to video media items
- [`7ff2d54`](https://github.com/TeamAuraMusic/AuraMusic/commit/7ff2d54) Fix: add missing LyricsHelper import
- [`b13c72a`](https://github.com/TeamAuraMusic/AuraMusic/commit/b13c72a) Implement native video subtitles
- [`7aea6f0`](https://github.com/TeamAuraMusic/AuraMusic/commit/7aea6f0) Fix: add missing C import for INDEX_UNSET
- [`44769a3`](https://github.com/TeamAuraMusic/AuraMusic/commit/44769a3) Fix build: use native PlayerView subtitle rendering
- [`f0a9d9b`](https://github.com/TeamAuraMusic/AuraMusic/commit/f0a9d9b) Implement native ExoPlayer subtitle rendering like SmartTube

### Build Update
- Version: 1.0.13 (Build 14)
- VersionCode: 14

---

Full Changelog: https://github.com/TeamAuraMusic/AuraMusic/compare/v1.0.12...v1.0.13
