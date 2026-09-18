package pt.vcc.vccmusic.podcast.model

import com.google.gson.annotations.SerializedName

data class PodcastFeed(
    @field:SerializedName("id")
    val id: Long,
    @field:SerializedName("title")
    val title: String,
    @field:SerializedName("url")
    val url: String?,
    @field:SerializedName("originalUrl")
    val originalUrl: String?,
    @field:SerializedName("link")
    val link: String?,
    @field:SerializedName("description")
    val description: String?,
    @field:SerializedName("author")
    val author: String?,
    @field:SerializedName("image")
    val image: String?,
    @field:SerializedName("artwork")
    val artwork: String?,
    @field:SerializedName("episodeCount")
    val episodeCount: Int?,
    @field:SerializedName("language")
    val language: String? = null,
)

data class PodcastEpisode(
    @field:SerializedName("id")
    val id: Long,
    @field:SerializedName("title")
    val title: String,
    @field:SerializedName("description")
    val description: String?,
    @field:SerializedName("link")
    val link: String?,
    @field:SerializedName("datePublished")
    val datePublished: Long?,
    @field:SerializedName("datePublishedPretty")
    val datePublishedPretty: String?,
    @field:SerializedName("enclosureUrl")
    val enclosureUrl: String?,
    @field:SerializedName("enclosureType")
    val enclosureType: String?,
    @field:SerializedName("enclosureLength")
    val enclosureLength: Long?,
    @field:SerializedName("duration")
    val duration: Long?,
    @field:SerializedName("image")
    val image: String?,
    @field:SerializedName("feedId")
    val feedId: Long?,
    @field:SerializedName("feedTitle")
    val feedTitle: String?,
    @field:SerializedName("feedImage")
    val feedImage: String?,
    @field:SerializedName("feedAuthor")
    val feedAuthor: String?,
    @field:SerializedName("season")
    val season: Int?,
    @field:SerializedName("episode")
    val episode: Int?,
)
