package pt.vcc.vccmusic.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaIdsTest {
    @Test
    fun formatsTrackIdsConsistently() {
        assertEquals("track:1554", MediaIds.track(1554))
    }
}
