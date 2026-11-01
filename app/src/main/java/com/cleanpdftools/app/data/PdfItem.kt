package com.cleanpdftools.app.data

import android.net.Uri
import java.io.File

data class PdfDocumentItem(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val pageCount: Int = 0
)

data class PdfPageItem(
    val pageNumber: Int, // 1-based
    val isSelected: Boolean = false,
    val rotation: Int = 0 // degrees
)

data class ProcessedPdfFile(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val modifiedTime: Long,
    val pageCount: Int
)

enum class CompressionLevel(val title: String, val description: String, val quality: Int, val targetDpi: Int) {
    LOW("Low Compression", "High visual quality, minimal compression", 85, 200),
    MEDIUM("Medium (Recommended)", "Balanced quality and significant size reduction", 70, 150),
    HIGH("High Compression", "Strong size reduction for email and sharing", 50, 100),
    EXTREME("Extreme (Basic Docs / Text)", "Aggressive shrink (~72 DPI) for invoices, receipts & text", 30, 72)
}
