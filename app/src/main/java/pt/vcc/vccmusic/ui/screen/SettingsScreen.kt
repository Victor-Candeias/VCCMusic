package pt.vcc.vccmusic.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.vcc.vccmusic.R

private data class SettingsAction(
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit,
)

@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    onPickRoot: () -> Unit,
    onReindex: () -> Unit,
    onRefreshOnlineRadios: () -> Unit,
    onConfigureOnlineRadios: () -> Unit,
    radioApiUrl: String,
    onValidateRadioApiUrl: (String) -> Unit,
    radioApiValidationMessage: String?,
) {
    var editableRadioApiUrl by remember(radioApiUrl) { mutableStateOf(radioApiUrl) }
    val scrollState = rememberScrollState()
    LaunchedEffect(radioApiUrl) {
        editableRadioApiUrl = radioApiUrl
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 24.dp, vertical = 18.dp)
            .verticalScroll(scrollState)
            .testTag("screen-settings"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.settings_description))
        BoxWithConstraints {
            val columns = when {
                maxWidth >= 1100.dp -> 4
                maxWidth >= 650.dp -> 3
                else -> 2
            }
            val actions = listOf(
                SettingsAction(R.string.change_music_root, Icons.Default.Folder, onPickRoot),
                SettingsAction(R.string.reindex, Icons.Default.Refresh, onReindex),
                SettingsAction(R.string.configure_online_radios, Icons.Default.Settings, onConfigureOnlineRadios),
            )
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                actions.chunked(columns).forEach { rowActions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        rowActions.forEach { action ->
                            SettingsTile(action, Modifier.weight(1f))
                        }
                        repeat(columns - rowActions.size) {
                            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        OutlinedTextField(
            value = editableRadioApiUrl,
            onValueChange = { editableRadioApiUrl = it },
            label = { Text(stringResource(R.string.radio_api_url)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            ElevatedButton(onClick = { onValidateRadioApiUrl(editableRadioApiUrl) }) {
                Text(stringResource(R.string.validate_radio_api))
            }
        }
        radioApiValidationMessage?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }
        Text(
            stringResource(R.string.online_radio_settings),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            stringResource(R.string.settings_scope_note),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SettingsTile(
    action: SettingsAction,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = action.onClick,
        modifier = modifier
            .height(150.dp)
            .testTag("settings-${action.labelRes}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFAD2A9F)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.padding(bottom = 10.dp),
            )
            Text(
                text = stringResource(action.labelRes),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
