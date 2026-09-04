package pt.vcc.vccmusic.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pt.vcc.vccmusic.R
import pt.vcc.vccmusic.data.local.MusicFolderEntity
import pt.vcc.vccmusic.data.local.TrackEntity
import pt.vcc.vccmusic.playback.QueueSource

private sealed interface LibraryLocation {
    data object Root : LibraryLocation
    data object AllTracks : LibraryLocation
    data class Folder(val folder: MusicFolderEntity) : LibraryLocation
}

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    playbackViewModel: PlaybackViewModel,
    contentPadding: PaddingValues,
    onPickRoot: () -> Unit,
    onReindex: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val root by viewModel.activeRoot.collectAsStateWithLifecycle(initialValue = null)
    var location by remember { mutableStateOf<LibraryLocation>(LibraryLocation.Root) }
    var folderPath by remember { mutableStateOf<List<MusicFolderEntity>>(emptyList()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .testTag("screen-library"),
    ) {
        if (root == null) {
            EmptyLibrary(onPickRoot)
        } else {
            val activeRoot = root!!
            when (val current = location) {
                LibraryLocation.Root -> RootContents(
                    rootId = activeRoot.id,
                    viewModel = viewModel,
                    onOpenFolder = {
                        folderPath = listOf(it)
                        location = LibraryLocation.Folder(it)
                    },
                    onShowAllTracks = {
                        folderPath = emptyList()
                        location = LibraryLocation.AllTracks
                    },
                    onReindex = onReindex,
                    onPickRoot = onPickRoot,
                    playbackViewModel = playbackViewModel,
                )
                LibraryLocation.AllTracks -> TrackContents(
                    title = stringResource(R.string.all_music),
                    tracks = viewModel.observeAllTracks(activeRoot.id)
                        .collectAsStateWithLifecycle(initialValue = emptyList()).value,
                    onBack = { location = LibraryLocation.Root },
                    playbackViewModel = playbackViewModel,
                )
                is LibraryLocation.Folder -> FolderContents(
                    rootId = activeRoot.id,
                    folder = current.folder,
                    viewModel = viewModel,
                    onOpenFolder = {
                        folderPath = folderPath + it
                        location = LibraryLocation.Folder(it)
                    },
                    onBack = {
                        folderPath = folderPath.dropLast(1)
                        location = folderPath.lastOrNull()?.let(LibraryLocation::Folder)
                            ?: LibraryLocation.Root
                    },
                    playbackViewModel = playbackViewModel,
                )
            }
        }
    }
}

@Composable
private fun EmptyLibrary(onPickRoot: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(stringResource(R.string.library), style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(R.string.no_music_root),
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(onClick = onPickRoot, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(R.string.choose_music_root))
        }
    }
}

@Composable
private fun RootContents(
    rootId: Long,
    viewModel: LibraryViewModel,
    onOpenFolder: (MusicFolderEntity) -> Unit,
    onShowAllTracks: () -> Unit,
    onReindex: () -> Unit,
    onPickRoot: () -> Unit,
    playbackViewModel: PlaybackViewModel,
) {
    val folders = viewModel.observeFolders(rootId, null)
        .collectAsStateWithLifecycle(initialValue = emptyList()).value
    val tracks = viewModel.observeAllTracks(rootId)
        .collectAsStateWithLifecycle(initialValue = emptyList()).value
    LibraryHeader(title = stringResource(R.string.library))
    LibraryShortcuts(onShowAllTracks, onReindex, onPickRoot)
    if (folders.isEmpty() && tracks.isEmpty()) {
        EmptyContent(R.string.library_empty)
    } else {
        LibraryItems(folders, tracks, onOpenFolder, playbackViewModel)
    }
}

@Composable
private fun FolderContents(
    rootId: Long,
    folder: MusicFolderEntity,
    viewModel: LibraryViewModel,
    onOpenFolder: (MusicFolderEntity) -> Unit,
    onBack: () -> Unit,
    playbackViewModel: PlaybackViewModel,
) {
    val folders = viewModel.observeFolders(rootId, folder.id)
        .collectAsStateWithLifecycle(initialValue = emptyList()).value
    val tracks = viewModel.observeFolderTracks(folder.id)
        .collectAsStateWithLifecycle(initialValue = emptyList()).value
    LibraryHeader(folder.name, onBack)
    if (folders.isEmpty() && tracks.isEmpty()) {
        EmptyContent(R.string.folder_empty)
    } else {
        LibraryItems(folders, tracks, onOpenFolder, playbackViewModel)
    }
}

@Composable
private fun TrackContents(
    title: String,
    tracks: List<TrackEntity>,
    onBack: () -> Unit,
    playbackViewModel: PlaybackViewModel,
) {
    LibraryHeader(title, onBack)
    if (tracks.isEmpty()) EmptyContent(R.string.no_tracks) else TrackList(tracks) {
        playbackViewModel.playTracks(tracks, it, QueueSource.ALL_TRACKS)
    }
}

@Composable
private fun LibraryHeader(title: String, onBack: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
            }
        }
        Text(title, style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
private fun LibraryShortcuts(
    onShowAllTracks: () -> Unit,
    onReindex: () -> Unit,
    onPickRoot: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Button(onClick = onShowAllTracks) {
            Icon(Icons.Default.Home, null)
            Text(stringResource(R.string.all_music), modifier = Modifier.padding(start = 8.dp))
        }
        Button(
            onClick = onReindex,
            modifier = Modifier.padding(start = 8.dp),
        ) {
            Text(stringResource(R.string.reindex))
        }
        Button(
            onClick = onPickRoot,
            modifier = Modifier.padding(start = 8.dp),
        ) {
            Text(stringResource(R.string.change_music_root))
        }
    }
}

@Composable
private fun LibraryItems(
    folders: List<MusicFolderEntity>,
    tracks: List<TrackEntity>,
    onOpenFolder: (MusicFolderEntity) -> Unit,
    playbackViewModel: PlaybackViewModel,
) {
    LazyColumn {
        items(folders, key = { it.id }) { folder ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onOpenFolder(folder) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Home, null)
                Text(folder.name, modifier = Modifier.padding(start = 12.dp))
            }
            HorizontalDivider()
        }
        items(tracks, key = { it.id }) { track ->
            Column(modifier = Modifier.fillMaxWidth().clickable { playbackViewModel.playTracks(tracks, track.id) }.padding(16.dp)) {
                Text(track.title, style = MaterialTheme.typography.titleMedium)
                val details = listOfNotNull(track.artist, track.album).joinToString(" - ")
                if (details.isNotBlank()) {
                    Text(details, style = MaterialTheme.typography.bodyMedium)
                }
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun TrackList(tracks: List<TrackEntity>, onPlay: (Long) -> Unit) {
    LazyColumn {
        items(tracks, key = { it.id }) { track ->
            Column(modifier = Modifier.fillMaxWidth().clickable { onPlay(track.id) }.padding(16.dp)) {
                Text(track.title, style = MaterialTheme.typography.titleMedium)
                val details = listOfNotNull(track.artist, track.album).joinToString(" - ")
                if (details.isNotBlank()) Text(details, style = MaterialTheme.typography.bodyMedium)
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun EmptyContent(messageRes: Int) {
    Text(
        stringResource(messageRes),
        modifier = Modifier.padding(24.dp),
        style = MaterialTheme.typography.bodyLarge,
    )
}
