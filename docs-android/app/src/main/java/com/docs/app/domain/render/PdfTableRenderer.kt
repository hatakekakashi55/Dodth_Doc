package com.docs.app.domain.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.docs.app.domain.ooxml.XlsxSheet
import java.io.File
import java.io.FileOutputStream

/**
 * Renders spreadsheet rows into a professional, paginated PDF table.
 *
 * A slate section band introduces every sheet; the first data row is treated
 * as an indigo header with white text; zebra striping and hairline grid lines
 * keep dense sheets readable. Wide sheets are scaled down to A4 width.
 */
object PdfTableRenderer {

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 40f
    private const val ROW_H = 19f
    private const val HEADER_H = 22f
    private const val BAND_H = 26f

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 12.5f; isFakeBoldText = true; color = Color.parseColor("#3730A3")
    }
    private val sheetLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 7.5f; color = Color.parseColor("#6366F1")
    }
    private val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 8.6f; isFakeBoldText = true; color = Color.WHITE
    }
    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 8.2f; color = Color.parseColor("#1F2937")
    }
    private val bandFill = Paint().apply { color = Color.parseColor("#EEF0FF") }
    private val headerFill = Paint().apply { color = Color.parseColor("#4F46E5") }
    private val zebraFill = Paint().apply { color = Color.parseColor("#F7F8FB") }
    private val gridPaint = Paint().apply {
        color = Color.parseColor("#E3E5EA"); strokeWidth = 0.6f; style = Paint.Style.STROKE
    }

    fun render(sheets: List<XlsxSheet>, output: File): Int {
        val doc = PdfDocument()
        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var y = 0f
        val contentWidth = PAGE_W - 2 * MARGIN
        val pageBottom = PAGE_H - 44f

        fun newPage() {
            pageNumber += 1
            val info = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumber).create()
            page = doc.startPage(info)
            page!!.canvas.drawColor(Color.WHITE)
            y = MARGIN
        }

        fun finishPage() {
            val current = page ?: return
            PageFurniture.drawFooter(current.canvas, pageNumber, PAGE_W, PAGE_H, MARGIN)
            doc.finishPage(current)
            page = null
        }

        if (sheets.isEmpty()) {
            newPage()
            page!!.canvas.drawText("Empty workbook — nothing to render.", MARGIN, y + 40f, cellPaint)
            finishPage()
        }

        sheets.forEachIndexed { sheetIndex, sheet ->
            val sheetName = sheet.name.ifBlank { "Sheet ${sheetIndex + 1}" }
            newPage()
            // --- section band ---
            val bandTop = y
            page!!.canvas.drawRoundRect(RectF(MARGIN, bandTop, PAGE_W - MARGIN, bandTop + BAND_H), 7f, 7f, bandFill)
            page!!.canvas.drawText(ellipsize(sheetName, titlePaint, contentWidth - 90f), MARGIN + 10f, bandTop + BAND_H / 2f + 4.5f, titlePaint)
            val label = "SHEET ${sheetIndex + 1} OF ${sheets.size}"
            val labelWidth = sheetLabelPaint.measureText(label)
            page!!.canvas.drawText(label, PAGE_W - MARGIN - 10f - labelWidth, bandTop + BAND_H / 2f + 3f, sheetLabelPaint)
            y = bandTop + BAND_H + 12f

            val rows = sheet.rows
            if (rows.isEmpty()) {
                page!!.canvas.drawText("This sheet has no data rows.", MARGIN, y + 16f, cellPaint)
                finishPage()
                return@forEachIndexed
            }

            val columnCount = rows.maxOf { it.size }.coerceAtLeast(1)
            // --- column widths from content ---
            val rawWidths = FloatArray(columnCount) { col ->
                var widest = 30f
                for (row in rows) {
                    val cell = row.getOrNull(col).orEmpty()
                    val w = cellPaint.measureText(cell) + 14f
                    if (w > widest) widest = w
                }
                widest.coerceIn(44f, 150f)
            }
            val rawTotal = rawWidths.sum()
            val scale = if (rawTotal > contentWidth) contentWidth / rawTotal else 1f
            val colWidths = FloatArray(columnCount) { rawWidths[it] * scale }

            fun drawTableHeader(canvas: Canvas, top: Float): Float {
                canvas.drawRect(MARGIN, top, MARGIN + contentWidth, top + HEADER_H, headerFill)
                var x = MARGIN
                for (c in 0 until columnCount) {
                    val text = rows[0].getOrNull(c).orEmpty()
                    canvas.drawText(ellipsize(text, headerPaint, colWidths[c] - 10f), x + 6f, top + HEADER_H / 2f + 3.2f, headerPaint)
                    x += colWidths[c]
                }
                return top + HEADER_H
            }

            fun drawRow(canvas: Canvas, top: Float, rowIndex: Int, fill: Boolean) {
                if (fill) canvas.drawRect(MARGIN, top, MARGIN + contentWidth, top + ROW_H, zebraFill)
                var x = MARGIN
                for (c in 0 until columnCount) {
                    val text = rows[rowIndex].getOrNull(c).orEmpty()
                    canvas.drawText(ellipsize(text, cellPaint, colWidths[c] - 10f), x + 6f, top + ROW_H / 2f + 3f, cellPaint)
                    x += colWidths[c]
                }
            }

            fun drawGrid(canvas: Canvas, top: Float, bottom: Float) {
                canvas.drawLine(MARGIN, top, MARGIN + contentWidth, top, gridPaint)
                canvas.drawLine(MARGIN, bottom, MARGIN + contentWidth, bottom, gridPaint)
                var x = MARGIN
                for (c in 0 until columnCount) {
                    canvas.drawLine(x, top, x, bottom, gridPaint)
                    x += colWidths[c]
                }
                canvas.drawLine(x, top, x, bottom, gridPaint)
            }

            // header row (row 0), then body
            var headerBottom = drawTableHeader(page!!.canvas, y)
            drawGrid(page!!.canvas, y, headerBottom)
            y = headerBottom

            for (rowIndex in 1 until rows.size) {
                if (y + ROW_H > pageBottom) {
                    finishPage()
                    newPage()
                    val contLabel = page!!.canvas
                    contLabel.drawText("$sheetName — continued", MARGIN, y - 6f, sheetLabelPaint)
                    y = drawTableHeader(page!!.canvas, y + 4f)
                    drawGrid(page!!.canvas, y - HEADER_H, y)
                }
                val fill = rowIndex % 2 == 0
                drawRow(page!!.canvas, y, rowIndex, fill)
                drawGrid(page!!.canvas, y, y + ROW_H)
                y += ROW_H
            }
            finishPage()
        }

        FileOutputStream(output).use { doc.writeTo(it) }
        doc.close()
        return pageNumber
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (text.isEmpty() || paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text, 0, end) + paint.measureText("…") > maxWidth) {
            end = (end * 0.85f).toInt().coerceAtLeast(1)
        }
        return text.substring(0, end).trimEnd() + "…"
    }
}
