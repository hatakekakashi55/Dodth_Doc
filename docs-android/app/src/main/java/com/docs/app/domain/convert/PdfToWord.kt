package com.docs.app.domain.convert

import com.docs.app.domain.ooxml.DocxOutLine
import com.docs.app.domain.ooxml.DocxWriter
import com.docs.app.domain.pdf.PdfText
import java.io.File

/**
 * Converts a text-based PDF into an editable Word (.docx) document.
 * Each non-blank line becomes a paragraph; empty lines become spacing.
 */
object PdfToWord {

    fun convert(input: File, out: File) {
        val extracted = PdfText.extract(input)
        val lines = mutableListOf<DocxOutLine>()
        for ((pageIndex, pageText) in extracted.pages.withIndex()) {
            if (pageText.isEmpty()) continue
            if (pageIndex > 0) lines += DocxOutLine("") // page gap
            pageText.lines().forEach { raw ->
                val line = raw.trim()
                lines += if (line.isEmpty()) DocxOutLine("") else DocxOutLine(line, sizePt = 10.5f)
            }
        }
        if (lines.isEmpty()) {
            lines += DocxOutLine(
                "This PDF contains no extractable text. It is probably a scanned document — run OCR PDF first, then convert again.",
                sizePt = 10.5f
            )
        }
        DocxWriter.write(out, "Converted from ${input.name}", lines)
    }
}
