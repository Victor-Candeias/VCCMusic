package pt.vcc.vccmusic.ui.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
            else -> Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                stations.forEach { station ->
                    RadioCard(station) {
                        playbackViewModel.playRadio(station.name, station.streamUrl)
                    }
                }
            }
        }
    }
}

@Composable
private fun RadioCard(station: RadioBrowserStation, onPlay: () -> Unit) {
    Card(
        onClick = onPlay,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.play), tint = Color.White)
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
