package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val GuardianDarkColorScheme = darkColorScheme(
    primary = LimeAccent,
    onPrimary = ObsidianBlack,
    primaryContainer = DeepOlive,
    onPrimaryContainer = LimeAccent,
    secondary = MutedLime,
    onSecondary = ObsidianBlack,
    secondaryContainer = ElevatedSurface,
    onSecondaryContainer = TextPrimary,
    tertiary = AmberWarning,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = TextPrimary,
    surface = CharcoalSurface,
    onSurface = TextPrimary,
    surfaceVariant = CardSurface,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLow = CharcoalSurface,
    surfaceContainer = CardSurface,
    surfaceContainerHigh = ElevatedSurface,
    outline = SubtleBorder,
    outlineVariant = LimeSubtleBorder,
    error = CrimsonAlert,
    onError = Color.White,
)

val GuardianShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun GuardianAITheme(
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = GuardianDarkColorScheme,
        typography = GuardianTypography,
        shapes = GuardianShapes,
        content = content,
    )
}
