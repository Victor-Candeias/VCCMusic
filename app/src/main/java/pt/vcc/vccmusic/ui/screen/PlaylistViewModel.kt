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

    fun observeTracks(playlistId: Long): Flow<List<TrackEntity>> =
        repository.observePlaylistTracks(playlistId)

    fun observeAllTracks(rootId: Long): Flow<List<TrackEntity>> =
        repository.observeAllTracks(rootId)

    fun create(name: String) = viewModelScope.launch {
        if (name.isNotBlank()) repository.createPlaylist(name)
    }

    fun rename(playlistId: Long, name: String) = viewModelScope.launch {
        if (name.isNotBlank()) repository.renamePlaylist(playlistId, name)
    }

    fun delete(playlistId: Long) = viewModelScope.launch {
        repository.deletePlaylist(playlistId)
    }

    fun replaceTracks(playlistId: Long, trackIds: List<Long>) = viewModelScope.launch {
        repository.replacePlaylistTracks(playlistId, trackIds)
    }
}
