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
