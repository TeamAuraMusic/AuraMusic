package com.auramusic.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaylistLinksTest {
    private val pl = "PLx0sYbCqOb8TBPRdmBHs5Iftvv9TPboYG"

    @Test
    fun findsPlaylistInShareLinks() {
        assertEquals(pl, PlaylistLinks.playlistId("https://music.youtube.com/playlist?list=$pl&si=abc123"))
        assertEquals(pl, PlaylistLinks.playlistId("  Check this out https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=$pl "))
        assertEquals(pl, PlaylistLinks.playlistId("https://youtu.be/dQw4w9WgXcQ?list=$pl"))
        assertEquals(pl, PlaylistLinks.playlistId("https://music.youtube.com/browse/VL$pl"))
        assertEquals("RDCLAK5uy_kmPRjHDECIcuVwnKsx2Ng7fyNgFKWNJFs", PlaylistLinks.playlistId("https://music.youtube.com/playlist?list=RDCLAK5uy_kmPRjHDECIcuVwnKsx2Ng7fyNgFKWNJFs"))
    }

    @Test
    fun acceptsRawPlaylistIds() {
        assertEquals(pl, PlaylistLinks.playlistId(pl))
        assertEquals(pl, PlaylistLinks.playlistId("VL$pl"))
    }

    @Test
    fun ignoresSearchTextMixesAndOtherSites() {
        assertNull(PlaylistLinks.playlistId("playlist chill vibes"))
        assertNull(PlaylistLinks.playlistId("Playlist"))
        assertNull(PlaylistLinks.playlistId("https://music.youtube.com/watch?v=dQw4w9WgXcQ&list=RDAMVMdQw4w9WgXcQ"))
        assertNull(PlaylistLinks.playlistId("https://music.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertNull(PlaylistLinks.playlistId("https://evil.example.com/playlist?list=$pl"))
        assertNull(PlaylistLinks.playlistId("https://notyoutube.com/playlist?list=$pl"))
    }
}
