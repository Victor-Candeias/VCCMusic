package pt.vcc.vccmusic.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import pt.vcc.vccmusic.data.local.MusicDatabase
import pt.vcc.vccmusic.data.local.MusicFolderEntity
import pt.vcc.vccmusic.data.local.MusicRootEntity
import pt.vcc.vccmusic.data.local.PlaylistEntity
import pt.vcc.vccmusic.data.local.TrackEntity

interface MusicRepository {
    /** Observa a raiz de música atualmente ativa. */
    fun observeActiveRoot(): Flow<MusicRootEntity?>
    /** Obtém a raiz de música atualmente ativa. */
    suspend fun activeRoot(): MusicRootEntity?
    /** Observa as pastas filhas de uma pasta na raiz indicada. */
    fun observeFolders(rootId: Long, parentId: Long?): Flow<List<MusicFolderEntity>>
    /** Procura uma pasta pelo identificador. */
    suspend fun folder(folderId: Long): MusicFolderEntity?
    /** Observa as faixas diretamente contidas numa pasta. */
    fun observeDirectTracks(folderId: Long): Flow<List<TrackEntity>>
    /** Observa as faixas associadas ao URI da raiz. */
    fun observeRootTracks(rootId: Long, rootUri: String): Flow<List<TrackEntity>>
    /** Procura uma faixa pelo identificador. */
    suspend fun track(trackId: Long): TrackEntity?
    /** Atualiza o favorito de uma faixa. */
    suspend fun setTrackFavorite(trackId: Long, isFavorite: Boolean)
    /** Observa todas as faixas de uma raiz. */
    fun observeAllTracks(rootId: Long): Flow<List<TrackEntity>>
    /** Observa as faixas favoritas de uma raiz. */
    fun observeFavoriteTracks(rootId: Long): Flow<List<TrackEntity>>
    /** Observa as playlists existentes. */
    fun observePlaylists(): Flow<List<PlaylistEntity>>
    /** Observa as faixas de uma playlist. */
    fun observePlaylistTracks(playlistId: Long): Flow<List<TrackEntity>>
    /** Cria uma playlist e devolve o seu identificador. */
    suspend fun createPlaylist(name: String): Long
    /** Renomeia uma playlist, removendo espaços nas extremidades. */
    suspend fun renamePlaylist(playlistId: Long, name: String)
    /** Atualiza o favorito de uma playlist. */
    suspend fun setPlaylistFavorite(playlistId: Long, isFavorite: Boolean)
    /** Remove uma playlist. */
    suspend fun deletePlaylist(playlistId: Long)
    /** Substitui as faixas de uma playlist, eliminando duplicados. */
    suspend fun replacePlaylistTracks(playlistId: Long, trackIds: List<Long>)
    /** Acrescenta uma faixa ao fim da playlist. */
    suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long)
    /** Define a raiz ativa e limpa os dados indexados dessa raiz. */
    suspend fun replaceRoot(rootUri: String, displayName: String)
    /** Remove a raiz e os dados relacionados. */
    suspend fun clearRoot(rootId: Long)
}

class RoomMusicRepository(
    private val database: MusicDatabase,
) : MusicRepository {
    /** Delega a observação da raiz ativa ao DAO. */
    override fun observeActiveRoot(): Flow<MusicRootEntity?> = database.musicRootDao().observeActive()

    /** Delega a consulta da raiz ativa ao DAO. */
    override suspend fun activeRoot(): MusicRootEntity? = database.musicRootDao().findActive()

    /** Observa as pastas filhas através do DAO. */
    override fun observeFolders(rootId: Long, parentId: Long?): Flow<List<MusicFolderEntity>> =
        database.musicFolderDao().observeChildren(rootId, parentId)

    /** Procura uma pasta através do DAO. */
    override suspend fun folder(folderId: Long): MusicFolderEntity? =
        database.musicFolderDao().findById(folderId)

    /** Observa as faixas diretas de uma pasta. */
    override fun observeDirectTracks(folderId: Long): Flow<List<TrackEntity>> =
        database.trackDao().observeDirectTracks(folderId)

    /** Observa as faixas ligadas ao URI da raiz. */
    override fun observeRootTracks(rootId: Long, rootUri: String): Flow<List<TrackEntity>> =
        database.trackDao().observeTracksInFolderUri(rootId, rootUri)

    /** Procura uma faixa através do DAO. */
    override suspend fun track(trackId: Long): TrackEntity? =
        database.trackDao().findById(trackId)

    /** Persiste o novo estado de favorito da faixa. */
    override suspend fun setTrackFavorite(trackId: Long, isFavorite: Boolean) {
        database.trackDao().setFavorite(trackId, isFavorite)
    }

    /** Observa todas as faixas indexadas da raiz. */
    override fun observeAllTracks(rootId: Long): Flow<List<TrackEntity>> =
        database.trackDao().observeAll(rootId)

    /** Observa as faixas favoritas indexadas da raiz. */
    override fun observeFavoriteTracks(rootId: Long): Flow<List<TrackEntity>> =
        database.trackDao().observeFavorites(rootId)

    /** Observa todas as playlists. */
    override fun observePlaylists(): Flow<List<PlaylistEntity>> =
        database.playlistDao().observeAll()

    /** Observa as faixas ordenadas de uma playlist. */
    override fun observePlaylistTracks(playlistId: Long): Flow<List<TrackEntity>> =
        database.playlistDao().observeTracks(playlistId)

    /** Cria uma playlist com o nome normalizado. */
    override suspend fun createPlaylist(name: String): Long =
        database.playlistDao().insert(PlaylistEntity(name = name.trim()))

    /** Atualiza o nome de uma playlist existente. */
    override suspend fun renamePlaylist(playlistId: Long, name: String) {
        database.playlistDao().findById(playlistId)?.let {
            database.playlistDao().update(it.copy(name = name.trim()))
        }
    }

    /** Persiste o estado de favorito da playlist. */
    override suspend fun setPlaylistFavorite(playlistId: Long, isFavorite: Boolean) {
        database.playlistDao().setFavorite(playlistId, isFavorite)
    }

    /** Remove a playlist indicada. */
    override suspend fun deletePlaylist(playlistId: Long) {
        database.playlistDao().delete(playlistId)
    }

    /** Substitui a seleção da playlist sem repetir faixas. */
    override suspend fun replacePlaylistTracks(playlistId: Long, trackIds: List<Long>) {
        database.playlistDao().replaceTracks(playlistId, trackIds.distinct())
    }

    /** Adiciona uma faixa ao fim da seleção atual da playlist. */
    override suspend fun addTrackToPlaylist(playlistId: Long, trackId: Long) {
        val currentTrackIds = database.playlistDao().findTrackIds(playlistId)
        database.playlistDao().replaceTracks(playlistId, currentTrackIds + trackId)
    }

    /** Troca a raiz ativa e elimina o índice antigo dentro de uma transação. */
    override suspend fun replaceRoot(rootUri: String, displayName: String) {
        database.withTransaction {
            val rootId = database.musicRootDao().replaceActive(rootUri, displayName)
            database.musicFolderDao().deleteForRoot(rootId)
            database.trackDao().deleteForRoot(rootId)
        }
    }

    /** Remove a raiz ativa e os seus dados dependentes. */
    override suspend fun clearRoot(rootId: Long) {
        database.musicRootDao().delete(rootId)
    }
}
