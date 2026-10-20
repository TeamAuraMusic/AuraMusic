package com.auramusic.app.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YTPlayerUtilsTest {
    @Test
    fun primaryWebClientResolvesCipheredStreamUrl() {
        assertTrue(
            shouldUseSlowUrlFallback(
                clientName = "WEB_REMIX",
                mainClientName = "WEB_REMIX",
                signatureCipher = "s=encrypted-signature",
            ),
        )
    }

    @Test
    fun primaryWebClientKeepsFastPathWithoutCipher() {
        assertFalse(
            shouldUseSlowUrlFallback(
                clientName = "WEB_REMIX",
                mainClientName = "WEB_REMIX",
                signatureCipher = null,
            ),
        )
    }

    @Test
    fun fallbackClientsKeepSlowUrlResolution() {
        assertTrue(
            shouldUseSlowUrlFallback(
                clientName = "ANDROID",
                mainClientName = "WEB_REMIX",
                signatureCipher = null,
            ),
        )
    }
}
