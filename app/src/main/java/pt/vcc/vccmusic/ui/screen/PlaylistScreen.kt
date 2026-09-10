package pt.vcc.vccmusic.ui.screen

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pt.vcc.vccmusic.R
import pt.vcc.vccmusic.data.local.PlaylistEntity
import pt.vcc.vccmusic.data.local.TrackEntity
import pt.vcc.vccmusic.data.withoutParentheticalText
import pt.vcc.vccmusic.playback.QueueSource
import pt.vcc.vccmusic.ui.theme.AppGradientCard
import pt.vcc.vccmusic.ui.theme.PlaylistCardEnd
import pt.vcc.vccmusic.ui.theme.PlaylistCardStart

@Composable
/** Apresenta playlists e o detalhe da playlist selecionada. */
fun PlaylistScreen(
    viewModel: PlaylistViewModel,
    rootId: Long?,
    contentPadding: PaddingValues,
    playbackViewModel: PlaybackViewModel,
    onTrackPlayed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle(initialValue = emptyList())
    var selected by remember { mutableStateOf<PlaylistEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .testTag("screen-playlists"),
    ) {
        if (selected == null) {
                PlaylistHeader()
            if (playlists.isEmpty()) {
                Text(stringResource(R.string.no_playlists), modifier = Modifier.padding(24.dp))
            } else {
                LazyColumn {
                    items(playlists, key = { it.id }) { playlist ->
                        val tracks by viewModel.observeTracks(playlist.id)
                            .collectAsStateWithLifecycle(initialValue = emptyList())
                        PlaylistCard(
                            playlist = playlist,
                            trackCount = tracks.size,
                            onOpen = { selected = playlist },
                            onPlay = {
                                playbackViewModel.playTracks(tracks, source = QueueSource.SELECTION, shuffle = false)
                                onTrackPlayed()
                            },
                            onShuffle = {
                                playbackViewModel.playTracks(tracks, source = QueueSource.SELECTION, shuffle = true)
                                onTrackPlayed()
                            },
                            onToggleFavorite = {
                                viewModel.setFavorite(playlist.id, !playlist.isFavorite)
                            },
                        )
                    }
                }
            }

        } else {
            PlaylistDetail(
                playlist = selected!!,
                viewModel = viewModel,
                onBack = { selected = null },
                onDelete = {
                    viewModel.delete(selected!!.id)
                    selected = null
                },
                playbackViewModel = playbackViewModel,
                onTrackPlayed = onTrackPlayed,
            )
        }
    }
}

@Composable
/** Mostra uma playlist com contagem, favorito e comandos de reprodução. */
private fun PlaylistCard(
    playlist: PlaylistEntity,
    trackCount: Int,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    AppGradientCard(
        onClick = onOpen,
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .padding(horizontal = 16.dp, vertical = 5.dp),
        start = PlaylistCardStart,
        end = PlaylistCardEnd,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(playlist.name.withoutParentheticalText(), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "$trackCount ${if (trackCount == 1) "música" else "músicas"}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (playlist.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = stringResource(
                        if (playlist.isFavorite) R.string.remove_from_favorites else R.string.add_to_favorites,
                    ),
                )
            }
            IconButton(onClick = onPlay, enabled = trackCount > 0) {
                Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.play_playlist))
            }
            IconButton(onClick = onShuffle, enabled = trackCount > 0) {
                Icon(Icons.Default.Shuffle, contentDescription = stringResource(R.string.shuffle))
            }
        }
    }
}

@Composable
/** Renderiza o título da área de playlists. */
private fun PlaylistHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.playlists), style = MaterialTheme.typography.headlineSmall)
    }
}

@Composable
/** Apresenta as faixas de uma playlist e ações de edição ou remoção. */
private fun PlaylistDetail(
    playlist: PlaylistEntity,
    viewModel: PlaylistViewModel,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    playbackViewModel: PlaybackViewModel,
    onTrackPlayed: () -> Unit,
) {
    val tracks by viewModel.observeTracks(playlist.id)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var showRename by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
        }
        Text(playlist.name.withoutParentheticalText(), style = MaterialTheme.typography.headlineSmall)
        IconButton(onClick = { showRename = true }) {
            Icon(Icons.Default.Edit, stringResource(R.string.rename_playlist))
        }
        IconButton(onClick = { confirmDelete = true }) {
            Icon(Icons.Default.Delete, stringResource(R.string.delete_playlist))
        }
    }
    Button(
        onClick = {
            playbackViewModel.playTracks(tracks, source = QueueSource.SELECTION, shuffle = false)
            onTrackPlayed()
        },
        enabled = tracks.isNotEmpty(),
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        Text(stringResource(R.string.play_playlist))
    }
    LazyColumn {
        items(tracks, key = { it.id }) { track ->
            TrackRow(
                track = track,
                onRemove = {
                    viewModel.replaceTracks(
                        playlist.id,
                        tracks.map { it.id }.filterNot { it == track.id },
                    )
                },
            )
        }
    }
    if (showRename) {
        PlaylistNameDialog(
            title = stringResource(R.string.rename_playlist),
            initialName = playlist.name,
            onDismiss = { showRename = false },
            onConfirm = {
                viewModel.rename(playlist.id, it)
                showRename = false
            },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_playlist)) },
            text = { Text(stringResource(R.string.confirm_delete_playlist)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) {
                    Text(stringResource(R.string.delete_playlist))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
/** Mostra uma faixa da playlist com a ação de a remover. */
private fun TrackRow(
    track: TrackEntity,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val artwork = track.artwork?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
        }
        if (artwork != null) {
            Image(
                bitmap = artwork,
                contentDescription = track.title.withoutParentheticalText(),
                modifier = Modifier.size(56.dp),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                Icons.AutoMirrored.Filled.List,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
            )
        }
        Text(
            track.title.withoutParentheticalText(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 12.dp).weight(1f),
        )
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Default.Delete,
                contentDescription = stringResource(R.string.remove),
            )
        }
    }
    HorizontalDivider()
}

@Composable
/** Solicita e valida o novo nome de uma playlist. */
private fun PlaylistNameDialog(
    title: String,
    initialName: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.playlist_name)) },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
