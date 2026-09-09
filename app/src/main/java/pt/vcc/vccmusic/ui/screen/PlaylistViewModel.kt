package pt.vcc.vccmusic.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import pt.vcc.vccmusic.data.MusicRepository
import pt.vcc.vccmusic.data.local.PlaylistEntity
import pt.vcc.vccmusic.data.local.TrackEntity

class PlaylistViewModel(
    private val repository: MusicRepository,
) : ViewModel() {
    val playlists: Flow<List<PlaylistEntity>> = repository.observePlaylists()

    /** Observa as faixas ordenadas de uma playlist. */
    fun observeTracks(playlistId: Long): Flow<List<TrackEntity>> =
        repository.observePlaylistTracks(playlistId)

    /** Observa todas as faixas da raiz para seleção na playlist. */
    fun observeAllTracks(rootId: Long): Flow<List<TrackEntity>> =
        repository.observeAllTracks(rootId)

    /** Cria uma playlist quando o nome não está vazio. */
    fun create(name: String) = viewModelScope.launch {
        if (name.isNotBlank()) repository.createPlaylist(name)
    }

    /** Renomeia uma playlist quando o nome não está vazio. */
    fun rename(playlistId: Long, name: String) = viewModelScope.launch {
        if (name.isNotBlank()) repository.renamePlaylist(playlistId, name)
    }

    /** Atualiza o favorito de uma playlist. */
    fun setFavorite(playlistId: Long, isFavorite: Boolean) = viewModelScope.launch {
        repository.setPlaylistFavorite(playlistId, isFavorite)
    }

    /** Remove a playlist indicada. */
    fun delete(playlistId: Long) = viewModelScope.launch {
        repository.deletePlaylist(playlistId)
    }

    /** Substitui as faixas associadas à playlist. */
    fun replaceTracks(playlistId: Long, trackIds: List<Long>) = viewModelScope.launch {
        repository.replacePlaylistTracks(playlistId, trackIds)
    }
}
