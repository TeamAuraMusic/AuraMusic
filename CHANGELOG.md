# AuraMusic v2.0.0 (Build 17) Changelog

## Features
- Added liquid glass customization options (blur radius, corner radius, opacity) in Appearance Settings
- Added Discord and Telegram links to About screen
- Added 4-dot shuffle button with animations to speed dial
- Added playing indicator in center of SpeedDialGridItem
- Updated README with socials section (Discord, Telegram)

## UI/UX Improvements
- Improved shuffle button loading indicator size and synchronization with isPlaying
- Track loaded song ID and stop loading when mediaMetadata matches
- Removed unnecessary video toast message after successful load
- Fixed video fit mode persistence across app restorts
- Reorganized About screen layout with updated sliders

## Video Playback Improvements
- Improved video loading speed with sequential subtitle fetching
- Added auto-play on first frame
- Fixed video song parsing in HomePage to extract musicVideoType

## Lyrics Improvements
- Fixed Rush lyrics sync by converting duration ms to seconds
- Fixed user lyrics selection to always respect preferred provider
- Refetch lyrics if cached from different provider
- Fixed lyrics provider conflicts and video playback in Speed Dial & Keep Listening

## Bug Fixes
- Fixed SpeedDialGridItem compile error
- Fixed missing setValue import for var delegation in HomeScreen
- Fixed duplicate column error with IF NOT EXISTS and column existence checks
- Fixed database migrations for seamless upgrades
- Fixed Discord and Telegram logo URLs in README

## Build
- Bumped versionCode to 17
- Version: 2.0.0
- Updated tinypinyin version to 2.0.1

---

**Full Changelog**: https://github.com/TeamAuraMusic/AuraMusic/compare/v1.0.15...v2.0.0


# AuraMusic v1.0.15 (Build 16) Changelog

## Features
- Fixed lyrics provider priority not being respected when user sets provider order
- Added proper check for customized provider order vs default order

## Performance Improvements
- Optimized HomeScreen with key parameters to prevent unnecessary recomposition
- Added derivedStateOf for expensive calculations in LazyGrids
- Improved LazyGrid list rendering performance

## Bug Fixes
- Fixed duplicate key crash in Moods & Genres grid by using unique keys
- Fixed provider priority not being saved and loaded from preferences
- Fixed RushLyrics not showing when set as first priority provider

## Build
- Bumped versionCode to 16
- Version: 1.0.15

---

# AuraMusic v1.0.14 (Build 15) Changelog

## Features
- Added AudioVisualizerView with Android Visualizer API for real-time wave visualization
- Added SamsungSlider component with wave style
- Added Listen Together at top setting - moves Listen Together to top of nav bar when enabled
- Added Listen Together card to HomeScreen
- Added subtitle language preference setting in player settings
- Added Fixed (FIXED_WIDTH) option to video fit settings
- Renamed Samsung slider style to Liquid

## UI/UX Improvements
- Rewrote AudioVisualizerSlider with ocean wave style that replaces progress bar
- Implemented Samsung notification bar wave slider style
- Fixed liquid glass effect in dark mode
- Removed Listen Together icon from top app bar and updated setting label
- Position captions lower in video mode to show in empty space
- Show caption loading status indicator below thumbnail when captions are unavailable

## Video Playback Improvements
- Fixed video mode switching with improved caption fetching reliability
- Fixed video captions to enable VideoLyricsOverlay and auto subtitle language by default
- Fixed video mode is enabled before fetching captions
- Fixed video captions to cache captions per video ID to avoid reloading on player expand/collapse
- Fixed handle caption track URLs that may not have proper domain
- Use proper YouTube headers when fetching caption track content
- Use MOBILE/ANDROID client as fallback for caption tracks to improve caption availability

## Lyrics Improvements
- Improved RushLyrics malformed timestamp detection and fixing
- Fixed RushLyrics malformed timestamps - generate valid line timing
- Fixed RushLyrics invalid timestamp handling
- Fixed lyrics all-highlighted bug
- Caption re-fetching improvements
- Removed auto-reordering of lyrics providers

## Bug Fixes
- Fixed numerous compilation errors in MainActivity, HomeScreen, AudioVisualizerView, and AppearanceSettings
- Fixed duplicate videoModeEnabled declaration
- Fixed remove duplicate videoModeEnabled declaration in VideoLyricsOverlay
- Fixed explicitly type videoId as String to resolve nullable type mismatch
- Fixed remove redundant toFloat() calls in AudioVisualizerView
- Fixed use LinearEasing instead of LinearRepeatable
- Fixed missing SAMSUNG branch in when expression
- Fixed Pass SongItem metadata with isVideoSong flag to enable video mode for trending carousel
- Fixed compilation errors in MainActivity and HomeScreen
- Fixed show loading indicator during video buffering for faster perceived loading

## Build
- Bumped versionCode to 15
- Bumped versionName to 1.0.14
- Updated Gradle wrapper to 9.4.1
- Added Gradle 9.4.1 SHA256 checksum
- Restored tinypinyin to 2.0.3 for build compatibility


**Full Changelog**: https://github.com/TeamAuraMusic/AuraMusic/compare/v1.0.13...v1.0.14


# AuraMusic v1.0.13 (Build 14) Changelog

## Features
- Implemented native ExoPlayer subtitle rendering using PlayerView
- Added YouTube caption track fetching with VTT conversion
- Added CC button to toggle subtitles on/off
- Added Fastlane metadata for F-Droid submission
- Added liquid glass effect setting in appearance settings
- Added video subtitles toggle in player controls

## F-Droid Compatibility
- Removed Google ML Kit dependency (LanguageDetectionHelper)
- Fixed workflow YAML indentation
- Added short_description.txt and full_description.txt
- Added changelogs for F-Droid submission

## UI/UX Improvements
- Fixed liquid glass effect in dark mode with pure black theme
- Updated appearance settings toggle UI for liquid glass
- Liquid glass now works correctly in all theme modes

## Video Playback Improvements
- Video songs now start at 0:00 position
- Video songs preserve current position when switching to video
- Parallel fetching of captions and stream URL for faster loading
- Improved video mode switching performance

## Bug Fixes
- Fixed numerous build errors and compilation issues
- Fixed missing imports for MusicService constants
- Fixed MediaLibrarySessionCallback constant references (ROOT, SONG, ARTIST, ALBUM, PLAYLIST, YOUTUBE_PLAYLIST, SHUFFLE_ACTION, SEARCH)
- Fixed subtitle track selection method
- Fixed caption fetching reliability
- Fixed video autoplay and thumbnail layout issues
- Fixed caption visibility in fullscreen video mode
- Fixed resume video playback when player screen is not visible

## Build
- Bumped versionCode to 14
- Bumped versionName to 1.0.13


**Full Changelog**: https://github.com/TeamAuraMusic/AuraMusic/compare/v1.0.12...v1.0.13


# AuraMusic v1.0.12 (Build 13) Changelog

## Features
- Added Hero Carousel banner to Home Screen
- Added "Trending Now" header with carousel on Home Screen
- Added thumbnail cropping on small screens for carousel
- Added shimmer placeholder for carousel loading
- Added title and artist below thumbnail instead of overlay
- Added full-cover carousel thumbnails
- Added build type display in About screen
- Animated About screen icon
- Improved video lyrics sync timing
- Fixed video autoplay timing

## UI/UX Improvements
- Moved carousel text below thumbnail for better readability
- Improved PayPal icon/ logo
- Removed video fill mode for cleaner UI
- Made hero carousel responsive for tablets and small screens
- Increased carousel heights for better visibility
- Fixed carousel thumbnail fit (ContentScale.Fit)

## Bug Fixes
- Fixed Explore screen not displaying mixes, podcasts, or albums
- Fixed duplicate "Music Videos for You" sections
- Fixed missing import for toMediaMetadata in YouTube grid items
- Fixed incorrect import (androidx.compose.ui.layout.aspectRatio → androidx.compose.foundation.layout.aspectRatio)
- Fixed video mode autoplay issues

## Build
- Bumped versionCode to 13
- Bumped versionName to 1.0.12


**Full Changelog**: https://github.com/TeamAuraMusic/AuraMusic/compare/v1.0.11...v1.0.12


# AuraMusic v1.0.11 (Build 12) Changelog

## Features
- Added podcasts and episodes support
- Added Top 100 charts with extended sections
- Improved video mode with auto-enable and simplified UI
- Remove video mode for Regular Songs 

## Improvements
- Enhanced About screen (icon, tablet layout, animations)
- Updated Explore, Search, and Top Charts UI
- Improved icon and drawable handling

## Fixes
- Fixed compilation errors across multiple screens
- Fixed PayPal donation link behavior
- Fixed video mode syntax issues
- Fixed exhaustive when expression errors
- Fixed deprecated API usage (HiltViewModel)
- Fixed navigation and scaffold issues
- Fixed LocalPlayerConnection reference issues

## Performance
- Improved app stability and reduced crashes
- Optimized memory and resource usage

## CI/CD
- Added GitHub Actions workflow for automated builds
- Fixed APK output path and detection
- Fixed keystore decoding and signing
- Updated Gradle configuration and repositories

## Build
- Bumped versionCode to 12
- Bumped versionName to 1.0.11


**Full Changelog**: https://github.com/TeamAuraMusic/AuraMusic/compare/v1.0.10...v1.0.11


# AuraMusic v1.0.10 (Build 11) Changelog

## New Features

### Video Player Improvements
- **Video Switching Loading Indicator**: Added smooth loading animation while video is being fetched
- **Improved Video Lyrics Sync**: Reduced polling interval from 150ms to 50ms for perfectly synced lyrics with video playback
- **Music Video Search Algorithm Overhaul**: Completely rewritten video search with much higher accuracy
  - Normalized title comparison with automatic bracketed content stripping
  - Artist token matching for more reliable artist detection
  - Multi-query search with cross-query result comparison
  - Expanded exclusion list for non-official videos (karaoke, sped up, slowed, nightcore, etc.)
  - Early exit for high-confidence matches
  - Minimum confidence threshold for more reliable results

### General Improvements
- **Updater**: Added automatic redirect following for GitHub API requests

## Changes
- Repository moved to Team AuraMusic organization: https://github.com/TeamAuraMusic/AuraMusic
- All repository URLs updated across entire codebase (settings, API, Discord RPC, README, etc.)
- Build version bump: 1.0.9 (Build 10) → 1.0.10 (Build 11)

## Bug Fixes
- Fixed black screen flash when switching between audio/video modes
- Fixed lyrics offset not being properly applied in video mode
- Fixed video background during loading state

---

**Full Changelog**: https://github.com/TeamAuraMusic/AuraMusic/compare/v1.0.9...v1.0.10

---

# AuraMusic v1.0.9 (Build 10) Changelog

## New Features

### Complete Video Player Overhaul
- **Animated Lyrics**: Lyrics slide up and fade in smoothly with transitions when changing lines
- **Next Lyric Preview**: Dimmed smaller text shows the upcoming line below the current lyric
- **Lyrics Glow Effect**: Double-render with primary color glow shadow for maximum readability
- **Auto-hide Controls**: Settings button fades out after 3s, tap video to toggle
- **Double-tap Seek**: Expanding circle ripple animation + arrow icon on seek
- **Video Fit Mode Selector**: Fit/Fill/Stretch options in YouTube-style settings menu
- **Progress Gradient Bar**: Thin animated gradient bar at the top of the video
- **Brightness/Volume Gestures**: Swipe left side for brightness, right side for volume with vertical indicator

### Music Video Improvements
- **Regular song video fallback**: All songs can now play music videos automatically
- **⚠️ Note**: Some songs might show other videos - we are working on improving matching accuracy
- **Video quality selector directly on thumbnail**: No more going through settings menus
- **Improved video search matching**: Better filtering and scoring for official music videos

## Bug Fixes

- Fixed duplicate lyrics showing (removed small text lyrics when video is playing)
- Fixed lyrics sync issues in video mode
- Fixed quality selection algorithm to properly respect user preferences
- Fixed video not filling properly on different screen sizes
- Fixed lyrics offset calculation direction

## Build Updates

- Version bump: 1.0.8 (Build 9) → 1.0.9 (Build 10)
- Repository moved to Team AuraMusic organization: https://github.com/TeamAuraMusic/AuraMusic

---

**Full Changelog**: https://github.com/Team-AuraMusic/AuraMusic/compare/v1.0.8...v1.0.9

---

# AuraMusic v1.0.8 (Build 9) Changelog

## New Features

### Video Mode - Official Music Video Search
- **Smart Video Fallback**: When video mode is enabled for regular songs (non-video songs), the app now automatically searches YouTube for the official music video
- Uses "{song title} {artist} official music video" search query to find the best match
- Prioritizes official music videos, Vevo, "MV" tagged videos, and videos containing the song title
- Falls back to the first search result if no preferred match is found
- Enabled by default for new installations
- Marked as "Experimental" in Settings

### Video Mode UI Improvements
- Added video toggle icon in the player UI
- Better error handling with user-friendly toast messages when video is unavailable
- Improved black screen issue - video mode now properly falls back to audio on error
- Fixed video playback detection for better stream selection

### Video Quality Selection
- Added video quality selection option in Player Settings (360p/480p/720p/1080p)
- Quality preference is saved and applied automatically when video mode is enabled
- Smart fallback: if selected quality is not available, automatically uses the next available quality

### Listen Together Updates
- Now uses api.auramusic.site for Listen Together functionality

### Settings Improvements
- Added website link in About settings: auramusic.site

## Bug Fixes

- Fixed compile errors related to duplicate video result handling
- Fixed black screen flicker issue by preventing auto-reset on playback errors
- Improved video URL extraction and MIME type handling
- Fixed "Respect Lyrics Provider" setting to properly apply the user's preference
- Video now properly fills the entire player area in fullscreen mode
- Improved video quality selection to prioritize actual resolution (height) over bitrate

## Build Updates

- Version bump: 1.0.7 (Build 8) → 1.0.8 (Build 9)

---

**Full Changelog**: https://github.com/TeamAuraMusic/AuraMusic/compare/v1.0.7...v1.0.8

---

# AuraMusic v1.0.7 (Build 8) Changelog

## New Features

### AuraMusic Branding Update
- Updated all internal references from the previous branding to AuraMusic
- Changed "It seems like you found [previous app name] recently..." to "It seems like you found AuraMusic recently..." in WrappedData.kt

## Bug Fixes

- **Fixed % Display Issues in Wrapped**: Resolved an issue where percentage symbols were displaying literally instead of actual numbers in wrapped statistics screens:
  - WrappedTotalSongsScreen.kt - Added missing uniqueSongCount parameter to stringResource()
  - WrappedTotalArtistsScreen.kt - Added missing uniqueArtistCount parameter to stringResource()
  - AlbumPages.kt - Added missing uniqueAlbumCount parameter to stringResource()

- **Fixed Total Songs Not Showing in Wrapped Playlist**: Resolved an issue where the wrapped playlist was showing incorrect or zero song count. The root cause was a date mismatch between the playlist creation (hardcoded year from WrappedConstants.YEAR) and the dynamic date range used in data preparation:
  - Updated createPlaylist() method in WrappedManager.kt to use the same dynamic date range as the prepare() method
  - Updated generatePlaylistMap() method in WrappedManager.kt to use the same dynamic date range

## Build Updates

- Updated Java version to 17 for better compatibility
- Version bump: 1.0.6 → 1.0.7 (Build 8)

---

**Full Changelog**: https://github.com/TeamAuraMusic/AuraMusic/compare/v1.0.6...v1.0.7

---

# AuraMusic v1.0.6 (Build 7) Changelog

## New Features

### Music Recognition (Shazam)
- **Fixed SSL/TLS Recognition Error**: Resolved the "Recognition error: Domain specific configurations require that hostname aware checkServerTrusted" issue
- Switched Shazam HTTP client from CIO engine to OkHttp engine for better SSL/TLS handling
- Added pure Kotlin fallback for Shazam signature generation (VibraSignature)

### New Releases Screen
- Redesigned New Releases screen to display albums in grid/card format
- Now uses `YouTubeGridItem` for better visual presentation
- Shows only albums tab (simplified from songs/videos)

### Monthly Wrapped Card
- Added "Top Artist Albums" feature to the Wrapped card
- Displays all unique albums listened to from your #1 most played artist
- New screen shows horizontal scrollable album list with cover art, title, and year

### Repository Update
- Updated repository URL from `chila254/Auramusic-v1` to `TeamAuraMusic/AuraMusic`
- Updated all internal links and references:
  - Settings > About screen GitHub link
  - Updater (GitHub API base)
  - Discord integration links
  - Listen Together invite links
  - OpenRouter service HTTP-Referer header

### UI Improvements
- Changed "Play on app" text to "Play on AuraMusic" in recognition screen
- Updated notification icon to use white music note design

## Bug Fixes

- Fixed SSL certificate validation in Shazam music recognition
- Fixed repository URL references throughout the app

---

## Comparison with v1.0.5

### Added in v1.0.6:
- Music recognition SSL/TLS fix
- New Releases grid layout
- Top Artist Albums in Wrapped card
- Repository URL updates (Auramusic-v1 → AuraMusic)
- UI text and icon improvements

### From v1.0.5 (carried forward):
- Listen Together server with AuraMusicServer
- Improved build system with local.properties signing

---

**Full Changelog**: https://github.com/TeamAuraMusic/AuraMusic/compare/v1.0.5...v1.0.6

---

# AuraMusic v1.0.5 (Build 6) Changelog

## New Features

### Listen Together Server Update
- Replaced metroserver with AuraMusicServer for Listen Together feature
- New server URL: `wss://auramusicserver.onrender.com/ws`
- Server operated by chila254 in Ohio (US East)
- Full protocol compatibility with the existing Listen Together feature

### Build System Improvements
- Moved all signing configurations to local.properties
- Removed hardcoded credentials from build configuration
- Improved signing config to work within Android Gradle plugin scope

## Bug Fixes

- Fixed project name typo from 'Auramusic' to 'AuraMusic'
- Fixed RushLyrics link in README
- Fixed signing config variable naming conflict

## Documentation

- Modernized README to match project structure
- Restructured README with improved layout
- Added better screenshots section
- Updated .gitignore

---

**Full Changelog**: https://github.com/TeamAuraMusic/AuraMusic/compare/v1.0.4...v1.0.5
