package com.docs.app.domain.convert

import android.text.Html
import android.text.Spanned
import android.text.Typeface
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import android.text.style.URLSpan
import android.text.style.CharacterStyle
import com.docs.app.domain.render.FlowBlock
import com.docs.app.domain.render.FlowParagraph
import com.docs.app.domain.render.FlowSpan
import com.docs.app.domain.render.PdfComposer
import java.io.File

/**
 * Converts HTML files to PDF fully on-device using the framework HTML parser.
 * Headings, emphasis, underline, links and lists survive as styled text;
 * layout becomes a clean paginated document.
 */
object HtmlToPdf {

    private const val INK = 0xFF1F2937.toInt()
    private const val LINK = 0xFF4F46E5.toInt()

    fun convert(input: File, out: File) {
        val html = readHtml(input)
        @Suppress("DEPRECATION")
        val spanned: Spanned = Html.fromHtml(html, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
        val blocks = spannedToFlowBlocks(spanned)
        PdfComposer.render(blocks, out)
    }

    private fun readHtml(input: File): String {
        val bytes = input.readBytes()
        val head = String(bytes.take(2048).toByteArray(), Charsets.ISO_8859_1)
        val match = Regex("charset\\s*=\\s*[\"']?([\\w-]+)", RegexOption.IGNORE_CASE).find(head)
        val charset = when (match?.groupValues?.getOrNull(1)?.lowercase()) {
            "iso-8859-1", "latin1", "windows-1252" -> Charsets.ISO_8859_1
            else -> Charsets.UTF_8
        }
        return String(bytes, charset)
    }

    /** Splits a Spanned into styled segments, then groups them into paragraphs on newlines. */
    private fun spannedToFlowBlocks(spanned: Spanned): List<FlowBlock> {
        val length = spanned.length
        if (length == 0) return emptyList()

        val spans = spanned.getSpans(0, length, CharacterStyle::class.java)
        val breakpoints = sortedSetOf(0, length)
        spans.forEach { span ->
            breakpoints += spanned.getSpanStart(span)
            breakpoints += spanned.getSpanEnd(span)
        }
        val points = breakpoints.toList()

        data class Segment(
            val text: String,
            val bold: Boolean,
            val italic: Boolean,
            val underline: Boolean,
            val sizeMult: Float,
            val color: Int?
        )

        val segments = mutableListOf<Segment>()
        for (i in 0 until points.lastIndex) {
            val a = points[i]; val b = points[i + 1]
            if (a >= b) continue
            var bold = false
            var italic = false
            var underline = false
            var sizeMult = 1f
            var color: Int? = null
            for (span in spans) {
                if (spanned.getSpanStart(span) > a || spanned.getSpanEnd(span) < b) continue
                when (span) {
                    is StyleSpan -> when (span.style) {
                        Typeface.BOLD -> bold = true
                        Typeface.ITALIC -> italic = true
                        Typeface.BOLD_ITALIC -> { bold = true; italic = true }
                    }
                    is TypefaceSpan -> if (span.family.equals("serif", ignoreCase = true)) Unit
                    is UnderlineSpan -> underline = true
                    is URLSpan -> { underline = true; color = LINK }
                    is ForegroundColorSpan -> if (color == null) color = span.foregroundColor
                    is RelativeSizeSpan -> sizeMult *= span.sizeChange
                    is AbsoluteSizeSpan -> sizeMult *= span.size / 10f
                }
            }
            segments += Segment(spanned.subSequence(a, b).toString(), bold, italic, underline, sizeMult, color)
        }

        val blocks = mutableListOf<FlowBlock>()
        var current = mutableListOf<FlowSpan>()

        fun flush() {
            val text = current.joinToString("") { it.text }
            if (text.isNotBlank()) {
                blocks += FlowParagraph(spans = current.toList(), spaceAfterPt = 6f)
            }
            current = mutableListOf()
        }

        for (segment in segments) {
            val parts = segment.text.split("\n")
            parts.forEachIndexed { index, part ->
                if (part.isNotEmpty()) {
                    val sizePt = (10.5f * segment.sizeMult).coerceIn(4f, 72f)
                    current += FlowSpan(
                        text = part,
                        bold = segment.bold,
                        italic = segment.italic,
                        underline = segment.underline,
                        sizePt = sizePt,
                        colorArgb = segment.color ?: INK
                    )
                }
                if (index < parts.lastIndex) flush()
            }
        }
        flush()
        return blocks
    }
}
