/**
 * Auramusic Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.auramusic.app.sponsorblock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class SponsorBlockSeekBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private var segments: List<SeekBarSegment> = emptyList()
    private val segmentPaints = mutableMapOf<Int, Paint>()
    private val defaultSegmentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    fun setSegments(newSegments: List<SeekBarSegment>) {
        segments = newSegments
        // Pre-create paints for each color
        newSegments.forEach { seg ->
            if (seg.color !in segmentPaints) {
                segmentPaints[seg.color] = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = seg.color
                }
            }
        }
        invalidate()
    }

    fun clearSegments() {
        segments = emptyList()
        segmentPaints.clear()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (segments.isEmpty() || width <= 0) return

        val barHeight = height.toFloat()
        val barWidth = width.toFloat()
        val cornerRadius = barHeight / 2f

        for (segment in segments) {
            val paint = segmentPaints[segment.color] ?: defaultSegmentPaint.apply {
                color = segment.color
            }

            val left = segment.startProgress * barWidth
            val right = segment.endProgress * barWidth

            // Ensure minimum width so tiny segments are visible
            val minWidth = barHeight / 2f
            val adjustedRight = if (right - left < minWidth) left + minWidth else right

            val rect = RectF(
                left.coerceAtLeast(0f),
                0f,
                adjustedRight.coerceAtMost(barWidth),
                barHeight,
            )
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
        }
    }
}
