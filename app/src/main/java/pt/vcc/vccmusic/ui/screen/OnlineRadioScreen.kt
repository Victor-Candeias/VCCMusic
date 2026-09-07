package pt.vcc.vccmusic.ui.screen

import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import pt.vcc.vccmusic.R
import androidx.compose.foundation.layout.BoxWithConstraints

@Composable
fun OnlineRadioScreen(
    playbackViewModel: PlaybackViewModel,
    contentPadding: PaddingValues,
    repository: RadioBrowserRepository,
) {
    val stations by repository.observePortugueseStations().collectAsState(initial = emptyList())
    val playbackState by playbackViewModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            if (stations.isEmpty()) repository.refreshPortugueseStations()
        } catch (exception: Exception) {
            error = exception.message ?: "Não foi possível carregar as rádios."
        } finally {
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF212121))
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
                color = Color.White,
            )
            Text(
                text = stringResource(R.string.online_radio_description),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
            )
        }
        when {
            loading -> CircularProgressIndicator()
            error != null -> Text(error ?: "", color = MaterialTheme.colorScheme.error)
            stations.isEmpty() -> Text(stringResource(R.string.no_online_radios))
            else -> BoxWithConstraints {
                val numberOfColumns = when {
                    maxWidth >= 900.dp -> 5
                    maxWidth >= 600.dp -> 4
                    else -> 3
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(numberOfColumns),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(15.dp),
                ) {
                    items(stations, key = { it.id }) { station ->
                        RadioCard(
                            station = station,
                            isPlaying = playbackState.mediaId == "radio:${station.streamUrl}",
                            onPlay = { playbackViewModel.playRadio(station.name, station.streamUrl) },
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
private fun RadioCard(
    station: RadioBrowserStation,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onFavoriteChanged: (Boolean) -> Unit,
) {
    Card(
        onClick = onPlay,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlaying) Color(0xFF1B5E20) else Color(0xFF4CAF50),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
                RadioFavicon(station, Modifier.size(48.dp))
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
private fun RadioFavicon(station: RadioBrowserStation, modifier: Modifier = Modifier) {
    val image by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, station.favicon) {
        value = station.favicon?.let { favicon ->
            runCatching {
                withContext(Dispatchers.IO) {
                    URL(favicon).openStream().use { input ->
                        BitmapFactory.decodeStream(input)?.asImageBitmap()
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
