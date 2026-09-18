package pt.vcc.vccmusic.podcast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.vcc.vccmusic.podcast.repository.PodcastIndexResponseParser

class PodcastIndexResponseParserTest {
    @Test
    fun skipsIncompleteFeedsAndKeepsValidResults() {
        val response = PodcastIndexResponseParser.parseSearch(
            """{"count":2,"feeds":[{"id":5751673,"title":"Valid feed","episodeCount":3},{"id":0}]}""",
        )

        assertEquals(1, response.feeds.size)
        assertEquals(5751673L, response.feeds.single().id)
    }

    @Test
    fun keepsEpisodesWithOptionalFieldsMissing() {
        val episodes = PodcastIndexResponseParser.parseEpisodes(
            """{"count":1,"items":[{"id":10,"title":"Episode","enclosureUrl":null}]}""",
        )

        assertEquals(1, episodes.size)
        assertEquals("Episode", episodes.single().title)
        assertNull(episodes.single().enclosureUrl)
    }

    @Test
    fun skipsMalformedEpisodesWithoutFailingTheResponse() {
        val episodes = PodcastIndexResponseParser.parseEpisodes(
            """{"count":2,"items":[{"id":10,"title":"Valid"},{"id":11}]}""",
        )

        assertEquals(1, episodes.size)
        assertTrue(episodes.single().id == 10L)
    }

    @Test
    fun parsesDiscoveryFeedsManuallyIncludingLanguage() {
        val feeds = PodcastIndexResponseParser.parseFeeds(
            """{"feeds":[{"id":42,"title":"Portuguese podcast","language":"pt"}]}""",
        )

        assertEquals(1, feeds.size)
        assertEquals("pt", feeds.single().language)
    }

    @Test
    fun rejectsDiscoveryResponseWithWrongArrayType() {
        val error = runCatching {
            PodcastIndexResponseParser.parseFeeds("""{"feeds":{}}""")
        }.exceptionOrNull()

        assertTrue(error is com.google.gson.JsonParseException)
    }
}
