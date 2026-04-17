package com.auramusic.app.voice

import android.content.Context
import android.media.AudioManager
import androidx.core.net.toUri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.auramusic.app.playback.ExoDownloadService
import com.auramusic.app.playback.PlayerConnection
import com.auramusic.app.playback.queues.YouTubeQueue
import com.auramusic.app.utils.dataStore
import com.auramusic.app.models.toMediaMetadata
import com.auramusic.innertube.YouTube
import com.auramusic.innertube.models.SongItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

object VoiceCommandActionExecutor {

    suspend fun execute(
        command: VoiceCommand,
        playerConnection: PlayerConnection?,
        onSearch: (String) -> Unit,
        onNavigate: (String) -> Unit,
    ): String = withContext(Dispatchers.Main) {
        when (command) {
            is VoiceCommand.Search -> {
                onSearch(command.query)
                "Searching for \"${command.query}\""
            }
            is VoiceCommand.WakeWordDetected -> "Listening..."
            is VoiceCommand.Unknown -> "I didn't understand that"
            is VoiceCommand.PlaySearch -> {
                val conn = playerConnection ?: return@withContext "No player connected"
                val result = withContext(Dispatchers.IO) {
                    YouTube.search(command.query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                }
                val firstSong = result?.items?.filterIsInstance<SongItem>()?.firstOrNull()
                if (firstSong != null) {
                    val metadata = firstSong.toMediaMetadata()
                    conn.playQueue(YouTubeQueue.radio(metadata))
                    "Playing ${firstSong.title}"
                } else {
                    "No results found for \"${command.query}\""
                }
            }

            // Navigation
            is VoiceCommand.ShowQueue -> { onNavigate("queue"); "Opening queue" }
            is VoiceCommand.OpenHome -> { onNavigate("home"); "Opening home" }
            is VoiceCommand.OpenLibrary -> { onNavigate("library"); "Opening library" }
            is VoiceCommand.OpenSearch -> { onNavigate("search"); "Opening search" }
            is VoiceCommand.OpenSettings -> { onNavigate("settings"); "Opening settings" }
            
            // Playback commands
            else -> {
                val conn = playerConnection ?: return@withContext "No player connected"
                executePlaybackCommand(command, conn)
            }
        }
    }

    private suspend fun executePlaybackCommand(
        command: VoiceCommand,
        conn: PlayerConnection,
    ): String {
        val player = conn.player
        val context = conn.service as? Context ?: return "Error"

        return when (command) {
            is VoiceCommand.Play -> {
                if (player.playbackState == ExoPlayer.STATE_IDLE) player.prepare()
                player.playWhenReady = true
                "Playing"
            }
            is VoiceCommand.Pause -> {
                player.playWhenReady = false
                "Paused"
            }
            is VoiceCommand.TogglePlayPause -> {
                if (player.isPlaying) {
                    player.playWhenReady = false
                    "Paused"
                } else {
                    if (player.playbackState == ExoPlayer.STATE_IDLE) player.prepare()
                    player.playWhenReady = true
                    "Playing"
                }
            }
            is VoiceCommand.Next -> { conn.seekToNext(); "Next track" }
            is VoiceCommand.Previous -> { conn.seekToPrevious(); "Previous track" }
            is VoiceCommand.Shuffle -> {
                val current = conn.shuffleModeEnabled.value
                player.shuffleModeEnabled = !current
                if (!current) "Shuffle on" else "Shuffle off"
            }
            is VoiceCommand.ShuffleOn -> { player.shuffleModeEnabled = true; "Shuffle on" }
            is VoiceCommand.ShuffleOff -> { player.shuffleModeEnabled = false; "Shuffle off" }
            is VoiceCommand.Repeat -> {
                player.repeatMode = when (player.repeatMode) {
                    ExoPlayer.REPEAT_MODE_OFF -> ExoPlayer.REPEAT_MODE_ALL
                    ExoPlayer.REPEAT_MODE_ALL -> ExoPlayer.REPEAT_MODE_ONE
                    else -> ExoPlayer.REPEAT_MODE_OFF
                }
                when (player.repeatMode) {
                    ExoPlayer.REPEAT_MODE_ONE -> "Repeat one"
                    ExoPlayer.REPEAT_MODE_ALL -> "Repeat all"
                    else -> "Repeat off"
                }
            }
            is VoiceCommand.RepeatOne -> { player.repeatMode = ExoPlayer.REPEAT_MODE_ONE; "Repeat one" }
            is VoiceCommand.RepeatAll -> { player.repeatMode = ExoPlayer.REPEAT_MODE_ALL; "Repeat all" }
            is VoiceCommand.RepeatOff -> { player.repeatMode = ExoPlayer.REPEAT_MODE_OFF; "Repeat off" }
            is VoiceCommand.SeekForward -> {
                val newPos = (player.currentPosition + command.milliseconds).coerceAtMost(player.duration)
                player.seekTo(newPos)
                "Skipped forward"
            }
            is VoiceCommand.SeekBackward -> {
                val newPos = (player.currentPosition - command.milliseconds).coerceAtLeast(0)
                player.seekTo(newPos)
                "Skipped backward"
            }
            is VoiceCommand.VolumeUp -> {
                try {
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    audioManager.adjustVolume(AudioManager.ADJUST_RAISE, 0)
                } catch (_: Exception) {
                    player.volume = (player.volume + 0.1f).coerceAtMost(1f)
                }
                "Volume up"
            }
            is VoiceCommand.VolumeDown -> {
                try {
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    audioManager.adjustVolume(AudioManager.ADJUST_LOWER, 0)
                } catch (_: Exception) {
                    player.volume = (player.volume - 0.1f).coerceAtLeast(0f)
                }
                "Volume down"
            }
            is VoiceCommand.Mute -> { conn.setMuted(true); "Muted" }
            is VoiceCommand.Unmute -> { conn.setMuted(false); "Unmuted" }
            is VoiceCommand.SpeedUp -> {
                val newSpeed = (player.playbackParameters.speed * 1.25f).coerceAtMost(2.0f)
                player.setPlaybackSpeed(newSpeed)
                "Speed up"
            }
            is VoiceCommand.SlowDown -> {
                val newSpeed = (player.playbackParameters.speed * 0.75f).coerceAtLeast(0.5f)
                player.setPlaybackSpeed(newSpeed)
                "Slow down"
            }
            is VoiceCommand.ResetSpeed -> { player.setPlaybackSpeed(1.0f); "Normal speed" }
            is VoiceCommand.ToggleLike -> { conn.toggleLike(); "Toggled like" }
            is VoiceCommand.ClearQueue -> { player.clearMediaItems(); "Queue cleared" }
            is VoiceCommand.AddToQueue -> "Added to queue"
            
            // Settings
            is VoiceCommand.SetDarkMode -> {
                val darkModeKey = stringPreferencesKey("darkMode")
                context.dataStore.edit { prefs ->
                    prefs[darkModeKey] = if (command.enabled) "ON" else "OFF"
                }
                if (command.enabled) "Dark mode on" else "Light mode on"
            }
            is VoiceCommand.ToggleTheme -> {
                val darkModeKey = stringPreferencesKey("darkMode")
                val current = context.dataStore.data.map { prefs -> prefs[darkModeKey] ?: "AUTO" }.first()
                val newMode = when (current) {
                    "AUTO" -> "ON"
                    "ON" -> "OFF"
                    "OFF" -> "AUTO"
                    else -> "AUTO"
                }
                context.dataStore.edit { prefs -> prefs[darkModeKey] = newMode }
                "Theme toggled"
            }
            is VoiceCommand.ShowLyrics -> {
                val key = booleanPreferencesKey("showLyrics")
                context.dataStore.edit { it[key] = true }
                "Lyrics shown"
            }
            is VoiceCommand.HideLyrics -> {
                val key = booleanPreferencesKey("showLyrics")
                context.dataStore.edit { it[key] = false }
                "Lyrics hidden"
            }
            is VoiceCommand.ToggleLyrics -> {
                val key = booleanPreferencesKey("showLyrics")
                val current = context.dataStore.data.map { it[key] ?: false }.first()
                context.dataStore.edit { it[key] = !current }
                "Lyrics toggled"
            }
            is VoiceCommand.EnableVideo -> { conn.toggleVideoMode(); "Video on" }
            is VoiceCommand.DisableVideo -> { conn.toggleVideoMode(); "Video off" }
            is VoiceCommand.ToggleVideo -> { conn.toggleVideoMode(); "Video toggled" }

            // Download commands
            is VoiceCommand.DownloadCurrentSong -> {
                val service = conn.service
                val mediaMetadata = service.currentMediaMetadata.value
                if (mediaMetadata == null) {
                    "No song is currently playing"
                } else {
                    val songId = mediaMetadata.id
                    val isDownloaded = withContext(Dispatchers.IO) {
                        service.database.song(songId).first()?.song?.isDownloaded ?: false
                    }
                    if (isDownloaded) {
                        "This song is already downloaded"
                    } else {
                        val downloadRequest = DownloadRequest.Builder(songId, songId.toUri())
                            .setCustomCacheKey(songId)
                            .setData(mediaMetadata.title.toByteArray())
                            .build()
                        DownloadService.sendAddDownload(
                            service,
                            ExoDownloadService::class.java,
                            downloadRequest,
                            false,
                        )
                        "Downloading \"${mediaMetadata.title}\""
                    }
                }
            }
            is VoiceCommand.DownloadCurrentPlaylist -> {
                val service = conn.service
                val player = service.player
                val mediaItems = mutableListOf<androidx.media3.common.MediaItem>()
                for (i in 0 until player.mediaItemCount) {
                    player.getMediaItemAt(i)?.let { mediaItems.add(it) }
                }
                if (mediaItems.size <= 1) {
                    return "The queue is empty or has only one song"
                }
                var downloadCount = 0
                val skippedList = mutableListOf<String>()
                mediaItems.forEach { item ->
                    val songId = item.mediaId
                    val isDownloaded = withContext(Dispatchers.IO) {
                        service.database.song(songId).first()?.song?.isDownloaded ?: false
                    }
                    if (!isDownloaded) {
                        val downloadRequest = DownloadRequest.Builder(songId, songId.toUri())
                            .setCustomCacheKey(songId)
                            .setData(item.mediaMetadata?.title?.toString()?.toByteArray() ?: "download".toByteArray())
                            .build()
                        DownloadService.sendAddDownload(
                            service,
                            ExoDownloadService::class.java,
                            downloadRequest,
                            false,
                        )
                        downloadCount++
                    } else {
                        item.mediaMetadata?.title?.toString()?.let { skippedList.add(it) }
                    }
                }
                val skipped = skippedList.size
                when {
                    downloadCount > 0 -> "Downloading $downloadCount songs to queue" + if (skipped > 0) " ($skipped already downloaded)" else ""
                    skipped == mediaItems.size -> "All songs already downloaded"
                    else -> "No songs to download"
                }
            }
            is VoiceCommand.DownloadCurrentAlbum -> {
                val service = conn.service
                val mediaMetadata = service.currentMediaMetadata.value
                if (mediaMetadata?.album == null) {
                    return "Current song has no album information"
                }
                val albumId = mediaMetadata.album.id
                val albumSongsFlow = service.database.albumSongs(albumId)
                val albumSongs = albumSongsFlow.first()
                if (albumSongs.isEmpty()) {
                    return "No songs found in album"
                }
                var downloadCount = 0
                albumSongs.forEach { song ->
                    val songId = song.song.id
                    val isDownloaded = withContext(Dispatchers.IO) {
                        service.database.song(songId).first()?.song?.isDownloaded ?: false
                    }
                    if (!isDownloaded) {
                        val downloadRequest = DownloadRequest.Builder(songId, songId.toUri())
                            .setCustomCacheKey(songId)
                            .setData(song.song.title.toByteArray())
                            .build()
                        DownloadService.sendAddDownload(
                            service,
                            ExoDownloadService::class.java,
                            downloadRequest,
                            false,
                        )
                        downloadCount++
                    }
                }
                val total = albumSongs.size
                val already = total - downloadCount
                when {
                    downloadCount > 0 -> "Downloading album \"${mediaMetadata.album.title}\": $downloadCount songs" + if (already > 0) " ($already already downloaded)" else ""
                    else -> "All songs in this album already downloaded"
                }
            }

            else -> "Done"
        }
    }
}
