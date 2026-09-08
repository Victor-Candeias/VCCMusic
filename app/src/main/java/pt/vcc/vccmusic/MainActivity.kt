package pt.vcc.vccmusic

import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pt.vcc.vccmusic.data.saf.RootAccess
import pt.vcc.vccmusic.scanner.ReindexScheduler
import pt.vcc.vccmusic.ui.VccMusicApp
import pt.vcc.vccmusic.ui.theme.VccMusicTheme
import pt.vcc.vccmusic.ui.theme.ThemePreferences

class MainActivity : ComponentActivity() {
    private var reindexJob: Job? = null

    private val rootPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri == null) return@registerForActivityResult

        lifecycleScope.launch {
            val result = appInstance.container.safRootRepository.accept(uri)
            if (result.isSuccess) {
                val acceptedUri = result.getOrThrow()
                appInstance.container.musicRepository.replaceRoot(
                    acceptedUri.toString(),
                    acceptedUri.lastPathSegment ?: getString(R.string.app_name),
                )
                reindexJob?.cancel()
                reindexCurrentRoot(showFeedback = false)
            } else {
                val error = result.exceptionOrNull()
                    ?: IllegalStateException("Falha desconhecida ao selecionar a raiz.")
                Toast.makeText(
                    this@MainActivity,
                    error.message ?: getString(R.string.root_selection_failed),
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val darkTheme by ThemePreferences.observeDarkTheme(this@MainActivity)
                .collectAsStateWithLifecycle(initialValue = false)
            VccMusicTheme(darkTheme = darkTheme) {
                VccMusicApp(
                    musicRepository = appInstance.container.musicRepository,
                    onPickRoot = { rootPicker.launch(null) },
                    onReindex = { reindexCurrentRoot(showFeedback = true) },
                    isDarkTheme = darkTheme,
                    onToggleTheme = {
                        lifecycleScope.launch {
                            ThemePreferences.setDarkTheme(this@MainActivity, !darkTheme)
                        }
                    },
                    onExit = ::finish,
                )
            }
        }
        ReindexScheduler.enqueue(this)
    }

    private fun reindexCurrentRoot(showFeedback: Boolean) {
        if (reindexJob?.isActive == true) return
        reindexJob = lifecycleScope.launch {
            val rootUri = appInstance.container.safRootRepository.loadActiveRoot()
            if (rootUri == null) {
                if (showFeedback) showReindexMessage(getString(R.string.no_music_root))
                return@launch
            }
            if (appInstance.container.safRootRepository.access(rootUri) != RootAccess.Available) {
                if (showFeedback) showReindexMessage(getString(R.string.root_access_lost))
                return@launch
            }
            val root = appInstance.container.musicRepository.activeRoot()
            if (root == null) {
                if (showFeedback) showReindexMessage(getString(R.string.no_music_root))
                return@launch
            }
            val result = try {
                appInstance.container.musicScanner.scan(rootUri, root.id)
            } catch (error: Exception) {
                if (showFeedback) {
                    showReindexMessage(
                        error.message ?: getString(R.string.reindex_failed),
                    )
                }
                return@launch
            }
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

    private fun showReindexMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private val appInstance: VccMusicApplication
        get() = super.getApplication() as VccMusicApplication
}
