package com.cleanpdftools.app.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.automirrored.filled.RotateRight
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
import com.cleanpdftools.app.data.CompressionLevel
import com.cleanpdftools.app.data.FileRepository
import com.cleanpdftools.app.engine.PdfEngine
import com.cleanpdftools.app.engine.WorkflowEngine
import com.cleanpdftools.app.theme.AppColors
import com.cleanpdftools.app.theme.AppRadius
import com.cleanpdftools.app.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// Helper to get display name from URI
fun getFileName(context: android.content.Context, uri: Uri): String {
    var name = "Document.pdf"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1 && cursor.moveToFirst()) {
            name = cursor.getString(nameIndex)
        }
    }
    return name
}

/* ===================================================================================
   1. DELETE PAGES SCREEN
   =================================================================================== */
@Composable
fun DeletePagesScreen(
    fileRepo: FileRepository,
    onBack: () -> Unit,
    onFinished: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var outputName by remember { mutableStateOf("") }
    var pageCount by remember { mutableIntStateOf(0) }
    val pageThumbnails = remember { mutableStateMapOf<Int, Bitmap?>() }
    val pagesToDelete = remember { mutableStateListOf<Int>() }
    var isProcessing by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            val name = getFileName(context, uri)
            fileName = name
            outputName = "deleted_${name.removeSuffix(".pdf")}"
            pagesToDelete.clear()
            pageThumbnails.clear()
            scope.launch {
                try {
                    val count = PdfEngine.getPageCount(context, uri)
                    pageCount = count
                    for (i in 0 until count) {
                        launch {
                            val bmp = PdfEngine.renderPageThumbnail(context, uri, i, 220)
                            pageThumbnails[i + 1] = bmp
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to load PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        StudioTopBar(
            title = "Delete Pages",
            subtitle = "Remove specific unwanted pages",
            showBackButton = true,
            onBackClick = onBack
        )

        if (selectedUri == null) {
            EmptyFilePickerPlaceholder(
                title = "Select PDF Document",
                description = "Choose a PDF to select and delete unwanted pages",
                buttonText = "Pick PDF File",
                onPick = { filePicker.launch("application/pdf") }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                DocHeaderCard(
                    fileName = fileName,
                    pageCount = pageCount,
                    extraInfo = "${pagesToDelete.size} pages selected for deletion",
                    onChangeDoc = { filePicker.launch("application/pdf") }
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Tap pages to mark for deletion:",
                    fontSize = 13.sp,
                    color = AppColors.TextMuted
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items((1..pageCount).toList()) { pageNum ->
                        val isDeleted = pagesToDelete.contains(pageNum)
                        PageThumbnailItem(
                            pageNumber = pageNum,
                            bitmap = pageThumbnails[pageNum],
                            isMarkedForDeletion = isDeleted,
                            onClick = {
                                if (isDeleted) pagesToDelete.remove(pageNum) else pagesToDelete.add(pageNum)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutputFileNameField(
                    value = outputName,
                    onValueChange = { outputName = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom button container with navigation safe space
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 20.dp)
                ) {
                    StudioPrimaryButton(
                        text = if (pagesToDelete.isEmpty()) "Select Pages to Delete" else "Delete ${pagesToDelete.size} Page(s) & Save",
                        enabled = pagesToDelete.isNotEmpty() && pagesToDelete.size < pageCount && !isProcessing,
                        gradient = androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(AppColors.Danger, Color(0xFFE11D48))),
                        onClick = {
                            isProcessing = true
                            scope.launch {
                                try {
                                    val outFile = fileRepo.createOutputFile(outputName.ifBlank { "deleted_${fileName.removeSuffix(".pdf")}" })
                                    val result = PdfEngine.deletePages(context, selectedUri!!, pagesToDelete.toSet(), outFile)
                                    fileRepo.refreshFiles()
                                    isProcessing = false
                                    Toast.makeText(context, "Saved: ${result.name}", Toast.LENGTH_SHORT).show()
                                    onFinished(result)
                                } catch (e: Exception) {
                                    isProcessing = false
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (isProcessing) {
        ProcessingDialog(title = "Deleting Pages...", statusText = "Generating clean PDF...")
    }
}

/* ===================================================================================
   2. MERGE PDFS SCREEN
   =================================================================================== */
@Composable
fun MergePdfsScreen(
    fileRepo: FileRepository,
    onBack: () -> Unit,
    onFinished: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    data class MergeItem(val uri: Uri, val name: String, val size: Long, var pages: Int = 0)
    val mergeList = remember { mutableStateListOf<MergeItem>() }
    var outputName by remember { mutableStateOf("merged_document") }
    var isProcessing by remember { mutableStateOf(false) }

    val multiPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        scope.launch {
            for (u in uris) {
                val n = getFileName(context, u)
                var p = 0
                try { p = PdfEngine.getPageCount(context, u) } catch (_: Exception) {}
                mergeList.add(MergeItem(u, n, 0L, p))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        StudioTopBar(
            title = "Merge PDFs",
            subtitle = "Combine multiple PDF files into one",
            showBackButton = true,
            onBackClick = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${mergeList.size} Document(s) Added",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Text
                )

                Button(
                    onClick = { multiPicker.launch("application/pdf") },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent),
                    shape = RoundedCornerShape(AppRadius.pill),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add PDFs", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (mergeList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(AppRadius.card))
                        .background(AppColors.Surface)
                        .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.card)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.AutoMirrored.Filled.CallMerge, contentDescription = null, tint = AppColors.TextMuted, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No PDFs added yet", color = AppColors.TextMuted, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Tap '+ Add PDFs' to select 2 or more files", color = AppColors.TextFaint, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(mergeList) { index, item ->
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
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(AppColors.SurfaceRaised),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${index + 1}", color = AppColors.Accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, color = AppColors.Text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                                    Text("${item.pages} pages", color = AppColors.TextMuted, fontSize = 11.sp)
                                }
                                // Move Up
                                IconButton(
                                    onClick = {
                                        if (index > 0) {
                                            val el = mergeList.removeAt(index)
                                            mergeList.add(index - 1, el)
                                        }
                                    },
                                    enabled = index > 0
                                ) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = "Up", tint = if (index > 0) AppColors.Text else AppColors.TextFaint, modifier = Modifier.size(18.dp))
                                }
                                // Move Down
                                IconButton(
                                    onClick = {
                                        if (index < mergeList.lastIndex) {
                                            val el = mergeList.removeAt(index)
                                            mergeList.add(index + 1, el)
                                        }
                                    },
                                    enabled = index < mergeList.lastIndex
                                ) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = "Down", tint = if (index < mergeList.lastIndex) AppColors.Text else AppColors.TextFaint, modifier = Modifier.size(18.dp))
                                }
                                // Remove
                                IconButton(onClick = { mergeList.removeAt(index) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = AppColors.Danger, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutputFileNameField(
                value = outputName,
                onValueChange = { outputName = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp)
            ) {
                StudioPrimaryButton(
                    text = "Merge ${mergeList.size} PDFs",
                    enabled = mergeList.size >= 2 && !isProcessing,
                    onClick = {
                        isProcessing = true
                        scope.launch {
                            try {
                                val outFile = fileRepo.createOutputFile(outputName.ifBlank { "merged_document" })
                                val result = PdfEngine.mergePdfs(context, mergeList.map { it.uri }, outFile)
                                fileRepo.refreshFiles()
                                isProcessing = false
                                Toast.makeText(context, "Merged successfully!", Toast.LENGTH_SHORT).show()
                                onFinished(result)
                            } catch (e: Exception) {
                                isProcessing = false
                                Toast.makeText(context, "Merge failed: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }
        }
    }

    if (isProcessing) {
        ProcessingDialog(title = "Merging PDFs...", statusText = "Combining documents in order...")
    }
}

/* ===================================================================================
   STANDALONE: REPLACE PAGE TOOL SCREEN
   =================================================================================== */
@Composable
fun ReplacePageToolScreen(
    fileRepo: FileRepository,
    onBack: () -> Unit,
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
    var isProcessing by remember { mutableStateOf(false) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        StudioTopBar(
            title = "Replace Page",
            subtitle = "Swap any page with a new document page",
            showBackButton = true,
            onBackClick = onBack
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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

            item {
                StudioCard {
                    Text("2. Replacement Document", fontWeight = FontWeight.Bold, color = AppColors.Text, fontSize = 13.sp)
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

            item {
                OutputFileNameField(
                    value = outputName,
                    onValueChange = { outputName = it }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            StudioPrimaryButton(
                text = "Replace Page & Save",
                enabled = baseUri != null && repUri != null && !isProcessing,
                onClick = {
                    isProcessing = true
                    scope.launch {
                        try {
                            val outFile = fileRepo.createOutputFile(outputName.ifBlank { "replaced_page_doc" })
                            val result = WorkflowEngine.executeReplacePage(
                                context = context,
                                baseUri = baseUri!!,
                                targetPage1Based = targetPage,
                                replacementUri = repUri!!,
                                replacementPage1Based = repPageNum,
                                outputFile = outFile
                            )
                            fileRepo.refreshFiles()
                            isProcessing = false
                            Toast.makeText(context, "Page $targetPage replaced!", Toast.LENGTH_SHORT).show()
                            onFinished(result)
                        } catch (e: Exception) {
                            isProcessing = false
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }
    }

    if (isProcessing) {
        ProcessingDialog(title = "Replacing Page...", statusText = "Daisy-chaining split and merge...")
    }
}

/* ===================================================================================
   STANDALONE: INSERT IN MIDDLE TOOL SCREEN
   =================================================================================== */
@Composable
fun InsertInMiddleToolScreen(
    fileRepo: FileRepository,
    onBack: () -> Unit,
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
    var isProcessing by remember { mutableStateOf(false) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        StudioTopBar(
            title = "Insert in Middle",
            subtitle = "Insert a PDF into any page position",
            showBackButton = true,
            onBackClick = onBack
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                            extraInfo = "$insPages pages to insert",
                            onChangeDoc = { insPicker.launch("application/pdf") }
                        )
                    }
                }
            }

            item {
                OutputFileNameField(
                    value = outputName,
                    onValueChange = { outputName = it }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            StudioPrimaryButton(
                text = "Insert PDF & Save",
                enabled = baseUri != null && insUri != null && !isProcessing,
                onClick = {
                    isProcessing = true
                    scope.launch {
                        try {
                            val outFile = fileRepo.createOutputFile(outputName.ifBlank { "inserted_document" })
                            val result = WorkflowEngine.executeInsertInMiddle(
                                context = context,
                                baseUri = baseUri!!,
                                insertAfterPage1Based = insertAfterPage,
                                insertUri = insUri!!,
                                outputFile = outFile
                            )
                            fileRepo.refreshFiles()
                            isProcessing = false
                            Toast.makeText(context, "Inserted successfully!", Toast.LENGTH_SHORT).show()
                            onFinished(result)
                        } catch (e: Exception) {
                            isProcessing = false
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )
        }
    }

    if (isProcessing) {
        ProcessingDialog(title = "Inserting PDF...", statusText = "Splitting and splicing document...")
    }
}

/* ===================================================================================
   3. REORDER PAGES SCREEN
   =================================================================================== */
@Composable
fun ReorderPagesScreen(
    fileRepo: FileRepository,
    onBack: () -> Unit,
    onFinished: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var outputName by remember { mutableStateOf("") }
    val orderedPages = remember { mutableStateListOf<Int>() }
    val pageThumbnails = remember { mutableStateMapOf<Int, Bitmap?>() }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            val name = getFileName(context, uri)
            fileName = name
            outputName = "reordered_${name.removeSuffix(".pdf")}"
            orderedPages.clear()
            pageThumbnails.clear()
            selectedIndex = null
            scope.launch {
                val count = PdfEngine.getPageCount(context, uri)
                for (i in 1..count) {
                    orderedPages.add(i)
                    launch {
                        val bmp = PdfEngine.renderPageThumbnail(context, uri, i - 1, 200)
                        pageThumbnails[i] = bmp
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        StudioTopBar(
            title = "Reorder Pages",
            subtitle = "Rearrange page sequence visually",
            showBackButton = true,
            onBackClick = onBack
        )

        if (selectedUri == null) {
            EmptyFilePickerPlaceholder(
                title = "Select PDF Document",
                description = "Choose a PDF to rearrange its pages",
                buttonText = "Pick PDF File",
                onPick = { filePicker.launch("application/pdf") }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                DocHeaderCard(
                    fileName = fileName,
                    pageCount = orderedPages.size,
                    extraInfo = if (selectedIndex != null) "Page ${orderedPages[selectedIndex!!]} selected (Move left/right)" else "Tap a page to move it",
                    onChangeDoc = { filePicker.launch("application/pdf") }
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (selectedIndex != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(AppRadius.pill))
                            .background(AppColors.Surface)
                            .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.pill))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Move Page ${orderedPages[selectedIndex!!]}:", fontSize = 12.sp, color = AppColors.Text)
                        Row {
                            TextButton(
                                onClick = {
                                    val idx = selectedIndex!!
                                    if (idx > 0) {
                                        val item = orderedPages.removeAt(idx)
                                        orderedPages.add(idx - 1, item)
                                        selectedIndex = idx - 1
                                    }
                                },
                                enabled = selectedIndex!! > 0
                            ) {
                                Text("◀ Left", color = if (selectedIndex!! > 0) AppColors.Accent else AppColors.TextFaint, fontSize = 12.sp)
                            }
                            TextButton(
                                onClick = {
                                    val idx = selectedIndex!!
                                    if (idx < orderedPages.lastIndex) {
                                        val item = orderedPages.removeAt(idx)
                                        orderedPages.add(idx + 1, item)
                                        selectedIndex = idx + 1
                                    }
                                },
                                enabled = selectedIndex!! < orderedPages.lastIndex
                            ) {
                                Text("Right ▶", color = if (selectedIndex!! < orderedPages.lastIndex) AppColors.Accent else AppColors.TextFaint, fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(orderedPages) { index, originalPageNum ->
                        val isSelected = selectedIndex == index
                        PageThumbnailItem(
                            pageNumber = originalPageNum,
                            bitmap = pageThumbnails[originalPageNum],
                            isSelected = isSelected,
                            onClick = {
                                selectedIndex = if (isSelected) null else index
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutputFileNameField(
                    value = outputName,
                    onValueChange = { outputName = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 20.dp)
                ) {
                    StudioPrimaryButton(
                        text = "Save Reordered PDF",
                        enabled = orderedPages.isNotEmpty() && !isProcessing,
                        onClick = {
                            isProcessing = true
                            scope.launch {
                                try {
                                    val outFile = fileRepo.createOutputFile(outputName.ifBlank { "reordered_${fileName.removeSuffix(".pdf")}" })
                                    val result = PdfEngine.reorderPages(context, selectedUri!!, orderedPages.toList(), outFile)
                                    fileRepo.refreshFiles()
                                    isProcessing = false
                                    Toast.makeText(context, "Saved successfully!", Toast.LENGTH_SHORT).show()
                                    onFinished(result)
                                } catch (e: Exception) {
                                    isProcessing = false
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (isProcessing) {
        ProcessingDialog(title = "Saving Reordered PDF...", statusText = "Assembling pages in new sequence...")
    }
}

/* ===================================================================================
   4. EXTRACT PAGES SCREEN
   =================================================================================== */
@Composable
fun ExtractPagesScreen(
    fileRepo: FileRepository,
    onBack: () -> Unit,
    onFinished: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var outputName by remember { mutableStateOf("") }
    var pageCount by remember { mutableIntStateOf(0) }
    val pageThumbnails = remember { mutableStateMapOf<Int, Bitmap?>() }
    val pagesToExtract = remember { mutableStateListOf<Int>() }
    var isProcessing by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            val name = getFileName(context, uri)
            fileName = name
            outputName = "extracted_${name.removeSuffix(".pdf")}"
            pagesToExtract.clear()
            pageThumbnails.clear()
            scope.launch {
                val count = PdfEngine.getPageCount(context, uri)
                pageCount = count
                for (i in 1..count) {
                    launch {
                        val bmp = PdfEngine.renderPageThumbnail(context, uri, i - 1, 200)
                        pageThumbnails[i] = bmp
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        StudioTopBar(
            title = "Extract Pages",
            subtitle = "Extract selected pages into a new document",
            showBackButton = true,
            onBackClick = onBack
        )

        if (selectedUri == null) {
            EmptyFilePickerPlaceholder(
                title = "Select PDF Document",
                description = "Choose a PDF to extract pages from",
                buttonText = "Pick PDF File",
                onPick = { filePicker.launch("application/pdf") }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                DocHeaderCard(
                    fileName = fileName,
                    pageCount = pageCount,
                    extraInfo = "${pagesToExtract.size} pages selected for extraction",
                    onChangeDoc = { filePicker.launch("application/pdf") }
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select pages to keep:", fontSize = 13.sp, color = AppColors.TextMuted)
                    TextButton(onClick = {
                        if (pagesToExtract.size == pageCount) pagesToExtract.clear()
                        else {
                            pagesToExtract.clear()
                            pagesToExtract.addAll(1..pageCount)
                        }
                    }) {
                        Text(if (pagesToExtract.size == pageCount) "Deselect All" else "Select All", color = AppColors.Accent, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items((1..pageCount).toList()) { pageNum ->
                        val isSelected = pagesToExtract.contains(pageNum)
                        PageThumbnailItem(
                            pageNumber = pageNum,
                            bitmap = pageThumbnails[pageNum],
                            isSelected = isSelected,
                            onClick = {
                                if (isSelected) pagesToExtract.remove(pageNum) else pagesToExtract.add(pageNum)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutputFileNameField(
                    value = outputName,
                    onValueChange = { outputName = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 20.dp)
                ) {
                    StudioPrimaryButton(
                        text = "Extract ${pagesToExtract.size} Page(s) & Save",
                        enabled = pagesToExtract.isNotEmpty() && !isProcessing,
                        onClick = {
                            isProcessing = true
                            scope.launch {
                                try {
                                    val outFile = fileRepo.createOutputFile(outputName.ifBlank { "extracted_${fileName.removeSuffix(".pdf")}" })
                                    val result = PdfEngine.extractPages(context, selectedUri!!, pagesToExtract.sorted(), outFile)
                                    fileRepo.refreshFiles()
                                    isProcessing = false
                                    Toast.makeText(context, "Extracted successfully!", Toast.LENGTH_SHORT).show()
                                    onFinished(result)
                                } catch (e: Exception) {
                                    isProcessing = false
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (isProcessing) {
        ProcessingDialog(title = "Extracting Pages...", statusText = "Creating new document...")
    }
}

/* ===================================================================================
   5. SPLIT PDF SCREEN
   =================================================================================== */
@Composable
fun SplitPdfScreen(
    fileRepo: FileRepository,
    onBack: () -> Unit,
    onFinished: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var outputBaseName by remember { mutableStateOf("") }
    var pageCount by remember { mutableIntStateOf(0) }
    var splitAfterPage by remember { mutableIntStateOf(1) }
    var isProcessing by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            val name = getFileName(context, uri)
            fileName = name
            outputBaseName = name.removeSuffix(".pdf")
            scope.launch {
                val count = PdfEngine.getPageCount(context, uri)
                pageCount = count
                splitAfterPage = maxOf(1, count / 2)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        StudioTopBar(
            title = "Split PDF",
            subtitle = "Divide into two separate documents",
            showBackButton = true,
            onBackClick = onBack
        )

        if (selectedUri == null) {
            EmptyFilePickerPlaceholder(
                title = "Select PDF Document",
                description = "Choose a PDF with 2 or more pages to split",
                buttonText = "Pick PDF File",
                onPick = { filePicker.launch("application/pdf") }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                DocHeaderCard(
                    fileName = fileName,
                    pageCount = pageCount,
                    extraInfo = "Split at page $splitAfterPage",
                    onChangeDoc = { filePicker.launch("application/pdf") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                StudioCard {
                    Text("Split Position", fontWeight = FontWeight.Bold, color = AppColors.Text, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Split after page $splitAfterPage:", color = AppColors.TextMuted, fontSize = 13.sp)

                    if (pageCount > 1) {
                        Slider(
                            value = splitAfterPage.toFloat(),
                            onValueChange = { splitAfterPage = it.toInt() },
                            valueRange = 1f..(pageCount - 1).toFloat(),
                            steps = maxOf(0, pageCount - 3),
                            colors = SliderDefaults.colors(
                                thumbColor = AppColors.Accent,
                                activeTrackColor = AppColors.Accent,
                                inactiveTrackColor = Color(0xFF1E2746)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(AppRadius.md))
                                .background(AppColors.SurfaceRaised)
                                .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.md))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text("Part 1", color = AppColors.Accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Pages 1 to $splitAfterPage", color = AppColors.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("($splitAfterPage pages)", color = AppColors.TextMuted, fontSize = 11.sp)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(AppRadius.md))
                                .background(AppColors.SurfaceRaised)
                                .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.md))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text("Part 2", color = AppColors.Warning, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Pages ${splitAfterPage + 1} to $pageCount", color = AppColors.Text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("(${pageCount - splitAfterPage} pages)", color = AppColors.TextMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutputFileNameField(
                    value = outputBaseName,
                    onValueChange = { outputBaseName = it }
                )

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 20.dp)
                ) {
                    StudioPrimaryButton(
                        text = "Split Document",
                        enabled = pageCount > 1 && !isProcessing,
                        onClick = {
                            isProcessing = true
                            scope.launch {
                                try {
                                    val base = outputBaseName.ifBlank { fileName.removeSuffix(".pdf") }
                                    val part1File = fileRepo.createOutputFile("${base}_part1")
                                    val part2File = fileRepo.createOutputFile("${base}_part2")
                                    val result = PdfEngine.splitPdf(context, selectedUri!!, splitAfterPage, part1File, part2File)
                                    fileRepo.refreshFiles()
                                    isProcessing = false
                                    Toast.makeText(context, "Split completed into 2 files!", Toast.LENGTH_SHORT).show()
                                    onFinished(result.first)
                                } catch (e: Exception) {
                                    isProcessing = false
                                    Toast.makeText(context, "Split error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (isProcessing) {
        ProcessingDialog(title = "Splitting PDF...", statusText = "Generating Part 1 and Part 2...")
    }
}

/* ===================================================================================
   6. ROTATE PAGES SCREEN
   =================================================================================== */
@Composable
fun RotatePagesScreen(
    fileRepo: FileRepository,
    onBack: () -> Unit,
    onFinished: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var outputName by remember { mutableStateOf("") }
    var pageCount by remember { mutableIntStateOf(0) }
    var rotationAngle by remember { mutableIntStateOf(90) }
    var applyToAll by remember { mutableStateOf(true) }
    val selectedPages = remember { mutableStateListOf<Int>() }
    val pageThumbnails = remember { mutableStateMapOf<Int, Bitmap?>() }
    var isProcessing by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            val name = getFileName(context, uri)
            fileName = name
            outputName = "rotated_${name.removeSuffix(".pdf")}"
            selectedPages.clear()
            pageThumbnails.clear()
            scope.launch {
                val count = PdfEngine.getPageCount(context, uri)
                pageCount = count
                for (i in 1..count) {
                    launch {
                        val bmp = PdfEngine.renderPageThumbnail(context, uri, i - 1, 200)
                        pageThumbnails[i] = bmp
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        StudioTopBar(
            title = "Rotate Pages",
            subtitle = "Rotate entire PDF or specific pages",
            showBackButton = true,
            onBackClick = onBack
        )

        if (selectedUri == null) {
            EmptyFilePickerPlaceholder(
                title = "Select PDF Document",
                description = "Choose a PDF to rotate its pages",
                buttonText = "Pick PDF File",
                onPick = { filePicker.launch("application/pdf") }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                DocHeaderCard(
                    fileName = fileName,
                    pageCount = pageCount,
                    extraInfo = if (applyToAll) "Rotating All Pages by $rotationAngle°" else "Rotating ${selectedPages.size} Selected Pages by $rotationAngle°",
                    onChangeDoc = { filePicker.launch("application/pdf") }
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(90 to "90° CW", 180 to "180° Flip", 270 to "90° CCW").forEach { (deg, label) ->
                        val isSelected = rotationAngle == deg
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(AppRadius.pill))
                                .background(if (isSelected) AppColors.Accent else AppColors.Surface)
                                .border(1.dp, if (isSelected) AppColors.Accent else AppColors.Border, RoundedCornerShape(AppRadius.pill))
                                .clickable { rotationAngle = deg },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (isSelected) Color.White else AppColors.Text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (applyToAll) "Scope: All Pages" else "Scope: Selected (${selectedPages.size})", fontSize = 13.sp, color = AppColors.TextMuted)
                    TextButton(onClick = { applyToAll = !applyToAll }) {
                        Text(if (applyToAll) "Switch to Select Pages" else "Switch to All Pages", color = AppColors.Warning, fontSize = 12.sp)
                    }
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items((1..pageCount).toList()) { pageNum ->
                        val isTarget = applyToAll || selectedPages.contains(pageNum)
                        PageThumbnailItem(
                            pageNumber = pageNum,
                            bitmap = pageThumbnails[pageNum],
                            isSelected = isTarget,
                            rotation = if (isTarget) rotationAngle else 0,
                            onClick = {
                                if (!applyToAll) {
                                    if (selectedPages.contains(pageNum)) selectedPages.remove(pageNum) else selectedPages.add(pageNum)
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutputFileNameField(
                    value = outputName,
                    onValueChange = { outputName = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 20.dp)
                ) {
                    StudioPrimaryButton(
                        text = "Apply Rotation & Save",
                        enabled = (applyToAll || selectedPages.isNotEmpty()) && !isProcessing,
                        onClick = {
                            isProcessing = true
                            scope.launch {
                                try {
                                    val outFile = fileRepo.createOutputFile(outputName.ifBlank { "rotated_${fileName.removeSuffix(".pdf")}" })
                                    val targets = if (applyToAll) null else selectedPages.toSet()
                                    val result = PdfEngine.rotatePages(context, selectedUri!!, rotationAngle, targets, outFile)
                                    fileRepo.refreshFiles()
                                    isProcessing = false
                                    Toast.makeText(context, "Rotated and saved!", Toast.LENGTH_SHORT).show()
                                    onFinished(result)
                                } catch (e: Exception) {
                                    isProcessing = false
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (isProcessing) {
        ProcessingDialog(title = "Rotating Pages...", statusText = "Adjusting page orientations...")
    }
}

/* ===================================================================================
   7. IMAGE TO PDF SCREEN
   =================================================================================== */
@Composable
fun ImageToPdfScreen(
    fileRepo: FileRepository,
    onBack: () -> Unit,
    onFinished: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val imageUris = remember { mutableStateListOf<Uri>() }
    var outputName by remember { mutableStateOf("images_converted") }
    var fitPage by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        imageUris.addAll(uris)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        StudioTopBar(
            title = "Images to PDF",
            subtitle = "Convert images & scans into a clean PDF",
            showBackButton = true,
            onBackClick = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${imageUris.size} Images Selected", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppColors.Text)
                Button(
                    onClick = { imagePicker.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent),
                    shape = RoundedCornerShape(AppRadius.pill)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Images", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (imageUris.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(AppRadius.card))
                        .background(AppColors.Surface)
                        .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.card)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = AppColors.TextMuted, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No images chosen", color = AppColors.TextMuted, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Tap '+ Add Images' to select pictures", color = AppColors.TextFaint, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(imageUris) { index, uri ->
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
                                Text("${index + 1}.", color = AppColors.Accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(getFileName(context, uri), color = AppColors.Text, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1)
                                IconButton(onClick = { imageUris.removeAt(index) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = AppColors.Danger, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppRadius.md))
                    .background(AppColors.Surface)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Fit to standard A4 page", fontSize = 13.sp, color = AppColors.Text)
                Switch(
                    checked = fitPage,
                    onCheckedChange = { fitPage = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AppColors.Accent)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutputFileNameField(
                value = outputName,
                onValueChange = { outputName = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp)
            ) {
                StudioPrimaryButton(
                    text = "Generate PDF from ${imageUris.size} Images",
                    enabled = imageUris.isNotEmpty() && !isProcessing,
                    onClick = {
                        isProcessing = true
                        scope.launch {
                            try {
                                val outFile = fileRepo.createOutputFile(outputName.ifBlank { "images_converted" })
                                val result = PdfEngine.imagesToPdf(context, imageUris.toList(), fitPage, outFile)
                                fileRepo.refreshFiles()
                                isProcessing = false
                                Toast.makeText(context, "PDF Created!", Toast.LENGTH_SHORT).show()
                                onFinished(result)
                            } catch (e: Exception) {
                                isProcessing = false
                                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }
        }
    }

    if (isProcessing) {
        ProcessingDialog(title = "Converting Images to PDF...", statusText = "Embedding and formatting images...")
    }
}

/* ===================================================================================
   8. PDF TO IMAGES SCREEN
   =================================================================================== */
@Composable
fun PdfToImagesScreen(
    fileRepo: FileRepository,
    onBack: () -> Unit,
    onFinished: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var pageCount by remember { mutableIntStateOf(0) }
    var format by remember { mutableStateOf(Bitmap.CompressFormat.PNG) }
    var dpi by remember { mutableIntStateOf(150) }
    var isProcessing by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            fileName = getFileName(context, uri)
            scope.launch {
                pageCount = PdfEngine.getPageCount(context, uri)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        StudioTopBar(
            title = "PDF to Images",
            subtitle = "Export pages as high-resolution images",
            showBackButton = true,
            onBackClick = onBack
        )

        if (selectedUri == null) {
            EmptyFilePickerPlaceholder(
                title = "Select PDF Document",
                description = "Choose a PDF to render into images",
                buttonText = "Pick PDF File",
                onPick = { filePicker.launch("application/pdf") }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                DocHeaderCard(
                    fileName = fileName,
                    pageCount = pageCount,
                    extraInfo = "Exporting all $pageCount pages",
                    onChangeDoc = { filePicker.launch("application/pdf") }
                )

                Spacer(modifier = Modifier.height(14.dp))

                StudioCard {
                    Text("Export Options", fontWeight = FontWeight.Bold, color = AppColors.Text, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Format:", color = AppColors.TextMuted, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(Bitmap.CompressFormat.PNG to "PNG (Lossless)", Bitmap.CompressFormat.JPEG to "JPEG (Smaller size)").forEach { (fmt, label) ->
                            val isSel = format == fmt
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(AppRadius.pill))
                                    .background(if (isSel) AppColors.Accent else AppColors.SurfaceRaised)
                                    .border(1.dp, if (isSel) AppColors.Accent else AppColors.Border, RoundedCornerShape(AppRadius.pill))
                                    .clickable { format = fmt },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, color = if (isSel) Color.White else AppColors.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Resolution / DPI:", color = AppColors.TextMuted, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(150 to "150 DPI (Fast)", 300 to "300 DPI (Crisp Print)").forEach { (d, label) ->
                            val isSel = dpi == d
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(AppRadius.pill))
                                    .background(if (isSel) AppColors.Accent else AppColors.SurfaceRaised)
                                    .border(1.dp, if (isSel) AppColors.Accent else AppColors.Border, RoundedCornerShape(AppRadius.pill))
                                    .clickable { dpi = d },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(label, color = if (isSel) Color.White else AppColors.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 20.dp)
                ) {
                    StudioPrimaryButton(
                        text = "Export $pageCount Pages as Images",
                        enabled = pageCount > 0 && !isProcessing,
                        onClick = {
                            isProcessing = true
                            scope.launch {
                                try {
                                    val outDir = File(context.getExternalFilesDir(null), "CleanPdfTools/Images_${System.currentTimeMillis()}").apply { mkdirs() }
                                    val files = PdfEngine.pdfToImages(context, selectedUri!!, null, format, dpi, outDir)
                                    fileRepo.refreshFiles()
                                    isProcessing = false
                                    Toast.makeText(context, "Exported ${files.size} images!", Toast.LENGTH_SHORT).show()
                                    if (files.isNotEmpty()) onFinished(files.first())
                                } catch (e: Exception) {
                                    isProcessing = false
                                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (isProcessing) {
        ProcessingDialog(title = "Rendering Pages...", statusText = "Converting PDF pages into high-res images...")
    }
}

/* ===================================================================================
   9. CROP PDF SCREEN (WITH INTERACTIVE VISUAL GUIDE)
   =================================================================================== */
@Composable
fun CropPdfScreen(
    fileRepo: FileRepository,
    onBack: () -> Unit,
    onFinished: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var outputName by remember { mutableStateOf("") }
    var pageCount by remember { mutableIntStateOf(0) }
    var currentPage0Based by remember { mutableIntStateOf(0) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var leftPercent by remember { mutableFloatStateOf(0.05f) }
    var topPercent by remember { mutableFloatStateOf(0.05f) }
    var rightPercent by remember { mutableFloatStateOf(0.05f) }
    var bottomPercent by remember { mutableFloatStateOf(0.05f) }
    var applyToAll by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }

    fun loadCurrentPage(uri: Uri, page: Int) {
        scope.launch {
            currentBitmap = PdfEngine.renderPageThumbnail(context, uri, page, 480)
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            val name = getFileName(context, uri)
            fileName = name
            outputName = "cropped_${name.removeSuffix(".pdf")}"
            currentPage0Based = 0
            scope.launch {
                pageCount = PdfEngine.getPageCount(context, uri)
                loadCurrentPage(uri, 0)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        StudioTopBar(
            title = "Crop PDF",
            subtitle = "Visual guide to crop margins & white space",
            showBackButton = true,
            onBackClick = onBack
        )

        if (selectedUri == null) {
            EmptyFilePickerPlaceholder(
                title = "Select PDF Document",
                description = "Choose a PDF to crop with interactive visual guide",
                buttonText = "Pick PDF File",
                onPick = { filePicker.launch("application/pdf") }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Previewing Page ${currentPage0Based + 1} of $pageCount",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Text
                    )
                    Row {
                        IconButton(
                            onClick = {
                                if (currentPage0Based > 0) {
                                    currentPage0Based--
                                    loadCurrentPage(selectedUri!!, currentPage0Based)
                                }
                            },
                            enabled = currentPage0Based > 0
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Page", tint = if (currentPage0Based > 0) AppColors.Text else AppColors.TextFaint)
                        }
                        IconButton(
                            onClick = {
                                if (currentPage0Based < pageCount - 1) {
                                    currentPage0Based++
                                    loadCurrentPage(selectedUri!!, currentPage0Based)
                                }
                            },
                            enabled = currentPage0Based < pageCount - 1
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Page", tint = if (currentPage0Based < pageCount - 1) AppColors.Text else AppColors.TextFaint)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                InteractiveCropGuide(
                    bitmap = currentBitmap,
                    leftPercent = leftPercent,
                    topPercent = topPercent,
                    rightPercent = rightPercent,
                    bottomPercent = bottomPercent,
                    onMarginsChange = { l, t, r, b ->
                        leftPercent = l
                        topPercent = t
                        rightPercent = r
                        bottomPercent = b
                    },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AppRadius.md))
                        .background(AppColors.Surface)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Apply crop to all pages", fontSize = 13.sp, color = AppColors.Text)
                    Switch(
                        checked = applyToAll,
                        onCheckedChange = { applyToAll = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AppColors.Accent)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutputFileNameField(
                    value = outputName,
                    onValueChange = { outputName = it }
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 20.dp)
                ) {
                    StudioPrimaryButton(
                        text = "Crop & Save PDF",
                        enabled = !isProcessing,
                        onClick = {
                            isProcessing = true
                            scope.launch {
                                try {
                                    val outFile = fileRepo.createOutputFile(outputName.ifBlank { "cropped_${fileName.removeSuffix(".pdf")}" })
                                    val targets = if (applyToAll) null else setOf(currentPage0Based + 1)
                                    val result = PdfEngine.cropPdf(
                                        context = context,
                                        uri = selectedUri!!,
                                        leftPercent = leftPercent,
                                        topPercent = topPercent,
                                        rightPercent = rightPercent,
                                        bottomPercent = bottomPercent,
                                        pages1Based = targets,
                                        outputFile = outFile
                                    )
                                    fileRepo.refreshFiles()
                                    isProcessing = false
                                    Toast.makeText(context, "Cropped successfully!", Toast.LENGTH_SHORT).show()
                                    onFinished(result)
                                } catch (e: Exception) {
                                    isProcessing = false
                                    Toast.makeText(context, "Crop error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (isProcessing) {
        ProcessingDialog(title = "Cropping PDF...", statusText = "Applying precision margins...")
    }
}

/* ===================================================================================
   10. COMPRESS PDF SCREEN (WITH EXTREME COMPRESSION OPTION)
   =================================================================================== */
@Composable
fun CompressPdfScreen(
    fileRepo: FileRepository,
    onBack: () -> Unit,
    onFinished: (File) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var outputName by remember { mutableStateOf("") }
    var pageCount by remember { mutableIntStateOf(0) }
    var originalSizeBytes by remember { mutableLongStateOf(0L) }
    var selectedLevel by remember { mutableStateOf(CompressionLevel.MEDIUM) }
    var isProcessing by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            val name = getFileName(context, uri)
            fileName = name
            outputName = "compressed_${name.removeSuffix(".pdf")}"
            scope.launch {
                try {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        originalSizeBytes = pfd.statSize
                    }
                } catch (_: Exception) {}
                pageCount = PdfEngine.getPageCount(context, uri)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        StudioTopBar(
            title = "Compress PDF",
            subtitle = "Optimize and shrink document file size",
            showBackButton = true,
            onBackClick = onBack
        )

        if (selectedUri == null) {
            EmptyFilePickerPlaceholder(
                title = "Select PDF Document",
                description = "Choose a PDF to compress and reduce file size",
                buttonText = "Pick PDF File",
                onPick = { filePicker.launch("application/pdf") }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                DocHeaderCard(
                    fileName = fileName,
                    pageCount = pageCount,
                    extraInfo = "Original Size: ${formatFileSize(originalSizeBytes)}",
                    onChangeDoc = { filePicker.launch("application/pdf") }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Select Compression Level:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppColors.Text)

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(CompressionLevel.entries) { _, level ->
                        val isSel = selectedLevel == level
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(AppRadius.card))
                                .background(if (isSel) Color(0xFF161333) else AppColors.Surface)
                                .border(1.dp, if (isSel) AppColors.Accent else AppColors.Border, RoundedCornerShape(AppRadius.card))
                                .clickable { selectedLevel = level }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSel,
                                    onClick = { selectedLevel = level },
                                    colors = RadioButtonDefaults.colors(selectedColor = AppColors.Accent)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(level.title, fontWeight = FontWeight.Bold, color = AppColors.Text, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(level.description, color = AppColors.TextMuted, fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutputFileNameField(
                            value = outputName,
                            onValueChange = { outputName = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 20.dp)
                ) {
                    StudioPrimaryButton(
                        text = "Compress PDF",
                        enabled = !isProcessing,
                        onClick = {
                            isProcessing = true
                            scope.launch {
                                try {
                                    val outFile = fileRepo.createOutputFile(outputName.ifBlank { "compressed_${fileName.removeSuffix(".pdf")}" })
                                    val result = PdfEngine.compressPdf(context, selectedUri!!, selectedLevel, outFile)
                                    fileRepo.refreshFiles()
                                    isProcessing = false
                                    val savedPct = if (originalSizeBytes > 0) {
                                        val saved = ((originalSizeBytes - result.length()).toFloat() / originalSizeBytes * 100).toInt()
                                        "Saved ~$saved% space!"
                                    } else "Compressed successfully!"
                                    Toast.makeText(context, savedPct, Toast.LENGTH_SHORT).show()
                                    onFinished(result)
                                } catch (e: Exception) {
                                    isProcessing = false
                                    Toast.makeText(context, "Compression error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (isProcessing) {
        ProcessingDialog(title = "Compressing PDF...", statusText = "Optimizing page streams and images...")
    }
}

/* ===================================================================================
   SHARED COMMON SUB-COMPONENTS
   =================================================================================== */

@Composable
fun OutputFileNameField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Output File Name", color = AppColors.TextMuted, fontSize = 12.sp) },
        singleLine = true,
        trailingIcon = {
            Text(".pdf", color = AppColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(end = 12.dp))
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppColors.Accent,
            unfocusedBorderColor = AppColors.Border,
            focusedTextColor = AppColors.Text,
            unfocusedTextColor = AppColors.Text,
            focusedContainerColor = AppColors.Surface,
            unfocusedContainerColor = AppColors.Surface
        ),
        shape = RoundedCornerShape(AppRadius.md),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun EmptyFilePickerPlaceholder(
    title: String,
    description: String,
    buttonText: String,
    onPick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        StudioCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(AppColors.SurfaceRaised)
                        .border(1.dp, AppColors.Border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = AppColors.Accent,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.Text
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = AppColors.TextMuted,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onPick,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent),
                    shape = RoundedCornerShape(AppRadius.pill),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(buttonText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun DocHeaderCard(
    fileName: String,
    pageCount: Int,
    extraInfo: String,
    onChangeDoc: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadius.card))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.card))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Description, contentDescription = null, tint = AppColors.Accent, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(fileName, fontWeight = FontWeight.Bold, color = AppColors.Text, fontSize = 14.sp, maxLines = 1)
                Spacer(modifier = Modifier.height(2.dp))
                Text("$pageCount pages • $extraInfo", color = AppColors.TextMuted, fontSize = 12.sp)
            }
            TextButton(onClick = onChangeDoc) {
                Text("Change", color = AppColors.Warning, fontSize = 12.sp)
            }
        }
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 KB"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb >= 1.0) String.format("%.2f MB", mb) else String.format("%.0f KB", kb)
}
