package com.docs.app.domain.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/** Shared page chrome: a hairline rule above a centered "Page N" caption. */
internal object PageFurniture {

    private val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E3E5EA")
        strokeWidth = 0.8f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9CA3AF")
        textSize = 8f
        textAlign = Paint.Align.CENTER
    }

    fun drawFooter(canvas: Canvas, pageNumber: Int, pageWidth: Int, pageHeight: Int, marginX: Float) {
        val y = pageHeight - 30f
        canvas.drawLine(marginX, y - 6f, pageWidth - marginX, y - 6f, rulePaint)
        canvas.drawText("Page $pageNumber", pageWidth / 2f, y + 4f, textPaint)
    }
}
