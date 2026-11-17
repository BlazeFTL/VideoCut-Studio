package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.model.AppTheme

@Composable
fun VideoCutterTheme(
    appTheme: AppTheme = AppTheme.MODERN_INDIGO,
    content: @Composable () -> Unit
) {
    val colorScheme = lightColorScheme(
        primary = appTheme.primaryColor,
        onPrimary = OnPrimaryWhite,
        primaryContainer = appTheme.primaryContainer,
        onPrimaryContainer = appTheme.onPrimaryContainer,
        secondary = appTheme.secondaryColor,
        onSecondary = OnPrimaryWhite,
        secondaryContainer = appTheme.secondaryContainer,
        onSecondaryContainer = TextPrimaryDark,
        tertiary = appTheme.secondaryColor,
        background = SlateBackground,
        onBackground = TextPrimaryDark,
        surface = CardSurfaceWhite,
        onSurface = TextPrimaryDark,
        surfaceVariant = SurfaceContainerLight,
        onSurfaceVariant = TextSecondaryMuted,
        outline = SurfaceBorderLight,
        error = RoseError,
        onError = OnPrimaryWhite,
        errorContainer = RoseContainer,
        onErrorContainer = Color(0xFF881337)
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = true
                    isAppearanceLightNavigationBars = true
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
