package pt.vcc.vccmusic.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.vcc.vccmusic.R

@Composable
fun MainMenuScreen(
    contentPadding: PaddingValues,
    onMusic: () -> Unit,
    onPlaylists: () -> Unit,
    onNowPlaying: () -> Unit,
    onOnlineRadio: () -> Unit,
    onSettings: () -> Unit,
    onExit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(24.dp)
            .testTag("screen-main-menu"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.main_menu), style = MaterialTheme.typography.headlineMedium)
        MenuButton(R.string.music, onMusic)
        MenuButton(R.string.playlists, onPlaylists)
        MenuButton(R.string.now_playing, onNowPlaying)
        MenuButton(R.string.online_radio, onOnlineRadio)
        MenuButton(R.string.settings, onSettings)
        MenuButton(R.string.close_app, onExit)
    }
}

@Composable
private fun MenuButton(labelRes: Int, onClick: () -> Unit) {
    ElevatedButton(
        onClick = onClick,
    ) {
        Text(stringResource(labelRes))
    }
}
