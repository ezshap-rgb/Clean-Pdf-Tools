package com.cleanpdftools.app.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cleanpdftools.app.theme.AppColors
import com.cleanpdftools.app.theme.AppRadius
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun EmbeddedPdfViewerDialog(
    file: File,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onSaveToDownloads: () -> Unit
) {
    val context = LocalContext.current
    var totalPages by remember { mutableIntStateOf(0) }
    val renderedPages = remember { mutableStateMapOf<Int, Bitmap?>() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Load PDF or image pages
    LaunchedEffect(file) {
        withContext(Dispatchers.IO) {
            val isPdf = file.extension.equals("pdf", ignoreCase = true)
            if (isPdf) {
                var pfd: ParcelFileDescriptor? = null
                var renderer: PdfRenderer? = null
                try {
                    pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    renderer = PdfRenderer(pfd)
                    totalPages = renderer.pageCount

                    val displayMetrics = context.resources.displayMetrics
                    val screenWidthPx = displayMetrics.widthPixels

                    for (i in 0 until totalPages) {
                        val page = renderer.openPage(i)
                        val aspectRatio = page.height.toFloat() / page.width.toFloat()
                        val targetW = minOf(screenWidthPx, (page.width * 2f).roundToInt())
                        val targetH = max(1, (targetW * aspectRatio).roundToInt())

                        val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()

                        withContext(Dispatchers.Main) {
                            renderedPages[i] = bitmap
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    try { renderer?.close() } catch (_: Exception) {}
                    try { pfd?.close() } catch (_: Exception) {}
                }
            } else {
                // Image file (PNG, JPG, etc.)
                try {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        totalPages = 1
                        withContext(Dispatchers.Main) {
                            renderedPages[0] = bitmap
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Clean up bitmaps when dialog is dismissed
    DisposableEffect(Unit) {
        onDispose {
            renderedPages.values.forEach { it?.recycle() }
            renderedPages.clear()
        }
    }

    val visibleItemIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex + 1 }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Background)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(AppRadius.md))
                                .background(AppColors.Surface)
                                .border(1.dp, AppColors.Border, RoundedCornerShape(AppRadius.md))
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = AppColors.Text)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = file.name,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.Text,
                                fontSize = 15.sp,
                                maxLines = 1
                            )
                            if (totalPages > 0) {
                                Text(
                                    text = "Page $visibleItemIndex of $totalPages",
                                    color = AppColors.Accent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Row {
                        IconButton(onClick = onShare) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = AppColors.Text)
                        }
                        IconButton(onClick = onSaveToDownloads) {
                            Icon(Icons.Default.Download, contentDescription = "Download", tint = AppColors.Text)
                        }
                    }
                }

                // Page Stream View
                if (totalPages == 0) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AppColors.Accent)
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(totalPages) { pageIdx ->
                            val bitmap = renderedPages[pageIdx]
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(androidx.compose.ui.graphics.Color.White)
                                        .border(1.dp, AppColors.Border, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (bitmap != null && !bitmap.isRecycled) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "Page ${pageIdx + 1}",
                                            contentScale = ContentScale.FillWidth,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(350.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(32.dp),
                                                color = AppColors.Accent
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "- ${pageIdx + 1} -",
                                    color = AppColors.TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

