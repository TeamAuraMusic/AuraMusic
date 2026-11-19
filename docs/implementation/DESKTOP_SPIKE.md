# Desktop spike (Windows, libmpv)

Phase 0 of the desktop plan. This is a **Compose Desktop** app that reuses the
Android project's portable InnerTube sources. It is not a port of the mobile UI
yet; it proves the backend, playback and release path.

## What is working

- `:innertube-jvm` compiles the existing `innertube/src/main/kotlin` sources on
  the JVM with no changes to the Android app. Timber is AAR-only, so the tiny
  `:timber-jvm` module supplies the log calls InnerTube uses.
- `:desktop` is a Compose Desktop window with song search, stream resolution
  through NewPipeExtractor, and audio playback through **libmpv via JNA**.
- Playback is audio-only (`vo=null`) in this spike. Play/pause, seek, stop and
  volume are wired; the window shows position/duration and the shared version.
- Android and desktop read one version from `gradle.properties`
  (`auramusic.versionName`, `auramusic.versionCode`). The desktop installer uses
  the same version name, and the build number is generated into
  `DesktopBuildInfo` and shown in the UI.
- `build.yml` builds a Windows desktop app on every push; `release.yml` builds
  an MSI + EXE on `windows-latest` and attaches them to the same GitHub release
  as the APKs.

## Windows setup

libmpv is not bundled yet. Install a Windows build of libmpv (for example from
an MSYS2 `mingw-w64-x86_64-libmpv` install or an mpv release containing
`mpv-2.dll` / `libmpv-2.dll`), then either:

- set `MPV_LIBRARY` to the absolute DLL path, or
- pass `-Dauramusic.mpv.library=<absolute DLL path>` to the JVM, or
- drop `mpv-2.dll` into `desktop/resources/windows/` before packaging. Compose
  Desktop copies that directory into the app resources and exposes it through
  `compose.application.resources.dir`, which the JNA loader checks.

Without the DLL the window still opens and reports "libmpv unavailable" instead
of crashing.

## Build and run

```powershell
./gradlew.bat :desktop:run                    # needs a GUI session
./gradlew.bat :desktop:packageReleaseExe      # Windows installer -> binaries/main-release/exe
./gradlew.bat :desktop:packageReleaseMsi      # Windows MSI -> binaries/main-release/msi
```

The Gradle build also works on other hosts for compilation:

```sh
./gradlew :desktop:compileKotlin
```

## Release/version flow

`release.yml` derives the version name from the tag (or the manual input) and
the build number from `gradle.properties`, writes both back to
`gradle.properties`, and commits that file. The desktop job uses the same
values, so the installer, the APKs, `RELEASE_NOTES.md` and `CHANGELOG.md` all
describe the same release.

## Next steps

1. Bundle libmpv in the Windows installer (and add macOS/Linux targets).
2. Desktop-owned shell: sidebar navigation, library, playlists, settings.
3. Move shared repositories/queue logic into a `:core` module.
4. Add media keys, tray, updates, Discord Rich Presence, scrobbling.
5. Video: host libmpv video output in a Compose/Swing surface; keep a single
   video pipeline and mux separate audio/video streams when needed.
