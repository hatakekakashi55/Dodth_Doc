package com.docs.app.domain.pdf

import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.File

/** Extracts plain text from text-based PDFs using PDFBox. */
object PdfText {

    data class ExtractedText(
        /** Concatenated text of the whole document. */
        val full: String,
        /** Per-page text, 0-indexed, trailing whitespace trimmed. */
        val pages: List<String>
    )

    fun extract(file: File, password: String? = null): ExtractedText {
        val document = if (password.isNullOrEmpty()) {
            PDDocument.load(file)
        } else {
            PDDocument.load(file, password)
        }
        try {
            val stripper = com.tom_roush.pdfbox.text.PDFTextStripper().apply {
                sortByPosition = true
            }
            val pages = mutableListOf<String>()
            for (i in 1..document.numberOfPages) {
                stripper.startPage = i
                stripper.endPage = i
                pages += stripper.getText(document).trim()
            }
            return ExtractedText(pages.joinToString("\n").trim(), pages)
        } finally {
            document.close()
        }
    }
}
