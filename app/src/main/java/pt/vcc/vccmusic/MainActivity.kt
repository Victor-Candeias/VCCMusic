package pt.vcc.vccmusic

import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pt.vcc.vccmusic.ui.VccMusicApp
import pt.vcc.vccmusic.ui.theme.VccMusicTheme

class MainActivity : ComponentActivity() {
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
                val root = appInstance.container.musicRepository.activeRoot()
                if (root != null) {
                    appInstance.container.musicScanner.scan(acceptedUri, root.id)
                }
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
            VccMusicTheme {
                VccMusicApp(onPickRoot = { rootPicker.launch(null) })
            }
        }
    }

    private val appInstance: VccMusicApplication
        get() = super.getApplication() as VccMusicApplication
}
