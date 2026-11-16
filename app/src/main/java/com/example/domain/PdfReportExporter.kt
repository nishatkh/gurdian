package com.example.domain

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object PdfReportExporter {
    fun exportDraftPdf(context: Context, incidentId: String, draftText: String): File {
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val outFile = File(reportsDir, "guardian_gd_draft_${incidentId.take(8)}.pdf")

        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val headerPaint = Paint().apply {
            color = Color.rgb(18, 28, 10)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, 595f, 82f, headerPaint)

        val titlePaint = Paint().apply {
            color = Color.rgb(182, 255, 85)
            textSize = 18f
            isFakeBoldText = true
        }
        canvas.drawText("GUARDIAN AI — INCIDENT REPORT DRAFT", 36f, 38f, titlePaint)

        val subPaint = Paint().apply {
            color = Color.WHITE
            textSize = 10f
        }
        canvas.drawText("DRAFT ONLY — NOT AN OFFICIALLY SUBMITTED BANGLADESH POLICE GD", 36f, 60f, subPaint)

        val bodyPaint = Paint().apply {
            color = Color.rgb(30, 30, 30)
            textSize = 11f
        }
        var y = 116f
        val maxCharsPerLine = 84
        for (rawLine in draftText.lines()) {
            val wrapped = if (rawLine.length <= maxCharsPerLine) {
                listOf(rawLine)
            } else {
                rawLine.chunked(maxCharsPerLine)
            }
            for (line in wrapped) {
                if (y > 800f) break
                canvas.drawText(line, 36f, y, bodyPaint)
                y += 16f
            }
        }
        doc.finishPage(page)

        FileOutputStream(outFile).use { fos ->
            doc.writeTo(fos)
        }
        doc.close()
        return outFile
    }

    fun sharePdf(context: Context, pdfFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Guardian AI — Incident Report Draft (Not Official GD)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export / Print Draft PDF"))
    }
}
