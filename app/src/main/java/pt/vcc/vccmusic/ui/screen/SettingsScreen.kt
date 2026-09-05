package pt.vcc.vccmusic.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.vcc.vccmusic.R

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    onPickRoot: () -> Unit,
    onReindex: () -> Unit,
    radioStations: List<RadioStation>,
    onRadioStationEnabledChanged: (String, Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(24.dp)
            .testTag("screen-settings"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.settings_description))
        ElevatedButton(onClick = onPickRoot) {
            Text(stringResource(R.string.change_music_root))
        }
        ElevatedButton(onClick = onReindex) {
            Text(stringResource(R.string.reindex))
        }
        Text(
            stringResource(R.string.online_radio_settings),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        radioStations.forEach { station ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(station.name)
                    Text(station.streamUrl, style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = station.enabled,
                    onCheckedChange = { onRadioStationEnabledChanged(station.name, it) },
                )
            }
        }
        Text(
            stringResource(R.string.settings_scope_note),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
