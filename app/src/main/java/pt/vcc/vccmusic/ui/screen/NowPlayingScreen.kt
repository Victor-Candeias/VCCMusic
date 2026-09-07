package pt.vcc.vccmusic.ui.screen

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
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
    val scrollState = rememberScrollState()
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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF442E2A), Color(0xFF101015)),
                ),
            )
            .padding(contentPadding)
            .then(swipeModifier)
            .testTag("screen-now-playing"),
    ) {
        val landscape = maxWidth > maxHeight
        val contentModifier = Modifier
            .then(if (landscape) Modifier else Modifier.verticalScroll(scrollState))
            .padding(horizontal = 16.dp, vertical = 8.dp)

        if (landscape) {
            Row(
                modifier = contentModifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Artwork(
                    state = state,
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 480.dp)
                        .aspectRatio(1f),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Header()
                    PlaybackControls(state = state, viewModel = viewModel)
                }
            }
        } else {
            Column(
                modifier = contentModifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Header()
                Artwork(
                    state = state,
                    modifier = Modifier
                        .fillMaxWidth(0.78f)
                        .aspectRatio(1f),
                )
                PlaybackControls(state = state, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun Header() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.now_playing), style = MaterialTheme.typography.titleMedium, color = Color.White)
        IconButton(onClick = {}) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options))
        }
    }
}

@Composable
private fun Artwork(
    state: PlaybackUiState,
    modifier: Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        val artwork = state.artworkData?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
        }
        if (artwork != null) {
            Image(
                bitmap = artwork,
                contentDescription = state.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text("♫", color = Color.White, fontSize = 72.sp)
        }
    }
}

@Composable
private fun PlaybackControls(
    state: PlaybackUiState,
    viewModel: PlaybackViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(state.title ?: stringResource(R.string.nothing_playing), style = MaterialTheme.typography.titleLarge, color = Color.White)
                Text(state.artist.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.75f))
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.FavoriteBorder, contentDescription = stringResource(R.string.favorite))
            }
        }
        SpectrumEqualizer(
            values = state.spectrum,
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .testTag("now-playing-equalizer"),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            listOf("60 Hz", "250 Hz", "1 kHz", "4 kHz", "16 kHz").forEach { frequency ->
                Text(
                    text = frequency,
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Slider(
            value = if (state.durationMs > 0) state.positionMs.toFloat() / state.durationMs else 0f,
            onValueChange = { viewModel.seekTo((it * state.durationMs).toLong()) },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.durationMs > 0,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(state.positionMs), color = Color.White)
            Text(formatTime(state.durationMs), color = Color.White)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = viewModel::toggleShuffle) { Text("⇆", color = Color.White, fontSize = 26.sp) }
            IconButton(onClick = viewModel::skipPrevious) { Text("|◀", color = Color.White, fontSize = 22.sp) }
            IconButton(onClick = viewModel::playPause) {
                Text(
                    if (state.isPlaying) "Ⅱ" else "▶",
                    color = Color.White,
                    fontSize = 28.sp,
                )
            }
            IconButton(onClick = viewModel::skipNext) { Text("▶|", color = Color.White, fontSize = 22.sp) }
            IconButton(onClick = viewModel::cycleRepeat) { Text("↻", color = Color.White, fontSize = 26.sp) }
        }
    }
}

@Composable
private fun SpectrumEqualizer(
    values: List<Float>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val gap = size.width / (values.size * 3f)
        val barWidth = gap * 2f
        values.forEachIndexed { index, value ->
            val barHeight = size.height * (0.08f + value * 0.92f)
            drawRoundRect(
                color = Color(0xFF55D6BE),
                topLeft = androidx.compose.ui.geometry.Offset(
                    x = gap + index * (barWidth + gap),
                    y = size.height - barHeight,
                ),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f),
            )
        }
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
