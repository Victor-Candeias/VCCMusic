package pt.vcc.vccmusic.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture

interface PlaybackController {
    /** Inicia ou retoma a reprodução atual. */
    fun play()
    /** Pausa a reprodução atual. */
    fun pause()
    /** Procura uma posição em milissegundos na faixa atual. */
    fun seekTo(positionMs: Long)
    /** Avança para a faixa anterior segundo a fila do leitor. */
    fun skipToPrevious()
    /** Avança para a próxima faixa segundo a fila do leitor. */
    fun skipToNext()
    /** Ativa ou desativa a reprodução aleatória. */
    fun setShuffleEnabled(enabled: Boolean)
    /** Define o modo de repetição do leitor Media3. */
    fun setRepeatMode(mode: Int)
    /** Liberta o controlador e a ligação à sessão multimédia. */
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

    /** Encaminha o comando de reprodução para o controlador Media3. */
    override fun play() {
        controllerFuture.get().play()
    }

    /** Encaminha o comando de pausa para o controlador Media3. */
    override fun pause() {
        controllerFuture.get().pause()
    }

    /** Encaminha a procura de posição para o controlador Media3. */
    override fun seekTo(positionMs: Long) {
        controllerFuture.get().seekTo(positionMs)
    }

    /** Solicita a faixa anterior ao controlador Media3. */
    override fun skipToPrevious() {
        controllerFuture.get().seekToPreviousMediaItem()
    }

    /** Solicita a faixa seguinte ao controlador Media3. */
    override fun skipToNext() {
        controllerFuture.get().seekToNextMediaItem()
    }

    /** Atualiza o modo aleatório da sessão ligada. */
    override fun setShuffleEnabled(enabled: Boolean) {
        controllerFuture.get().shuffleModeEnabled = enabled
    }

    /** Atualiza o modo de repetição da sessão ligada. */
    override fun setRepeatMode(mode: Int) {
        controllerFuture.get().repeatMode = mode
    }

    /** Substitui a fila de reprodução da sessão ligada. */
    fun setQueue(items: List<MediaItem>) {
        controllerFuture.get().setMediaItems(items)
    }

    /** Liberta a sessão quando o futuro do controlador já terminou. */
    override fun release() {
        if (controllerFuture.isDone) controllerFuture.get().release()
    }
}
