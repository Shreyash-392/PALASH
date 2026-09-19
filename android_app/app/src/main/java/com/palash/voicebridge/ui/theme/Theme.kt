package com.palash.voicebridge.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = White,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = PrimaryBlueDark,
    secondary = SecondaryOrange,
    onSecondary = White,
    secondaryContainer = Color(0xFFFFF0E0),
    onSecondaryContainer = SecondaryOrange,
    tertiary = Gray600,
    onTertiary = White,
    tertiaryContainer = Gray200,
    onTertiaryContainer = Gray900,
    background = White,
    onBackground = Gray900,
    surface = White,
    onSurface = Gray900,
    surfaceVariant = PrimaryLight,
    onSurfaceVariant = Gray600,
    error = StatusRed,
    onError = White,
    outline = Gray400
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueDark,
    onPrimary = White,
    primaryContainer = Color(0xFF1E3A70),
    onPrimaryContainer = PrimaryLight,
    secondary = SecondaryOrangeLight,
    onSecondary = Gray900,
    secondaryContainer = Color(0xFF6B3A00),
    onSecondaryContainer = SecondaryOrangeLight,
    background = Gray900,
    onBackground = Gray200,
    surface = DarkCard,
    onSurface = Gray200,
    surfaceVariant = Color(0xFF333333),
    onSurfaceVariant = Gray400,
    error = StatusRed,
    onError = White,
    outline = Gray600
)

@Composable
fun PalashVoiceBridgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PalashTypography,
        content = content
    )
}
