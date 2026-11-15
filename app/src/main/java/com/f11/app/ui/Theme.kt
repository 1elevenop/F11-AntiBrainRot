package com.f11.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Pure-black, low-stimulation palette with three soft gradients for hero cards. */
object F11Palette {
    val Black = Color(0xFF000000)
    val Surface = Color(0xFF0B0B0D)
    val Tile = Color(0xFF151518)
    val TileHigh = Color(0xFF1E1E22)
    val Hairline = Color(0x22FFFFFF)
    val Text = Color(0xFFF5F5F7)
    val TextDim = Color(0xFF8A8A93)
    val TextFaint = Color(0xFF4A4A52)
    val Violet = Color(0xFF8E7BFF)
    val Mint = Color(0xFF5BE3A5)
    val Ember = Color(0xFFFF6A3D)
    val Lime = Color(0xFFD7F45B)

    val Magenta = Brush.verticalGradient(listOf(Color(0xFFD65BC9), Color(0xFF6A2A7E), Color(0xFF1C0B24)))
    val Indigo = Brush.verticalGradient(listOf(Color(0xFF3F46E0), Color(0xFF1A1E6B), Color(0xFF0A0B26)))
    val Sunset = Brush.verticalGradient(listOf(Color(0xFFFF7A45), Color(0xFFB2341F), Color(0xFF2A0D08)))
    val Graphite = Brush.verticalGradient(listOf(Color(0xFF2A2A2F), Color(0xFF121214)))
}

private val F11Colors = darkColorScheme(
    primary = F11Palette.Violet,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF2A2250),
    onPrimaryContainer = F11Palette.Text,
    secondary = F11Palette.Mint,
    onSecondary = Color.Black,
    error = F11Palette.Ember,
    onError = Color.Black,
    background = F11Palette.Black,
    onBackground = F11Palette.Text,
    surface = F11Palette.Black,
    onSurface = F11Palette.Text,
    surfaceVariant = F11Palette.TileHigh,
    onSurfaceVariant = F11Palette.TextDim,
    surfaceContainer = F11Palette.Tile,
    surfaceContainerHigh = F11Palette.TileHigh,
    surfaceContainerHighest = F11Palette.TileHigh,
    outline = F11Palette.Hairline,
)

private val F11Type = Typography().run {
    copy(
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        labelSmall = TextStyle(fontSize = 11.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Medium),
    )
}

/** F11 is dark by default and by design: no bright, attention-grabbing surfaces. */
@Composable
fun F11Theme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = F11Colors, typography = F11Type, content = content)
}
