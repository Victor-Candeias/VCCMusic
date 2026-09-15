package pt.vcc.vccmusic.ui.screen

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import pt.vcc.vccmusic.data.withoutParentheticalText
import pt.vcc.vccmusic.playback.QueueSource
import pt.vcc.vccmusic.ui.theme.appCardBrush
import pt.vcc.vccmusic.ui.theme.FoldersCardEnd
import pt.vcc.vccmusic.ui.theme.FoldersCardStart

private sealed interface LibraryLocation {
    data object Root : LibraryLocation
    data object AllTracks : LibraryLocation
    data class Folder(val folder: MusicFolderEntity) : LibraryLocation
}

enum class LibraryStart { ROOT, ALL_TRACKS }

private enum class TrackSortMode(@androidx.annotation.StringRes val labelRes: Int) {
    NAME(R.string.sort_name),
    ARTIST(R.string.sort_artist),
    ALBUM(R.string.sort_album),
}

@Composable
/** Coordena a navegação entre raiz, pastas e lista completa de faixas. */
fun LibraryScreen(
    viewModel: LibraryViewModel,
    playbackViewModel: PlaybackViewModel,
    contentPadding: PaddingValues,
    onPickRoot: () -> Unit,
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
    LaunchedEffect(start) {
        location = if (start == LibraryStart.ALL_TRACKS) {
            LibraryLocation.AllTracks
        } else {
            folderPath = emptyList()
            LibraryLocation.Root
        }
    }

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
                    rootUri = activeRoot.uri,
                    viewModel = viewModel,
                    onOpenFolder = {
                        folderPath = listOf(it)
                        location = LibraryLocation.Folder(it)
                    },
                    playbackViewModel = playbackViewModel,
                    onTrackPlayed = onTrackPlayed,
                )
                LibraryLocation.AllTracks -> TrackContents(
                    title = stringResource(R.string.library),
                    tracks = viewModel.observeAllTracks(activeRoot.id)
                        .collectAsStateWithLifecycle(initialValue = emptyList()).value,
                    onBack = null,
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
/** Apresenta o estado vazio e permite escolher uma raiz musical. */
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
/** Mostra as pastas e faixas diretamente sob a raiz ativa. */
private fun RootContents(
    rootId: Long,
    rootUri: String,
    viewModel: LibraryViewModel,
    onOpenFolder: (MusicFolderEntity) -> Unit,
    playbackViewModel: PlaybackViewModel,
    onTrackPlayed: () -> Unit,
) {
    val rootEntries = viewModel.observeFolders(rootId, null)
        .collectAsStateWithLifecycle(initialValue = emptyList()).value
    val indexedRoot = rootEntries.firstOrNull { it.uri == rootUri }
    val folders = indexedRoot?.let { rootFolder ->
        viewModel.observeFolders(rootId, rootFolder.id)
            .collectAsStateWithLifecycle(initialValue = emptyList()).value
    } ?: rootEntries.filter { it.uri != rootUri }
    val tracks = viewModel.observeRootTracks(rootId, rootUri)
        .collectAsStateWithLifecycle(initialValue = emptyList()).value
    val visibleFolders = folders.filterNot { it.name.startsWith(".") }
    LibraryHeader(title = stringResource(R.string.folders))
    if (visibleFolders.isEmpty() && tracks.isEmpty()) {
        EmptyContent(R.string.no_folders)
    } else {
        LibraryItems(folders, tracks, onOpenFolder, viewModel, playbackViewModel, onTrackPlayed)
    }
}

@Composable
/** Mostra o conteúdo de uma pasta, incluindo subpastas e faixas. */
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
    val visibleFolders = folders.filterNot { it.name.startsWith(".") }
    LibraryHeader(folder.name.withoutParentheticalText(), onBack)
    if (visibleFolders.isEmpty() && tracks.isEmpty()) {
        EmptyContent(R.string.folder_empty)
    } else {
        LibraryItems(folders, tracks, onOpenFolder, viewModel, playbackViewModel, onTrackPlayed)
    }
}

@Composable
/** Filtra, ordena e apresenta todas as faixas com ações de reprodução. */
private fun TrackContents(
    title: String,
    tracks: List<TrackEntity>,
    onBack: (() -> Unit)?,
    viewModel: LibraryViewModel,
    playbackViewModel: PlaybackViewModel,
    onTrackPlayed: () -> Unit,
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle(initialValue = emptyList())
    var trackForPlaylist by remember { mutableStateOf<TrackEntity?>(null) }
    var showCreatePlaylist by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(TrackSortMode.NAME) }
    val visibleTracks = tracks
        .filter { track ->
            searchQuery.isBlank() ||
                listOf(track.title, track.artist.orEmpty(), track.album.orEmpty())
                    .any { it.contains(searchQuery, ignoreCase = true) }
        }
        .sortedWith(
            when (sortMode) {
                TrackSortMode.NAME -> compareBy<TrackEntity> { it.title.lowercase() }
                TrackSortMode.ARTIST -> compareBy<TrackEntity> { it.artist.orEmpty().lowercase() }
                TrackSortMode.ALBUM -> compareBy<TrackEntity> { it.album.orEmpty().lowercase() }
            }.thenBy { it.title.lowercase() },
        )
    LibraryHeader(title, onBack)
    if (tracks.isEmpty()) {
        EmptyContent(R.string.no_tracks)
    } else {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            singleLine = true,
            label = { Text(stringResource(R.string.search_tracks)) },
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.sort_by), modifier = Modifier.padding(end = 4.dp))
            TrackSortMode.entries.forEach { mode ->
                TextButton(onClick = { sortMode = mode }) {
                    Text(
                        text = stringResource(mode.labelRes),
                        color = if (sortMode == mode) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = {
                    playbackViewModel.playTracks(visibleTracks, source = QueueSource.ALL_TRACKS)
                    onTrackPlayed()
                },
                enabled = visibleTracks.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.play_all))
            }
            Button(
                onClick = {
                    playbackViewModel.playTracks(
                        visibleTracks.shuffled(),
                        source = QueueSource.ALL_TRACKS,
                    )
                    onTrackPlayed()
                },
                enabled = visibleTracks.isNotEmpty(),
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.play_all_shuffle))
            }
        }
        TrackList(
            tracks = visibleTracks,
            onPlay = {
                playbackViewModel.playTracks(visibleTracks, it, QueueSource.ALL_TRACKS)
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
/** Renderiza o cabeçalho da biblioteca com navegação opcional para trás. */
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
/** Lista pastas e faixas de uma localização da biblioteca. */
private fun LibraryItems(
    folders: List<MusicFolderEntity>,
    tracks: List<TrackEntity>,
    onOpenFolder: (MusicFolderEntity) -> Unit,
    viewModel: LibraryViewModel,
    playbackViewModel: PlaybackViewModel,
    onTrackPlayed: () -> Unit,
) {
    val visibleFolders = folders.filterNot { it.name.startsWith(".") }
    val playlists by viewModel.playlists.collectAsStateWithLifecycle(initialValue = emptyList())
    var trackForPlaylist by remember { mutableStateOf<TrackEntity?>(null) }
    var showCreatePlaylist by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(visibleFolders, key = { "folder-${it.id}" }) { folder ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(
                        appCardBrush(FoldersCardStart, FoldersCardEnd),
                        MaterialTheme.shapes.small,
                    )
                    .clickable { onOpenFolder(folder) }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Home,
                    stringResource(R.string.folder_icon),
                    tint = Color.White,
                )
                Text(
                    folder.name.withoutParentheticalText(),
                    color = Color.White,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
        items(tracks, key = { "track-${it.id}" }) { track ->
            TrackCard(
                track = track,
                onPlay = {
                    playbackViewModel.playTracks(tracks, track.id, QueueSource.ALL_TRACKS)
                    onTrackPlayed()
                },
                onToggleFavorite = {
                    viewModel.setFavorite(track.id, !track.isFavorite)
                },
                onAddToPlaylist = { trackForPlaylist = track },
            )
        }
    }

    if (trackForPlaylist != null && !showCreatePlaylist) {
        PlaylistPickerDialog(
            playlists = playlists,
            onDismiss = { trackForPlaylist = null },
            onCreatePlaylist = {
                newPlaylistName = ""
                showCreatePlaylist = true
            },
            onPlaylistSelected = { playlist ->
                viewModel.addToPlaylist(playlist.id, trackForPlaylist!!.id)
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
/** Renderiza uma lista de faixas delegando ações ao cartão de cada faixa. */
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
/** Mostra metadados, artwork e ações de uma faixa. */
private fun TrackCard(
    track: TrackEntity,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color.Black.copy(alpha = 0.16f))
            .clickable(onClick = onPlay)
            .testTag("track-${track.id}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = 14.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val artwork = remember(track.id, track.artwork) {
                track.artwork?.let { bytes ->
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                }
            }
            if (artwork != null) {
                Image(
                    bitmap = artwork,
                    contentDescription = track.title.withoutParentheticalText(),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(4.dp)),
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.88f),
                    modifier = Modifier.size(25.dp),
                )
            }
            Text(
                text = track.title.withoutParentheticalText(),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.92f),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
            )
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (track.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = stringResource(
                        if (track.isFavorite) R.string.remove_from_favorites else R.string.add_to_favorites,
                    ),
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.76f),
                )
            }
            IconButton(onClick = onAddToPlaylist) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.more_options),
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.76f),
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.18f))
    }
}

@Composable
/** Permite escolher uma playlist existente ou iniciar a sua criação. */
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
                            Text(playlist.name.withoutParentheticalText(), modifier = Modifier.fillMaxWidth())
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
/** Apresenta uma mensagem quando não há conteúdo para listar. */
private fun EmptyContent(messageRes: Int) {
    Text(
        stringResource(messageRes),
        modifier = Modifier.padding(24.dp),
        style = MaterialTheme.typography.bodyLarge,
    )
}
