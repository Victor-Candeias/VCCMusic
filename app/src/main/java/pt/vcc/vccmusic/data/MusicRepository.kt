package pt.vcc.vccmusic.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import pt.vcc.vccmusic.data.local.MusicDatabase
import pt.vcc.vccmusic.data.local.MusicFolderEntity
import pt.vcc.vccmusic.data.local.MusicRootEntity
import pt.vcc.vccmusic.data.local.PlaylistEntity
import pt.vcc.vccmusic.data.local.TrackEntity

interface MusicRepository {
    fun observeActiveRoot(): Flow<MusicRootEntity?>
    suspend fun activeRoot(): MusicRootEntity?
    fun observeFolders(rootId: Long, parentId: Long?): Flow<List<MusicFolderEntity>>
    fun observeDirectTracks(folderId: Long): Flow<List<TrackEntity>>
    fun observeAllTracks(rootId: Long): Flow<List<TrackEntity>>
    fun observePlaylists(): Flow<List<PlaylistEntity>>
    fun observePlaylistTracks(playlistId: Long): Flow<List<TrackEntity>>
    suspend fun createPlaylist(name: String): Long
    suspend fun renamePlaylist(playlistId: Long, name: String)
    suspend fun deletePlaylist(playlistId: Long)
    suspend fun replacePlaylistTracks(playlistId: Long, trackIds: List<Long>)
    suspend fun replaceRoot(rootUri: String, displayName: String)
    suspend fun clearRoot(rootId: Long)
}

class RoomMusicRepository(
    private val database: MusicDatabase,
) : MusicRepository {
    override fun observeActiveRoot(): Flow<MusicRootEntity?> = database.musicRootDao().observeActive()

    override suspend fun activeRoot(): MusicRootEntity? = database.musicRootDao().findActive()

    override fun observeFolders(rootId: Long, parentId: Long?): Flow<List<MusicFolderEntity>> =
        database.musicFolderDao().observeChildren(rootId, parentId)

    override fun observeDirectTracks(folderId: Long): Flow<List<TrackEntity>> =
        database.trackDao().observeDirectTracks(folderId)

    override fun observeAllTracks(rootId: Long): Flow<List<TrackEntity>> =
        database.trackDao().observeAll(rootId)

    override fun observePlaylists(): Flow<List<PlaylistEntity>> =
        database.playlistDao().observeAll()

    override fun observePlaylistTracks(playlistId: Long): Flow<List<TrackEntity>> =
        database.playlistDao().observeTracks(playlistId)

    override suspend fun createPlaylist(name: String): Long =
        database.playlistDao().insert(PlaylistEntity(name = name.trim()))

    override suspend fun renamePlaylist(playlistId: Long, name: String) {
        database.playlistDao().findById(playlistId)?.let {
            database.playlistDao().update(it.copy(name = name.trim()))
        }
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        database.playlistDao().delete(playlistId)
    }

    override suspend fun replacePlaylistTracks(playlistId: Long, trackIds: List<Long>) {
        database.playlistDao().replaceTracks(playlistId, trackIds.distinct())
    }

    override suspend fun replaceRoot(rootUri: String, displayName: String) {
        database.withTransaction {
            val rootId = database.musicRootDao().replaceActive(rootUri, displayName)
            database.musicFolderDao().deleteForRoot(rootId)
            database.trackDao().deleteForRoot(rootId)
        }
    }

    override suspend fun clearRoot(rootId: Long) {
        database.musicRootDao().delete(rootId)
    }
}
