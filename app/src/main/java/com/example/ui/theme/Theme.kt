package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.model.AppTheme
import com.example.model.ThemeBrightnessMode

val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VideoCutterTheme(
    appTheme: AppTheme = AppTheme.MODERN_INDIGO,
    brightnessMode: ThemeBrightnessMode = ThemeBrightnessMode.SYSTEM,
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (brightnessMode) {
        ThemeBrightnessMode.SYSTEM -> systemDark
        ThemeBrightnessMode.LIGHT -> false
        ThemeBrightnessMode.DARK, ThemeBrightnessMode.AMOLED -> true
    }
    val isAmoled = brightnessMode == ThemeBrightnessMode.AMOLED
    val context = LocalContext.current

    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val dynamicScheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (isAmoled) {
                dynamicScheme.copy(
                    background = AmoledBackground,
                    surface = AmoledSurface,
                    surfaceVariant = AmoledSurfaceContainer,
                    surfaceContainerLowest = AmoledBackground,
                    surfaceContainerLow = AmoledSurface,
                    surfaceContainer = AmoledSurfaceContainer,
                    surfaceContainerHigh = AmoledSurfaceContainerHigh,
                    surfaceContainerHighest = Color(0xFF222636),
                    outline = AmoledOutline,
                    outlineVariant = Color(0xFF1B1F2E)
                )
            } else {
                dynamicScheme
            }
        }
        isDark -> {
            val primaryTone = lerp(appTheme.primaryColor, Color.White, 0.22f)
            val secondaryTone = lerp(appTheme.secondaryColor, Color.White, 0.22f)
            val primaryContainerDark = lerp(appTheme.primaryColor, Color.Black, 0.68f)
            val secondaryContainerDark = lerp(appTheme.secondaryColor, Color.Black, 0.68f)

            darkColorScheme(
                primary = primaryTone,
                onPrimary = Color(0xFF0A0D14),
                primaryContainer = primaryContainerDark,
                onPrimaryContainer = lerp(appTheme.primaryColor, Color.White, 0.78f),
                secondary = secondaryTone,
                onSecondary = Color(0xFF0A0D14),
                secondaryContainer = secondaryContainerDark,
                onSecondaryContainer = lerp(appTheme.secondaryColor, Color.White, 0.78f),
                tertiary = lerp(appTheme.secondaryColor, appTheme.primaryColor, 0.4f),
                background = if (isAmoled) AmoledBackground else DarkBackground,
                onBackground = DarkTextPrimary,
                surface = if (isAmoled) AmoledSurface else DarkSurface,
                onSurface = DarkTextPrimary,
                surfaceVariant = if (isAmoled) AmoledSurfaceContainer else DarkSurfaceContainer,
                onSurfaceVariant = DarkTextMuted,
                surfaceContainerLowest = if (isAmoled) AmoledBackground else Color(0xFF0A0C11),
                surfaceContainerLow = if (isAmoled) AmoledSurface else DarkSurface,
                surfaceContainer = if (isAmoled) AmoledSurfaceContainer else DarkSurfaceContainer,
                surfaceContainerHigh = if (isAmoled) AmoledSurfaceContainerHigh else DarkSurfaceContainerHigh,
                surfaceContainerHighest = if (isAmoled) Color(0xFF222636) else Color(0xFF2D3347),
                outline = if (isAmoled) AmoledOutline else DarkOutline,
                outlineVariant = if (isAmoled) Color(0xFF1A1E2B) else Color(0xFF242A3A),
                error = Color(0xFFFF6B81),
                onError = Color(0xFF3A000D),
                errorContainer = Color(0xFF5A1120),
                onErrorContainer = Color(0xFFFFD9DF)
            )
        }
        else -> {
            lightColorScheme(
                primary = appTheme.primaryColor,
                onPrimary = Color.White,
                primaryContainer = appTheme.primaryContainer,
                onPrimaryContainer = appTheme.onPrimaryContainer,
                secondary = appTheme.secondaryColor,
                onSecondary = Color.White,
                secondaryContainer = appTheme.secondaryContainer,
                onSecondaryContainer = TextPrimaryDark,
                tertiary = appTheme.secondaryColor,
                background = SlateBackground,
                onBackground = TextPrimaryDark,
                surface = CardSurfaceWhite,
                onSurface = TextPrimaryDark,
                surfaceVariant = SurfaceContainerLight,
                onSurfaceVariant = TextSecondaryMuted,
                surfaceContainerLowest = CardSurfaceWhite,
                surfaceContainerLow = Color(0xFFF9FAFD),
                surfaceContainer = SurfaceContainerLight,
                surfaceContainerHigh = SurfaceContainerHighLight,
                surfaceContainerHighest = Color(0xFFDCE2EE),
                outline = SurfaceBorderLight,
                outlineVariant = Color(0xFFECEFF6),
                error = RoseError,
                onError = Color.White,
                errorContainer = RoseContainer,
                onErrorContainer = Color(0xFF881337)
            )
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
            }
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = Typography,
        shapes = ExpressiveShapes,
        content = content
    )
}
