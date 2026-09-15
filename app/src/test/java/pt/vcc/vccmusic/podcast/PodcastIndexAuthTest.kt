package pt.vcc.vccmusic.podcast

import org.junit.Assert.assertEquals
import org.junit.Test
import pt.vcc.vccmusic.podcast.network.PodcastIndexAuth

class PodcastIndexAuthTest {
    @Test
    fun createsLowercaseSha1Digest() {
        assertEquals(
            "a9993e364706816aba3e25717850c26c9cd0d89d",
            PodcastIndexAuth.sha1("abc"),
        )
    }
}
