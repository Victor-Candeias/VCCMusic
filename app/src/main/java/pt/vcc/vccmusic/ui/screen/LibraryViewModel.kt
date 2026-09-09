package pt.vcc.vccmusic.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import pt.vcc.vccmusic.data.MusicRepository
import pt.vcc.vccmusic.data.local.MusicFolderEntity
import pt.vcc.vccmusic.data.local.MusicRootEntity
import pt.vcc.vccmusic.data.local.TrackEntity

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModel(
    private val repository: MusicRepository,
) : ViewModel() {
    val activeRoot: Flow<MusicRootEntity?> = repository.observeActiveRoot()
    val favoriteTracks: Flow<List<TrackEntity>> = activeRoot.flatMapLatest { root ->
        root?.id?.let(repository::observeFavoriteTracks) ?: flowOf(emptyList())
    }

    /** Observa as pastas filhas da localização indicada. */
    fun observeFolders(rootId: Long, parentId: Long?): Flow<List<MusicFolderEntity>> =
        repository.observeFolders(rootId, parentId)

    /** Observa as faixas diretamente contidas numa pasta. */
    fun observeFolderTracks(folderId: Long): Flow<List<TrackEntity>> =
        repository.observeDirectTracks(folderId)

    /** Observa faixas associadas ao URI da raiz ativa. */
    fun observeRootTracks(rootId: Long, rootUri: String): Flow<List<TrackEntity>> =
        repository.observeRootTracks(rootId, rootUri)

    /** Observa todas as faixas da raiz para listagem ou reprodução. */
    fun observeAllTracks(rootId: Long): Flow<List<TrackEntity>> =
        repository.observeAllTracks(rootId)

    val playlists = repository.observePlaylists()

    /** Atualiza o favorito de uma faixa no escopo do ViewModel. */
    fun setFavorite(trackId: Long, isFavorite: Boolean) = viewModelScope.launch {
        repository.setTrackFavorite(trackId, isFavorite)
    }

    /** Acrescenta uma faixa à playlist selecionada. */
    fun addToPlaylist(playlistId: Long, trackId: Long) = viewModelScope.launch {
        repository.addTrackToPlaylist(playlistId, trackId)
    }

    /** Cria uma playlist com nome válido e adiciona-lhe a faixa. */
    fun createPlaylistAndAdd(name: String, trackId: Long) = viewModelScope.launch {
        if (name.isNotBlank()) {
            val playlistId = repository.createPlaylist(name)
            repository.addTrackToPlaylist(playlistId, trackId)
        }
    }
}
