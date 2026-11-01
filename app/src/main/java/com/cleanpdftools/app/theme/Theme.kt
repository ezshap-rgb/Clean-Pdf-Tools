package com.cleanpdftools.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

object AppSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

object AppRadius {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 18.dp
    val card = 20.dp
    val pill = 999.dp
}

private val DarkColorScheme = darkColorScheme(
    primary = AppColors.Accent,
    secondary = AppColors.Warning,
    background = AppColors.Background,
    surface = AppColors.Surface,
    onPrimary = AppColors.Text,
    onSecondary = AppColors.Text,
    onBackground = AppColors.Text,
    onSurface = AppColors.Text,
    error = AppColors.Danger
)

@Composable
fun CleanPdfToolsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

