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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import pt.vcc.vccmusic.R
import pt.vcc.vccmusic.data.withoutParentheticalText
import pt.vcc.vccmusic.ui.theme.DarkBackgroundBottom
import kotlin.math.abs
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
@OptIn(ExperimentalMaterial3Api::class)
@Composable
/** Apresenta a faixa atual, artwork, equalizador, controlos e a fila deslizante. */
fun NowPlayingScreen(
    viewModel: PlaybackViewModel,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val scaffoldState = rememberBottomSheetScaffoldState()
    val sheetState = scaffoldState.bottomSheetState
    val coroutineScope = rememberCoroutineScope()
    val queueExpanded = sheetState.targetValue == SheetValue.Expanded
    BackHandler(enabled = queueExpanded) {
        coroutineScope.launch { sheetState.partialExpand() }
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                pt.vcc.vccmusic.ui.theme.appBackgroundBrush(
                    darkTheme = MaterialTheme.colorScheme.background == DarkBackgroundBottom,
                ),
            )
            .testTag("screen-now-playing"),
    ) {
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            modifier = Modifier
                .padding(contentPadding)
                .clipToBounds(),
            sheetPeekHeight = QueuePeekHeight,
            containerColor = Color.Transparent,
            sheetContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            sheetDragHandle = {
                QueueHandle(
                    trackCount = queue.entries.size,
                    expanded = queueExpanded,
                    onToggle = {
                        coroutineScope.launch {
                            if (queueExpanded) sheetState.partialExpand() else sheetState.expand()
                        }
                    },
                )
            },
            sheetContent = {
                QueueList(
                    queue = queue,
                    currentArtwork = state.artworkData,
                    expanded = queueExpanded,
                    onSelect = viewModel::playQueueItem,
                )
            },
        ) { sheetPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(sheetPadding)
                    .then(swipeModifier),
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
                                .widthIn(max = 360.dp)
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
                                .fillMaxWidth(0.62f)
                                .aspectRatio(1f),
                        )
                        PlaybackControls(state = state, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

private val QueuePeekHeight = 56.dp

@Composable
/** Pega do painel da fila: arrastar ou tocar abre e fecha a lista. */
private fun QueueHandle(
    trackCount: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(QueuePeekHeight)
            .clickable(onClick = onToggle)
            .testTag("now-playing-queue-handle"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.playback_queue),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
            )
            Text(
                text = trackCountLabel(trackCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                contentDescription = stringResource(if (expanded) R.string.hide_queue else R.string.show_queue),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
/** Lista as faixas da fila pela ordem de reprodução; tocar numa faixa salta para ela. */
private fun QueueList(
    queue: PlaybackQueue,
    currentArtwork: ByteArray?,
    expanded: Boolean,
    onSelect: (Int) -> Unit,
) {
    val listState = rememberLazyListState()
    val currentPosition = queue.entries.indexOfFirst { it.windowIndex == queue.currentWindowIndex }
    LaunchedEffect(expanded) {
        if (expanded && currentPosition >= 0) listState.scrollToItem(currentPosition)
    }
    if (queue.entries.isEmpty()) {
        Text(
            text = stringResource(R.string.queue_empty),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        return
    }
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f)
            .testTag("now-playing-queue"),
    ) {
        itemsIndexed(queue.entries, key = { _, entry -> entry.windowIndex }) { position, entry ->
            val isCurrent = entry.windowIndex == queue.currentWindowIndex
            LibraryListRow(
                title = entry.title.withoutParentheticalText(),
                subtitle = entry.artist?.withoutParentheticalText(),
                onClick = { onSelect(entry.windowIndex) },
                highlighted = isCurrent,
                modifier = Modifier.testTag("queue-item-$position"),
                leading = {
                    ListRowArtwork(
                        trackId = entry.windowIndex.toLong(),
                        artwork = if (isCurrent) currentArtwork else null,
                        contentDescription = entry.title.withoutParentheticalText(),
                    )
                },
            )
        }
    }
}

@Composable
/** Mostra o título da área e o menu de opções. */
private fun Header() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.now_playing), style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = {}) {
            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options))
        }
    }
}

@Composable
/** Exibe a imagem da faixa ou o ícone da aplicação quando não existe artwork. */
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
                contentDescription = state.title?.withoutParentheticalText(),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Image(
                painter = painterResource(R.mipmap.ic_launcher),
                contentDescription = stringResource(R.string.app_name),
                modifier = Modifier.fillMaxSize(0.6f),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

@Composable
/** Renderiza metadados, progresso, equalizador e comandos do leitor. */
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
                Text(
                    state.title?.withoutParentheticalText() ?: stringResource(R.string.nothing_playing),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    state.artist.orEmpty().withoutParentheticalText(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                )
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
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
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
            Text(formatTime(state.positionMs))
            Text(formatTime(state.durationMs))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = viewModel::toggleShuffle) { Text("⇆", fontSize = 26.sp) }
            IconButton(onClick = viewModel::skipPrevious) { Text("|◀", fontSize = 22.sp) }
            IconButton(onClick = viewModel::playPause) {
                Text(
                    if (state.isPlaying) "Ⅱ" else "▶",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 28.sp,
                )
            }
            IconButton(onClick = viewModel::skipNext) { Text("▶|", fontSize = 22.sp) }
            IconButton(onClick = viewModel::cycleRepeat) { Text("↻", fontSize = 26.sp) }
        }
    }
}

@Composable
/** Desenha as barras do equalizador com base nos valores normalizados. */
private fun SpectrumEqualizer(
    values: List<Float>,
    modifier: Modifier = Modifier,
) {
    val equalizerColor = MaterialTheme.colorScheme.secondary
    Canvas(modifier = modifier) {
        val gap = size.width / (values.size * 3f)
        val barWidth = gap * 2f
        values.forEachIndexed { index, value ->
            val barHeight = size.height * (0.08f + value * 0.92f)
            drawRoundRect(
                color = equalizerColor,
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

/** Formata milissegundos como minutos e segundos para a barra de progresso. */
private fun formatTime(milliseconds: Long): String {
    val totalSeconds = milliseconds.coerceAtLeast(0) / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Composable
/** Traduz o modo de repetição do leitor para o texto localizado. */
private fun repeatLabel(mode: Int): String = when (mode) {
    Player.REPEAT_MODE_ONE -> stringResource(R.string.repeat_track)
    Player.REPEAT_MODE_ALL -> stringResource(R.string.repeat_queue)
    else -> stringResource(R.string.repeat_off)
}
