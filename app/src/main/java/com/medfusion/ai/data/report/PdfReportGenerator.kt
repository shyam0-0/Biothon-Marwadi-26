package com.medfusion.ai.data.report

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.medfusion.ai.domain.model.Case
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Renders a simple, clean PDF summary of a case entirely on-device and saves it
 * to the public Downloads folder. Used as the offline/demo fallback for report
 * download when the backend /generate-pdf endpoint is unavailable.
 */
@Singleton
class PdfReportGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** @return the display name of the saved file. */
    fun generateAndSave(case: Case): String {
        val fileName = "MedFusion-Report-${case.caseId.take(8)}.pdf"
        val document = buildDocument(case)
        try {
            saveToDownloads(fileName, document)
        } finally {
            document.close()
        }
        return fileName
    }

    private fun buildDocument(case: Case): PdfDocument {
        val doc = PdfDocument()
        // A4 at 72dpi ≈ 595 x 842 pt.
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val title = Paint().apply { color = Color.parseColor("#1E6FD9"); textSize = 24f; isFakeBoldText = true }
        val heading = Paint().apply { color = Color.parseColor("#0F1B2D"); textSize = 14f; isFakeBoldText = true }
        val body = Paint().apply { color = Color.parseColor("#33415C"); textSize = 12f }
        val muted = Paint().apply { color = Color.parseColor("#5A6B82"); textSize = 10f }

        var y = 60f
        val left = 48f
        canvas.drawText("MedFusion AI — Health Report", left, y, title)
        y += 28f
        canvas.drawText("Case: ${case.caseId}", left, y, muted)
        y += 32f

        canvas.drawText("Recommended test", left, y, heading); y += 18f
        canvas.drawText(case.recommendedTest.ifBlank { "—" }, left, y, body); y += 28f

        canvas.drawText("Urgency", left, y, heading); y += 18f
        canvas.drawText(case.urgencyLevel.label, left, y, body); y += 28f

        val fusion = case.fusionResult
        canvas.drawText("AI findings", left, y, heading); y += 18f
        y = drawWrapped(canvas, fusion?.findings ?: "Analysis pending.", left, y, 500f, body, 16f); y += 18f

        if (fusion != null) {
            canvas.drawText("Confidence: ${fusion.confidenceLevel.label}", left, y, body); y += 28f
        }

        // Disclaimer at the bottom.
        drawWrapped(
            canvas,
            "This is an AI-assisted insight, not a diagnosis. Please consult your assigned doctor.",
            left, 800f, 500f, muted, 14f,
        )

        doc.finishPage(page)
        return doc
    }

    /** Naive word-wrap; returns the new y after drawing. */
    private fun drawWrapped(
        canvas: android.graphics.Canvas,
        text: String,
        x: Float,
        startY: Float,
        maxWidth: Float,
        paint: Paint,
        lineHeight: Float,
    ): Float {
        var y = startY
        val words = text.split(" ")
        var line = StringBuilder()
        for (word in words) {
            val candidate = if (line.isEmpty()) word else "$line $word"
            if (paint.measureText(candidate) > maxWidth) {
                canvas.drawText(line.toString(), x, y, paint)
                y += lineHeight
                line = StringBuilder(word)
            } else {
                line = StringBuilder(candidate)
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line.toString(), x, y, paint)
            y += lineHeight
        }
        return y
    }

    private fun saveToDownloads(fileName: String, document: PdfDocument) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Could not create Downloads entry")
            resolver.openOutputStream(uri)?.use { document.writeTo(it) }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } else {
            // Legacy path (API 26–28) uses WRITE_EXTERNAL_STORAGE (declared maxSdk 28).
            @Suppress("DEPRECATION")
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloads.exists()) downloads.mkdirs()
            FileOutputStream(File(downloads, fileName)).use { document.writeTo(it) }
        }
    }
}
