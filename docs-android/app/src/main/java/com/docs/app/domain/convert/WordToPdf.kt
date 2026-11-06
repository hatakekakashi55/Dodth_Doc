package com.docs.app.domain.convert

import com.docs.app.domain.render.FlowAlign
import com.docs.app.domain.render.FlowBlock
import com.docs.app.domain.render.FlowImage
import com.docs.app.domain.render.FlowParagraph
import com.docs.app.domain.render.FlowSpan
import com.docs.app.domain.render.PdfComposer
import com.docs.app.domain.ooxml.DocxBlock
import com.docs.app.domain.ooxml.DocxParagraphKind
import com.docs.app.domain.ooxml.DocxReader
import com.docs.app.domain.ooxml.DocxText
import com.docs.app.domain.ooxml.DocxPicture
import java.io.File

/**
 * Converts Word (.docx) to PDF fully on-device:
 * OOXML parse → flowable layout → paginated A4 PDF with footer.
 */
object WordToPdf {

    private const val INK_DARK = 0xFF111827.toInt()
    private const val INK_BODY = 0xFF1F2937.toInt()

    fun convert(input: File, out: File) {
        val document = DocxReader.read(input)
        val blocks = mutableListOf<FlowBlock>()

        for (block in document.blocks) {
            when (block) {
                is DocxText -> blocks += paragraphToFlow(block.paragraph)
                is DocxPicture -> {
                    document.images[block.relsId]?.let { bytes ->
                        blocks += FlowImage(bytes, maxHeightPt = 380f)
                    }
                }
            }
        }
        PdfComposer.render(blocks, out)
    }

    private fun paragraphToFlow(p: com.docs.app.domain.ooxml.DocxParagraph): FlowParagraph {
        val kind = p.kind
        val kindSize = when (kind) {
            DocxParagraphKind.Title -> 24f
            DocxParagraphKind.Heading1 -> 17.5f
            DocxParagraphKind.Heading2 -> 14f
            DocxParagraphKind.Heading3 -> 12f
            DocxParagraphKind.Body -> 10.5f
        }
        val isHeading = kind != DocxParagraphKind.Body
        val ink = if (isHeading) INK_DARK else INK_BODY

        val spans = p.inlines.map { part ->
            FlowSpan(
                text = part.text,
                bold = part.bold || isHeading,
                italic = part.italic,
                underline = part.underline,
                sizePt = if (isHeading && part.sizePt == 10.5f) kindSize else part.sizePt,
                colorArgb = part.colorArgb?.toInt() ?: ink
            )
        }

        val align = when {
            p.center -> FlowAlign.CENTER
            p.right -> FlowAlign.END
            else -> FlowAlign.START
        }
        val spaceAfter = when (kind) {
            DocxParagraphKind.Title -> 12f
            DocxParagraphKind.Heading1 -> 9f
            DocxParagraphKind.Heading2 -> 7f
            DocxParagraphKind.Heading3 -> 6f
            DocxParagraphKind.Body -> 5f
        }
        return FlowParagraph(
            spans = spans,
            align = align,
            bullet = p.bullet,
            spaceAfterPt = spaceAfter,
            lineSpacingMultiplier = if (isHeading) 1.08f else 1.16f
        )
    }
}
