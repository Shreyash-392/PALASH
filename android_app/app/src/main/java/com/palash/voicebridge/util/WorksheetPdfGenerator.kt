package com.palash.voicebridge.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.palash.voicebridge.domain.model.WorksheetTemplate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Native Offline PDF Generator for PALASH Bilingual Worksheets.
 *
 * Uses Android's built-in PdfDocument API to render crisp A4 printable documents
 * with bilingual instructions (Hindi + Santali Ol Chiki), question boxes, and write-in blanks.
 */
class WorksheetPdfGenerator(
    private val context: Context
) {
    companion object {
        const val PAGE_WIDTH = 595 // A4 standard width (pt)
        const val PAGE_HEIGHT = 842 // A4 standard height (pt)
        const val MARGIN = 36f
        const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2)
    }

    suspend fun generateWorksheetPdf(
        template: WorksheetTemplate,
        isBilingual: Boolean = true
    ): File = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        drawWorksheetPage(canvas, template, isBilingual)

        document.finishPage(page)

        // Save to cache/worksheets directory
        val outputDir = File(context.cacheDir, "worksheets").apply { mkdirs() }
        val outputFile = File(outputDir, "worksheet_${template.id}_${System.currentTimeMillis()}.pdf")

        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        return@withContext outputFile
    }

    private fun drawWorksheetPage(
        canvas: Canvas,
        template: WorksheetTemplate,
        isBilingual: Boolean
    ) {
        val titlePaint = Paint().apply {
            color = Color.rgb(230, 81, 0) // Palash Orange
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val headingPaint = Paint().apply {
            color = Color.BLACK
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val olChikiPaint = Paint().apply {
            color = Color.rgb(109, 76, 65) // Ol Chiki brown accent
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val borderPaint = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            isAntiAlias = true
        }

        val cardBgPaint = Paint().apply {
            color = Color.rgb(255, 248, 225) // Pale cream
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        var y = MARGIN + 10f

        // 1. Page Header Banner
        val headerRect = RectF(MARGIN, y, MARGIN + CONTENT_WIDTH, y + 65f)
        canvas.drawRoundRect(headerRect, 8f, 8f, cardBgPaint)
        canvas.drawRoundRect(headerRect, 8f, 8f, borderPaint)

        canvas.drawText("🌺 PALASH VoiceBridge — प्राथमिक विद्यालय वर्कशीट", MARGIN + 14f, y + 24f, titlePaint)
        canvas.drawText("कक्षा (Class): ${template.classLevel}   |   विषय (Subject): ${template.subjectHindi} (${template.subject})   |   विषयवस्तु: ${template.topicHindi}", MARGIN + 14f, y + 44f, subtitlePaint)

        y += 75f

        // 2. Student Info Header Line
        canvas.drawText("विद्यार्थी का नाम (Student Name): ________________________", MARGIN + 10f, y + 16f, textPaint)
        canvas.drawText("दिनांक (Date): ____________", MARGIN + CONTENT_WIDTH - 180f, y + 16f, textPaint)

        y += 30f
        canvas.drawLine(MARGIN, y, MARGIN + CONTENT_WIDTH, y, borderPaint)
        y += 15f

        // 3. Instruction Box
        canvas.drawText("निर्देश (Instructions): ${template.instructionHindi}", MARGIN + 10f, y + 12f, headingPaint)
        if (isBilingual && template.instructionSantali.isNotEmpty()) {
            canvas.drawText("ᱥᱟᱱᱛᱟᱲᱤ ᱱᱤᱨᱫᱮᱥ: ${template.instructionSantali}", MARGIN + 10f, y + 28f, olChikiPaint)
            y += 18f
        }
        y += 28f

        // 4. Question Items
        template.questions.forEachIndexed { index, q ->
            val qBoxTop = y
            val qBoxHeight = if (!q.options.isNullOrEmpty()) 80f else 62f
            val qBox = RectF(MARGIN, qBoxTop, MARGIN + CONTENT_WIDTH, qBoxTop + qBoxHeight)

            canvas.drawRoundRect(qBox, 6f, 6f, borderPaint)

            // Question Header & Text
            canvas.drawText("प्र. ${index + 1})  ${q.questionHindi}", MARGIN + 12f, y + 20f, headingPaint)

            if (isBilingual && q.questionSantali.isNotEmpty()) {
                canvas.drawText("    ${q.questionSantali}", MARGIN + 12f, y + 36f, olChikiPaint)
            }

            // Options or Answer space
            if (!q.options.isNullOrEmpty()) {
                var optX = MARGIN + 20f
                q.options.forEachIndexed { optIdx, opt ->
                    val optLabel = "${('A' + optIdx)}) [  ] $opt"
                    val satOpt = if (isBilingual && !q.optionsSantali.isNullOrEmpty() && optIdx < q.optionsSantali.size) {
                        " (${q.optionsSantali[optIdx]})"
                    } else ""
                    canvas.drawText(optLabel + satOpt, optX, y + 58f, textPaint)
                    optX += 130f
                }
            } else {
                canvas.drawText("उत्तर (Answer): ________________________________________", MARGIN + 20f, y + 52f, subtitlePaint)
            }

            y += qBoxHeight + 12f
        }

        // 5. Footer Banner
        val footerY = PAGE_HEIGHT - MARGIN - 15f
        canvas.drawLine(MARGIN, footerY - 10f, MARGIN + CONTENT_WIDTH, footerY - 10f, borderPaint)
        val footerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 9f
            isAntiAlias = true
        }
        canvas.drawText("PALASH VoiceBridge • Smart India Hackathon 2026 • 100% Offline Primary Education Suite", MARGIN, footerY + 6f, footerPaint)
        canvas.drawText("पेज (Page) 1 / 1", MARGIN + CONTENT_WIDTH - 60f, footerY + 6f, footerPaint)
    }

    /**
     * Creates an Android Intent to view/share the generated PDF.
     */
    fun getOpenPdfIntent(file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun getSharePdfIntent(file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "PALASH VoiceBridge Bilingual Worksheet")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
