package pt.vcc.vccmusic.podcast.network

import pt.vcc.vccmusic.podcast.model.PodcastEpisode
import pt.vcc.vccmusic.podcast.model.PodcastFeed
import retrofit2.http.GET
import retrofit2.http.Query

data class PodcastSearchResponse(
    val count: Int,
    val feeds: List<PodcastFeed>,
)

data class PodcastEpisodesResponse(
    val count: Int,
    val items: List<PodcastEpisode>,
)

interface PodcastIndexApi {
    @GET("search/byterm")
    suspend fun searchByTerm(@Query("q") query: String): PodcastSearchResponse

    @GET("episodes/byfeedid")
    suspend fun episodesByFeedId(@Query("id") feedId: Long): PodcastEpisodesResponse
}
