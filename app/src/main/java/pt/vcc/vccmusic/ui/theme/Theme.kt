package pt.vcc.vccmusic.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val DarkBackgroundTop = Color(0xFF442E2A)
val DarkBackgroundBottom = Color(0xFF101015)
val LightBackgroundTop = Color(0xFFF8F8F8)
val LightBackgroundBottom = Color(0xFFDCDCDC)

private val DarkColors = darkColorScheme(
    primary = Indigo80,
    secondary = Violet80,
    background = DarkBackgroundBottom,
    surface = DarkBackgroundBottom,
)

private val LightColors = lightColorScheme(
    primary = Indigo40,
    secondary = Violet40,
    background = LightBackgroundTop,
    surface = LightBackgroundTop,
)

@Composable
fun VccMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}

fun appBackgroundBrush(darkTheme: Boolean): Brush = Brush.verticalGradient(
    colors = if (darkTheme) {
        listOf(DarkBackgroundTop, DarkBackgroundBottom)
    } else {
        listOf(LightBackgroundTop, LightBackgroundBottom)
    },
)
