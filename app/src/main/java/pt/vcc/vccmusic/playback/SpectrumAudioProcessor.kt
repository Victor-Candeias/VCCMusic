package pt.vcc.vccmusic.playback

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.sqrt

object SpectrumAnalyzer {

    private val _spectrum =
        MutableStateFlow(List(24) { 0f })

    val spectrum: StateFlow<List<Float>> =
        _spectrum

    private val analyzerScope =
        CoroutineScope(
            SupervisorJob() + Dispatchers.Default
        )

    private val lock = Any()

    private var pending: PendingAnalysis? = null

    private var processing = false

    /**
     * Publica uma janela de amostras, descartando
     * trabalho pendente obsoleto.
     */
    internal fun publish(
        samples: ShortArray,
        sampleRate: Int,
    ) {

        synchronized(lock) {

            pending =
                PendingAnalysis(
                    samples.copyOf(),
                    sampleRate,
                )

            if (processing) {
                return
            }

            processing = true
        }

        analyzerScope.launch {

            while (true) {

                val analysis =
                    synchronized(lock) {

                        pending.also {

                            pending = null

                            if (it == null) {
                                processing = false
                            }
                        }

                    } ?: return@launch

                _spectrum.value =
                    calculate(
                        analysis.samples,
                        analysis.sampleRate,
                    )
            }
        }
    }

    /**
     * Calcula 24 bandas normalizadas a partir
     * das amostras PCM.
     */
    private fun calculate(
        samples: ShortArray,
        sampleRate: Int,
    ): List<Float> {

        if (samples.isEmpty() || sampleRate <= 0) {
            return List(24) { 0f }
        }

        return List(24) { band ->

            val frequency =
                60.0 *
                    (16000.0 / 60.0)
                        .pow(band / 23.0)

            val coefficient =
                2.0 *
                    cos(
                        2.0 *
                            Math.PI *
                            frequency /
                            sampleRate
                    )

            var previous = 0.0
            var previousPrevious = 0.0

            samples.forEach { sample ->

                val current =
                    coefficient * previous -
                        previousPrevious +
                        sample

                previousPrevious = previous
                previous = current
            }

            val magnitude =
                sqrt(
                    previous * previous +
                        previousPrevious * previousPrevious -
                        previous *
                        previousPrevious *
                        coefficient
                )

            (
                magnitude /
                    (samples.size * 32768.0) *
                    8.0
                )
                .toFloat()
                .coerceIn(0f, 1f)
        }
    }

    private fun Double.pow(
        exponent: Double,
    ): Double =
        Math.pow(this, exponent)

    private data class PendingAnalysis(
        val samples: ShortArray,
        val sampleRate: Int,
    )
}

/**
 * Cria o processador que observa o áudio entregue
 * ao AudioSink sem modificar o sinal.
 */
@OptIn(UnstableApi::class)
fun createSpectrumAudioProcessor(): AudioProcessor {
    val windowSize = 2048

    return TeeAudioProcessor(
        object : TeeAudioProcessor.AudioBufferSink {

            private var sampleRate = 0
            private var channelCount = 0
            private var encoding = 0

            private val samples = ShortArray(windowSize)
            private var sampleCount = 0

            override fun flush(
                sampleRate: Int,
                channelCount: Int,
                encoding: Int,
            ) {
                this.sampleRate = sampleRate
                this.channelCount = channelCount
                this.encoding = encoding
                sampleCount = 0
            }

            override fun handleBuffer(
                buffer: ByteBuffer,
            ) {
                if (sampleRate <= 0 || channelCount <= 0) {
                    return
                }

                if (encoding != C.ENCODING_PCM_16BIT) {
                    return
                }

                val input =
                    buffer
                        .duplicate()
                        .order(ByteOrder.LITTLE_ENDIAN)

                while (input.remaining() >= channelCount * 2) {

                    var mixed = 0

                    repeat(channelCount) {
                        mixed += input.short.toInt()
                    }

                    samples[sampleCount++] =
                        (mixed / channelCount).toShort()

                    if (sampleCount == samples.size) {
                        SpectrumAnalyzer.publish(
                            samples,
                            sampleRate,
                        )

                        sampleCount = 0
                    }
                }
            }
        }
    )
}

@OptIn(UnstableApi::class)
class SpectrumRenderersFactory(context: Context,) : DefaultRenderersFactory(context) {

    /**
     * Configura o AudioSink Media3 com
     * o processador do espectro.
     */
    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioOutputPlaybackParameters: Boolean,
    ): AudioSink =

        DefaultAudioSink.Builder(context)

            /*
             * O analisador trabalha em PCM16.
             * Evitar float output garante que o
             * TeeAudioProcessor recebe PCM16.
             */
            .setEnableFloatOutput(false)

            .setEnableAudioOutputPlaybackParameters(
                enableAudioOutputPlaybackParameters
            )

            .setAudioProcessors(
                arrayOf(
                    createSpectrumAudioProcessor()
                )
            )

            .build()
}