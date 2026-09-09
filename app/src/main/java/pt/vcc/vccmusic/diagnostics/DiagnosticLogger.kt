package pt.vcc.vccmusic.diagnostics

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DiagnosticLogger {
    private const val LOG_FILE_NAME = "vccmusic.log"
    private const val MAX_LOG_SIZE_BYTES = 2 * 1024 * 1024L
    private val lock = Any()
    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US)
    private val exportFileFormatter = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)

    /** Gera um nome de ficheiro com timestamp para exportar o diagnóstico. */
    fun suggestedExportFileName(): String =
        "vccmusic-diagnostic-${exportFileFormatter.format(Date())}.log"

    /** Acrescenta uma entrada limitada ao ficheiro de diagnóstico sem interromper a aplicação. */
    fun log(context: Context, component: String, message: String, error: Throwable? = null) {
        val line = buildString {
            append(formatter.format(Date()))
            append(" [")
            append(component)
            append("] ")
            append(message)
            error?.let {
                append(" | ")
                append(it::class.java.simpleName)
                append(": ")
                append(it.message ?: "sem mensagem")
            }
            append('\n')
        }
        synchronized(lock) {
            try {
                val file = logFile(context)
                if (file.length() + line.toByteArray().size > MAX_LOG_SIZE_BYTES) {
                    file.writeText("")
                }
                file.appendText(line)
            } catch (_: IOException) {
                // Logging must never interrupt playback or browsing.
            }
        }
    }

    /** Copia o ficheiro de diagnóstico para o destino SAF fornecido. */
    fun writeTo(context: Context, destination: Uri) {
        synchronized(lock) {
            try {
                val source = logFile(context)
                context.contentResolver.openOutputStream(destination)?.use { output ->
                    source.inputStream().use { input -> input.copyTo(output) }
                } ?: throw IOException("Não foi possível abrir o destino do log.")
            } catch (error: IOException) {
                throw error
            }
        }
    }

    /** Resolve o ficheiro privado utilizado para armazenar os diagnósticos. */
    private fun logFile(context: Context): File =
        File(context.filesDir, LOG_FILE_NAME)
}
