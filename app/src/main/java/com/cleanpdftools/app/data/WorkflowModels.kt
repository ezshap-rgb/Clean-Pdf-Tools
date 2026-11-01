package com.cleanpdftools.app.data

import android.net.Uri

sealed interface WorkflowStepConfig {
    val title: String
    val description: String

    data class DeletePages(
        val pageNumbers1Based: Set<Int>
    ) : WorkflowStepConfig {
        override val title = "Delete Pages"
        override val description = "Remove pages: ${pageNumbers1Based.sorted().joinToString(", ")}"
    }

    data class Rotate(
        val angleDegrees: Int,
        val targetPages1Based: Set<Int>? = null // null means all
    ) : WorkflowStepConfig {
        override val title = "Rotate"
        override val description = "${angleDegrees}° CW on ${if (targetPages1Based.isNullOrEmpty()) "All Pages" else "Pages ${targetPages1Based.joinToString(", ")}"}"
    }

    data class Crop(
        val leftPercent: Float,
        val topPercent: Float,
        val rightPercent: Float,
        val bottomPercent: Float,
        val targetPages1Based: Set<Int>? = null
    ) : WorkflowStepConfig {
        override val title = "Crop Margins"
        override val description = "L: ${(leftPercent * 100).toInt()}%, T: ${(topPercent * 100).toInt()}%, R: ${(rightPercent * 100).toInt()}%, B: ${(bottomPercent * 100).toInt()}%"
    }

    data class ExtractPages(
        val pageNumbers1Based: List<Int>
    ) : WorkflowStepConfig {
        override val title = "Extract Pages"
        override val description = "Keep pages: ${pageNumbers1Based.joinToString(", ")}"
    }

    data class InsertPdf(
        val insertUri: Uri,
        val insertDocName: String,
        val insertAfterPage1Based: Int // 0 means at beginning
    ) : WorkflowStepConfig {
        override val title = "Insert PDF"
        override val description = "Insert '$insertDocName' after page $insertAfterPage1Based"
    }

    data class Compress(
        val level: CompressionLevel
    ) : WorkflowStepConfig {
        override val title = "Compress"
        override val description = level.title
    }
}

data class WorkflowStepItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val config: WorkflowStepConfig
)

data class PipelineExecutionProgress(
    val currentStepIndex: Int,
    val totalSteps: Int,
    val stepDescription: String,
    val isFinished: Boolean = false,
    val errorMessage: String? = null
)

