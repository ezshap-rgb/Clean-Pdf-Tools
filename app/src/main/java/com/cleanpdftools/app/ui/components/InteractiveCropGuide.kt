package com.cleanpdftools.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpdftools.app.theme.AppColors
import com.cleanpdftools.app.theme.AppRadius

@Composable
fun InteractiveCropGuide(
    bitmap: Bitmap?,
    leftPercent: Float,
    topPercent: Float,
    rightPercent: Float,
    bottomPercent: Float,
    onMarginsChange: (left: Float, top: Float, right: Float, bottom: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Visual Preview with Crop Box Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(AppRadius.md))
                .background(Color(0xFF05070E))
                .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.md)),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight(0.92f)
                        .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White)
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Page Preview",
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Crop scrim and border overlay
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        val cropLeft = w * leftPercent.coerceIn(0f, 0.45f)
                        val cropTop = h * topPercent.coerceIn(0f, 0.45f)
                        val cropRight = w * (1f - rightPercent.coerceIn(0f, 0.45f))
                        val cropBottom = h * (1f - bottomPercent.coerceIn(0f, 0.45f))

                        val cropWidth = cropRight - cropLeft
                        val cropHeight = cropBottom - cropTop

                        val scrimColor = Color(0x99000000)

                        // Top scrim
                        drawRect(
                            color = scrimColor,
                            topLeft = Offset(0f, 0f),
                            size = Size(w, cropTop)
                        )
                        // Bottom scrim
                        drawRect(
                            color = scrimColor,
                            topLeft = Offset(0f, cropBottom),
                            size = Size(w, h - cropBottom)
                        )
                        // Left scrim
                        drawRect(
                            color = scrimColor,
                            topLeft = Offset(0f, cropTop),
                            size = Size(cropLeft, cropHeight)
                        )
                        // Right scrim
                        drawRect(
                            color = scrimColor,
                            topLeft = Offset(cropRight, cropTop),
                            size = Size(w - cropRight, cropHeight)
                        )

                        // Crop active box border (Studio Violet/Gold)
                        drawRect(
                            color = Color(0xFFF59E0B),
                            topLeft = Offset(cropLeft, cropTop),
                            size = Size(cropWidth, cropHeight),
                            style = Stroke(width = 3f)
                        )

                        // Corner handles
                        val handleLength = 16f
                        val handleStroke = 5f
                        val handleColor = Color(0xFF5C4FEE)

                        // Top-Left corner
                        drawLine(handleColor, Offset(cropLeft, cropTop), Offset(cropLeft + handleLength, cropTop), handleStroke)
                        drawLine(handleColor, Offset(cropLeft, cropTop), Offset(cropLeft, cropTop + handleLength), handleStroke)

                        // Top-Right corner
                        drawLine(handleColor, Offset(cropRight, cropTop), Offset(cropRight - handleLength, cropTop), handleStroke)
                        drawLine(handleColor, Offset(cropRight, cropTop), Offset(cropRight, cropTop + handleLength), handleStroke)

                        // Bottom-Left corner
                        drawLine(handleColor, Offset(cropLeft, cropBottom), Offset(cropLeft + handleLength, cropBottom), handleStroke)
                        drawLine(handleColor, Offset(cropLeft, cropBottom), Offset(cropLeft, cropBottom - handleLength), handleStroke)

                        // Bottom-Right corner
                        drawLine(handleColor, Offset(cropRight, cropBottom), Offset(cropRight - handleLength, cropBottom), handleStroke)
                        drawLine(handleColor, Offset(cropRight, cropBottom), Offset(cropRight, cropBottom - handleLength), handleStroke)
                    }
                }
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = AppColors.Accent
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Margin Slider Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppRadius.card))
                .background(AppColors.Surface)
                .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.card))
                .padding(14.dp)
        ) {
            Text(
                text = "Crop Margins",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.Text
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Top
            MarginSliderRow(
                label = "Top",
                value = topPercent,
                onValueChange = { onMarginsChange(leftPercent, it, rightPercent, bottomPercent) }
            )

            // Bottom
            MarginSliderRow(
                label = "Bottom",
                value = bottomPercent,
                onValueChange = { onMarginsChange(leftPercent, topPercent, rightPercent, it) }
            )

            // Left
            MarginSliderRow(
                label = "Left",
                value = leftPercent,
                onValueChange = { onMarginsChange(it, topPercent, rightPercent, bottomPercent) }
            )

            // Right
            MarginSliderRow(
                label = "Right",
                value = rightPercent,
                onValueChange = { onMarginsChange(leftPercent, topPercent, it, bottomPercent) }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { onMarginsChange(0f, 0f, 0f, 0f) }
                ) {
                    Text("Reset Margins", color = AppColors.Warning, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun MarginSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = AppColors.TextMuted,
            modifier = Modifier.width(50.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..0.45f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = AppColors.Accent,
                activeTrackColor = AppColors.Accent,
                inactiveTrackColor = Color(0xFF1E2746)
            )
        )
        Text(
            text = "${(value * 100).toInt()}%",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.Text,
            modifier = Modifier.width(38.dp)
        )
    }
}

