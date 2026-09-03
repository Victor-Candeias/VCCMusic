package pt.vcc.vccmusic.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TrackIdTest {
    @Test
    fun preservesNonBlankValue() {
        assertEquals("content://music/track/1", TrackId("content://music/track/1").value)
    }

    @Test
    fun rejectsBlankValue() {
        assertThrows(IllegalArgumentException::class.java) {
            TrackId(" ")
        }
    }
}

