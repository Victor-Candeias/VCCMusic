package pt.vcc.vccmusic.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.vcc.vccmusic.R

@Composable
fun OnlineRadioScreen(
    stations: List<RadioStation>,
    playbackViewModel: PlaybackViewModel,
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(24.dp)
            .testTag("screen-online-radio"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.online_radio), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.online_radio_description))
        stations.filter { it.enabled }.forEach { station ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ElevatedButton(onClick = { playbackViewModel.playRadio(station.name, station.streamUrl) }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.play))
                    Text(
                        station.name,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
        if (stations.none { it.enabled }) {
            Text(stringResource(R.string.no_enabled_radio_stations))
        }
    }
}
