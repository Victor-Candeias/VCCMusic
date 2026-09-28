package pt.vcc.vccmusic.playback
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionError
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pt.vcc.vccmusic.VccMusicApplication
import pt.vcc.vccmusic.R
import pt.vcc.vccmusic.data.local.TrackEntity
import pt.vcc.vccmusic.data.withoutParentheticalText
import pt.vcc.vccmusic.diagnostics.DiagnosticLogger
import pt.vcc.vccmusic.data.local.PodcastFavoriteEntity
import pt.vcc.vccmusic.podcast.model.PodcastEpisode
import pt.vcc.vccmusic.podcast.model.PodcastFeed
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import pt.vcc.vccmusic.ui.screen.RadioBrowserRepository
import pt.vcc.vccmusic.ui.screen.RadioBrowserStation

@OptIn(UnstableApi::class)
class MusicPlaybackService : MediaLibraryService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var player: ExoPlayer
    private var mediaLibrarySession: MediaLibrarySession? = null
    private lateinit var libraryCallback: LibraryCallback
    private var radioHttpFallbackAttemptedFor: String? = null
    private var heartbeatJob: kotlinx.coroutines.Job? = null
    private var connectedControllers = 0

    /** Cria o leitor, a sessão multimédia e inicia a sincronização da biblioteca. */
    override fun onCreate() {
        super.onCreate()
        DiagnosticLogger.log(
            this,
            "PlaybackService",
            "Serviço multimédia iniciado: thread=${Thread.currentThread().name}",
        )
        radioRepository = RadioBrowserRepository(this)
        player = ExoPlayer.Builder(this, SpectrumRenderersFactory(this))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        player.addListener(
            object : Player.Listener {
                /** Regista erros e tenta HTTP ou a próxima faixa quando aplicável. */
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    val currentItem = player.currentMediaItem
                    val currentUri = currentItem?.localConfiguration?.uri
                    DiagnosticLogger.log(
                        this@MusicPlaybackService,
                        "Player",
                        "Erro de reprodução: code=${error.errorCode}, " +
                            "name=${error.errorCodeName}, mediaId=${currentItem?.mediaId}, " +
                            "uri=$currentUri, state=${playerStateDescription()}",
                        error,
                    )
                    if (
                        currentItem?.mediaId?.startsWith("radio:") == true &&
                        currentUri != null &&
                        currentUri.scheme.equals("https", ignoreCase = true) &&
                        radioHttpFallbackAttemptedFor != currentItem.mediaId
                    ) {
                        radioHttpFallbackAttemptedFor = currentItem.mediaId
                        retryRadioOverHttp(currentItem, currentUri)
                    } else if (player.hasNextMediaItem()) {
                        player.seekToNextMediaItem()
                    } else {
                        player.stop()
                    }
                }

                /** Regista alterações do estado do leitor para diagnosticar bloqueios de reprodução. */
                override fun onPlaybackStateChanged(playbackState: Int) {
                    val state = when (playbackState) {
                        Player.STATE_IDLE -> "IDLE"
                        Player.STATE_BUFFERING -> "BUFFERING"
                        Player.STATE_READY -> "READY"
                        Player.STATE_ENDED -> "ENDED"
                        else -> playbackState.toString()
                    }
                    DiagnosticLogger.log(this@MusicPlaybackService, "Player", "Estado alterado: $state")
                }

                /** Regista o início e a paragem efetiva da reprodução. */
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    DiagnosticLogger.log(this@MusicPlaybackService, "Player", "A reproduzir: $isPlaying")
                }

                /** Regista a decisão de reprodução e a razão de cada alteração. */
                override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                    DiagnosticLogger.log(
                        this@MusicPlaybackService,
                        "Player",
                        "Reprodução solicitada: playWhenReady=$playWhenReady, motivo=$reason",
                    )
                }

                /** Regista bloqueios de reprodução impostos pelo sistema ou pela sessão. */
                override fun onPlaybackSuppressionReasonChanged(playbackSuppressionReason: Int) {
                    DiagnosticLogger.log(
                        this@MusicPlaybackService,
                        "Player",
                        "Reprodução suprimida: motivo=$playbackSuppressionReason",
                    )
                }

                /** Regista quando o leitor começa ou termina de carregar dados. */
                override fun onIsLoadingChanged(isLoading: Boolean) {
                    DiagnosticLogger.log(this@MusicPlaybackService, "Player", "A carregar: $isLoading")
                }

                /** Regista a faixa selecionada pelo Android Auto ou pela aplicação. */
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    DiagnosticLogger.log(
                        this@MusicPlaybackService,
                        "Player",
                        "Faixa alterada: mediaId=${mediaItem?.mediaId}, motivo=$reason",
                    )
                }

                override fun onEvents(player: Player, events: Player.Events) {
                    DiagnosticLogger.log(
                        this@MusicPlaybackService,
                        "Player",
                        "Eventos agrupados: flags=${events}, state=${playerStateDescription()}",
                    )
                }
            },
        )
        libraryCallback = LibraryCallback()
        mediaLibrarySession = MediaLibrarySession.Builder(
            this,
            player,
            libraryCallback,
        ).build()
        heartbeatJob = serviceScope.launch {
            while (isActive) {
                delay(30_000)
                DiagnosticLogger.log(
                    this@MusicPlaybackService,
                    "Heartbeat",
                    "Serviço vivo: controllers=$connectedControllers, ${playerStateDescription()}",
                )
            }
        }
        loadActiveLibrary()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        DiagnosticLogger.log(
            this,
            "PlaybackService",
            "onStartCommand: action=${intent?.action ?: "nenhuma"}, " +
                "flags=$flags, startId=$startId, ${playerStateDescription()}",
        )
        return super.onStartCommand(intent, flags, startId)
    }

    /** Regista os pedidos de ligação ao serviço, incluindo os feitos pelo Android Auto. */
    override fun onBind(intent: Intent?): IBinder? {
        DiagnosticLogger.log(
            this,
            "PlaybackService",
            "Serviço ligado: ação=${intent?.action ?: "nenhuma"}, " +
                "component=${intent?.component}, ${playerStateDescription()}",
        )
        return super.onBind(intent)
    }

    /** Regista o fim de cada ligação ao serviço antes da limpeza do estado. */
    override fun onUnbind(intent: Intent?): Boolean {
        DiagnosticLogger.log(
            this,
            "PlaybackService",
            "Serviço desligado: ação=${intent?.action ?: "nenhuma"}, " +
                "component=${intent?.component}, ${playerStateDescription()}",
        )
        return super.onUnbind(intent)
    }

    private lateinit var radioRepository: RadioBrowserRepository

    /** Repete uma rádio trocando HTTPS por HTTP como fallback de compatibilidade. */
    private fun retryRadioOverHttp(mediaItem: MediaItem, uri: Uri) {
        DiagnosticLogger.log(
            this,
            "Player",
            "Fallback rádio HTTPS->HTTP: mediaId=${mediaItem.mediaId}, uri=$uri",
        )
        player.setMediaItem(
            mediaItem.buildUpon()
                .setUri(uri.buildUpon().scheme("http").build())
                .build(),
        )
        player.prepare()
        player.play()
    }

    /** Observa a raiz ativa e mantém a fila local do leitor sincronizada. */
    private fun loadActiveLibrary() {
        serviceScope.launch(Dispatchers.IO) {
            val container = (application as VccMusicApplication).container
            val root = container.musicRepository.activeRoot()
            if (root == null) {
                DiagnosticLogger.log(this@MusicPlaybackService, "Library", "Serviço iniciado sem pasta ativa")
                return@launch
            }
            val tracks = container.musicRepository.observeAllTracks(root.id)
            tracks.collect { entities ->
                val items = QueueBuilder.build(
                    QueueRequest(QueueSource.ALL_TRACKS, entities),
                ).map(::toMediaItem)
                launch(Dispatchers.Main) {
                    val currentId = player.currentMediaItem?.mediaId
                    if (currentId?.startsWith("radio:") == true ||
                        currentId?.startsWith(MediaIds.PODCAST_EPISODE_PREFIX) == true
                    ) {
                        return@launch
                    }
                    if (items.isEmpty()) {
                        player.clearMediaItems()
                    } else {
                        val index = player.currentMediaItemIndex
                            .coerceIn(0, items.lastIndex)
                        player.setMediaItems(items, index, player.currentPosition)
                        player.prepare()
                    }
                }
            }
        }
    }

    /** Converte uma entidade de faixa num item reproduzível da biblioteca. */
    private fun toMediaItem(track: TrackEntity): MediaItem =
        MediaItem.Builder()
            .setMediaId(trackId(track.id))
            .setUri(track.uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title.withoutParentheticalText())
                    .setArtist(track.artist?.withoutParentheticalText())
                    .setAlbumTitle(track.album?.withoutParentheticalText())
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build(),
            )
            .build()

    /** Cria um item navegável que representa uma pasta. */
    private fun toFolderItem(id: Long, name: String): MediaItem =
        MediaItem.Builder()
            .setMediaId(folderId(id))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(name.withoutParentheticalText())
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build(),
            )
            .build()

    /** Cria um item navegável para uma categoria da biblioteca. */
    private fun toCategoryItem(id: String, title: String): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build(),
            )
            .build()

    /** Cria um item navegável que representa uma playlist. */
    private fun toPlaylistItem(id: Long, name: String): MediaItem =
        MediaItem.Builder()
            .setMediaId(playlistId(id))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(name.withoutParentheticalText())
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build(),
            )
            .build()

    private fun toPodcastFeedItem(feed: PodcastFavoriteEntity): MediaItem =
        MediaItem.Builder()
            .setMediaId(MediaIds.podcastFeed(feed.feedId))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(feed.title)
                    .setArtist(feed.author)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build(),
            )
            .build()

    private fun toPodcastFeedItem(feed: PodcastFeed): MediaItem =
        MediaItem.Builder()
            .setMediaId(MediaIds.podcastFeed(feed.id))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(feed.title)
                    .setArtist(feed.author ?: "Podcast")
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build(),
            )
            .build()

    private fun toPodcastEpisodeItem(episode: PodcastEpisode): MediaItem =
        MediaItem.Builder()
            .setMediaId(MediaIds.podcastEpisode(episode.id))
            .setUri(episode.enclosureUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(episode.title)
                    .setArtist(episode.feedTitle ?: episode.feedAuthor ?: "Podcast")
                    .setIsBrowsable(false)
                    .setIsPlayable(!episode.enclosureUrl.isNullOrBlank())
                    .build(),
            )
            .build()

    /** Devolve a sessão multimédia e regista o controlador que a solicitou. */
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        mediaLibrarySession.also {
            DiagnosticLogger.log(
                this,
                "AndroidAuto",
                "Sessão solicitada: ${controllerDescription(controllerInfo)}, " +
                    "sessionAvailable=${it != null}",
            )
        }

    /** Encerra o serviço quando a tarefa é removida e não há reprodução ativa. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        DiagnosticLogger.log(
            this,
            "PlaybackService",
            "Tarefa removida: intent=$rootIntent, controllers=$connectedControllers, " +
                "${playerStateDescription()}",
        )
        if (!player.isPlaying) stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    /** Cancela tarefas, liberta a sessão e liberta o leitor Media3. */
    override fun onDestroy() {
        DiagnosticLogger.log(
            this,
            "PlaybackService",
            "Serviço multimédia terminado: controllers=$connectedControllers, " +
                "${playerStateDescription()}",
        )
        heartbeatJob?.cancel()
        serviceScope.cancel()
        mediaLibrarySession?.release()
        player.release()
        mediaLibrarySession = null
        super.onDestroy()
    }

    /** Identifica o cliente que iniciou cada operação da sessão multimédia. */
    private fun controllerDescription(controller: MediaSession.ControllerInfo): String {
        val origin = if (controller.packageName == packageName) "local" else "externa"
        val hints = controller.connectionHints.keySet().joinToString(",")
        return "origem=$origin, package=${controller.packageName}, uid=${controller.uid}, " +
            "hints=${if (hints.isEmpty()) "nenhumas" else hints}"
    }

    private fun playerStateDescription(): String =
        "state=${player.playbackState}, isPlaying=${player.isPlaying}, " +
            "playWhenReady=${player.playWhenReady}, loading=${player.isLoading}, " +
            "suppression=${player.playbackSuppressionReason}, " +
            "mediaId=${player.currentMediaItem?.mediaId}, position=${player.currentPosition}"

    private fun mediaItemsDescription(items: List<MediaItem>): String =
        items.joinToString(prefix = "[", postfix = "]") { it.mediaId }

    private fun logLibraryResult(
        operation: String,
        startNanos: Long,
        result: LibraryResult<*>,
    ) {
        val durationMs = (System.nanoTime() - startNanos) / 1_000_000
        DiagnosticLogger.log(
            this@MusicPlaybackService,
            "MediaLibrary",
            "$operation concluído: duraçãoMs=$durationMs, resultCode=${result.resultCode}",
        )
    }

    private inner class LibraryCallback : MediaLibrarySession.Callback {
        override fun onPlayerCommandRequest(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            playerCommand: Int,
        ): Int {
            DiagnosticLogger.log(
                this@MusicPlaybackService,
                "AndroidAuto",
                "Comando de reprodução: ${controllerDescription(controller)}, " +
                    "command=$playerCommand, ${playerStateDescription()}",
            )
            return super.onPlayerCommandRequest(session, controller, playerCommand)
        }

        override fun onMediaButtonEvent(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaButtonEvent: Intent,
        ): Boolean {
            DiagnosticLogger.log(
                this@MusicPlaybackService,
                "AndroidAuto",
                "Evento de botão: ${controllerDescription(controller)}, " +
                    "action=${mediaButtonEvent.action}, extras=${mediaButtonEvent.extras?.keySet()?.joinToString(",")}",
            )
            return super.onMediaButtonEvent(session, controller, mediaButtonEvent)
        }

        override fun onSetMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            DiagnosticLogger.log(
                this@MusicPlaybackService,
                "AndroidAuto",
                "Definir faixas: ${controllerDescription(controller)}, " +
                    "items=${mediaItemsDescription(mediaItems)}, " +
                    "startIndex=$startIndex, startPositionMs=$startPositionMs",
            )
            val future =
                SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
            serviceScope.launch(Dispatchers.IO) {
                try {
                    val resolvedItems = mediaItems.mapNotNull { item ->
                        resolvePlayableMediaItem(item)
                    }
                    if (resolvedItems.isEmpty()) {
                        future.set(
                            MediaSession.MediaItemsWithStartPosition(
                                mediaItems,
                                startIndex,
                                startPositionMs,
                            )
                        )
                        return@launch
                    }
                    val safeIndex =
                        startIndex.coerceIn(0, resolvedItems.lastIndex)
                    future.set(
                        MediaSession.MediaItemsWithStartPosition(
                            resolvedItems,
                            safeIndex,
                            startPositionMs,
                        )
                    )
                } catch (error: Exception) {
                    DiagnosticLogger.log(
                        this@MusicPlaybackService,
                        "AndroidAuto",
                        "Falha ao resolver MediaItems para reprodução",
                        error,
                    )
                    future.setException(error)
                }
            }
            return future
        }

        override fun onAddMediaItems(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>,
        ): ListenableFuture<List<MediaItem>> {
            DiagnosticLogger.log(
                this@MusicPlaybackService,
                "AndroidAuto",
                "Adicionar faixas: ${controllerDescription(controller)}, " +
                    "items=${mediaItemsDescription(mediaItems)}",
            )
            return super.onAddMediaItems(session, controller, mediaItems)
        }

        override fun onPlaybackResumption(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            DiagnosticLogger.log(
                this@MusicPlaybackService,
                "AndroidAuto",
                "Retomar reprodução: ${controllerDescription(controller)}, ${playerStateDescription()}",
            )
            return super.onPlaybackResumption(session, controller)
        }

        override fun onPlayerInteractionFinished(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            playerCommands: Player.Commands,
        ) {
            DiagnosticLogger.log(
                this@MusicPlaybackService,
                "AndroidAuto",
                "Interação terminada: ${controllerDescription(controller)}, " +
                    "commands=$playerCommands, ${playerStateDescription()}",
            )
            super.onPlayerInteractionFinished(session, controller, playerCommands)
        }

        override fun onSubscribe(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            params: MediaLibraryService.LibraryParams?,
        ): ListenableFuture<LibraryResult<Void>> {
            DiagnosticLogger.log(
                this@MusicPlaybackService,
                "AndroidAuto",
                "Subscrever conteúdos: ${controllerDescription(browser)}, " +
                    "parentId=$parentId, params=${params?.extras?.keySet()?.joinToString(",")}",
            )
            return super.onSubscribe(session, browser, parentId, params)
        }

        override fun onUnsubscribe(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
        ): ListenableFuture<LibraryResult<Void>> {
            DiagnosticLogger.log(
                this@MusicPlaybackService,
                "AndroidAuto",
                "Cancelar subscrição: ${controllerDescription(browser)}, parentId=$parentId",
            )
            return super.onUnsubscribe(session, browser, parentId)
        }

        override fun onSearch(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            params: MediaLibraryService.LibraryParams?,
        ): ListenableFuture<LibraryResult<Void>> {
            DiagnosticLogger.log(
                this@MusicPlaybackService,
                "AndroidAuto",
                "Pesquisar biblioteca: ${controllerDescription(browser)}, " +
                    "query=${query.take(80)}, params=${params?.extras?.keySet()?.joinToString(",")}",
            )
            return super.onSearch(session, browser, query, params)
        }

        override fun onGetSearchResult(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            query: String,
            page: Int,
            pageSize: Int,
            params: MediaLibraryService.LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            DiagnosticLogger.log(
                this@MusicPlaybackService,
                "AndroidAuto",
                "Resultados de pesquisa: ${controllerDescription(browser)}, " +
                    "query=${query.take(80)}, page=$page, pageSize=$pageSize",
            )
            return super.onGetSearchResult(session, browser, query, page, pageSize, params)
        }

        /** Regista a ligação de cada controlador, incluindo o Android Auto. */
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            connectedControllers++
            DiagnosticLogger.log(
                this@MusicPlaybackService,
                "AndroidAuto",
                "Controlador ligado: ${controllerDescription(controller)}, " +
                    "controllers=$connectedControllers",
            )
            return super.onConnect(session, controller)
        }

        /** Regista a desconexão de cada controlador da sessão multimédia. */
        override fun onDisconnected(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ) {
            connectedControllers = (connectedControllers - 1).coerceAtLeast(0)
            DiagnosticLogger.log(
                this@MusicPlaybackService,
                "AndroidAuto",
                "Controlador desligado: ${controllerDescription(controller)}, " +
                    "controllers=$connectedControllers, ${playerStateDescription()}",
            )
            super.onDisconnected(session, controller)
        }

        /** Fornece imediatamente a categoria raiz navegável da biblioteca. */
        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: MediaLibraryService.LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val startNanos = System.nanoTime()
            DiagnosticLogger.log(
                this@MusicPlaybackService,
                "AndroidAuto",
                "Pedido de raiz: ${controllerDescription(browser)}, " +
                    "params=${params?.extras?.keySet()?.joinToString(",") ?: "nenhum"}",
            )
            val result = LibraryResult.ofItem(
                toCategoryItem(ROOT_ID, getString(R.string.app_name)),
                params,
            )
            logLibraryResult("getLibraryRoot", startNanos, result)
            return Futures.immediateFuture(result)
        }

        /** Resolve filhos de categorias, pastas e playlists com paginação. */
        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: MediaLibraryService.LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = asyncResult {
            val startNanos = System.nanoTime()
            DiagnosticLogger.log(
                this@MusicPlaybackService,
                "AndroidAuto",
                "Pedido de conteúdos: ${controllerDescription(browser)}, " +
                    "parentId=$parentId, page=$page, pageSize=$pageSize, " +
                    "params=${params?.extras?.keySet()?.joinToString(",") ?: "nenhum"}",
            )
            val repository = (application as VccMusicApplication).container.musicRepository
            val podcastStore = (application as VccMusicApplication).container.podcastStore
            val podcastRepository = (application as VccMusicApplication).container.podcastRepository
            val root = repository.activeRoot()
            val items = when (parentId) {
                // Teste de compatibilidade Android Auto: raiz mínima.
                // Se esta versão abrir corretamente no carro, adiciona as restantes
                // categorias novamente, uma a uma, para identificar a que causa o bloqueio.
                ROOT_ID -> listOf(
                    toCategoryItem(FOLDERS_ID, getString(R.string.folders)),
                    toCategoryItem(ALL_TRACKS_ID, getString(R.string.all_music)),
                    toCategoryItem(PODCASTS_ID, getString(R.string.podcasts)),
                    toCategoryItem(PLAYLISTS_ID, getString(R.string.playlists)),
                    toCategoryItem(ONLINE_RADIOS_ID, "Rádios Online"),
                )
                FOLDERS_ID -> root?.let {
                    repository.observeFolders(it.id, null).first().map { folder ->
                        toFolderItem(folder.id, folder.name)
                    }
                }.orEmpty()
                ALL_TRACKS_ID -> root?.let {
                    repository.observeAllTracks(it.id).first().map(::toMediaItem)
                }.orEmpty()
                PLAYLISTS_ID -> repository.observePlaylists().first().map {
                    toPlaylistItem(it.id, it.name)
                }
                SHUFFLE_ID -> root?.let {
                    repository.observeAllTracks(it.id).first().shuffled().map(::toMediaItem)
                }.orEmpty()
                PODCASTS_ID -> podcastStore.observeFavorites().first().map(::toPodcastFeedItem)
                ONLINE_RADIOS_ID ->
                    radioRepository
                        .observePortugueseStations()
                        .first()
                        .map(::toRadioItem)
                TRENDING_PODCASTS_ID -> podcastRepository.trending(30, "pt").map(::toPodcastFeedItem)
                RECENT_PODCASTS_ID -> podcastRepository.recent(30, "pt").map(::toPodcastFeedItem)
                else -> when {
                    parentId.startsWith(FOLDER_PREFIX) -> {
                        val folder = repository.folder(parentId.removePrefix(FOLDER_PREFIX).toLongOrNull() ?: -1)
                        if (folder == null) emptyList() else {
                            val children = repository.observeFolders(folder.rootId, folder.id).first()
                                .map { child -> toFolderItem(child.id, child.name) }
                            children + repository.observeDirectTracks(folder.id).first().map(::toMediaItem)
                        }
                    }
                    parentId.startsWith(PLAYLIST_PREFIX) -> {
                        repository.observePlaylistTracks(
                            parentId.removePrefix(PLAYLIST_PREFIX).toLongOrNull() ?: -1,
                        ).first().map(::toMediaItem)
                    }
                    parentId.startsWith(MediaIds.PODCAST_FEED_PREFIX) -> {
                        podcastRepository.episodes(
                            parentId.removePrefix(MediaIds.PODCAST_FEED_PREFIX).toLongOrNull() ?: -1,
                        ).map(::toPodcastEpisodeItem)
                    }
                    else -> emptyList()
                }
            }
            return@asyncResult LibraryResult.ofItemList(page(items, page, pageSize), params).also { result ->
                logLibraryResult(
                    "getChildren parentId=$parentId page=$page pageSize=$pageSize items=${items.size}",
                    startNanos,
                    result,
                )
            }
        }

        /** Resolve um item individual a partir do identificador Media3. */
        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> = asyncResult {
            val startNanos = System.nanoTime()
            DiagnosticLogger.log(
                this@MusicPlaybackService,
                "AndroidAuto",
                "Pedido de item: ${controllerDescription(browser)}, mediaId=$mediaId",
            )
            val repository = (application as VccMusicApplication).container.musicRepository
            val podcastStore = (application as VccMusicApplication).container.podcastStore
            val podcastRepository = (application as VccMusicApplication).container.podcastRepository
            val item = when {
                mediaId == ROOT_ID -> toCategoryItem(ROOT_ID, getString(R.string.app_name))
                mediaId == FOLDERS_ID -> toCategoryItem(FOLDERS_ID, getString(R.string.folders))
                mediaId == ALL_TRACKS_ID -> toCategoryItem(ALL_TRACKS_ID, getString(R.string.all_music))
                mediaId == PLAYLISTS_ID -> toCategoryItem(PLAYLISTS_ID, getString(R.string.playlists))
                mediaId == SHUFFLE_ID -> toCategoryItem(SHUFFLE_ID, getString(R.string.shuffle))
                mediaId == PODCASTS_ID -> toCategoryItem(PODCASTS_ID, getString(R.string.podcasts))
                mediaId == ONLINE_RADIOS_ID -> toCategoryItem(ONLINE_RADIOS_ID, "Rádios Online")
                mediaId == TRENDING_PODCASTS_ID ->
                    toCategoryItem(TRENDING_PODCASTS_ID, getString(R.string.trending_podcasts))
                mediaId == RECENT_PODCASTS_ID ->
                    toCategoryItem(RECENT_PODCASTS_ID, getString(R.string.recent_podcasts))
                mediaId.startsWith(RADIO_PREFIX) -> {
                    val radioValue = mediaId.removePrefix(RADIO_PREFIX)
                    radioRepository
                        .observePortugueseStations()
                        .first()
                        .firstOrNull { station ->
                            station.id == radioValue || station.streamUrl == radioValue
                        }
                        ?.let(::toRadioItem)
                }
                mediaId.startsWith(MediaIds.PODCAST_FEED_PREFIX) -> podcastStore.observeFavorites().first()
                    .firstOrNull {
                        MediaIds.podcastFeed(it.feedId) == mediaId
                    }?.let(::toPodcastFeedItem)
                    ?: (
                        podcastRepository.trending(30, "pt") + podcastRepository.recent(30, "pt")
                    ).firstOrNull { MediaIds.podcastFeed(it.id) == mediaId }?.let(::toPodcastFeedItem)
                    mediaId.startsWith(MediaIds.PODCAST_EPISODE_PREFIX) -> {
                        val episodeId =
                            mediaId.removePrefix(MediaIds.PODCAST_EPISODE_PREFIX)
                                .toLongOrNull()
                        episodeId
                            ?.let { findPodcastEpisodeById(it) }
                            ?.let(::toPodcastEpisodeItem)
                    }
                mediaId.startsWith(FOLDER_PREFIX) -> repository.folder(
                    mediaId.removePrefix(FOLDER_PREFIX).toLongOrNull() ?: -1,
                )?.let { toFolderItem(it.id, it.name) }
                mediaId.startsWith(PLAYLIST_PREFIX) -> repository.observePlaylists().first()
                    .firstOrNull { it.id == mediaId.removePrefix(PLAYLIST_PREFIX).toLongOrNull() }
                    ?.let { toPlaylistItem(it.id, it.name) }
                mediaId.startsWith(MediaIds.TRACK_PREFIX) -> repository.track(
                    mediaId.removePrefix(MediaIds.TRACK_PREFIX).toLongOrNull() ?: -1,
                )?.let(::toMediaItem)
                else -> null
            }
            return@asyncResult (item?.let { LibraryResult.ofItem(it, null) }
                ?: LibraryResult.ofError<MediaItem>(SessionError.ERROR_BAD_VALUE)
                ).also { result -> logLibraryResult("getItem mediaId=$mediaId", startNanos, result) }
        }

        /** Executa um callback suspenso fora da thread do serviço e expõe um futuro. */
        private fun <T> asyncResult(
            block: suspend () -> LibraryResult<T>,
        ): ListenableFuture<LibraryResult<T>> {
            val future = SettableFuture.create<LibraryResult<T>>()
            val startNanos = System.nanoTime()
            serviceScope.launch(Dispatchers.IO) {
                try {
                    future.set(block())
                } catch (error: Exception) {
                    val durationMs = (System.nanoTime() - startNanos) / 1_000_000
                    DiagnosticLogger.log(
                        this@MusicPlaybackService,
                        "MediaLibrary",
                        "Callback falhou: duraçãoMs=$durationMs",
                        error,
                    )
                    future.setException(error)
                }
            }
            return future
        }

        /** Devolve a página pedida sem ultrapassar os limites da lista. */
        private fun page(items: List<MediaItem>, page: Int, pageSize: Int): List<MediaItem> {
            if (page < 0 || pageSize <= 0) return emptyList()
            val start = (page.toLong() * pageSize).coerceAtMost(items.size.toLong()).toInt()
            val end = (start + pageSize).coerceAtMost(items.size)
            return items.subList(start, end)
        }
    }

    private suspend fun resolvePlayableMediaItem(requested: MediaItem): MediaItem? {
        val id = requested.mediaId
        val container =
            (application as VccMusicApplication).container
        val repository =
            container.musicRepository
        val podcastRepository =
            container.podcastRepository
        /*
        * Música local
        */
        if (id.startsWith(MediaIds.TRACK_PREFIX)) {
            val trackId =
                id.removePrefix(MediaIds.TRACK_PREFIX)
                    .toLongOrNull()
                    ?: return null
            return repository.track(trackId)
                ?.let(::toMediaItem)
        }
        /*
        * Episódio de podcast
        */
        if (id.startsWith(MediaIds.PODCAST_EPISODE_PREFIX)) {
            val episodeId =
                id.removePrefix(MediaIds.PODCAST_EPISODE_PREFIX)
                    .toLongOrNull()
                    ?: return null
            return findPodcastEpisodeById(episodeId)
                ?.let(::toPodcastEpisodeItem)
        }
        if (id.startsWith(RADIO_PREFIX)) {
            val radioValue = id.removePrefix(RADIO_PREFIX)
            val station = radioRepository
                .observePortugueseStations()
                .first()
                .firstOrNull { candidate ->
                    candidate.id == radioValue || candidate.streamUrl == radioValue
                }
            return station?.let(::toRadioItem)
        }
        /*
        * Caso o Android Auto ainda tenha enviado
        * o MediaItem completo com URI.
        */
        if (requested.localConfiguration?.uri != null) {
            return requested
        }
        return null
    }

    private fun toRadioItem(
        station: RadioBrowserStation,
    ): MediaItem =
        MediaItem.Builder()
            .setMediaId("$RADIO_PREFIX${station.id}")
            .setUri(station.streamUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name.withoutParentheticalText())
                    .setArtist(station.tags.ifBlank { "Rádio Online" })
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build(),
            )
            .build()

    private suspend fun findPodcastEpisodeById(episodeId: Long): PodcastEpisode?
    {
        val container =
            (application as VccMusicApplication).container
        val podcastStore = container.podcastStore
        val podcastRepository = container.podcastRepository
        val checkedFeedIds = mutableSetOf<Long>()
        // Favoritos
        val favorites = podcastStore.observeFavorites().first()
        for (feed in favorites) {
            if (!checkedFeedIds.add(feed.feedId)) continue
            val episode =
                podcastRepository
                    .episodes(feed.feedId)
                    .firstOrNull { it.id == episodeId }
            if (episode != null) {
                return episode
            }
        }
        // Trending
        val trending = podcastRepository.trending(30, "pt")
        for (feed in trending) {
            if (!checkedFeedIds.add(feed.id)) continue
            val episode =
                podcastRepository
                    .episodes(feed.id)
                    .firstOrNull { it.id == episodeId }
            if (episode != null) {
                return episode
            }
        }
        // Recentes
        val recent = podcastRepository.recent(30, "pt")
        for (feed in recent) {
            if (!checkedFeedIds.add(feed.id)) continue
            val episode =
                podcastRepository
                    .episodes(feed.id)
                    .firstOrNull { it.id == episodeId }
            if (episode != null) {
                return episode
            }
        }
        return null
    }
    /** Codifica o identificador de uma faixa no formato da sessão. */
    private fun trackId(id: Long) = MediaIds.track(id)

    /** Codifica o identificador de uma pasta no formato da sessão. */
    private fun folderId(id: Long) = "$FOLDER_PREFIX$id"

    /** Codifica o identificador de uma playlist no formato da sessão. */
    private fun playlistId(id: Long) = "$PLAYLIST_PREFIX$id"
    
    private companion object {
        const val ROOT_ID = "root"
        const val FOLDERS_ID = "folders"
        const val ALL_TRACKS_ID = "all_tracks"
        const val PLAYLISTS_ID = "playlists"
        const val SHUFFLE_ID = "shuffle"
        const val PODCASTS_ID = "podcasts"
        const val ONLINE_RADIOS_ID = "online_radios"
        const val TRENDING_PODCASTS_ID = "podcasts_trending"
        const val RECENT_PODCASTS_ID = "podcasts_recent"
        const val FOLDER_PREFIX = "folder:"
        const val PLAYLIST_PREFIX = "playlist:"
        const val RADIO_PREFIX = "radio:"
    }
}