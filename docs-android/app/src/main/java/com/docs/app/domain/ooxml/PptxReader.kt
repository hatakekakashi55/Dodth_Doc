package com.docs.app.domain.ooxml

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

/**
 * Minimal OOXML (.pptx) reader.
 *
 * Returns the text paragraphs of every slide, in slide order.
 * Formatting is intentionally flattened; the first non-empty paragraph
 * of a slide is treated as its title by callers.
 */
object PptxReader {

    /** One entry per slide: its non-empty text paragraphs. */
    fun readSlides(file: File): List<List<String>> {
        ZipFile(file).use { zip ->
            val slideNames = zip.entries().asSequence()
                .map { it.name }
                .filter { isSlidePath(it) }
                .sortedBy { slideNumber(it) }
                .toList()

            return slideNames.map { path ->
                val entry = zip.getEntry(path) ?: return@map emptyList()
                parseSlideText(zip.getInputStream(entry))
            }
        }
    }

    private val SLIDE_REGEX = Regex("^ppt/slides/slide(\\d+)\\.xml$")

    private fun isSlidePath(name: String): Boolean = SLIDE_REGEX.matches(name)

    private fun slideNumber(name: String): Int =
        SLIDE_REGEX.find(name)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: Int.MAX_VALUE

    private fun parseSlideText(input: InputStream): List<String> {
        val paragraphs = mutableListOf<String>()
        parseXml(input) { parser ->
            var current = StringBuilder()
            var inParagraph = false
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                val local = localName(parser.name)
                when (event) {
                    XmlPullParser.START_TAG -> when (local) {
                        "p" -> { if (!inParagraph) { current = StringBuilder(); inParagraph = true } }
                        "t" -> if (inParagraph) current.append(parser.nextText())
                    }
                    XmlPullParser.END_TAG -> if (local == "p" && inParagraph) {
                        val text = current.toString().trim()
                        if (text.isNotEmpty()) paragraphs += text
                        inParagraph = false
                    }
                }
                event = parser.next()
            }
        }
        return paragraphs
    }

    private fun localName(name: String?): String {
        if (name == null) return ""
        val idx = name.indexOf(':')
        return if (idx >= 0) name.substring(idx + 1) else name
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
