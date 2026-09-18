package pt.vcc.vccmusic.ui.screen

import android.os.SystemClock
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
    val trending: List<PodcastFeed> = emptyList(),
    val recent: List<PodcastFeed> = emptyList(),
    val discoveryLoading: Boolean = false,
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
    private var searchJob: Job? = null

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
        loadDiscovery()
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query, error = null) }
    }

    fun search() {
        val query = state.value.query.trim()

        if (query.length < 2) {
            DiagnosticLogger.log(applicationContext, "Podcast", "Pesquisa recusada: termo vazio")
            _state.update {
                it.copy(error = "Escreva pelo menos dois caracteres para pesquisar podcasts.")
            }
            return
        }

        DiagnosticLogger.log(
            applicationContext,
            "Podcast",
            "Pesquisa solicitada: termo=${query.take(80)}",
        )
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val startedAt = SystemClock.elapsedRealtime()
            DiagnosticLogger.log(
                applicationContext,
                "Podcast",
                "Pesquisa iniciada: termo=${query.take(80)}",
            )
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
                    "Pesquisa concluída: resultados=${feeds.size}, duraçãoMs=${SystemClock.elapsedRealtime() - startedAt}",
                )
            } catch (error: IOException) {
                DiagnosticLogger.log(
                    applicationContext,
                    "Podcast",
                    "Pesquisa falhou: rede, duraçãoMs=${SystemClock.elapsedRealtime() - startedAt}",
                    error,
                )
                _state.update {
                    it.copy(
                        loading = false,
                        error = "Não foi possível ligar à Podcast Index.",
                    )
                }
            } catch (error: HttpException) {
                DiagnosticLogger.log(
                    applicationContext,
                    "Podcast",
                    "Pesquisa falhou: HTTP, duraçãoMs=${SystemClock.elapsedRealtime() - startedAt}",
                    error,
                )
                _state.update {
                    it.copy(
                        loading = false,
                        error = "A Podcast Index devolveu HTTP ${error.code()}.",
                    )
                }
            } catch (error: JsonParseException) {
                DiagnosticLogger.log(
                    applicationContext,
                    "Podcast",
                    "Pesquisa falhou: resposta inválida, duraçãoMs=${SystemClock.elapsedRealtime() - startedAt}",
                    error,
                )
                _state.update {
                    it.copy(
                        loading = false,
                        error = "A Podcast Index devolveu uma resposta inválida.",
                    )
                }
            } catch (error: IllegalArgumentException) {
                DiagnosticLogger.log(
                    applicationContext,
                    "Podcast",
                    "Pesquisa falhou: configuração inválida, duraçãoMs=${SystemClock.elapsedRealtime() - startedAt}",
                    error,
                )
                _state.update {
                    it.copy(
                        loading = false,
                        error = "A configuração da Podcast Index é inválida.",
                    )
                }
            } catch (error: ClassCastException) {
                DiagnosticLogger.log(
                    applicationContext,
                    "Podcast",
                    "Pesquisa falhou: tipo de resposta incompatível, duraçãoMs=${SystemClock.elapsedRealtime() - startedAt}",
                    error,
                )
                _state.update {
                    it.copy(
                        loading = false,
                        error = "A Podcast Index devolveu um formato incompatível.",
                    )
                }
            } catch (error: TimeoutCancellationException) {
                DiagnosticLogger.log(
                    applicationContext,
                    "Podcast",
                    "Pesquisa excedeu o tempo limite, duraçãoMs=${SystemClock.elapsedRealtime() - startedAt}",
                    error,
                )
                _state.update {
                    it.copy(
                        loading = false,
                        error = "A pesquisa demorou demasiado tempo. Tente novamente.",
                    )
                }
            } catch (error: CancellationException) {
                DiagnosticLogger.log(
                    applicationContext,
                    "Podcast",
                    "Pesquisa cancelada, duraçãoMs=${SystemClock.elapsedRealtime() - startedAt}",
                    error,
                )
                throw error
            }
        }

    }

    /** Carrega listas curtas de descoberta para a aplicação e o Android Auto. */
    fun loadDiscovery() {
        viewModelScope.launch {
            _state.update { it.copy(discoveryLoading = true) }
            try {
                val trending = repository.trending(max = 30, language = "pt")
                _state.update { it.copy(trending = trending) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                DiagnosticLogger.log(applicationContext, "Podcast", "Trending falhou", error)
            }
            try {
                val recent = repository.recent(max = 30, language = "pt")
                _state.update { it.copy(recent = recent) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                DiagnosticLogger.log(applicationContext, "Podcast", "Podcasts recentes falharam", error)
            }
            _state.update { it.copy(discoveryLoading = false) }
        }
    }

    fun openFeed(feed: PodcastFeed) {
        viewModelScope.launch {
            val startedAt = SystemClock.elapsedRealtime()
            DiagnosticLogger.log(
                applicationContext,
                "Podcast",
                "Episódios iniciados: feedId=${feed.id}",
            )
            _state.update { it.copy(selectedFeed = feed, loading = true, error = null, episodes = emptyList()) }
            try {
                _state.update { it.copy(episodes = repository.episodes(feed.id), loading = false) }
                DiagnosticLogger.log(
                    applicationContext,
                    "Podcast",
                    "Episódios concluídos: feedId=${feed.id}, resultados=${state.value.episodes.size}, duraçãoMs=${SystemClock.elapsedRealtime() - startedAt}",
                )
            } catch (error: IOException) {
                DiagnosticLogger.log(
                    applicationContext,
                    "Podcast",
                    "Episódios falharam: rede, feedId=${feed.id}, duraçãoMs=${SystemClock.elapsedRealtime() - startedAt}",
                    error,
                )
                _state.update { it.copy(loading = false, error = "Não foi possível carregar os episódios.") }
            } catch (error: HttpException) {
                DiagnosticLogger.log(
                    applicationContext,
                    "Podcast",
                    "Episódios falharam: HTTP, feedId=${feed.id}, duraçãoMs=${SystemClock.elapsedRealtime() - startedAt}",
                    error,
                )
                _state.update { it.copy(loading = false, error = "A Podcast Index devolveu HTTP ${error.code()}.") }
            } catch (error: JsonParseException) {
                DiagnosticLogger.log(
                    applicationContext,
                    "Podcast",
                    "Episódios falharam: resposta inválida, feedId=${feed.id}, duraçãoMs=${SystemClock.elapsedRealtime() - startedAt}",
                    error,
                )
                _state.update {
                    it.copy(
                        loading = false,
                        error = "A Podcast Index devolveu uma resposta inválida.",
                    )
                }
            } catch (error: IllegalArgumentException) {
                DiagnosticLogger.log(
                    applicationContext,
                    "Podcast",
                    "Episódios falharam: configuração inválida, feedId=${feed.id}, duraçãoMs=${SystemClock.elapsedRealtime() - startedAt}",
                    error,
                )
                _state.update {
                    it.copy(
                        loading = false,
                        error = "A configuração da Podcast Index é inválida.",
                    )
                }
            } catch (error: ClassCastException) {
                DiagnosticLogger.log(
                    applicationContext,
                    "Podcast",
                    "Episódios falharam: tipo de resposta incompatível, feedId=${feed.id}, duraçãoMs=${SystemClock.elapsedRealtime() - startedAt}",
                    error,
                )
                _state.update {
                    it.copy(
                        loading = false,
                        error = "A Podcast Index devolveu um formato incompatível.",
                    )
                }
            } catch (error: CancellationException) {
                DiagnosticLogger.log(
                    applicationContext,
                    "Podcast",
                    "Episódios cancelados: feedId=${feed.id}, duraçãoMs=${SystemClock.elapsedRealtime() - startedAt}",
                    error,
                )
                throw error
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
