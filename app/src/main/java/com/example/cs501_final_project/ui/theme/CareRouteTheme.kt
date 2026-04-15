package com.example.cs501_final_project.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.cs501_final_project.data.AccentThemeOption

@Composable
fun CareRouteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentTheme: AccentThemeOption = AccentThemeOption.BLUE,
    content: @Composable () -> Unit
) {
    val accent = when (accentTheme) {
        AccentThemeOption.BLUE -> Color(0xFF4F8EEB)
        AccentThemeOption.PURPLE -> Color(0xFF7B61FF)
        AccentThemeOption.GREEN -> Color(0xFF12B76A)
        AccentThemeOption.ORANGE -> Color(0xFFF79009)
    }

    val lightColors = lightColorScheme(
        primary = accent,
        secondary = accent.copy(alpha = 0.85f),
        tertiary = accent.copy(alpha = 0.70f),
        background = Color(0xFFF6F8FC),
        surface = Color.White,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color(0xFF101828),
        onSurface = Color(0xFF101828)
    )

    val darkColors = darkColorScheme(
        primary = accent,
        secondary = accent.copy(alpha = 0.85f),
        tertiary = accent.copy(alpha = 0.70f),
        background = Color(0xFF0B1220),
        surface = Color(0xFF111827),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color(0xFFF3F4F6),
        onSurface = Color(0xFFF3F4F6)
    )

    MaterialTheme(
        colorScheme = if (darkTheme) darkColors else lightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
