package pt.vcc.vccmusic.podcast.network

import pt.vcc.vccmusic.podcast.model.PodcastFeed
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

data class PodcastSearchResponse(
    val count: Int,
    val feeds: List<PodcastFeed>,
)

interface PodcastIndexApi {
    @GET("search/byterm")
    suspend fun searchByTerm(@Query("q") query: String): retrofit2.Response<ResponseBody>

    @GET("episodes/byfeedid")
    suspend fun episodesByFeedId(@Query("id") feedId: Long): retrofit2.Response<ResponseBody>
}
