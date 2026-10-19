package pt.vcc.vccmusic.ui.screen

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import pt.vcc.vccmusic.R

internal const val LIST_ROW_TEXT_ALPHA = 0.92f
internal const val LIST_ROW_ACTION_ALPHA = 0.76f

@Composable
/** Linha de lista com o aspeto da biblioteca: fundo translúcido, texto compacto e divisor. */
internal fun LibraryListRow(
    title: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    highlighted: Boolean = false,
    leading: @Composable () -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val textColor = if (highlighted) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onBackground.copy(alpha = LIST_ROW_TEXT_ALPHA)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color.Black.copy(alpha = if (highlighted) 0.32f else 0.16f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = 14.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp, end = 8.dp),
            ) {
                Text(
                    text = title,
                    color = textColor,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                )
                subtitle?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.64f),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
            }
            trailing()
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.18f))
    }
}

@Composable
/** Ícone à esquerda de uma linha de lista, com a opacidade usada nas pastas da biblioteca. */
internal fun ListRowIcon(icon: ImageVector, contentDescription: String? = null) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.88f),
    )
}

@Composable
/** Artwork de 40 dp de uma faixa, com o ícone da aplicação quando não existe capa. */
internal fun ListRowArtwork(trackId: Long, artwork: ByteArray?, contentDescription: String?) {
    val bitmap = remember(trackId, artwork) {
        artwork?.let { bytes -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }
    }
    val imageModifier = Modifier
        .size(40.dp)
        .clip(RoundedCornerShape(4.dp))
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = imageModifier,
        )
    } else {
        Image(
            painter = painterResource(R.mipmap.ic_launcher),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = imageModifier,
        )
    }
}
