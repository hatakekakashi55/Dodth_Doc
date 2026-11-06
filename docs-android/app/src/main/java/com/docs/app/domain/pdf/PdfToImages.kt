package com.docs.app.domain.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.docs.app.core.ZipX
import java.io.File
import java.io.FileOutputStream

/**
 * Rasterizes every page of a PDF to a high-quality JPEG (2x scale)
 * and packs them into a single .zip archive — one image per page.
 */
object PdfToImages {

    private const val RENDER_SCALE = 2.0f
    private const val MAX_BITMAP_EDGE = 2800
    private const val JPEG_QUALITY = 90

    /** Returns the number of pages rasterized. */
    fun convert(input: File, zipOut: File, tempDir: File): Int {
        val descriptor = ParcelFileDescriptor.open(input, ParcelFileDescriptor.MODE_READ_ONLY)
            ?: throw IllegalStateException("Could not open this PDF. It may be corrupted.")
        val renderer = try {
            PdfRenderer(descriptor)
        } catch (e: Exception) {
            descriptor.close()
            throw IllegalStateException("This PDF could not be rendered. It may be corrupted or password-protected.")
        }

        val tempFiles = mutableListOf<File>()
        val entryNames = mutableListOf<String>()
        try {
            for (index in 0 until renderer.pageCount) {
                renderer.openPage(index).use { page ->
                    val scale = minOf(RENDER_SCALE, MAX_BITMAP_EDGE / maxOf(page.width, page.height).toFloat())
                    val widthPx = (page.width * scale).toInt().coerceAtLeast(1)
                    val heightPx = (page.height * scale).toInt().coerceAtLeast(1)
                    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
                    try {
                        // Force white page background (transparent PDFs look black otherwise)
                        Canvas(bitmap).drawColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val name = "page_${index + 1}.jpg"
                        val temp = File(tempDir, "img_${System.currentTimeMillis()}_$name")
                        FileOutputStream(temp).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                        }
                        tempFiles += temp
                        entryNames += name
                    } finally {
                        bitmap.recycle()
                    }
                }
            }
            ZipX.zipFiles(tempFiles, entryNames, zipOut)
            return tempFiles.size
        } finally {
            renderer.close()
            descriptor.close()
            tempFiles.forEach { it.delete() }
        }
    }
}
