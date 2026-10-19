package pt.vcc.vccmusic.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.vcc.vccmusic.R
import pt.vcc.vccmusic.data.local.PlaylistEntity
import pt.vcc.vccmusic.data.local.TrackEntity
import pt.vcc.vccmusic.data.withoutParentheticalText
import pt.vcc.vccmusic.podcast.model.PodcastFeed

private data class QuickAction(
    val labelRes: Int,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val testTag: String,
)

@Composable
/** Apresenta atalhos principais e os favoritos de música e rádio. */
fun MainMenuScreen(
    contentPadding: PaddingValues,
    onMusic: () -> Unit,
    onPlaylists: () -> Unit,
    onOnlineRadio: () -> Unit,
    onPodcasts: () -> Unit,
    favoriteTracks: List<TrackEntity>,
    favoritePlaylists: List<PlaylistEntity>,
    favoritePlaylistTrackCounts: Map<Long, Int>,
    favoriteStations: List<RadioBrowserStation>,
    favoritePodcasts: List<PodcastFeed>,
    onFavoriteTrack: (TrackEntity) -> Unit,
    onFavoritePlaylist: (PlaylistEntity, Boolean) -> Unit,
    onFavoriteRadio: (RadioBrowserStation) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(vertical = 20.dp)
            .verticalScroll(rememberScrollState())
            .testTag("screen-main-menu"),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
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
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                QuickAction(R.string.music, Icons.Default.LibraryMusic, onMusic, "action-music"),
                QuickAction(R.string.playlists, Icons.AutoMirrored.Filled.List, onPlaylists, "action-playlists"),
                QuickAction(R.string.online_radio, Icons.Default.Radio, onOnlineRadio, "action-online-radio"),
                QuickAction(R.string.podcasts, Icons.Default.Podcasts, onPodcasts, "action-podcasts"),
            ).forEach { action ->
                LibraryListRow(
                    title = stringResource(action.labelRes),
                    onClick = action.onClick,
                    modifier = Modifier.testTag(action.testTag),
                    leading = { ListRowIcon(action.icon) },
                )
            }
        }
        Text(
            text = stringResource(R.string.favorites),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 4.dp, start = 24.dp, end = 24.dp),
        )
        if (favoriteTracks.isEmpty() && favoritePlaylists.isEmpty() && favoriteStations.isEmpty() && favoritePodcasts.isEmpty()) {
            Text(
                text = stringResource(R.string.no_favorites),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                favoriteTracks.forEach { track ->
                    LibraryListRow(
                        title = track.title.withoutParentheticalText(),
                        subtitle = track.artist?.withoutParentheticalText(),
                        onClick = { onFavoriteTrack(track) },
                        leading = {
                            ListRowArtwork(track.id, track.artwork, track.title.withoutParentheticalText())
                        },
                    )
                }
                favoritePlaylists.forEach { playlist ->
                    val trackCount = favoritePlaylistTrackCounts[playlist.id] ?: 0
                    LibraryListRow(
                        title = playlist.name.withoutParentheticalText(),
                        subtitle = trackCountLabel(trackCount),
                        onClick = { onFavoritePlaylist(playlist, false) },
                        leading = { ListRowIcon(Icons.AutoMirrored.Filled.List) },
                        trailing = {
                            IconButton(onClick = { onFavoritePlaylist(playlist, true) }, enabled = trackCount > 0) {
                                Icon(
                                    Icons.Default.Shuffle,
                                    contentDescription = stringResource(R.string.shuffle),
                                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = LIST_ROW_ACTION_ALPHA),
                                )
                            }
                        },
                    )
                }
                favoriteStations.forEach { station ->
                    LibraryListRow(
                        title = station.name.withoutParentheticalText(),
                        onClick = { onFavoriteRadio(station) },
                        leading = { ListRowIcon(Icons.Default.Radio) },
                    )
                }
                favoritePodcasts.forEach { podcast ->
                    LibraryListRow(
                        title = podcast.title,
                        subtitle = podcast.author,
                        onClick = onPodcasts,
                        leading = { ListRowIcon(Icons.Default.Podcasts) },
                    )
                }
            }
        }
    }
}

/** Formata o número de músicas de uma playlist. */
internal fun trackCountLabel(trackCount: Int): String =
    "$trackCount ${if (trackCount == 1) "música" else "músicas"}"
