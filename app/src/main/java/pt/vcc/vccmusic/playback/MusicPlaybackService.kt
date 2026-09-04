package pt.vcc.vccmusic.playback

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import pt.vcc.vccmusic.VccMusicApplication
import pt.vcc.vccmusic.data.local.TrackEntity

class MusicPlaybackService : MediaLibraryService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var player: ExoPlayer
    private var mediaLibrarySession: MediaLibrarySession? = null

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this)
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
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    if (player.hasNextMediaItem()) player.seekToNextMediaItem() else player.stop()
                }
            },
        )
        mediaLibrarySession = MediaLibrarySession.Builder(
            this,
            player,
            LibraryCallback(),
        ).build()
        loadActiveLibrary()
    }

    private fun loadActiveLibrary() {
        serviceScope.launch(Dispatchers.IO) {
            val container = (application as VccMusicApplication).container
            val root = container.musicRepository.activeRoot() ?: return@launch
            val tracks = container.musicRepository.observeAllTracks(root.id)
            tracks.collect { entities ->
                val items = entities.map(::toMediaItem)
                launch(Dispatchers.Main) {
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

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
        mediaLibrarySession

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.isPlaying) stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        mediaLibrarySession?.release()
        player.release()
        mediaLibrarySession = null
        super.onDestroy()
    }

    private class LibraryCallback : MediaLibrarySession.Callback
}
