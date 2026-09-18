package pt.vcc.vccmusic.podcast.repository

import android.content.Context
import android.os.SystemClock
import com.google.gson.JsonParseException
import pt.vcc.vccmusic.diagnostics.DiagnosticLogger
import pt.vcc.vccmusic.podcast.model.PodcastEpisode
import pt.vcc.vccmusic.podcast.model.PodcastFeed
import pt.vcc.vccmusic.podcast.network.PodcastIndexApi
import retrofit2.HttpException
import okhttp3.ResponseBody
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import java.util.concurrent.ConcurrentHashMap

class PodcastRepository(
    private val api: PodcastIndexApi,
    context: Context,
) {
    private val applicationContext = context.applicationContext
    private data class CacheEntry<T>(val value: T, val expiresAt: Long)
    private val searchCache = ConcurrentHashMap<String, CacheEntry<List<PodcastFeed>>>()
    private val trendingCache = ConcurrentHashMap<String, CacheEntry<List<PodcastFeed>>>()
    private val recentCache = ConcurrentHashMap<String, CacheEntry<List<PodcastFeed>>>()
    private val episodesCache = ConcurrentHashMap<Long, CacheEntry<List<PodcastEpisode>>>()

    suspend fun search(query: String): List<PodcastFeed> {
        require(query.isNotBlank()) { "Podcast search query cannot be blank." }
        val normalizedQuery = query.trim()
        val cacheKey = normalizedQuery.lowercase()
        searchCache[cacheKey]?.takeIf { it.expiresAt > System.currentTimeMillis() }?.let {
            DiagnosticLogger.log(applicationContext, "PodcastNetwork", "Pesquisa servida da cache: termo=${normalizedQuery.take(80)}")
            return it.value
        }
        val startedAt = SystemClock.elapsedRealtime()
        DiagnosticLogger.log(applicationContext, "PodcastNetwork", "Pesquisa combinada iniciada: termo=${normalizedQuery.take(80)}")
        val results = supervisorScope {
            listOf(
                async { fetchSearch("byperson", normalizedQuery) },
                async { fetchSearch("byterm", normalizedQuery) },
                async { fetchSearch("bytitle", normalizedQuery) },
            ).awaitAll()
        }
        val successful = results.filter { it.error == null }
        if (successful.isEmpty()) {
            throw results.firstNotNullOf { it.error }
        }
        val feeds = rankAndDeduplicate(normalizedQuery, successful.flatMap { it.feeds })
        searchCache[cacheKey] = CacheEntry(feeds, System.currentTimeMillis() + SEARCH_CACHE_MS)
        DiagnosticLogger.log(
            applicationContext,
            "PodcastNetwork",
            "Pesquisa combinada concluída: resultados=${feeds.size}, endpointsOK=${successful.size}/3, duraçãoMs=${SystemClock.elapsedRealtime() - startedAt}",
        )
        return feeds
    }

    suspend fun trending(max: Int = 30, language: String? = "pt"): List<PodcastFeed> =
        cachedFeeds(trendingCache, "${max}:${language.orEmpty()}", TRENDING_CACHE_MS) {
            fetchFeeds("trending", api.trending(max.coerceIn(1, 100), language))
        }

    suspend fun recent(max: Int = 30, language: String? = "pt"): List<PodcastFeed> =
        cachedFeeds(recentCache, "${max}:${language.orEmpty()}", TRENDING_CACHE_MS) {
            fetchFeeds("recent", api.recentFeeds(max.coerceIn(1, 100), language))
        }

    suspend fun episodes(feedId: Long): List<PodcastEpisode> {
        episodesCache[feedId]?.takeIf { it.expiresAt > System.currentTimeMillis() }?.let { return it.value }
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
        episodesCache[feedId] = CacheEntry(episodes, System.currentTimeMillis() + EPISODES_CACHE_MS)
        DiagnosticLogger.log(
            applicationContext,
            "PodcastNetwork",
            "Pedido de episódios concluído: HTTP=${httpResponse.code()}, feedId=$feedId, resultados=${episodes.size}, duraçãoMs=${SystemClock.elapsedRealtime() - startedAt}",
        )
        return episodes
    }

    private data class EndpointResult(
        val feeds: List<PodcastFeed>,
        val error: Throwable? = null,
    )

    private suspend fun fetchSearch(endpoint: String, query: String): EndpointResult =
        try {
            val response = when (endpoint) {
                "byperson" -> api.searchByPerson(query)
                "bytitle" -> api.searchByTitle(query)
                else -> api.searchByTerm(query)
            }
            EndpointResult(fetchFeeds(endpoint, response))
        } catch (error: Throwable) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            DiagnosticLogger.log(applicationContext, "PodcastNetwork", "Endpoint de pesquisa falhou: endpoint=$endpoint", error)
            EndpointResult(emptyList(), error)
        }

    private suspend fun fetchFeeds(endpoint: String, response: retrofit2.Response<ResponseBody>): List<PodcastFeed> {
        if (!response.isSuccessful) throw HttpException(response)
        val body = response.body()?.string() ?: throw JsonParseException("Podcast Index devolveu um corpo vazio.")
        val feeds = PodcastIndexResponseParser.parseFeeds(body)
        DiagnosticLogger.log(applicationContext, "PodcastNetwork", "Endpoint concluído: endpoint=$endpoint, HTTP=${response.code()}, resultados=${feeds.size}")
        return feeds
    }

    private suspend fun cachedFeeds(
        cache: ConcurrentHashMap<String, CacheEntry<List<PodcastFeed>>>,
        key: String,
        ttl: Long,
        loader: suspend () -> List<PodcastFeed>,
    ): List<PodcastFeed> {
        cache[key]?.takeIf { it.expiresAt > System.currentTimeMillis() }?.let { return it.value }
        val result = loader()
        cache[key] = CacheEntry(result, System.currentTimeMillis() + ttl)
        return result
    }

    private fun rankAndDeduplicate(query: String, feeds: List<PodcastFeed>): List<PodcastFeed> =
        feeds.groupBy { it.id }.map { (_, duplicates) ->
            duplicates.maxByOrNull { score(query, it) }!!
        }.sortedByDescending { score(query, it) }

    private fun score(query: String, feed: PodcastFeed): Int {
        val normalized = query.lowercase()
        val title = feed.title.lowercase()
        val author = feed.author.orEmpty().lowercase()
        return (if (title == normalized) 100 else 0) +
            (if (title.startsWith(normalized)) 50 else 0) +
            (if (title.contains(normalized)) 25 else 0) +
            (if (author.contains(normalized)) 20 else 0) +
            (if (feed.language?.startsWith("pt", ignoreCase = true) == true) 5 else 0)
    }

    private companion object {
        const val SEARCH_CACHE_MS = 10 * 60 * 1000L
        const val TRENDING_CACHE_MS = 45 * 60 * 1000L
        const val EPISODES_CACHE_MS = 4 * 60 * 60 * 1000L
    }
}
