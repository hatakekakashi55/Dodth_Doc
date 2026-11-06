package com.docs.app.domain.ooxml

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

data class XlsxSheet(
    val name: String,
    val rows: List<List<String>>
)

/**
 * Minimal-but-real OOXML (.xlsx) reader.
 *
 * Reads shared strings, workbook sheet order and cell values
 * (shared strings, inline strings, numbers, booleans and formula results).
 * Styling, formulas and charts are intentionally flattened to plain text.
 */
object XlsxReader {

    fun read(file: File): List<XlsxSheet> {
        ZipFile(file).use { zip ->
            val sharedStrings = parseSharedStrings(zip)
            val sheetRefs = parseWorkbook(zip)          // ordered (name, rId)
            val targets = parseWorkbookRels(zip)        // rId -> target path
            if (sheetRefs.isEmpty()) return emptyList()
            return sheetRefs.map { (name, rId) ->
                val target = targets[rId] ?: "worksheets/sheet${rId.filter { it.isDigit() }}.xml"
                val path = normalizePartPath("xl/", target)
                val rows = parseSheet(zip, path, sharedStrings)
                XlsxSheet(name, rows)
            }
        }
    }

    // ------------------------------------------------------------- shared strings

    private fun parseSharedStrings(zip: ZipFile): List<String> {
        val entry = zip.getEntry("xl/sharedStrings.xml") ?: return emptyList()
        val out = mutableListOf<String>()
        parseXml(zip.getInputStream(entry)) { parser ->
            var current = StringBuilder()
            var inSi = false
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                val local = localName(parser.name)
                when (event) {
                    XmlPullParser.START_TAG -> when (local) {
                        "si" -> { current = StringBuilder(); inSi = true }
                        "t" -> if (inSi) current.append(parser.nextText())
                    }
                    XmlPullParser.END_TAG -> when (local) {
                        "si" -> { out += current.toString(); inSi = false }
                    }
                }
                event = parser.next()
            }
        }
        return out
    }

    // -------------------------------------------------------------- workbook rels

    private fun parseWorkbook(zip: ZipFile): List<Pair<String, String>> {
        val entry = zip.getEntry("xl/workbook.xml") ?: return emptyList()
        val out = mutableListOf<Pair<String, String>>()
        parseXml(zip.getInputStream(entry)) { parser ->
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && localName(parser.name) == "sheet") {
                    val name = attr(parser, "name") ?: "Sheet"
                    val id = attr(parser, "id") ?: attr(parser, "sheetId") ?: "1"
                    out += name to id
                }
                event = parser.next()
            }
        }
        return out
    }

    private fun parseWorkbookRels(zip: ZipFile): Map<String, String> {
        val entry = zip.getEntry("xl/_rels/workbook.xml.rels") ?: return emptyMap()
        val map = HashMap<String, String>()
        parseXml(zip.getInputStream(entry)) { parser ->
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && localName(parser.name) == "Relationship") {
                    val id = attr(parser, "Id")
                    val target = attr(parser, "Target")
                    if (id != null && target != null) map[id] = target
                }
                event = parser.next()
            }
        }
        return map
    }

    private fun normalizePartPath(base: String, target: String): String {
        var t = target.replace('\\', '/')
        if (t.startsWith("/")) return t.removePrefix("/")
        t = base + t
        val stack = ArrayDeque<String>()
        for (part in t.split('/')) {
            when (part) {
                "", "." -> Unit
                ".." -> if (stack.isNotEmpty()) stack.removeLast()
                else -> stack.addLast(part)
            }
        }
        return stack.joinToString("/")
    }

    // ------------------------------------------------------------------- sheet

    private fun parseSheet(zip: ZipFile, path: String, sharedStrings: List<String>): List<List<String>> {
        val entry = zip.getEntry(path) ?: return emptyList()
        val rows = mutableListOf<List<String>>()

        parseXml(zip.getInputStream(entry)) { parser ->
            var rowCells = HashMap<Int, String>()
            var maxCol = -1
            var cellCol = -1
            var cellType: String? = null
            var cellValue: StringBuilder? = null
            var inInlineString = false
            var event = parser.eventType

            fun finishCell() {
                if (cellCol >= 0) {
                    val raw = cellValue?.toString()?.trim().orEmpty()
                    val resolved = when (cellType) {
                        "s" -> raw.toIntOrNull()?.let { sharedStrings.getOrNull(it) }.orEmpty()
                        "b" -> if (raw == "1") "TRUE" else "FALSE"
                        "str", "inlineStr" -> raw
                        else -> trimNumber(raw)
                    }
                    rowCells[cellCol] = resolved
                    if (cellCol > maxCol) maxCol = cellCol
                }
                cellCol = -1; cellType = null; cellValue = null; inInlineString = false
            }

            while (event != XmlPullParser.END_DOCUMENT) {
                val local = localName(parser.name)
                when (event) {
                    XmlPullParser.START_TAG -> when (local) {
                        "row" -> { rowCells = HashMap(); maxCol = -1 }
                        "c" -> {
                            cellCol = colLettersToIndex(columnLetters(attr(parser, "r")))
                            cellType = attr(parser, "t")
                            cellValue = StringBuilder()
                        }
                        "v" -> cellValue?.append(parser.nextText())
                        "is" -> inInlineString = true
                        "t" -> if (inInlineString) cellValue?.append(parser.nextText())
                    }
                    XmlPullParser.END_TAG -> when (local) {
                        "c" -> finishCell()
                        "is" -> inInlineString = false
                        "row" -> {
                            if (maxCol >= 0) {
                                rows += List(maxCol + 1) { idx -> rowCells[idx].orEmpty() }
                            }
                            rowCells = HashMap(); maxCol = -1
                        }
                    }
                }
                event = parser.next()
            }
        }
        return rows
    }

    /** "B12" -> "B" */
    private fun columnLetters(cellRef: String?): String? =
        cellRef?.takeWhile { it.isLetter() }?.ifEmpty { null }

    /** "A" -> 0, "B" -> 1 … "AA" -> 26 */
    private fun colLettersToIndex(letters: String?): Int {
        if (letters == null) return -1
        var index = 0
        for (c in letters.uppercase()) {
            if (c !in 'A'..'Z') return -1
            index = index * 26 + (c - 'A' + 1)
        }
        return index - 1
    }

    /** 41000.0 -> "41000" (keeps integers tidy in generated PDFs). */
    private fun trimNumber(raw: String): String {
        if (raw.isEmpty()) return raw
        val d = raw.toDoubleOrNull() ?: return raw
        return if (d == Math.floor(d) && !d.isInfinite() && Math.abs(d) < 1e15) d.toLong().toString() else raw
    }

    // ------------------------------------------------------------------ shared

    private fun localName(name: String?): String {
        if (name == null) return ""
        val idx = name.indexOf(':')
        return if (idx >= 0) name.substring(idx + 1) else name
    }

    private fun attr(parser: XmlPullParser, local: String): String? {
        for (i in 0 until parser.attributeCount) {
            if (localName(parser.getAttributeName(i)).equals(local, ignoreCase = true)) {
                return parser.getAttributeValue(i)
            }
        }
        return null
    }

    private inline fun parseXml(input: InputStream, block: (XmlPullParser) -> Unit) {
        input.use { stream ->
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(stream, "UTF-8")
            block(parser)
        }
    }
}
