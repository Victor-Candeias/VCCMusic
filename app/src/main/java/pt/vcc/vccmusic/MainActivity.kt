package pt.vcc.vccmusic

import android.os.Bundle
import android.net.Uri
import android.content.Intent
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pt.vcc.vccmusic.data.saf.RootAccess
import pt.vcc.vccmusic.diagnostics.DiagnosticLogger
import pt.vcc.vccmusic.scanner.ReindexScheduler
import pt.vcc.vccmusic.scanner.ScanProgress
import pt.vcc.vccmusic.ui.VccMusicApp
import pt.vcc.vccmusic.ui.theme.VccMusicTheme
import pt.vcc.vccmusic.ui.theme.ThemePreferences

class MainActivity : ComponentActivity() {
    private var reindexJob: Job? = null
    private var reindexing by androidx.compose.runtime.mutableStateOf(false)
    private var reindexProgress by androidx.compose.runtime.mutableStateOf(ScanProgress(0, 0, 0))
    private val diagnosticLogDestination = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val destination = result.data?.data
        if (destination == null) return@registerForActivityResult
        try {
            DiagnosticLogger.writeTo(this, destination)
            Toast.makeText(
                this,
                getString(R.string.diagnostic_log_saved, destination.lastPathSegment ?: destination.toString()),
                Toast.LENGTH_LONG,
            ).show()
        } catch (error: java.io.IOException) {
            DiagnosticLogger.log(this, "UI", "Falha ao exportar log", error)
            Toast.makeText(this, R.string.diagnostic_log_save_failed, Toast.LENGTH_LONG).show()
        }
    }

    private val rootPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri == null) return@registerForActivityResult

        DiagnosticLogger.log(this, "UI", "Pasta de música selecionada: ${uri.scheme}")
        reindexing = true
        reindexProgress = ScanProgress(0, 0, 0)
        lifecycleScope.launch {
            val result = appInstance.container.safRootRepository.accept(uri)
            if (result.isSuccess) {
                val acceptedUri = result.getOrThrow()
                appInstance.container.musicRepository.replaceRoot(
                    acceptedUri.toString(),
                    acceptedUri.lastPathSegment ?: getString(R.string.app_name),
                )
                reindexJob?.cancel()
                reindexCurrentRoot(showFeedback = false, showProgress = true)
            } else {
                val error = result.exceptionOrNull()
                    ?: IllegalStateException("Falha desconhecida ao selecionar a raiz.")
                reindexing = false
                DiagnosticLogger.log(this@MainActivity, "UI", "Falha ao aceitar pasta", error)
                Toast.makeText(
                    this@MainActivity,
                    error.message ?: getString(R.string.root_selection_failed),
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    /** Inicializa a interface, preferências de tema e o trabalho de reindexação. */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DiagnosticLogger.log(this, "App", "Aplicação aberta")
        enableEdgeToEdge()
        setContent {
            val darkTheme by ThemePreferences.observeDarkTheme(this@MainActivity)
                .collectAsStateWithLifecycle(initialValue = false)
            VccMusicTheme(darkTheme = darkTheme) {
                VccMusicApp(
                    musicRepository = appInstance.container.musicRepository,
                    onPickRoot = { rootPicker.launch(null) },
                    onReindex = { reindexCurrentRoot(showFeedback = true, showProgress = true) },
                    reindexing = reindexing,
                    reindexProgress = reindexProgress,
                    isDarkTheme = darkTheme,
                    onToggleTheme = {
                        lifecycleScope.launch {
                            ThemePreferences.setDarkTheme(this@MainActivity, !darkTheme)
                        }
                    },
                    onExportDiagnosticLog = ::shareDiagnosticLog,
                    onExit = ::finish,
                )
            }
        }
        ReindexScheduler.enqueue(this)
    }

    /** Reindexa a raiz ativa, atualizando progresso e feedback conforme solicitado. */
    private fun reindexCurrentRoot(showFeedback: Boolean, showProgress: Boolean) {
        if (reindexJob?.isActive == true) return
        if (showProgress) {
            reindexing = true
            reindexProgress = ScanProgress(0, 0, 0)
        }
        DiagnosticLogger.log(this, "Reindex", "Reindexação iniciada")
        reindexJob = lifecycleScope.launch {
            val rootUri = appInstance.container.safRootRepository.loadActiveRoot()
            if (rootUri == null) {
                reindexing = false
                if (showFeedback) showReindexMessage(getString(R.string.no_music_root))
                return@launch
            }
            if (appInstance.container.safRootRepository.access(rootUri) != RootAccess.Available) {
                reindexing = false
                if (showFeedback) showReindexMessage(getString(R.string.root_access_lost))
                return@launch
            }
            val root = appInstance.container.musicRepository.activeRoot()
            if (root == null) {
                reindexing = false
                if (showFeedback) showReindexMessage(getString(R.string.no_music_root))
                return@launch
            }
            val result = try {
                appInstance.container.musicScanner.scan(rootUri, root.id) {
                    reindexProgress = it
                    DiagnosticLogger.log(
                        this@MainActivity,
                        "Reindex",
                        "Progresso: pastas=${it.folders}, faixas=${it.tracks}, erros=${it.errors}",
                    )
                }
            } catch (error: Exception) {
                reindexing = false
                DiagnosticLogger.log(this@MainActivity, "Reindex", "Reindexação falhou", error)
                if (showFeedback) {
                    showReindexMessage(
                        error.message ?: getString(R.string.reindex_failed),
                    )
                }
                return@launch
            }
            reindexing = false
            DiagnosticLogger.log(
                this@MainActivity,
                "Reindex",
                "Reindexação terminada: completa=${result.completed}, pastas=${result.folders}, faixas=${result.tracks}, erros=${result.errors.size}",
            )
            if (showFeedback) {
                val message = if (result.completed) {
                    resources.getQuantityString(
                        R.plurals.reindex_completed,
                        result.tracks,
                        result.tracks,
                    )
                } else {
                    getString(R.string.reindex_incomplete)
                }
                showReindexMessage(message)
            }
        }
    }

    /** Apresenta ao utilizador o resultado textual de uma operação de reindexação. */
    private fun showReindexMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    /** Abre o seletor SAF para exportar o ficheiro de diagnóstico. */
    private fun shareDiagnosticLog() {
        DiagnosticLogger.log(this, "UI", "Log exportado pelo utilizador")
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, DiagnosticLogger.suggestedExportFileName())
            putExtra(
                DocumentsContract.EXTRA_INITIAL_URI,
                Uri.parse("content://com.android.providers.downloads.documents/root/downloads"),
            )
        }
        diagnosticLogDestination.launch(intent)
    }

    /** Obtém a instância da aplicação para aceder ao contentor de dependências. */
    private val appInstance: VccMusicApplication
        get() = super.getApplication() as VccMusicApplication
}
