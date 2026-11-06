package com.docs.app.domain.ooxml

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

/** A styled run of text inside a paragraph. */
data class DocxInline(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val sizePt: Float = 10.5f,
    val colorArgb: Long? = null
)

enum class DocxParagraphKind { Title, Heading1, Heading2, Heading3, Body }

data class DocxParagraph(
    val inlines: List<DocxInline>,
    val kind: DocxParagraphKind = DocxParagraphKind.Body,
    val center: Boolean = false,
    val right: Boolean = false,
    val bullet: Boolean = false
)

sealed interface DocxBlock
data class DocxText(val paragraph: DocxParagraph) : DocxBlock
data class DocxPicture(val relsId: String) : DocxBlock

/** Parsed representation of a .docx file. */
data class DocxDocument(
    val blocks: List<DocxBlock>,
    /** rId -> decoded image bytes for every picture referenced in the document. */
    val images: Map<String, ByteArray>
)

/**
 * Minimal-but-real OOXML (.docx) reader.
 *
 * Extracts paragraphs (with bold/italic/underline/size/color per run),
 * heading levels from styles.xml, list bullets, text alignment and
 * embedded raster images resolved through document.xml.rels.
 *
 * Not supported (kept as clean plain text): tables (cells flatten to lines),
 * tracked changes, comments, and vector (EMF/WMF) images.
 */
object DocxReader {

    fun read(file: File): DocxDocument {
        ZipFile(file).use { zip ->
            val styles = parseStyles(zip)          // styleId -> heading level (0 = Title)
            val rels = parseRels(zip)              // Id -> target path (relative to word/)
            val (blocks, referencedImageIds) = parseDocument(zip, styles)

            val images = HashMap<String, ByteArray>()
            referencedImageIds.forEach { id ->
                val target = rels[id] ?: return@forEach
                val mediaPath = normalizeMediaPath(target)
                val entry = zip.getEntry(mediaPath) ?: return@forEach
                // Skip vector EMF/WMF payloads — they cannot be decoded by BitmapFactory
                val lower = mediaPath.lowercase()
                if (lower.endsWith(".emf") || lower.endsWith(".wmf") || lower.endsWith(".pict")) return@forEach
                images[id] = zip.getInputStream(entry).use { it.readBytes() }
            }
            return DocxDocument(blocks, images)
        }
    }

    // ------------------------------------------------------------------ styles

    private fun parseStyles(zip: ZipFile): Map<String, Int> {
        val entry = zip.getEntry("word/styles.xml") ?: return emptyMap()
        val map = HashMap<String, Int>()
        parseXml(zip.getInputStream(entry)) { parser ->
            var currentStyleId: String? = null
            var currentOutline: Int? = null
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                val local = localName(parser.name)
                when (event) {
                    XmlPullParser.START_TAG -> when (local) {
                        "style" -> {
                            currentStyleId = attr(parser, "styleId")?.lowercase()
                            currentOutline = null
                        }
                        "outlineLvl" -> {
                            currentOutline = attr(parser, "val")?.toIntOrNull()
                        }
                    }
                    XmlPullParser.END_TAG -> if (local == "style") {
                        val id = currentStyleId
                        if (id != null) {
                            val level = outlineToHeading(id, currentOutline)
                            if (level != null) map[id] = level
                        }
                        currentStyleId = null
                        currentOutline = null
                    }
                }
                event = parser.next()
            }
        }
        return map
    }

    /** heading level: 0 = Title, 1..3 = Heading1..3 */
    private fun outlineToHeading(styleId: String, outline: Int?): Int? {
        if (styleId == "title" || styleId.startsWith("title")) return 0
        val normalized = styleId.filter { it.isLetterOrDigit() }.lowercase()
        when {
            normalized.startsWith("heading") -> {
                val n = normalized.removePrefix("heading").toIntOrNull() ?: return null
                return n.coerceIn(1, 3)
            }
            normalized.contains("subtitle") -> return 3
        }
        if (outline != null) return (outline + 1).coerceIn(1, 3)
        return null
    }

    // -------------------------------------------------------------------- rels

    private fun parseRels(zip: ZipFile): Map<String, String> {
        val entry = zip.getEntry("word/_rels/document.xml.rels") ?: return emptyMap()
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

    /** Resolves a relationship target (e.g. "media/image1.png" or "../media/image1.png") against word/. */
    private fun normalizeMediaPath(target: String): String {
        var t = target.replace('\\', '/')
        if (t.startsWith("/")) t = t.removePrefix("/") else t = "word/$t"
        // Collapse "a/../b" segments
        val parts = t.split('/')
        val stack = ArrayDeque<String>()
        for (part in parts) {
            when (part) {
                "", "." -> Unit
                ".." -> if (stack.isNotEmpty()) stack.removeLast()
                else -> stack.addLast(part)
            }
        }
        return stack.joinToString("/")
    }

    // --------------------------------------------------------------- document

    private class ParaCtx {
        var kind = DocxParagraphKind.Body
        var align = "start"
        var bullet = false
        val inlines = mutableListOf<DocxInline>()
        val imageIds = mutableListOf<String>()

        // current run state
        var bold = false
        var italic = false
        var underline = false
        var sizePt = 10.5f
        var color: Long? = null

        fun resetRun() {
            bold = false; italic = false; underline = false; sizePt = 10.5f; color = null
        }

        fun appendText(text: String) {
            if (text.isEmpty()) return
            inlines += DocxInline(text, bold, italic, underline, sizePt, color)
        }
    }

    private fun parseDocument(zip: ZipFile, styles: Map<String, Int>): Pair<List<DocxBlock>, List<String>> {
        val entry = zip.getEntry("word/document.xml")
            ?: throw IllegalStateException("This file does not look like a valid .docx (missing word/document.xml).")
        val blocks = mutableListOf<DocxBlock>()
        val referencedImages = LinkedHashSet<String>()

        parseXml(zip.getInputStream(entry)) { parser ->
            var ctx: ParaCtx? = null
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                val local = localName(parser.name)
                when (event) {
                    XmlPullParser.START_TAG -> when (local) {
                        "p" -> ctx = ParaCtx()
                        "pStyle" -> {
                            val raw = attr(parser, "val")?.lowercase()
                            val compact = raw?.filter { it.isLetterOrDigit() }
                            val level = compact?.let { styles[it] } ?: raw?.let { styles[it] }
                            ctx?.kind = when (level) {
                                0 -> DocxParagraphKind.Title
                                1 -> DocxParagraphKind.Heading1
                                2 -> DocxParagraphKind.Heading2
                                3 -> DocxParagraphKind.Heading3
                                else -> DocxParagraphKind.Body
                            }
                        }
                        "numPr" -> ctx?.bullet = true
                        "jc" -> ctx?.align = attr(parser, "val")?.lowercase() ?: "start"
                        "r" -> ctx?.resetRun()
                        "b" -> if (boolAttr(parser)) ctx?.bold = true
                        "bCs" -> if (boolAttr(parser)) ctx?.bold = true
                        "i" -> if (boolAttr(parser)) ctx?.italic = true
                        "iCs" -> if (boolAttr(parser)) ctx?.italic = true
                        "u" -> {
                            val v = attr(parser, "val")?.lowercase()
                            ctx?.underline = v == null || v != "none"
                        }
                        "sz" -> attr(parser, "val")?.toFloatOrNull()?.let { ctx?.sizePt = (it / 2f).coerceIn(4f, 96f) }
                        "color" -> attr(parser, "val")?.let { parseHexColor(it)?.let { c -> ctx?.color = c } }
                        "t" -> ctx?.appendText(parser.nextText())
                        "br", "cr" -> ctx?.appendText("\n")
                        "tab" -> ctx?.appendText("\t")
                        "blip" -> attr(parser, "embed")?.let { id ->
                            ctx?.imageIds?.add(id)
                            referencedImages += id
                        }
                    }
                    XmlPullParser.END_TAG -> if (local == "p") {
                        val done = ctx
                        ctx = null
                        if (done != null) flushParagraph(done, blocks)
                    }
                }
                event = parser.next()
            }
        }
        return blocks to referencedImages.toList()
    }

    private fun flushParagraph(ctx: ParaCtx, blocks: MutableList<DocxBlock>) {
        // Merge consecutive chunks that share identical styling (word splits runs aggressively)
        val merged = ArrayList<DocxInline>(ctx.inlines.size)
        for (inline in ctx.inlines) {
            val last = merged.lastOrNull()
            if (last != null &&
                last.bold == inline.bold && last.italic == inline.italic &&
                last.underline == inline.underline && last.sizePt == inline.sizePt &&
                last.colorArgb == inline.colorArgb
            ) {
                merged[merged.size - 1] = last.copy(text = last.text + inline.text)
            } else {
                merged += inline
            }
        }
        val text = merged.joinToString("") { it.text }
        if (merged.isNotEmpty() && text.isNotBlank()) {
            val paragraph = DocxParagraph(
                inlines = merged,
                kind = ctx.kind,
                center = ctx.align == "center",
                right = ctx.align == "right" || ctx.align == "end",
                bullet = ctx.bullet
            )
            blocks += DocxText(paragraph)
        }
        ctx.imageIds.distinct().forEach { id -> blocks += DocxPicture(id) }
    }

    // --------------------------------------------------------------- helpers

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

    /** Boolean OOXML attribute: absent => true, explicit "0"/"false"/"off" => false. */
    private fun boolAttr(parser: XmlPullParser): Boolean {
        val v = attr(parser, "val")?.lowercase() ?: return true
        return v != "0" && v != "false" && v != "off" && v != "none"
    }

    private fun parseHexColor(raw: String): Long? {
        val hex = raw.trim().removePrefix("#")
        return try {
            when (hex.length) {
                6 -> 0xFF000000L or hex.toLong(16)
                8 -> hex.toLong(16)
                3 -> {
                    val r = hex[0].toString().repeat(2)
                    val g = hex[1].toString().repeat(2)
                    val b = hex[2].toString().repeat(2)
                    0xFF000000L or (r + g + b).toLong(16)
                }
                else -> null
            }
        } catch (e: NumberFormatException) {
            null
        }
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
