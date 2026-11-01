package com.cleanpdftools.app.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cleanpdftools.app.data.*
import com.cleanpdftools.app.engine.PdfEngine
import com.cleanpdftools.app.engine.WorkflowEngine
import com.cleanpdftools.app.theme.AppColors
import com.cleanpdftools.app.theme.AppRadius
import com.cleanpdftools.app.ui.components.*
import kotlinx.coroutines.launch
import java.io.File

enum class WorkflowMode {
    RECIPES, CUSTOM_PIPELINE
}

@Composable
fun WorkflowStudioScreen(
    fileRepo: FileRepository,
    onBack: () -> Unit,
    onFinished: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var workflowMode by remember { mutableStateOf(WorkflowMode.RECIPES) }
    var isProcessing by remember { mutableStateOf(false) }
    var currentProgressText by remember { mutableStateOf("Executing Workflow...") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        StudioTopBar(
            title = "Workflow Studio",
            subtitle = "Daisy-chain PDF operations & smart recipes",
            showBackButton = true,
            onBackClick = onBack
        )

        // Mode Switcher Pill Tabs
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(AppRadius.pill))
                .background(AppColors.Surface)
                .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.pill))
                .padding(4.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Smart Recipes
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(AppRadius.pill))
                        .background(if (workflowMode == WorkflowMode.RECIPES) AppColors.Accent else Color.Transparent)
                        .clickable { workflowMode = WorkflowMode.RECIPES },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Smart Recipes",
                        color = if (workflowMode == WorkflowMode.RECIPES) Color.White else AppColors.TextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                // Custom Pipeline
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(AppRadius.pill))
                        .background(if (workflowMode == WorkflowMode.CUSTOM_PIPELINE) AppColors.Accent else Color.Transparent)
                        .clickable { workflowMode = WorkflowMode.CUSTOM_PIPELINE },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Daisy-Chain Pipeline",
                        color = if (workflowMode == WorkflowMode.CUSTOM_PIPELINE) Color.White else AppColors.TextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (workflowMode == WorkflowMode.RECIPES) {
            SmartRecipesContent(
                fileRepo = fileRepo,
                onStartProcessing = {
                    isProcessing = true
                    currentProgressText = it
                },
                onEndProcessing = {
                    isProcessing = false
                },
                onFinished = onFinished
            )
        } else {
            CustomPipelineContent(
                fileRepo = fileRepo,
                onStartProcessing = {
                    isProcessing = true
                    currentProgressText = it
                },
                onUpdateProgress = {
                    currentProgressText = it
                },
                onEndProcessing = {
                    isProcessing = false
                },
                onFinished = onFinished
            )
        }
    }

    if (isProcessing) {
        ProcessingDialog(
            title = "Workflow Studio Active",
            statusText = currentProgressText
        )
    }
}

/* ===================================================================================
   SMART RECIPES (Replace Page, Insert in Middle, Extract & Compress)
   =================================================================================== */
@Composable
private fun SmartRecipesContent(
    fileRepo: FileRepository,
    onStartProcessing: (String) -> Unit,
    onEndProcessing: () -> Unit,
    onFinished: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeRecipe by remember { mutableIntStateOf(0) } // 0: Replace Page, 1: Insert Middle

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Recipe Selector Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Replace Page", "Insert in Middle").forEachIndexed { idx, title ->
                val isSel = activeRecipe == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(AppRadius.pill))
                        .background(if (isSel) Color(0xFF1B163B) else AppColors.Surface)
                        .border(1.dp, if (isSel) AppColors.Accent else AppColors.Border, RoundedCornerShape(AppRadius.pill))
                        .clickable { activeRecipe = idx },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (isSel) AppColors.Warning else AppColors.Text,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (activeRecipe == 0) {
            // RECIPE 1: REPLACE PAGE (Internally Delete + Split + Merge)
            ReplacePageRecipeView(
                fileRepo = fileRepo,
                onStartProcessing = onStartProcessing,
                onEndProcessing = onEndProcessing,
                onFinished = onFinished
            )
        } else {
            // RECIPE 2: INSERT IN MIDDLE (Internally Split + Merge)
            InsertInMiddleRecipeView(
                fileRepo = fileRepo,
                onStartProcessing = onStartProcessing,
                onEndProcessing = onEndProcessing,
                onFinished = onFinished
            )
        }
    }
}

@Composable
private fun ReplacePageRecipeView(
    fileRepo: FileRepository,
    onStartProcessing: (String) -> Unit,
    onEndProcessing: () -> Unit,
    onFinished: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var baseUri by remember { mutableStateOf<Uri?>(null) }
    var baseName by remember { mutableStateOf("") }
    var basePages by remember { mutableIntStateOf(0) }
    var targetPage by remember { mutableIntStateOf(1) }

    var repUri by remember { mutableStateOf<Uri?>(null) }
    var repName by remember { mutableStateOf("") }
    var repPages by remember { mutableIntStateOf(0) }
    var repPageNum by remember { mutableIntStateOf(1) }
    var outputName by remember { mutableStateOf("replaced_page_doc") }

    val basePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            baseUri = uri
            val name = getFileName(context, uri)
            baseName = name
            outputName = "replaced_${name.removeSuffix(".pdf")}"
            scope.launch {
                basePages = PdfEngine.getPageCount(context, uri)
                targetPage = 1
            }
        }
    }

    val repPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            repUri = uri
            repName = getFileName(context, uri)
            scope.launch {
                repPages = PdfEngine.getPageCount(context, uri)
                repPageNum = 1
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Recipe Header & Explanation
            StudioCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(AppColors.Accent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FindReplace, contentDescription = null, tint = AppColors.Accent, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Replace Page Workflow", fontWeight = FontWeight.Bold, color = AppColors.Text, fontSize = 14.sp)
                        Text("Daisy-chain delete, split, and merge in one seamless step", color = AppColors.TextMuted, fontSize = 11.sp)
                    }
                }
            }
        }

        // Section 1: Base Document & Target Page
        item {
            StudioCard {
                Text("1. Base Document", fontWeight = FontWeight.Bold, color = AppColors.Text, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))

                if (baseUri == null) {
                    StudioSecondaryButton(text = "Select Base PDF") { basePicker.launch("application/pdf") }
                } else {
                    DocHeaderCard(
                        fileName = baseName,
                        pageCount = basePages,
                        extraInfo = "Target: Page $targetPage",
                        onChangeDoc = { basePicker.launch("application/pdf") }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Select which page to replace:", fontSize = 12.sp, color = AppColors.TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Slider(
                            value = targetPage.toFloat(),
                            onValueChange = { targetPage = it.toInt() },
                            valueRange = 1f..maxOf(1, basePages).toFloat(),
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = AppColors.Accent, activeTrackColor = AppColors.Accent, inactiveTrackColor = Color(0xFF1E2746))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Page $targetPage", fontWeight = FontWeight.Bold, color = AppColors.Warning, fontSize = 13.sp)
                    }
                }
            }
        }

        // Section 2: Replacement Document
        item {
            StudioCard {
                Text("2. Replacement Page Source", fontWeight = FontWeight.Bold, color = AppColors.Text, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))

                if (repUri == null) {
                    StudioSecondaryButton(text = "Select Replacement PDF") { repPicker.launch("application/pdf") }
                } else {
                    DocHeaderCard(
                        fileName = repName,
                        pageCount = repPages,
                        extraInfo = "Use Page $repPageNum",
                        onChangeDoc = { repPicker.launch("application/pdf") }
                    )

                    if (repPages > 1) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Use which page from replacement PDF:", fontSize = 12.sp, color = AppColors.TextMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Slider(
                                value = repPageNum.toFloat(),
                                onValueChange = { repPageNum = it.toInt() },
                                valueRange = 1f..repPages.toFloat(),
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = AppColors.Accent, activeTrackColor = AppColors.Accent, inactiveTrackColor = Color(0xFF1E2746))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Page $repPageNum", fontWeight = FontWeight.Bold, color = AppColors.Accent, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Section 3: Execute Action
        item {
            Spacer(modifier = Modifier.height(4.dp))
            OutputFileNameField(
                value = outputName,
                onValueChange = { outputName = it }
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            ) {
                StudioPrimaryButton(
                    text = "Run 'Replace Page' Workflow",
                    enabled = baseUri != null && repUri != null,
                    onClick = {
                        onStartProcessing("Replacing Page $targetPage with Page $repPageNum...")
                        scope.launch {
                            try {
                                val outFile = fileRepo.createOutputFile(outputName.ifBlank { "replaced_p${targetPage}_${baseName.removeSuffix(".pdf")}" })
                                val result = WorkflowEngine.executeReplacePage(
                                    context = context,
                                    baseUri = baseUri!!,
                                    targetPage1Based = targetPage,
                                    replacementUri = repUri!!,
                                    replacementPage1Based = repPageNum,
                                    outputFile = outFile,
                                    onProgress = { prog -> onStartProcessing(prog.stepDescription) }
                                )
                                fileRepo.refreshFiles()
                                onEndProcessing()
                                Toast.makeText(context, "Page $targetPage successfully replaced!", Toast.LENGTH_SHORT).show()
                                onFinished(result)
                            } catch (e: Exception) {
                                onEndProcessing()
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun InsertInMiddleRecipeView(
    fileRepo: FileRepository,
    onStartProcessing: (String) -> Unit,
    onEndProcessing: () -> Unit,
    onFinished: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var baseUri by remember { mutableStateOf<Uri?>(null) }
    var baseName by remember { mutableStateOf("") }
    var basePages by remember { mutableIntStateOf(0) }
    var insertAfterPage by remember { mutableIntStateOf(1) }

    var insUri by remember { mutableStateOf<Uri?>(null) }
    var insName by remember { mutableStateOf("") }
    var insPages by remember { mutableIntStateOf(0) }
    var outputName by remember { mutableStateOf("inserted_document") }

    val basePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            baseUri = uri
            val name = getFileName(context, uri)
            baseName = name
            outputName = "inserted_${name.removeSuffix(".pdf")}"
            scope.launch {
                basePages = PdfEngine.getPageCount(context, uri)
                insertAfterPage = maxOf(1, basePages / 2)
            }
        }
    }

    val insPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            insUri = uri
            insName = getFileName(context, uri)
            scope.launch {
                insPages = PdfEngine.getPageCount(context, uri)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StudioCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(AppColors.Warning.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Input, contentDescription = null, tint = AppColors.Warning, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Insert in Middle Workflow", fontWeight = FontWeight.Bold, color = AppColors.Text, fontSize = 14.sp)
                        Text("Splits base document and merges inserted PDF into exact middle position", color = AppColors.TextMuted, fontSize = 11.sp)
                    }
                }
            }
        }

        item {
            StudioCard {
                Text("1. Base Document & Insert Point", fontWeight = FontWeight.Bold, color = AppColors.Text, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))

                if (baseUri == null) {
                    StudioSecondaryButton(text = "Select Base PDF") { basePicker.launch("application/pdf") }
                } else {
                    DocHeaderCard(
                        fileName = baseName,
                        pageCount = basePages,
                        extraInfo = "Insert after Page $insertAfterPage",
                        onChangeDoc = { basePicker.launch("application/pdf") }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Insert after Page:", fontSize = 12.sp, color = AppColors.TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Slider(
                            value = insertAfterPage.toFloat(),
                            onValueChange = { insertAfterPage = it.toInt() },
                            valueRange = 0f..basePages.toFloat(),
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = AppColors.Accent, activeTrackColor = AppColors.Accent, inactiveTrackColor = Color(0xFF1E2746))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (insertAfterPage == 0) "Beginning" else "After Page $insertAfterPage",
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Warning,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        item {
            StudioCard {
                Text("2. Document to Insert", fontWeight = FontWeight.Bold, color = AppColors.Text, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))

                if (insUri == null) {
                    StudioSecondaryButton(text = "Select PDF to Insert") { insPicker.launch("application/pdf") }
                } else {
                    DocHeaderCard(
                        fileName = insName,
                        pageCount = insPages,
                        extraInfo = "$insPages pages will be inserted",
                        onChangeDoc = { insPicker.launch("application/pdf") }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            OutputFileNameField(
                value = outputName,
                onValueChange = { outputName = it }
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            ) {
                StudioPrimaryButton(
                    text = "Run 'Insert in Middle' Workflow",
                    enabled = baseUri != null && insUri != null,
                    onClick = {
                        onStartProcessing("Executing Split & Insert Workflow...")
                        scope.launch {
                            try {
                                val outFile = fileRepo.createOutputFile(outputName.ifBlank { "inserted_${baseName.removeSuffix(".pdf")}" })
                                val result = WorkflowEngine.executeInsertInMiddle(
                                    context = context,
                                    baseUri = baseUri!!,
                                    insertAfterPage1Based = insertAfterPage,
                                    insertUri = insUri!!,
                                    outputFile = outFile,
                                    onProgress = { prog -> onStartProcessing(prog.stepDescription) }
                                )
                                fileRepo.refreshFiles()
                                onEndProcessing()
                                Toast.makeText(context, "Document inserted successfully!", Toast.LENGTH_SHORT).show()
                                onFinished(result)
                            } catch (e: Exception) {
                                onEndProcessing()
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }
        }
    }
}

/* ===================================================================================
   DAISY-CHAIN CUSTOM PIPELINE BUILDER
   =================================================================================== */
@Composable
private fun CustomPipelineContent(
    fileRepo: FileRepository,
    onStartProcessing: (String) -> Unit,
    onUpdateProgress: (String) -> Unit,
    onEndProcessing: () -> Unit,
    onFinished: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var inputUri by remember { mutableStateOf<Uri?>(null) }
    var inputName by remember { mutableStateOf("") }
    var inputPages by remember { mutableIntStateOf(0) }
    var outputName by remember { mutableStateOf("pipeline_output") }

    val steps = remember { mutableStateListOf<WorkflowStepItem>() }
    var showAddStepSheet by remember { mutableStateOf(false) }

    val inputPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            inputUri = uri
            val name = getFileName(context, uri)
            inputName = name
            outputName = "pipeline_${name.removeSuffix(".pdf")}"
            scope.launch {
                inputPages = PdfEngine.getPageCount(context, uri)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Source Document Card
        item {
            StudioCard {
                Text("Starting Document", fontWeight = FontWeight.Bold, color = AppColors.Text, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))

                if (inputUri == null) {
                    StudioSecondaryButton(text = "Pick Starting PDF") { inputPicker.launch("application/pdf") }
                } else {
                    DocHeaderCard(
                        fileName = inputName,
                        pageCount = inputPages,
                        extraInfo = "Pipeline Start",
                        onChangeDoc = { inputPicker.launch("application/pdf") }
                    )
                }
            }
        }

        // Pipeline Chain Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daisy-Chain Steps (${steps.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Text
                )

                Button(
                    onClick = { showAddStepSheet = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent),
                    shape = RoundedCornerShape(AppRadius.pill),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Step", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (steps.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(AppRadius.card))
                        .background(AppColors.Surface)
                        .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.card)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AccountTree, contentDescription = null, tint = AppColors.TextMuted, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No chained steps yet", color = AppColors.TextMuted, fontSize = 13.sp)
                        Text("Tap '+ Add Step' to chain rotate, crop, compress, etc.", color = AppColors.TextFaint, fontSize = 11.sp)
                    }
                }
            }
        } else {
            itemsIndexed(steps) { index, stepItem ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AppRadius.md))
                        .background(AppColors.Surface)
                        .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.md))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(AppColors.Accent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${index + 1}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(stepItem.config.title, fontWeight = FontWeight.Bold, color = AppColors.Text, fontSize = 13.sp)
                            Text(stepItem.config.description, color = AppColors.TextMuted, fontSize = 11.sp)
                        }

                        IconButton(onClick = { steps.removeAt(index) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove Step", tint = AppColors.Danger, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // Run Button
        item {
            Spacer(modifier = Modifier.height(4.dp))
            OutputFileNameField(
                value = outputName,
                onValueChange = { outputName = it }
            )
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            ) {
                StudioPrimaryButton(
                    text = "Run Daisy-Chain Pipeline",
                    enabled = inputUri != null && steps.isNotEmpty(),
                    onClick = {
                        onStartProcessing("Starting pipeline execution...")
                        scope.launch {
                            try {
                                val outFile = fileRepo.createOutputFile(outputName.ifBlank { "pipeline_output_${inputName.removeSuffix(".pdf")}" })
                                val result = WorkflowEngine.executePipeline(
                                    context = context,
                                    initialUri = inputUri!!,
                                    steps = steps.toList(),
                                    finalOutputFile = outFile,
                                    onProgress = { p -> onUpdateProgress(p.stepDescription) }
                                )
                                fileRepo.refreshFiles()
                                onEndProcessing()
                                Toast.makeText(context, "Pipeline completed!", Toast.LENGTH_SHORT).show()
                                onFinished(result)
                            } catch (e: Exception) {
                                onEndProcessing()
                                Toast.makeText(context, "Pipeline error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }
        }
    }

    // Add Step Bottom Sheet / Dialog
    if (showAddStepSheet) {
        AddStepDialog(
            onDismiss = { showAddStepSheet = false },
            onAddStep = { stepConfig ->
                steps.add(WorkflowStepItem(config = stepConfig))
                showAddStepSheet = false
            }
        )
    }
}

@Composable
private fun AddStepDialog(
    onDismiss: () -> Unit,
    onAddStep: (WorkflowStepConfig) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Pipeline Step", fontWeight = FontWeight.Bold, color = AppColors.Text) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Rotate 90
                StepOptionRow("Rotate 90° CW", "Rotate entire document clockwise") {
                    onAddStep(WorkflowStepConfig.Rotate(90))
                }
                // Rotate 180
                StepOptionRow("Rotate 180°", "Flip document upside down") {
                    onAddStep(WorkflowStepConfig.Rotate(180))
                }
                // Crop Margins
                StepOptionRow("Crop Margins 5%", "Trim standard 5% whitespace margins") {
                    onAddStep(WorkflowStepConfig.Crop(0.05f, 0.05f, 0.05f, 0.05f))
                }
                // Compress (Medium)
                StepOptionRow("Compress (Recommended)", "Optimize streams and balance quality") {
                    onAddStep(WorkflowStepConfig.Compress(CompressionLevel.MEDIUM))
                }
                // Compress (High)
                StepOptionRow("Compress (High)", "Strong size reduction for sharing") {
                    onAddStep(WorkflowStepConfig.Compress(CompressionLevel.HIGH))
                }
                // Compress (Extreme)
                StepOptionRow("Compress (Extreme)", "Maximum shrink (~72 DPI) for basic docs") {
                    onAddStep(WorkflowStepConfig.Compress(CompressionLevel.EXTREME))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = AppColors.TextMuted) }
        },
        containerColor = AppColors.Surface,
        shape = RoundedCornerShape(AppRadius.card)
    )
}

@Composable
private fun StepOptionRow(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadius.md))
            .background(AppColors.SurfaceRaised)
            .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.md))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Column {
            Text(title, fontWeight = FontWeight.Bold, color = AppColors.Text, fontSize = 13.sp)
            Text(description, color = AppColors.TextMuted, fontSize = 11.sp)
        }
    }
}

