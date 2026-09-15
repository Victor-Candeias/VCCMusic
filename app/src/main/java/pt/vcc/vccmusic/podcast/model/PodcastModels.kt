package pt.vcc.vccmusic.podcast.model

data class PodcastFeed(
    val id: Long,
    val title: String,
    val url: String?,
    val originalUrl: String?,
    val link: String?,
    val description: String?,
    val author: String?,
    val image: String?,
    val artwork: String?,
    val episodeCount: Int?,
)

data class PodcastEpisode(
    val id: Long,
    val title: String,
    val description: String?,
    val link: String?,
    val datePublished: Long?,
    val datePublishedPretty: String?,
    val enclosureUrl: String?,
    val enclosureType: String?,
    val enclosureLength: Long?,
    val duration: Long?,
    val image: String?,
    val feedId: Long?,
    val feedTitle: String?,
    val feedImage: String?,
    val feedAuthor: String?,
    val season: Int?,
    val episode: Int?,
)
