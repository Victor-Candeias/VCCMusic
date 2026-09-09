package pt.vcc.vccmusic.ui.screen

import android.content.ComponentName
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.C
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pt.vcc.vccmusic.data.local.TrackEntity
import pt.vcc.vccmusic.playback.MusicPlaybackService
import pt.vcc.vccmusic.playback.QueueBuilder
import pt.vcc.vccmusic.playback.QueueRequest
import pt.vcc.vccmusic.playback.QueueSource
import pt.vcc.vccmusic.playback.SpectrumAnalyzer

data class PlaybackUiState(
    val mediaId: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val artworkData: ByteArray? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val spectrum: List<Float> = List(24) { 0f },
)

class PlaybackViewModel(context: Context) : ViewModel() {
    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()
    private val controllerFuture = MediaController.Builder(
        context,
        SessionToken(context, ComponentName(context, MusicPlaybackService::class.java)),
    ).buildAsync()
    private var controller: MediaController? = null
    private var ticker: Job? = null
    private var pendingRadio: Pair<String, String>? = null
    private var pendingTracks: Quadruple<List<TrackEntity>, Long?, QueueSource, Boolean?>? = null

    init {
        controllerFuture.addListener(
            {
                controller = controllerFuture.get().also {
                    it.addListener(listener)
                    updateState(it)
                        pendingRadio?.let { (name, streamUrl) ->
                            pendingRadio = null
                            playRadioOnController(it, name, streamUrl)
                        }
                        pendingTracks?.let { (tracks, selectedId, source, shuffle) ->
                            pendingTracks = null
                            playTracks(tracks, selectedId, source, shuffle)
                        }
                    ticker = viewModelScope.launch {
                        while (isActive) {
                            updateState(it)
                            delay(500)
                        }
                    }
                    viewModelScope.launch {
                        SpectrumAnalyzer.spectrum.collect { spectrum ->
                            _state.update { it.copy(spectrum = spectrum) }
                        }
                    }
                }
            },
            MoreExecutors.directExecutor(),
        )
    }

    fun playTracks(
        tracks: List<TrackEntity>,
        selectedId: Long? = null,
        source: QueueSource = QueueSource.SELECTION,
        shuffle: Boolean? = null,
    ) {
        val queue = QueueBuilder.build(QueueRequest(source, tracks, selectedId))
        if (queue.isEmpty()) return
        val selectedTrack = queue.firstOrNull { it.id == selectedId } ?: queue.first()
        _state.update {
            it.copy(
                mediaId = selectedTrack.id.toString(),
                title = selectedTrack.title,
                artist = selectedTrack.artist,
                artworkData = selectedTrack.artwork,
            )
        }
        val currentController = controller
        if (currentController == null) {
            pendingTracks = Quadruple(tracks, selectedId, source, shuffle)
            return
        }
        currentController.setMediaItems(queue.map(::toMediaItem))
        shuffle?.let { currentController.shuffleModeEnabled = it }
        val selectedIndex = selectedId?.let { id -> queue.indexOfFirst { it.id == id } } ?: 0
        currentController.prepare()
        currentController.seekTo(selectedIndex.coerceAtLeast(0), 0)
        currentController.play()
    }

    private data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
    )

    fun playRadio(name: String, streamUrl: String, artworkPath: String? = null) {
        val mediaId = "radio:$streamUrl"
        viewModelScope.launch {
            val artworkData = artworkPath?.let {
                withContext(Dispatchers.IO) { java.io.File(it).takeIf { file -> file.exists() }?.readBytes() }
            }
            _state.update { state ->
                if (state.mediaId == mediaId || artworkData != null) {
                    state.copy(mediaId = mediaId, title = name, artist = "Rádio Online", artworkData = artworkData)
                } else {
                    state.copy(mediaId = mediaId, title = name, artist = "Rádio Online", artworkData = null)
                }
            }
        }
        val currentController = controller
        if (currentController == null) {
            pendingRadio = name to streamUrl
            return
        }
        playRadioOnController(currentController, name, streamUrl)
    }

    private fun playRadioOnController(
        currentController: MediaController,
        name: String,
        streamUrl: String,
    ) {
        currentController.setMediaItem(
            MediaItem.Builder()
                .setMediaId("radio:$streamUrl")
                .setUri(streamUrl)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(name)
                        .setArtist("Rádio Online")
                        .build(),
                )
                .build(),
        )
        currentController.prepare()
        currentController.play()
    }

    fun playPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
    }

    fun skipPrevious() {
        controller?.seekToPreviousMediaItem()
    }

    fun skipNext() {
        controller?.seekToNextMediaItem()
    }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            updateState(player)
        }
    }

    private fun updateState(player: Player) {
        val metadata = player.mediaMetadata
        val mediaId = player.currentMediaItem?.mediaId
        _state.value = PlaybackUiState(
            mediaId = mediaId,
            title = metadata.title?.toString(),
            artist = metadata.artist?.toString(),
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = player.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0) ?: 0,
            shuffleEnabled = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
            spectrum = _state.value.spectrum,
            artworkData = _state.value.artworkData
                ?.takeIf { _state.value.mediaId == mediaId }
                ?: metadata.artworkData,
        )
    }

    private fun toMediaItem(track: TrackEntity): MediaItem =
        MediaItem.Builder()
            .setMediaId(track.id.toString())
            .setUri(track.uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .build(),
            )
            .build()

    override fun onCleared() {
        ticker?.cancel()
        controller?.removeListener(listener)
        if (controllerFuture.isDone) controllerFuture.get().release()
    }

}
