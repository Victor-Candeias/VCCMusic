package pt.vcc.vccmusic.ui.screen

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import pt.vcc.vccmusic.R
import kotlin.math.abs

@Composable
fun NowPlayingScreen(
    viewModel: PlaybackViewModel,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val artwork = remember(state.artworkData) {
        state.artworkData?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }
    val swipeModifier = Modifier.pointerInput(Unit) {
        var distance = 0f
        detectHorizontalDragGestures(
            onHorizontalDrag = { _, dragAmount -> distance += dragAmount },
            onDragEnd = {
                if (abs(distance) >= 80f) {
                    if (distance > 0) viewModel.skipPrevious() else viewModel.skipNext()
                }
                distance = 0f
            },
            onDragCancel = { distance = 0f },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .then(swipeModifier)
            .testTag("screen-now-playing"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.now_playing), style = MaterialTheme.typography.titleLarge)
            IconButton(onClick = {}) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options))
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (artwork != null) {
                androidx.compose.foundation.Image(
                    bitmap = artwork,
                    contentDescription = stringResource(R.string.album_art),
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    Icons.Default.Home,
                    contentDescription = stringResource(R.string.album_art),
                    tint = Color.White,
                    modifier = Modifier.fillMaxSize(0.42f),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(state.title ?: stringResource(R.string.nothing_playing), style = MaterialTheme.typography.headlineSmall)
                Text(state.artist.orEmpty(), style = MaterialTheme.typography.bodyLarge)
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.FavoriteBorder, contentDescription = stringResource(R.string.favorite))
            }
        }
        Slider(
            value = if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f,
            onValueChange = { viewModel.seekTo((it * state.durationMs).toLong()) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.durationMs > 0,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(state.positionMs))
            Text(formatTime(state.durationMs))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = viewModel::toggleShuffle) { Icon(Icons.Default.Home, stringResource(R.string.shuffle)) }
            IconButton(onClick = viewModel::skipPrevious) { Icon(Icons.Default.Home, stringResource(R.string.previous)) }
            IconButton(onClick = viewModel::playPause) {
                Icon(
                    Icons.Default.PlayArrow,
                    stringResource(if (state.isPlaying) R.string.pause else R.string.play),
                )
            }
            IconButton(onClick = viewModel::skipNext) { Icon(Icons.Default.Home, stringResource(R.string.next)) }
            IconButton(onClick = viewModel::cycleRepeat) { Icon(Icons.Default.Home, repeatLabel(state.repeatMode)) }
        }
        Text(
            stringResource(R.string.swipe_to_change_track),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds.coerceAtLeast(0) / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Composable
private fun repeatLabel(mode: Int): String = when (mode) {
    Player.REPEAT_MODE_ONE -> stringResource(R.string.repeat_track)
    Player.REPEAT_MODE_ALL -> stringResource(R.string.repeat_queue)
    else -> stringResource(R.string.repeat_off)
}
