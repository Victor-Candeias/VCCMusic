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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .testTag("screen-main-menu"),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = stringResource(R.string.my_music),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.my_music_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FeatureCard(R.string.music, R.string.all_your_tracks, Icons.Default.Home, MaterialTheme.colorScheme.primary, onMusic)
            FeatureCard(R.string.playlists, R.string.your_collections, Icons.Default.List, MaterialTheme.colorScheme.tertiary, onPlaylists)
            FeatureCard(R.string.now_playing, R.string.listening_now, Icons.Default.PlayArrow, MaterialTheme.colorScheme.secondary, onNowPlaying)
        }
        Text(
            text = stringResource(R.string.quick_actions),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CompactAction(R.string.online_radio, Icons.Default.PlayArrow, onOnlineRadio, "action-online-radio")
            CompactAction(R.string.settings, Icons.Default.Settings, onSettings)
        }
        CompactAction(R.string.close_app, Icons.Default.PlayArrow, onExit)
    }
}

@Composable
private fun FeatureCard(
    titleRes: Int,
    subtitleRes: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier,
        colors = CardDefaults.cardColors(containerColor = color),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = null, tint = Color.White)
            Text(stringResource(titleRes), color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text(stringResource(subtitleRes), color = Color.White.copy(alpha = 0.82f))
        }
    }
}

@Composable
private fun CompactAction(
    labelRes: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    testTag: String? = null,
) {
    ElevatedButton(
        onClick = onClick,
        modifier = testTag?.let(Modifier::testTag) ?: Modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, contentDescription = null)
            Text(stringResource(labelRes))
        }
    }
}
