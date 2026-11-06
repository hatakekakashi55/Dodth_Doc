package com.docs.app.domain.convert

import com.docs.app.domain.ooxml.PptxSlide
import com.docs.app.domain.ooxml.PptxWriter
import com.docs.app.domain.pdf.PdfText
import java.io.File

/**
 * Converts a text-based PDF into a 16:9 PowerPoint deck —
 * one slide per PDF page; the first line becomes the slide title.
 */
object PdfToPpt {

    fun convert(input: File, out: File) {
        val extracted = PdfText.extract(input)
        val slides = mutableListOf<PptxSlide>()
        for ((pageIndex, pageText) in extracted.pages.withIndex()) {
            val lines = pageText.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val title = lines.firstOrNull()?.take(90) ?: "Page ${pageIndex + 1}"
            val bullets = lines.drop(1).take(12).map { if (it.length > 160) it.take(157) + "…" else it }
            slides += PptxSlide(title = title, bullets = bullets)
        }
        if (slides.isEmpty()) {
            slides += PptxSlide(
                title = "No extractable text",
                bullets = listOf("This PDF appears to be scanned. Run OCR PDF first, then convert again.")
            )
        }
        PptxWriter.write(out, slides, title = "Converted from ${input.name}")
    }
}
