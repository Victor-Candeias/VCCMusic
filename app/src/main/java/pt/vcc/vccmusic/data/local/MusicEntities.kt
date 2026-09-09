package pt.vcc.vccmusic.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "music_roots",
    indices = [Index(value = ["uri"], unique = true)],
)
data class MusicRootEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val displayName: String,
    val isActive: Boolean = true,
)

@Entity(
    tableName = "music_folders",
    foreignKeys = [
        ForeignKey(
            entity = MusicRootEntity::class,
            parentColumns = ["id"],
            childColumns = ["rootId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MusicFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["rootId", "uri"], unique = true),
        Index(value = ["rootId", "parentId", "name"]),
        Index(value = ["parentId"]),
    ],
)
data class MusicFolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rootId: Long,
    val parentId: Long?,
    val name: String,
    val uri: String,
)

@Entity(
    tableName = "tracks",
    foreignKeys = [
        ForeignKey(
            entity = MusicRootEntity::class,
            parentColumns = ["id"],
            childColumns = ["rootId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MusicFolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["rootId", "uri"], unique = true),
        Index(value = ["folderId", "title", "uri"]),
        Index(value = ["rootId", "title", "uri"]),
    ],
)
data class TrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rootId: Long,
    val folderId: Long,
    val title: String,
    val artist: String?,
    val album: String?,
    val durationMs: Long?,
    val uri: String,
    val artworkUri: String?,
    val artwork: ByteArray? = null,
    val isFavorite: Boolean = false,
)

@Entity(
    tableName = "playlists",
    indices = [Index(value = ["name"], unique = true)],
)
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isFavorite: Boolean = false,
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["playlistId", "position"], unique = true),
        Index(value = ["trackId"]),
    ],
)
data class PlaylistTrackEntity(
    val playlistId: Long,
    val trackId: Long,
    val position: Int,
)
