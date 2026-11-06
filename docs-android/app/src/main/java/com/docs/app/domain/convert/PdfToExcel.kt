package com.docs.app.domain.convert

import com.docs.app.domain.ooxml.XlsxWriter
import com.docs.app.domain.pdf.PdfText
import java.io.File

/**
 * Converts a text-based PDF into an Excel (.xlsx) workbook.
 * Lines are tokenized into cells on tabs or runs of 2+ spaces —
 * the usual signature of columns in extracted PDF text.
 */
object PdfToExcel {

    private val CELL_SPLIT = Regex("\\t+|\\s{2,}")

    fun convert(input: File, out: File) {
        val extracted = PdfText.extract(input)
        val rows = mutableListOf<List<String>>()
        for (pageText in extracted.pages) {
            pageText.lines().forEach { raw ->
                val line = raw.trimEnd()
                if (line.isBlank()) return@forEach
                val cells = CELL_SPLIT.split(line.trim()).map { it.trim() }.filter { it.isNotEmpty() }
                if (cells.isNotEmpty()) rows += cells
            }
        }
        if (rows.isEmpty()) {
            rows += listOf(
                listOf("This PDF contains no extractable text. It is probably a scanned document — run OCR PDF first, then convert again.")
            )
        }
        // Normalize ragged rows to a rectangle
        val width = rows.maxOf { it.size }
        val normalized = rows.map { row -> row + List(width - row.size) { "" } }
        XlsxWriter.write(out, normalized, sheetName = "Sheet1", title = "Converted from ${input.name}")
    }
}
