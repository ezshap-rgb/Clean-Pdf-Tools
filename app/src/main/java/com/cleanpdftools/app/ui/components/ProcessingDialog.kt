package com.cleanpdftools.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cleanpdftools.app.theme.AppColors
import com.cleanpdftools.app.theme.AppRadius

@Composable
fun ProcessingDialog(
    title: String = "Processing PDF...",
    statusText: String = "Please wait a moment",
    progressFraction: Float? = null,
    onDismissRequest: () -> Unit = {}
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier
                .width(320.dp)
                .clip(RoundedCornerShape(AppRadius.card))
                .background(AppColors.Surface)
                .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.card))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 3.5.dp,
                    color = AppColors.Accent
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Text
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = statusText,
                    fontSize = 13.sp,
                    color = AppColors.TextMuted
                )

                if (progressFraction != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(AppRadius.pill)),
                        color = AppColors.Accent,
                        trackColor = Color(0xFF1E2746)
                    )
                }
            }
        }
    }
}

