package pt.vcc.vccmusic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicRootDao {
    @Query("SELECT * FROM music_roots WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<MusicRootEntity?>

    @Query("SELECT * FROM music_roots WHERE uri = :uri LIMIT 1")
    suspend fun findByUri(uri: String): MusicRootEntity?

    @Query("SELECT * FROM music_roots WHERE isActive = 1 LIMIT 1")
    suspend fun findActive(): MusicRootEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(root: MusicRootEntity): Long

    @Query("UPDATE music_roots SET isActive = 0")
    suspend fun deactivateAll()

    @Query("DELETE FROM music_roots WHERE id = :rootId")
    suspend fun delete(rootId: Long)

    @Transaction
    suspend fun replaceActive(uri: String, displayName: String): Long {
        deactivateAll()
        val existing = findByUri(uri)
        return if (existing == null) {
            insert(MusicRootEntity(uri = uri, displayName = displayName, isActive = true))
        } else {
            insert(existing.copy(displayName = displayName, isActive = true))
        }
    }
}

@Dao
interface MusicFolderDao {
    @Query("SELECT * FROM music_folders WHERE rootId = :rootId")
    suspend fun findAll(rootId: Long): List<MusicFolderEntity>

    @Query("SELECT * FROM music_folders WHERE rootId = :rootId AND uri = :uri LIMIT 1")
    suspend fun findByUri(rootId: Long, uri: String): MusicFolderEntity?

    @Query("SELECT * FROM music_folders WHERE id = :folderId LIMIT 1")
    suspend fun findById(folderId: Long): MusicFolderEntity?

    @Query(
        """
        SELECT * FROM music_folders
        WHERE rootId = :rootId AND parentId IS :parentId
        ORDER BY name COLLATE NOCASE, uri
        """,
    )
    fun observeChildren(rootId: Long, parentId: Long?): Flow<List<MusicFolderEntity>>

    @Upsert
    suspend fun insertAll(folders: List<MusicFolderEntity>)

    @Upsert
    suspend fun insert(folder: MusicFolderEntity): Long

    @Query("UPDATE music_folders SET parentId = :parentId WHERE id = :folderId")
    suspend fun updateParent(folderId: Long, parentId: Long?)

    @Query("DELETE FROM music_folders WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM music_folders WHERE rootId = :rootId")
    suspend fun deleteForRoot(rootId: Long)
}

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks WHERE rootId = :rootId")
    suspend fun findAll(rootId: Long): List<TrackEntity>

    @Query(
        """
        SELECT * FROM tracks
        WHERE folderId = :folderId
        ORDER BY title COLLATE NOCASE, uri
        """,
    )
    fun observeDirectTracks(folderId: Long): Flow<List<TrackEntity>>

    @Query(
        """
        SELECT * FROM tracks
        WHERE rootId = :rootId
        ORDER BY title COLLATE NOCASE, uri
        """,
    )
    fun observeAll(rootId: Long): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE rootId = :rootId AND isFavorite = 1 ORDER BY title COLLATE NOCASE, uri")
    fun observeFavorites(rootId: Long): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE id = :trackId LIMIT 1")
    suspend fun findById(trackId: Long): TrackEntity?

    @Query("UPDATE tracks SET isFavorite = :isFavorite WHERE id = :trackId")
    suspend fun setFavorite(trackId: Long, isFavorite: Boolean)

    @Query("SELECT * FROM tracks WHERE id IN (:ids) ORDER BY id")
    suspend fun findByIds(ids: List<Long>): List<TrackEntity>

    @Upsert
    suspend fun insertAll(tracks: List<TrackEntity>)

    @Query("DELETE FROM tracks WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM tracks WHERE rootId = :rootId")
    suspend fun deleteForRoot(rootId: Long)
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE, id")
    fun observeAll(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId LIMIT 1")
    suspend fun findById(playlistId: Long): PlaylistEntity?

    @Insert
    suspend fun insert(playlist: PlaylistEntity): Long

    @Update
    suspend fun update(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun delete(playlistId: Long)

    @Query(
        """
        SELECT tracks.* FROM tracks
        INNER JOIN playlist_tracks ON playlist_tracks.trackId = tracks.id
        WHERE playlist_tracks.playlistId = :playlistId
        ORDER BY playlist_tracks.position
        """,
    )
    fun observeTracks(playlistId: Long): Flow<List<TrackEntity>>

    @Query("SELECT trackId FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position")
    suspend fun findTrackIds(playlistId: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(entries: List<PlaylistTrackEntity>)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun deleteTracks(playlistId: Long)

    @Transaction
    suspend fun replaceTracks(playlistId: Long, trackIds: List<Long>) {
        deleteTracks(playlistId)
        insertTracks(
            trackIds.mapIndexed { position, trackId ->
                PlaylistTrackEntity(playlistId = playlistId, trackId = trackId, position = position)
            },
        )
    }
}
