package pt.vcc.vccmusic.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import retrofit2.HttpException
import java.io.IOException
import pt.vcc.vccmusic.podcast.model.PodcastEpisode
import pt.vcc.vccmusic.podcast.model.PodcastFeed
import pt.vcc.vccmusic.podcast.repository.PodcastRepository
import pt.vcc.vccmusic.podcast.repository.PodcastStore

data class PodcastUiState(
    val query: String = "",
    val feeds: List<PodcastFeed> = emptyList(),
    val selectedFeed: PodcastFeed? = null,
    val episodes: List<PodcastEpisode> = emptyList(),
    val favorites: List<PodcastFeed> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)

class PodcastViewModel(
    private val repository: PodcastRepository,
    private val store: PodcastStore,
) : ViewModel() {
    private val _state = MutableStateFlow(PodcastUiState())
    val state: StateFlow<PodcastUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            store.observeFavorites().collectLatest { favorites ->
                _state.update {
                    it.copy(
                        favorites = favorites.map { favorite ->
                            PodcastFeed(
                                id = favorite.feedId,
                                title = favorite.title,
                                url = null,
                                originalUrl = null,
                                link = null,
                                description = favorite.description,
                                author = favorite.author,
                                image = favorite.image,
                                artwork = favorite.image,
                                episodeCount = null,
                            )
                        },
                    )
                }
            }
        }
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query, error = null) }
    }

    fun search() {
        val query = state.value.query.trim()
        if (query.isBlank()) {
            _state.update { it.copy(error = "Escreva um termo para pesquisar podcasts.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, selectedFeed = null, episodes = emptyList()) }
            try {
                _state.update { it.copy(feeds = repository.search(query), loading = false) }
            } catch (error: IOException) {
                _state.update { it.copy(loading = false, error = "Não foi possível ligar à Podcast Index.") }
            } catch (error: HttpException) {
                _state.update { it.copy(loading = false, error = "A Podcast Index devolveu HTTP ${error.code()}.") }
            }
        }
    }

    fun openFeed(feed: PodcastFeed) {
        viewModelScope.launch {
            _state.update { it.copy(selectedFeed = feed, loading = true, error = null, episodes = emptyList()) }
            try {
                _state.update { it.copy(episodes = repository.episodes(feed.id), loading = false) }
            } catch (error: IOException) {
                _state.update { it.copy(loading = false, error = "Não foi possível carregar os episódios.") }
            } catch (error: HttpException) {
                _state.update { it.copy(loading = false, error = "A Podcast Index devolveu HTTP ${error.code()}.") }
            }

        }
    }

    fun toggleFavorite(feed: PodcastFeed) {
        viewModelScope.launch {
            store.setFavorite(feed, state.value.favorites.none { it.id == feed.id })
        }
    }

    fun closeFeed() {
        _state.update { it.copy(selectedFeed = null, episodes = emptyList(), error = null) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
