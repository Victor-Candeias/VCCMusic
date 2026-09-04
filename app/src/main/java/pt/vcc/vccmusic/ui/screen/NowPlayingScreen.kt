package pt.vcc.vccmusic.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import pt.vcc.vccmusic.R

@Composable
fun NowPlayingScreen(
    viewModel: PlaybackViewModel,
    contentPadding: PaddingValues,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(state.title ?: stringResource(R.string.nothing_playing), style = MaterialTheme.typography.headlineSmall)
        Text(state.artist ?: "", modifier = Modifier.padding(top = 8.dp))
        Slider(
            value = if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f,
            onValueChange = { viewModel.seekTo((it * state.durationMs).toLong()) },
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            enabled = state.durationMs > 0,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = viewModel::skipPrevious) {
                Icon(Icons.Default.Home, stringResource(R.string.previous))
            }
            IconButton(onClick = viewModel::playPause) {
                Icon(
                    Icons.Default.PlayArrow,
                    stringResource(if (state.isPlaying) R.string.pause else R.string.play),
                )
            }
            IconButton(onClick = viewModel::skipNext) {
                Icon(Icons.Default.Home, stringResource(R.string.next))
            }
        }
        Row {
            IconButton(onClick = viewModel::toggleShuffle) {
                Icon(Icons.Default.Home, stringResource(R.string.shuffle))
            }
            IconButton(onClick = viewModel::cycleRepeat) {
                Icon(Icons.Default.Home, repeatLabel(state.repeatMode))
            }
        }
    }
}

private fun repeatLabel(mode: Int): String = when (mode) {
    Player.REPEAT_MODE_ONE -> "Repetir faixa"
    Player.REPEAT_MODE_ALL -> "Repetir fila"
    else -> "Repetição desligada"
}
