package com.cleanpdftools.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpdftools.app.theme.AppColors
import com.cleanpdftools.app.theme.AppRadius

@Composable
fun PageThumbnailItem(
    pageNumber: Int,
    bitmap: Bitmap?,
    isSelected: Boolean = false,
    isMarkedForDeletion: Boolean = false,
    rotation: Int = 0,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val borderColor = when {
        isMarkedForDeletion -> AppColors.Danger
        isSelected -> AppColors.Accent
        else -> AppColors.Border
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(AppRadius.md))
            .background(AppColors.Surface)
            .border(if (isSelected || isMarkedForDeletion) 2.dp else 1.dp, borderColor, RoundedCornerShape(AppRadius.md))
            .clickable { onClick() }
            .padding(6.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
                    .clip(RoundedCornerShape(AppRadius.sm))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Page $pageNumber",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = AppColors.Accent
                    )
                }

                // Delete / Selection overlay
                if (isMarkedForDeletion) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AppColors.Danger.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(AppColors.Danger),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Deleted",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else if (isSelected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(AppColors.Accent.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(6.dp)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(AppColors.Accent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Page $pageNumber",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isMarkedForDeletion) AppColors.Danger else AppColors.Text
                )
                if (rotation != 0) {
                    Text(
                        text = "$rotation°",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.DotGold
                    )
                }
            }
        }
    }
}

