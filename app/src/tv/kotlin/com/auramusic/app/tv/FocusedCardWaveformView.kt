package com.auramusic.app.tv

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView

/** Artwork stays static; only the small waveform child invalidates during playback. */
internal class FocusedCardWaveformView(context: Context) : FrameLayout(context) {
    private val artworkView = ImageView(context).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        // Projectivy may keep the previous preview Surface frame visible while it
        // switches programs. Cover that frame immediately until this card's cached
        // artwork is installed; transparency here makes old artwork appear to slide
        // onto the newly focused card.
        setBackgroundColor(Color.BLACK)
    }
    private val waveformView = WaveformBarsView(context)

    init {
        val radius = 10f * resources.displayMetrics.density
        background = GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            cornerRadius = radius
        }
        clipToOutline = true
        addView(artworkView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(waveformView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun setArtwork(bitmap: android.graphics.Bitmap?) {
        artworkView.setImageBitmap(bitmap)
    }

    fun maskPreviousPreviewFrame() {
        // Keep this card and its playback indicator visible while Projectivy swaps its
        // Surface frame. Hiding the whole overlay exposes the launcher's stale frame.
        setArtwork(null)
        setPreviewFocused(true)
    }

    fun setPreviewFocused(focused: Boolean) {
        visibility = if (focused) View.VISIBLE else View.INVISIBLE
        waveformView.setPreviewFocused(focused)
    }

    private class WaveformBarsView(context: Context) : View(context) {
        private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(205, 8, 10, 20)
        }
        private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 74, 166)
        }
        private val panel = RectF()
        private val bar = RectF()
        private val bars = floatArrayOf(0.30f, 0.58f, 0.86f, 0.52f, 0.74f, 0.42f, 0.90f, 0.57f, 0.34f)
        private val density = resources.displayMetrics.density
        private val radius = 10f * density
        private var previewFocused = false
        private val paintTick = object : Runnable {
            override fun run() {
                invalidate()
                postDelayed(this, FRAME_INTERVAL_MS)
            }
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            if (previewFocused) post(paintTick)
        }

        fun setPreviewFocused(focused: Boolean) {
            if (previewFocused == focused) return
            previewFocused = focused
            visibility = if (focused) View.VISIBLE else View.INVISIBLE
            if (focused && isAttachedToWindow) post(paintTick) else removeCallbacks(paintTick)
            invalidate()
        }

        override fun onDetachedFromWindow() {
            removeCallbacks(paintTick)
            super.onDetachedFromWindow()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if (!previewFocused || width <= 0 || height <= 0) return

            val panelWidth = width * 0.48f
            val panelHeight = height * 0.24f
            panel.set(
                (width - panelWidth) / 2f,
                height * 0.68f,
                (width + panelWidth) / 2f,
                height * 0.68f + panelHeight,
            )
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
        }

        private companion object {
            const val FRAME_INTERVAL_MS = 50L
        }
    }
}
