package pt.vcc.vccmusic.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

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

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
)

private val AppTypography = Typography().run {
    copy(
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
/** Aplica o esquema de cores e o conteúdo Compose da aplicação. */
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
        shapes = AppShapes,
        typography = AppTypography,
        content = content,
    )
}

/** Cria o gradiente de fundo correspondente ao tema selecionado. */
fun appBackgroundBrush(darkTheme: Boolean): Brush = Brush.verticalGradient(
    colors = if (darkTheme) {
        listOf(DarkBackgroundTop, DarkBackgroundBottom)
    } else {
        listOf(LightBackgroundTop, LightBackgroundBottom)
    },
)

fun appCardBrush(start: Color, end: Color): Brush =
    Brush.verticalGradient(listOf(start, end))

@Composable
fun AppGradientCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp),
    start: Color,
    end: Color,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .shadow(3.dp, shape)
            .clip(shape)
            .background(appCardBrush(start, end), shape)
            .clickable(onClick = onClick),
        content = content,
    )
}

val MainMenuCardStart = Color(0xFFD45DCC)
val MainMenuCardEnd = Color(0xFF74156E)
val LibraryCardStart = Color(0xFF4FC3F1)
val LibraryCardEnd = Color(0xFF087CA8)
val PlaylistCardStart = Color(0xFF45D45A)
val PlaylistCardEnd = Color(0xFF0A541B)
val FoldersCardStart = Color(0xFFF89A70)
val FoldersCardEnd = Color(0xFFB84C0F)
val RadioCardStart = Color(0xFF48AED8)
val RadioCardEnd = Color(0xFF0D4F6C)
val SettingsDarkCardStart = Color(0xFFFFFFFF)
val SettingsDarkCardEnd = Color(0xFFE8E8E8)
val SettingsLightCardStart = Color(0xFF303030)
val SettingsLightCardEnd = Color(0xFF000000)
