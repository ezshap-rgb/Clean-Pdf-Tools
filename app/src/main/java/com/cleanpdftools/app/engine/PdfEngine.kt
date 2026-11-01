package com.cleanpdftools.app.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.cleanpdftools.app.data.CompressionLevel
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.roundToInt

object PdfEngine {

    /**
     * Efficiently copy a content or file URI to a private temp cache file
     */
    fun copyUriToTemp(context: Context, uri: Uri, prefix: String = "input_temp"): File {
        val tempFile = File.createTempFile(prefix, ".pdf", context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalArgumentException("Could not open input stream for $uri")
        return tempFile
    }

    suspend fun getPageCount(context: Context, uri: Uri): Int = withContext(Dispatchers.IO) {
        val temp = copyUriToTemp(context, uri)
        try {
            PDDocument.load(temp).use { doc ->
                doc.numberOfPages
            }
        } finally {
            temp.delete()
        }
    }

    /**
     * Render a page thumbnail using Android native PdfRenderer for high performance and low memory
     */
    suspend fun renderPageThumbnail(
        context: Context,
        uri: Uri,
        pageIndex0Based: Int,
        targetWidth: Int = 300
    ): Bitmap? = withContext(Dispatchers.IO) {
        val temp = copyUriToTemp(context, uri)
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            if (pageIndex0Based !in 0 until renderer.pageCount) return@withContext null

            val page = renderer.openPage(pageIndex0Based)
            val aspectRatio = page.height.toFloat() / page.width.toFloat()
            val targetHeight = max(1, (targetWidth * aspectRatio).roundToInt())

            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
            temp.delete()
        }
    }

    /**
     * Delete specific 1-based pages
     */
    suspend fun deletePages(
        context: Context,
        uri: Uri,
        pagesToDelete1Based: Set<Int>,
        outputFile: File
    ): File = withContext(Dispatchers.IO) {
        val temp = copyUriToTemp(context, uri)
        try {
            PDDocument.load(temp).use { doc ->
                val total = doc.numberOfPages
                val indicesToDeleteDesc = pagesToDelete1Based
                    .map { it - 1 }
                    .filter { it in 0 until total }
                    .sortedDescending()

                if (indicesToDeleteDesc.size >= total) {
                    throw IllegalStateException("Cannot delete all pages of the document.")
                }

                for (idx in indicesToDeleteDesc) {
                    doc.removePage(idx)
                }
                doc.save(outputFile)
            }
            outputFile
        } finally {
            temp.delete()
        }
    }

    /**
     * Merge multiple PDF files in order
     */
    suspend fun mergePdfs(
        context: Context,
        uris: List<Uri>,
        outputFile: File
    ): File = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) throw IllegalArgumentException("No PDF files selected to merge.")
        val tempFiles = uris.mapIndexed { idx, u -> copyUriToTemp(context, u, "merge_$idx") }
        try {
            val merger = PDFMergerUtility().apply {
                destinationFileName = outputFile.absolutePath
            }
            for (f in tempFiles) {
                merger.addSource(f)
            }
            merger.mergeDocuments(null)
            outputFile
        } finally {
            tempFiles.forEach { it.delete() }
        }
    }

    /**
     * Reorder pages according to specified 1-based page order
     */
    suspend fun reorderPages(
        context: Context,
        uri: Uri,
        newOrder1Based: List<Int>,
        outputFile: File
    ): File = withContext(Dispatchers.IO) {
        val temp = copyUriToTemp(context, uri)
        try {
            PDDocument.load(temp).use { srcDoc ->
                val newDoc = PDDocument()
                for (page1Based in newOrder1Based) {
                    val idx = page1Based - 1
                    if (idx in 0 until srcDoc.numberOfPages) {
                        newDoc.importPage(srcDoc.getPage(idx))
                    }
                }
                newDoc.save(outputFile)
                newDoc.close()
            }
            outputFile
        } finally {
            temp.delete()
        }
    }

    /**
     * Extract specific 1-based pages into a new document
     */
    suspend fun extractPages(
        context: Context,
        uri: Uri,
        pagesToExtract1Based: List<Int>,
        outputFile: File
    ): File = withContext(Dispatchers.IO) {
        reorderPages(context, uri, pagesToExtract1Based, outputFile)
    }

    /**
     * Split PDF into two documents at splitAfterPage1Based (1..splitAfter and (splitAfter+1)..total)
     */
    suspend fun splitPdf(
        context: Context,
        uri: Uri,
        splitAfterPage1Based: Int,
        part1OutputFile: File,
        part2OutputFile: File
    ): Pair<File, File> = withContext(Dispatchers.IO) {
        val temp = copyUriToTemp(context, uri)
        try {
            PDDocument.load(temp).use { srcDoc ->
                val total = srcDoc.numberOfPages
                if (splitAfterPage1Based < 1 || splitAfterPage1Based >= total) {
                    throw IllegalArgumentException("Split page must be between 1 and ${total - 1}")
                }

                val doc1 = PDDocument()
                val doc2 = PDDocument()
                for (i in 0 until total) {
                    val page = srcDoc.getPage(i)
                    if (i < splitAfterPage1Based) {
                        doc1.importPage(page)
                    } else {
                        doc2.importPage(page)
                    }
                }
                doc1.save(part1OutputFile)
                doc1.close()

                doc2.save(part2OutputFile)
                doc2.close()
            }
            Pair(part1OutputFile, part2OutputFile)
        } finally {
            temp.delete()
        }
    }

    /**
     * Rotate pages (90, 180, 270 degrees). If targetPages1Based is null or empty, rotates all pages.
     */
    suspend fun rotatePages(
        context: Context,
        uri: Uri,
        rotationDegrees: Int,
        targetPages1Based: Set<Int>? = null,
        outputFile: File
    ): File = withContext(Dispatchers.IO) {
        val temp = copyUriToTemp(context, uri)
        try {
            PDDocument.load(temp).use { doc ->
                val total = doc.numberOfPages
                val targets = targetPages1Based?.map { it - 1 }?.toSet()

                for (i in 0 until total) {
                    if (targets == null || i in targets) {
                        val page = doc.getPage(i)
                        val newRotation = (page.rotation + rotationDegrees) % 360
                        page.rotation = if (newRotation < 0) newRotation + 360 else newRotation
                    }
                }
                doc.save(outputFile)
            }
            outputFile
        } finally {
            temp.delete()
        }
    }

    /**
     * Convert images to a single PDF document
     */
    suspend fun imagesToPdf(
        context: Context,
        imageUris: List<Uri>,
        fitPage: Boolean = true,
        outputFile: File
    ): File = withContext(Dispatchers.IO) {
        val doc = PDDocument()
        try {
            for (imgUri in imageUris) {
                var stream: InputStream? = null
                var bitmap: Bitmap? = null
                try {
                    stream = context.contentResolver.openInputStream(imgUri)
                    bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        val pdImage = JPEGFactory.createFromImage(doc, bitmap, 0.9f)
                        val pageWidth = if (fitPage) PDRectangle.A4.width else pdImage.width.toFloat()
                        val pageHeight = if (fitPage) PDRectangle.A4.height else pdImage.height.toFloat()

                        val page = PDPage(PDRectangle(pageWidth, pageHeight))
                        doc.addPage(page)

                        PDPageContentStream(doc, page).use { cs ->
                            if (fitPage) {
                                val scale = minOf(pageWidth / pdImage.width, pageHeight / pdImage.height)
                                val drawW = pdImage.width * scale
                                val drawH = pdImage.height * scale
                                val offsetX = (pageWidth - drawW) / 2f
                                val offsetY = (pageHeight - drawH) / 2f
                                cs.drawImage(pdImage, offsetX, offsetY, drawW, drawH)
                            } else {
                                cs.drawImage(pdImage, 0f, 0f, pdImage.width.toFloat(), pdImage.height.toFloat())
                            }
                        }
                    }
                } finally {
                    stream?.close()
                    bitmap?.recycle()
                }
            }
            doc.save(outputFile)
            outputFile
        } finally {
            doc.close()
        }
    }

    /**
     * Convert PDF pages to images (PNG or JPEG)
     */
    suspend fun pdfToImages(
        context: Context,
        uri: Uri,
        pages1Based: List<Int>? = null,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        dpi: Int = 150,
        outputDir: File
    ): List<File> = withContext(Dispatchers.IO) {
        val temp = copyUriToTemp(context, uri)
        val createdFiles = mutableListOf<File>()
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            pfd = ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val total = renderer.pageCount
            val targetPages0Based = pages1Based?.map { it - 1 }?.filter { it in 0 until total }
                ?: (0 until total).toList()

            val scale = dpi.toFloat() / 72f
            val ext = if (format == Bitmap.CompressFormat.JPEG) "jpg" else "png"

            for (pageIdx in targetPages0Based) {
                val page = renderer.openPage(pageIdx)
                val width = max(1, (page.width * scale).roundToInt())
                val height = max(1, (page.height * scale).roundToInt())

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                val outFile = File(outputDir, "page_${pageIdx + 1}_${System.currentTimeMillis()}.$ext")
                FileOutputStream(outFile).use { out ->
                    bitmap.compress(format, 90, out)
                }
                bitmap.recycle()
                createdFiles.add(outFile)
            }
            createdFiles
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
            temp.delete()
        }
    }

    /**
     * Crop PDF with normalized margin percentages (0.0 .. 1.0)
     */
    suspend fun cropPdf(
        context: Context,
        uri: Uri,
        leftPercent: Float,
        topPercent: Float,
        rightPercent: Float,
        bottomPercent: Float,
        pages1Based: Set<Int>? = null,
        outputFile: File
    ): File = withContext(Dispatchers.IO) {
        val temp = copyUriToTemp(context, uri)
        try {
            PDDocument.load(temp).use { doc ->
                val total = doc.numberOfPages
                val targets = pages1Based?.map { it - 1 }?.toSet()

                for (i in 0 until total) {
                    if (targets == null || i in targets) {
                        val page = doc.getPage(i)
                        val box = page.cropBox ?: page.mediaBox

                        val cropL = box.lowerLeftX + (box.width * leftPercent.coerceIn(0f, 0.49f))
                        val cropB = box.lowerLeftY + (box.height * bottomPercent.coerceIn(0f, 0.49f))
                        val cropR = box.upperRightX - (box.width * rightPercent.coerceIn(0f, 0.49f))
                        val cropT = box.upperRightY - (box.height * topPercent.coerceIn(0f, 0.49f))

                        val width = max(10f, cropR - cropL)
                        val height = max(10f, cropT - cropB)

                        page.cropBox = PDRectangle(cropL, cropB, width, height)
                    }
                }
                doc.save(outputFile)
            }
            outputFile
        } finally {
            temp.delete()
        }
    }

    /**
     * Compress PDF by re-rendering and re-encoding high resolution pages or streams
     */
    suspend fun compressPdf(
        context: Context,
        uri: Uri,
        level: CompressionLevel,
        outputFile: File
    ): File = withContext(Dispatchers.IO) {
        val temp = copyUriToTemp(context, uri)
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        val doc = PDDocument()
        try {
            pfd = ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val total = renderer.pageCount
            val scale = level.targetDpi.toFloat() / 72f

            for (i in 0 until total) {
                val page = renderer.openPage(i)
                val width = max(1, (page.width * scale).roundToInt())
                val height = max(1, (page.height * scale).roundToInt())

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                val pdImage = JPEGFactory.createFromImage(doc, bitmap, level.quality / 100f)
                val p = PDPage(PDRectangle(page.width.toFloat(), page.height.toFloat()))
                doc.addPage(p)

                PDPageContentStream(doc, p).use { cs ->
                    cs.drawImage(pdImage, 0f, 0f, page.width.toFloat(), page.height.toFloat())
                }
                bitmap.recycle()
            }
            doc.save(outputFile)
            outputFile
        } finally {
            try { doc.close() } catch (_: Exception) {}
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
            temp.delete()
        }
    }
}

