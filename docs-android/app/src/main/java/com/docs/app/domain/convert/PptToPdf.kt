package com.docs.app.domain.convert

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.StaticLayout
import android.text.TextPaint
import android.text.Layout
import com.docs.app.domain.ooxml.PptxReader
import java.io.File
import java.io.FileOutputStream

/**
 * Converts PowerPoint (.pptx) to a 16:9 PDF deck — one slide per page,
 * with a light background, indigo accent spine, kicker label, title and bullets.
 */
object PptToPdf {

    private const val SLIDE_W = 960   // 13.33in @ 72dpi, points — 16:9
    private const val SLIDE_H = 540   // 7.5in @ 72dpi
    private const val MARGIN = 56f
    private const val ACCENT = 0xFF6366F1.toInt()

    private fun textPaint(size: Float, alpha: Int = 255, color: Int = 0xFF111827.toInt(), bold: Boolean = false) =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

    fun convert(input: File, out: File) {
        val slides = PptxReader.readSlides(input)
        if (slides.isEmpty()) {
            throw IllegalStateException("No slides found in this presentation.")
        }
        val document = PdfDocument()
        try {
            slides.forEachIndexed { index, paragraphs ->
                val info = PdfDocument.PageInfo.Builder(SLIDE_W, SLIDE_H, index + 1).create()
                val page = document.startPage(info)
                val canvas: Canvas = page.canvas
                canvas.drawColor(Color.WHITE)

                // Left accent spine
                canvas.drawRect(0f, 0f, 16f, SLIDE_H.toFloat(), Paint().apply { color = ACCENT })

                // Kicker
                val kickerPaint = textPaint(10f, color = 0xFF9CA3AF.toInt())
                canvas.drawText("SLIDE ${index + 1} OF ${slides.size}", MARGIN, 54f, kickerPaint)

                val contentWidth = (SLIDE_W - 2 * MARGIN).toInt()
                var y = 78f

                // Title: first non-empty paragraph
                val title = paragraphs.firstOrNull() ?: "Slide ${index + 1}"
                val titleLayout = StaticLayout.Builder
                    .obtain(title, 0, title.length, textPaint(24f, color = 0xFF111827.toInt(), bold = true), contentWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setIncludePad(false)
                    .setLineSpacing(0f, 1.05f)
                    .build()
                canvas.save()
                canvas.translate(MARGIN, y)
                titleLayout.draw(canvas)
                canvas.restore()
                y += titleLayout.height + 14f

                // Divider
                canvas.drawLine(MARGIN, y, SLIDE_W - MARGIN, y, Paint().apply {
                    color = Color.parseColor("#E5E7EB"); strokeWidth = 1.4f
                })
                y += 22f

                // Body bullets
                val bodyPaint = textPaint(13f, color = 0xFF374151.toInt())
                val bulletPaint = textPaint(13f, color = ACCENT, bold = true)
                for (line in paragraphs.drop(1)) {
                    val available = SLIDE_H - y - 40f
                    if (available < 30f) break
                    val bulletLayout = StaticLayout.Builder
                        .obtain(line, 0, line.length, bodyPaint, contentWidth - 34)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setIncludePad(false)
                        .setLineSpacing(0f, 1.22f)
                        .build()
                    canvas.drawText("•", MARGIN + 4f, y + 13f, bulletPaint)
                    canvas.save()
                    canvas.translate(MARGIN + 26f, y)
                    bulletLayout.draw(canvas)
                    canvas.restore()
                    y += bulletLayout.height + 9f
                }

                // Footer
                val footerPaint = textPaint(8.5f, color = 0xFF9CA3AF.toInt())
                val label = "${index + 1} / ${slides.size}"
                canvas.drawText(label, SLIDE_W - MARGIN - footerPaint.measureText(label), SLIDE_H - 24f, footerPaint)

                document.finishPage(page)
            }
            FileOutputStream(out).use { document.writeTo(it) }
        } finally {
            document.close()
        }
    }
}
