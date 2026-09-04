package pt.vcc.vccmusic.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

interface PlaybackController {
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun skipToPrevious()
    fun skipToNext()
    fun setShuffleEnabled(enabled: Boolean)
    fun setRepeatMode(mode: Int)
    fun release()
}

class Media3PlaybackController(
    context: Context,
) : PlaybackController {
    private val controllerFuture: ListenableFuture<MediaController> =
        MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, MusicPlaybackService::class.java)),
        ).buildAsync()

    override fun play() {
        controllerFuture.get().play()
    }

    override fun pause() {
        controllerFuture.get().pause()
    }

    override fun seekTo(positionMs: Long) {
        controllerFuture.get().seekTo(positionMs)
    }

    override fun skipToPrevious() {
        controllerFuture.get().seekToPreviousMediaItem()
    }

    override fun skipToNext() {
        controllerFuture.get().seekToNextMediaItem()
    }

    override fun setShuffleEnabled(enabled: Boolean) {
        controllerFuture.get().shuffleModeEnabled = enabled
    }

    override fun setRepeatMode(mode: Int) {
        controllerFuture.get().repeatMode = mode
    }

    fun setQueue(items: List<MediaItem>) {
        controllerFuture.get().setMediaItems(items)
    }

    override fun release() {
        if (controllerFuture.isDone) controllerFuture.get().release()
    }
}
