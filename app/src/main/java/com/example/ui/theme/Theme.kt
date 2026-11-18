package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val GuardianOrganicWellnessScheme = lightColorScheme(
    primary = SoftCharcoal,
    onPrimary = OnCharcoal,
    primaryContainer = ButterYellow,
    onPrimaryContainer = SoftCharcoal,
    secondary = SageGreen,
    onSecondary = SoftCharcoal,
    secondaryContainer = PowderBlueSoft,
    onSecondaryContainer = SoftCharcoal,
    tertiary = PastelPink,
    onTertiary = SoftCharcoal,
    background = WarmIvory,
    onBackground = TextPrimary,
    surface = SoftCreamSurface,
    onSurface = TextPrimary,
    surfaceVariant = CreamFieldSurface,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLow = CreamFieldSurface,
    surfaceContainer = SoftCreamSurface,
    surfaceContainerHigh = SoftCreamSurface,
    outline = SoftOutline,
    outlineVariant = DottedStroke,
    error = WarmCoral,
    onError = OnCharcoal,
)

val GuardianShapes = Shapes(
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(30.dp),
    extraLarge = RoundedCornerShape(36.dp),
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
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = GuardianOrganicWellnessScheme,
        typography = GuardianTypography,
        shapes = GuardianShapes,
        content = content,
    )
}
