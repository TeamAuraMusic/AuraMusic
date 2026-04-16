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
