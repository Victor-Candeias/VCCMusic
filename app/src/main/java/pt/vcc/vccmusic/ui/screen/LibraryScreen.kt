package pt.vcc.vccmusic.ui.screen

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pt.vcc.vccmusic.R
import pt.vcc.vccmusic.data.local.MusicFolderEntity
import pt.vcc.vccmusic.data.local.PlaylistEntity
import pt.vcc.vccmusic.data.local.TrackEntity
import pt.vcc.vccmusic.playback.QueueSource

private sealed interface LibraryLocation {
    data object Root : LibraryLocation
    data object AllTracks : LibraryLocation
    data class Folder(val folder: MusicFolderEntity) : LibraryLocation
}

enum class LibraryStart { ROOT, ALL_TRACKS }

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    playbackViewModel: PlaybackViewModel,
    contentPadding: PaddingValues,
    onPickRoot: () -> Unit,
    onReindex: () -> Unit,
    start: LibraryStart = LibraryStart.ROOT,
    onTrackPlayed: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val root by viewModel.activeRoot.collectAsStateWithLifecycle(initialValue = null)
    var location by remember {
        mutableStateOf<LibraryLocation>(
            if (start == LibraryStart.ALL_TRACKS) LibraryLocation.AllTracks else LibraryLocation.Root,
        )
    }
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
                    onTrackPlayed = onTrackPlayed,
                )
                LibraryLocation.AllTracks -> TrackContents(
                    title = stringResource(R.string.all_music),
                    tracks = viewModel.observeAllTracks(activeRoot.id)
                        .collectAsStateWithLifecycle(initialValue = emptyList()).value,
                    onBack = { location = LibraryLocation.Root },
                    viewModel = viewModel,
                    playbackViewModel = playbackViewModel,
                    onTrackPlayed = onTrackPlayed,
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
                    onTrackPlayed = onTrackPlayed,
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
    onTrackPlayed: () -> Unit,
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
        LibraryItems(folders, tracks, onOpenFolder, playbackViewModel, onTrackPlayed)
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
    onTrackPlayed: () -> Unit,
) {
    val folders = viewModel.observeFolders(rootId, folder.id)
        .collectAsStateWithLifecycle(initialValue = emptyList()).value
    val tracks = viewModel.observeFolderTracks(folder.id)
        .collectAsStateWithLifecycle(initialValue = emptyList()).value
    LibraryHeader(folder.name, onBack)
    if (folders.isEmpty() && tracks.isEmpty()) {
        EmptyContent(R.string.folder_empty)
    } else {
        LibraryItems(folders, tracks, onOpenFolder, playbackViewModel, onTrackPlayed)
    }
}

@Composable
private fun TrackContents(
    title: String,
    tracks: List<TrackEntity>,
    onBack: () -> Unit,
    viewModel: LibraryViewModel,
    playbackViewModel: PlaybackViewModel,
    onTrackPlayed: () -> Unit,
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle(initialValue = emptyList())
    var trackForPlaylist by remember { mutableStateOf<TrackEntity?>(null) }
    var showCreatePlaylist by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    LibraryHeader(title, onBack)
    if (tracks.isEmpty()) {
        EmptyContent(R.string.no_tracks)
    } else {
        TrackList(
            tracks = tracks,
            onPlay = {
                playbackViewModel.playTracks(tracks, it, QueueSource.ALL_TRACKS)
                onTrackPlayed()
            },
            onToggleFavorite = { track -> viewModel.setFavorite(track.id, !track.isFavorite) },
            onAddToPlaylist = { trackForPlaylist = it },
        )
    }
    if (trackForPlaylist != null && !showCreatePlaylist) {
        val track = trackForPlaylist!!
        PlaylistPickerDialog(
            playlists = playlists,
            onDismiss = { trackForPlaylist = null },
            onCreatePlaylist = {
                newPlaylistName = ""
                showCreatePlaylist = true
            },
            onPlaylistSelected = { playlist ->
                viewModel.addToPlaylist(playlist.id, track.id)
                trackForPlaylist = null
            },
        )
    }
    if (showCreatePlaylist && trackForPlaylist != null) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylist = false },
            title = { Text(stringResource(R.string.create_playlist)) },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.playlist_name)) },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createPlaylistAndAdd(newPlaylistName, trackForPlaylist!!.id)
                        showCreatePlaylist = false
                        trackForPlaylist = null
                    },
                    enabled = newPlaylistName.isNotBlank(),
                ) {
                    Text(stringResource(R.string.create_playlist))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylist = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
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
            Icon(Icons.Default.Home, stringResource(R.string.all_music_icon))
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
    onTrackPlayed: () -> Unit,
) {
    LazyColumn {
        items(folders, key = { "folder-${it.id}" }) { folder ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onOpenFolder(folder) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Home, stringResource(R.string.folder_icon))
                Text(folder.name, modifier = Modifier.padding(start = 12.dp))
            }
            HorizontalDivider()
        }
        items(tracks, key = { "track-${it.id}" }) { track ->
            Column(
                modifier = Modifier.fillMaxWidth().clickable {
                    playbackViewModel.playTracks(tracks, track.id)
                    onTrackPlayed()
                }.padding(16.dp),
            ) {
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
private fun TrackList(
    tracks: List<TrackEntity>,
    onPlay: (Long) -> Unit,
    onToggleFavorite: (TrackEntity) -> Unit,
    onAddToPlaylist: (TrackEntity) -> Unit,
) {
    LazyColumn {
        items(tracks, key = { it.id }) { track ->
            TrackCard(
                track = track,
                onPlay = { onPlay(track.id) },
                onToggleFavorite = { onToggleFavorite(track) },
                onAddToPlaylist = { onAddToPlaylist(track) },
            )
        }
    }
}

@Composable
private fun TrackCard(
    track: TrackEntity,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
) {
    Card(
        onClick = onPlay,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp)
            .testTag("track-${track.id}"),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE96A2C)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val artwork = remember(track.id, track.artwork) {
                track.artwork?.let { bytes ->
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                }
            }
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                if (artwork != null) {
                    Image(
                        bitmap = artwork,
                        contentDescription = track.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text("♫", color = Color.White, style = MaterialTheme.typography.titleLarge)
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(track.title, color = Color.White, style = MaterialTheme.typography.titleMedium)
                val details = listOfNotNull(track.artist, track.album).joinToString(" - ")
                if (details.isNotBlank()) {
                    Text(
                        details,
                        color = Color.White.copy(alpha = 0.82f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = stringResource(
                        if (track.isFavorite) R.string.remove_from_favorites else R.string.add_to_favorites,
                    ),
                    tint = Color.White,
                )
            }
            IconButton(onClick = onAddToPlaylist) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.more_options),
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun PlaylistPickerDialog(
    playlists: List<PlaylistEntity>,
    onDismiss: () -> Unit,
    onCreatePlaylist: () -> Unit,
    onPlaylistSelected: (PlaylistEntity) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_to_playlist)) },
        text = {
            Column {
                if (playlists.isEmpty()) {
                    Text(stringResource(R.string.no_playlists))
                } else {
                    playlists.forEach { playlist ->
                        TextButton(
                            onClick = { onPlaylistSelected(playlist) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(playlist.name, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
                Button(
                    onClick = onCreatePlaylist,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.create_playlist))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun EmptyContent(messageRes: Int) {
    Text(
        stringResource(messageRes),
        modifier = Modifier.padding(24.dp),
        style = MaterialTheme.typography.bodyLarge,
    )
}
