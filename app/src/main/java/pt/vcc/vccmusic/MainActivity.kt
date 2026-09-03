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
            appInstance.container.safRootRepository.accept(uri)
                .onFailure { error ->
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
