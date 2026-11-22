package com.sdfish.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdfish.app.R

/*
 * "Lime" design: soft graphite background, lime-to-amber accents, Unbounded for headings.
 * Dark is the default (saves battery on OLED screens); a light variant is chosen in settings.
 */

/** Ends of the accent gradient, the same in both themes. */
val Lime = Color(0xFFD2E870)
val Amber = Color(0xFFF2BE55)

/** Text and icons drawn on the accent gradient. */
val OnAccent = Color(0xFF161A15)

/** Lime to amber, used on the main buttons. */
val AccentBrush: Brush = Brush.linearGradient(listOf(Lime, Amber), start = Offset.Zero, end = Offset.Infinite)

private val DarkColors = darkColorScheme(
    primary = Lime,
    onPrimary = OnAccent,
    primaryContainer = Color(0xFF3E4820),
    onPrimaryContainer = Color(0xFFE5F3A6),
    inversePrimary = Color(0xFF556F0F),
    secondary = Amber,
    onSecondary = Color(0xFF2A1C00),
    secondaryContainer = Color(0xFF473A1A),
    onSecondaryContainer = Color(0xFFFBE1A6),
    tertiary = Color(0xFFEFA65A),
    onTertiary = Color(0xFF2C1600),
    tertiaryContainer = Color(0xFF4D3218),
    onTertiaryContainer = Color(0xFFFFDCBF),
    background = Color(0xFF1C201B),
    onBackground = Color(0xFFECEFE6),
    surface = Color(0xFF1C201B),
    onSurface = Color(0xFFECEFE6),
    surfaceVariant = Color(0xFF2E342C),
    onSurfaceVariant = Color(0xFFA3AA9E),
    surfaceDim = Color(0xFF161A15),
    surfaceBright = Color(0xFF363C34),
    surfaceContainerLowest = Color(0xFF131612),
    surfaceContainerLow = Color(0xFF20241F),
    surfaceContainer = Color(0xFF232822),
    surfaceContainerHigh = Color(0xFF2A2F28),
    surfaceContainerHighest = Color(0xFF30362E),
    outline = Color(0xFF5C6458),
    outlineVariant = Color(0xFF3A4137),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF556F0F),
    onPrimary = Color.White,
    primaryContainer = Lime,
    onPrimaryContainer = Color(0xFF1A2400),
    inversePrimary = Lime,
    secondary = Color(0xFF8A5B00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFBDE9C),
    onSecondaryContainer = Color(0xFF2B1C00),
    tertiary = Color(0xFF9A4F12),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDCC2),
    onTertiaryContainer = Color(0xFF331200),
    background = Color(0xFFF6F7F0),
    onBackground = Color(0xFF1C201B),
    surface = Color(0xFFF6F7F0),
    onSurface = Color(0xFF1C201B),
    surfaceVariant = Color(0xFFE0E4D6),
    onSurfaceVariant = Color(0xFF5B6256),
    surfaceDim = Color(0xFFD8DBD0),
    surfaceBright = Color(0xFFF6F7F0),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF0F2E9),
    surfaceContainer = Color(0xFFEAEDE3),
    surfaceContainerHigh = Color(0xFFE4E7DC),
    surfaceContainerHighest = Color(0xFFDEE2D6),
    outline = Color(0xFF767D70),
    outlineVariant = Color(0xFFC6CBBC),
)

private val Unbounded = FontFamily(
    Font(R.font.unbounded_semibold, FontWeight.SemiBold),
    Font(R.font.unbounded_bold, FontWeight.Bold),
)

private val AppTypography = Typography().let { base ->
    fun heading(style: androidx.compose.ui.text.TextStyle, weight: FontWeight = FontWeight.Bold) =
        style.copy(fontFamily = Unbounded, fontWeight = weight)
    base.copy(
        displayLarge = heading(base.displayLarge),
        displayMedium = heading(base.displayMedium),
        displaySmall = heading(base.displaySmall),
        headlineLarge = heading(base.headlineLarge),
        headlineMedium = heading(base.headlineMedium),
        headlineSmall = heading(base.headlineSmall),
        titleLarge = heading(base.titleLarge, FontWeight.SemiBold).copy(fontSize = 20.sp),
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Dark or light follows the choice in settings (applied through AppCompat night mode). */
@Composable
fun SdFishTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
