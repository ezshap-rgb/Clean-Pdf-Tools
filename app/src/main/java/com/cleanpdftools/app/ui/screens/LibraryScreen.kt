package com.cleanpdftools.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.cleanpdftools.app.data.FileRepository
import com.cleanpdftools.app.data.ProcessedPdfFile
import com.cleanpdftools.app.theme.AppColors
import com.cleanpdftools.app.theme.AppRadius
import com.cleanpdftools.app.ui.components.EmbeddedPdfViewerDialog
import com.cleanpdftools.app.ui.components.StudioCard
import com.cleanpdftools.app.ui.components.StudioTopBar
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LibraryScreen(
    fileRepo: FileRepository,
    onNavigateToTools: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val files by fileRepo.recentFiles.collectAsState()

    var fileToDelete by remember { mutableStateOf<ProcessedPdfFile?>(null) }
    var fileToRename by remember { mutableStateOf<ProcessedPdfFile?>(null) }
    var renameInputText by remember { mutableStateOf("") }
    var viewingFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(Unit) {
        fileRepo.refreshFiles()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        StudioTopBar(
            title = "Studio Library",
            subtitle = "${files.size} Processed Document(s)",
            showBackButton = false
        )

        if (files.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                StudioCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
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
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = AppColors.TextMuted,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "No Processed Files Yet",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.Text
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Use any tool or workflow to generate and save clean PDF documents here",
                            fontSize = 13.sp,
                            color = AppColors.TextMuted,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = onNavigateToTools,
                            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent),
                            shape = RoundedCornerShape(AppRadius.pill)
                        ) {
                            Text("Open PDF Tools", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(files, key = { it.file.absolutePath }) { item ->
                    FileLibraryCard(
                        item = item,
                        onView = {
                            if (item.name.endsWith(".pdf", ignoreCase = true)) {
                                viewingFile = item.file
                            } else {
                                fileRepo.openFile(item.file)
                            }
                        },
                        onRename = {
                            fileToRename = item
                            renameInputText = item.name.substringBeforeLast(".")
                        },
                        onShare = { fileRepo.shareFile(item.file) },
                        onSaveToDownloads = {
                            scope.launch {
                                val uri = fileRepo.saveToDownloads(item.file)
                                if (uri != null) {
                                    Toast.makeText(context, "Saved to Downloads", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to download", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onDelete = { fileToDelete = item }
                    )
                }
            }
        }
    }

    // Embedded PDF Viewer Dialog
    if (viewingFile != null) {
        EmbeddedPdfViewerDialog(
            file = viewingFile!!,
            onDismiss = { viewingFile = null },
            onShare = { viewingFile?.let { fileRepo.shareFile(it) } },
            onSaveToDownloads = {
                viewingFile?.let { f ->
                    scope.launch {
                        val uri = fileRepo.saveToDownloads(f)
                        if (uri != null) {
                            Toast.makeText(context, "Saved to Downloads", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }

    // Rename Dialog
    if (fileToRename != null) {
        AlertDialog(
            onDismissRequest = { fileToRename = null },
            title = { Text("Rename Document", color = AppColors.Text, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter new file name:", color = AppColors.TextMuted, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = renameInputText,
                        onValueChange = { renameInputText = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppColors.Accent,
                            unfocusedBorderColor = AppColors.Border,
                            focusedTextColor = AppColors.Text,
                            unfocusedTextColor = AppColors.Text
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = fileToRename
                        val newName = renameInputText.trim()
                        fileToRename = null
                        if (target != null && newName.isNotBlank()) {
                            scope.launch {
                                val res = fileRepo.renameFile(target.file, newName)
                                if (res != null) {
                                    Toast.makeText(context, "Renamed to ${res.name}", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Rename failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToRename = null }) {
                    Text("Cancel", color = AppColors.TextMuted)
                }
            },
            containerColor = AppColors.Surface,
            shape = RoundedCornerShape(AppRadius.card)
        )
    }

    // Delete Confirmation Dialog
    if (fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Delete Document?", color = AppColors.Text, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete '${fileToDelete?.name}'?", color = AppColors.TextMuted) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = fileToDelete
                        fileToDelete = null
                        if (target != null) {
                            scope.launch {
                                fileRepo.deleteFile(target.file)
                                Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Delete", color = AppColors.Danger, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancel", color = AppColors.TextMuted)
                }
            },
            containerColor = AppColors.Surface,
            shape = RoundedCornerShape(AppRadius.card)
        )
    }
}

@Composable
private fun FileLibraryCard(
    item: ProcessedPdfFile,
    onView: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onSaveToDownloads: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()) }
    val formattedDate = remember(item.modifiedTime) { dateFormat.format(Date(item.modifiedTime)) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadius.card))
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.card))
            .clickable { onView() }
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppColors.Accent.copy(alpha = 0.15f))
                        .border(1.dp, AppColors.Accent.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.name.endsWith(".pdf", ignoreCase = true)) Icons.Default.PictureAsPdf else Icons.Default.Image,
                        contentDescription = null,
                        tint = AppColors.Accent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Text,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$formattedDate • ${formatFileSize(item.sizeBytes)}",
                        fontSize = 11.sp,
                        color = AppColors.TextMuted
                    )
                }

                if (item.pageCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(AppRadius.pill))
                            .background(AppColors.SurfaceRaised)
                            .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.pill))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${item.pageCount} pgs",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.Accent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row: View, Rename, Share, Download, Delete
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // In-App View
                IconButton(onClick = onView, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Visibility, contentDescription = "View", tint = AppColors.Accent, modifier = Modifier.size(18.dp))
                }

                // Rename
                IconButton(onClick = onRename, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename", tint = AppColors.Text, modifier = Modifier.size(18.dp))
                }

                // Share
                IconButton(onClick = onShare, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = AppColors.Text, modifier = Modifier.size(18.dp))
                }

                // Save to downloads
                IconButton(onClick = onSaveToDownloads, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Download, contentDescription = "Download", tint = AppColors.Text, modifier = Modifier.size(18.dp))
                }

                // Delete
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = AppColors.Danger, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
