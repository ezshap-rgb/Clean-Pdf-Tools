package com.cleanpdftools.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpdftools.app.theme.AppColors
import com.cleanpdftools.app.theme.AppRadius

enum class PdfToolType(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color
) {
    DELETE_PAGES(
        title = "Delete Pages",
        description = "Remove specific unwanted pages",
        icon = Icons.Default.DeleteSweep,
        accentColor = AppColors.Danger
    ),
    MERGE_PDFS(
        title = "Merge PDFs",
        description = "Combine multiple PDF files",
        icon = Icons.AutoMirrored.Filled.CallMerge,
        accentColor = AppColors.Accent
    ),
    REPLACE_PAGE(
        title = "Replace Page",
        description = "Swap any page with a new document",
        icon = Icons.Default.FindReplace,
        accentColor = AppColors.Warning
    ),
    INSERT_IN_MIDDLE(
        title = "Insert in Middle",
        description = "Insert PDF into exact page position",
        icon = Icons.Default.Input,
        accentColor = Color(0xFFEAB308)
    ),
    REORDER_PAGES(
        title = "Reorder Pages",
        description = "Rearrange and sort page order",
        icon = Icons.Default.SwapVert,
        accentColor = Color(0xFFA855F7)
    ),
    EXTRACT_PAGES(
        title = "Extract Pages",
        description = "Save selected pages as a new PDF",
        icon = Icons.Default.ContentCut,
        accentColor = Color(0xFF06B6D4)
    ),
    SPLIT_PDF(
        title = "Split PDF",
        description = "Divide into two separate documents",
        icon = Icons.AutoMirrored.Filled.CallSplit,
        accentColor = Color(0xFF8B5CF6)
    ),
    ROTATE_PAGES(
        title = "Rotate Pages",
        description = "Rotate entire PDF or specific pages",
        icon = Icons.AutoMirrored.Filled.RotateRight,
        accentColor = AppColors.DotGold
    ),
    IMAGE_TO_PDF(
        title = "Image to PDF",
        description = "Convert photos and scans to PDF",
        icon = Icons.Default.Image,
        accentColor = Color(0xFF10B981)
    ),
    PDF_TO_IMAGES(
        title = "PDF to Images",
        description = "Export pages as PNG or JPEG",
        icon = Icons.Default.Collections,
        accentColor = Color(0xFFEC4899)
    ),
    CROP_PDF(
        title = "Crop PDF",
        description = "Trim margins with visual guide",
        icon = Icons.Default.Crop,
        accentColor = Color(0xFF3B82F6)
    ),
    COMPRESS_PDF(
        title = "Compress PDF",
        description = "Shrink file size and optimize",
        icon = Icons.Default.Compress,
        accentColor = Color(0xFFF97316)
    )
}

@Composable
fun ToolsOverviewScreen(
    onSelectTool: (PdfToolType) -> Unit,
    onOpenWorkflowStudio: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Tools 2-Column Grid with Workflow Studio at bottom
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp, bottom = 90.dp)
        ) {
            // All Tools (Replace and Insert sit right below Delete and Merge)
            items(PdfToolType.entries) { tool ->
                ToolGridCard(
                    tool = tool,
                    onClick = { onSelectTool(tool) }
                )
            }

            // Workflow Studio Banner sits below the standard actions
            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AppRadius.card))
                        .background(Color(0xFF13102C))
                        .border(1.dp, AppColors.Accent.copy(alpha = 0.5f), RoundedCornerShape(AppRadius.card))
                        .clickable { onOpenWorkflowStudio() }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(AppColors.StudioGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoMode,
                                contentDescription = "Workflow",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Workflow Studio",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.Text
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(AppRadius.pill))
                                        .background(AppColors.Accent)
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "DAISY-CHAIN",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Build multi-step sequential pipelines: Chain Delete, Rotate, Crop, & Compress",
                                fontSize = 11.sp,
                                color = AppColors.TextMuted,
                                maxLines = 2
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Open",
                            tint = AppColors.TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolGridCard(
    tool: PdfToolType,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .clip(RoundedCornerShape(AppRadius.card))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.card))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                .background(tool.accentColor.copy(alpha = 0.16f))
                .border(1.dp, tool.accentColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = tool.title,
                    tint = tool.accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = tool.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Text,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tool.description,
                    fontSize = 11.sp,
                    color = AppColors.TextMuted,
                    lineHeight = 14.sp,
                    maxLines = 2
                )
            }
        }
    }
}
