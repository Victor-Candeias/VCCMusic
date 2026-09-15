package pt.vcc.vccmusic.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "podcast_favorites")
data class PodcastFavoriteEntity(
    @PrimaryKey val feedId: Long,
    val title: String,
    val author: String?,
    val description: String?,
    val image: String?,
)

@Entity(tableName = "podcast_progress")
data class PodcastProgressEntity(
    @PrimaryKey val episodeId: Long,
    val feedId: Long?,
    val title: String,
    val positionMs: Long,
    val durationMs: Long?,
    val updatedAt: Long,
)
