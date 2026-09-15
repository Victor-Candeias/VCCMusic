package pt.vcc.vccmusic.podcast.network

import okhttp3.Interceptor
import okhttp3.Response
import pt.vcc.vccmusic.BuildConfig
import java.time.Instant

class PodcastIndexAuthInterceptor(
    private val apiKey: String = BuildConfig.PODCAST_INDEX_API_KEY,
    private val apiSecret: String = BuildConfig.PODCAST_INDEX_API_SECRET,
    private val userAgent: String = "VCCMusic/1.0 Android",
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        require(apiKey.isNotBlank()) { "Podcast Index API key is not configured." }
        require(apiSecret.isNotBlank()) { "Podcast Index API secret is not configured." }

        val timestamp = Instant.now().epochSecond.toString()
        val authorization = PodcastIndexAuth.sha1(apiKey + apiSecret + timestamp)
        val request = chain.request().newBuilder()
            .header("User-Agent", userAgent)
            .header("X-Auth-Key", apiKey)
            .header("X-Auth-Date", timestamp)
            .header("Authorization", authorization)
            .build()
        return chain.proceed(request)
    }
}
