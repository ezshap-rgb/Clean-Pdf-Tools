package com.cleanpdftools.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpdftools.app.theme.AppColors
import com.cleanpdftools.app.theme.AppRadius

@Composable
fun StudioCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = AppRadius.card,
    borderColor: Color = AppColors.Border,
    backgroundColor: Color = AppColors.Surface,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val clickModifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadius))
            .then(clickModifier)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun StudioPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    gradient: Brush = AppColors.StudioGradient,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(AppRadius.pill))
            .background(if (enabled && !isLoading) gradient else Brush.linearGradient(listOf(Color(0xFF2A2845), Color(0xFF2A2845))))
            .clickable(enabled = enabled && !isLoading) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.5.dp
            )
        } else {
            Text(
                text = text,
                color = if (enabled) Color.White else AppColors.TextFaint,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun StudioSecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(AppRadius.pill))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.pill))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) AppColors.Text else AppColors.TextFaint,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

