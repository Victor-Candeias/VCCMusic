package pt.vcc.vccmusic.ui.screen

import android.content.ComponentName
import android.content.Context
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pt.vcc.vccmusic.data.local.TrackEntity
import pt.vcc.vccmusic.playback.MusicPlaybackService
import pt.vcc.vccmusic.playback.QueueBuilder
import pt.vcc.vccmusic.playback.QueueRequest
import pt.vcc.vccmusic.playback.QueueSource

data class PlaybackUiState(
    val title: String? = null,
    val artist: String? = null,
    val artworkData: ByteArray? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
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

    init {
        controllerFuture.addListener(
            {
                controller = controllerFuture.get().also {
                    it.addListener(listener)
                    updateState(it)
                    ticker = viewModelScope.launch {
                        while (isActive) {
                            updateState(it)
                            delay(500)
                        }
                    }
                }
            },
            MoreExecutors.directExecutor(),
        )
    }

    fun playTracks(tracks: List<TrackEntity>, selectedId: Long? = null, source: QueueSource = QueueSource.SELECTION) {
        val currentController = controller ?: return
        val queue = QueueBuilder.build(QueueRequest(source, tracks, selectedId))
        if (queue.isEmpty()) return
        currentController.setMediaItems(queue.map(::toMediaItem))
        val selectedIndex = selectedId?.let { id -> queue.indexOfFirst { it.id == id } } ?: 0
        currentController.prepare()
        currentController.seekTo(selectedIndex.coerceAtLeast(0), 0)
        currentController.play()
    }

    fun playRadio(name: String, streamUrl: String) {
        val currentController = controller ?: return
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
        _state.value = PlaybackUiState(
            title = metadata.title?.toString(),
            artist = metadata.artist?.toString(),
            artworkData = metadata.artworkData,
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0),
            durationMs = player.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0) ?: 0,
            shuffleEnabled = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
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
                    .setArtworkData(track.artwork)
                    .build(),
            )
            .build()

    override fun onCleared() {
        ticker?.cancel()
        controller?.removeListener(listener)
        if (controllerFuture.isDone) controllerFuture.get().release()
    }
}
