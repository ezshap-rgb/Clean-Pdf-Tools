package com.cleanpdftools.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpdftools.app.data.FileRepository
import com.cleanpdftools.app.theme.AppColors
import com.cleanpdftools.app.theme.AppRadius
import com.cleanpdftools.app.ui.components.StudioTopBar
import com.cleanpdftools.app.ui.screens.*

enum class StudioMainTab {
    TOOLS, WORKFLOWS, LIBRARY
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val fileRepo = remember { FileRepository(context) }

    var currentTab by remember { mutableStateOf(StudioMainTab.TOOLS) }
    var activeTool by remember { mutableStateOf<PdfToolType?>(null) }

    // Intercept back button if inside a tool
    BackHandler(enabled = activeTool != null) {
        activeTool = null
    }

    if (activeTool != null) {
        when (activeTool!!) {
            PdfToolType.DELETE_PAGES -> {
                DeletePagesScreen(
                    fileRepo = fileRepo,
                    onBack = { activeTool = null },
                    onFinished = {
                        activeTool = null
                        currentTab = StudioMainTab.LIBRARY
                    }
                )
            }
            PdfToolType.MERGE_PDFS -> {
                MergePdfsScreen(
                    fileRepo = fileRepo,
                    onBack = { activeTool = null },
                    onFinished = {
                        activeTool = null
                        currentTab = StudioMainTab.LIBRARY
                    }
                )
            }
            PdfToolType.REPLACE_PAGE -> {
                ReplacePageToolScreen(
                    fileRepo = fileRepo,
                    onBack = { activeTool = null },
                    onFinished = {
                        activeTool = null
                        currentTab = StudioMainTab.LIBRARY
                    }
                )
            }
            PdfToolType.INSERT_IN_MIDDLE -> {
                InsertInMiddleToolScreen(
                    fileRepo = fileRepo,
                    onBack = { activeTool = null },
                    onFinished = {
                        activeTool = null
                        currentTab = StudioMainTab.LIBRARY
                    }
                )
            }
            PdfToolType.REORDER_PAGES -> {
                ReorderPagesScreen(
                    fileRepo = fileRepo,
                    onBack = { activeTool = null },
                    onFinished = {
                        activeTool = null
                        currentTab = StudioMainTab.LIBRARY
                    }
                )
            }
            PdfToolType.EXTRACT_PAGES -> {
                ExtractPagesScreen(
                    fileRepo = fileRepo,
                    onBack = { activeTool = null },
                    onFinished = {
                        activeTool = null
                        currentTab = StudioMainTab.LIBRARY
                    }
                )
            }
            PdfToolType.SPLIT_PDF -> {
                SplitPdfScreen(
                    fileRepo = fileRepo,
                    onBack = { activeTool = null },
                    onFinished = {
                        activeTool = null
                        currentTab = StudioMainTab.LIBRARY
                    }
                )
            }
            PdfToolType.ROTATE_PAGES -> {
                RotatePagesScreen(
                    fileRepo = fileRepo,
                    onBack = { activeTool = null },
                    onFinished = {
                        activeTool = null
                        currentTab = StudioMainTab.LIBRARY
                    }
                )
            }
            PdfToolType.IMAGE_TO_PDF -> {
                ImageToPdfScreen(
                    fileRepo = fileRepo,
                    onBack = { activeTool = null },
                    onFinished = {
                        activeTool = null
                        currentTab = StudioMainTab.LIBRARY
                    }
                )
            }
            PdfToolType.PDF_TO_IMAGES -> {
                PdfToImagesScreen(
                    fileRepo = fileRepo,
                    onBack = { activeTool = null },
                    onFinished = {
                        activeTool = null
                        currentTab = StudioMainTab.LIBRARY
                    }
                )
            }
            PdfToolType.CROP_PDF -> {
                CropPdfScreen(
                    fileRepo = fileRepo,
                    onBack = { activeTool = null },
                    onFinished = {
                        activeTool = null
                        currentTab = StudioMainTab.LIBRARY
                    }
                )
            }
            PdfToolType.COMPRESS_PDF -> {
                CompressPdfScreen(
                    fileRepo = fileRepo,
                    onBack = { activeTool = null },
                    onFinished = {
                        activeTool = null
                        currentTab = StudioMainTab.LIBRARY
                    }
                )
            }
        }
    } else {
        Scaffold(
            containerColor = AppColors.Background,
            bottomBar = {
                // Exact bottom segmented toggle matching Clean Recorder design
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .clip(RoundedCornerShape(AppRadius.pill))
                        .background(Color(0xFF0E1324))
                        .border(1.dp, Color(0xFF1E2746), RoundedCornerShape(AppRadius.pill))
                        .padding(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Tools Tab
                        NavPillItem(
                            title = "Tools",
                            isSelected = currentTab == StudioMainTab.TOOLS,
                            modifier = Modifier.weight(1f),
                            onClick = { currentTab = StudioMainTab.TOOLS }
                        )

                        // Workflows Tab
                        NavPillItem(
                            title = "Workflows",
                            isSelected = currentTab == StudioMainTab.WORKFLOWS,
                            modifier = Modifier.weight(1f),
                            onClick = { currentTab = StudioMainTab.WORKFLOWS }
                        )

                        // Library Tab
                        NavPillItem(
                            title = "Library",
                            isSelected = currentTab == StudioMainTab.LIBRARY,
                            modifier = Modifier.weight(1f),
                            onClick = { currentTab = StudioMainTab.LIBRARY }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentTab) {
                    StudioMainTab.TOOLS -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            StudioTopBar(
                                title = "Clean PDF Tools",
                                subtitle = "Fast, Private & Powerful PDF Suite",
                                showBackButton = false
                            )
                            ToolsOverviewScreen(
                                onSelectTool = { tool -> activeTool = tool },
                                onOpenWorkflowStudio = { currentTab = StudioMainTab.WORKFLOWS }
                            )
                        }
                    }
                    StudioMainTab.WORKFLOWS -> {
                        WorkflowStudioScreen(
                            fileRepo = fileRepo,
                            onBack = { currentTab = StudioMainTab.TOOLS },
                            onFinished = { currentTab = StudioMainTab.LIBRARY }
                        )
                    }
                    StudioMainTab.LIBRARY -> {
                        LibraryScreen(
                            fileRepo = fileRepo,
                            onNavigateToTools = { currentTab = StudioMainTab.TOOLS }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavPillItem(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(AppRadius.pill))
            .background(if (isSelected) AppColors.Accent else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else Color(0xFF94A3B8),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

