# AuraMusic v3.1.0 (Build 28) Changelog

> [!NOTE]
> This changelog covers all 57 commits from `e2f86cd` through `e1d2c0e`, inclusive. No commits in the requested range are omitted.

## Highlights

- **Playback hardened for 2026** — PO token attestation, stream probing, media3 lifecycle and notification fixes eliminate the `foreground notification` and `next-song` crashes.
- **Extensive TV polish** — video playback, screensaver, library thumbnails, duplicate-key crashes and D-pad focus all fixed; TV library now always shows Liked Songs.
- **Thumbnails restored** — missing video thumbnails, liked-album empty states and playlist artwork now fall back correctly on both mobile and TV.
- **Split APKs fixed** — ABI splits now actually contain only one ABI and are ~30% smaller.

## Major Changes

### Playback, Streaming & Media3
- Fixed foreground notification `bot-check` crash and lifecycle alignment with media3 best practices.
- Resolved next-song crash, wrong-video advance and stream URL chunking.
- Reworked stream resolution and PO token attestation for 2026 enforcement — probing, rate-limit respect, guest-client fallback, exponential backoff and corrected `IO_BAD_HTTP_STATUS` handling.
- Upgraded media3 to 1.11.0 then pinned to 1.10.1 for stability; fixed `audio flush()` overload and FGS lifecycle.
- Fixed media notifications not showing and hardened FGS crash handling.
- Restored video thumbnails and added font size / boldness settings.

### Android TV
- Unified Google TV and Android TV UI scaling and enlarged TV fonts.
- Fixed video playback, next-song crashes, black screen on 2nd re-entry and surface restoration; video mode now enables instantly on home taps.
- Hardened video-song reliability with SponsorBlock/AutoMix guards and stopped regular songs from fetching video.
- Added artist subscribe, album like (YouTube Music-synced), synced lyrics and the Liked Songs tile; library always shows Liked Songs.
- Fixed duplicate-key crashes in all lazy lists and library `distinctBy`/`key` handling.
- Built and refined the in-app screensaver — video with captions, prevents Dream, fetches lyrics/video correctly, respects Do Not Sleep, stays on top and shows only for audio (not video).

### Discord
- Switched Discord HTTP from CIO to OkHttp to fix TLS login errors; uses presence OAuth scope and persists profile.
- Removed `application_id` from presence RPC that was blocking artist and song thumbnails in Discord.

### Library, Thumbnails & Artwork
- Fixed liked albums showing empty from the library and playlist thumbnails missing — fills missing `thumbnailUrl` during sync and shows fallback icons.
- Fixed TV liked playlists to show the first song thumbnail instead of black; `MediaCard` and `ItemThumbnail` now show a fallback when artwork is missing.

### Home, Widgets & Customization
- Added homepage layout toggle (grid/list), mini-player tweaks and liquid-glass effects; fixed layout-toggle crash and Quick Picks list mode.
- Improved widgets with full-bleed square art and visible play button.

### Build & Distribution
- Fixed split APKs — removed the `defaultConfig` ABI filter that forced all 4 ABIs into every variant. `universal` keeps 4 ABIs, `arm64`/`armeabi`/`x86`/`x86_64` each contain only one.

### Localization
- German, Chinese (Simplified), Russian, French, Spanish and Turkish translation updates via Weblate.

## Full Changelog — All 57 Commits

- Translated using Weblate (German) ([e2f86cd](https://github.com/TeamAuraMusic/AuraMusic/commit/e2f86cdad860eb4476d45369b8ba4c71a9fd4f54)) — zhx000
- Translated using Weblate (Chinese (Simplified Han script)) ([d1e57a1](https://github.com/TeamAuraMusic/AuraMusic/commit/d1e57a1ed206329e53e999d5f0dbb16f7641ee09)) — gmkeebiy
- Translated using Weblate (Chinese (Simplified Han script)) ([61e8813](https://github.com/TeamAuraMusic/AuraMusic/commit/61e8813d08f5ec8f6a324d40f219839f6a1a68f3)) — gmkeebiy
- fix: crash from foreground notification and playback bot-check ([a9f9cae](https://github.com/TeamAuraMusic/AuraMusic/commit/a9f9cae9af21dddcb0fffa0074fc47cc15446ee8)) — chila254
- fix(tv): unify Google TV and Android TV UI scaling ([ce03ccb](https://github.com/TeamAuraMusic/AuraMusic/commit/ce03ccb3ed7fbd5f1e635629f700783f6ff56cd5)) — chila254
- fix(tv): video playback, next-song crashes, and home hero design ([dcb5ebd](https://github.com/TeamAuraMusic/AuraMusic/commit/dcb5ebd05515eb21f05ae4efa40e7af69835454d)) — chila254
- fix(playback): resolve next-song crash, wrong-video advance, and stream URL chunking ([905e95c](https://github.com/TeamAuraMusic/AuraMusic/commit/905e95cc511c83f837ca2291fa45d393f402315a)) — chila254
- fix(widget): full-bleed square art, visible play button, and serialized updates ([3880bce](https://github.com/TeamAuraMusic/AuraMusic/commit/3880bce20dfebd20ea723fc4c943f2911ed250b8)) — chila254
- fix(tv): video-song reliability, SponsorBlock/Automix guards, and larger TV fonts ([053ca7b](https://github.com/TeamAuraMusic/AuraMusic/commit/053ca7b083856cf07314dbef8edf7cd78f75ae3a)) — chila254
- fix(playback): dedupe automix queue entries and use guest clients when logged in ([5c85ec9](https://github.com/TeamAuraMusic/AuraMusic/commit/5c85ec922ea1986ce382bfcbae965d8f1e1c683b)) — chila254
- fix(discord): switch HTTP from CIO to OkHttp engine to fix login TLS error ([12dc9e7](https://github.com/TeamAuraMusic/AuraMusic/commit/12dc9e72e8d1e81f2f949373469c98da579dd1d4)) — chila254
- fix(playback): configurable automix blend point and bot-check streaming fixes ([212cd05](https://github.com/TeamAuraMusic/AuraMusic/commit/212cd05e443714190fbfec0de12eb634c373a8be)) — chila254
- fix(discord): use presence OAuth scope and persist profile so Rich Presence stays connected ([f2aeb75](https://github.com/TeamAuraMusic/AuraMusic/commit/f2aeb75f77b01e8c8bc9b205b003c8894080dd48)) — chila254
- feat(customization): add homepage layout toggle, mini player tweaks, and expanded liquid glass effects ([7bcadf5](https://github.com/TeamAuraMusic/AuraMusic/commit/7bcadf5fba723e1c28109620804ae264ed860a1a)) — chila254
- fix(home): fix layout toggle crash and add quick picks list mode ([a850acd](https://github.com/TeamAuraMusic/AuraMusic/commit/a850acdbec81210a1456afaa10d764cf0ce020e4)) — chila254
- fix(playback): don't mislabel all IO_BAD_HTTP_STATUS errors as age-restricted ([cbcff60](https://github.com/TeamAuraMusic/AuraMusic/commit/cbcff60f41bfdb7ff393d3162631000818243b5c)) — chila254
- fix(playback): add exponential backoff to IO error retry handlers ([cb363e7](https://github.com/TeamAuraMusic/AuraMusic/commit/cb363e7fdb1bb8afc8c914162568aca0e9ec07e1)) — chila254
- Prokopyev Added translation using Weblate (Russian) ([1cc0897](https://github.com/TeamAuraMusic/AuraMusic/commit/1cc0897064fdab35cd6bef8d5212d02e2d53e5ea)) — Maxim
- Binos Translated using Weblate (French) ([5a946d8](https://github.com/TeamAuraMusic/AuraMusic/commit/5a946d8c624559fb41a6b0d6aece39b13f0cf392)) — Mickaël
- Prokopyev Translated using Weblate (Russian) ([ec24e37](https://github.com/TeamAuraMusic/AuraMusic/commit/ec24e37928c40290ad07b7436eeef32869f84603)) — Maxim
- Prokopyev Translated using Weblate (Russian) ([3a0708f](https://github.com/TeamAuraMusic/AuraMusic/commit/3a0708fbd2e4075d139bc16a54a37d8c0e56dc96)) — Maxim
- Translated using Weblate (Russian) ([d6977f3](https://github.com/TeamAuraMusic/AuraMusic/commit/d6977f322e624a799c682889244e5652f71662d2)) — LibreTranslate
- Translated using Weblate (Russian) ([8e6a999](https://github.com/TeamAuraMusic/AuraMusic/commit/8e6a99972852b7644368a04038e46497d20de3f1)) — vityatii
- Translated using Weblate (Russian) ([7aa06a3](https://github.com/TeamAuraMusic/AuraMusic/commit/7aa06a3467e410c2402b43a2f7a6a243a3536e8a)) — LibreTranslate
- Translated using Weblate (Russian) ([55d683a](https://github.com/TeamAuraMusic/AuraMusic/commit/55d683a416d1e5f025c811bb6cb0c805eefc0062)) — vityatii
- Translated using Weblate (Russian) ([96917b7](https://github.com/TeamAuraMusic/AuraMusic/commit/96917b717591e5caf1dfdf513497903d1d309434)) — LibreTranslate
- fix(playback): upgrade media3 to 1.11.0, stop FGS crash, and harden stream resolution ([2385e95](https://github.com/TeamAuraMusic/AuraMusic/commit/2385e95815988f540c8d7647225d0f78153587be)) — chila254
- fix(playback): probe stream URLs, respect rate limits, keep miniplayer visible ([acefb95](https://github.com/TeamAuraMusic/AuraMusic/commit/acefb95efb7dc2b018678442ad343a877114a4da)) — chila254
- fix(playback): rework stream resolution and PO token attestation for 2026 enforcement ([606bd4b](https://github.com/TeamAuraMusic/AuraMusic/commit/606bd4b5b9d6e2790990f0b05de7568df0f8909f)) — chila254
- Fix media notification not showing in notification panel ([793c613](https://github.com/TeamAuraMusic/AuraMusic/commit/793c613664a7065b0ed0c5cc0cac22a49f3e3f24)) — chila254
- Fix media notification not showing by removing ForegroundSafeMediaNotificationProvider wrapper ([b7bafe5](https://github.com/TeamAuraMusic/AuraMusic/commit/b7bafe5eef26bf7e8e9137ee89eb59ae977d2986)) — chila254
- Fix media notification by aligning notification lifecycle with media3 best practices ([556171d](https://github.com/TeamAuraMusic/AuraMusic/commit/556171df38f9ac64910d21e22ce76117a43816e6)) — chila254
- Downgrade media3 from 1.11.0 to 1.10.1 ([c2b2f2e](https://github.com/TeamAuraMusic/AuraMusic/commit/c2b2f2e8d4ba3dfde2d11d5707c1708480b29a04)) — chila254
- fix: audio flush() overload error and video thumbnails not showing ([d0d71c2](https://github.com/TeamAuraMusic/AuraMusic/commit/d0d71c21e98f6e1bca33451575202462273ff9d5)) — chila254
- Weblate user 159403 Translated using Weblate (Spanish) ([c1395c7](https://github.com/TeamAuraMusic/AuraMusic/commit/c1395c78fc092446a737583abb5f6df8754a5511)) — Hosted
- fix: video thumbnails not showing + add font size/boldness settings ([491e75a](https://github.com/TeamAuraMusic/AuraMusic/commit/491e75a2d22debc7098bc9be10a98bd34bd054ac)) — chila254
- feat(tv): artist subscribe, album like, synced lyrics, screensaver, liked-music tile + fixes ([261bb09](https://github.com/TeamAuraMusic/AuraMusic/commit/261bb09b687042c5d386b904c5b4d752309bbbc3)) — chila254
- fix(tv): resolve TV compile errors (onPreviewKeyEvent braces, voice imports) + Discord Rich Presence app id ([7652af2](https://github.com/TeamAuraMusic/AuraMusic/commit/7652af269adbe6f5b5c93eef813ab3030639110b)) — chila254
- Translated using Weblate (Turkish) ([1bbb87b](https://github.com/TeamAuraMusic/AuraMusic/commit/1bbb87b5e80268ad00d160bf9b25c1120cb44900)) — Buğra
- Translated using Weblate (Turkish) ([a2ee6c6](https://github.com/TeamAuraMusic/AuraMusic/commit/a2ee6c64a391e238e8b59d5e43ec3bf0dacdf94d)) — Buğra
- Ersen Translated using Weblate (Turkish) ([9e4bd56](https://github.com/TeamAuraMusic/AuraMusic/commit/9e4bd56f1ed321b998b01bbae0e99d738f41b2af)) — Oğuz
- fix(tv+mobile): Enhanced Lyrics default on, screensaver dismiss, DB main-thread crash, India content, cover art ([405ad5a](https://github.com/TeamAuraMusic/AuraMusic/commit/405ad5a15def721ccd50177d8ba61ed7eae6cf9e)) — chila254
- feat(tv): screensaver shows video with captions instead of lyrics ([da99fa7](https://github.com/TeamAuraMusic/AuraMusic/commit/da99fa74e9e49917a2b8b70edb44fc96a3536995)) — chila254
- fix(tv): keep-screen-on respects Do Not Sleep on all screens, screensaver always on top ([dc63af6](https://github.com/TeamAuraMusic/AuraMusic/commit/dc63af6e6ffa66a4faba4a110d0cbcb551a1f208)) — chila254
- fix(tv): enhanced lyrics panel not focusable on TV ([2850936](https://github.com/TeamAuraMusic/AuraMusic/commit/285093655c865d317e0cb42f1886ec4c016da60a)) — chila254
- fix(tv): video mode enables instantly on home taps so video loads as fast as search ([5b82f5c](https://github.com/TeamAuraMusic/AuraMusic/commit/5b82f5c5df9e4c75cdfc51d197f7ad2c55eb662b)) — chila254
- fix(tv): screensaver prevents Dream, fetches lyrics/video correctly ([6c241f8](https://github.com/TeamAuraMusic/AuraMusic/commit/6c241f89c33bff710d7a6bf104cda4a542ac1996)) — chila254
- fix(tv): screensaver only for audio, not video ([5a0d962](https://github.com/TeamAuraMusic/AuraMusic/commit/5a0d9626c527857593837bd79fc083d676a2dcf5)) — chila254
- fix(tv): library always shows Liked Songs playlist ([cedc8ac](https://github.com/TeamAuraMusic/AuraMusic/commit/cedc8acfbd46869759097a72738c9c933589fe2f)) — chila254
- fix(tv): library duplicate key crash, regular songs not fetching video, video player surface ([9adca57](https://github.com/TeamAuraMusic/AuraMusic/commit/9adca57a6b54b4dc804abdbc67f0421a1f51c6b8)) — chila254
- fix(tv): stop regular songs fetching video, fix duplicate key crashes in all lazy lists ([7aa4158](https://github.com/TeamAuraMusic/AuraMusic/commit/7aa4158a6bcb5d385cd58f8944061e10578f0275)) — chila254
- fix(tv): video black screen on 2nd re-entry, screensaver for regular songs, explicit badge ([3cd8997](https://github.com/TeamAuraMusic/AuraMusic/commit/3cd8997876ca2f8223e13abafb1ecfcf249049ad)) — chila254
- Binos Translated using Weblate (French) ([9656fa4](https://github.com/TeamAuraMusic/AuraMusic/commit/9656fa4c3086a352a9c8772bcb2897f7a733f79c)) — Mickaël
- fix: liked albums empty, playlist thumbnails missing, null thumbnail fallback icons ([6184dc6](https://github.com/TeamAuraMusic/AuraMusic/commit/6184dc632795f51197974069c6add611c551204f)) — chila254
- fix: remove Discord application_id, fix TV playlist thumbnails, paste release notes from file ([50ca674](https://github.com/TeamAuraMusic/AuraMusic/commit/50ca6740a5b8d6ab235e48709f42796f7dd38f88)) — chila254
- fix(tv): liked playlists show first song thumbnail in library ([9666695](https://github.com/TeamAuraMusic/AuraMusic/commit/9666695f9fcedd0505d3d40cb0ad83359c6d5109)) — chila254
- fix(build): make split APKs actually split by ABI ([e1d2c0e](https://github.com/TeamAuraMusic/AuraMusic/commit/e1d2c0e15edea230df3b3d194dfc5d0045deda90)) — chila254

**Complete commit range:** `e2f86cd` through `e1d2c0e`, inclusive

**Full diff:** https://github.com/TeamAuraMusic/AuraMusic/compare/v3.0.0...e1d2c0e

# AuraMusic v3.0.0 (Build 27) Changelog

# AuraMusic v3.0.0 (Build 27) Release Notes

## What's New

**Full Changelog**: https://github.com/TeamAuraMusic/AuraMusic/compare/...424c5395ab2a6bea6178f9d77452b946c4bfef69

# AuraMusic v3.0.0 (Build 27) Changelog

> [!NOTE]
> This changelog covers all 38 commits from `03d919e` through `e446e6a`, inclusive. No commits in the requested range are omitted.

## Major Features

### AutoMix and Playback
- Added AutoMix settings on mobile and TV.
- Added streamed-song AutoMix and service-owned automatic queue extension.
- Preserved repeat modes and improved trigger scheduling, duration readiness, transition stability, and TV audio handoff.
- Improved radio candidate quality and protected queues from stale asynchronous results.
- Fixed Android Auto discovery with the correct descriptor format.

### Lyrics Providers
- Integrated Paxsenix synchronized lyrics and initialized it during app startup.
- Integrated Musixmatch RichSync, synchronized subtitle, and plain-lyrics fallbacks.
- Fixed Musixmatch request formatting, `text/plain` JSON deserialization, word spacing, provider ordering, and TV presentation.

### Android TV
- Reworked player, top bar, back navigation, overlays, D-pad focus, mini-player sizing, and section navigation.
- Improved video rendering, loading, seeking, surface restoration, and song transitions.
- Added persistent expanded lyrics, expanded video, a YouTube Music-synced Like button, and a live AutoMix queue.
- Added full-screen focused artwork to the Library.
- Added Last.fm settings with authentication, scrobbling, now-playing, and like synchronization.
- Improved TV home filtering, recommendations, search, settings navigation, and Android 14 playback stability.

### Accounts, Updates, and Distribution
- Improved YouTube Music login persistence on mobile and TV.
- Fixed Discord OAuth scope handling and updated the Discord invite.
- Added standalone ABI release flavors and canonical FOSS, GMS, TV, ARM64, Armeabi, x86, and x86_64 APK assets.
- Updated updater architecture detection, installed-variant labels, workflow signing, and release artifact selection.

### Localization
- Added Korean app and store-listing translations by @윤성.

## Full Changelog — All 38 Commits

- chore(release): prepare v2.9.0 (Build 26) - Discord OAuth2 rewrite, video/SponsorBlock fixes ([03d919e](https://github.com/TeamAuraMusic/AuraMusic/commit/03d919efb4fcc2840fa7d5a20972b886229a66c2)) — chila254
- chore(release): v2.9.0 (build 26) ([a70ddde](https://github.com/TeamAuraMusic/AuraMusic/commit/a70dddefa61fcaf69a6a3bc3cd67d11b8c07502a)) — TeamAuraMusic
- Added translation using Weblate (Korean) ([2894a09](https://github.com/TeamAuraMusic/AuraMusic/commit/2894a090686a8d21a5a4bdf09ef8d2e15de033f3)) — 윤성
- Translated using Weblate (Korean) ([b336081](https://github.com/TeamAuraMusic/AuraMusic/commit/b3360814ee28810671578f4ccadce9c23e47d418)) — 윤성
- Translated using Weblate (Korean) ([ec40ccd](https://github.com/TeamAuraMusic/AuraMusic/commit/ec40ccd9e926d8f79e0705aeaae3ae70e2e30002)) — 윤성
- fix(tv): compact mini player, preserve the top bar, stop music on exit, restore video position, and improve buffering state ([e5d9692](https://github.com/TeamAuraMusic/AuraMusic/commit/e5d9692c2093784d2a9c0269ff5f8afd6b468d78)) — chila254
- fix(tv): hide mini player in player screen, reset overlays from navigation, move back below top bar, and compact detail panel ([3594e11](https://github.com/TeamAuraMusic/AuraMusic/commit/3594e11038ed47863f06e692a5e448fb4ff6cd19)) — chila254
- revert(tv): restore detail panel to 360dp with original padding ([0c79b41](https://github.com/TeamAuraMusic/AuraMusic/commit/0c79b41298121a64b6cfa83d444f15210905af21)) — chila254
- fix(tv): prevent false video loading when returning to the player ([8a92956](https://github.com/TeamAuraMusic/AuraMusic/commit/8a9295619a2d0528b2437b1dd905d4438419c5ec)) — chila254
- fix(tv): keep PlayerView visible in video mode and overlay loading state ([e8fd37e](https://github.com/TeamAuraMusic/AuraMusic/commit/e8fd37e11f59cf0838d872367f11a3839219888b)) — chila254
- fix(tv): stabilize playback/navigation, overlay reset, focus, back behavior, and top-level selection ([ee5d5be](https://github.com/TeamAuraMusic/AuraMusic/commit/ee5d5befbdac7d495135a2b2be6542a2c5a96bc8)) — chila254
- fix(tv): move player back button into the top bar ([f2ae851](https://github.com/TeamAuraMusic/AuraMusic/commit/f2ae851b50d27676e8e42cc80f08804fdc9eb721)) — chila254
- fix(tv): resolve TvPlayer and BackHandler build errors ([eafeefe](https://github.com/TeamAuraMusic/AuraMusic/commit/eafeefe075ed6fe82bbe678ecec5098ec3cfd8d2)) — chila254
- fix(tv): restore missing TvPlayer closing brace ([f8a7d4a](https://github.com/TeamAuraMusic/AuraMusic/commit/f8a7d4ad7cee80cb8115618a813d275149c12c3b)) — chila254
- fix(tv): improve D-pad player focus and restore video surfaces after navigation ([5b664d6](https://github.com/TeamAuraMusic/AuraMusic/commit/5b664d667554737f57fb520ab6b5eb342cdec85b)) — chila254
- Fixed Android Auto by using the correct descriptor format ([b1b3b7d](https://github.com/TeamAuraMusic/AuraMusic/commit/b1b3b7d0ed86f6157a86e2b1a33ee670c424d47e)) — chila254
- feat(lyrics): integrate Paxsenix synchronized Apple Music lyrics ([1eaa547](https://github.com/TeamAuraMusic/AuraMusic/commit/1eaa54719377ce943ff3e2ce4e540bd49da96f79)) — chila254
- feat(tv): add lyrics expansion, Last.fm navigation, and Paxsenix settings support ([2c2f45e](https://github.com/TeamAuraMusic/AuraMusic/commit/2c2f45e2b5334b5c7f13aacafb8c320c4d80f324)) — chila254
- fix(lyrics): initialize Paxsenix synchronously and filter empty/null home sections ([02d5123](https://github.com/TeamAuraMusic/AuraMusic/commit/02d51235c739e8a5c0c3e28deb91da7bed539419)) — chila254
- fix: repair HomeViewModel toggleChip syntax ([a26df25](https://github.com/TeamAuraMusic/AuraMusic/commit/a26df2580cddd75f5dfa4e08165552ada14902ce)) — chila254
- feat(tv): add complete Last.fm settings, scrobbling, now-playing, and like sync ([f071acc](https://github.com/TeamAuraMusic/AuraMusic/commit/f071acc683a7944327f523a81c08f700d51b6459)) — chila254
- fix(tv): repair Last.fm settings compilation and navigation ([9e85824](https://github.com/TeamAuraMusic/AuraMusic/commit/9e858241cbdf34ffe461ed9f2598844de853b7c5)) — chila254
- fix: add missing AutoMix preference import in MusicService ([9983807](https://github.com/TeamAuraMusic/AuraMusic/commit/99838078f788bc016030fa715aa24e1462821efa)) — chila254
- fix(automix): preserve settings, correct trigger calculation, and retry duration readiness ([0289236](https://github.com/TeamAuraMusic/AuraMusic/commit/0289236b3c45e5bed9a04e2161a19aae2b2ea664)) — chila254
- feat(settings): add AutoMix controls to mobile and TV ([a783882](https://github.com/TeamAuraMusic/AuraMusic/commit/a7838829e34db9d484cdd7252e6a09a475d2363f)) — chila254
- fix: add missing AutoMix preference import in TV settings ([8d46dbf](https://github.com/TeamAuraMusic/AuraMusic/commit/8d46dbfc79c09afc952f3dc452223917778f2675)) — chila254
- fix(automix): move crossfade trigger from 80% to 95% ([6cee48a](https://github.com/TeamAuraMusic/AuraMusic/commit/6cee48a23fe48f1b71be260c27aa40cc76b232a8)) — chila254
- fix: improve AutoMix, radio suggestions, TV UI/recommendations, and login persistence ([dec1276](https://github.com/TeamAuraMusic/AuraMusic/commit/dec12765ab22ceb10bedfd944d0995f086b678c4)) — chila254
- feat(lyrics): integrate Musixmatch with synchronized lyrics ([87f23ce](https://github.com/TeamAuraMusic/AuraMusic/commit/87f23ce29f5757185b4b5dc28dc2e64b06c0acfc)) — chila254
- fix(playback): support streamed AutoMix, preserve repeat modes, and repair TV lyrics/Last.fm navigation ([ac6f810](https://github.com/TeamAuraMusic/AuraMusic/commit/ac6f81053bbe6b1017b0d9e5779294df4ddf747c)) — chila254
- fix(lyrics): add Musixmatch `format=json` requests ([a1cd7ba](https://github.com/TeamAuraMusic/AuraMusic/commit/a1cd7ba8689fa42e9ab319ac12a7efb246fd0123)) — chila254
- Implement the release architecture/flavor contract across CI and the updater ([daac1c5](https://github.com/TeamAuraMusic/AuraMusic/commit/daac1c5d15492e4eca9d6bac830265c6df5c1547)) — chila254
- fix(tv): stabilize playback transitions, restore lyrics toggle, and align release variants ([e76a0fd](https://github.com/TeamAuraMusic/AuraMusic/commit/e76a0fd3fa81e488f5d598b444b86a53fbd98124)) — chila254
- fix(updater): show installed APK details and repair release signing ([32df5d9](https://github.com/TeamAuraMusic/AuraMusic/commit/32df5d93abf371add66cfacce03a42b947461046)) — chila254
- fix(lyrics): deserialize Musixmatch text responses as JSON ([3c7bc8c](https://github.com/TeamAuraMusic/AuraMusic/commit/3c7bc8cda32f8831b1bd3f47aeb84f91edc45853)) — chila254
- fix(lyrics): restore spacing, persist TV view, and update queue ([a84c88f](https://github.com/TeamAuraMusic/AuraMusic/commit/a84c88f7880cfa23ad17e71af56cf4b309c07742)) — chila254
- fix(tv): stabilize playback UI and improve player controls ([84f6619](https://github.com/TeamAuraMusic/AuraMusic/commit/84f6619ff07ed04263891001af3bdb1582eaa289)) — chila254
- fix(tv): preserve lyrics, smooth AutoMix, and add Library backdrops ([e446e6a](https://github.com/TeamAuraMusic/AuraMusic/commit/e446e6ab46b55bfcb969de55f70eedb41557915d)) — chila254

**Complete commit range:** `03d919e` through `e446e6a`, inclusive

**Full diff:** https://github.com/TeamAuraMusic/AuraMusic/compare/2578c64c0966569721aba26f46aba4b914bf4245...e446e6ab46b55bfcb969de55f70eedb41557915d

# AuraMusic v2.9.0 (Build 26) Changelog

# AuraMusic v2.9.0 (Build 26) Release Notes

## What's New

**Full Changelog**: https://github.com/TeamAuraMusic/AuraMusic/compare/...03d919efb4fcc2840fa7d5a20972b886229a66c2

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
