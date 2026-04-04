# AuraMusic v1.0.9 (Build 10) Release Notes

## 🎉 What's New in This Update

### ✨ Video Player Overhaul
This release brings a complete refresh of the video player experience with YouTube-inspired design and smooth animations:

1. **Animated Lyrics with Fade Transitions**
   - Lyrics now slide up and fade in smoothly when changing lines
   - Clean animations synchronized with playback position

2. **Next Lyric Line Preview**
   - Dimmed, smaller text shows the upcoming line below the current lyric
   - Helps you follow along easily without surprises

3. **Lyrics Glow Effect**
   - Double-rendered with primary color glow shadow for maximum readability
   - Works perfectly on dark and bright backgrounds alike

4. **Auto-hide/show Controls**
   - Settings button fades out automatically after 3 seconds
   - Tap anywhere on the video to toggle controls visibility

5. **Double-tap Seek with Ripple Animation**
   - Expanding circle ripple effect when seeking
   - Arrow indicator clearly shows seek direction
   - Smooth animation feedback

6. **Video Fit Mode Selector**
   - YouTube-style Fit/Fill/Stretch options in settings menu
   - Finally fixes videos not filling properly on different screen sizes
   - Cycle modes directly from the menu with one tap

7. **Video Progress Gradient Bar**
   - Thin animated gradient bar at the very top of the video
   - Smoothly animates with playback progress
   - Uses primary + tertiary color gradient

8. **Brightness/Volume Swipe Gestures**
   - Swipe left side of video to adjust brightness
   - Swipe right side to adjust volume
   - Beautiful vertical indicator with progress bar and percentage display

### 🎵 Music Video Improvements
- **Regular songs now fetch music videos automatically** - Enable video mode for any song
- **⚠️ Known limitation**: Some songs might show other videos that are not the exact song video. We are actively working on improving matching accuracy.
- **Video quality selector now available directly on video thumbnail** - No more navigating through settings menus
- **Improved video search matching** - Better filtering and scoring to find official music videos

### 🛠️ Bug Fixes & Improvements
- Fixed lyrics sync issues in video mode
- Removed duplicate lyrics showing (no more small text + overlay lyrics
- Fixed quality selection algorithm to properly respect user preferences
- Updated repository moved to Team AuraMusic organization

---

## 💻 Technical Details

### Full Changelog (Commits since last release:
- `ce499b5` v1.0.9 (Build 10) - Full video player update
- `d9730a6` Fix overlapping buttons in video thumbnail: move cast button to left in video mode
- `38b69d9` Remove video lyrics toggle references and enhance video lyrics overlay
- `6755fc8` Add video lyrics overlay and lyrics button
- `6b901a3` Fix video quality menu not closing
- `a4e9e28` Fix exhaustive when warning in video quality selector
- `c8ff929` Add video settings overlay with quality display
- `c41e75b` Disable auto-load lyrics for videos
- `07499c2` Remove full video player, use ExoPlayer controls instead
- `4557179` Improve video search for regular songs with better filtering
- `dca7cbb` Fix video fills player area in fullscreen mode, improve quality selection
- `9cbd113` Fix apply video quality preference in MusicService
- `c34777b` Add video quality selection (360p/480p/720p/1080p)

### Repository Update
AuraMusic is now officially hosted at the Team AuraMusic organization:
**https://github.com/Team-AuraMusic/AuraMusic**

---

**Full Changelog**: https://github.com/Team-AuraMusic/AuraMusic/compare/v1.0.8...v1.0.9
