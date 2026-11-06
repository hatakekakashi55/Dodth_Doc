package com.docs.app.domain

import com.docs.app.core.CachedInput
import com.docs.app.core.Files
import com.docs.app.domain.convert.*
import com.docs.app.domain.pdf.*
import com.docs.app.domain.ocr.OcrProcessor
import java.io.File

/**
 * Routes a tool id to its on-device engine and produces a [ProcessingOutput].
 * All engines write into [workDir]; the UI moves/saves results from there.
 */
object ProcessingDispatcher {

    suspend fun run(
        toolId: String,
        inputs: List<CachedInput>,
        options: ProcessingOptions,
        workDir: File
    ): ProcessingOutput {
        if (inputs.isEmpty()) throw IllegalArgumentException("Select at least one file first.")
        val base = Files.stripExtension(inputs.first().displayName).ifBlank { "document" }

        fun single(): CachedInput {
            if (inputs.size != 1) throw IllegalArgumentException("This tool works on one file at a time.")
            return inputs.first()
        }

        fun output(name: String, mime: String, summary: String? = null): ProcessingOutput {
            val clean = Files.sanitizeFileName(name)
            val unique = uniqueFile(workDir, clean)
            return ProcessingOutput(unique, unique.name, mime, summary)
        }

        return when (toolId) {
            // ---------- Convert to PDF ----------
            "word-to-pdf" -> {
                val input = single()
                val out = output("$base.pdf", Files.MIME_PDF)
                WordToPdf.convert(input.file, out.file)
                out
            }
            "excel-to-pdf" -> {
                val input = single()
                val out = output("$base.pdf", Files.MIME_PDF)
                ExcelToPdf.convert(input.file, out.file)
                out
            }
            "ppt-to-pdf" -> {
                val input = single()
                val out = output("$base.pdf", Files.MIME_PDF)
                PptToPdf.convert(input.file, out.file)
                out
            }
            "jpg-to-pdf" -> {
                val out = output("images_combined.pdf", Files.MIME_PDF)
                ImagesToPdf.convert(inputs.map { it.file }, out.file)
                out
            }
            "html-to-pdf" -> {
                val input = single()
                val out = output("$base.pdf", Files.MIME_PDF)
                HtmlToPdf.convert(input.file, out.file)
                out
            }

            // ---------- Convert from PDF ----------
            "pdf-to-word" -> {
                val input = single()
                val out = output("$base.docx", Files.MIME_DOCX)
                PdfToWord.convert(input.file, out.file)
                out
            }
            "pdf-to-ppt" -> {
                val input = single()
                val out = output("$base.pptx", Files.MIME_PPTX)
                PdfToPpt.convert(input.file, out.file)
                out
            }
            "pdf-to-excel" -> {
                val input = single()
                val out = output("$base.xlsx", Files.MIME_XLSX)
                PdfToExcel.convert(input.file, out.file)
                out
            }
            "pdf-to-jpg" -> {
                val input = single()
                val out = output("${base}_images.zip", Files.MIME_ZIP)
                val pages = PdfToImages.convert(input.file, out.file, workDir)
                out.copy(summary = "$pages page(s) exported as JPG")
            }
            "ocr" -> {
                val input = single()
                val out = output("${base}_ocr.pdf", Files.MIME_PDF)
                OcrProcessor.process(input.file, out.file)
                out
            }

            // ---------- Organize PDF ----------
            "merge" -> {
                if (inputs.size < 2) throw IllegalArgumentException("Select at least 2 PDF files to merge.")
                val out = output("merged_document.pdf", Files.MIME_PDF)
                val pages = PdfOrganizer.merge(inputs.map { it.file }, out.file)
                out.copy(summary = "${inputs.size} files · $pages pages")
            }
            "split" -> {
                val input = single()
                val out = output("${base}_split.zip", Files.MIME_ZIP)
                val count = PdfOrganizer.split(input.file, options.pageRange, out.file, workDir)
                out.copy(summary = "$count page(s) extracted")
            }
            "rotate" -> {
                val input = single()
                val out = output("${base}_rotated.pdf", Files.MIME_PDF)
                PdfOrganizer.rotate(input.file, out.file, options.angle)
                out
            }
            "compress" -> {
                val input = single()
                val out = output("${base}_compressed.pdf", Files.MIME_PDF)
                val (before, after) = PdfOrganizer.compress(input.file, out.file)
                val saved = if (before > 0 && after < before) {
                    "${Files.formatFileSize(before)} → ${Files.formatFileSize(after)} · ${((1.0 - after.toDouble() / before) * 100).toInt()}% smaller"
                } else {
                    "Already optimized — ${Files.formatFileSize(after)}"
                }
                out.copy(summary = saved)
            }
            "add-page-numbers" -> {
                val input = single()
                val out = output("${base}_numbered.pdf", Files.MIME_PDF)
                PdfOrganizer.addPageNumbers(input.file, out.file)
                out
            }
            "delete-pages" -> {
                val input = single()
                val spec = options.pageRange.trim()
                if (spec.isEmpty() || spec.equals("all", ignoreCase = true)) {
                    throw IllegalArgumentException("Specify which pages to delete (e.g. 2,4-6). Deleting every page is not allowed.")
                }
                val out = output("${base}_modified.pdf", Files.MIME_PDF)
                val remaining = PdfOrganizer.deletePages(input.file, out.file, spec)
                out.copy(summary = "$remaining page(s) remaining")
            }

            // ---------- PDF Security ----------
            "protect" -> {
                val input = single()
                val out = output("${base}_protected.pdf", Files.MIME_PDF)
                PdfSecurity.protect(input.file, out.file, options.password)
                out.copy(summary = "256-bit AES · password required to open")
            }
            "unlock" -> {
                val input = single()
                val out = output("${base}_unlocked.pdf", Files.MIME_PDF)
                PdfSecurity.unlock(input.file, out.file, options.password)
                out
            }
            "watermark" -> {
                val input = single()
                val out = output("${base}_watermarked.pdf", Files.MIME_PDF)
                PdfSecurity.watermark(input.file, out.file, options.watermarkText, options.watermarkOpacity)
                out
            }

            else -> throw IllegalArgumentException("Unknown tool: $toolId")
        }
    }

    /** Collision-free output: "name.pdf" → "name_1.pdf" / "name_2.pdf" … */
    private fun uniqueFile(dir: File, name: String): File {
        val clean = Files.sanitizeFileName(name)
        val dot = clean.lastIndexOf('.')
        val stem = if (dot > 0) clean.substring(0, dot) else clean
        val ext = if (dot > 0) clean.substring(dot) else ""
        var candidate = File(dir, clean)
        var n = 1
        while (candidate.exists()) {
            candidate = File(dir, "${stem}_$n$ext")
            n += 1
        }
        return candidate
    }
}
