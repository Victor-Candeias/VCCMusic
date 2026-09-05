package pt.vcc.vccmusic.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.vcc.vccmusic.R

@Composable
fun OnlineRadioScreen(
    playbackViewModel: PlaybackViewModel,
    contentPadding: PaddingValues,
) {
    val repository = remember { RadioBrowserRepository() }
    var stations by remember { mutableStateOf<List<RadioBrowserStation>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            stations = repository.loadPortugueseStations()
        } catch (exception: Exception) {
            error = exception.message ?: "Não foi possível carregar as rádios."
        } finally {
            loading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .testTag("screen-online-radio"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.online_radio), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.online_radio_description))
        when {
            loading -> CircularProgressIndicator()
            error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
            stations.isEmpty() -> Text(stringResource(R.string.no_online_radios))
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(stations, key = { it.id }) { station ->
                    RadioCard(
                        station = station,
                        onPlay = { playbackViewModel.playRadio(station.name, station.streamUrl) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RadioCard(station: RadioBrowserStation, onPlay: () -> Unit) {
    Card(
        onClick = onPlay,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.play), tint = Color.White)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(station.name, color = Color.White, style = MaterialTheme.typography.titleLarge)
                if (station.tags.isNotBlank()) {
                    Text(
                        station.tags,
                        color = Color.White.copy(alpha = 0.82f),
                        maxLines = 2,
                    )
                }
            }
        }
    }
}
