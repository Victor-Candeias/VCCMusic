package pt.vcc.vccmusic.playback

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.sqrt

object SpectrumAnalyzer {
    private val _spectrum = MutableStateFlow(List(24) { 0f })
    val spectrum: StateFlow<List<Float>> = _spectrum
    private val analyzerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Any()
    private var pending: PendingAnalysis? = null
    private var processing = false

    internal fun publish(samples: ShortArray, sampleRate: Int) {
        synchronized(lock) {
            pending = PendingAnalysis(samples.copyOf(), sampleRate)
            if (processing) return
            processing = true
        }
        analyzerScope.launch {
            while (true) {
                val analysis = synchronized(lock) {
                    pending.also {
                        pending = null
                        if (it == null) processing = false
                    }
                } ?: return@launch
                _spectrum.value = calculate(analysis.samples, analysis.sampleRate)
            }
        }
    }

    private fun calculate(samples: ShortArray, sampleRate: Int): List<Float> {
        val values = List(24) { band ->
            val frequency = 60.0 * (16000.0 / 60.0).pow(band / 23.0)
            val coefficient = 2.0 * cos(2.0 * Math.PI * frequency / sampleRate)
            var previous = 0.0
            var previousPrevious = 0.0
            samples.forEach { sample ->
                val current = coefficient * previous - previousPrevious + sample
                previousPrevious = previous
                previous = current
            }
            val magnitude = sqrt(
                previous * previous +
                    previousPrevious * previousPrevious -
                    previous * previousPrevious * coefficient,
            )
            (magnitude / (samples.size * 32768.0) * 8.0)
                .toFloat()
                .coerceIn(0f, 1f)
        }
        return values
    }

    private fun Double.pow(exponent: Double): Double = Math.pow(this, exponent)

    private data class PendingAnalysis(
        val samples: ShortArray,
        val sampleRate: Int,
    )
}

class SpectrumAudioProcessor : BaseAudioProcessor() {
    private val samples = ShortArray(WINDOW_SIZE)
    private var sampleCount = 0

    override fun onConfigure(
        inputAudioFormat: AudioProcessor.AudioFormat,
    ): AudioProcessor.AudioFormat {
        return inputAudioFormat
    }

    override fun isActive(): Boolean = true

    override fun queueInput(input: ByteBuffer) {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
            val output = replaceOutputBuffer(input.remaining())
            output.put(input)
            output.flip()
            return
        }
        val bytes = input.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        val channels = inputAudioFormat.channelCount
        while (bytes.remaining() >= channels * 2) {
            var mixed = 0
            repeat(channels) { mixed += bytes.short.toInt() }
            samples[sampleCount++] = (mixed / channels).toShort()
            if (sampleCount == WINDOW_SIZE) {
                SpectrumAnalyzer.publish(samples, inputAudioFormat.sampleRate)
                sampleCount = 0
            }
        }

        val output = replaceOutputBuffer(input.remaining())
        output.put(input)
        output.flip()
    }

    override fun onFlush() {
        sampleCount = 0
    }

    override fun onReset() {
        sampleCount = 0
    }

    private companion object {
        const val WINDOW_SIZE = 2048
    }
}

fun createSpectrumAudioProcessor(): AudioProcessor =
    TeeAudioProcessor(
        object : TeeAudioProcessor.AudioBufferSink {
            private var sampleRate = 0
            private var channelCount = 0
            private val samples = ShortArray(2048)
            private var sampleCount = 0

            override fun flush(sampleRate: Int, channelCount: Int, encoding: Int) {
                this.sampleRate = sampleRate
                this.channelCount = channelCount
                sampleCount = 0
            }

            override fun handleBuffer(buffer: ByteBuffer) {
                if (channelCount <= 0) return
                val input = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
                while (input.remaining() >= channelCount * 2) {
                    var mixed = 0
                    repeat(channelCount) { mixed += input.short.toInt() }
                    samples[sampleCount++] = (mixed / channelCount).toShort()
                    if (sampleCount == samples.size) {
                        SpectrumAnalyzer.publish(samples, sampleRate)
                        sampleCount = 0
                    }
                }
            }
        },
    )

class SpectrumRenderersFactory(context: Context) : DefaultRenderersFactory(context) {
    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioOutputPlaybackParameters: Boolean,
    ): AudioSink =
        DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioOutputPlaybackParameters(enableAudioOutputPlaybackParameters)
            .setAudioProcessors(arrayOf(createSpectrumAudioProcessor()))
            .build()
}
