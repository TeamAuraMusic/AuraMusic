package com.auramusic.app.tv

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TvPreviewContentProviderInstrumentedTest {
    @Test
    fun audioOnlyPreviewUriAdvertisesAudioMp4ToProjectivy() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val uri = TvPreviewUri.forAudioOnlyProgram(context, "test-program", "test-revision")

        assertEquals("audio/mp4", TvPreviewContentProvider().getType(uri))
    }

    @Test
    fun generatedPreviewUriAdvertisesVideoMp4ToProjectivy() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val uri = TvPreviewUri.forProgram(context, "test-program", "test-revision")

        assertEquals("video/mp4", TvPreviewContentProvider().getType(uri))
    }
}
