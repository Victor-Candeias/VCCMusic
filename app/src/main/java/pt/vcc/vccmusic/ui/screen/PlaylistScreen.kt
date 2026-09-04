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
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pt.vcc.vccmusic.R
import pt.vcc.vccmusic.data.local.PlaylistEntity
import pt.vcc.vccmusic.data.local.TrackEntity

@Composable
fun PlaylistScreen(
    viewModel: PlaylistViewModel,
    rootId: Long?,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle(initialValue = emptyList())
    var selected by remember { mutableStateOf<PlaylistEntity?>(null) }
    var showCreate by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().padding(contentPadding)) {
        if (selected == null) {
            PlaylistHeader(onCreate = { showCreate = true })
            if (playlists.isEmpty()) {
                Text(stringResource(R.string.no_playlists), modifier = Modifier.padding(24.dp))
            } else {
                LazyColumn {
                    items(playlists, key = { it.id }) { playlist ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { selected = playlist }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.List, null)
                            Text(playlist.name, modifier = Modifier.padding(start = 12.dp))
                        }
                        HorizontalDivider()
                    }
                }
            }
        } else {
            PlaylistDetail(
                playlist = selected!!,
                viewModel = viewModel,
                rootId = rootId,
                onBack = { selected = null },
                onDelete = {
                    viewModel.delete(selected!!.id)
                    selected = null
                },
            )
        }
    }
    if (showCreate) {
        PlaylistNameDialog(
            title = stringResource(R.string.create_playlist),
            onDismiss = { showCreate = false },
            onConfirm = {
                viewModel.create(it)
                showCreate = false
            },
        )
    }
}

@Composable
private fun PlaylistHeader(onCreate: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.playlists), style = MaterialTheme.typography.headlineSmall)
        Button(onClick = onCreate) {
            Text(stringResource(R.string.create_playlist))
        }
    }
}

@Composable
private fun PlaylistDetail(
    playlist: PlaylistEntity,
    viewModel: PlaylistViewModel,
    rootId: Long?,
    onBack: () -> Unit,
    onDelete: () -> Unit,
) {
    val tracks by viewModel.observeTracks(playlist.id)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val allTracks = rootId?.let {
        viewModel.observeAllTracks(it).collectAsStateWithLifecycle(initialValue = emptyList()).value
    } ?: emptyList()
    var showRename by remember { mutableStateOf(false) }
    val trackIds = tracks.map { it.id }
    var confirmDelete by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
        }
        Text(playlist.name, style = MaterialTheme.typography.headlineSmall)
        IconButton(onClick = { showRename = true }) {
            Icon(Icons.Default.Edit, stringResource(R.string.rename_playlist))
        }
        IconButton(onClick = { confirmDelete = true }) {
            Icon(Icons.Default.Delete, stringResource(R.string.delete_playlist))
        }
    }
    LazyColumn {
        items(tracks, key = { it.id }) { track ->
            TrackRow(
                track = track,
                onRemove = { viewModel.replaceTracks(playlist.id, trackIds - track.id) },
                onMoveUp = {
                    val index = trackIds.indexOf(track.id)
                    if (index > 0) viewModel.replaceTracks(playlist.id, trackIds.swap(index, index - 1))
                },
                onMoveDown = {
                    val index = trackIds.indexOf(track.id)
                    if (index < trackIds.lastIndex) viewModel.replaceTracks(playlist.id, trackIds.swap(index, index + 1))
                },
            )
        }
        items(allTracks.filterNot { it.id in trackIds }, key = { "available-${it.id}" }) { track ->
            OutlinedButton(
                onClick = { viewModel.replaceTracks(playlist.id, trackIds + track.id) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Text(stringResource(R.string.add_track, track.title))
            }
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
private fun TrackRow(
    track: TrackEntity,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(track.title, style = MaterialTheme.typography.titleMedium)
        Row {
            TextButton(onClick = onMoveUp) { Text(stringResource(R.string.move_up)) }
            TextButton(onClick = onMoveDown) { Text(stringResource(R.string.move_down)) }
            TextButton(onClick = onRemove) { Text(stringResource(R.string.remove)) }
        }
        HorizontalDivider()
    }
}

@Composable
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

private fun List<Long>.swap(first: Int, second: Int): List<Long> =
    toMutableList().also { values ->
        val value = values[first]
        values[first] = values[second]
        values[second] = value
    }
