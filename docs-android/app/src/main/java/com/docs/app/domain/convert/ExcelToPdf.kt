package com.docs.app.domain.convert

import com.docs.app.domain.ooxml.XlsxReader
import com.docs.app.domain.render.PdfTableRenderer
import java.io.File

/** Converts Excel (.xlsx) workbooks to a paginated PDF table document. */
object ExcelToPdf {

    fun convert(input: File, out: File) {
        val sheets = XlsxReader.read(input)
        if (sheets.isEmpty()) {
            throw IllegalStateException("No readable sheets found in this workbook.")
        }
        PdfTableRenderer.render(sheets, out)
    }
}
