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
