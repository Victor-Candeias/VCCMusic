package pt.vcc.vccmusic.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onRefreshOnlineRadios: () -> Unit,
    radioApiUrl: String,
    onValidateRadioApiUrl: (String) -> Unit,
    radioApiValidationMessage: String?,
    radioStations: List<RadioStation>,
    onRadioStationEnabledChanged: (String, Boolean) -> Unit,
) {
    var editableRadioApiUrl by remember(radioApiUrl) { mutableStateOf(radioApiUrl) }
    LaunchedEffect(radioApiUrl) {
        editableRadioApiUrl = radioApiUrl
    }
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
        ElevatedButton(onClick = onRefreshOnlineRadios) {
            Text(stringResource(R.string.refresh_online_radios))
        }
        OutlinedTextField(
            value = editableRadioApiUrl,
            onValueChange = { editableRadioApiUrl = it },
            label = { Text(stringResource(R.string.radio_api_url)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        ElevatedButton(onClick = { onValidateRadioApiUrl(editableRadioApiUrl) }) {
            Text(stringResource(R.string.validate_radio_api))
        }
        radioApiValidationMessage?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
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
