package pt.vcc.vccmusic.playback

import org.junit.Assert.assertEquals
import org.junit.Test
import pt.vcc.vccmusic.data.local.TrackEntity

class QueueBuilderTest {
    @Test
    fun sortsAndDeduplicatesTracksByUri() {
        val result = QueueBuilder.build(
            QueueRequest(
                QueueSource.ALL_TRACKS,
                listOf(track(2, "Bravo", "content://2"), track(1, "alpha", "content://1"), track(3, "copy", "content://1")),
            ),
        )

        assertEquals(listOf(1L, 2L), result.map { it.id })
    }

    @Test
    fun selectedTrackStartsTheQueue() {
        val result = QueueBuilder.build(
            QueueRequest(
                QueueSource.FOLDER,
                listOf(track(1, "Alpha", "content://1"), track(2, "Bravo", "content://2")),
                selectedTrackId = 2,
            ),
        )

        assertEquals(listOf(2L, 1L), result.map { it.id })
    }

    @Test
    fun emptyAndInvalidTracksProduceAnEmptyQueue() {
        val result = QueueBuilder.build(
            QueueRequest(QueueSource.SELECTION, listOf(track(1, "Missing", ""))),
        )

        assertEquals(emptyList<TrackEntity>(), result)
    }

    private fun track(id: Long, title: String, uri: String) = TrackEntity(
        id = id,
        rootId = 1,
        folderId = 1,
        title = title,
        artist = null,
        album = null,
        durationMs = null,
        uri = uri,
        artworkUri = null,
    )
}
