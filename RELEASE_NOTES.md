# AuraMusic v1.0.14 (Build 15) Release Notes

## What's New in This Update

### Audio Visualizer
This release introduces real-time audio visualization:
- Added AudioVisualizerView with Android Visualizer API for real-time wave visualization
- Rewrote AudioVisualizerSlider with ocean wave style that replaces progress bar
- Implemented Samsung/Liquid notification bar wave slider style
- Smooth ocean waves above progress bar with modern UI

### Listen Together Improvements
- Added Listen Together at top setting - moves Listen Together to top of nav bar when enabled
- Added Listen Together card to HomeScreen
- Removed Listen Together icon from top app bar and updated setting label

### Subtitle & Caption Improvements
- Added subtitle language preference setting in player settings
- Fixed video captions to enable VideoLyricsOverlay and auto subtitle language by default
- Use proper YouTube headers when fetching caption track content
- Use MOBILE/ANDROID client as fallback for caption tracks to improve caption availability
- Fixed caption fetching reliability with improved headers

### Lyrics Improvements
- Improved RushLyrics malformed timestamp detection and fixing
- Fixed RushLyrics malformed timestamps - generate valid line timing
- Fixed lyrics all-highlighted bug

### Video Playback Improvements
- Added Fixed (FIXED_WIDTH) option to video fit settings
- Position captions lower in video mode to show in empty space
- Show caption loading status indicator below thumbnail when captions are unavailable

### Bug Fixes
- Fixed numerous compilation errors in MainActivity, HomeScreen, AudioVisualizerView, and AppearanceSettings
- Fixed duplicate videoModeEnabled declaration
- Fixed remove redundant toFloat() calls
- Fixed use LinearEasing instead of LinearRepeatable
- Fixed missing SAMSUNG branch in when expression

---

## Technical Details

### Full Changelog (Commits since last release):
- [`de85c8e`](https://github.com/TeamAuraMusic/AuraMusic/commit/de85c8e) Fix: Position captions lower below video
- [`4229034`](https://github.com/TeamAuraMusic/AuraMusic/commit/4229034) Remove FIT mode entirely, use Fixed by default
- [`518faaa`](https://github.com/TeamAuraMusic/AuraMusic/commit/518faaa) Add Fixed (FIXED_WIDTH) option to video fit settings
- [`dc72a10`](https://github.com/TeamAuraMusic/AuraMusic/commit/dc72a10) Fix: Use FIXED_WIDTH mode for video to fill thumbnail like HeroCarousel
- [`0cbb81d`](https://github.com/TeamAuraMusic/AuraMusic/commit/0cbb81d) Revert: Keep FIT mode for video to preserve quality
- [`ec7defe`](https://github.com/TeamAuraMusic/AuraMusic/commit/ec7defe) Fix: Use ZOOM resize mode for video to fill thumbnail like HeroCarousel
- [`c6ab1ce`](https://github.com/TeamAuraMusic/AuraMusic/commit/c6ab1ce) Fix: Remove dynamic icon restart logic that causes app crash
- [`68356cb`](https://github.com/TeamAuraMusic/AuraMusic/commit/68356cb) Remove Listen Together icon from top app bar and update setting label
- [`544920f`](https://github.com/TeamAuraMusic/AuraMusic/commit/544920f) Fix: Remove redundant toFloat calls and unused Elvis operator in AudioVisualizerView
- [`b9aa141`](https://github.com/TeamAuraMusic/AuraMusic/commit/b9aa141) Fix: Compilation errors in MainActivity and HomeScreen
- [`f457051`](https://github.com/TeamAuraMusic/AuraMusic/commit/f457051) Fix: Listen Together in top bar when enabled, in bottom nav when disabled
- [`6c049f0`](https://github.com/TeamAuraMusic/AuraMusic/commit/6c049f0) Add Listen Together card to HomeScreen and remove New Releases icon from top bar
- [`4fb64c0`](https://github.com/TeamAuraMusic/AuraMusic/commit/4fb64c0) Add Listen Together at top setting - moves Listen Together to top of nav bar when enabled
- [`d0a63cd`](https://github.com/TeamAuraMusic/AuraMusic/commit/d0a63cd) Fix: Pass SongItem metadata with isVideoSong flag to enable video mode for trending carousel
- [`5fe6b22`](https://github.com/TeamAuraMusic/AuraMusic/commit/5fe6b22) Restore tinypinyin to 2.0.3
- [`608e96c`](https://github.com/TeamAuraMusic/AuraMusic/commit/608e96c) Add Gradle 9.4.1 SHA256 checksum
- [`de70499`](https://github.com/TeamAuraMusic/AuraMusic/commit/de70499) Update Gradle wrapper to 9.4.1
- [`148165c`](https://github.com/TeamAuraMusic/AuraMusic/commit/148165c) Downgrade tinypinyin to 2.0.1 for build compatibility
- [`2e4749f`](https://github.com/TeamAuraMusic/AuraMusic/commit/2e4749f) Fix: Show loading indicator during video buffering for faster perceived loading
- [`b6866a5`](https://github.com/TeamAuraMusic/AuraMusic/commit/b6866a5) Fix: Position captions lower in FIT video mode to show in empty space
- [`99c7377`](https://github.com/TeamAuraMusic/AuraMusic/commit/99c7377) Fix: Make ocean waves larger and more visible
- [`f461141`](https://github.com/TeamAuraMusic/AuraMusic/commit/f461141) Fix: Smooth ocean waves above progress bar with modern UI
- [`9998f8b`](https://github.com/TeamAuraMusic/AuraMusic/commit/9998f8b) Fix: Smooth ocean waves without straight line buzz
- [`e622e71`](https://github.com/TeamAuraMusic/AuraMusic/commit/e622e71) Fix: Add missing AudioVisualizerPreview composable
- [`7b98494`](https://github.com/TeamAuraMusic/AuraMusic/commit/7b98494) Rewrite AudioVisualizerSlider with ocean wave style that replaces progress bar
- [`b47af7b`](https://github.com/TeamAuraMusic/AuraMusic/commit/b47af7b) Fix: Remove redundant toFloat() calls in AudioVisualizerView
- [`679ff2f`](https://github.com/TeamAuraMusic/AuraMusic/commit/679ff2f) Fix: Various compilation errors in AudioVisualizerView and AppearanceSettings
- [`1925b4e`](https://github.com/TeamAuraMusic/AuraMusic/commit/1925b4e) Add AudioVisualizerView with Android Visualizer API for real-time wave visualization
- [`811fc42`](https://github.com/TeamAuraMusic/AuraMusic/commit/811fc42) Rename Samsung slider style to Liquid
- [`b02a9d0`](https://github.com/TeamAuraMusic/AuraMusic/commit/b02a9d0) Fix: Remove redundant toFloat() calls in SamsungSlider
- [`435cd90`](https://github.com/TeamAuraMusic/AuraMusic/commit/435cd90) Fix: Use LinearEasing instead of LinearRepeatable
- [`e0cf4e9`](https://github.com/TeamAuraMusic/AuraMusic/commit/e0cf4e9) Fix: Implement Samsung notification bar wave slider style and remove auto-reordering of lyrics providers
- [`a58d4dc`](https://github.com/TeamAuraMusic/AuraMusic/commit/a58d4dc) Fix: Add missing SAMSUNG branch in when expression
- [`ffb44f6`](https://github.com/TeamAuraMusic/AuraMusic/commit/ffb44f6) Add SamsungSlider component and various fixes for player, settings, and lyrics
- [`2c1f547`](https://github.com/TeamAuraMusic/AuraMusic/commit/2c1f547) fix: lyrics all-highlighted bug, caption re-fetching, and liquid glass in dark mode
- [`14f715f`](https://github.com/TeamAuraMusic/AuraMusic/commit/14f715f) Improve RushLyrics malformed timestamp detection and fixing
- [`6ade99f`](https://github.com/TeamAuraMusic/AuraMusic/commit/6ade99f) Fix RushLyrics malformed timestamps - generate valid line timing
- [`ac1e268`](https://github.com/TeamAuraMusic/AuraMusic/commit/ac1e268) Fix RushLyrics invalid timestamp handling
- [`7ebb0a0`](https://github.com/TeamAuraMusic/AuraMusic/commit/7ebb0a0) Fix: Respect user subtitle preference for video mode
- [`e343b6e`](https://github.com/TeamAuraMusic/AuraMusic/commit/e343b6e) Check if captions/subtitles are enabled before fetching
- [`4881863`](https://github.com/TeamAuraMusic/AuraMusic/commit/4881863) Add more debug logging for caption fetching in MusicService
- [`11486b8`](https://github.com/TeamAuraMusic/AuraMusic/commit/11486b8) Add debug logging to caption fetching to diagnose HTML error
- [`7b3ddd5`](https://github.com/TeamAuraMusic/AuraMusic/commit/7b3ddd5) Improve caption fetching: use Android UA, X-Origin header, and add more headers for better compatibility
- [`3c63697`](https://github.com/TeamAuraMusic/AuraMusic/commit/3c63697) Fix caption subtitle fetching: use youtube.com origin/headers instead of music.youtube.com
- [`ccabc63`](https://github.com/TeamAuraMusic/AuraMusic/commit/ccabc63) Fix: use proper YouTube headers when fetching caption track content
- [`26057e8`](https://github.com/TeamAuraMusic/AuraMusic/commit/26057e8) Fix: handle caption track URLs that may not have proper domain
- [`c5a9a0d`](https://github.com/TeamAuraMusic/AuraMusic/commit/c5a9a0d) Add MOBILE/ANDROID client as fallback for caption tracks to improve caption availability
- [`8389273`](https://github.com/TeamAuraMusic/AuraMusic/commit/8389273) Fix: run Toast on Main dispatcher to avoid crash in background thread
- [`acfd742`](https://github.com/TeamAuraMusic/AuraMusic/commit/acfd742) Add Toast messages to show caption fetch results; fix type issues
- [`c444240`](https://github.com/TeamAuraMusic/AuraMusic/commit/c444240) Fix: explicitly type videoId as String to resolve nullable type mismatch
- [`230cc10`](https://github.com/TeamAuraMusic/AuraMusic/commit/230cc10) Fix: remove duplicate videoModeEnabled declaration and use early return check
- [`3acdeb6`](https://github.com/TeamAuraMusic/AuraMusic/commit/3acdeb6) Fix: ensure video mode is enabled before fetching captions
- [`20728ab`](https://github.com/TeamAuraMusic/AuraMusic/commit/20728ab) Fix: properly track attempted video IDs to prevent re-fetching on player collapse/expand
- [`fa099d4`](https://github.com/TeamAuraMusic/AuraMusic/commit/fa099d4) Fix VideoLyricsOverlay: cache captions per video ID to avoid reloading on player expand/collapse
- [`cc723d3`](https://github.com/TeamAuraMusic/AuraMusic/commit/cc723d3) Fix duplicate videoModeEnabled declaration in VideoLyricsOverlay
- [`94690a1`](https://github.com/TeamAuraMusic/AuraMusic/commit/94690a1) Fix video captions: enable VideoLyricsOverlay and auto subtitle language by default
- [`aea3945`](https://github.com/TeamAuraMusic/AuraMusic/commit/aea3945) Fix: Remove duplicate orphaned code from Thumbnail.kt
- [`be28e3f`](https://github.com/TeamAuraMusic/AuraMusic/commit/be28e3f) Fix: Remove orphaned captionError code from Thumbnail.kt
- [`e2714df`](https://github.com/TeamAuraMusic/AuraMusic/commit/e2714df) Make caption fetching react to language preference changes
- [`602e971`](https://github.com/TeamAuraMusic/AuraMusic/commit/602e971) Show caption loading status indicator below thumbnail when captions are unavailable
- [`5c1a96c`](https://github.com/TeamAuraMusic/AuraMusic/commit/5c1a96c) Add @OptIn for experimental Material3 Expressive API in VideoLyricsOverlay
- [`90c0b8c`](https://github.com/TeamAuraMusic/AuraMusic/commit/90c0b8c) Restore Thumbnail.kt with video duration and loading indicator changes
- [`8c8a4c7`](https://github.com/TeamAuraMusic/AuraMusic/commit/8c8a4c7) Fix: remove title param from ListDialog, add OptIn for ExperimentalMaterial3Api
- [`4d1c04e`](https://github.com/TeamAuraMusic/AuraMusic/commit/4d1c04e) Fix: rename getCaptionTracks to getCaptionTracksWithDuration to avoid overload conflict
- [`2da39a9`](https://github.com/TeamAuraMusic/AuraMusic/commit/2da39a9) Add subtitle language preference setting in player settings
- [`311eb8d`](https://github.com/TeamAuraMusic/AuraMusic/commit/311eb8d) Pass video duration to VTT conversion for last cue timing
- [`77b3928`](https://github.com/TeamAuraMusic/AuraMusic/commit/77b3928) Merge branch 'main' of https://github.com/TeamAuraMusic/AuraMusic Fix error when pushing changes to GitHub
- [`fdc95f1`](https://github.com/TeamAuraMusic/AuraMusic/commit/fdc95f1) fix: use native ExoPlayer subtitle rendering for video captions
- [`095f3dc`](https://github.com/TeamAuraMusic/AuraMusic/commit/095f3dc) Fix typo in changelog regarding video songs
- [`5ea6a9e`](https://github.com/TeamAuraMusic/AuraMusic/commit/5ea6a9e) fix: improve subtitle rendering with cached content and proper parsing
- [`ca0515d`](https://github.com/TeamAuraMusic/AuraMusic/commit/ca0515d) Fix: use empty format to parse timed text (not vtt)
- [`5b11c36`](https://github.com/TeamAuraMusic/AuraMusic/commit/5b11c36) Fix: revert imports to Companion.object syntax, fix return statement
- [`300f22d`](https://github.com/TeamAuraMusic/AuraMusic/commit/300f22d) Refactor: move MusicService constants to companion object, fix subtitle handler logic, remove unused imports
- [`0ee50d5`](https://github.com/TeamAuraMusic/AuraMusic/commit/0ee50d5) Add Timber logging dependency to innertube module

### Build Update
- Version: 1.0.14 (Build 15)
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
