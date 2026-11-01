package com.cleanpdftools.app.data

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.tom_roush.pdfbox.pdmodel.PDDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class FileRepository(private val context: Context) {

    private val _recentFiles = MutableStateFlow<List<ProcessedPdfFile>>(emptyList())
    val recentFiles: StateFlow<List<ProcessedPdfFile>> = _recentFiles.asStateFlow()

    private val outputDir: File
        get() {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "CleanPdfTools")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    fun createOutputFile(preferredName: String, extension: String = "pdf"): File {
        val cleanName = preferredName
            .removeSuffix(".pdf")
            .removeSuffix(".png")
            .removeSuffix(".jpg")
            .replace("[^a-zA-Z0-9_\\-\\s]".toRegex(), "_")
            .trim()
            .ifBlank { "CleanPdfDocument" }

        var target = File(outputDir, "$cleanName.$extension")
        var counter = 1
        while (target.exists()) {
            target = File(outputDir, "${cleanName}_$counter.$extension")
            counter++
        }
        return target
    }

    suspend fun renameFile(file: File, newNameWithoutExt: String): File? = withContext(Dispatchers.IO) {
        val cleanName = newNameWithoutExt
            .removeSuffix(".pdf")
            .removeSuffix(".png")
            .removeSuffix(".jpg")
            .replace("[^a-zA-Z0-9_\\-\\s]".toRegex(), "_")
            .trim()

        if (cleanName.isBlank()) return@withContext null

        val ext = file.extension
        var newFile = File(file.parentFile, "$cleanName.$ext")
        if (newFile.absolutePath == file.absolutePath) return@withContext file

        var counter = 1
        while (newFile.exists()) {
            newFile = File(file.parentFile, "${cleanName}_$counter.$ext")
            counter++
        }

        val success = file.renameTo(newFile)
        if (success) {
            refreshFiles()
            newFile
        } else {
            null
        }
    }

    suspend fun refreshFiles() = withContext(Dispatchers.IO) {
        val files = outputDir.listFiles { file ->
            file.isFile && (file.extension.equals("pdf", ignoreCase = true) || file.extension.equals("png", ignoreCase = true) || file.extension.equals("jpg", ignoreCase = true))
        }?.sortedByDescending { it.lastModified() } ?: emptyList()

        val list = files.map { file ->
            var pages = 0
            if (file.extension.equals("pdf", ignoreCase = true)) {
                try {
                    val doc = PDDocument.load(file)
                    pages = doc.numberOfPages
                    doc.close()
                } catch (_: Exception) {
                    pages = 1
                }
            }
            ProcessedPdfFile(
                file = file,
                name = file.name,
                sizeBytes = file.length(),
                modifiedTime = file.lastModified(),
                pageCount = pages
            )
        }
        _recentFiles.value = list
    }

    suspend fun deleteFile(file: File): Boolean = withContext(Dispatchers.IO) {
        val deleted = file.delete()
        if (deleted) {
            refreshFiles()
        }
        deleted
    }

    fun shareFile(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val mimeType = if (file.extension.equals("pdf", ignoreCase = true)) "application/pdf" else "image/*"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share via Clean PDF Tools")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun openFile(file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val mimeType = if (file.extension.equals("pdf", ignoreCase = true)) "application/pdf" else "image/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            shareFile(file)
        }
    }

    /**
     * Directly saves to the root Download directory, not a subfolder.
     */
    suspend fun saveToDownloads(file: File): Uri? = withContext(Dispatchers.IO) {
        try {
            val mimeType = if (file.extension.equals("pdf", ignoreCase = true)) "application/pdf" else "image/png"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(file).use { input ->
                            input.copyTo(out)
                        }
                    }
                    return@withContext uri
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val targetFile = File(downloadsDir, file.name)
                FileInputStream(file).use { input ->
                    FileOutputStream(targetFile).use { out ->
                        input.copyTo(out)
                    }
                }
                return@withContext Uri.fromFile(targetFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }
}
