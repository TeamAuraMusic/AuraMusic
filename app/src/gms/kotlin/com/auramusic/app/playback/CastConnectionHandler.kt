package com.auramusic.app.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.Player
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaQueueItem
import com.google.android.gms.cast.MediaSeekOptions
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManager
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
import com.auramusic.app.extensions.metadata
import com.auramusic.app.models.MediaMetadata as AppMediaMetadata
import com.auramusic.app.ui.utils.resize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class CastConnectionHandler(
    private val context: Context,
    private val scope: CoroutineScope,
    private val musicService: Any? = null
) {
    private var castContext: CastContext? = null
    private var sessionManager: SessionManager? = null
    private var mediaRouter: MediaRouter? = null
    private var routeSelector: MediaRouteSelector? = null
    private var remoteMediaClient: RemoteMediaClient? = null
    private var castSession: CastSession? = null
    
    private val _isCasting = MutableStateFlow(false)
    val isCasting: StateFlow<Boolean> = _isCasting.asStateFlow()
    
    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()
    
    private val _castDeviceName = MutableStateFlow<String?>(null)
    val castDeviceName: StateFlow<String?> = _castDeviceName.asStateFlow()
    
    private val _castPosition = MutableStateFlow(0L)
    val castPosition: StateFlow<Long> = _castPosition.asStateFlow()
    
    private val _castDuration = MutableStateFlow(0L)
    val castDuration: StateFlow<Long> = _castDuration.asStateFlow()
    
    private val _castIsPlaying = MutableStateFlow(false)
    val castIsPlaying: StateFlow<Boolean> = _castIsPlaying.asStateFlow()
    
    private val _castVolume = MutableStateFlow(1.0f)
    val castVolume: StateFlow<Float> = _castVolume.asStateFlow()
    
    private val musicServiceImpl: MusicService?
        get() = musicService as? MusicService
    
    private var positionUpdateJob: Job? = null
    private var currentMediaId: String? = null
    private var lastCastItemId: Int = -1
    private var isReloadingQueue: Boolean = false
    
    var isSyncingFromCast: Boolean = false
        private set
    
    private val remoteMediaClientCallback = object : RemoteMediaClient.Callback() {
        override fun onStatusUpdated() {
            remoteMediaClient?.let { client ->
                val mediaStatus = client.mediaStatus
                val playerState = mediaStatus?.playerState
                _castIsPlaying.value = playerState == MediaStatus.PLAYER_STATE_PLAYING ||
                                       playerState == MediaStatus.PLAYER_STATE_BUFFERING ||
                                       playerState == MediaStatus.PLAYER_STATE_LOADING
                _castDuration.value = client.streamDuration

                val currentItemId = mediaStatus?.currentItemId ?: -1
                if (currentItemId != -1 && currentItemId != lastCastItemId && lastCastItemId != -1 && !isReloadingQueue && mediaStatus != null) {
                    handleCastItemChanged(mediaStatus)
                }
                lastCastItemId = currentItemId
            }
        }
        
        override fun onMediaError(error: com.google.android.gms.cast.MediaError) {
            Timber.e("Cast media error: ${error.reason}")
        }
    }
    
    private var syncResetJob: Job? = null
    
    private fun handleCastItemChanged(mediaStatus: MediaStatus) {
        val queueItems = mediaStatus.queueItems
        if (queueItems.isEmpty()) return
        val currentItemId = mediaStatus.currentItemId
        val currentIndex = queueItems.indexOfFirst { it.itemId == currentItemId }
        
        if (currentIndex < 0) return
        
        val currentQueueItem = queueItems[currentIndex]
        val customData = currentQueueItem.media?.customData
        val castMediaId = customData?.optString("mediaId")
        
        if (castMediaId != null && castMediaId != currentMediaId) {
            currentMediaId = castMediaId
            
            syncResetJob?.cancel()
            isSyncingFromCast = true
            
            val player = musicServiceImpl?.player
            val playerItemCount = player.mediaItemCount
            
            for (i in 0 until playerItemCount) {
                val mediaItem = player.getMediaItemAt(i)
                if (mediaItem.mediaId == castMediaId) {
                    player.pause()
                    player.seekTo(i, 0)
                    player.pause()
                    
                    val itemsAhead = queueItems.size - 1 - currentIndex
                    val itemsBehind = currentIndex
                    
                    if (itemsAhead < 2 || itemsBehind < 2) {
                        scope.launch {
                            val metadata = mediaItem.metadata
                            if (metadata != null) {
                                extendQueueIfNeeded(i, playerItemCount, queueItems)
                            }
                        }
                    }
                    break
                }
            }
            
            syncResetJob = scope.launch {
                delay(300)
                isSyncingFromCast = false
            }
        }
    }
    
    private suspend fun extendQueueIfNeeded(localPlayerIndex: Int, playerItemCount: Int, currentCastQueue: List<MediaQueueItem>) {
        if (isReloadingQueue) return
        
        val client = remoteMediaClient ?: return
        val currentCastIndex = currentCastQueue.indexOfFirst { 
            it.media?.customData?.optString("mediaId") == currentMediaId 
        }
        if (currentCastIndex < 0) return
        
        isReloadingQueue = true
        
        try {
            val itemsAhead = currentCastQueue.size - 1 - currentCastIndex
            if (itemsAhead < 2) {
                val lastCastItem = currentCastQueue.lastOrNull()
                val lastMediaId = lastCastItem?.media?.customData?.optString("mediaId")
                
                var lastLocalIndex = -1
                for (i in 0 until playerItemCount) {
                    if (musicServiceImpl?.player.getMediaItemAt(i).mediaId == lastMediaId) {
                        lastLocalIndex = i
                        break
                    }
                }
                
                if (lastLocalIndex >= 0 && lastLocalIndex < playerItemCount - 1) {
                    val itemsToAdd = mutableListOf<MediaQueueItem>()
                    val addCount = minOf(2, playerItemCount - lastLocalIndex - 1)
                    
                    for (i in 1..addCount) {
                        val nextItem = musicServiceImpl?.player.getMediaItemAt(lastLocalIndex + i)
                        nextItem.metadata?.let { metadata ->
                            buildMediaInfo(metadata)?.let { mediaInfo ->
                                itemsToAdd.add(MediaQueueItem.Builder(mediaInfo).build())
                            }
                        }
                    }
                    
                    if (itemsToAdd.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            client.queueAppendItem(itemsToAdd.first(), null)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to extend Cast queue")
        } finally {
            delay(500)
            isReloadingQueue = false
        }
    }
    
    private val sessionManagerListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) {
            Timber.d("Cast session starting")
            _isConnecting.value = true
        }
        
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            Timber.d("Cast session started: $sessionId")
            _isCasting.value = true
            _isConnecting.value = false
            _castDeviceName.value = session.castDevice?.friendlyName
            castSession = session
            remoteMediaClient = session.remoteMediaClient
            remoteMediaClient?.registerCallback(remoteMediaClientCallback)
            
            _castVolume.value = session.volume.toFloat()
            
            startPositionUpdates()
            
            loadCurrentMedia()
        }
        
        override fun onSessionStartFailed(session: CastSession, error: Int) {
            Timber.e("Cast session start failed: $error")
            _isCasting.value = false
            _isConnecting.value = false
        }
        
        override fun onSessionEnding(session: CastSession) {
            val castPosition = remoteMediaClient?.approximateStreamPosition ?: _castPosition.value
            if (castPosition > 0) {
                musicServiceImpl?.player.seekTo(castPosition)
            }
        }
        
        override fun onSessionEnded(session: CastSession, error: Int) {
            Timber.d("Cast session ended: error=$error")
            _isCasting.value = false
            _isConnecting.value = false
            _castDeviceName.value = null
            castSession = null
            
            remoteMediaClient?.unregisterCallback(remoteMediaClientCallback)
            remoteMediaClient = null
            
            stopPositionUpdates()
            
            musicServiceImpl?.player.pause()
        }
        
        override fun onSessionResuming(session: CastSession, sessionId: String) {
            _isConnecting.value = true
        }
        
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            _isCasting.value = true
            _isConnecting.value = false
            _castDeviceName.value = session.castDevice?.friendlyName
            
            remoteMediaClient = session.remoteMediaClient
            remoteMediaClient?.registerCallback(remoteMediaClientCallback)
            
            startPositionUpdates()
        }
        
        override fun onSessionResumeFailed(session: CastSession, error: Int) {
            _isConnecting.value = false
        }
        
        override fun onSessionSuspended(session: CastSession, reason: Int) {}
    }
    
    fun initialize(): Boolean {
        return try {
            castContext = CastContext.getSharedInstance(context)
            sessionManager = castContext?.sessionManager
            mediaRouter = MediaRouter.getInstance(context)
            routeSelector = MediaRouteSelector.Builder()
                .addControlCategory(CastMediaControlIntent.categoryForCast(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID))
                .build()
            
            sessionManager?.addSessionManagerListener(sessionManagerListener, CastSession::class.java)
            
            sessionManager?.currentCastSession?.let { session ->
                _isCasting.value = true
                _castDeviceName.value = session.castDevice?.friendlyName
                remoteMediaClient = session.remoteMediaClient
                remoteMediaClient?.registerCallback(remoteMediaClientCallback)
                startPositionUpdates()
            }
            
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Cast")
            false
        }
    }
    
    fun getAvailableRoutes(): List<MediaRouter.RouteInfo> {
        val router = mediaRouter ?: return emptyList()
        val selector = routeSelector ?: return emptyList()
        
        return router.routes.filter { route ->
            route.matchesSelector(selector) && !route.isDefault
        }
    }
    
    fun connectToRoute(route: MediaRouter.RouteInfo) {
        if (mediaRouter == null) {
            initialize()
        }
        _isConnecting.value = true
        mediaRouter?.selectRoute(route)
    }
    
    fun disconnect() {
        sessionManager?.endCurrentSession(true)
    }
    
    fun loadCurrentMedia() {
        val metadata = musicServiceImpl?.currentMediaMetadata.value ?: return
        loadMediaWithQueue(metadata)
    }
    
    fun loadMedia(metadata: AppMediaMetadata) {
        loadMediaWithQueue(metadata)
    }
    
    private suspend fun buildMediaInfo(metadata: AppMediaMetadata): MediaInfo? {
        val streamUrl = musicServiceImpl?.getStreamUrl(metadata.id) ?: return null
        
        val castMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MUSIC_TRACK).apply {
            putString(MediaMetadata.KEY_TITLE, metadata.title)
            putString(MediaMetadata.KEY_ARTIST, metadata.artists.joinToString(", ") { it.name })
            metadata.album?.title?.let { putString(MediaMetadata.KEY_ALBUM_TITLE, it) }
            metadata.thumbnailUrl?.let { thumbUrl ->
                val highQualityUrl = thumbUrl.resize(1080, 1080)
                addImage(WebImage(Uri.parse(highQualityUrl)))
            }
        }
        
        return MediaInfo.Builder(streamUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType("audio/mp4")
            .setMetadata(castMetadata)
            .setCustomData(org.json.JSONObject().put("mediaId", metadata.id))
            .build()
    }
    
    private fun loadMediaWithQueue(metadata: AppMediaMetadata) {
        if (!_isCasting.value) return
        
        isReloadingQueue = true
        scope.launch {
            try {
                currentMediaId = metadata.id
                lastCastItemId = -1
                
                val player = musicServiceImpl?.player
                val currentIndex = player.currentMediaItemIndex
                val mediaItemCount = player.mediaItemCount
                val shuffleEnabled = player.shuffleModeEnabled
                val timeline = player.currentTimeline
                
                val queueItems = mutableListOf<MediaQueueItem>()
                
                val prevItems = mutableListOf<androidx.media3.common.MediaItem>()
                if (!timeline.isEmpty) {
                    var prevIdx = currentIndex
                    for (i in 0 until 2) {
                        prevIdx = timeline.getPreviousWindowIndex(prevIdx, Player.REPEAT_MODE_OFF, shuffleEnabled)
                        if (prevIdx == androidx.media3.common.C.INDEX_UNSET) break
                        prevItems.add(0, player.getMediaItemAt(prevIdx))
                    }
                }
                
                for (prevItem in prevItems) {
                    prevItem.metadata?.let { prevMetadata ->
                        buildMediaInfo(prevMetadata)?.let { mediaInfo ->
                            queueItems.add(MediaQueueItem.Builder(mediaInfo).build())
                        }
                    }
                }
                val startIndex = queueItems.size
                
                val currentMediaInfo = buildMediaInfo(metadata)
                if (currentMediaInfo == null) {
                    Timber.e("Failed to get stream URL for Cast")
                    return@launch
                }
                queueItems.add(MediaQueueItem.Builder(currentMediaInfo).build())
                
                if (!timeline.isEmpty) {
                    var nextIdx = currentIndex
                    for (i in 0 until 2) {
                        nextIdx = timeline.getNextWindowIndex(nextIdx, Player.REPEAT_MODE_OFF, shuffleEnabled)
                        if (nextIdx == androidx.media3.common.C.INDEX_UNSET) break
                        val nextItem = player.getMediaItemAt(nextIdx)
                        nextItem.metadata?.let { nextMetadata ->
                            buildMediaInfo(nextMetadata)?.let { mediaInfo ->
                                queueItems.add(MediaQueueItem.Builder(mediaInfo).build())
                            }
                        }
                    }
                }
                
                val startPosition = if (player.currentMediaItem?.mediaId == metadata.id) {
                    player.currentPosition
                } else {
                    0L
                }
                
                withContext(Dispatchers.Main) {
                    val client = remoteMediaClient ?: return@withContext
                    
                    client.queueLoad(
                        queueItems.toTypedArray(),
                        startIndex,
                        MediaStatus.REPEAT_MODE_REPEAT_OFF,
                        startPosition,
                        org.json.JSONObject()
                    )
                    
                    musicServiceImpl?.player.pause()
                }
                
                delay(1500)
                isReloadingQueue = false
            } catch (e: Exception) {
                Timber.e(e, "Failed to load media on Cast")
            } finally {
                delay(1500)
                isReloadingQueue = false
            }
        }
    }
    
    fun play() {
        remoteMediaClient?.play()
    }
    
    fun pause() {
        remoteMediaClient?.pause()
    }
    
    fun seekTo(position: Long) {
        val seekOptions = MediaSeekOptions.Builder()
            .setPosition(position)
            .build()
        remoteMediaClient?.seek(seekOptions)
    }
    
    fun setVolume(volume: Float) {
        try {
            val clampedVolume = volume.coerceIn(0f, 1f)
            castSession?.volume = clampedVolume.toDouble()
            _castVolume.value = clampedVolume
        } catch (e: Exception) {
            Timber.e(e, "Failed to set Cast volume")
        }
    }
    
    fun navigateToMediaIfInQueue(mediaId: String): Boolean {
        val client = remoteMediaClient ?: return false
        val mediaStatus = client.mediaStatus ?: return false
        val queueItems = mediaStatus.queueItems
        if (queueItems.isEmpty()) return false
        
        val targetIndex = queueItems.indexOfFirst { 
            it.media?.customData?.optString("mediaId") == mediaId 
        }
        
        if (targetIndex < 0) {
            return false
        }
        
        val currentItemId = mediaStatus.currentItemId
        val currentIndex = queueItems.indexOfFirst { it.itemId == currentItemId }
        
        if (targetIndex == currentIndex) {
            currentMediaId = mediaId
            musicServiceImpl?.player.pause()
            return true
        }
        
        val targetItem = queueItems[targetIndex]
        
        isSyncingFromCast = true
        
        val player = musicServiceImpl?.player
        for (i in 0 until player.mediaItemCount) {
            if (player.getMediaItemAt(i).mediaId == mediaId) {
                player.seekTo(i, 0)
                break
            }
        }
        player.pause()
        
        client.queueJumpToItem(targetItem.itemId, org.json.JSONObject())
        currentMediaId = mediaId
        
        scope.launch {
            delay(300)
            isSyncingFromCast = false
        }
        
        return true
    }
    
    fun skipToNext() {
        val client = remoteMediaClient
        val mediaStatus = client?.mediaStatus
        if (mediaStatus != null && mediaStatus.queueItemCount > 0) {
            val currentItemId = mediaStatus.currentItemId
            val queueItems = mediaStatus.queueItems
            val currentIndex = queueItems.indexOfFirst { it.itemId == currentItemId }
            if (currentIndex >= 0 && currentIndex < queueItems.size - 1) {
                client.queueNext(org.json.JSONObject())
                musicServiceImpl?.player.pause()
                return
            }
        }
        
        val player = musicServiceImpl?.player
        if (player.hasNextMediaItem()) {
            player.pause()
            player.seekToNextMediaItem()
        }
    }
    
    fun skipToPrevious() {
        val client = remoteMediaClient
        val mediaStatus = client?.mediaStatus
        if (mediaStatus != null && mediaStatus.queueItemCount > 0) {
            val currentItemId = mediaStatus.currentItemId
            val queueItems = mediaStatus.queueItems
            val currentIndex = queueItems.indexOfFirst { it.itemId == currentItemId }
            if (currentIndex > 0) {
                client.queuePrev(org.json.JSONObject())
                musicServiceImpl?.player.pause()
                return
            }
        }
        
        val player = musicServiceImpl?.player
        if (player.hasPreviousMediaItem()) {
            player.pause()
            player.seekToPreviousMediaItem()
        }
    }
    
    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = scope.launch {
            while (isActive && _isCasting.value) {
                remoteMediaClient?.let { client ->
                    _castPosition.value = client.approximateStreamPosition
                }
                delay(500)
            }
        }
    }
    
    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }
    
    fun release() {
        stopPositionUpdates()
        remoteMediaClient?.unregisterCallback(remoteMediaClientCallback)
        sessionManager?.removeSessionManagerListener(sessionManagerListener, CastSession::class.java)
    }
}