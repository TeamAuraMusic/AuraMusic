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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import android.util.LruCache
import java.io.ByteArrayOutputStream
import com.auramusic.app.playback.MusicService

/** Adds a card-bounded indicator because Projectivy 4.73's focused video Surface remains visually blank. */
class TvPreviewFocusAccessibilityService : AccessibilityService() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val bounds = Rect()
    private val indicatorBounds = Rect()
    private var windowManager: WindowManager? = null
    private var indicator: FocusedCardWaveformView? = null
    private var indicatorLayoutParams: WindowManager.LayoutParams? = null
    private var boundsSyncUntilMs = 0L
    private val programCardBounds = mutableMapOf<String, Rect>()
    private var lastPrograms = emptyMap<String, TvPreviewFocusTargets.Program>()
    private var titlesLoadedAtMs = 0L
    private var currentArtworkUri: Uri? = null
    private var focusedProgramTitle: String? = null
    private var pendingBoundsRefreshTitle: String? = null
    private var playbackMonitor: AudioPlaybackMonitor? = null
    private val audioPrefetchExecutor = ThreadPoolExecutor(
        1,
        1,
        0L,
        TimeUnit.MILLISECONDS,
        LinkedBlockingQueue(1),
        ThreadPoolExecutor.DiscardOldestPolicy(),
    )
    private var pendingAudioPrefetch: Runnable? = null
    private var activeAudioPrefetch: Future<*>? = null
    private var audioPrefetchKey: String? = null
    private var musicServiceBinding = false
    private var musicService: MusicService? = null
    private val artworkCache = object : LruCache<String, Bitmap>(4) {}
    private val artworkLoads = ConcurrentHashMap.newKeySet<String>()
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
    private val syncFocusedCardBounds = object : Runnable {
        override fun run() {
            val view = indicator ?: return
            val root = rootInActiveWindow
            if (root == null || root.packageName?.toString() != PROJECTIVY_PACKAGE) {
                root?.recycleCompat()
                hideIndicator("Projectivy left foreground during card transition")
                return
            }
            try {
                val focused = focusedNode(root)
                if (focused == null) {
                    hideIndicator("Projectivy focus disappeared during card transition")
                    return
                }
                try {
                    val title = focused.contentDescription?.toString()?.trim().orEmpty()
                    if (title != focusedProgramTitle) {
                        view.maskPreviousPreviewFrame()
                        Timber.d(
                            "TV preview masked stale frame while focus refresh is pending old=%s new=%s",
                            focusedProgramTitle, title,
                        )
                        scheduleFocusedCardRefresh()
                        return
                    }
                    val program = publishedPrograms()[title]
                    if (program == null || !isWithinChannelRow(focused, program.channelNames)) {
                        hideIndicator("focused card left the AuraMusic row during transition")
                        return
                    }
                    focused.getBoundsInScreen(bounds)
                    if (bounds.width() >= MIN_CARD_SIZE_PX &&
                        bounds.height() >= MIN_CARD_SIZE_PX && focused.isVisibleToUser
                    ) {
                        val currentBounds = Rect(bounds)
                        if (currentBounds != indicatorBounds) {
                            // Projectivy animates the row after accessibility focus has
                            // moved. Keep the overlay attached for the whole movement,
                            // not just the first fixed window, or its frame can hang over
                            // the neighboring card while that animation is still running.
                            boundsSyncUntilMs = maxOf(
                                boundsSyncUntilMs,
                                SystemClock.uptimeMillis() + CARD_BOUNDS_SETTLE_MS,
                            )
                        }
                        updateIndicatorLayout(currentBounds)
                    }
                } finally {
                    focused.recycleCompat()
                }
            } finally {
                root.recycleCompat()
            }
            if (indicator === view && SystemClock.uptimeMillis() < boundsSyncUntilMs) {
                mainHandler.postDelayed(this, CARD_BOUNDS_POLL_MS)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WindowManager::class.java)
        runCatching {
            playbackMonitor = AudioPlaybackMonitor(this, mainHandler, ::updatePlaybackState)
        }.onFailure {
            Timber.w(it, "Could not monitor Projectivy media playback for diagnostics")
        }
        Timber.i("TV preview focus indicator service connected")
        scheduleFocusedCardRefresh()
    }

    private fun updatePlaybackState(active: Boolean) {
        Timber.d("TV preview media playback state active=%s focusedTitle=%s", active, focusedProgramTitle)
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
            if (source != null &&
                (event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
                    event.eventType == AccessibilityEvent.TYPE_VIEW_SELECTED)
            ) {
                val incomingTitle = source.contentDescription?.toString()?.trim().orEmpty()
                if (incomingTitle.isNotEmpty() && incomingTitle != focusedProgramTitle) {
                    indicator?.maskPreviousPreviewFrame()
                    Timber.d(
                        "TV preview previous artwork masked at focus event old=%s new=%s",
                        focusedProgramTitle, incomingTitle,
                    )
                }
            }
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
            val focused = focusedNode(root)
            if (focused == null) {
                hideIndicator("Projectivy has no focused node")
                return
            }
            try {
                cacheProgramCardBounds(root, focused.contentDescription?.toString()?.trim().orEmpty())
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

    private fun focusedNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? =
        root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
            ?: findFocusedDescendant(root, depth = 0)

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
        scheduleAudioPrefetch(title, program.previewUri)
        node.getBoundsInScreen(bounds)
        if (bounds.width() < MIN_CARD_SIZE_PX || bounds.height() < MIN_CARD_SIZE_PX || !node.isVisibleToUser) {
            if (indicator != null && title != focusedProgramTitle && pendingBoundsRefreshTitle != title) {
                pendingBoundsRefreshTitle = title
                indicator?.maskPreviousPreviewFrame()
                Timber.d("TV preview masked while focused card bounds settle title=%s", title)
                scheduleFocusedCardRefresh()
                return
            }
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

    private fun cacheProgramCardBounds(root: AccessibilityNodeInfo, focusedTitle: String) {
        val programs = publishedPrograms()
        val currentBounds = mutableMapOf<String, Rect>()
        fun visit(node: AccessibilityNodeInfo, depth: Int) {
            if (depth > MAX_FOCUS_TREE_DEPTH) return
            val title = node.contentDescription?.toString()?.trim().orEmpty()
            val program = programs[title]
            if (program != null && isWithinChannelRow(node, program.channelNames)) {
                val card = Rect()
                node.getBoundsInScreen(card)
                if (card.width() > 0 && card.height() > 0 && node.isVisibleToUser) {
                    currentBounds[title] = card
                }
            }
            for (index in 0 until node.childCount) {
                val child = node.getChild(index) ?: continue
                try {
                    visit(child, depth + 1)
                } finally {
                    child.recycleCompat()
                }
            }
        }
        visit(root, 0)
        programCardBounds.clear()
        programCardBounds.putAll(currentBounds)
        val focusedBounds = currentBounds[focusedTitle]
        currentBounds.keys
            .sortedBy { title ->
                val card = currentBounds.getValue(title)
                if (focusedBounds == null) Int.MAX_VALUE else kotlin.math.abs(card.centerX() - focusedBounds.centerX())
            }
            .take(MAX_VISIBLE_ARTWORK_PREFETCH)
            .forEach { title -> programs[title]?.artwork?.let { preloadArtwork(title, it) } }
    }

    private fun preloadArtwork(title: String, uri: Uri) {
        val key = uri.toString()
        if (artworkCache.get(key) != null || !artworkLoads.add(key)) return
        Timber.d("TV preview artwork prefetch started title=%s", title)
        artworkExecutor.execute {
            val bitmap = runCatching { loadArtwork(uri) }
                .onFailure { Timber.w(it, "Could not preload AuraMusic artwork uri=%s", uri.scheme) }
                .getOrNull()
            if (bitmap != null) artworkCache.put(key, bitmap)
            artworkLoads.remove(key)
            if (bitmap != null) {
                mainHandler.post {
                    if (currentArtworkUri == uri) {
                        indicator?.setArtwork(bitmap)
                        Timber.i("TV preview focused artwork ready title=%s size=%s", title, "${bitmap.width}x${bitmap.height}")
                    }
                }
            }
        }
    }

    private fun showIndicator(card: Rect, title: String, artworkUri: Uri?) {
        val previousTitle = focusedProgramTitle
        pendingBoundsRefreshTitle = null
        if (previousTitle != null && previousTitle != title) {
            mainHandler.removeCallbacks(syncFocusedCardBounds)
            indicator?.let { oldView ->
                oldView.setPreviewFocused(false)
                runCatching { windowManager?.removeView(oldView) }
            }
            indicator = null
            indicatorLayoutParams = null
            indicatorBounds.setEmpty()
            currentArtworkUri = null
            Timber.d("TV preview focus overlay window replaced old=%s new=%s", previousTitle, title)
        }
        if (previousTitle != title) {
            focusedProgramTitle = title
            pauseAuraMusicForPreview(title)
        }
        mainHandler.removeCallbacks(activeWindowCheck)
        val existing = indicator
        val view = existing ?: FocusedCardWaveformView(this).also { indicator = it }
        // Change/clear the bitmap before moving the reusable overlay to the next card.
        if (currentArtworkUri != artworkUri) {
            currentArtworkUri = artworkUri
            view.setArtwork(artworkUri?.let { artworkCache.get(it.toString()) })
            if (artworkUri != null && artworkCache.get(artworkUri.toString()) != null) {
                Timber.d("TV preview focused artwork cache hit title=%s", title)
            }
            if (artworkUri != null) preloadArtwork(title, artworkUri)
        }
        view.setPreviewFocused(true)
        try {
            // Re-anchor immediately on the selected card. Projectivy can retain its old
            // preview Surface frame while scrolling; translating this overlay from the
            // adjacent card makes the old art appear to slide across cards.
            updateIndicatorLayout(card)
        } catch (error: Exception) {
            if (existing == null) indicator = null
            throw error
        }
        Timber.d("TV preview focus indicator shown title=%s bounds=%s", title, card)
        mainHandler.postDelayed(activeWindowCheck, ACTIVE_WINDOW_CHECK_MS)
        scheduleFocusedBoundsSync()
    }

    private fun scheduleAudioPrefetch(title: String, previewUri: Uri?) {
        val candidate = previewUri?.takeIf { uri ->
            TvPreviewFocusTargets.isLocalAudioOnlyPreviewUri(
                scheme = uri.scheme,
                authority = uri.authority,
                mode = uri.getQueryParameter("mode"),
                mimeType = uri.getQueryParameter("mime"),
                expectedAuthority = "$packageName.tvpreview",
            )
        }
        val nextKey = candidate?.let { "$title|$it" }
        if (nextKey == audioPrefetchKey) return
        cancelAudioPrefetch(resetKey = true)
        if (candidate == null || nextKey == null) return
        audioPrefetchKey = nextKey
        val callback = Runnable {
            pendingAudioPrefetch = null
            if (focusedProgramTitle != title || audioPrefetchKey != nextKey) return@Runnable
            activeAudioPrefetch = audioPrefetchExecutor.submit {
                val startedAt = SystemClock.elapsedRealtime()
                try {
                    val descriptor = contentResolver.openFileDescriptor(candidate, "r")
                        ?: error("Audio preview provider returned no file descriptor")
                    descriptor.use { }
                    Timber.i(
                        "TV preview focus audio prefetch ready title=%s elapsedMs=%d",
                        title,
                        SystemClock.elapsedRealtime() - startedAt,
                    )
                } catch (error: Exception) {
                    if (!Thread.currentThread().isInterrupted) {
                        Timber.w(error, "TV preview focus audio prefetch failed title=%s", title)
                    }
                } finally {
                    mainHandler.post {
                        if (audioPrefetchKey == nextKey) activeAudioPrefetch = null
                    }
                }
            }
        }
        pendingAudioPrefetch = callback
        mainHandler.postDelayed(callback, AUDIO_PREFETCH_DELAY_MS)
        Timber.d(
            "TV preview focus audio prefetch scheduled title=%s delayMs=%d",
            title,
            AUDIO_PREFETCH_DELAY_MS,
        )
    }

    private fun cancelAudioPrefetch(resetKey: Boolean) {
        pendingAudioPrefetch?.let(mainHandler::removeCallbacks)
        pendingAudioPrefetch = null
        activeAudioPrefetch?.cancel(true)
        activeAudioPrefetch = null
        audioPrefetchExecutor.purge()
        if (resetKey) audioPrefetchKey = null
    }

    private fun scheduleFocusedBoundsSync() {
        mainHandler.removeCallbacks(syncFocusedCardBounds)
        boundsSyncUntilMs = SystemClock.uptimeMillis() + CARD_BOUNDS_SYNC_MS
        mainHandler.post(syncFocusedCardBounds)
    }

    private fun updateIndicatorLayout(card: Rect) {
        val view = indicator ?: return
        val manager = windowManager ?: return
        val params = indicatorLayoutParams
        if (params == null) {
            val initialParams = WindowManager.LayoutParams(
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
            manager.addView(view, initialParams)
            indicatorLayoutParams = initialParams
            indicatorBounds.set(card)
            return
        }
        if (indicatorBounds == card) return
        params.width = card.width()
        params.height = card.height()
        params.x = card.left
        params.y = card.top
        manager.updateViewLayout(view, params)
        indicatorBounds.set(card)
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
        cancelAudioPrefetch(resetKey = true)
        mainHandler.removeCallbacks(activeWindowCheck)
        mainHandler.removeCallbacks(syncFocusedCardBounds)
        boundsSyncUntilMs = 0L
        programCardBounds.clear()
        indicator?.let { view -> runCatching { windowManager?.removeView(view) } }
        if (indicator != null) Timber.d("TV preview focus indicator hidden reason=%s", reason)
        indicator = null
        indicatorLayoutParams = null
        indicatorBounds.setEmpty()
        focusedProgramTitle = null
        pendingBoundsRefreshTitle = null
        unbindMusicService()
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
        hideIndicator("service destroyed")
        artworkExecutor.shutdownNow()
        audioPrefetchExecutor.shutdownNow()
        runCatching { playbackMonitor?.close() }
        playbackMonitor = null
        super.onDestroy()
    }

    private companion object {
        const val PROJECTIVY_PACKAGE = "com.spocky.projengmenu"
        const val TITLES_CACHE_MS = 10_000L
        const val ACTIVE_WINDOW_CHECK_MS = 500L
        const val FOCUS_REFRESH_DELAY_MS = 100L
        const val MAX_VISIBLE_ARTWORK_PREFETCH = 4
        const val AUDIO_PREFETCH_DELAY_MS = 120L
        const val CARD_BOUNDS_SYNC_MS = 1_200L
        const val CARD_BOUNDS_SETTLE_MS = 450L
        const val CARD_BOUNDS_POLL_MS = 50L
        const val MAX_FOCUS_TREE_DEPTH = 30
        const val MAX_CHANNEL_ANCESTOR_DEPTH = 16
        const val MIN_CARD_SIZE_PX = 80
        const val ARTWORK_TIMEOUT_MS = 5_000
        const val MAX_ARTWORK_BYTES = 12 * 1024 * 1024
        const val MAX_ARTWORK_EDGE = 640
    }
}
