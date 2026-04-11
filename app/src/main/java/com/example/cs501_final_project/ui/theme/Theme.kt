package com.example.cs501_final_project.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

// Define app color scheme
private val AppColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    background = BackgroundGray,
    surface = CardWhite
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography(),
        content = content
    )
}