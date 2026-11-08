package com.auramusic.app.tv

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RectF
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.OverlaySettings
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.StaticOverlaySettings

/** Animated, transparent equalizer badge composited over the original artwork. */
@OptIn(UnstableApi::class)
internal class PlaybackIndicatorOverlay(
    width: Int,
    height: Int,
) : BitmapOverlay() {
    private val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    private val canvas = Canvas(bitmap)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(176, 8, 10, 20)
    }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 74, 166)
    }
    private val panel = RectF()
    private var lastFrameMs = -1L
    private val overlaySettings: OverlaySettings = StaticOverlaySettings.Builder()
        .setBackgroundFrameAnchor(0f, 0f)
        .setOverlayFrameAnchor(0f, 0f)
        .build()

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val timeMs = presentationTimeUs / 1_000L
        if (lastFrameMs >= 0L && timeMs / FRAME_INTERVAL_MS == lastFrameMs / FRAME_INTERVAL_MS) return bitmap
        lastFrameMs = timeMs
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val scale = (bitmap.width.coerceAtMost(bitmap.height) / 256f).coerceAtLeast(1f)
        val barWidth = 5f * scale
        val gap = 5f * scale
        val barsWidth = BAR_COUNT * barWidth + (BAR_COUNT - 1) * gap
        val panelPadding = 13f * scale
        val panelHeight = 56f * scale
        val panelWidth = barsWidth + panelPadding * 2
        val left = (bitmap.width - panelWidth) / 2f
        // Projectivy lays preview videos into a landscape card viewport and center-crops
        // square artwork. Keep the indicator inside the shared safe area instead of at
        // the source image's bottom edge, where the launcher crops it away.
        val top = bitmap.height * 0.68f - panelHeight / 2f
        panel.set(left, top, left + panelWidth, top + panelHeight)
        canvas.drawRoundRect(panel, panelHeight / 2f, panelHeight / 2f, backgroundPaint)

        val centerY = top + panelHeight / 2f
        for (index in 0 until BAR_COUNT) {
            val phase = (timeMs * 0.012 + index * 1.13).toFloat()
            val height = (10f + (kotlin.math.sin(phase) + 1f) * 13f) * scale
            val x = left + panelPadding + index * (barWidth + gap)
            canvas.drawRoundRect(
                RectF(x, centerY - height / 2f, x + barWidth, centerY + height / 2f),
                barWidth / 2f,
                barWidth / 2f,
                barPaint,
            )
        }
        return bitmap
    }

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings = overlaySettings

    override fun release() {
        super.release()
        if (!bitmap.isRecycled) bitmap.recycle()
    }

    private companion object {
        const val BAR_COUNT = 5
        const val FRAME_INTERVAL_MS = 250L
    }
}
