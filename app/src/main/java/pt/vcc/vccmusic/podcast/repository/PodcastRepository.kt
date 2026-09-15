package pt.vcc.vccmusic.podcast.repository

import pt.vcc.vccmusic.podcast.model.PodcastEpisode
import pt.vcc.vccmusic.podcast.model.PodcastFeed
import pt.vcc.vccmusic.podcast.network.PodcastIndexApi

class PodcastRepository(
    private val api: PodcastIndexApi,
) {
    suspend fun search(query: String): List<PodcastFeed> {
        require(query.isNotBlank()) { "Podcast search query cannot be blank." }
        return api.searchByTerm(query.trim()).feeds
    }

    suspend fun episodes(feedId: Long): List<PodcastEpisode> =
        api.episodesByFeedId(feedId).items
}
