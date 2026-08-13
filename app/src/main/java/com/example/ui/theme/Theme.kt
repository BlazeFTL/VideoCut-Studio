package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.model.AppTheme

@Composable
fun VideoCutterTheme(
    appTheme: AppTheme = AppTheme.MODERN_INDIGO,
    content: @Composable () -> Unit
) {
    val colorScheme = lightColorScheme(
        primary = appTheme.primaryColor,
        onPrimary = Color.White,
        primaryContainer = appTheme.primaryContainer,
        onPrimaryContainer = appTheme.onPrimaryContainer,
        secondary = appTheme.secondaryColor,
        onSecondary = Color.White,
        secondaryContainer = appTheme.secondaryContainer,
        background = SlateBackground,
        onBackground = TextPrimaryDark,
        surface = CardSurfaceWhite,
        onSurface = TextPrimaryDark,
        surfaceVariant = SlateBackground,
        onSurfaceVariant = TextSecondaryMuted,
        outline = SurfaceBorderLight,
        error = RoseError,
        errorContainer = RoseContainer
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}


