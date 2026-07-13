# AuraMusic v2.9.0 (Build 26) Changelog

> [!NOTE]
> This release delivers a complete rewrite of the Discord integration using an OAuth2 PKCE flow, alongside major video playback stability fixes, SponsorBlock improvements, grouped search results, a fix for the Vosk voice crash on Android 16, and YouTube Music history sync.

> [!WARNING]
> The Discord integration is still a work in progress and has **not** been fully finished. The new OAuth2 login and rich presence are functional but remain under active development and may still experience intermittent issues (login race conditions, scope errors, presence timeouts).

## Major Features

### Discord Integration — OAuth2 PKCE Rewrite
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

### Search & Library
- feat: add grouped search results by item type
- Fix YouTube Music history sync for mobile playback
- fix(youtube-music): fix history sync and Discord OAuth2 login
- Fix Discord profile info and rich presence reliability
- Fix tablet search navigation clicks being swallowed

### Voice Recognition (Vosk)
- fix(voice): fix Vosk crash on Android 16 by upgrading JNA to 5.19.1

## Bug Fixes

### Video Playback Stability
- Stop blocking video startup on subtitle loading
- Avoid duplicate video stream extraction
- Preserve real video MIME types to reduce black-screen playback
- Fall back to audio when restricted video playback fails
- fix(video): remove guard that blocked video display
- Fix mobile video mode and Discord presence reliability
- Fix Tv player controls, TV mini player, and Discord presence
- fix(discord,video,anr): stop video autoloading, reduce ANR risk

### SponsorBlock
- Fix SponsorBlock video segment loading
- Update SponsorBlock duration handling and empty category behavior
- fix(tv,sponsorblock): fix miniplayer size, player controls, and SponsorBlock

### Discord Presence & Scrobbling
- Fix Discord presence, sleep timer, and Last.fm scrobbling

## Docs & README
- Add Trendshift badge and logo to README
- Improve Trendshift logo visibility in README (light/dark mode)
- docs: update RELEASE_NOTES.md and CHANGELOG.md for v2.8.0 in v2.6.0 format
- docs: add interview presentation guide for GiveDirectly application (later removed)
- Delete INTERVIEW_PREP.md

## Full Changelog (Commits since last release)

- docs: update RELEASE_NOTES.md and CHANGELOG.md for v2.8.0 in v2.6.0 format ([6af9942](https://github.com/TeamAuraMusic/AuraMusic/commit/6af9942)) — chila254
- Fix SponsorBlock video segment loading ([0a8db82](https://github.com/TeamAuraMusic/AuraMusic/commit/0a8db82)) — chila254
- Add Trendshift badge to README ([4b5dbbe](https://github.com/TeamAuraMusic/AuraMusic/commit/4b5dbbe)) — chila254
- Add Trendshift logo to README ([d5941c0](https://github.com/TeamAuraMusic/AuraMusic/commit/d5941c0)) — chila254
- Improve Trendshift logo visibility in README ([7f5eb53](https://github.com/TeamAuraMusic/AuraMusic/commit/7f5eb53)) — chila254
- Fix Trendshift Logo visibility in README dark mode ([0fea8bf](https://github.com/TeamAuraMusic/AuraMusic/commit/0fea8bf)) — chila254
- Stop blocking video startup on subtitle loading, avoid duplicate video stream extraction, preserve real video MIME types, fall back to audio on restricted video, fix tablet search navigation ([ca847fb](https://github.com/TeamAuraMusic/AuraMusic/commit/ca847fb)) — chila254
- Fix Tv player controls, TV mini player, and Discord presence ([de5d328](https://github.com/TeamAuraMusic/AuraMusic/commit/de5d328)) — chila254
- Fix Discord profile info and rich presence reliability ([048fab6](https://github.com/TeamAuraMusic/AuraMusic/commit/048fab6)) — chila254
- Fix mobile video mode and Discord presence reliability ([245663b](https://github.com/TeamAuraMusic/AuraMusic/commit/245663b)) — chila254
- Fix YouTube Music history sync for mobile playback ([c4ca041](https://github.com/TeamAuraMusic/AuraMusic/commit/c4ca041)) — chila254
- Fix Discord presence, sleep timer, and Last.fm scrobbling ([9c8ed84](https://github.com/TeamAuraMusic/AuraMusic/commit/9c8ed84)) — chila254
- feat: add grouped search results by item type ([66d3420](https://github.com/TeamAuraMusic/AuraMusic/commit/66d3420)) — chila254
- fix: discord rpc connection and token handling issues ([0c73839](https://github.com/TeamAuraMusic/AuraMusic/commit/0c73839)) — chila254
- fix(discord): fix RPC connection lifecycle, applicationId, and presence deduplication ([83f5558](https://github.com/TeamAuraMusic/AuraMusic/commit/83f5558)) — chila254
- fix(discord): add missing HEARTBEAT_ACK import ([b38a9f8](https://github.com/TeamAuraMusic/AuraMusic/commit/b38a9f8)) — chila254
- docs: add interview presentation guide for GiveDirectly application ([c9f0b73](https://github.com/TeamAuraMusic/AuraMusic/commit/c9f0b73)) — chila254
- Delete INTERVIEW_PREP.md ([6748336](https://github.com/TeamAuraMusic/AuraMusic/commit/6748336)) — Franklin Chilango
- fix(discord): improve login display, HTTP client config, and error handling ([69415c0](https://github.com/TeamAuraMusic/AuraMusic/commit/69415c0)) — chila254
- fix(discord): remove ContentNegotiation plugin that caused compile error ([8a11436](https://github.com/TeamAuraMusic/AuraMusic/commit/8a11436)) — chila254
- fix(discord): use OkHttp engine for getUserInfo to fix SSL certificate error ([246de84](https://github.com/TeamAuraMusic/AuraMusic/commit/246de84)) — chila254
- debug(discord): add logging to trace RPC presence flow ([269564b](https://github.com/TeamAuraMusic/AuraMusic/commit/269564b)) — chila254
- fix(discord): fix IDENTIFY token format, device ID, and image resolution ([4033000](https://github.com/TeamAuraMusic/AuraMusic/commit/4033000)) — chila254
- fix(discord): fix compile errors in ExternalAssets API and KizzyRPC ([a304ce1](https://github.com/TeamAuraMusic/AuraMusic/commit/a304ce1)) — chila254
- fix(discord): add periodic presence refresh and connection logging ([1acf0db](https://github.com/TeamAuraMusic/AuraMusic/commit/1acf0db)) — chila254
- fix(discord): send raw user token in gateway IDENTIFY so presence shows ([aa7d579](https://github.com/TeamAuraMusic/AuraMusic/commit/aa7d579)) — chila254
- Added translation using Weblate (Hindi) ([c3c9eb2](https://github.com/TeamAuraMusic/AuraMusic/commit/c3c9eb2)) — Franklin Chilango
- feat(discord): complete rewrite of Discord integration with OAuth2 PKCE flow ([b136d94](https://github.com/TeamAuraMusic/AuraMusic/commit/b136d94)) — chila254
- fix(voice): fix Vosk crash on Android 16 by upgrading JNA to 5.19.1 ([e42e03e](https://github.com/TeamAuraMusic/AuraMusic/commit/e42e03e)) — chila254
- fix(youtube-music): fix history sync and Discord OAuth2 login ([7903e0b](https://github.com/TeamAuraMusic/AuraMusic/commit/7903e0b)) — chila254
- fix(tv,sponsorblock): fix miniplayer size, player controls, and SponsorBlock ([a78eb8d](https://github.com/TeamAuraMusic/AuraMusic/commit/a78eb8d)) — chila254
- fix(discord): fix login never completing - events lost on gateway replacement ([5ba382b](https://github.com/TeamAuraMusic/AuraMusic/commit/5ba382b)) — chila254
- Translated using Weblate (Hindi) ([123b865](https://github.com/TeamAuraMusic/AuraMusic/commit/123b865)) — chila254
- fix(discord,video,anr): fix login, stop video autoloading, reduce ANR risk ([09bf8d1](https://github.com/TeamAuraMusic/AuraMusic/commit/09bf8d1)) — chila254
- fix(discord): wire gateway events bus on first init - login was silently broken ([1aadcbc](https://github.com/TeamAuraMusic/AuraMusic/commit/1aadcbc)) — chila254
- fix(video): remove guard that blocked video display ([8846fa4](https://github.com/TeamAuraMusic/AuraMusic/commit/8846fa4)) — chila254
- fix(discord): fix login not reflecting authenticated state - handle onNewIntent for singleTask OAuth activity, use local readyDeferred to prevent race conditions, show logged-in state when token exchange succeeds even if gateway READY times out, fetch user info on rehydration ([d85400c](https://github.com/TeamAuraMusic/AuraMusic/commit/d85400c)) — chila254
- fix(discord): fix scope mismatch - change openid to identify scope to match Discord Developer Portal config, add detailed token exchange logging for debugging ([ebc8d4c](https://github.com/TeamAuraMusic/AuraMusic/commit/ebc8d4c)) — chila254
- fix(discord): revert scopes to match working Metrolist implementation - use openid not identify ([974eeaa](https://github.com/TeamAuraMusic/AuraMusic/commit/974eeaa)) — chila254
- debug(discord): add verbose logging to diagnose OAuth token exchange failure ([d0ea881](https://github.com/TeamAuraMusic/AuraMusic/commit/d0ea881)) — chila254
- debug(discord): add Toast popup messages to diagnose OAuth failure without laptop ([dbaa76b](https://github.com/TeamAuraMusic/AuraMusic/commit/dbaa76b)) — chila254
- Added translation using Weblate (Turkish) ([8b4e5b6](https://github.com/TeamAuraMusic/AuraMusic/commit/8b4e5b6)) — Weblate
- Translated using Weblate (Chinese (Simplified Han script)) ([f4d8a13](https://github.com/TeamAuraMusic/AuraMusic/commit/f4d8a13)) — Weblate
- fix(discord): change openid scope to identify - openid causes invalid_scope error ([2578c64](https://github.com/TeamAuraMusic/AuraMusic/commit/2578c64)) — chila254

**Full diff:** https://github.com/TeamAuraMusic/AuraMusic/compare/v2.8.0...v2.9.0

# AuraMusic v2.8.0 (Build 25) Changelog

> [!NOTE]
> This release brings a complete Android TV redesign with Spotify-style focused detail panel, major video playback stability fixes, improved lyrics fallbacks with LyricsPlus and Genius support, and streamlined CI/CD pipeline.

## Major Features

### Android TV — Spotify-Style Focused Detail Panel
- feat(tv): replace hero carousel with Spotify-style focused detail panel
- feat(tv): add Continue Listening row on Google TV home screen
- Modernize TV home focused detail panel with transparent top bar overlay
- Restructure home screen layout so focused panel joins top bar and metadata shows below
- Remove Recently Played row from TV home screen
- Increase focused panel height to fully cover previous row content when scrolling
- Reduce section headers for cleaner layout

### Android TV Navigation & Focus
- Separate mini player into two distinct focusable areas: song info and play/pause button
- Fix mini player single-press activation using Surface(onClick)
- Add proper D-pad navigation throughout all TV screens
- Push focused panel content below the nav bar with proper top padding on all screens
- Improve login screen D-pad navigation to handle all focusable elements

## Bug Fixes

### Video Playback Stability
- Show loading indicator during video switching instead of black screen
- Fix video black screen on auto-advance between video songs
- Fix playback freeze, crash on navigate, and audio stutter during video transitions
- Fix video black screen and progress bar stuck issues

### Lyrics Providers
- Fix Rush lyrics fetching with LyricsPlus and Genius fallback
- Fix Rush and Better Lyrics fetching fallbacks for more reliable lyrics loading
- Fix Rush lyrics fallback and TV queue video transitions

### Android TV Fixes
- Fix Android 14 crash in TvRecommendationService by calling startForeground() immediately
- Create notification channel before posting notification
- Remove Picture-in-Picture from TV player (TVs don't support PiP)
- Fix video black screen on auto-advance between video songs
- Fix mini player click requiring two presses
- Fix video playback when skipping to next song

## Build & CI/CD
- Add ProGuard rules for Kuromoji jar dictionary files
- Use repository owner username for git commits instead of github-actions bot
- Reformat release notes with categorized sections matching v2.6.0 style
- Rename APK artifacts: AuraMusic.apk, AuraMusic-with-Google-Cast.apk, AuraMusic-Tv.apk

**Full Changelog**: https://github.com/TeamAuraMusic/AuraMusic/compare/v2.7.0...v2.8.0

# AuraMusic v2.7.0 (Build 24) Changelog

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

# AuraMusic v2.6.0 (Build 23) Changelog

<!-- Release notes generated using configuration in .github/release.yml at 7f7ece89489e58a71f5d25f871919f4ed704148b -->

**Full Changelog**: https://github.com/TeamAuraMusic/AuraMusic/compare/v2.5.0...v2.6.0

## Major Features

### Lyrics Improvements
- feat(lyrics): Implement lyrics font selection for normal lyrics display
- Fix YouTube Music song parsing after InnerTube response change
- Fix logged-in YouTube Music browse/search failures
- Fix YouTube session refresh and logged-out search results

### AuraCanvas Enhancements
- Fix AuraCanvas display logic and Thumbnail component parameters
- Fix AuraCanvas to show only when player is expanded, hide thumbnail when canvas active
- Fix AuraCanvas to show only when canvas is available, hide thumbnail when canvas active
- Move player canvases to full-screen background
- Remove CastButton from AuraCanvasOverlay for foss compatibility
- Tighten player canvas matching
- Improve AuraCanvasOverlay: switch to TextureView, add error handling and fade-in animation

### YouTube & Playback
- Changed the client engine to cio for improved networking performance
- Speed up uncached YouTube Music stream startup
- Fix preloaded song queues skipping to the next track
- Play new release from notification tap

### Funding & Contributions
- Add funding details for Ko-fi
- Update funding sources in FUNDING.yml
- Fix quotes in custom funding URL

### Subscribed Artists
- Fix subscribed artists showing local songs instead of subscriber count
- Fix subscribed artist metadata and release notification polling
- Improve subscribed artist library metadata and release alerts

## Bug Fixes

### YouTube API 2026 Compatibility
- Fix YouTube API 2026 breaking changes
- Remove karaoke implementation (server-side deprecation)

### General Fixes
- Fix library artist subtitles and refresh app fonts
- Delete kilo.json file

## Translation Contributors

- @AntonioOliveira2 made their first contribution to AuraMusic
- @wafL implemented Translated using Weblate (Portuguese)
- @Mickael81 implemented Translated using Weblate (French)
- @SantosSi implemented Translated using Weblate (Portuguese Portugal)

# AuraMusic v2.5.0 (Build 22) Changelog

## Major Features

### AuraCanvas - Dynamic Video Backdrops
- feat(player): AuraCanvas - looping video backdrops behind album art
- feat(canvas): implement AuraCanvas for artist headers and album covers
- feat(canvas): implement correct TTML fetching and parsing
- Improve AuraCanvasOverlay: switch to TextureView, add error handling and fade-in animation
- fix(canvas): make AuraCanvas actually show videos in the player

### Lyrics Provider Fixes
- fix(lyrics): remove broken getAllLyrics implementation in BetterLyrics that was causing silent fetch failures
- fix(lyrics): make BetterLyrics actually return lyrics, honour provider priority, and unblock the retry button
- Fix HTML entity decoding in SimpMusic lyrics provider

### Icons & UI
- feat(icons): Add monochrome layer to all adaptive icons for better dynamic/themed icon consistency on Android 13+

### Networking
- Changed the client engine to cio for improved networking performance

### Build & Other
- Fix start page playback and speed dial shuffle loading
- fix(discord): update Discord invite link to https://discord.gg/935CRM8u3 in About section and README

**Full range**: 065a222...3e6a1d3

# AuraMusic v2.4.0 (Build 21) Changelog

## Major Features

### Video
- feat(video): support true 1080p+ playback by merging video-only and audio-only streams (chila254)

### Karaoke
- Full server karaoke integration with ML backend, downloaded song support, connection UI, progress, retries, and hardening (multiple commits by chila254)

### Playback
- feat: Implement Proof-of-Origin (PO) token support via WebView + BotGuard + automatic invalidation on errors (chila254)

### Lyrics
- feat(lyrics): rebuild Enhanced Lyrics with word-level rendering
- feat(lyrics): add experimental animated lyrics style + Monochrome animated background
- Fix Enhanced Lyrics instrumental gap indicators and connected lines

### Thumbnails & UI
- fix(thumbnails): upgrade resize() to handle all Google CDN hosts and produce sharp album/player artwork
- Improve thumbnail and album art quality
- feat(about): Modernize About screen - Contributors section with GitHub avatars + Liberapay
- Remove dark background surface from donation rows

### Build & Other
- Multiple F-Droid compatibility fixes (Gradle cache, sourceSets, Java 21, etc.)
- String/plural cleanup across translations

## New Contributors (First time in AuraMusic)

- [Mickaël Binos](https://github.com/Mickael81) made their first contribution in AuraMusic with the commit [Translated using Weblate (French)](https://github.com/TeamAuraMusic/AuraMusic/commit/5dcac14)

- [ItsMeCrizzzGD](https://github.com/iamcrizzzgd) made their first contribution in AuraMusic with the commit [Translated using Weblate (Spanish)](https://github.com/TeamAuraMusic/AuraMusic/commit/55ffaff)

- [Silvério Santos](https://github.com/SantosSi) made their first contribution in AuraMusic with the commit [Added translation using Weblate (Portuguese (Portugal))](https://github.com/TeamAuraMusic/AuraMusic/commit/c5659da886b5fc547b3ca26ee9a02702ac2bf9f6)

**Full range**: 2e1feb0...66cbc3b

# AuraMusic v2.3.0 (Build 20) Changelog

## Features

### Hardware Integration & Smart Device Ecosystem
- Complete hardware integration with Bluetooth device support
- Audio device picker style implementation for mini-player
- Bluetooth profile proxy usage and active hardware flow enhancements
- Wake-up and snooze alarm features
- Hardware dialog layout improvements

### Enhanced Lyrics Features
- Instrumental indicators in enhanced lyrics mode
- Connected lines for improved lyrics readability
- Intro wavy circular progress indicator before first vocal line
- Enhanced lyrics with interval indicators
- Custom font support with Google Fonts integration
- Material 3 font icon support
- Larger lyrics offset and improved timing (first vocal line reduced to 1000ms)

### Internationalization
- Added Weblate translation badge and link for community translations

## UI/UX Improvements
- Enhanced thumbnail quality for all YouTube videos and streamed songs
- Fixed blurry album and item thumbnails
- Smooth UI transitions and better visual effects
- Replaced circular refresh indicator with ContainedLoadingIndicator
- Font selection and application options
- Queue position moved to left in old player design
- Share as image functionality with background options

## Bug Fixes

### Navigation & UI
- Fixed NPE crash in backToMain navigation
- Fixed TV settings focus restoration when returning from sub-settings
- Fixed video mode persistence issues
- Fixed song click handling (removed combinedClickable that broke touch events)
- Fixed refresh indicator positioning

### Hardware & Connectivity
- Fixed Bluetooth smart-cast errors
- Resolved hardware integration and audio device flow issues

### Lyrics & Media
- Fixed lyrics provider issues and instrumental indicator support
- Fixed instrumental indicator and connected lines for all lyrics providers
- Fixed lyrics structure restoration and interval indicator limitations

### Build & Compatibility
- Fixed TV APK naming and updater support
- Resolved Android Gradle Plugin API compatibility issues
- Fixed F-Droid Java version compatibility (removed jvmToolchain from all modules)
- Updated JVM target to Java 21 to fix compilation inconsistency
- Fixed sourceSets API and replaced deprecated buildDir usage
- Fixed ShazamKit JvmTarget import in build script

## Build
- Bumped versionCode to 20
- Version: 2.3.0

---

**Full Changelog**: https://github.com/TeamAuraMusic/AuraMusic/compare/v2.2.0...v2.3.0

---

# AuraMusic v2.2.0 (Build 19) Changelog

### Android TV / Google TV Support
- **Complete TV App Implementation**: Full-featured TV client with D-pad navigation, focus management, and 10-foot UI
- **TV Home Screen**: Personalized Quick Picks, Forgotten Favorites, Keep Listening, Similar Recommendations, YouTube sections, and Your Playlists
- **TV Player Screen**: Large centered controls, progress bar, play/pause/skip, queue sidebar, sleep timer, lyrics toggle, and video mode support
- **TV Navigation**: Custom lightweight navigator with back stack, bidirectional navigation between top bar and content, per-section focus requesters
- **TV Lyrics Display**: Read-only lyrics overlay optimized for TV (no click-to-seek, no autoscroll)
- **TV Settings**: Comprehensive settings suite — Appearance (theme, dynamic colors, theme color picker), Content (auto-load queue toggle), Storage (with cache clearing), Updater (real update checking with download links), About (version/build info)
- **Radio Queue**: Tapping any song in Quick Picks, Forgotten Favorites, or Keep Listening now loads a YouTube radio queue with related songs (matching mobile behavior)

### Voice Command Improvements
- Added confidence and audio energy filtering to reduce false wake word triggers
- Lowered wake word detection thresholds for maximum sensitivity
- Added AEC, NoiseSuppressor, and RMS energy filtering to wake word detection
- Fixed wake word service to stop when starting manual voice session
- Fixed minimum speech length requirements for command mode
- Improved TTS greeting and audio ducking during voice commands
- Fixed microphone loop by stopping wake word service before restart

### UI/UX Improvements
- Added sleep timer and lyrics buttons to queue bar in new player design
- Added shuffle button with 4-dot animation to old player design
- Added kebab menu with animations to old player design
- Added gradient to static icon foreground for visual consistency
- Changed dynamic icon background from orange to grey for better visibility
- Fixed default icon background to black when installing
- Moved kebab menu from top area to bottom right
- Added gradient colors to dynamic icon foreground

### Widget Redesigns
- Increased compact square widget to 4x4 size
- Modernized music player, compact square, and compact wide widgets
- Added full-cover album art backgrounds
- Added placeholder image to turntable widget album art
- Fixed widget showing 'can't load widget' when service not running
- Fixed widget_wide_play_container to widget_wide_play_pause

### TV-Specific Features
- TV-specific storage handling with no-disk image cache to prevent accumulation
- Real TV updater using GitHub API with download links for TV builds
- TV content settings with auto-load queue toggle
- TV appearance settings with full theme color picker
- TV player marquee scrolling for long song titles (prevents layout shift)
- TV settings back navigation restores focus to previously selected item

## Bug Fixes

### TV Bug Fixes
- Fixed TV settings back navigation focus drifting to top nav bar
- Fixed TV lyrics not displaying due to improper song change handling
- Fixed TV lyrics storage (no database persistence, fresh fetch per song)
- Fixed TV content settings compilation and Add/Clear queue functionality
- Fixed TV navigation focus issues across Home, Details, Player, and Settings screens
- Fixed TV player white screen on launch
- Fixed TV UP navigation in all screens
- Fixed TV player and queue item long title overflow pushing icons down (added marquee)
- Fixed TV home screen title to "AuraMusic Tv"
- Fixed TV lyrics to be display-only without click-to-seek and autoscroll
- Fixed TV streaming cache and persistent lyrics toggle
- Fixed TV mini-player display and navigation issues
- Fixed TV compilation errors throughout module

### Mobile Bug Fixes
- Fixed ForegroundServiceDidNotStartInTimeException crash on Android 14+/SDK 36
- Fixed ANR caused by VOSK native cleanup blocking main thread
- Fixed SecurityException when starting microphone FGS from background on Android 14+
- Fixed VOSK detector memory leaks and false wake word triggers during playback
- Fixed mic contention between VOSK wake word and SpeechRecognizer
- Fixed TTS volume muting after voice commands
- Fixed VOSK model download corruption and validation
- Fixed "Hey Aura" / "Hello Aura" not recognizing
- Fixed wake word detection not triggering voice command overlay
- Fixed standalone 'aura' false positives in wake word grammar

## Build
- Bumped versionCode to 19
- Version: 2.2.0

---

**Full Changelog**: https://github.com/TeamAuraMusic/AuraMusic/compare/v2.1.0...v2.2.0

# AuraMusic v2.1.0 (Build 18) Changelog

## Features
- Added hands-free "Hey Aura" wake word detection using VOSK offline speech recognition
- Added voice commands with interactive overlay (Siri/Gemini-like wave animations)
- Added Text-to-Speech voice feedback with multi-voice selection and audio ducking
- Added Google Cast support for GMS variant with CastPickerSheet device selection
- Redesigned widgets with modern UI, full-cover album art, and increased compact square to 4x4
- Removed turntable widget
- Updated README with Google Cast and voice control features

## Voice Command Improvements
- Added confidence and audio energy filtering to reduce false wake word triggers
- Lowered wake word detection thresholds for maximum sensitivity
- Added AEC, NoiseSuppressor, and RMS energy filtering to wake word detection
- Fixed wake word service to stop when starting manual voice session
- Fixed minimum speech length requirements for command mode
- Improved TTS greeting and audio ducking during voice commands
- Fixed microphone loop by stopping wake word service before restart

### Voice Commands Supported
**Playback:** Play, Pause, Toggle play/pause, Next, Previous, Shuffle (on/off/toggle), Repeat (one/all/off)
**Seek:** Skip forward/backward N seconds/minutes
**Volume:** Volume up/down, Mute/Unmute
**Speed:** Speed up, Slow down, Reset to normal speed
**Search:** Search, Play search query
**Downloads:** Download current song, Download playlist, Download album
**Lyrics:** Show/Hide/Toggle lyrics
**Video:** Enable/Disable/Toggle video mode
**Media:** Toggle like, Show/Clear queue, Add to queue
**Settings:** Dark mode on/off, Toggle theme
**Navigation:** Go home, Go library, Open search, Open settings

## UI/UX Improvements
- Added sleep timer and lyrics buttons to queue bar in new player design
- Added shuffle button with 4-dot animation to old player design
- Added kebab menu with animations to old player design
- Added gradient to static icon foreground for visual consistency
- Changed dynamic icon background from orange to grey for better visibility
- Fixed default icon background to black when installing
- Moved kebab menu from top area to bottom right
- Added gradient colors to dynamic icon foreground

## Widget Redesigns
- Increased compact square widget to 4x4 size
- Modernized music player, compact square, and compact wide widgets
- Added full-cover album art backgrounds
- Added placeholder image to turntable widget album art
- Fixed widget showing 'can't load widget' when service not running
- Fixed widget_wide_play_container to widget_wide_play_pause

## Bug Fixes
- Fixed ForegroundServiceDidNotStartInTimeException crash on Android 14+/SDK 36
- Fixed ANR caused by VOSK native cleanup blocking main thread
- Fixed SecurityException when starting microphone FGS from background on Android 14+
- Fixed VOSK detector memory leaks and false wake word triggers during playback
- Fixed mic contention between VOSK wake word and SpeechRecognizer
- Fixed TTS volume muting after voice commands
- Fixed VOSK model download corruption and validation
- Fixed "Hey Aura" / "Hello Aura" not recognizing
- Fixed wake word detection not triggering voice command overlay
- Fixed standalone 'aura' false positives in wake word grammar

## Build
- Bumped versionCode to 18
- Version: 2.1.0
- Updated VOSK to 0.3.75
- Added Google Cast dependencies for GMS variant

---

**Full Changelog**: https://github.com/TeamAuraMusic/AuraMusic/compare/v2.0.0...v2.1.0


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
