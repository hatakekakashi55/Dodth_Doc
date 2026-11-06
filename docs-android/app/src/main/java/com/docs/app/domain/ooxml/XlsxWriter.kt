package com.docs.app.domain.ooxml

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Writes a minimal, valid .xlsx (SpreadsheetML) workbook without external
 * dependencies. Produces a single worksheet with one shared-strings table,
 * auto column widths and a bold header row.
 */
object XlsxWriter {

    fun write(output: File, rows: List<List<String>>, sheetName: String = "Sheet1", title: String = "Converted Document") {
        val safeRows = rows.ifEmpty { listOf(listOf("")) }
        val maxCols = safeRows.maxOf { it.size }.coerceAtLeast(1)

        // --- shared strings table (first row is the styled header) ---
        val shared = LinkedHashMap<String, Int>()
        fun intern(value: String): Int =
            shared.getOrPut(value) { shared.size }

        val headerIndex = 0
        val cellRefs = safeRows.mapIndexed { rowIndex, row ->
            row.mapIndexed { colIndex, raw ->
                val clean = XmlEsc.stripIllegal(raw).replace("\r", " ").replace("\n", " ").trim()
                intern(clean) to rowIndex
            }
        }

        val sstCount = cellRefs.sumOf { it.size }
        val sstXml = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="$sstCount" uniqueCount="${shared.size}">""")
            shared.keys.forEach { text ->
                append("""<si><t xml:space="preserve">""").append(XmlEsc.esc(text)).append("</t></si>")
            }
            append("</sst>")
        }

        // --- column widths from content ---
        val colWidths = (0 until maxCols).map { col ->
            val maxLen = safeRows.maxOf { row -> (row.getOrNull(col)?.length ?: 0) }
            (maxLen.coerceAtMost(58) + 2).coerceAtLeast(10)
        }
        val colsXml = buildString {
            append("<cols>")
            colWidths.forEachIndexed { index, width ->
                append("""<col min="${index + 1}" max="${index + 1}" width="$width" customWidth="1"/>""")
            }
            append("</cols>")
        }

        val sheetData = buildString {
            append("<sheetData>")
            safeRows.forEachIndexed { rowIndex, row ->
                val excelRow = rowIndex + 1
                append("""<row r="$excelRow">""")
                for (colIndex in 0 until maxCols) {
                    val ref = "${columnName(colIndex)}$excelRow"
                    val text = row.getOrNull(colIndex)?.let { XmlEsc.stripIllegal(it).replace("\r", " ").replace("\n", " ").trim() } ?: ""
                    val sharedIndex = shared.getValue(text)
                    val style = if (rowIndex == headerIndex && safeRows.size > 1) """ s="1"""" else ""
                    append("""<c r="$ref" t="s"$style><v>$sharedIndex</v></c>""")
                }
                append("</row>")
            }
            append("</sheetData>")
        }

        val sheetXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheetViews><sheetView workbookViewId="0" showGridLines="1"/></sheetViews><sheetFormatPr defaultRowHeight="15"/>$colsXml$sheetData</worksheet>"""

        val workbookXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><workbookPr/><bookViews><workbookView xWindow="0" yWindow="0" windowWidth="24000" windowHeight="12000"/></bookViews><sheets><sheet name="${XmlEsc.attr(sheetName.take(31))}" sheetId="1" r:id="rId1"/></sheets></workbook>"""

        val stylesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><fonts count="2"><font><sz val="11"/><color theme="1"/><name val="Calibri"/><family val="2"/></font><font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/><family val="2"/></font></fonts><fills count="3"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill><fill><patternFill patternType="solid"><fgColor rgb="FF4F46E5"/><bgColor indexed="64"/></patternFill></fill></fills><borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders><cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs><cellXfs count="2"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/><xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/></cellXfs><cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles></styleSheet>"""

        val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/><Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/><Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/></Types>"""

        val rootRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/><Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/></Relationships>"""

        val workbookRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/><Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/></Relationships>"""

        val coreXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dcmitype="http://purl.org/dc/dcmitype/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><dc:title>${XmlEsc.esc(title)}</dc:title><dc:creator>Docs — Document Utility Suite</dc:creator><cp:lastModifiedBy>Docs</cp:lastModifiedBy><dcterms:created xsi:type="dcterms:W3CDTF">2026-01-01T00:00:00Z</dcterms:created><dcterms:modified xsi:type="dcterms:W3CDTF">2026-01-01T00:00:00Z</dcterms:modified></cp:coreProperties>"""

        val appXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes"><Application>Docs</Application><DocSecurity>0</DocSecurity><ScaleCrop>false</ScaleCrop><Company>Docs</Company><AppVersion>1.0</AppVersion></Properties>"""

        ZipOutputStream(BufferedOutputStream(FileOutputStream(output))).use { zos ->
            fun put(name: String, content: String) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(content.toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
            put("[Content_Types].xml", contentTypes)
            put("_rels/.rels", rootRels)
            put("xl/workbook.xml", workbookXml)
            put("xl/_rels/workbook.xml.rels", workbookRels)
            put("xl/worksheets/sheet1.xml", sheetXml)
            put("xl/sharedStrings.xml", sstXml)
            put("xl/styles.xml", stylesXml)
            put("docProps/core.xml", coreXml)
            put("docProps/app.xml", appXml)
        }
    }

    /** 0 -> "A", 25 -> "Z", 26 -> "AA" */
    fun columnName(index: Int): String {
        var n = index + 1
        val sb = StringBuilder()
        while (n > 0) {
            val rem = (n - 1) % 26
            sb.append(('A' + rem))
            n = (n - 1) / 26
        }
        return sb.reverse().toString()
    }
}
