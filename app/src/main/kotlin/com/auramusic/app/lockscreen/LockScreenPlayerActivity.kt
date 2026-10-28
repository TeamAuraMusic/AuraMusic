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
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaRouter2
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.format.DateFormat
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.auramusic.app.LocalDatabase
import com.auramusic.app.LocalPlayerConnection
import com.auramusic.app.R
import com.auramusic.app.constants.LockScreenPlayerExpandedKey
import com.auramusic.app.constants.LockScreenPlayerLyricsKey
import com.auramusic.app.db.MusicDatabase
import com.auramusic.app.db.entities.LyricsEntity
import com.auramusic.app.extensions.metadata
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
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * AuraMusic's own lock screen player, modelled on One UI's Now Bar: a compact pill under a
 * lock screen style clock, which expands into a full player with lyrics, the queue and a
 * waveform seek bar. It is drawn over the keyguard but never dismisses it: opening the app
 * asks for the PIN or fingerprint first.
 *
 * Android hides its own lock screen (clock, notifications, the system media player) while
 * any app is shown on it, so the compact view draws its own clock, and tapping the empty
 * area hands the screen straight back to the system lock screen.
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
        WindowCompat.setDecorFitsSystemWindows(window, false)

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

private val Scrim = Color(0xFF0B0B0E)

@Composable
private fun LockScreenPlayer(
    connection: PlayerConnection,
    onDismiss: () -> Unit,
    onOpenApp: () -> Unit,
) {
    val activity = LocalContext.current as LockScreenPlayerActivity
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
    val accent by animateColorAsState(rememberArtworkAccent(song.thumbnailUrl), tween(600), label = "accent")

    Box(Modifier.fillMaxSize().background(Scrim)) {
        // Blurred artwork wash behind both views, tinted with the artwork's own colour.
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = if (expanded) 0.6f else 0.35f,
            modifier = Modifier.fillMaxSize().blur(90.dp),
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(accent.copy(alpha = 0.28f), Scrim.copy(alpha = 0.55f), Scrim.copy(alpha = 0.92f)),
                ),
            ),
        )

        AnimatedContent(
            targetState = expanded,
            transitionSpec = {
                (fadeIn(tween(250)) + slideInVertically { it / 8 }) togetherWith
                    (fadeOut(tween(200)) + slideOutVertically { it / 8 })
            },
            label = "lockScreenPlayer",
        ) { isExpanded ->
            if (isExpanded) {
                ExpandedPlayer(
                    connection = connection,
                    title = song.title,
                    artist = song.artists.joinToString { it.name },
                    artwork = song.thumbnailUrl,
                    songId = song.id,
                    accent = accent,
                    isPlaying = isPlaying,
                    onCollapse = { expanded = false },
                    onOpenApp = onOpenApp,
                )
            } else {
                CompactView(
                    connection = connection,
                    title = song.title,
                    artist = song.artists.joinToString { it.name },
                    artwork = song.thumbnailUrl,
                    accent = accent,
                    isPlaying = isPlaying,
                    onExpand = { expanded = true },
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

/** The artwork's most vivid colour, used to tint the whole player like One UI does. */
@Composable
private fun rememberArtworkAccent(url: String?): Color {
    val context = LocalContext.current
    var color by remember { mutableStateOf(Color(0xFF8AB4F8)) }
    LaunchedEffect(url) {
        if (url == null) return@LaunchedEffect
        val bitmap = withContext(Dispatchers.IO) {
            val request = ImageRequest.Builder(context).data(url).size(128).allowHardware(false).build()
            runCatching { context.imageLoader.execute(request).image?.toBitmap() }.getOrNull()
        } ?: return@LaunchedEffect
        val palette = withContext(Dispatchers.Default) { Palette.from(bitmap).maximumColorCount(12).generate() }
        val rgb = palette.vibrantSwatch?.rgb ?: palette.lightVibrantSwatch?.rgb ?: palette.dominantSwatch?.rgb
        if (rgb != null) color = Color(rgb)
    }
    return color
}

/** Current time, refreshed on the minute, in the user's 12/24 hour format. */
@Composable
private fun rememberClock(): Pair<String, String> {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000L - now % 60_000L)
        }
    }
    val time = remember(now) { DateFormat.getTimeFormat(context).format(Date(now)).substringBefore(' ') }
    val date = remember(now) { SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date(now)) }
    return time to date
}

@Composable
private fun CompactView(
    connection: PlayerConnection,
    title: String,
    artist: String,
    artwork: String?,
    accent: Color,
    isPlaying: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
) {
    val (time, date) = rememberClock()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            // Tapping anywhere but the pill hands the screen back to the system lock screen.
            .clickable(interactionSource = null, indication = null, onClick = onDismiss)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Spacer(Modifier.height(72.dp))
        Text(time, color = Color.White, fontSize = 88.sp, fontWeight = FontWeight.Light, letterSpacing = (-2).sp)
        Text(date, color = Color.White.copy(alpha = 0.85f), fontSize = 17.sp)
        Spacer(Modifier.weight(1f))
        NowBarPill(
            connection = connection,
            title = title,
            artist = artist,
            artwork = artwork,
            accent = accent,
            isPlaying = isPlaying,
            onExpand = onExpand,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun NowBarPill(
    connection: PlayerConnection,
    title: String,
    artist: String,
    artwork: String?,
    accent: Color,
    isPlaying: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
) {
    val progress = rememberProgress(connection)
    Column(
        modifier = Modifier
            .padding(start = 14.dp, end = 14.dp, bottom = 88.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(34.dp))
            .background(Color(0xD91C1C21))
            .clickable(onClick = onExpand)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -12) onExpand() else if (dragAmount > 12) onDismiss()
                }
            },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
            AsyncImage(
                model = artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(14.dp)),
            )
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(artist, color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            RoundButton(R.drawable.skip_previous, stringResource(R.string.previous), size = 40.dp, onClick = connection::seekToPrevious)
            RoundButton(
                icon = if (isPlaying) R.drawable.pause else R.drawable.play,
                description = stringResource(if (isPlaying) R.string.pause else R.string.play),
                size = 46.dp,
                container = Color.White,
                tint = Color.Black,
                onClick = connection::togglePlayPause,
            )
            RoundButton(R.drawable.skip_next, stringResource(R.string.next), size = 40.dp, onClick = connection::seekToNext)
        }
        // Thin progress line along the pill's foot.
        Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 8.dp).height(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.15f))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(progress.fraction).background(accent))
        }
    }
}

private class Progress(val position: Long, val duration: Long) {
    val fraction: Float get() = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
}

@Composable
private fun rememberProgress(connection: PlayerConnection): Progress {
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    LaunchedEffect(connection) {
        while (true) {
            val player = connection.player
            position = player.currentPosition
            duration = player.duration.coerceAtLeast(0L)
            delay(500)
        }
    }
    return Progress(position, duration)
}

@Composable
private fun ExpandedPlayer(
    connection: PlayerConnection,
    title: String,
    artist: String,
    artwork: String?,
    songId: String,
    accent: Color,
    isPlaying: Boolean,
    onCollapse: () -> Unit,
    onOpenApp: () -> Unit,
) {
    var showLyrics by rememberPreference(LockScreenPlayerLyricsKey, true)
    var showQueue by remember { mutableStateOf(false) }
    val lyrics by connection.currentLyrics.collectAsState(initial = null)
    val lyricsAvailable = lyrics?.lyrics?.let { it.isNotBlank() && it != LyricsEntity.LYRICS_NOT_FOUND } == true
    val lyricsOnScreen = showLyrics && lyricsAvailable
    val currentSong by connection.currentSong.collectAsState(initial = null)
    val liked = currentSong?.song?.liked == true
    val shuffle by connection.shuffleModeEnabled.collectAsState()
    val repeatMode by connection.repeatMode.collectAsState()
    val (time, _) = rememberClock()

    BackHandler(enabled = showQueue) { showQueue = false }

    Box(Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 8.dp),
        ) {
            // Top bar: collapse, the time, unlock-and-open.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                RoundButton(R.drawable.expand_more, stringResource(R.string.lock_screen_player_collapse), size = 44.dp, container = Color.White.copy(alpha = 0.1f), onClick = onCollapse)
                Text(time, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                RoundButton(R.drawable.lock_open, stringResource(R.string.lock_screen_player_open_app), size = 44.dp, container = Color.White.copy(alpha = 0.1f), onClick = onOpenApp)
            }

            Spacer(Modifier.height(16.dp))

            // Stage: synced lyrics, or the artwork with the title laid over it (One UI 9 style).
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(Unit) { detectVerticalDragGestures { _, dy -> if (dy > 24) onCollapse() } },
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(targetState = lyricsOnScreen, label = "stage") { onLyrics ->
                    if (onLyrics) {
                        Box(Modifier.fillMaxSize()) {
                            ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center, color = Color.White)) {
                                MaterialTheme(colorScheme = darkColorScheme(primary = Color.White)) {
                                    Lyrics(
                                        sliderPositionProvider = { null },
                                        modifier = Modifier.fillMaxSize(),
                                        showLyrics = true,
                                        disableInteractiveFeatures = true,
                                    )
                                }
                            }
                        }
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(32.dp))) {
                                AsyncImage(model = artwork, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                Box(
                                    Modifier.fillMaxSize().background(
                                        Brush.verticalGradient(0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.75f)),
                                    ),
                                )
                                Column(Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                                    Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(artist, color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // With lyrics on the stage, the title moves down here next to a thumbnail.
            if (lyricsOnScreen) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    AsyncImage(model = artwork, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(artist, color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            OutputChip(accent)

            WaveformSeekBar(connection = connection, songId = songId, accent = accent, isPlaying = isPlaying)

            // Transport: round buttons, the big one filled with the artwork colour.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            ) {
                ToggleButton(
                    icon = if (shuffle) R.drawable.shuffle_on else R.drawable.shuffle,
                    description = stringResource(R.string.shuffle),
                    active = shuffle,
                    accent = accent,
                    onClick = connection::toggleShuffle,
                )
                RoundButton(R.drawable.skip_previous, stringResource(R.string.previous), size = 60.dp, iconSize = 32.dp, container = Color.White.copy(alpha = 0.12f), onClick = connection::seekToPrevious)
                RoundButton(
                    icon = if (isPlaying) R.drawable.pause else R.drawable.play,
                    description = stringResource(if (isPlaying) R.string.pause else R.string.play),
                    size = 78.dp,
                    iconSize = 38.dp,
                    container = accent,
                    tint = if (accent.luminance() > 0.5f) Color.Black else Color.White,
                    onClick = connection::togglePlayPause,
                )
                RoundButton(R.drawable.skip_next, stringResource(R.string.next), size = 60.dp, iconSize = 32.dp, container = Color.White.copy(alpha = 0.12f), onClick = connection::seekToNext)
                ToggleButton(
                    icon = when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> R.drawable.repeat_one_on
                        Player.REPEAT_MODE_ALL -> R.drawable.repeat_on
                        else -> R.drawable.repeat
                    },
                    description = stringResource(R.string.repeat),
                    active = repeatMode != Player.REPEAT_MODE_OFF,
                    accent = accent,
                    onClick = { connection.player.toggleRepeatMode() },
                )
            }

            // Secondary actions: like, lyrics on/off, queue.
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
            ) {
                ActionChip(
                    icon = if (liked) R.drawable.favorite else R.drawable.favorite_border,
                    label = stringResource(R.string.like),
                    active = liked,
                    accent = accent,
                    onClick = connection::toggleLike,
                )
                ActionChip(
                    icon = R.drawable.lyrics,
                    label = stringResource(R.string.lyrics),
                    active = showLyrics,
                    accent = accent,
                    onClick = { showLyrics = !showLyrics },
                )
                ActionChip(
                    icon = R.drawable.queue_music,
                    label = stringResource(R.string.queue),
                    active = showQueue,
                    accent = accent,
                    onClick = { showQueue = true },
                )
            }
        }

        AnimatedVisibility(
            visible = showQueue,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            QueuePanel(connection = connection, accent = accent, onClose = { showQueue = false })
        }
    }
}

private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

/** Where the sound is going, like One UI's "This phone" chip, and a way to switch it. */
@Composable
private fun OutputChip(accent: Color) {
    val context = LocalContext.current
    val thisPhone = stringResource(R.string.lock_screen_player_this_phone)
    var output by remember { mutableStateOf(thisPhone) }
    LaunchedEffect(Unit) {
        while (true) {
            output = currentOutputName(context) ?: thisPhone
            delay(3000)
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(bottom = 4.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.1f))
            .clickable {
                if (Build.VERSION.SDK_INT >= 34) runCatching { MediaRouter2.getInstance(context).showSystemOutputSwitcher() }
            }
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Icon(painterResource(R.drawable.devices), contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(output, color = Color.White, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun currentOutputName(context: Context): String? {
    val audio = context.getSystemService(AudioManager::class.java) ?: return null
    val external = setOf(
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_USB_HEADSET,
    ) + if (Build.VERSION.SDK_INT >= 31) setOf(AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLE_SPEAKER) else emptySet()
    return audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        .firstOrNull { it.type in external }
        ?.productName?.toString()?.takeIf { it.isNotBlank() }
}

/**
 * One UI 9 style seek bar: a row of waveform bars that fill with the artwork colour as the
 * song plays and sway gently while it is playing. Tap or drag anywhere on it to seek.
 */
@Composable
private fun WaveformSeekBar(connection: PlayerConnection, songId: String, accent: Color, isPlaying: Boolean) {
    val progress = rememberProgress(connection)
    var dragFraction by remember { mutableFloatStateOf(-1f) }
    val fraction = if (dragFraction >= 0f) dragFraction else progress.fraction
    val heights = remember(songId) {
        val random = Random(songId.hashCode())
        List(56) { 0.3f + random.nextFloat() * 0.7f }
    }
    val phase by rememberInfiniteTransition(label = "wave").animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "wavePhase",
    )

    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .pointerInput(progress.duration) {
                    detectTapGestures { offset ->
                        connection.seekTo((offset.x / size.width * progress.duration).toLong())
                    }
                }
                .pointerInput(progress.duration) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragFraction = (it.x / size.width).coerceIn(0f, 1f) },
                        onDragEnd = {
                            connection.seekTo((dragFraction * progress.duration).toLong())
                            dragFraction = -1f
                        },
                        onDragCancel = { dragFraction = -1f },
                        onHorizontalDrag = { change, _ ->
                            dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            change.consume()
                        },
                    )
                },
        ) {
            val gap = 3.dp.toPx()
            val barWidth = (size.width - gap * (heights.size - 1)) / heights.size
            heights.forEachIndexed { index, base ->
                val sway = if (isPlaying) 0.82f + 0.18f * sin(phase + index * 0.45f) else 1f
                val barHeight = size.height * base * sway
                val x = index * (barWidth + gap)
                val played = (index + 0.5f) / heights.size <= fraction
                drawRoundRect(
                    color = if (played) accent else Color.White.copy(alpha = 0.22f),
                    topLeft = Offset(x, (size.height - barHeight) / 2f),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            val shown = if (dragFraction >= 0f) (dragFraction * progress.duration).toLong() else progress.position
            Text(makeTimeString(shown), color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Text(makeTimeString(progress.duration), color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun QueuePanel(connection: PlayerConnection, accent: Color, onClose: () -> Unit) {
    val windows by connection.queueWindows.collectAsState()
    val currentIndex by connection.currentWindowIndex.collectAsState()
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) { if (currentIndex > 0) listState.scrollToItem(currentIndex) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.62f)
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(Color(0xF2141418))
            .navigationBarsPadding(),
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp).size(width = 36.dp, height = 4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp)) {
            Text(stringResource(R.string.queue), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            RoundButton(R.drawable.close, stringResource(R.string.close), size = 40.dp, onClick = onClose)
        }
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(top = 4.dp)) {
            itemsIndexed(windows, key = { index, window -> "$index:${window.uid.hashCode()}" }) { index, window ->
                val item = window.mediaItem.metadata ?: return@itemsIndexed
                val current = index == currentIndex
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (current) accent.copy(alpha = 0.18f) else Color.Transparent)
                        .clickable {
                            connection.player.seekToDefaultPosition(window.firstPeriodIndex)
                            connection.player.playWhenReady = true
                        }
                        .padding(10.dp),
                ) {
                    AsyncImage(model = item.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(46.dp).clip(RoundedCornerShape(10.dp)))
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(item.title, color = if (current) accent else Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(item.artists.joinToString { it.name }, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(makeTimeString(item.duration * 1000L), color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun RoundButton(
    icon: Int,
    description: String,
    size: Dp,
    iconSize: Dp = size * 0.5f,
    container: Color = Color.Transparent,
    tint: Color = Color.White,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size).clip(CircleShape).background(container).clickable(onClick = onClick),
    ) {
        Icon(painterResource(icon), contentDescription = description, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
private fun ToggleButton(icon: Int, description: String, active: Boolean, accent: Color, onClick: () -> Unit) {
    RoundButton(
        icon = icon,
        description = description,
        size = 46.dp,
        iconSize = 22.dp,
        container = if (active) accent.copy(alpha = 0.22f) else Color.Transparent,
        tint = if (active) accent else Color.White.copy(alpha = 0.7f),
        onClick = onClick,
    )
}

@Composable
private fun ActionChip(icon: Int, label: String, active: Boolean, accent: Color, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (active) accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = if (active) accent else Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
