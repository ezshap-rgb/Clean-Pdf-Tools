package com.cleanpdftools.app.engine

import android.content.Context
import android.net.Uri
import com.cleanpdftools.app.data.PipelineExecutionProgress
import com.cleanpdftools.app.data.WorkflowStepConfig
import com.cleanpdftools.app.data.WorkflowStepItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object WorkflowEngine {

    /**
     * Executes a chain of sequential workflow steps on a PDF, passing intermediate results.
     */
    suspend fun executePipeline(
        context: Context,
        initialUri: Uri,
        steps: List<WorkflowStepItem>,
        finalOutputFile: File,
        onProgress: (PipelineExecutionProgress) -> Unit
    ): File = withContext(Dispatchers.IO) {
        if (steps.isEmpty()) {
            val temp = PdfEngine.copyUriToTemp(context, initialUri)
            temp.copyTo(finalOutputFile, overwrite = true)
            temp.delete()
            return@withContext finalOutputFile
        }

        var currentInputFile: File = PdfEngine.copyUriToTemp(context, initialUri, "wf_initial")
        val intermediateFiles = mutableListOf<File>()

        try {
            for ((index, stepItem) in steps.withIndex()) {
                val stepConfig = stepItem.config
                val isLast = index == steps.lastIndex
                val stepOut = if (isLast) finalOutputFile else File.createTempFile("wf_step_${index}_", ".pdf", context.cacheDir)
                if (!isLast) intermediateFiles.add(stepOut)

                onProgress(
                    PipelineExecutionProgress(
                        currentStepIndex = index + 1,
                        totalSteps = steps.size,
                        stepDescription = "Step ${index + 1}/${steps.size}: ${stepConfig.title}"
                    )
                )

                val inputUri = Uri.fromFile(currentInputFile)

                when (stepConfig) {
                    is WorkflowStepConfig.DeletePages -> {
                        PdfEngine.deletePages(context, inputUri, stepConfig.pageNumbers1Based, stepOut)
                    }
                    is WorkflowStepConfig.Rotate -> {
                        PdfEngine.rotatePages(context, inputUri, stepConfig.angleDegrees, stepConfig.targetPages1Based, stepOut)
                    }
                    is WorkflowStepConfig.Crop -> {
                        PdfEngine.cropPdf(
                            context,
                            inputUri,
                            stepConfig.leftPercent,
                            stepConfig.topPercent,
                            stepConfig.rightPercent,
                            stepConfig.bottomPercent,
                            stepConfig.targetPages1Based,
                            stepOut
                        )
                    }
                    is WorkflowStepConfig.ExtractPages -> {
                        PdfEngine.extractPages(context, inputUri, stepConfig.pageNumbers1Based, stepOut)
                    }
                    is WorkflowStepConfig.InsertPdf -> {
                        executeInsertInMiddle(
                            context = context,
                            baseUri = inputUri,
                            insertAfterPage1Based = stepConfig.insertAfterPage1Based,
                            insertUri = stepConfig.insertUri,
                            outputFile = stepOut
                        )
                    }
                    is WorkflowStepConfig.Compress -> {
                        PdfEngine.compressPdf(context, inputUri, stepConfig.level, stepOut)
                    }
                }

                currentInputFile = stepOut
            }

            onProgress(
                PipelineExecutionProgress(
                    currentStepIndex = steps.size,
                    totalSteps = steps.size,
                    stepDescription = "Pipeline Completed Successfully!",
                    isFinished = true
                )
            )

            finalOutputFile
        } finally {
            intermediateFiles.forEach { it.delete() }
        }
    }

    /**
     * Smart Workflow: Replace a specific page in a base PDF with a page from another PDF/document
     */
    suspend fun executeReplacePage(
        context: Context,
        baseUri: Uri,
        targetPage1Based: Int,
        replacementUri: Uri,
        replacementPage1Based: Int = 1,
        outputFile: File,
        onProgress: (PipelineExecutionProgress) -> Unit = {}
    ): File = withContext(Dispatchers.IO) {
        val tempFiles = mutableListOf<File>()
        try {
            onProgress(PipelineExecutionProgress(1, 3, "Analyzing document structure..."))
            val totalPages = PdfEngine.getPageCount(context, baseUri)
            val replacementTotal = PdfEngine.getPageCount(context, replacementUri)

            if (targetPage1Based !in 1..totalPages) {
                throw IllegalArgumentException("Target page $targetPage1Based is outside document bounds (1..$totalPages)")
            }
            if (replacementPage1Based !in 1..replacementTotal) {
                throw IllegalArgumentException("Replacement page $replacementPage1Based is outside document bounds (1..$replacementTotal)")
            }

            // Extract the single replacement page
            onProgress(PipelineExecutionProgress(2, 3, "Preparing replacement page..."))
            val singleReplacement = File.createTempFile("wf_rep_page_", ".pdf", context.cacheDir)
            tempFiles.add(singleReplacement)
            PdfEngine.extractPages(context, replacementUri, listOf(replacementPage1Based), singleReplacement)

            onProgress(PipelineExecutionProgress(3, 3, "Assembling final document..."))

            if (totalPages == 1) {
                singleReplacement.copyTo(outputFile, overwrite = true)
                return@withContext outputFile
            }

            val mergeList = mutableListOf<Uri>()

            if (targetPage1Based > 1) {
                val part1 = File.createTempFile("wf_base_p1_", ".pdf", context.cacheDir)
                tempFiles.add(part1)
                val p1Pages = (1 until targetPage1Based).toList()
                PdfEngine.extractPages(context, baseUri, p1Pages, part1)
                mergeList.add(Uri.fromFile(part1))
            }

            mergeList.add(Uri.fromFile(singleReplacement))

            if (targetPage1Based < totalPages) {
                val part2 = File.createTempFile("wf_base_p2_", ".pdf", context.cacheDir)
                tempFiles.add(part2)
                val p2Pages = ((targetPage1Based + 1)..totalPages).toList()
                PdfEngine.extractPages(context, baseUri, p2Pages, part2)
                mergeList.add(Uri.fromFile(part2))
            }

            PdfEngine.mergePdfs(context, mergeList, outputFile)
            outputFile
        } finally {
            tempFiles.forEach { it.delete() }
        }
    }

    /**
     * Smart Workflow: Insert a PDF into the middle of an existing PDF (after page N)
     */
    suspend fun executeInsertInMiddle(
        context: Context,
        baseUri: Uri,
        insertAfterPage1Based: Int,
        insertUri: Uri,
        outputFile: File,
        onProgress: (PipelineExecutionProgress) -> Unit = {}
    ): File = withContext(Dispatchers.IO) {
        val tempFiles = mutableListOf<File>()
        try {
            onProgress(PipelineExecutionProgress(1, 2, "Splitting base document..."))
            val total = PdfEngine.getPageCount(context, baseUri)

            if (insertAfterPage1Based <= 0) {
                // Prepend
                PdfEngine.mergePdfs(context, listOf(insertUri, baseUri), outputFile)
            } else if (insertAfterPage1Based >= total) {
                // Append
                PdfEngine.mergePdfs(context, listOf(baseUri, insertUri), outputFile)
            } else {
                // Split and Merge: Part 1 + Insert + Part 2
                val part1 = File.createTempFile("wf_ins_p1_", ".pdf", context.cacheDir)
                val part2 = File.createTempFile("wf_ins_p2_", ".pdf", context.cacheDir)
                tempFiles.add(part1)
                tempFiles.add(part2)

                PdfEngine.splitPdf(context, baseUri, insertAfterPage1Based, part1, part2)

                onProgress(PipelineExecutionProgress(2, 2, "Merging inserted pages..."))
                PdfEngine.mergePdfs(
                    context,
                    listOf(Uri.fromFile(part1), insertUri, Uri.fromFile(part2)),
                    outputFile
                )
            }
            outputFile
        } finally {
            tempFiles.forEach { it.delete() }
        }
    }
}

