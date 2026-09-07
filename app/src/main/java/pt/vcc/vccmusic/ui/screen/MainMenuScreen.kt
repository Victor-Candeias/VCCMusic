package pt.vcc.vccmusic.ui.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Star
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
import pt.vcc.vccmusic.ui.screen.RadioBrowserStation

@Composable
fun MainMenuScreen(
    contentPadding: PaddingValues,
    onMusic: () -> Unit,
    onPlaylists: () -> Unit,
    onOnlineRadio: () -> Unit,
    favoriteStations: List<RadioBrowserStation>,
    onFavoriteRadio: (RadioBrowserStation) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .verticalScroll(rememberScrollState())
            .testTag("screen-main-menu"),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = stringResource(R.string.app_name),
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
            FeatureCard(R.string.music, Icons.Default.LibraryMusic, MaterialTheme.colorScheme.primary, onMusic, "action-music")
            FeatureCard(R.string.playlists, Icons.AutoMirrored.Filled.List, MaterialTheme.colorScheme.tertiary, onPlaylists, "action-playlists")
            FeatureCard(R.string.online_radio, Icons.Default.Radio, MaterialTheme.colorScheme.secondary, onOnlineRadio, "action-online-radio")
        }
        Text(
            text = stringResource(R.string.favorites),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (favoriteStations.isEmpty()) {
            Text(
                text = stringResource(R.string.no_favorites),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                favoriteStations.forEach { station ->
                    FavoriteCard(station, onClick = { onFavoriteRadio(station) })
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(
    titleRes: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    testTag: String,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(160.dp)
            .size(width = 160.dp, height = 142.dp)
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = color),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = null, tint = Color.White)
            Text(stringResource(titleRes), color = Color.White, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun FavoriteCard(
    station: RadioBrowserStation,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.size(width = 160.dp, height = 142.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Default.Star, contentDescription = stringResource(R.string.favorite))
            Text(station.name, style = MaterialTheme.typography.titleLarge, maxLines = 2)
        }
    }
}
