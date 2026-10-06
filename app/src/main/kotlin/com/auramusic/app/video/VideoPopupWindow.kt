/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.video

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.auramusic.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A small video window drawn over the rest of the app.
 *
 * System picture-in-picture shrinks the whole Activity: the app's own UI goes away and only a
 * tiny window is left over the launcher. This is the other shape - the Activity stays full
 * screen and usable, and the video floats above it. It needs the "display over other apps"
 * permission because the window has to outlive the bounds of a normal view.
 *
 * The video is rendered by a second PlayerView bound to the same player the in-app overlay
 * uses, so playback state, position and controls stay shared. Because a player can only feed
 * one surface at a time, [VideoPlayerOverlay] unmounts its own view while this window is up
 * and this window only attaches afterwards - see [SHOW_DELAY_MS].
 */
object VideoPopupWindow {

    /** Long enough for the in-app overlay to recompose away and release its surface. */
    private const val SHOW_DELAY_MS = 150L

    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active.asStateFlow()

    private val handler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var root: FrameLayout? = null
    private var playerView: PlayerView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    fun isPermissionGranted(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(context)

    fun requestPermission(activity: Activity) {
        val uri = Uri.parse("package:${activity.packageName}")
        runCatching {
            activity.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri))
        }.onFailure {
            runCatching {
                activity.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri),
                )
            }
        }
    }

    /**
     * Shows the floating window over [player]. Returns false when the overlay permission is
     * missing so the caller can send the user to the system toggle instead.
     */
    fun show(activity: Activity, player: ExoPlayer, aspectRatio: Float?): Boolean {
        if (_active.value) return true
        if (!isPermissionGranted(activity)) return false
        // Flag first: the overlay reacts to this and drops its own PlayerView. Attaching the
        // window afterwards keeps this view last in line for the video surface.
        _active.value = true
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ attach(activity, player, aspectRatio) }, SHOW_DELAY_MS)
        return true
    }

    fun hide() {
        handler.removeCallbacksAndMessages(null)
        _active.value = false
        detach()
    }

    @OptIn(UnstableApi::class)
    private fun attach(activity: Activity, player: ExoPlayer, aspectRatio: Float?) {
        if (!_active.value) return
        if (activity.isFinishing || activity.isDestroyed) {
            _active.value = false
            return
        }
        val manager = activity.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        if (manager == null) {
            _active.value = false
            return
        }

        val density = activity.resources.displayMetrics.density
        val screenWidth = activity.resources.displayMetrics.widthPixels
        val width = (screenWidth * 0.72f).toInt()
            .coerceIn((240 * density).toInt(), (420 * density).toInt())
        val ratio = aspectRatio?.takeIf { it > 0.5f && it < 3f } ?: (16f / 9f)
        val height = (width / ratio).toInt().coerceAtLeast((120 * density).toInt())

        val params = WindowManager.LayoutParams(
            width,
            height,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (screenWidth - width) / 2
            y = (72 * density).toInt()
        }

        val container = FrameLayout(activity).apply {
            setBackgroundColor(Color.BLACK)
            clipToOutline = true
        }

        val video = PlayerView(activity).apply {
            useController = true
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            setKeepContentOnPlayerReset(true)
        }
        container.addView(
            video,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )

        container.addView(
            buildButton(activity, R.drawable.close, R.string.close) { hide() },
            FrameLayout.LayoutParams(
                (48 * density).toInt(),
                (48 * density).toInt(),
                Gravity.TOP or Gravity.END,
            ),
        )
        container.addView(
            buildButton(activity, R.drawable.fullscreen, R.string.video_player_float_expand) {
                // expand() drops this window and brings the in-app player back.
                VideoPlaybackManager.expand()
            },
            FrameLayout.LayoutParams(
                (48 * density).toInt(),
                (48 * density).toInt(),
                Gravity.TOP or Gravity.START,
            ),
        )

        // Only the strip between the two buttons drags the window, so the buttons stay tappable.
        val dragHandle = View(activity)
        container.addView(
            dragHandle,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (44 * density).toInt(),
                Gravity.TOP,
            ).apply {
                marginStart = (48 * density).toInt()
                marginEnd = (48 * density).toInt()
            },
        )
        attachDrag(dragHandle, params, container, manager)

        try {
            manager.addView(container, params)
        } catch (e: Exception) {
            _active.value = false
            return
        }
        video.player = player
        windowManager = manager
        root = container
        playerView = video
        layoutParams = params
    }

    private fun detach() {
        val view = root ?: return
        playerView?.player = null
        runCatching { windowManager?.removeView(view) }
        root = null
        playerView = null
        layoutParams = null
        windowManager = null
    }

    private fun buildButton(
        context: Context,
        iconRes: Int,
        contentDescriptionRes: Int,
        onClick: () -> Unit,
    ): ImageButton {
        val density = context.resources.displayMetrics.density
        return ImageButton(context).apply {
            setImageResource(iconRes)
            imageTintList = ColorStateList.valueOf(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(140, 0, 0, 0))
            }
            contentDescription = context.getString(contentDescriptionRes)
            setOnClickListener { onClick() }
            val pad = (14 * density).toInt()
            setPadding(pad, pad, pad, pad)
        }
    }

    private fun attachDrag(
        handle: View,
        params: WindowManager.LayoutParams,
        container: View,
        manager: WindowManager,
    ) {
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        handle.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (event.rawX - touchX).toInt()
                    params.y = startY + (event.rawY - touchY).toInt()
                    runCatching { manager.updateViewLayout(container, params) }
                    true
                }
                else -> false
            }
        }
    }
}
