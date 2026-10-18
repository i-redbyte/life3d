package ru.redbyte.life3d.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import ru.redbyte.life3d.domain.CellPalette

object LifeColors {
    val Background = Color(0xFF060B14)
    val Panel = Color(0xCC0C1626)
    val PanelBorder = Color(0x332E5A7A)
    val Text = Color(0xFFE6F1FF)
    val TextDim = Color(0xFF8FA5BF)
}

val CellPalette.color: Color get() = Color(red, green, blue)

@Composable
fun LifeTheme(accent: Color, content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = accent,
        onPrimary = LifeColors.Background,
        secondary = accent,
        background = LifeColors.Background,
        surface = LifeColors.Panel,
        onBackground = LifeColors.Text,
        onSurface = LifeColors.Text,
        onSurfaceVariant = LifeColors.TextDim,
        surfaceVariant = LifeColors.Panel,
        outline = LifeColors.PanelBorder,
    )
    MaterialTheme(colorScheme = scheme, content = content)
}
