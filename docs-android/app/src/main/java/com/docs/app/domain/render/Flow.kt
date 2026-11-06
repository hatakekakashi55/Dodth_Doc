package com.docs.app.domain.render

/** A styled text run ("span") in flowable document content. */
data class FlowSpan(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val sizePt: Float = 10.5f,
    val colorArgb: Int = 0xFF1F2937.toInt()
)

enum class FlowAlign { START, CENTER, END }

/** Block-level flowable content accepted by [PdfComposer]. */
sealed interface FlowBlock

data class FlowParagraph(
    val spans: List<FlowSpan>,
    val align: FlowAlign = FlowAlign.START,
    val bullet: Boolean = false,
    val spaceAfterPt: Float = 6f,
    val lineSpacingMultiplier: Float = 1.16f
) : FlowBlock

data class FlowImage(
    val bytes: ByteArray,
    val maxHeightPt: Float = 420f
) : FlowBlock
