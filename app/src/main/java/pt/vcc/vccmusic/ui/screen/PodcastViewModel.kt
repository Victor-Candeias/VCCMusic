package pt.vcc.vccmusic.ui.screen

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonParseException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.TimeoutCancellationException
import retrofit2.HttpException
import java.io.IOException
import pt.vcc.vccmusic.diagnostics.DiagnosticLogger
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
    context: Context,
    private val repository: PodcastRepository,
    private val store: PodcastStore,
) : ViewModel() {
    private val applicationContext = context.applicationContext
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
            DiagnosticLogger.log(applicationContext, "Podcast", "Pesquisa recusada: termo vazio")
            _state.update {
                it.copy(error = "Escreva um termo para pesquisar podcasts.")
            }
            return
        }

        viewModelScope.launch {
            DiagnosticLogger.log(applicationContext, "Podcast", "Pesquisa iniciada")
            _state.update {
                it.copy(
                    loading = true,
                    error = null,
                    selectedFeed = null,
                    episodes = emptyList(),
                )
            }

            try {
                val feeds = kotlinx.coroutines.withTimeout(30_000) {
                    repository.search(query)
                }

                _state.update {
                    it.copy(
                        feeds = feeds,
                        loading = false,
                    )
                }
                DiagnosticLogger.log(
                    applicationContext,
                    "Podcast",
                    "Pesquisa concluída: resultados=${feeds.size}",
                )
            } catch (error: IOException) {
                DiagnosticLogger.log(applicationContext, "Podcast", "Pesquisa falhou: rede", error)
                _state.update {
                    it.copy(
                        loading = false,
                        error = "Não foi possível ligar à Podcast Index.",
                    )
                }
            } catch (error: HttpException) {
                DiagnosticLogger.log(applicationContext, "Podcast", "Pesquisa falhou: HTTP", error)
                _state.update {
                    it.copy(
                        loading = false,
                        error = "A Podcast Index devolveu HTTP ${error.code()}.",
                    )
                }
            } catch (error: JsonParseException) {
                DiagnosticLogger.log(applicationContext, "Podcast", "Pesquisa falhou: resposta inválida", error)
                _state.update {
                    it.copy(
                        loading = false,
                        error = "A Podcast Index devolveu uma resposta inválida.",
                    )
                }
            } catch (error: IllegalArgumentException) {
                DiagnosticLogger.log(applicationContext, "Podcast", "Pesquisa falhou: configuração inválida", error)
                _state.update {
                    it.copy(
                        loading = false,
                        error = "A configuração da Podcast Index é inválida.",
                    )
                }
            } catch (error: TimeoutCancellationException) {
                DiagnosticLogger.log(applicationContext, "Podcast", "Pesquisa excedeu o tempo limite", error)
                _state.update {
                    it.copy(
                        loading = false,
                        error = "A pesquisa demorou demasiado tempo. Tente novamente.",
                    )
                }
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
            } catch (error: JsonParseException) {
                _state.update {
                    it.copy(
                        loading = false,
                        error = "A Podcast Index devolveu uma resposta inválida.",
                    )
                }
            } catch (error: IllegalArgumentException) {
                _state.update {
                    it.copy(
                        loading = false,
                        error = "A configuração da Podcast Index é inválida.",
                    )
                }
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
