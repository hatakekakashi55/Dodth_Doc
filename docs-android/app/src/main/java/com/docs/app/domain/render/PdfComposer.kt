package com.docs.app.domain.render

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import java.io.File
import java.io.FileOutputStream

/**
 * Flowable-content PDF page composer built on Android's framework PdfDocument.
 *
 * Handles word wrap, per-span styling (weight / slant / underline / size /
 * color), hard line breaks, embedded images and automatic pagination across
 * A4 pages with a subtle "Page N" footer.
 */
object PdfComposer {

    const val PAGE_WIDTH = 595   // A4 @ 72 dpi, points
    const val PAGE_HEIGHT = 842
    const val MARGIN_X = 56f
    const val MARGIN_TOP = 54f
    const val MARGIN_BOTTOM = 56f
    private const val BASE_TEXT_SIZE = 10f

    /** Renders [blocks] to [output]. Returns the number of pages written. */
    fun render(blocks: List<FlowBlock>, output: File): Int {
        val effectiveBlocks = blocks.ifEmpty {
            listOf(
                FlowParagraph(
                    spans = listOf(
                        FlowSpan(
                            "No extractable text was found in this document. " +
                                "It may be a scanned file — try OCR PDF instead.",
                            italic = true,
                            colorArgb = 0xFF6B7280.toInt()
                        )
                    )
                )
            )
        }

        val pdfDocument = PdfDocument()
        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var y = MARGIN_TOP
        val contentWidth = (PAGE_WIDTH - 2 * MARGIN_X).toInt()
        val pageBottom = PAGE_HEIGHT - MARGIN_BOTTOM

        fun newPage() {
            pageNumber += 1
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            page = pdfDocument.startPage(info)
            page!!.canvas.drawColor(Color.WHITE)
            y = MARGIN_TOP
        }

        fun finishPage() {
            val current = page ?: return
            PageFurniture.drawFooter(current.canvas, pageNumber, PAGE_WIDTH, PAGE_HEIGHT, MARGIN_X)
            pdfDocument.finishPage(current)
            page = null
        }

        newPage()

        for (block in effectiveBlocks) {
            when (block) {
                is FlowParagraph -> {
                    val layout = buildLayout(block, contentWidth)
                    var lineStart = 0
                    while (lineStart < layout.lineCount) {
                        val remaining = pageBottom - y
                        var lineEnd = lineStart
                        while (lineEnd < layout.lineCount &&
                            layout.getLineBottom(lineEnd) - layout.getLineTop(lineStart) <= remaining
                        ) {
                            lineEnd += 1
                        }
                        if (lineEnd == lineStart) {
                            if (y <= MARGIN_TOP + 0.5f) {
                                // Fresh page but a single line still doesn't fit — draw it clipped.
                                lineEnd = lineStart + 1
                            } else {
                                finishPage()
                                newPage()
                                continue
                            }
                        }
                        val target = page!!.canvas
                        target.save()
                        target.clipRect(0, y.toInt(), PAGE_WIDTH, (y + remaining).toInt() + 1)
                        target.translate(MARGIN_X, y - layout.getLineTop(lineStart))
                        layout.draw(target)
                        target.restore()
                        y += layout.getLineBottom(lineEnd - 1) - layout.getLineTop(lineStart)
                        lineStart = lineEnd
                        if (lineStart < layout.lineCount) {
                            finishPage()
                            newPage()
                        }
                    }
                    y += block.spaceAfterPt
                }

                is FlowImage -> {
                    val bitmap = decodeToFit(block.bytes, contentWidth.toFloat(), block.maxHeightPt)
                    if (bitmap == null) continue
                    if (y + bitmap.height > pageBottom && y > MARGIN_TOP) {
                        finishPage()
                        newPage()
                    }
                    page!!.canvas.drawBitmap(
                        bitmap,
                        MARGIN_X + (contentWidth - bitmap.width) / 2f,
                        y,
                        null
                    )
                    y += bitmap.height + 12f
                    bitmap.recycle()
                }
            }
        }

        finishPage()
        FileOutputStream(output).use { pdfDocument.writeTo(it) }
        pdfDocument.close()
        return pageNumber
    }

    private fun buildLayout(block: FlowParagraph, contentWidth: Int): StaticLayout {
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = BASE_TEXT_SIZE
            color = 0xFF1F2937.toInt()
        }

        val builder = SpannableStringBuilder()
        val bulletSize = block.spans.firstOrNull()?.sizePt ?: 10.5f
        val spans = if (block.bullet) {
            listOf(FlowSpan("•  ", sizePt = bulletSize, colorArgb = 0xFF4F46E5.toInt())) + block.spans
        } else block.spans

        val flags = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        val maxSize = spans.maxOfOrNull { it.sizePt } ?: 10.5f
        for (span in spans) {
            if (span.text.isEmpty()) continue
            val start = builder.length
            builder.append(span.text)
            builder.setSpan(StyleSpan(typefaceStyle(span.bold, span.italic)), start, builder.length, flags)
            if (span.underline) builder.setSpan(UnderlineSpan(), start, builder.length, flags)
            builder.setSpan(ForegroundColorSpan(span.colorArgb), start, builder.length, flags)
            builder.setSpan(RelativeSizeSpan(span.sizePt / BASE_TEXT_SIZE), start, builder.length, flags)
        }

        return StaticLayout.Builder.obtain(builder, 0, builder.length, textPaint, contentWidth)
            .setAlignment(
                when (block.align) {
                    FlowAlign.START -> Layout.Alignment.ALIGN_NORMAL
                    FlowAlign.CENTER -> Layout.Alignment.ALIGN_CENTER
                    FlowAlign.END -> Layout.Alignment.ALIGN_OPPOSITE
                }
            )
            .setLineSpacing(maxSize * 0.14f, block.lineSpacingMultiplier)
            .setIncludePad(false)
            .build()
    }

    private fun typefaceStyle(bold: Boolean, italic: Boolean): Int = when {
        bold && italic -> Typeface.BOLD_ITALIC
        bold -> Typeface.BOLD
        italic -> Typeface.ITALIC
        else -> Typeface.NORMAL
    }

    private fun decodeToFit(bytes: ByteArray, maxWidth: Float, maxHeight: Float): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        // Decode at half-resolution steps until we're near the target size.
        var sample = 1
        val targetWidth = (maxWidth * 2).toInt().coerceAtLeast(256)
        while (bounds.outWidth / (sample * 2) >= targetWidth) sample *= 2

        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return null

        val scale = minOf(maxWidth / decoded.width, maxHeight / decoded.height, 2f)
        return if (scale < 0.99f || scale > 1.01f) {
            val scaled = Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true
            )
            if (scaled != decoded) decoded.recycle()
            scaled
        } else {
            decoded
        }
    }
}
