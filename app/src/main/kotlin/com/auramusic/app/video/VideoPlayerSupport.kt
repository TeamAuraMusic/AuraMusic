/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.video

import android.app.ActivityManager
import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultAllocator
import com.auramusic.innertube.YouTube
import java.io.File
import okhttp3.OkHttpClient

/**
 * The heavy dependencies of the video player, built once, in one place.
 *
 * A stock `ExoPlayer.Builder(context).build()` is not safe for video on this app: the default
 * load control buffers fifty seconds of a high-bitrate stream with no memory cap, the default
 * renderer factory has no decoder fallback, and the default data source re-downloads every
 * byte on every seek. On a small heap shared with the Compose image caches that pressure
 * showed up as the whole application stuttering while a video played. Every knob here exists
 * because the music player - which has all of them - does not stutter.
 */
@UnstableApi
object VideoPlayerSupport {

    private const val MIN_BUFFER_MS = 15_000
    private const val MAX_BUFFER_MS = 45_000
    private const val LOW_MEMORY_MIN_BUFFER_MS = 8_000
    private const val LOW_MEMORY_MAX_BUFFER_MS = 18_000
    private const val MID_MEMORY_MAX_BUFFER_MS = 30_000
    private const val BUFFER_FOR_PLAYBACK_MS = 2_500
    private const val BUFFER_FOR_REBUFFER_MS = 5_000

    private const val TARGET_BUFFER_BYTES = 32 * 1024 * 1024
    private const val MID_MEMORY_TARGET_BUFFER_BYTES = 12 * 1024 * 1024
    private const val LOW_MEMORY_TARGET_BUFFER_BYTES = 4 * 1024 * 1024

    /** Instant rewind for a few seconds without hitting the cache; too dear on small heaps. */
    private const val BACK_BUFFER_MS = 10_000

    private const val ALLOCATOR_BUFFER_SIZE = 64 * 1024
    private const val VIDEO_CACHE_SIZE_BYTES = 256L * 1024L * 1024L
    private const val VIDEO_CACHE_DIR_NAME = "video_cache"

    private data class MemoryTier(
        val targetBufferBytes: Int,
        val minBufferMs: Int,
        val maxBufferMs: Int,
        val backBufferMs: Int,
        val maxVideoWidth: Int,
        val maxVideoHeight: Int,
    )

    /**
     * Buffers and decode budgets scaled to what this device's app heap can afford. The numbers
     * come from what the device reports, not the build flavour: a 4 GB phone running a vendor
     * skin with a small heap gets the small profile.
     */
    private fun memoryTier(context: Context): MemoryTier {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryClassMb = activityManager?.memoryClass ?: 256
        val isLowMemoryDevice = activityManager?.isLowRamDevice == true || memoryClassMb <= 256
        return when {
            isLowMemoryDevice -> MemoryTier(
                LOW_MEMORY_TARGET_BUFFER_BYTES, LOW_MEMORY_MIN_BUFFER_MS, LOW_MEMORY_MAX_BUFFER_MS,
                backBufferMs = 0, maxVideoWidth = 1920, maxVideoHeight = 1080,
            )
            memoryClassMb <= 384 -> MemoryTier(
                MID_MEMORY_TARGET_BUFFER_BYTES, MIN_BUFFER_MS, MID_MEMORY_MAX_BUFFER_MS,
                backBufferMs = 0, maxVideoWidth = 2560, maxVideoHeight = 1440,
            )
            else -> MemoryTier(
                TARGET_BUFFER_BYTES, MIN_BUFFER_MS, MAX_BUFFER_MS,
                BACK_BUFFER_MS, maxVideoWidth = 3840, maxVideoHeight = 2160,
            )
        }
    }

    /**
     * The video load control. Time-based thresholds so the window is bounded in seconds, plus
     * a hard byte budget so a high-bitrate stream can never grow past what the heap can spare.
     */
    fun createLoadControl(context: Context): DefaultLoadControl {
        val tier = memoryTier(context)
        return DefaultLoadControl.Builder()
            .setAllocator(DefaultAllocator(true, ALLOCATOR_BUFFER_SIZE))
            .setBufferDurationsMs(
                tier.minBufferMs,
                tier.maxBufferMs,
                BUFFER_FOR_PLAYBACK_MS,
                BUFFER_FOR_REBUFFER_MS,
            )
            .setBackBuffer(tier.backBufferMs, /* retainBackBufferFromKeyframe = */ true)
            .setPrioritizeTimeOverSizeThresholds(true)
            .setTargetBufferBytes(tier.targetBufferBytes)
            .build()
    }

    /**
     * Decoder fallback is the important flag: without it a hardware decoder that fails to
     * configure ends playback instead of falling back to the platform's software codec, and a
     * codec the device can only run in software at 1080p is what pins a big core for the whole
     * session. The extension-renderer line is a no-op while no codec extensions are bundled;
     * it is here so bundling one later needs no code change.
     */
    fun createRenderersFactory(context: Context): DefaultRenderersFactory =
        DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

    /**
     * Track selection capped to a resolution the heap can actually decode smoothly. The stream
     * picker already honours the quality preference; this is the safety net for anything that
     * slips past it.
     */
    fun createTrackSelector(context: Context): DefaultTrackSelector {
        val tier = memoryTier(context)
        return DefaultTrackSelector(context, AdaptiveTrackSelection.Factory()).apply {
            setParameters(
                buildUponParameters()
                    .setViewportSizeToPhysicalDisplaySize(context, true)
                    .setMaxVideoSize(tier.maxVideoWidth, tier.maxVideoHeight)
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .build(),
            )
        }
    }

    @Volatile private var videoCache: SimpleCache? = null
    @Volatile private var cacheDatabaseProvider: StandaloneDatabaseProvider? = null

    /**
     * The video stream cache, shared by every playback of the process. Bounded by an LRU
     * evictor so a long session can never fill the disk. Kept for the process lifetime -
     * releasing it between videos would throw away the one thing that makes seeking back and
     * replaying cheap.
     */
    @Synchronized
    fun getOrCreateCache(context: Context): SimpleCache =
        videoCache ?: run {
            val appContext = context.applicationContext
            val db = cacheDatabaseProvider
                ?: StandaloneDatabaseProvider(appContext).also { cacheDatabaseProvider = it }
            SimpleCache(
                File(appContext.filesDir, VIDEO_CACHE_DIR_NAME),
                LeastRecentlyUsedCacheEvictor(VIDEO_CACHE_SIZE_BYTES),
                db,
            ).also { videoCache = it }
        }

    /**
     * The data source the video player reads streams through: a disk cache in front of the
     * same OkHttp stack the music player uses, so proxy settings apply to video too. Cache
     * failures degrade to plain network reads instead of failing the playback.
     */
    fun createDataSourceFactory(context: Context): DataSource.Factory {
        val appContext = context.applicationContext
        val okHttpClient = OkHttpClient
            .Builder()
            .proxy(YouTube.proxy)
            .proxyAuthenticator { _, response ->
                YouTube.proxyAuth?.let { auth ->
                    response.request.newBuilder()
                        .header("Proxy-Authorization", auth)
                        .build()
                } ?: response.request
            }
            .build()
        val cacheFactory = CacheDataSource.Factory()
            .setCache(getOrCreateCache(appContext))
            .setUpstreamDataSourceFactory(
                DefaultDataSource.Factory(appContext, OkHttpDataSource.Factory(okHttpClient)),
            )
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        // TV builds stay streaming-only, mirroring the music cache policy: nothing accumulates
        // on a device whose storage the user cannot inspect.
        if (appContext.packageManager.hasSystemFeature(
                android.content.pm.PackageManager.FEATURE_LEANBACK,
            )
        ) {
            cacheFactory.setCacheWriteDataSinkFactory(null)
        }
        return cacheFactory
    }
}
