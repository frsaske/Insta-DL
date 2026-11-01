package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AmoledDarkColorScheme = darkColorScheme(
    primary = TextPrimary,
    onPrimary = PureBlack,
    primaryContainer = DarkPillHighlight,
    onPrimaryContainer = TextPrimary,
    secondary = TextSecondary,
    onSecondary = PureBlack,
    secondaryContainer = DarkGreySurfaceVariant,
    onSecondaryContainer = TextPrimary,
    background = PureBlack,
    onBackground = TextPrimary,
    surface = PureBlack,
    onSurface = TextPrimary,
    surfaceVariant = DarkGreySurface,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkGreySurfaceVariant
)

@Composable
fun InstaDLTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = PureBlack.toArgb()
            window.navigationBarColor = PureBlack.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = AmoledDarkColorScheme,
        typography = Typography,
        content = content
    )
}
