package pt.vcc.vccmusic.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class PlaylistTrackSnapshot(
    val playlistId: Long,
    val trackUri: String,
    val position: Int,
)

@Dao
interface MusicRootDao {
    /** Observa a raiz marcada como ativa, refletindo alterações na base de dados. */
    @Query("SELECT * FROM music_roots WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<MusicRootEntity?>

    /** Procura uma raiz pelo URI persistido. */
    @Query("SELECT * FROM music_roots WHERE uri = :uri LIMIT 1")
    suspend fun findByUri(uri: String): MusicRootEntity?

    /** Obtém a raiz atualmente ativa, se existir. */
    @Query("SELECT * FROM music_roots WHERE isActive = 1 LIMIT 1")
    suspend fun findActive(): MusicRootEntity?

    /** Insere ou substitui uma raiz e devolve o seu identificador. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(root: MusicRootEntity): Long

    /** Desativa todas as raízes antes de selecionar uma nova. */
    @Query("UPDATE music_roots SET isActive = 0")
    suspend fun deactivateAll()

    /** Remove uma raiz e as entidades dependentes em cascata. */
    @Query("DELETE FROM music_roots WHERE id = :rootId")
    suspend fun delete(rootId: Long)

    /** Troca atomicamente a raiz ativa, reutilizando o registo quando possível. */
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
    /** Lista todas as pastas pertencentes a uma raiz. */
    @Query("SELECT * FROM music_folders WHERE rootId = :rootId")
    suspend fun findAll(rootId: Long): List<MusicFolderEntity>

    /** Procura uma pasta pelo URI dentro de uma raiz. */
    @Query("SELECT * FROM music_folders WHERE rootId = :rootId AND uri = :uri LIMIT 1")
    suspend fun findByUri(rootId: Long, uri: String): MusicFolderEntity?

    /** Obtém uma pasta pelo identificador. */
    @Query("SELECT * FROM music_folders WHERE id = :folderId LIMIT 1")
    suspend fun findById(folderId: Long): MusicFolderEntity?

    /** Observa as pastas filhas de uma pasta, ordenadas por nome e URI. */
    @Query(
        """
        SELECT * FROM music_folders
        WHERE rootId = :rootId AND parentId IS :parentId
        ORDER BY name COLLATE NOCASE, uri
        """,
    )
    fun observeChildren(rootId: Long, parentId: Long?): Flow<List<MusicFolderEntity>>

    /** Insere ou atualiza várias pastas numa única operação. */
    @Upsert
    suspend fun insertAll(folders: List<MusicFolderEntity>)

    /** Insere ou atualiza uma pasta e devolve o identificador atribuído. */
    @Upsert
    suspend fun insert(folder: MusicFolderEntity): Long

    /** Atualiza a relação de parentesco de uma pasta. */
    @Query("UPDATE music_folders SET parentId = :parentId WHERE id = :folderId")
    suspend fun updateParent(folderId: Long, parentId: Long?)

    /** Remove as pastas indicadas pelos seus identificadores. */
    @Query("DELETE FROM music_folders WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    /** Remove todas as pastas de uma raiz. */
    @Query("DELETE FROM music_folders WHERE rootId = :rootId")
    suspend fun deleteForRoot(rootId: Long)
}

@Dao
interface TrackDao {
    /** Lista todas as faixas de uma raiz para reconstrução ou processamento. */
    @Query("SELECT * FROM tracks WHERE rootId = :rootId")
    suspend fun findAll(rootId: Long): List<TrackEntity>

    /** Observa as faixas diretamente contidas numa pasta. */
    @Query(
        """
        SELECT * FROM tracks
        WHERE folderId = :folderId
        ORDER BY title COLLATE NOCASE, uri
        """,
    )
    fun observeDirectTracks(folderId: Long): Flow<List<TrackEntity>>

    /** Observa as faixas localizadas na pasta correspondente ao URI indicado. */
    @Query(
        """
        SELECT tracks.* FROM tracks
        INNER JOIN music_folders ON music_folders.id = tracks.folderId
        WHERE music_folders.rootId = :rootId AND music_folders.uri = :folderUri
        ORDER BY tracks.title COLLATE NOCASE, tracks.uri
        """,
    )
    fun observeTracksInFolderUri(rootId: Long, folderUri: String): Flow<List<TrackEntity>>

    /** Observa todas as faixas da raiz em ordem estável. */
    @Query(
        """
        SELECT * FROM tracks
        WHERE rootId = :rootId
        ORDER BY title COLLATE NOCASE, uri
        """,
    )
    fun observeAll(rootId: Long): Flow<List<TrackEntity>>

    /** Observa apenas as faixas favoritas da raiz. */
    @Query("SELECT * FROM tracks WHERE rootId = :rootId AND isFavorite = 1 ORDER BY title COLLATE NOCASE, uri")
    fun observeFavorites(rootId: Long): Flow<List<TrackEntity>>

    /** Procura uma faixa pelo identificador. */
    @Query("SELECT * FROM tracks WHERE id = :trackId LIMIT 1")
    suspend fun findById(trackId: Long): TrackEntity?

    /** Procura faixas pelos seus URIs dentro de uma raiz. */
    @Query("SELECT * FROM tracks WHERE rootId = :rootId AND uri IN (:uris)")
    suspend fun findByUris(rootId: Long, uris: List<String>): List<TrackEntity>

    /** Atualiza o estado de favorito de uma faixa. */
    @Query("UPDATE tracks SET isFavorite = :isFavorite WHERE id = :trackId")
    suspend fun setFavorite(trackId: Long, isFavorite: Boolean)

    /** Obtém faixas pelos identificadores, preservando uma ordem determinística. */
    @Query("SELECT * FROM tracks WHERE id IN (:ids) ORDER BY id")
    suspend fun findByIds(ids: List<Long>): List<TrackEntity>

    /** Insere ou atualiza várias faixas. */
    @Upsert
    suspend fun insertAll(tracks: List<TrackEntity>)

    /** Remove as faixas indicadas pelos seus identificadores. */
    @Query("DELETE FROM tracks WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    /** Remove todas as faixas de uma raiz. */
    @Query("DELETE FROM tracks WHERE rootId = :rootId")
    suspend fun deleteForRoot(rootId: Long)
}

@Dao
interface PlaylistDao {
    /** Captura as faixas das playlists por URI para restaurá-las após uma reindexação. */
    @Query(
        """
        SELECT playlist_tracks.playlistId, tracks.uri AS trackUri, playlist_tracks.position
        FROM playlist_tracks
        INNER JOIN tracks ON tracks.id = playlist_tracks.trackId
        WHERE tracks.rootId = :rootId
        ORDER BY playlist_tracks.playlistId, playlist_tracks.position
        """,
    )
    suspend fun snapshotTracksForRoot(rootId: Long): List<PlaylistTrackSnapshot>

    /** Observa todas as playlists ordenadas pelo nome. */
    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE, id")
    fun observeAll(): Flow<List<PlaylistEntity>>

    /** Procura uma playlist pelo identificador. */
    @Query("SELECT * FROM playlists WHERE id = :playlistId LIMIT 1")
    suspend fun findById(playlistId: Long): PlaylistEntity?

    /** Insere uma playlist e devolve o identificador gerado. */
    @Insert
    suspend fun insert(playlist: PlaylistEntity): Long

    /** Atualiza os dados de uma playlist existente. */
    @Update
    suspend fun update(playlist: PlaylistEntity)

    /** Atualiza o estado de favorito de uma playlist. */
    @Query("UPDATE playlists SET isFavorite = :isFavorite WHERE id = :playlistId")
    suspend fun setFavorite(playlistId: Long, isFavorite: Boolean)

    /** Remove uma playlist e as suas associações em cascata. */
    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun delete(playlistId: Long)

    /** Observa as faixas de uma playlist na ordem guardada. */
    @Query(
        """
        SELECT tracks.* FROM tracks
        INNER JOIN playlist_tracks ON playlist_tracks.trackId = tracks.id
        WHERE playlist_tracks.playlistId = :playlistId
        ORDER BY playlist_tracks.position
        """,
    )
    fun observeTracks(playlistId: Long): Flow<List<TrackEntity>>

    /** Obtém os identificadores das faixas de uma playlist na ordem atual. */
    @Query("SELECT trackId FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position")
    suspend fun findTrackIds(playlistId: Long): List<Long>

    /** Insere as associações entre uma playlist e as suas faixas. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(entries: List<PlaylistTrackEntity>)

    /** Remove todas as associações de uma playlist. */
    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun deleteTracks(playlistId: Long)

    /** Substitui atomicamente a lista de faixas e recalcula as posições. */
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
