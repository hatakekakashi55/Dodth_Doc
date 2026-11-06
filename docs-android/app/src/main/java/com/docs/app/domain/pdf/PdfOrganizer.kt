package com.docs.app.domain.pdf

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDResources
import com.tom_roush.pdfbox.cos.COSDictionary
import com.tom_roush.pdfbox.cos.COSName
import com.tom_roush.pdfbox.multipdf.PDFCloneUtility
import com.docs.app.core.ZipX
import java.io.File
import java.io.FileOutputStream

/**
 * Organize-PDF operations implemented with Apache PDFBox for Android.
 * Merge · split · rotate · compress · page numbers · delete pages.
 */
object PdfOrganizer {

    private const val TITLE_SUFFIX = " — Docs Document Utility Suite"

    private fun load(file: File): PDDocument = try {
        PDDocument.load(file)
    } catch (e: InvalidPasswordException) {
        throw IllegalStateException("This PDF is password-protected. Use Unlock PDF first.")
    } catch (e: Exception) {
        throw IllegalStateException("Could not read this PDF. It may be corrupted or not a real PDF file.")
    }


    /**
     * Deep-clones [page] (content stream + resources) into [dest] while [src] is open.
     * The /Parent entry is stripped before cloning so the whole source document graph is
     * not copied; the destination's page tree re-parents the page on add.
     * Unlike PDDocument.importPage, resources are cloned too, so [src] may be closed
     * immediately after copying.
     */
    private fun clonePageInto(cloner: PDFCloneUtility, dest: PDDocument, page: PDPage) {
        val pageDict = page.getCOSObject()
        val parent = pageDict.getItem(COSName.PARENT)
        pageDict.removeItem(COSName.PARENT)
        val clonedDict = try {
            cloner.cloneForNewDocument(pageDict) as COSDictionary
        } finally {
            if (parent != null) pageDict.setItem(COSName.PARENT, parent)
        }
        val newPage = PDPage(clonedDict)
        newPage.setCropBox(page.getCropBox())
        newPage.setMediaBox(page.getMediaBox())
        newPage.setRotation(page.getRotation())
        val resources = page.getResources()
        newPage.setResources(
            resources?.let { PDResources(cloner.cloneForNewDocument(it) as COSDictionary) }
                ?: PDResources()
        )
        dest.addPage(newPage)
    }

    private fun save(document: PDDocument, out: File) {
        FileOutputStream(out).use { document.save(it) }
    }

    /** Merge PDFs in the given order. Returns the merged page count. */
    fun merge(inputs: List<File>, out: File): Int {
        if (inputs.size < 2) throw IllegalArgumentException("Please provide at least 2 PDF files to merge.")
        val merged = PDDocument()
        try {
            val cloner = PDFCloneUtility(merged)
            for (input in inputs) {
                load(input).use { src ->
                    for (page in src.pages) clonePageInto(cloner, merged, page)
                }
            }
            merged.documentInformation.apply {
                title = "Merged Document" + TITLE_SUFFIX
                creator = "Docs Document Utility Suite"
                producer = "Docs 1.0 (PDFBox)"
            }
            save(merged, out)
            return merged.numberOfPages
        } finally {
            merged.close()
        }
    }

    /**
     * Splits [input] according to [rangeSpec] ("all" or "1,3,5-7") into
     * single-page PDFs archived as [zipOut]. Returns the number of pages written.
     */
    fun split(input: File, rangeSpec: String, zipOut: File, tempDir: File): Int {
        load(input).use { src ->
            val selected = PageRangeParser.parse(rangeSpec, src.numberOfPages)
            if (selected.isEmpty()) {
                throw IllegalArgumentException("No pages matched “${rangeSpec.ifBlank { "all" }}”. Check the page range and try again.")
            }
            val tempFiles = mutableListOf<File>()
            val entryNames = mutableListOf<String>()
            try {
                for (zeroBased in selected) {
                    val single = PDDocument()
                    try {
                        val cloner = PDFCloneUtility(single)
                        clonePageInto(cloner, single, src.getPage(zeroBased))
                        val name = "page_${zeroBased + 1}.pdf"
                        val temp = File(tempDir, "split_${System.currentTimeMillis()}_$name")
                        FileOutputStream(temp).use { single.save(it) }
                        tempFiles += temp
                        entryNames += name
                    } finally {
                        single.close()
                    }
                }
                ZipX.zipFiles(tempFiles, entryNames, zipOut)
                return tempFiles.size
            } finally {
                tempFiles.forEach { it.delete() }
            }
        }
    }

    /** Rotates every page by [angle] (must be 90, 180 or 270). Returns the page count. */
    fun rotate(input: File, out: File, angle: Int): Int {
        if (angle !in listOf(90, 180, 270)) throw IllegalArgumentException("Angle must be 90, 180 or 270 degrees.")
        load(input).use { src ->
            for (page in src.pages) {
                val current = page.rotation
                page.setRotation((current + angle) % 360)
            }
            src.documentInformation.producer = "Docs 1.0 (rotated $angle°)"
            save(src, out)
            return src.numberOfPages
        }
    }

    /**
     * Rewrites [input] stripping unreferenced objects and re-serializing with
     * object streams — same technique the web suite uses.
     */
    fun compress(input: File, out: File): Pair<Long, Long> {
        val originalSize = input.length()
        load(input).use { src ->
            val compressed = PDDocument()
            try {
                val cloner = PDFCloneUtility(compressed)
                for (page in src.pages) clonePageInto(cloner, compressed, page)
                compressed.documentInformation.apply {
                    title = src.documentInformation.title ?: ("Compressed Document" + TITLE_SUFFIX)
                    creator = "Docs Document Utility Suite"
                    producer = "Docs 1.0 (compressed)"
                }
                save(compressed, out)
            } finally {
                compressed.close()
            }
        }
        return originalSize to out.length()
    }

    /** Deletes pages matched by [rangeSpec]. Returns the remaining page count. */
    fun deletePages(input: File, out: File, rangeSpec: String): Int {
        load(input).use { src ->
            val total = src.numberOfPages
            val toDelete = PageRangeParser.parse(rangeSpec, total)
            if (toDelete.isEmpty()) throw IllegalArgumentException("No pages matched “$rangeSpec”.")
            if (toDelete.size >= total) throw IllegalArgumentException("Cannot delete every page of the document.")
            // Remove from the back so indices stay valid
            for (index in toDelete.sortedDescending()) {
                src.removePage(index)
            }
            src.documentInformation.producer = "Docs 1.0 (pages removed)"
            save(src, out)
            return src.numberOfPages
        }
    }

    /** Stamps "N / total" centered at the bottom of every page. Returns the page count. */
    fun addPageNumbers(input: File, out: File): Int {
        load(input).use { src ->
            val font = PDType1Font.HELVETICA
            val total = src.numberOfPages
            src.pages.forEachIndexed { index, page ->
                val width = page.mediaBox.width
                val text = "${index + 1} / $total"
                val fontSize = 11f
                val textWidth = font.getStringWidth(text) / 1000f * fontSize
                PDPageContentStream(src, page, PDPageContentStream.AppendMode.APPEND, true, true).use { stream ->
                    stream.beginText()
                    stream.setFont(font, fontSize)
                    stream.setNonStrokingColor(0.42f, 0.45f, 0.51f)
                    stream.newLineAtOffset((width - textWidth) / 2f, 18f)
                    stream.showText(text)
                    stream.endText()
                }
            }
            src.documentInformation.producer = "Docs 1.0 (page numbers)"
            save(src, out)
            return total
        }
    }
}
