/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.video

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.auramusic.innertube.YouTube
import java.io.File
import okhttp3.OkHttpClient

/**
 * The video stream cache and the data source built on top of it.
 *
 * The player's own policies - buffers, renderers, track selection - live with the shared
 * music player that now renders video as well; what stays here is the one thing that is
 * genuinely video-specific: a bounded on-disk cache so seeking back and replaying a video
 * does not download the same bytes twice.
 */
@UnstableApi
object VideoPlayerSupport {

    private const val VIDEO_CACHE_SIZE_BYTES = 256L * 1024L * 1024L
    private const val VIDEO_CACHE_DIR_NAME = "video_cache"

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
