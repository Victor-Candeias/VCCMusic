package pt.vcc.vccmusic.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
/** Apresenta as opções de navegação da área de música. */
fun MusicMenuScreen(
    contentPadding: PaddingValues,
    onTracks: () -> Unit,
    onFolders: () -> Unit,
    onPlaylists: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(24.dp)
            .testTag("screen-music-menu"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.music), style = MaterialTheme.typography.headlineMedium)
        MenuAction(R.string.tracks, onTracks)
        MenuAction(R.string.folders, onFolders)
        MenuAction(R.string.playlists, onPlaylists)
        MenuAction(R.string.artists_unavailable, {})
        MenuAction(R.string.albums_unavailable, {})
        ElevatedButton(onClick = onBack) {
            Text(stringResource(R.string.back))
        }
    }
}

@Composable
/** Renderiza uma ação de menu com o texto e callback fornecidos. */
private fun MenuAction(labelRes: Int, onClick: () -> Unit) {
    ElevatedButton(onClick = onClick) {
        Text(stringResource(labelRes))
    }
}
