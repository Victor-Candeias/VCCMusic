package pt.vcc.vccmusic.podcast.repository

import kotlinx.coroutines.flow.Flow
import pt.vcc.vccmusic.data.local.PodcastDao
import pt.vcc.vccmusic.data.local.PodcastFavoriteEntity
import pt.vcc.vccmusic.data.local.PodcastProgressEntity
import pt.vcc.vccmusic.podcast.model.PodcastFeed

class PodcastStore(
    private val dao: PodcastDao,
) {
    fun observeFavorites(): Flow<List<PodcastFavoriteEntity>> = dao.observeFavorites()

    suspend fun setFavorite(feed: PodcastFeed, isFavorite: Boolean) {
        if (isFavorite) {
            dao.saveFavorite(
                PodcastFavoriteEntity(
                    feedId = feed.id,
                    title = feed.title,
                    author = feed.author,
                    description = feed.description,
                    image = feed.image ?: feed.artwork,
                ),
            )
        } else {
            dao.deleteFavorite(feed.id)
        }
    }

    suspend fun saveProgress(
        episodeId: Long,
        feedId: Long?,
        title: String,
        positionMs: Long,
        durationMs: Long?,
    ) {
        dao.saveProgress(
            PodcastProgressEntity(
                episodeId = episodeId,
                feedId = feedId,
                title = title,
                positionMs = positionMs,
                durationMs = durationMs,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun progress(episodeId: Long): PodcastProgressEntity? = dao.findProgress(episodeId)
}
