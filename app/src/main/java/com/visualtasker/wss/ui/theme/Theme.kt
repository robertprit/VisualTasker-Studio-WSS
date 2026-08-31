package com.visualtasker.wss.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Red80,
    secondary = RedGrey80,
    tertiary = DarkRed80
)

private val LightColorScheme = lightColorScheme(
    primary = Red40,
    secondary = RedGrey40,
    tertiary = DarkRed40
)

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = emptyList()
)

private val SpaceGrotesk = FontFamily(
    Font(googleFont = GoogleFont("Space Grotesk"), fontProvider = provider)
)

@Composable
fun VisualTaskerWssTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MultiPanelTheme(themeMode: String = "system", content: @Composable () -> Unit) {
    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val darkScheme = darkColorScheme(
        primary = M3EColors.Ultraviolet,
        onPrimary = Color.White,
        primaryContainer = M3EColors.Ultraviolet.copy(alpha = 0.25f),
        onPrimaryContainer = Color(0xFFD0BCFF),
        secondary = M3EColors.Auroraint,
        secondaryContainer = M3EColors.Auroraint.copy(alpha = 0.25f),
        tertiary = M3EColors.Sunsetcoral,
        surface = M3EColors.SurfaceDark,
        onSurface = Color(0xFFE6E1E5),
        surfaceVariant = M3EColors.DarkPanel,
        onSurfaceVariant = Color(0xFFCAC4D0),
        background = M3EColors.SurfaceDark,
        onBackground = Color(0xFFE6E1E5),
        outline = M3EColors.OutlineDark,
        outlineVariant = M3EColors.OutlineDark.copy(alpha = 0.5f),
    )
    val lightScheme = lightColorScheme(
        primary = Color(0xFF5B48D6),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE6E0FF),
        onPrimaryContainer = Color(0xFF1D1359),
        secondary = Color(0xFF0F7D74),
        secondaryContainer = Color(0xFFD0F4F0),
        tertiary = Color(0xFFC45152),
        surface = Color(0xFFF8F7FC),
        onSurface = Color(0xFF1A1A1F),
        surfaceVariant = Color(0xFFE8E6F0),
        onSurfaceVariant = Color(0xFF4B4756),
        background = Color(0xFFFDFBFF),
        onBackground = Color(0xFF1A1A1F),
        outline = Color(0xFF908A9E),
        outlineVariant = Color(0xFFC8C3D4),
    )
    val colorScheme = if (isDark) darkScheme else lightScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            titleMedium = MaterialTheme.typography.titleMedium.copy(fontFamily = SpaceGrotesk),
            bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontFamily = SpaceGrotesk),
            labelLarge = MaterialTheme.typography.labelLarge.copy(fontFamily = SpaceGrotesk),
        ),
        content = content
    )
}
