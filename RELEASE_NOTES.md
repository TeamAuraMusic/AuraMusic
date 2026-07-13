# AuraMusic v2.9.0 (Build 26) Release Notes

> [!NOTE]
> This release delivers a complete rewrite of the Discord integration using an OAuth2 PKCE flow, alongside major video playback stability fixes, SponsorBlock improvements, grouped search results, a fix for the Vosk voice crash on Android 16, and YouTube Music history sync.

> [!WARNING]
> The Discord integration is still a work in progress and has **not** been fully finished. The new OAuth2 login and rich presence are functional, but the feature remains under active development and may still experience intermittent issues (e.g. login race conditions, scope errors, or presence timeouts). We are continuing to stabilize it in upcoming releases.

## What's New

### Discord Integration — OAuth2 PKCE Rewrite
Implemented by @chila254

- feat(discord): complete rewrite of Discord integration with OAuth2 PKCE flow
- fix(discord): fix RPC connection lifecycle, applicationId, and presence deduplication
- fix(discord): send raw user token in gateway IDENTIFY so presence shows
- fix(discord): add periodic presence refresh and connection logging
- fix(discord): fix IDENTIFY token format, device ID, and image resolution
- fix(discord): fix login not reflecting authenticated state (handle onNewIntent for singleTask OAuth activity, use local readyDeferred to prevent race conditions)
- fix(discord): revert scopes to match working Metrolist implementation
- fix(discord): fix scope mismatch (openid → identify) and add detailed token exchange logging
- fix(discord): change openid scope to identify — openid causes invalid_scope error
- fix(discord): improve login display, HTTP client config, and error handling
- fix(discord): use OkHttp engine for getUserInfo to fix SSL certificate error
- fix(discord): remove ContentNegotiation plugin that caused compile error
- fix(discord): wire gateway events bus on first init — login was silently broken
- fix(discord): fix login never completing — events lost on gateway replacement
- fix(discord): add missing HEARTBEAT_ACK import
- fix(discord): fix compile errors in ExternalAssets API and KizzyRPC
- fix(discord): fix RPC connection and token handling issues
- debug(discord): add verbose logging to diagnose OAuth token exchange failure
- debug(discord): add Toast popup messages to diagnose OAuth failure without laptop
- debug(discord): add logging to trace RPC presence flow

The Discord integration has been rebuilt from the ground up around an OAuth2 PKCE login flow with a Discord gateway connection for rich presence. While the core flow works, it remains under active development — see the warning above.

### Video Playback & Stability Fixes
Implemented by @chila254

- Stop blocking video startup on subtitle loading
- Avoid duplicate video stream extraction
- Preserve real video MIME types to reduce black-screen playback
- Fall back to audio when restricted video playback fails
- fix(video): remove guard that blocked video display
- Fix mobile video mode and Discord presence reliability
- Fix Tv player controls, TV mini player, and Discord presence
- fix(discord,video,anr): stop video autoloading, reduce ANR risk

### SponsorBlock Improvements
Implemented by @chila254

- Fix SponsorBlock video segment loading
- Update SponsorBlock duration handling and empty category behavior
- fix(tv,sponsorblock): fix miniplayer size, player controls, and SponsorBlock

### Search, Library & Scrobbling
Implemented by @chila254

- feat: add grouped search results by item type
- Fix YouTube Music history sync for mobile playback
- fix(youtube-music): fix history sync and Discord OAuth2 login
- Fix Discord presence, sleep timer, and Last.fm scrobbling
- Fix Discord profile info and rich presence reliability
- Fix tablet search navigation clicks being swallowed

### Voice Recognition (Vosk) Fix
Implemented by @chila254

- fix(voice): fix Vosk crash on Android 16 by upgrading JNA to 5.19.1

### Docs & README
Implemented by @chila254

- Add Trendshift badge and logo to README
- Improve Trendshift logo visibility in README (light/dark mode)
- docs: update RELEASE_NOTES.md and CHANGELOG.md for v2.8.0 in v2.6.0 format
- docs: add interview presentation guide for GiveDirectly application (later removed)
- Delete INTERVIEW_PREP.md

## Translation Contributors

- @Franklin Chilango contributed Hindi translations via Weblate
- Turkish translation added via Weblate
- Chinese (Simplified Han script) translation updated via Weblate

**Full Changelog**: https://github.com/TeamAuraMusic/AuraMusic/compare/v2.8.0...v2.9.0

# AuraMusic v2.8.0 (Build 25) Release Notes

> [!NOTE]
> This release brings a complete Android TV redesign with Spotify-style focused detail panel, major video playback stability fixes, improved lyrics fallbacks with LyricsPlus and Genius support, and streamlined CI/CD pipeline.

## What's New

### Android TV Redesign — Spotify-Style Focused Detail Panel
Implemented by @chila254

- feat(tv): replace hero carousel with Spotify-style focused detail panel
- feat(tv): add Continue Listening row on Google TV home screen
- Modernize TV home focused detail panel with transparent top bar overlay
- Restructure home screen layout so focused panel joins top bar and metadata shows below
- Remove Recently Played row from TV home screen
- Increase focused panel height to fully cover previous row content when scrolling
- Reduce section headers (Quick Picks, Keep Listening, Similar To) for cleaner layout

The TV home screen now features a focused detail panel inspired by Spotify TV. When you navigate through content rows, the panel shows rich metadata about the focused item with the thumbnail art visible through the transparent top navigation bar.

### Android TV Navigation & Focus Improvements
Implemented by @chila254

- Separate mini player into two distinct focusable areas: song info (opens player) and play/pause button
- Fix mini player single-press activation using Surface(onClick) instead of separate focusable+clickable
- Add proper D-pad navigation throughout all TV screens
- Push focused panel content below the nav bar with proper top padding on all screens (Library, Search, Settings, Artist, Album, Playlist, sub-settings)
- Improve login screen D-pad navigation to handle all focusable elements

### Android TV Video Playback Fixes
Implemented by @chila254

- Show loading indicator during video switching instead of black screen
- Fix video black screen on auto-advance between video songs by always re-enabling video mode
- Fix playback freeze, crash on navigate, and audio stutter during video transitions
- Fix video black screen and progress bar stuck issues
- Immediately show loading state when transitioning between video songs

### Lyrics Provider Improvements
Implemented by @chila254

- Fix Rush lyrics fetching with LyricsPlus and Genius fallback
- Fix Rush and Better Lyrics fetching fallbacks for more reliable lyrics loading
- Fix Rush lyrics fallback and TV queue video transitions

### Android 14 Compatibility
Implemented by @chila254

- Fix Android 14 crash in TvRecommendationService by calling startForeground() immediately
- Create notification channel before posting notification
- Remove Picture-in-Picture from TV player (TVs don't support PiP mode)

### Build & CI/CD Improvements
Implemented by @chila254

- Add ProGuard rules for Kuromoji jar dictionary files
- Use repository owner username for git commits instead of github-actions bot
- Reformat release notes with categorized sections matching v2.6.0 style
- Rename APK artifacts: AuraMusic.apk, AuraMusic-with-Google-Cast.apk, AuraMusic-Tv.apk

## Translation Contributors

No new translation contributions in this release.

**Full Changelog**: https://github.com/TeamAuraMusic/AuraMusic/compare/v2.7.0...v2.8.0

**Full Changelog**: https://github.com/TeamAuraMusic/AuraMusic/compare/...298cc65adce05d66a541fa6a774d996d34b02acd

# AuraMusic v2.7.0 (Build 24) Release Notes

<!-- Release notes generated using configuration in .github/release.yml at 3a8e42ba4419a8e2451e5e440e63e85894a05e23 -->



**Full Changelog**: https://github.com/TeamAuraMusic/AuraMusic/compare/v2.6.0...v2.7.0

## Full Changelog (Commits since last release)

- Update v2.6.0 release notes and changelog ([8c5fd23](https://github.com/TeamAuraMusic/AuraMusic/commit/8c5fd23f204720bfb470ff3350a9391bf32ca23d)) — chila254
- feat: improve tablet split-view and lock-screen playback metadata ([3e23b3e](https://github.com/TeamAuraMusic/AuraMusic/commit/3e23b3ec705566cb6f28ce844762a4c4e06a87e8)) — chila254
- feat: add late night audio mode and surround passthrough ([cddc04b](https://github.com/TeamAuraMusic/AuraMusic/commit/cddc04b96186d97ee43a3b250dfdbfa9e6772cbb)) — chila254
- feat: add library audiobooks with resume bookmarks ([d44536a](https://github.com/TeamAuraMusic/AuraMusic/commit/d44536a590043d1845fb1e9149194d3e7201b5fc)) — chila254
- Added translation using Weblate (Chinese (Simplified Han script)) ([20369a6](https://github.com/TeamAuraMusic/AuraMusic/commit/20369a6374a298a9f0e9797d8a9d7a7d921907a6)) — Franklin Chilango
- fix: add missing AUDIOBOOKS case in AppearanceSettings ([d589f54](https://github.com/TeamAuraMusic/AuraMusic/commit/d589f5434d4a5adc0d91ed263cbf54e9a2dfa4fc)) — chila254
- feat: add Ko-fi donation option in About screen ([1246be9](https://github.com/TeamAuraMusic/AuraMusic/commit/1246be9ffaa186541c6c837ddf3ac1785ebe1172)) — chila254
- feat: add Ko-fi donation option in About screen ([0b2db2d](https://github.com/TeamAuraMusic/AuraMusic/commit/0b2db2dcdf70493a3a143a005645bf5f063a15eb)) — chila254
- Merge branch 'main' of https://github.com/TeamAuraMusic/AuraMusic ([d370bfe](https://github.com/TeamAuraMusic/AuraMusic/commit/d370bfe4cccb4bdfc206ef06cc91486f9134b154)) — chila254
- feat: add Ko-fi donation option in About screen ([12e8242](https://github.com/TeamAuraMusic/AuraMusic/commit/12e8242f7f03e6d40c8418152961499a6136bf48)) — chila254
- Translated using Weblate (Chinese (Simplified Han script)) ([368a126](https://github.com/TeamAuraMusic/AuraMusic/commit/368a126699f4de9f3f798e93eaa7487c5578b15f)) — LibreTranslate
- Translated using Weblate (Chinese (Simplified Han script)) ([7c55a00](https://github.com/TeamAuraMusic/AuraMusic/commit/7c55a006342050fc1ca4470db9ffce8e8ca6ea55)) — LibreTranslate
- Translated using Weblate (French) ([7253a3d](https://github.com/TeamAuraMusic/AuraMusic/commit/7253a3daee6d72b43afa178e7d88df98e8706195)) — Mickaël Binos
- Translated using Weblate (Portuguese (Portugal)) ([844cc48](https://github.com/TeamAuraMusic/AuraMusic/commit/844cc48158ef018833fd62431835eec9893244b7)) — LibreTranslate
- Translated using Weblate (Portuguese) ([d8554ca](https://github.com/TeamAuraMusic/AuraMusic/commit/d8554caaa43cc35edcf937e73d853b4f271f7049)) — LibreTranslate
- Translated using Weblate (Chinese (Simplified Han script)) ([b54a02c](https://github.com/TeamAuraMusic/AuraMusic/commit/b54a02ce7a6e2b5f65422e41cff2e517c516f0e0)) — LibreTranslate
- Translated using Weblate (German) ([00c5f2f](https://github.com/TeamAuraMusic/AuraMusic/commit/00c5f2fd502704db97c1f3840b210347d9c361eb)) — LibreTranslate
- Translated using Weblate (French) ([1b95ff6](https://github.com/TeamAuraMusic/AuraMusic/commit/1b95ff65d65fdadf04ce966c0c54b3953c4d5736)) — LibreTranslate
- Translated using Weblate (Spanish) ([6673824](https://github.com/TeamAuraMusic/AuraMusic/commit/66738242b57fbc86b0568bb9e72fe1dfc47316cd)) — LibreTranslate
- Added translation using Weblate (Arabic) ([2ac9db1](https://github.com/TeamAuraMusic/AuraMusic/commit/2ac9db134d34261f35069cda9af011e94b130753)) — Franklin Chilango
- Added translation using Weblate (Filipino) ([b442c41](https://github.com/TeamAuraMusic/AuraMusic/commit/b442c41fe4ec8959599caf6fc564b5f4dbdb82f4)) — Franklin Chilango
- feat: add audiobook resume playback on home ([b51915a](https://github.com/TeamAuraMusic/AuraMusic/commit/b51915a8beca717a43e35838b625bd7f736bc62f)) — chila254
- fix: show crossfade seconds and repair home widgets ([a277c4f](https://github.com/TeamAuraMusic/AuraMusic/commit/a277c4f6c49d38f07d889ca1a6b90c8b8d1b92cf)) — chila254
- fix: replace Ko-fi about icon with correct brand mark ([9eac05b](https://github.com/TeamAuraMusic/AuraMusic/commit/9eac05b7328d31659a594a04734b9d589af19b1d)) — chila254
- Translated using Weblate (Arabic) ([ae64d1d](https://github.com/TeamAuraMusic/AuraMusic/commit/ae64d1d8f1c2a3dc042a99d175732177e95ae2eb)) — LibreTranslate
- Translated using Weblate (Arabic) ([1cf8fb7](https://github.com/TeamAuraMusic/AuraMusic/commit/1cf8fb7b38b5b0989a41fe532c2b0a6e4629466c)) — LibreTranslate
- Translated using Weblate (French) ([55046f0](https://github.com/TeamAuraMusic/AuraMusic/commit/55046f06edf4caf5e936addee1b3ea36656c3b2f)) — Mickaël Binos
- Translated using Weblate (Spanish) ([b9e2215](https://github.com/TeamAuraMusic/AuraMusic/commit/b9e2215724dcd24408a95af79aba42400c1b3417)) — Libre
- Translated using Weblate (Spanish) ([4de6976](https://github.com/TeamAuraMusic/AuraMusic/commit/4de697697f63b74679d279e95df7f6d58d947538)) — ItsMeCrizzzGD
- Translated using Weblate (Spanish) ([549baf6](https://github.com/TeamAuraMusic/AuraMusic/commit/549baf66a73a8ac2116580f9a492de03d92c4287)) — Weblate Translation Memory
- Fix signed-in YouTube Music public requests ([b2adc07](https://github.com/TeamAuraMusic/AuraMusic/commit/b2adc0712c1e6219fa1564eeb99092d4f1753622)) — chila254
- Fix InnerTube login state and improve font readability ([38eac54](https://github.com/TeamAuraMusic/AuraMusic/commit/38eac5422e187352cddc3a0217113b076c7cb72c)) — chila254
- Translated using Weblate (French) ([15225ef](https://github.com/TeamAuraMusic/AuraMusic/commit/15225ef014735f1224f469db467f88be082a61cc)) — Mickaël Binos
- feat(tv): enhance TV variant with playback settings, visualizer, PiP, system settings, and polish ([145cbfc](https://github.com/TeamAuraMusic/AuraMusic/commit/145cbfc4cf904fcadcc10ac785f9e3f178923adc)) — chila254
- Merge branch 'main' of https://github.com/TeamAuraMusic/AuraMusic ([e070511](https://github.com/TeamAuraMusic/AuraMusic/commit/e0705117b85e3b15041e3265841eb6a075df8d21)) — chila254
- fix(playback): use reflection for TvMainActivity to fix mobile build ([17981ed](https://github.com/TeamAuraMusic/AuraMusic/commit/17981ed5dfff7c9279a32684905aac8483917739)) — chila254
- fix(tv): fix mini player, visualizer, and add BackHandler to all TV screens ([c14930d](https://github.com/TeamAuraMusic/AuraMusic/commit/c14930d0adfbb489846c6801f8b5ecab0ad77920)) — chila254
- build: enable minification and resource shrinking for release builds ([0d5a425](https://github.com/TeamAuraMusic/AuraMusic/commit/0d5a42549cc33bf795260ae99e540f5d5c831e02)) — chila254
- feat(wrapped): add 5 new pages and improve Wrapped experience ([1270277](https://github.com/TeamAuraMusic/AuraMusic/commit/1270277a9f7a61f68e089b9ea60efb3b17ce4f31)) — chila254
- Added translation using Weblate (Italian) ([bd7b350](https://github.com/TeamAuraMusic/AuraMusic/commit/bd7b3504e3012b482d92ed85af6682a98a042ac4)) — ferrari
- Translated using Weblate (Italian) ([a27899f](https://github.com/TeamAuraMusic/AuraMusic/commit/a27899f62eca47b3308a294db18a780141c61ba4)) — ferrari
- Translated using Weblate (Portuguese (Portugal)) ([974de54](https://github.com/TeamAuraMusic/AuraMusic/commit/974de5440c24cea7dcaeddc094230e11c0b01d17)) — LibreTranslate
- Translated using Weblate (Italian) ([6e70be5](https://github.com/TeamAuraMusic/AuraMusic/commit/6e70be5cf05343bf717ffeef59a93cebfb8b6e51)) — ferrari
- Translated using Weblate (Spanish) ([91edf83](https://github.com/TeamAuraMusic/AuraMusic/commit/91edf83e261243fe0130cf1319a779a10b7f120a)) — LibreTranslate
- Translated using Weblate (Italian) ([4e3a11d](https://github.com/TeamAuraMusic/AuraMusic/commit/4e3a11d637161bc2237c7024560017aff86f4db7)) — ferrari
- fix(tv): fix theme color not applying and focus escaping to miniplayer ([b11461c](https://github.com/TeamAuraMusic/AuraMusic/commit/b11461ca263634be5c4e0f2025cf99b51ac7751c)) — chila254
- feat: integrate SponsorBlock for auto-skipping sponsor segments ([1e5d7f6](https://github.com/TeamAuraMusic/AuraMusic/commit/1e5d7f6a18426cdf86f8cb05a355ed4f6e5449e4)) — chila254
- Fix SponsorBlock playback integration and settings icon ([59d4b93](https://github.com/TeamAuraMusic/AuraMusic/commit/59d4b936b52aa32ac5bfec8187149df21c0e12e4)) — chila254
- Expose music-focused SponsorBlock categories ([04700d9](https://github.com/TeamAuraMusic/AuraMusic/commit/04700d9a00b1161c198ee1b28e7f35c4fa024625)) — chila254
- Fix mobile YouTube login session refresh ([65ff27c](https://github.com/TeamAuraMusic/AuraMusic/commit/65ff27c438c68ffaab55a5d1668782f9fabe3a35)) — chila254
- Added translation using Weblate (Tamil) ([e53442a](https://github.com/TeamAuraMusic/AuraMusic/commit/e53442a5a244e1e50cb7e607e4df514d58a643ff)) — தமிழ்நேரம்
- fix: improve SponsorBlock integration based on SmartTube implementation ([ce49a88](https://github.com/TeamAuraMusic/AuraMusic/commit/ce49a88025288a49563bd670b09cf63c3dde3979)) — chila254
- Translated using Weblate (Tamil) ([1e2485c](https://github.com/TeamAuraMusic/AuraMusic/commit/1e2485ccc53e81ca115a9606d997465c7bec22ec)) — தமிழ்நேரம்
- Translated using Weblate (Tamil) ([a8a565b](https://github.com/TeamAuraMusic/AuraMusic/commit/a8a565b6f8f4fc2c5c8c6b73dcb7030bc8ac1b27)) — தமிழ்நேரம்
- Fix TV playback UI, settings, and focus stability ([df6cde6](https://github.com/TeamAuraMusic/AuraMusic/commit/df6cde668a1346bb9562f74c4341d3ff73c06852)) — chila254
- fix(lyrics): fix broken Rush and BetterLyrics providers ([42971f4](https://github.com/TeamAuraMusic/AuraMusic/commit/42971f4ef82ea089f5684e0fc9af15d0822d5da8)) — chila254
- fix(search): handle musicCarouselShelfRenderer in search summary ([1fad31a](https://github.com/TeamAuraMusic/AuraMusic/commit/1fad31a53ca4f43b37f62d8944fbb157fb9a6e28)) — chila254
- fix: correct FileProvider authority case to prevent crash ([c16ce75](https://github.com/TeamAuraMusic/AuraMusic/commit/c16ce7547c48f13c6fb15ddf7a5e5dd1fc2ca093)) — chila254
- fix: show all search result categories instead of only top result ([dd6b453](https://github.com/TeamAuraMusic/AuraMusic/commit/dd6b45382216a18565eebf1a61a71f9445278db0)) — chila254
- fix(tv): smooth playback, add stable keys, round artist thumbnails ([39281a4](https://github.com/TeamAuraMusic/AuraMusic/commit/39281a437bcbc3960ee0aa0d03b37d89cfe734d3)) — chila254
- fix(tv): exit dialog, keep screen on, login D-pad navigation ([ce7354d](https://github.com/TeamAuraMusic/AuraMusic/commit/ce7354d2056d8dfad7ab211c27c3213d903771a2)) — chila254
- fix(tv): crossfade slider focus, SponsorBlock video songs, more home categories ([5f2499f](https://github.com/TeamAuraMusic/AuraMusic/commit/5f2499fd95adade71534cb8d61b2145c0897d935)) — chila254
- Fix TV home metadata and remote playback controls ([eda4d1a](https://github.com/TeamAuraMusic/AuraMusic/commit/eda4d1a48e8e0aa15401ffc317a31343f0bc4ebd)) — chila254
- Fix release notes commit range generation ([f5abb49](https://github.com/TeamAuraMusic/AuraMusic/commit/f5abb49a56514e797b1033efbfd7c4f4adb8b9cd)) — chila254
- Fix TV player sleep feedback, video handoff, and YouTube login persistence ([8cf7c30](https://github.com/TeamAuraMusic/AuraMusic/commit/8cf7c306ecc28036a18890f335a727529e93d1b2)) — chila254
- chore(release): prepare v2.7.0 ([3a8e42b](https://github.com/TeamAuraMusic/AuraMusic/commit/3a8e42ba4419a8e2451e5e440e63e85894a05e23)) — chila254

**Full diff:** https://github.com/TeamAuraMusic/AuraMusic/compare/v2.6.0...3a8e42ba4419a8e2451e5e440e63e85894a05e23

## New Contributors

- @TamilNeram made their first contribution to AuraMusic

# AuraMusic v2.6.0 (Build 23) Release Notes

<!-- Release notes generated using configuration in .github/release.yml at 7f7ece89489e58a71f5d25f871919f4ed704148b -->

**Full Changelog**: https://github.com/TeamAuraMusic/AuraMusic/compare/v2.5.0...v2.6.0

> [!NOTE]
> This release brings lyrics font selection improvements, AuraCanvas video backdrop enhancements, YouTube API 2026 compatibility fixes, and Ko-fi funding support.

## What's New

### Lyrics Improvements
Implemented by @chila254

- feat(lyrics): Implement lyrics font selection for normal lyrics display
- Fix YouTube Music song parsing after InnerTube response change
- Fix logged-in YouTube Music browse/search failures
- Fix YouTube session refresh and logged-out search results
- Speed up uncached YouTube Music stream startup

Lyrics display has been improved with font selection options and better YouTube Music integration.

### AuraCanvas Enhancements
Implemented by @chila254

- Fix AuraCanvas display logic and Thumbnail component parameters
- Fix AuraCanvas to show only when player is expanded, hide thumbnail when canvas active
- Fix AuraCanvas to show only when canvas is available, hide thumbnail when canvas active
- Move player canvases to full-screen background
- Remove CastButton from AuraCanvasOverlay for foss compatibility
- Tighten player canvas matching
- Improve AuraCanvasOverlay: switch to TextureView, add error handling and fade-in animation

AuraCanvas now properly shows video backdrops only when available, with improved display logic and better compatibility.

### YouTube API 2026 Compatibility
- Fix YouTube API 2026 breaking changes
- Remove karaoke implementation (deprecated server)
- Changed the client engine to cio for improved networking performance

### Funding & Contributions
- Add funding details for Ko-fi
- Update funding sources in FUNDING.yml
- Fix quotes in custom funding URL

### Subscribed Artists
- Fix subscribed artists showing local songs instead of subscriber count
- Fix subscribed artist metadata and release notification polling
- Improve subscribed artist library metadata and release alerts

## Translation Contributors

- @AntonioOliveira2 made their first contribution to AuraMusic
- @wafL implemented Translated using Weblate (Portuguese)
- @Mickael81 implemented Translated using Weblate (French)
- @SantosSi implemented Translated using Weblate (Portuguese Portugal)

---

# AuraMusic v2.5.0 (Build 22) Release Notes

> [!NOTE]
> This release brings AuraCanvas - dynamic video backdrops for artist headers and album covers, fixes for BetterLyrics TTML fetching and parsing, improved HTML entity decoding in SimpMusic, monochrome layer support for adaptive icons on Android 13+, and updated Discord invite link.

## What's New

### AuraCanvas - Dynamic Video Backdrops
Implemented by @chila254

- feat(canvas): implement AuraCanvas for artist headers and album covers 
- Improve AuraCanvasOverlay: switch to TextureView, add error handling and fade-in animation 
- feat(player): AuraCanvas - looping video backdrops behind album art 
- fix(canvas): make AuraCanvas actually show videos in the player 

AuraCanvas brings dynamic video backdrops to artist headers and album covers. This feature adds visual depth to the app with looping video backgrounds that enhance the music experience.

### BetterLyrics & SimpMusic Improvements
Implemented by @chila254

- fix(betterlyrics): implement correct TTML fetching and parsing 
- fix(lyrics): remove broken getAllLyrics implementation in BetterLyrics 
- fix(lyrics): make BetterLyrics actually return lyrics, honour provider priority, and unblock the retry button 
- Fix HTML entity decoding in SimpMusic lyrics provider 

### Adaptive Icons Enhancement
Implemented by @chila254

- feat(icons): Add monochrome layer to all adaptive icons for better dynamic/themed icon consistency on Android 13+

### Networking Improvements
Implemented by @chila254

- Changed the client engine to cio 

### Bug Fixes
Implemented by @chila254

- Fix start page playback and speed dial shuffle loading 
- fix(discord): update Discord invite link to https://discord.gg/935CRM8u3 in About section and README


## Translation Contributors

- @Mickael81 implemented Translated using Weblate (French) 
- @SantosSi implemented Translated using Weblate (Portuguese Portugal)
- @SantosSi implemented Translated using Weblate (Portuguese)
- @iamcrizzzgd implemented Translated using Weblate (Spanish) 



**Full Changelog**: https://github.com/TeamAuraMusic/AuraMusic/compare/v2.4.0...v2.5.0

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
