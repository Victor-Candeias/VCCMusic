package pt.vcc.vccmusic.podcast.repository

import android.content.Context
import android.os.SystemClock
import com.google.gson.JsonParseException
import pt.vcc.vccmusic.diagnostics.DiagnosticLogger
import pt.vcc.vccmusic.podcast.model.PodcastEpisode
import pt.vcc.vccmusic.podcast.model.PodcastFeed
import pt.vcc.vccmusic.podcast.network.PodcastIndexApi
import retrofit2.HttpException

class PodcastRepository(
    private val api: PodcastIndexApi,
    context: Context,
) {
    private val applicationContext = context.applicationContext

    suspend fun search(query: String): List<PodcastFeed> {
        require(query.isNotBlank()) { "Podcast search query cannot be blank." }
        val normalizedQuery = query.trim()
        val startedAt = SystemClock.elapsedRealtime()
        DiagnosticLogger.log(
            applicationContext,
            "PodcastNetwork",
            "Pedido de pesquisa iniciado: termo=${normalizedQuery.take(80)}",
        )
        val httpResponse = api.searchByTerm(normalizedQuery)
        if (!httpResponse.isSuccessful) {
            throw HttpException(httpResponse)
        }
        val body = httpResponse.body()?.string()
            ?: throw JsonParseException("Podcast Index devolveu um corpo vazio.")
        val response = PodcastIndexResponseParser.parseSearch(body)
        DiagnosticLogger.log(
            applicationContext,
            "PodcastNetwork",
            "Pedido de pesquisa concluído: HTTP=${httpResponse.code()}, resultados=${response.feeds.size}, duraçãoMs=${SystemClock.elapsedRealtime() - startedAt}",
        )
        return response.feeds
    }

    suspend fun episodes(feedId: Long): List<PodcastEpisode> {
        val startedAt = SystemClock.elapsedRealtime()
        DiagnosticLogger.log(
            applicationContext,
            "PodcastNetwork",
            "Pedido de episódios iniciado: feedId=$feedId",
        )
        val httpResponse = api.episodesByFeedId(feedId)
        if (!httpResponse.isSuccessful) {
            throw HttpException(httpResponse)
        }
        val body = httpResponse.body()?.string()
            ?: throw JsonParseException("Podcast Index devolveu um corpo vazio.")
        val episodes = PodcastIndexResponseParser.parseEpisodes(body)
        DiagnosticLogger.log(
            applicationContext,
            "PodcastNetwork",
            "Pedido de episódios concluído: HTTP=${httpResponse.code()}, feedId=$feedId, resultados=${episodes.size}, duraçãoMs=${SystemClock.elapsedRealtime() - startedAt}",
        )
        return episodes
    }
}
