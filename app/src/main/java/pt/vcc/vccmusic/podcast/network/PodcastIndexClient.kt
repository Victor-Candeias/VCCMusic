package pt.vcc.vccmusic.podcast.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object PodcastIndexClient {
    private const val BASE_URL = "https://api.podcastindex.org/api/1.0/"

    val api: PodcastIndexApi by lazy {
        val httpClient = OkHttpClient.Builder()
            .addInterceptor(PodcastIndexAuthInterceptor())
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PodcastIndexApi::class.java)
    }
}
