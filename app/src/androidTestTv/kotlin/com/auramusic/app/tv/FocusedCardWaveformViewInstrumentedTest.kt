package com.auramusic.app.tv

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FocusedCardWaveformViewInstrumentedTest {
    @Test
    fun focusedCardKeepsArtworkAndDrawsAnimatedWaveformWhileFocused() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val artwork = Bitmap.createBitmap(320, 180, Bitmap.Config.ARGB_8888).apply {
            eraseColor(ARTWORK_COLOR)
        }
        lateinit var view: FocusedCardWaveformView
        instrumentation.runOnMainSync {
            view = FocusedCardWaveformView(instrumentation.targetContext)
            view.measure(
                View.MeasureSpec.makeMeasureSpec(CARD_WIDTH, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(CARD_HEIGHT, View.MeasureSpec.EXACTLY),
            )
            view.layout(0, 0, CARD_WIDTH, CARD_HEIGHT)
            view.setArtwork(artwork)
            view.setPreviewFocused(true)
        }

        val firstFrame = render(instrumentation, view)
        Thread.sleep(240)
        val secondFrame = render(instrumentation, view)
        val firstPinkPixels = pinkPixels(firstFrame)
        val secondPinkPixels = pinkPixels(secondFrame)

        assertEquals(ARTWORK_COLOR, firstFrame.getPixel(CARD_WIDTH / 2, 40))
        assertTrue("Focused card must contain the animated pink waveform", firstPinkPixels > 100)
        assertTrue(
            "Waveform pixels must change between frames",
            changedPixels(firstFrame, secondFrame) > 20,
        )

        lateinit var inactiveFrame: Bitmap
        instrumentation.runOnMainSync {
            view.setPreviewFocused(false)
            inactiveFrame = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(inactiveFrame))
        }
        assertEquals(View.INVISIBLE, view.visibility)
        assertEquals(0, pinkPixels(inactiveFrame))
        assertEquals(ARTWORK_COLOR, inactiveFrame.getPixel(CARD_WIDTH / 2, 40))

        artwork.recycle()
        firstFrame.recycle()
        secondFrame.recycle()
        inactiveFrame.recycle()
    }

    private fun render(instrumentation: android.app.Instrumentation, view: View): Bitmap {
        lateinit var frame: Bitmap
        instrumentation.runOnMainSync {
            frame = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(frame))
        }
        return frame
    }

    private fun pinkPixels(bitmap: Bitmap): Int {
        var count = 0
        for (y in (CARD_HEIGHT * 0.60f).toInt() until CARD_HEIGHT) {
            for (x in 0 until CARD_WIDTH) {
                val pixel = bitmap.getPixel(x, y)
                if (Color.red(pixel) > 210 && Color.green(pixel) < 130 && Color.blue(pixel) > 120) count++
            }
        }
        return count
    }

    private fun changedPixels(first: Bitmap, second: Bitmap): Int {
        var changed = 0
        for (y in 0 until CARD_HEIGHT) {
            for (x in 0 until CARD_WIDTH) {
                if (first.getPixel(x, y) != second.getPixel(x, y)) changed++
            }
        }
        return changed
    }

    private companion object {
        const val CARD_WIDTH = 640
        const val CARD_HEIGHT = 360
        val ARTWORK_COLOR = Color.rgb(30, 80, 180)
    }
}
