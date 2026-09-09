package pt.vcc.vccmusic.ui.screen

import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyColumnItems
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.vcc.vccmusic.R
import pt.vcc.vccmusic.ui.theme.RadioCardEnd
import pt.vcc.vccmusic.ui.theme.RadioCardStart
import pt.vcc.vccmusic.ui.theme.AppGradientCard
import androidx.compose.foundation.layout.BoxWithConstraints

private data class RadioGenre(val label: String, val query: String)

private val radioGenres = listOf(
    RadioGenre("Todas", ""),
    RadioGenre("Rock", "rock"),
    RadioGenre("Pop", "pop"),
    RadioGenre("Jazz", "jazz"),
    RadioGenre("Clássica", "classical"),
    RadioGenre("Dance", "dance"),
    RadioGenre("Eletrónica", "electronic"),
    RadioGenre("Notícias", "news"),
    RadioGenre("Talk", "talk"),
)

@Composable
/** Carrega, configura e apresenta as estações portuguesas disponíveis. */
fun OnlineRadioScreen(
    playbackViewModel: PlaybackViewModel,
    contentPadding: PaddingValues,
    repository: RadioBrowserRepository,
    forceConfiguration: Boolean = false,
    onConfigurationFinished: () -> Unit = {},
    onStationPlayed: () -> Unit = {},
) {
    val stations by repository.observePortugueseStations().collectAsState(initial = emptyList())
    val playbackState by playbackViewModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var availableStations by remember { mutableStateOf<List<RadioBrowserStation>?>(null) }
    var showConfiguration by remember { mutableStateOf(false) }

    LaunchedEffect(forceConfiguration) {
        try {
            val configuredStations = repository.observePortugueseStations().first()
            if (forceConfiguration || configuredStations.isEmpty()) {
                availableStations = repository.fetchAvailablePortugueseStations()
                showConfiguration = true
            }
        } catch (exception: Exception) {
            error = exception.message ?: "Não foi possível carregar as rádios."
        } finally {
            loading = false
        }
    }

    if (showConfiguration && availableStations != null) {
        RadioSelectionScreen(
            stations = availableStations.orEmpty(),
            initiallySelectedIds = stations.map { it.id }.toSet(),
            contentPadding = contentPadding,
            canCancel = stations.isNotEmpty(),
            onCancel = {
                showConfiguration = false
                onConfigurationFinished()
            },
            onSave = { selectedStations ->
                coroutineScope.launch {
                    repository.replaceConfiguredStations(selectedStations)
                    showConfiguration = false
                    availableStations = null
                    onConfigurationFinished()
                }
            },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 22.dp, vertical = 22.dp)
            .testTag("screen-online-radio"),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.online_radio),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.online_radio_description),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                maxLines = 1,
            )
        }
        when {
            loading -> CircularProgressIndicator()
            error != null -> Text(error ?: "", color = MaterialTheme.colorScheme.error)
            stations.isEmpty() -> Text(stringResource(R.string.no_online_radios))
            else -> BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                val numberOfColumns = when {
                    maxWidth >= 900.dp -> 5
                    maxWidth >= 600.dp -> 4
                    else -> 3
                }
                LazyVerticalGrid(
                    modifier = Modifier.fillMaxSize(),
                    columns = GridCells.Fixed(numberOfColumns),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(15.dp),
                ) {
                    items(stations, key = { it.id }) { station ->
                        RadioCard(
                            station = station,
                            repository = repository,
                            isPlaying = playbackState.mediaId == "radio:${station.streamUrl}",
                            onPlay = {
                                playbackViewModel.playRadio(
                                    station.name,
                                    station.streamUrl,
                                    station.faviconLocalPath,
                                )
                                onStationPlayed()
                            },
                            onFavoriteChanged = {
                                coroutineScope.launch { repository.setFavorite(station.id, it) }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
/** Permite pesquisar, filtrar e guardar a seleção de estações. */
private fun RadioSelectionScreen(
    stations: List<RadioBrowserStation>,
    initiallySelectedIds: Set<String>,
    contentPadding: PaddingValues,
    canCancel: Boolean,
    onCancel: () -> Unit,
    onSave: (List<RadioBrowserStation>) -> Unit,
) {
    var selectedIds by remember(stations, initiallySelectedIds) {
        mutableStateOf(initiallySelectedIds)
    }
    var searchQuery by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf(radioGenres.first()) }
    val visibleStations = stations.filter { station ->
        val searchableText = "${station.name} ${station.tags}"
        val matchesSearch = searchQuery.isBlank() ||
            searchableText.contains(searchQuery.trim(), ignoreCase = true)
        val matchesGenre = selectedGenre.query.isBlank() ||
            station.tags.contains(selectedGenre.query, ignoreCase = true)
        matchesSearch && matchesGenre
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 22.dp, vertical = 22.dp)
            .testTag("screen-radio-selection"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.configure_online_radios),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(R.string.configure_online_radios_description),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
        )
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.search_online_radios)) },
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            radioGenres.forEach { genre ->
                FilterChip(
                    selected = selectedGenre == genre,
                    onClick = { selectedGenre = genre },
                    label = { Text(genre.label) },
                )
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            lazyColumnItems(visibleStations, key = { it.id }) { station ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedIds = if (station.id in selectedIds) {
                                selectedIds - station.id
                            } else {
                                selectedIds + station.id
                            }
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = station.id in selectedIds,
                        onCheckedChange = { checked ->
                            selectedIds = if (checked) {
                                selectedIds + station.id
                            } else {
                                selectedIds - station.id
                            }
                        },
                    )
                    Text(station.name)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (canCancel) {
                ElevatedButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel))
                }
            }
            ElevatedButton(
                onClick = { onSave(stations.filter { it.id in selectedIds }) },
                enabled = selectedIds.isNotEmpty(),
            ) {
                Text(stringResource(R.string.save_online_radios))
            }
        }
    }
}

@Composable
/** Renderiza uma estação com estado de reprodução, favorito e favicon. */
private fun RadioCard(
    station: RadioBrowserStation,
    repository: RadioBrowserRepository,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onFavoriteChanged: (Boolean) -> Unit,
) {
    val cardShape = MaterialTheme.shapes.medium
    AppGradientCard(
        onClick = onPlay,
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            ,
        shape = cardShape,
        start = if (isPlaying) RadioCardStart else RadioCardStart.copy(alpha = 0.92f),
        end = RadioCardEnd,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
                RadioFavicon(station, repository, Modifier.size(48.dp))
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (station.isFavorite) Icons.Default.Star else Icons.Outlined.Star,
                    contentDescription = stringResource(R.string.favorite),
                    tint = if (station.isFavorite) Color.Yellow else Color.White,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { onFavoriteChanged(!station.isFavorite) },
                )
            }
                Text(
                    station.name,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
                Text(
                    text = station.tags.ifBlank { stringResource(R.string.online_radio) },
                    color = Color.White.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
        }
    }
}

@Composable
/** Carrega o favicon em cache e apresenta-o sem bloquear a composição. */
private fun RadioFavicon(
    station: RadioBrowserStation,
    repository: RadioBrowserRepository,
    modifier: Modifier = Modifier,
) {
    val image by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        null,
        station.id,
        station.favicon,
        station.faviconLocalPath,
    ) {
        value = station.favicon?.let {
            runCatching {
                withContext(Dispatchers.IO) {
                    val localPath = repository.cacheFavicon(station)
                    localPath?.let { path ->
                        BitmapFactory.decodeFile(path)?.asImageBitmap()
                    }
                }
            }.getOrNull()
        }
    }
    Box(
        modifier = modifier.height(48.dp).width(48.dp),
    ) {
        if (image != null) {
            androidx.compose.foundation.Image(
                bitmap = image!!,
                contentDescription = station.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
