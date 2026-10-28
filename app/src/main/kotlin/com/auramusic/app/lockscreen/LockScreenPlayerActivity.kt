/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.lockscreen

import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.auramusic.app.LocalDatabase
import com.auramusic.app.LocalPlayerConnection
import com.auramusic.app.R
import com.auramusic.app.constants.LockScreenPlayerExpandedKey
import com.auramusic.app.constants.LockScreenPlayerLyricsKey
import com.auramusic.app.db.MusicDatabase
import com.auramusic.app.db.entities.LyricsEntity
import com.auramusic.app.extensions.toggleRepeatMode
import com.auramusic.app.lyrics.LyricsHelper
import com.auramusic.app.playback.MusicService
import com.auramusic.app.playback.PlayerConnection
import com.auramusic.app.ui.component.Lyrics
import com.auramusic.app.utils.makeTimeString
import com.auramusic.app.utils.rememberPreference
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AuraMusic's own lock screen player: a compact pill over the lock screen that expands into a
 * full player with lyrics, the way One UI's Now Bar does. It is drawn over the keyguard but
 * never dismisses it: opening the app asks for the PIN or fingerprint first.
 */
@AndroidEntryPoint
class LockScreenPlayerActivity : ComponentActivity() {

    @Inject lateinit var database: MusicDatabase
    @Inject lateinit var lyricsHelper: LyricsHelper

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MusicService.MusicBinder) {
                playerConnection = runCatching {
                    PlayerConnection(this@LockScreenPlayerActivity, service, database, lifecycleScope)
                }.getOrNull()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playerConnection?.dispose()
            playerConnection = null
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        )

        setContent {
            val connection = playerConnection
            MaterialTheme(colorScheme = darkColorScheme()) {
                if (connection != null) {
                    CompositionLocalProvider(
                        LocalPlayerConnection provides connection,
                        LocalDatabase provides database,
                    ) {
                        LockScreenPlayer(
                            connection = connection,
                            onDismiss = ::finish,
                            onOpenApp = ::openApp,
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        bindService(Intent(this, MusicService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        runCatching { unbindService(serviceConnection) }
        playerConnection?.dispose()
        playerConnection = null
        super.onStop()
        // Once the user unlocks or leaves, this screen has done its job.
        if (!isChangingConfigurations) finish()
    }

    private fun openApp() {
        val launch = packageManager.getLaunchIntentForPackage(packageName) ?: return
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && keyguard?.isKeyguardLocked == true) {
            keyguard.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    startActivity(launch)
                    finish()
                }
            })
        } else {
            startActivity(launch)
            finish()
        }
    }

    /** Fetches lyrics the same way the full player does, so the lock screen is not left empty. */
    internal fun ensureLyrics(connection: PlayerConnection) {
        val metadata = connection.mediaMetadata.value ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            if (database.lyrics(metadata.id).first() != null) return@launch
            val fetched = runCatching { lyricsHelper.getLyrics(metadata) }.getOrNull() ?: return@launch
            if (database.lyrics(metadata.id).first() == null) {
                database.query { upsert(LyricsEntity(metadata.id, fetched.lyrics, fetched.provider)) }
            }
        }
    }

    companion object {
        fun intent(context: Context) =
            Intent(context, LockScreenPlayerActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
    }
}

@Composable
private fun LockScreenPlayer(
    connection: PlayerConnection,
    onDismiss: () -> Unit,
    onOpenApp: () -> Unit,
) {
    val activity = androidx.compose.ui.platform.LocalContext.current as LockScreenPlayerActivity
    val metadata by connection.mediaMetadata.collectAsState()
    val isPlaying by connection.isEffectivelyPlaying.collectAsState()
    var expanded by rememberPreference(LockScreenPlayerExpandedKey, false)
    val showLyrics by rememberPreference(LockScreenPlayerLyricsKey, true)

    BackHandler(enabled = expanded) { expanded = false }

    LaunchedEffect(metadata?.id, showLyrics) {
        if (showLyrics) activity.ensureLyrics(connection)
    }

    // Nothing to show once the queue is empty.
    if (metadata == null) {
        LaunchedEffect(Unit) { delay(1500); if (connection.mediaMetadata.value == null) onDismiss() }
        return
    }
    val song = metadata!!

    AnimatedContent(
        targetState = expanded,
        transitionSpec = {
            (fadeIn() + slideInVertically { it / 6 }) togetherWith (fadeOut() + slideOutVertically { it / 6 })
        },
        label = "lockScreenPlayer",
    ) { isExpanded ->
        if (isExpanded) {
            ExpandedPlayer(
                connection = connection,
                title = song.title,
                artist = song.artists.joinToString { it.name },
                artwork = song.thumbnailUrl,
                isPlaying = isPlaying,
                showLyrics = showLyrics,
                onCollapse = { expanded = false },
                onOpenApp = onOpenApp,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // Tapping the empty lock screen area hands it back to the system lock screen.
                    .clickable(interactionSource = null, indication = null, onClick = onDismiss),
                contentAlignment = Alignment.BottomCenter,
            ) {
                NowBarPill(
                    title = song.title,
                    artist = song.artists.joinToString { it.name },
                    artwork = song.thumbnailUrl,
                    isPlaying = isPlaying,
                    onExpand = { expanded = true },
                    onPlayPause = connection::togglePlayPause,
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun NowBarPill(
    title: String,
    artist: String,
    artwork: String?,
    isPlaying: Boolean,
    onExpand: () -> Unit,
    onPlayPause: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 96.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(36.dp))
            .background(Color(0xE6202024))
            .clickable(onClick = onExpand)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -12) onExpand() else if (dragAmount > 12) onDismiss()
                }
            }
            .padding(8.dp),
    ) {
        AsyncImage(
            model = artwork,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(artist, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        FilledIconButton(
            onClick = onPlayPause,
            colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Color.Black),
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
            )
        }
    }
}

@Composable
private fun ExpandedPlayer(
    connection: PlayerConnection,
    title: String,
    artist: String,
    artwork: String?,
    isPlaying: Boolean,
    showLyrics: Boolean,
    onCollapse: () -> Unit,
    onOpenApp: () -> Unit,
) {
    val lyrics by connection.currentLyrics.collectAsState(initial = null)
    val hasLyrics = showLyrics && lyrics?.lyrics?.let { it.isNotBlank() && it != LyricsEntity.LYRICS_NOT_FOUND } == true
    val currentSong by connection.currentSong.collectAsState(initial = null)
    val liked = currentSong?.song?.liked == true
    val shuffle by connection.shuffleModeEnabled.collectAsState()
    val repeatMode by connection.repeatMode.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AsyncImage(
            model = artwork,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().blur(80.dp),
            alpha = 0.55f,
        )
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount -> if (dragAmount > 24) onCollapse() }
                },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onCollapse) {
                    Icon(painterResource(R.drawable.expand_more), contentDescription = stringResource(R.string.lock_screen_player_collapse), tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onOpenApp) {
                    Icon(painterResource(R.drawable.lock_open), contentDescription = stringResource(R.string.lock_screen_player_open_app), tint = Color.White)
                }
            }

            // Lyrics take the stage when there are any; otherwise the artwork does.
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (hasLyrics) {
                    ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center, color = Color.White)) {
                        Lyrics(
                            sliderPositionProvider = { null },
                            modifier = Modifier.fillMaxSize(),
                            showLyrics = true,
                            disableInteractiveFeatures = true,
                        )
                    }
                } else {
                    AsyncImage(
                        model = artwork,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(24.dp)),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                if (hasLyrics) {
                    AsyncImage(
                        model = artwork,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)),
                    )
                    Spacer(Modifier.size(12.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(artist, color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = connection::toggleLike) {
                    Icon(
                        painterResource(if (liked) R.drawable.favorite else R.drawable.favorite_border),
                        contentDescription = stringResource(if (liked) R.string.action_remove_like else R.string.action_like),
                        tint = if (liked) MaterialTheme.colorScheme.error else Color.White,
                    )
                }
            }

            SeekBar(connection)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            ) {
                IconButton(onClick = connection::toggleShuffle) {
                    Icon(
                        painterResource(if (shuffle) R.drawable.shuffle_on else R.drawable.shuffle),
                        contentDescription = stringResource(R.string.shuffle),
                        tint = Color.White.copy(alpha = if (shuffle) 1f else 0.6f),
                    )
                }
                IconButton(onClick = connection::seekToPrevious, modifier = Modifier.size(56.dp)) {
                    Icon(painterResource(R.drawable.skip_previous), contentDescription = stringResource(R.string.previous), tint = Color.White, modifier = Modifier.size(36.dp))
                }
                FilledIconButton(
                    onClick = connection::togglePlayPause,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White, contentColor = Color.Black),
                    modifier = Modifier.size(72.dp),
                ) {
                    Icon(
                        painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                        contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                        modifier = Modifier.size(36.dp),
                    )
                }
                IconButton(onClick = connection::seekToNext, modifier = Modifier.size(56.dp)) {
                    Icon(painterResource(R.drawable.skip_next), contentDescription = stringResource(R.string.next), tint = Color.White, modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = { connection.player.toggleRepeatMode() }) {
                    Icon(
                        painterResource(
                            when (repeatMode) {
                                Player.REPEAT_MODE_ONE -> R.drawable.repeat_one_on
                                Player.REPEAT_MODE_ALL -> R.drawable.repeat_on
                                else -> R.drawable.repeat
                            },
                        ),
                        contentDescription = stringResource(R.string.repeat),
                        tint = Color.White.copy(alpha = if (repeatMode == Player.REPEAT_MODE_OFF) 0.6f else 1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SeekBar(connection: PlayerConnection) {
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(connection) {
        while (true) {
            val player = connection.player
            if (!dragging) position = player.currentPosition
            duration = player.duration.coerceAtLeast(0L)
            delay(500)
        }
    }

    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Slider(
            value = if (dragging) dragValue else position.toFloat(),
            valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
            onValueChange = { dragging = true; dragValue = it },
            onValueChangeFinished = {
                connection.seekTo(dragValue.toLong())
                position = dragValue.toLong()
                dragging = false
            },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.25f),
            ),
        )
        Row(Modifier.fillMaxWidth()) {
            Text(makeTimeString(if (dragging) dragValue.toLong() else position), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Text(makeTimeString(duration), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        }
    }
}
