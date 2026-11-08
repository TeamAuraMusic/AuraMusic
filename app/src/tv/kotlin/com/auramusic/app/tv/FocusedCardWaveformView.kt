package com.auramusic.app.tv

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.SystemClock
import android.view.View

/** Lightweight pink equalizer drawn inside the focused program card. */
internal class FocusedCardWaveformView(context: Context) : View(context) {
    private val artworkPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(205, 8, 10, 20) }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(255, 74, 166) }
    private val panel = RectF()
    private val bar = RectF()
    private val artworkRect = RectF()
    private val cardPath = Path()
    private var artwork: Bitmap? = null
    private var playbackActive = false
    private val bars = floatArrayOf(0.30f, 0.58f, 0.86f, 0.52f, 0.74f, 0.42f, 0.90f, 0.57f, 0.34f)
    private val density = resources.displayMetrics.density
    private val radius = 10f * density
    private val paintTick = object : Runnable {
        override fun run() {
            invalidate()
            postDelayed(this, FRAME_INTERVAL_MS)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (playbackActive) post(paintTick)
    }

    fun setArtwork(bitmap: Bitmap?) {
        artwork = bitmap
        invalidate()
    }

    fun setPlaybackActive(active: Boolean) {
        val changed = playbackActive != active
        playbackActive = active
        visibility = if (active) View.VISIBLE else View.INVISIBLE
        if (changed) {
            if (active && isAttachedToWindow) post(paintTick) else removeCallbacks(paintTick)
            invalidate()
        }
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(paintTick)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return
        val save = canvas.save()
        panel.set(0f, 0f, width.toFloat(), height.toFloat())
        cardPath.reset()
        cardPath.addRoundRect(panel, radius, radius, Path.Direction.CW)
        canvas.clipPath(cardPath)
        artwork?.let { bitmap ->
            val scale = maxOf(width / bitmap.width.toFloat(), height / bitmap.height.toFloat())
            val drawWidth = bitmap.width * scale
            val drawHeight = bitmap.height * scale
            val left = (width - drawWidth) / 2f
            val top = (height - drawHeight) / 2f
            artworkRect.set(left, top, left + drawWidth, top + drawHeight)
            canvas.drawBitmap(bitmap, null, artworkRect, artworkPaint)
        }

        if (!playbackActive) {
            canvas.restoreToCount(save)
            return
        }

        val panelWidth = width * 0.48f
        val panelHeight = height * 0.24f
        panel.set((width - panelWidth) / 2f, height * 0.68f, (width + panelWidth) / 2f, height * 0.68f + panelHeight)
        canvas.drawRoundRect(panel, radius, radius, panelPaint)
        val pad = maxOf(4f * density, panelWidth * 0.07f)
        val centerY = panel.centerY()
        val gap = panelWidth * 0.025f
        val barWidth = (panelWidth - 2f * pad - gap * (bars.size - 1)) / bars.size
        val phase = SystemClock.uptimeMillis() / 170.0
        val maxHeight = panelHeight * 0.66f
        for (index in bars.indices) {
            val oscillation = (0.25f + 0.75f * kotlin.math.abs(kotlin.math.sin(phase + index * 0.78))).toFloat()
            val barHeight = maxHeight * bars[(index + (phase.toInt() % bars.size)) % bars.size] * oscillation
            val left = panel.left + pad + index * (barWidth + gap)
            bar.set(left, centerY - barHeight / 2f, left + barWidth, centerY + barHeight / 2f)
            canvas.drawRoundRect(bar, barWidth / 2f, barWidth / 2f, barPaint)
        }
        canvas.restoreToCount(save)
    }

    private companion object {
        const val FRAME_INTERVAL_MS = 50L
    }
}
