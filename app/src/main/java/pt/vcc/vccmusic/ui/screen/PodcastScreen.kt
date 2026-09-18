package pt.vcc.vccmusic.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pt.vcc.vccmusic.R
import pt.vcc.vccmusic.podcast.model.PodcastEpisode
import pt.vcc.vccmusic.podcast.model.PodcastFeed

@Composable
fun PodcastScreen(
    viewModel: PodcastViewModel,
    playbackViewModel: PlaybackViewModel,
    contentPadding: PaddingValues,
    onEpisodePlayed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("screen-podcasts"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.selectedFeed == null) {
            PodcastSearchHeader(viewModel, state.query)
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (state.feeds.isEmpty()) {
                if (state.favorites.isEmpty() && state.trending.isEmpty() && state.recent.isEmpty()) {
                    Text(
                        stringResource(R.string.search_podcasts_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (state.favorites.isNotEmpty()) {
                            item { Text(stringResource(R.string.favorite_podcasts), style = MaterialTheme.typography.titleMedium) }
                        }
                        items(state.favorites, key = { "favorite-${it.id}" }) { feed ->
                            PodcastFeedRow(
                                feed = feed,
                                isFavorite = true,
                                onFavorite = { viewModel.toggleFavorite(feed) },
                                onClick = { viewModel.openFeed(feed) },
                            )
                        }
                        if (state.trending.isNotEmpty()) {
                            item { Text(stringResource(R.string.trending_podcasts), style = MaterialTheme.typography.titleMedium) }
                        }
                        items(state.trending, key = { "trending-${it.id}" }) { feed ->
                            PodcastFeedRow(
                                feed = feed,
                                isFavorite = state.favorites.any { it.id == feed.id },
                                onFavorite = { viewModel.toggleFavorite(feed) },
                                onClick = { viewModel.openFeed(feed) },
                            )
                        }
                        if (state.recent.isNotEmpty()) {
                            item { Text(stringResource(R.string.recent_podcasts), style = MaterialTheme.typography.titleMedium) }
                        }
                        items(state.recent, key = { "recent-${it.id}" }) { feed ->
                            PodcastFeedRow(
                                feed = feed,
                                isFavorite = state.favorites.any { it.id == feed.id },
                                onFavorite = { viewModel.toggleFavorite(feed) },
                                onClick = { viewModel.openFeed(feed) },
                            )
                        }
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(state.feeds, key = { it.id }) { feed ->
                        PodcastFeedRow(
                            feed = feed,
                            isFavorite = state.favorites.any { it.id == feed.id },
                            onFavorite = { viewModel.toggleFavorite(feed) },
                            onClick = { viewModel.openFeed(feed) },
                        )
                    }
                }
            }
        } else {
            val selectedFeed = state.selectedFeed ?: return@Column
            PodcastEpisodeList(
                feed = selectedFeed,
                episodes = state.episodes,
                loading = state.loading,
                error = state.error,
                onBack = viewModel::closeFeed,
                onPlay = {
                    playbackViewModel.playPodcast(it)
                    onEpisodePlayed()
                },
            )
        }
    }
}

@Composable
private fun PodcastSearchHeader(viewModel: PodcastViewModel, query: String) {
    Text(stringResource(R.string.podcasts), style = MaterialTheme.typography.headlineSmall)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::setQuery,
            modifier = Modifier.weight(1f),
            singleLine = true,
            label = { Text(stringResource(R.string.search_podcasts)) },
        )
        Button(onClick = viewModel::search) {
            Text(stringResource(R.string.search))
        }
    }
}

@Composable
private fun PodcastFeedRow(
    feed: PodcastFeed,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Podcasts, contentDescription = null)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(feed.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
            feed.author?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        }
        IconButton(onClick = onFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                contentDescription = stringResource(
                    if (isFavorite) R.string.remove_from_favorites else R.string.add_to_favorites,
                ),
            )
        }
    }
}

@Composable
private fun PodcastEpisodeList(
    feed: PodcastFeed,
    episodes: List<PodcastEpisode>,
    loading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onPlay: (PodcastEpisode) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
        }
        Text(feed.title, style = MaterialTheme.typography.titleLarge, maxLines = 2)
    }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    if (loading) {
        CircularProgressIndicator(modifier = Modifier.padding(vertical = 8.dp))
    } else if (episodes.isEmpty()) {
        Text(stringResource(R.string.no_podcast_episodes))
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(episodes, key = { it.id }) { episode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !episode.enclosureUrl.isNullOrBlank()) { onPlay(episode) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Podcasts, contentDescription = null)
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(episode.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
                        episode.datePublishedPretty?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
