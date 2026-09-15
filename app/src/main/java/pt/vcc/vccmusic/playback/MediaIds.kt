package pt.vcc.vccmusic.playback

/** Centraliza os identificadores usados pelos itens de faixa da sessão Media3. */
object MediaIds {
    const val TRACK_PREFIX = "track:"
    const val PODCAST_EPISODE_PREFIX = "podcast:episode:"
    const val PODCAST_FEED_PREFIX = "podcast:feed:"

    fun track(id: Long): String = "$TRACK_PREFIX$id"

    fun podcastEpisode(id: Long): String = "$PODCAST_EPISODE_PREFIX$id"

    fun podcastFeed(id: Long): String = "$PODCAST_FEED_PREFIX$id"
}
