package com.cleanpdftools.app.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object AppColors {
    val Background = Color(0xFF070913)
    val Surface = Color(0xFF0E1324)
    val SurfaceRaised = Color(0xFF151C33)
    val Border = Color(0xFF1E2746)

    // Primary Electric Violet
    val Accent = Color(0xFF5C4FEE)

    // Vibrant Magenta Highlight
    val Warning = Color(0xFFF50ACE)
    val Magenta = Color(0xFFF50ACE)

    // Actions & States
    val Danger = Color(0xFFF43F5E)
    val Success = Color(0xFF10B981)
    val DotGold = Color(0xFFF59E0B)

    // Text hierarchy
    val Text = Color(0xFFF8FAFC)
    val TextMuted = Color(0xFF94A3B8)
    val TextFaint = Color(0xFF64748B)

    val Scrim = Color(0xD9070913)

    // Studio Gradient
    val GradientStart = Color(0xFF5C4FEE)
    val GradientMid = Color(0xFFA816DF)
    val GradientEnd = Color(0xFFF50ACE)

    val StudioGradient = Brush.horizontalGradient(
        colors = listOf(GradientStart, GradientMid, GradientEnd)
    )

    val VerticalStudioGradient = Brush.verticalGradient(
        colors = listOf(GradientStart, GradientMid, GradientEnd)
    )
}

