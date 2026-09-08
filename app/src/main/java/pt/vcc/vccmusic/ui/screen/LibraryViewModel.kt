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

    fun observeFolders(rootId: Long, parentId: Long?): Flow<List<MusicFolderEntity>> =
        repository.observeFolders(rootId, parentId)

    fun observeFolderTracks(folderId: Long): Flow<List<TrackEntity>> =
        repository.observeDirectTracks(folderId)

    fun observeRootTracks(rootId: Long, rootUri: String): Flow<List<TrackEntity>> =
        repository.observeRootTracks(rootId, rootUri)

    fun observeAllTracks(rootId: Long): Flow<List<TrackEntity>> =
        repository.observeAllTracks(rootId)

    val playlists = repository.observePlaylists()

    fun setFavorite(trackId: Long, isFavorite: Boolean) = viewModelScope.launch {
        repository.setTrackFavorite(trackId, isFavorite)
    }

    fun addToPlaylist(playlistId: Long, trackId: Long) = viewModelScope.launch {
        repository.addTrackToPlaylist(playlistId, trackId)
    }

    fun createPlaylistAndAdd(name: String, trackId: Long) = viewModelScope.launch {
        if (name.isNotBlank()) {
            val playlistId = repository.createPlaylist(name)
            repository.addTrackToPlaylist(playlistId, trackId)
        }
    }
}
