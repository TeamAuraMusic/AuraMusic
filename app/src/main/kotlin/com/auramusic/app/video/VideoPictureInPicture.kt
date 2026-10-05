/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.video

import android.app.Activity
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.Context
import android.graphics.drawable.Icon
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.util.Rational
import androidx.annotation.RequiresApi

/**
 * Entry into, and bookkeeping for, the video picture-in-picture window.
 *
 * Kept out of the Compose overlay on purpose: PiP is an Activity-level transition, and going
 * through the Activity keeps the decision in one place rather than spread across a composable
 * that may or may not be composed when the user leaves the app.
 */
object VideoPictureInPicture {

    private const val TAG = "VideoPiP"

    /**
     * Rect of the on-screen video, handed to the platform so the PiP window can animate out of
     * the right place. Android ignores it if empty.
     */
    @Volatile
    var sourceRectHint: Rect? = null
        private set

    /**
     * Aspect ratio of the window most recently requested. Picture-in-picture params have to be
     * rebuilt on every change, and the platform discards the previous ones, so the last ratio is
     * retained to avoid resetting it unnecessarily.
     */
    @Volatile
    private var currentAspectRatio: Float = DEFAULT_VIDEO_ASPECT_RATIO

    private var activity: Activity? = null

    /** Binds the Activity that owns the PiP window. Call from its lifecycle. */
    fun attach(activity: Activity?) {
        this.activity = activity
    }

    /**
     * Whether this device and the bound Activity can show a PiP window at all. Some TV and
     * low-end devices omit the feature entirely, and the overlay hides its PiP button rather
     * than offering one that silently fails.
     */
    fun isSupported(): Boolean {
        val bound = activity ?: return false
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            bound.packageManager.hasSystemFeature(
                android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE
            )
    }

    fun updateSourceRectHint(rect: Rect?) {
        if (rect == null || rect.isEmpty) return
        sourceRectHint = Rect(rect)
    }

    fun clearSourceRectHint() {
        sourceRectHint = null
    }

    /**
     * Enters picture-in-picture.
     *
     * @param aspectRatio display ratio of the video, or null before the decoder has reported one.
     * @param isPlaying whether the video is currently playing, which decides if a play/pause
     * action is offered.
     * @return true if the window is now in PiP.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun enter(activity: Activity, aspectRatio: Float?, isPlaying: Boolean): Boolean {
        if (!activity.isPipCapable()) return false

        val safeRatio = sanitizePipAspectRatio(aspectRatio ?: DEFAULT_VIDEO_ASPECT_RATIO)
        currentAspectRatio = safeRatio
        val params = buildParams(activity, safeRatio, isPlaying)

        return runCatching { activity.enterPictureInPictureMode(params) }
            .onFailure { Log.w(TAG, "Unable to enter picture-in-picture", it) }
            .isSuccess
    }

    /**
     * Rebuilds and reapplies the PiP params. Called while already in PiP when playback or the
     * video's ratio changes — the platform keeps using the params it was given at entry
     * otherwise.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun update(activity: Activity, aspectRatio: Float?, isPlaying: Boolean) {
        if (!activity.isInPictureInPictureMode) return
        val safeRatio = sanitizePipAspectRatio(aspectRatio ?: currentAspectRatio)
        val params = buildParams(activity, safeRatio, isPlaying)
        runCatching { activity.setPictureInPictureParams(params) }
            .onFailure { Log.w(TAG, "Unable to update picture-in-picture params", it) }
        currentAspectRatio = safeRatio
    }

    /**
     * True when the app should drop into PiP by itself as the user leaves, rather than waiting
     * for an explicit button press.
     */
    fun shouldAutoEnter(isPlaying: Boolean, isVideoSessionActive: Boolean, isMusicPlaying: Boolean): Boolean =
        isPlaying && isVideoSessionActive && !isMusicPlaying

    private fun Activity.isPipCapable(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildParams(
        context: Context,
        aspectRatio: Float,
        isPlaying: Boolean,
    ): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(pipRational(aspectRatio))
            .setActions(listOf(buildTransportAction(context, isPlaying)))

        // Auto-enter is what makes "leave the app, keep watching" work without a button press.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(true)
            // Seamless resize re-lays-out the video on every window resize, which restarts the
            // SurfaceView pipeline mid-playback and is visible as a stutter.
            builder.setSeamlessResizeEnabled(false)
        }

        sourceRectHint?.takeIf { !it.isEmpty }?.let { builder.setSourceRectHint(it) }

        return builder.build()
    }

    /**
     * The system clamps this to 0.418..2.39 and throws outside it, so the value arriving here is
     * already sanitised; rounding keeps the ratio a stable rational rather than a float widened
     * into the platform's tolerance.
     */
    private fun pipRational(aspectRatio: Float): Rational {
        val clamped = aspectRatio.coerceIn(1f / MAX_PIP_ASPECT_RATIO, MAX_PIP_ASPECT_RATIO)
        return Rational((clamped * 1000).toInt().coerceAtLeast(1), 1000)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildTransportAction(context: Context, isPlaying: Boolean): RemoteAction {
        val resId = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        // android.R.string.play is not present across the whole PiP API range (it starts at 28,
        // PiP at 26), so the label falls back to the app's own string.
        val label = context.getString(
            if (isPlaying) com.auramusic.app.R.string.pause else com.auramusic.app.R.string.play
        )
        val intent = Intent(context, VideoPlaybackService::class.java).apply {
            action = VideoPlaybackService.ACTION_COMMAND_TOGGLE_PLAY_PAUSE
        }
        // RemoteAction's public constructor is used rather than its Builder: the Builder is
        // marked @hide in the platform SDK, so it is unavailable at compile time.
        return RemoteAction(
            Icon.createWithResource(context, resId),
            label,
            label,
            context.getPendingIntent(intent),
        )
    }

    private fun Context.getPendingIntent(intent: Intent): android.app.PendingIntent =
        android.app.PendingIntent.getService(
            this,
            0,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                android.app.PendingIntent.FLAG_IMMUTABLE,
        )
}