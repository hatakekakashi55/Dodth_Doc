package com.docs.app.domain.convert

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

/**
 * Combines JPG/PNG/WebP/BMP images into a single paginated A4 PDF.
 * Each image is centered and scaled to fit inside comfortable margins.
 */
object ImagesToPdf {

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 36f

    fun convert(images: List<File>, out: File): Int {
        if (images.isEmpty()) throw IllegalArgumentException("Select at least one image.")
        val document = PdfDocument()
        var pageNumber = 0
        try {
            images.forEachIndexed { index, image ->
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(image.absolutePath, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                    return@forEachIndexed // skip undecodable images
                }
                // Decode with a memory-friendly sample size
                var sample = 1
                val target = 1600
                while (bounds.outWidth / (sample * 2) >= target) sample *= 2
                val bitmap = BitmapFactory.decodeFile(
                    image.absolutePath,
                    BitmapFactory.Options().apply { inSampleSize = sample }
                ) ?: return@forEachIndexed

                pageNumber += 1
                val info = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNumber).create()
                val page = document.startPage(info)
                val canvas = page.canvas
                canvas.drawColor(Color.WHITE)

                val maxW = PAGE_W - 2 * MARGIN
                val maxH = PAGE_H - 2 * MARGIN
                val scale = minOf(maxW / bitmap.width, maxH / bitmap.height, 1f)
                val w = bitmap.width * scale
                val h = bitmap.height * scale
                canvas.drawBitmap(
                    bitmap,
                    null,
                    android.graphics.RectF((PAGE_W - w) / 2f, (PAGE_H - h) / 2f, (PAGE_W + w) / 2f, (PAGE_H + h) / 2f),
                    null
                )
                bitmap.recycle()
                document.finishPage(page)
            }
            if (pageNumber == 0) {
                throw IllegalStateException("None of the selected files could be decoded as images.")
            }
            FileOutputStream(out).use { document.writeTo(it) }
            return pageNumber
        } finally {
            document.close()
        }
    }
}
