package com.auramusic.app.tv

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.squareup.gifencoder.GifEncoder
import com.squareup.gifencoder.ImageOptions
import com.squareup.gifencoder.NearestColorDitherer
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.sin

/** Full-artwork animated poster fallback for Projectivy versions that animate GIF posters. */
internal object TvPreviewGif {
    const val WIDTH = 256
    const val HEIGHT = 144

    fun encode(artwork: Bitmap, output: OutputStream) {
        val encoder = GifEncoder(output, WIDTH, HEIGHT, 0)
        val frame = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(frame)
        val artworkPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(190, 8, 10, 20)
        }
        val wavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 74, 166)
            style = Paint.Style.STROKE
            strokeWidth = 2.6f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val scale = maxOf(WIDTH / artwork.width.toFloat(), HEIGHT / artwork.height.toFloat())
        val scaledWidth = artwork.width * scale
        val scaledHeight = artwork.height * scale
        val artworkRect = RectF(
            (WIDTH - scaledWidth) / 2f,
            (HEIGHT - scaledHeight) / 2f,
            (WIDTH + scaledWidth) / 2f,
            (HEIGHT + scaledHeight) / 2f,
        )
        val panel = RectF(82f, 100f, 174f, 133f)
        val path = android.graphics.Path()
        try {
            val pixels = IntArray(WIDTH * HEIGHT)
            val options = ImageOptions()
                .setDelay(FRAME_DELAY_MS, TimeUnit.MILLISECONDS)
                .setDitherer(NearestColorDitherer.INSTANCE)
            repeat(FRAME_COUNT) { frameIndex ->
                canvas.drawBitmap(artwork, null, artworkRect, artworkPaint)
                canvas.drawRoundRect(panel, 20f, 20f, backgroundPaint)
                path.reset()
                for (point in 0..WAVE_POINTS) {
                    val x = panel.left + 9f + point * ((panel.width() - 18f) / WAVE_POINTS)
                    val envelope = 0.28f + 0.72f * sin(point * Math.PI / WAVE_POINTS).toFloat()
                    val phase = frameIndex * (Math.PI * 2 / FRAME_COUNT) + point * 0.72
                    val y = panel.centerY() + (12f * envelope * sin(phase)).toFloat()
                    if (point == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                canvas.drawPath(path, wavePaint)
                frame.getPixels(pixels, 0, WIDTH, 0, 0, WIDTH, HEIGHT)
                for (index in pixels.indices) pixels[index] = pixels[index] and 0x00FFFFFF
                encoder.addImage(pixels, WIDTH, options)
            }
            encoder.finishEncoding()
        } finally {
            frame.recycle()
        }
    }

    private const val FRAME_COUNT = 4
    private const val FRAME_DELAY_MS = 250L
    private const val WAVE_POINTS = 60
}
