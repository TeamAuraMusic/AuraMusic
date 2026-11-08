package com.auramusic.app.tv

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import android.util.LruCache
import java.io.ByteArrayOutputStream
import com.auramusic.app.playback.MusicService

/** Adds a card-bounded indicator because Projectivy 4.73's focused video Surface remains visually blank. */
class TvPreviewFocusAccessibilityService : AccessibilityService() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val bounds = Rect()
    private var windowManager: WindowManager? = null
    private var indicator: FocusedCardWaveformView? = null
    private var lastPrograms = emptyMap<String, TvPreviewFocusTargets.Program>()
    private var titlesLoadedAtMs = 0L
    private var artworkRequest = 0
    private var currentArtworkUri: Uri? = null
    private var focusedProgramTitle: String? = null
    private var playbackMonitor: AudioPlaybackMonitor? = null
    private var hasActiveMediaPlayback = Build.VERSION.SDK_INT < Build.VERSION_CODES.O
    private var musicServiceBinding = false
    private var musicService: MusicService? = null
    private val artworkCache = object : LruCache<String, Bitmap>(4) {}
    private val artworkExecutor = Executors.newSingleThreadExecutor()
    private val musicServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            musicServiceBinding = true
            musicService = (service as? MusicService.MusicBinder)?.service
            val title = focusedProgramTitle
            if (title == null || indicator == null) {
                unbindMusicService()
                return
            }
            setAuraMusicPreviewFocus(title)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicServiceBinding = false
            musicService = null
        }
    }
    private val refreshFocusedCard = Runnable { refreshFocusedNode() }
    private val playbackRefreshLoop = FocusedPlaybackRefreshLoop(
        intervalMs = PLAYBACK_REFRESH_MS,
        isFocused = { playbackMonitor != null && indicator != null && focusedProgramTitle != null },
        refresh = {
            runCatching { playbackMonitor?.refresh() }
                .onFailure { Timber.w(it, "Could not refresh Projectivy playback state") }
        },
        postDelayed = { runnable, delay -> mainHandler.postDelayed(runnable, delay) },
        removeCallbacks = { runnable -> mainHandler.removeCallbacks(runnable) },
    )
    private val activeWindowCheck = object : Runnable {
        override fun run() {
            if (indicator == null) return
            if (rootInActiveWindow?.packageName?.toString() != PROJECTIVY_PACKAGE) {
                hideIndicator("active window changed")
            } else {
                mainHandler.postDelayed(this, ACTIVE_WINDOW_CHECK_MS)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WindowManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                playbackMonitor = AudioPlaybackMonitor(this, mainHandler, ::updatePlaybackState)
            }.onFailure {
                Timber.w(it, "Could not monitor active playback for the Projectivy indicator; using focus state")
                hasActiveMediaPlayback = true
            }
        }
        Timber.i("TV preview focus indicator service connected")
        scheduleFocusedCardRefresh()
    }

    private fun updatePlaybackState(active: Boolean) {
        if (active == hasActiveMediaPlayback) return
        hasActiveMediaPlayback = active
        Timber.i("TV preview media playback state active=%s", active)
        indicator?.setPlaybackActive(active)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val source = event.source
        try {
            Timber.d(
                "TV preview accessibility event type=%s package=%s source=%s description=%s",
                AccessibilityEvent.eventTypeToString(event.eventType),
                event.packageName,
                source?.className,
                source?.contentDescription,
            )
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED ||
                event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            ) {
                val eventPackage = event.packageName?.toString()
                if (eventPackage != null &&
                    eventPackage != PROJECTIVY_PACKAGE &&
                    eventPackage != applicationContext.packageName
                ) {
                    hideIndicator("launcher lost foreground")
                } else {
                    scheduleFocusedCardRefresh()
                }
                return
            }
            if (event.packageName?.toString() != PROJECTIVY_PACKAGE) return
            if (source != null) updateFocusedProgram(source)
            scheduleFocusedCardRefresh()
        } catch (error: Throwable) {
            Timber.w(error, "Could not update Projectivy focused-card indicator")
        } finally {
            @Suppress("DEPRECATION")
            source?.recycle()
        }
    }

    private fun scheduleFocusedCardRefresh() {
        mainHandler.removeCallbacks(refreshFocusedCard)
        mainHandler.postDelayed(refreshFocusedCard, FOCUS_REFRESH_DELAY_MS)
    }

    private fun refreshFocusedNode() {
        val root = rootInActiveWindow
        if (root == null || root.packageName?.toString() != PROJECTIVY_PACKAGE) {
            root?.recycleCompat()
            hideIndicator("Projectivy is not the active window")
            return
        }
        try {
            val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                ?: root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
                ?: findFocusedDescendant(root, depth = 0)
            if (focused == null) {
                hideIndicator("Projectivy has no focused node")
                return
            }
            try {
                Timber.d("TV preview focused-node refresh description=%s", focused.contentDescription)
                updateFocusedProgram(focused)
            } finally {
                focused.recycleCompat()
            }
        } finally {
            root.recycleCompat()
        }
    }

    private fun findFocusedDescendant(node: AccessibilityNodeInfo, depth: Int): AccessibilityNodeInfo? {
        if (depth > MAX_FOCUS_TREE_DEPTH) return null
        if (node.isFocused) return AccessibilityNodeInfo.obtain(node)
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            try {
                findFocusedDescendant(child, depth + 1)?.let { return it }
            } finally {
                child.recycleCompat()
            }
        }
        return null
    }

    @Suppress("DEPRECATION")
    private fun AccessibilityNodeInfo.recycleCompat() = recycle()

    private fun updateFocusedProgram(node: AccessibilityNodeInfo) {
        val title = node.contentDescription?.toString()?.trim().orEmpty()
        val program = publishedPrograms()[title]
        if (title.isEmpty() || program == null) {
            hideIndicator("focus is not a published AuraMusic program")
            return
        }
        if (!isWithinChannelRow(node, program.channelNames)) {
            hideIndicator("focused program is outside its AuraMusic channel row")
            return
        }
        node.getBoundsInScreen(bounds)
        if (bounds.width() < MIN_CARD_SIZE_PX || bounds.height() < MIN_CARD_SIZE_PX || !node.isVisibleToUser) {
            hideIndicator("focused program has no visible card bounds")
            return
        }
        showIndicator(bounds, title, program.artwork)
    }

    private fun isWithinChannelRow(node: AccessibilityNodeInfo, channelNames: Set<String>): Boolean {
        val ancestorDescriptions = buildList {
            var ancestor = node.parent
            var depth = 0
            try {
                while (ancestor != null && depth < MAX_CHANNEL_ANCESTOR_DEPTH) {
                    add(ancestor.contentDescription?.toString())
                    val parent = ancestor.parent
                    ancestor.recycleCompat()
                    ancestor = parent
                    depth++
                }
            } finally {
                ancestor?.recycleCompat()
            }
        }
        return TvPreviewFocusTargets.isInChannelRow(channelNames, ancestorDescriptions)
    }

    private fun publishedPrograms(): Map<String, TvPreviewFocusTargets.Program> {
        val now = SystemClock.elapsedRealtime()
        if (now - titlesLoadedAtMs >= TITLES_CACHE_MS) {
            lastPrograms = runCatching { TvPreviewFocusTargets.publishedPrograms(this) }
                .onFailure { Timber.w(it, "Could not read AuraMusic TV programs for focus matching") }
                .getOrDefault(emptyMap())
            titlesLoadedAtMs = now
        }
        return lastPrograms
    }

    private fun showIndicator(card: Rect, title: String, artworkUri: Uri?) {
        if (focusedProgramTitle != title) {
            focusedProgramTitle = title
            pauseAuraMusicForPreview(title)
        }
        mainHandler.removeCallbacks(activeWindowCheck)
        val manager = windowManager ?: return
        val existing = indicator
        val view = existing ?: FocusedCardWaveformView(this).also { indicator = it }
        val params = WindowManager.LayoutParams(
            card.width(),
            card.height(),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.LEFT
            x = card.left
            y = card.top
        }
        view.setPlaybackActive(hasActiveMediaPlayback)
        try {
            if (existing == null) manager.addView(view, params) else manager.updateViewLayout(view, params)
        } catch (error: Exception) {
            if (existing == null) indicator = null
            throw error
        }
        if (currentArtworkUri != artworkUri) {
            currentArtworkUri = artworkUri
            val request = ++artworkRequest
            view.setArtwork(artworkUri?.let { artworkCache.get(it.toString()) })
            if (artworkUri != null && artworkCache.get(artworkUri.toString()) == null) {
                artworkExecutor.execute {
                    val bitmap = runCatching { loadArtwork(artworkUri) }
                        .onFailure { Timber.w(it, "Could not load focused AuraMusic artwork uri=%s", artworkUri.scheme) }
                        .getOrNull()
                    if (bitmap != null) artworkCache.put(artworkUri.toString(), bitmap)
                    mainHandler.post {
                        if (request == artworkRequest && currentArtworkUri == artworkUri && indicator === view) {
                            view.setArtwork(bitmap)
                            Timber.i("TV preview focused artwork ready title=%s size=%s", title, bitmap?.let { "${it.width}x${it.height}" })
                        }
                    }
                }
            }
        }
        Timber.d("TV preview focus indicator shown title=%s bounds=%s", title, card)
        mainHandler.postDelayed(activeWindowCheck, ACTIVE_WINDOW_CHECK_MS)
        playbackRefreshLoop.start()
    }

    private fun loadArtwork(uri: Uri): Bitmap? {
        val connection = if (uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) {
            (URL(uri.toString()).openConnection() as HttpURLConnection).apply {
                connectTimeout = ARTWORK_TIMEOUT_MS
                readTimeout = ARTWORK_TIMEOUT_MS
                instanceFollowRedirects = true
            }
        } else null
        val bytes = try {
            val stream = connection?.let {
                it.connect()
                if (it.responseCode !in 200..299) return null
                it.inputStream
            } ?: when (uri.scheme?.lowercase()) {
                "content", "android.resource", "file" -> contentResolver.openInputStream(uri)
                else -> null
            } ?: return null
            stream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(16 * 1024)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_ARTWORK_BYTES) return null
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
        } finally {
            connection?.disconnect()
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > MAX_ARTWORK_EDGE || bounds.outHeight / sample > MAX_ARTWORK_EDGE) {
            sample *= 2
        }
        return BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            },
        )
    }

    private fun hideIndicator(reason: String) {
        mainHandler.removeCallbacks(activeWindowCheck)
        playbackRefreshLoop.stop()
        indicator?.let { view -> runCatching { windowManager?.removeView(view) } }
        if (indicator != null) Timber.d("TV preview focus indicator hidden reason=%s", reason)
        indicator = null
        focusedProgramTitle = null
        unbindMusicService()
        artworkRequest++
        currentArtworkUri = null
    }

    private fun pauseAuraMusicForPreview(title: String) {
        val service = musicService
        if (service != null) {
            setAuraMusicPreviewFocus(title)
            return
        }
        if (musicServiceBinding || focusedProgramTitle == null) return

        val bound = runCatching {
            // No BIND_AUTO_CREATE: focusing a card must not start AuraMusic's player service.
            bindService(
                Intent(this, MusicService::class.java),
                musicServiceConnection,
                Context.BIND_NOT_FOREGROUND,
            )
        }.onFailure { Timber.w(it, "Could not bind existing MusicService for Projectivy preview") }
            .getOrDefault(false)
        if (bound) {
            musicServiceBinding = true
        } else {
            Timber.d("MusicService is not running; no AuraMusic background playback to pause")
        }
    }

    private fun setAuraMusicPreviewFocus(title: String) {
        if (runCatching { musicService?.setProjectivyPreviewFocus(true, title) == true }
                .onFailure { Timber.w(it, "Could not pause AuraMusic background playback for Projectivy preview") }
                .getOrDefault(false)
        ) {
            Timber.i("Paused background AuraMusic playback before Projectivy preview title=%s", title)
        }
    }

    private fun unbindMusicService() {
        if (!musicServiceBinding) return
        runCatching { musicService?.setProjectivyPreviewFocus(false) }
        runCatching { unbindService(musicServiceConnection) }
        musicServiceBinding = false
        musicService = null
    }

    override fun onInterrupt() = hideIndicator("service interrupted")

    override fun onDestroy() {
        mainHandler.removeCallbacks(refreshFocusedCard)
        playbackRefreshLoop.stop()
        hideIndicator("service destroyed")
        artworkExecutor.shutdownNow()
        runCatching { playbackMonitor?.close() }
        playbackMonitor = null
        super.onDestroy()
    }

    private companion object {
        const val PROJECTIVY_PACKAGE = "com.spocky.projengmenu"
        const val TITLES_CACHE_MS = 10_000L
        const val ACTIVE_WINDOW_CHECK_MS = 500L
        const val PLAYBACK_REFRESH_MS = 500L
        const val FOCUS_REFRESH_DELAY_MS = 100L
        const val MAX_FOCUS_TREE_DEPTH = 30
        const val MAX_CHANNEL_ANCESTOR_DEPTH = 16
        const val MIN_CARD_SIZE_PX = 80
        const val ARTWORK_TIMEOUT_MS = 5_000
        const val MAX_ARTWORK_BYTES = 12 * 1024 * 1024
        const val MAX_ARTWORK_EDGE = 640
    }
}
