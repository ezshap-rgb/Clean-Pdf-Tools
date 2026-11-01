package com.cleanpdftools.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpdftools.app.R
import com.cleanpdftools.app.theme.AppColors
import com.cleanpdftools.app.theme.AppRadius

@Composable
fun StudioTopBar(
    title: String = "Clean PDF Tools",
    subtitle: String = "Fast, Private & Powerful PDF Suite",
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (showBackButton) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(AppRadius.md))
                        .background(AppColors.Surface)
                        .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.md))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = AppColors.Text
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                Image(
                    painter = painterResource(id = R.drawable.app_icon),
                    contentDescription = "App Icon",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Text
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // STUDIO badge matching Clean Recorder design
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(AppRadius.pill))
                            .background(Color(0xFF1B163B))
                            .border(1.dp, AppColors.Accent.copy(alpha = 0.6f), RoundedCornerShape(AppRadius.pill))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "STUDIO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AppColors.Warning,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = AppColors.TextMuted,
                    maxLines = 1
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}

