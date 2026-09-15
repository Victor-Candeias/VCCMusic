package pt.vcc.vccmusic.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PodcastDao {
    @Query("SELECT * FROM podcast_favorites ORDER BY title COLLATE NOCASE, feedId")
    fun observeFavorites(): Flow<List<PodcastFavoriteEntity>>

    @Query("SELECT * FROM podcast_favorites WHERE feedId = :feedId LIMIT 1")
    suspend fun findFavorite(feedId: Long): PodcastFavoriteEntity?

    @Upsert
    suspend fun saveFavorite(favorite: PodcastFavoriteEntity)

    @Query("DELETE FROM podcast_favorites WHERE feedId = :feedId")
    suspend fun deleteFavorite(feedId: Long)

    @Query("SELECT * FROM podcast_progress WHERE episodeId = :episodeId LIMIT 1")
    suspend fun findProgress(episodeId: Long): PodcastProgressEntity?

    @Upsert
    suspend fun saveProgress(progress: PodcastProgressEntity)
}
