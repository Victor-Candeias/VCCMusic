package pt.vcc.vccmusic.ui.screen

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import pt.vcc.vccmusic.data.MusicRepository
import pt.vcc.vccmusic.data.local.MusicFolderEntity
import pt.vcc.vccmusic.data.local.MusicRootEntity
import pt.vcc.vccmusic.data.local.TrackEntity

class LibraryViewModel(
    private val repository: MusicRepository,
) : ViewModel() {
    val activeRoot: Flow<MusicRootEntity?> = repository.observeActiveRoot()

    fun observeFolders(rootId: Long, parentId: Long?): Flow<List<MusicFolderEntity>> =
        repository.observeFolders(rootId, parentId)

    fun observeFolderTracks(folderId: Long): Flow<List<TrackEntity>> =
        repository.observeDirectTracks(folderId)

    fun observeAllTracks(rootId: Long): Flow<List<TrackEntity>> =
        repository.observeAllTracks(rootId)
}
