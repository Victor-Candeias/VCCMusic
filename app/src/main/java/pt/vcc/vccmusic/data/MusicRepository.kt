package pt.vcc.vccmusic.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import pt.vcc.vccmusic.data.local.MusicDatabase
import pt.vcc.vccmusic.data.local.MusicFolderEntity
import pt.vcc.vccmusic.data.local.MusicRootEntity
import pt.vcc.vccmusic.data.local.TrackEntity

interface MusicRepository {
    fun observeActiveRoot(): Flow<MusicRootEntity?>
    fun observeFolders(rootId: Long, parentId: Long?): Flow<List<MusicFolderEntity>>
    fun observeDirectTracks(folderId: Long): Flow<List<TrackEntity>>
    fun observeAllTracks(rootId: Long): Flow<List<TrackEntity>>
    suspend fun replaceRoot(rootUri: String, displayName: String)
    suspend fun clearRoot(rootId: Long)
}

class RoomMusicRepository(
    private val database: MusicDatabase,
) : MusicRepository {
    override fun observeActiveRoot(): Flow<MusicRootEntity?> = database.musicRootDao().observeActive()

    override fun observeFolders(rootId: Long, parentId: Long?): Flow<List<MusicFolderEntity>> =
        database.musicFolderDao().observeChildren(rootId, parentId)

    override fun observeDirectTracks(folderId: Long): Flow<List<TrackEntity>> =
        database.trackDao().observeDirectTracks(folderId)

    override fun observeAllTracks(rootId: Long): Flow<List<TrackEntity>> =
        database.trackDao().observeAll(rootId)

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
