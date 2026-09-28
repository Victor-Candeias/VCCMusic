package pt.vcc.vccmusic.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.vcc.vccmusic.R
import pt.vcc.vccmusic.data.local.TrackEntity
import pt.vcc.vccmusic.data.local.PlaylistEntity
import pt.vcc.vccmusic.data.withoutParentheticalText
import pt.vcc.vccmusic.podcast.model.PodcastFeed
import pt.vcc.vccmusic.ui.theme.MainMenuCardEnd
import pt.vcc.vccmusic.ui.theme.MainMenuCardStart
import pt.vcc.vccmusic.ui.theme.AppGradientCard
import pt.vcc.vccmusic.ui.theme.LibraryCardEnd
import pt.vcc.vccmusic.ui.theme.LibraryCardStart
import pt.vcc.vccmusic.ui.screen.RadioBrowserStation
import pt.vcc.vccmusic.ui.theme.PlaylistCardEnd
import pt.vcc.vccmusic.ui.theme.PlaylistCardStart
import pt.vcc.vccmusic.ui.theme.RadioCardEnd
import pt.vcc.vccmusic.ui.theme.RadioCardStart

private data class QuickAction(
    val labelRes: Int,
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color,
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val actions = listOf(
                QuickAction(
                    R.string.music,
                    Icons.Default.LibraryMusic,
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer,
                    onMusic,
                    "action-music",
                ),
                QuickAction(
                    R.string.playlists,
                    Icons.AutoMirrored.Filled.List,
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialTheme.colorScheme.onTertiaryContainer,
                    onPlaylists,
                    "action-playlists",
                ),
                QuickAction(
                    R.string.online_radio,
                    Icons.Default.Radio,
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.onSecondaryContainer,
                    onOnlineRadio,
                    "action-online-radio",
                ),
                QuickAction(
                    R.string.podcasts,
                    Icons.Default.Podcasts,
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer,
                    onPodcasts,
                    "action-podcasts",
                ),
            )
            actions.forEach { action ->
                FeatureCard(action, Modifier.fillMaxWidth(0.9f))
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val favorites = buildList<@Composable () -> Unit> {
                    favoriteTracks.forEach { track ->
                        add { FavoriteTrackCard(track, onClick = { onFavoriteTrack(track) }) }
                    }
                    favoritePlaylists.forEach { playlist ->
                        add {
                            FavoritePlaylistCard(
                                playlist = playlist,
                                trackCount = favoritePlaylistTrackCounts[playlist.id] ?: 0,
                                onClick = { onFavoritePlaylist(playlist, false) },
                                onShuffle = { onFavoritePlaylist(playlist, true) },
                            )
                        }
                    }
                    favoriteStations.forEach { station ->
                        add { FavoriteCard(station, onClick = { onFavoriteRadio(station) }) }
                    }
                    favoritePodcasts.forEach { podcast ->
                        add { FavoritePodcastCard(podcast, onClick = onPodcasts) }
                    }
                }

                favorites.forEach { favorite ->
                    Box(Modifier.fillMaxWidth(0.9f)) {
                        favorite()
                    }
                }
            }
        }
    }
}

@Composable
/** Mostra uma playlist favorita com contagem e opção de mistura. */
private fun FavoritePlaylistCard(
    playlist: PlaylistEntity,
    trackCount: Int,
    onClick: () -> Unit,
    onShuffle: () -> Unit,
) {
    AppGradientCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(28.dp),
        start = PlaylistCardStart,
        end = PlaylistCardEnd,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(playlist.name.withoutParentheticalText(), style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    text = "$trackCount ${if (trackCount == 1) "música" else "músicas"}",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                )
            }
            TextButton(onClick = onShuffle, enabled = trackCount > 0) {
                Text(stringResource(R.string.shuffle))
            }
        }
    }
}

@Composable
/** Mostra uma faixa favorita como cartão reproduzível. */
private fun FavoriteTrackCard(
    track: TrackEntity,
    onClick: () -> Unit,
) {
    AppGradientCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(28.dp),
        start = LibraryCardStart,
        end = LibraryCardEnd,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(track.title.withoutParentheticalText(), style = MaterialTheme.typography.titleMedium, maxLines = 1)
            track.artist?.takeIf { it.isNotBlank() }?.let {
                Text(it.withoutParentheticalText(), style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        }
    }
}

@Composable
/** Desenha um cartão de acesso a uma funcionalidade da aplicação. */
private fun FeatureCard(
    action: QuickAction,
    modifier: Modifier = Modifier,
) {
    AppGradientCard(
        onClick = action.onClick,
        modifier = modifier
            .height(56.dp)
            .testTag(action.testTag),
        shape = RoundedCornerShape(28.dp),
        start = MainMenuCardStart,
        end = MainMenuCardEnd,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(action.icon, contentDescription = null, tint = action.contentColor)
            Text(stringResource(action.labelRes), color = action.contentColor, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
/** Mostra uma estação de rádio favorita como cartão reproduzível. */
private fun FavoriteCard(
    station: RadioBrowserStation,
    onClick: () -> Unit,
) {
    AppGradientCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(28.dp),
        start = RadioCardStart,
        end = RadioCardEnd,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(station.name.withoutParentheticalText(), style = MaterialTheme.typography.titleMedium, maxLines = 2)
        }
    }
}

@Composable
/** Mostra um podcast favorito e abre a respetiva área de podcasts. */
private fun FavoritePodcastCard(
    podcast: PodcastFeed,
    onClick: () -> Unit,
) {
    AppGradientCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(28.dp),
        start = MainMenuCardStart,
        end = MainMenuCardEnd,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(podcast.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            podcast.author?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        }
    }
}
