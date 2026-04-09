# AuraMusic v1.0.12 (Build 13) Release Notes

## What's New in This Update

### Hero Carousel Redesign
This release brings major improvements to the Home Screen with a brand new carousel experience:

1. **New Carousel Layout**
   - Title and artist now displayed below thumbnail instead of overlay
   - Full-cover carousel thumbnails for better visual appeal
   - Shimmer placeholder during loading for smoother UX
   - Increased carousel heights for better visibility

2. **Trending Now Section**
   - Added "Trending Now" header with carousel on Home Screen
   - Thumbnail cropping on small screens for optimal display
   - Responsive design for tablets and small screens

### UI/UX Improvements
- Improved PayPal icon/logo across the app
- Removed video fill mode for cleaner interface
- Animated About screen icon
- Added build type display in About screen

### Video Improvements
- Fixed video autoplay timing issues
- Improved video lyrics sync timing

### Bug Fixes
- Fixed Explore screen not displaying mixes, podcasts, or albums
- Fixed duplicate "Music Videos for You" sections in HomeViewModel
- Fixed missing import for toMediaMetadata in YouTube grid item click handlers
- Fixed incorrect import (androidx.compose.ui.layout.aspectRatio → androidx.compose.foundation.layout.aspectRatio)
- Fixed video mode autoplay issues

---

## 💻 Technical Details

### Full Changelog (Commits since last release):
- [`f06b36b`](https://github.com/TeamAuraMusic/AuraMusic/commit/f06b36b) fix: explore screen content rendering
- [`f2ef8a0`](https://github.com/TeamAuraMusic/AuraMusic/commit/f2ef8a0) fix: Added the missing import com.auramusic.app.models.toMediaMetadata
- [`5b2d581`](https://github.com/TeamAuraMusic/AuraMusic/commit/5b2d581) 1. Duplicate "Music Videos for You" (HomeViewModel.kt)
- [`2a20be9`](https://github.com/TeamAuraMusic/AuraMusic/commit/2a20be9) feat: add hero carousel banner to home screen
- [`3d829c8`](https://github.com/TeamAuraMusic/AuraMusic/commit/3d829c8) fix: make hero carousel responsive for tablets and small screens
- [`258f674`](https://github.com/TeamAuraMusic/AuraMusic/commit/258f674) feat(carousel): increase heights and add shimmer placeholder
- [`c1053ff`](https://github.com/TeamAuraMusic/AuraMusic/commit/c1053ff) fix(carousel): show full thumbnail by using ContentScale.Fit
- [`12bb237`](https://github.com/TeamAuraMusic/AuraMusic/commit/12bb237) feat: clean up HeroCarousel thumbnails and fix video mode autoplay
- [`595f367`](https://github.com/TeamAuraMusic/AuraMusic/commit/595f367) Fixed - the import was androidx.compose.ui.layout.aspectRatio
- [`762c1ec`](https://github.com/TeamAuraMusic/AuraMusic/commit/762c1ec) feat(HeroCarousel): show title and artist below thumbnail instead of overlay
- [`aea4426`](https://github.com/TeamAuraMusic/AuraMusic/commit/aea4426) feat(HomeScreen): add Trending Now header and carousel thumbnail cropping on small screens
- [`e78f0d9`](https://github.com/TeamAuraMusic/AuraMusic/commit/e78f0d9) feat: animate about icon, show build type, fix PayPal icon, fix video autoplay timing, full-cover carousel thumbnails, improve video lyrics sync
- [`9e72ebf`](https://github.com/TeamAuraMusic/AuraMusic/commit/9e72ebf) fix: PayPal icon, carousel thumbnail fit, and video autoplay timing
- [`78904e2`](https://github.com/TeamAuraMusic/AuraMusic/commit/78904e2) fix: move carousel text below thumbnail, fix PayPal logo, remove video fill mode
- [`90362c7`](https://github.com/TeamAuraMusic/AuraMusic/commit/90362c7) Bump version code to 13 and version name to 1.0.12

### Build Update
- Version: 1.0.12 (Build 13)
- VersionCode: 13

---

**Full Changelog**: https://github.com/TeamAuraMusic/AuraMusic/compare/v1.0.11...v1.0.12