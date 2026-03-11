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
- Updated repository URL from `chila254/Auramusic-v1` to `chila254/AuraMusic`
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

**Full Changelog**: https://github.com/chila254/AuraMusic/compare/v1.0.5...v1.0.6

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

**Full Changelog**: https://github.com/chila254/AuraMusic/compare/v1.0.4...v1.0.5
