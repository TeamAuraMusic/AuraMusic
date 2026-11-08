package com.auramusic.app.tv

import android.content.ComponentName
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import com.auramusic.app.TvMainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TvRecommendationClickIntentInstrumentedTest {
    @Test
    fun publishedProgramClickTargetsAuraMusicAndPreservesMediaId() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mediaId = "deep-link-song-id"

        val intent = tvRecommendationClickIntent(context, mediaId)

        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(ComponentName(context, TvMainActivity::class.java), intent.component)
        assertEquals(mediaId, tvRecommendationMediaId(intent))
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TOP != 0)
        assertNull(tvRecommendationMediaId(Intent(Intent.ACTION_VIEW)))
    }
}
