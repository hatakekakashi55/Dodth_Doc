package com.docs.app.domain.ocr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.docs.app.core.await
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

/**
 * On-device OCR pipeline (Google ML Kit, Latin script):
 * PDF page → 2-3x bitmap → text recognition → rebuilt PDF with the page image
 * plus a fully transparent, selectable text layer (searchable PDF).
 *
 * The recognition model downloads once through Play services (~4 MB);
 * after that the whole process works offline.
 */
object OcrProcessor {

    private const val MAX_EDGE = 2400f

    /** Returns the number of pages OCR'd. Throws on unrecoverable failure. */
    suspend fun process(input: File, outFile: File): Int {
        val descriptor = ParcelFileDescriptor.open(input, ParcelFileDescriptor.MODE_READ_ONLY)
            ?: throw IllegalStateException("Could not open this PDF. It may be corrupted.")
        val renderer = try {
            PdfRenderer(descriptor)
        } catch (e: Exception) {
            descriptor.close()
            throw IllegalStateException("This PDF could not be rendered for OCR. It may be corrupted or password-protected.")
        }

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val outputDoc = PdfDocument()
        var ocrFailures = 0
        var emptyPages = 0

        var pagesWritten = 0

        try {
            for (index in 0 until renderer.pageCount) {
                renderer.openPage(index).use { page ->
                    val pageW = page.width
                    val pageH = page.height
                    val scale = min(3f, MAX_EDGE / maxOf(pageW, pageH))
                    val bitmap = Bitmap.createBitmap(
                        (pageW * scale).toInt().coerceAtLeast(1),
                        (pageH * scale).toInt().coerceAtLeast(1),
                        Bitmap.Config.ARGB_8888
                    )
                    Canvas(bitmap).drawColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    val recognized = try {
                        val image = InputImage.fromBitmap(bitmap, 0)
                        recognizer.process(image).await()
                    } catch (e: Exception) {
                        ocrFailures += 1
                        null
                    }

                    if (recognized != null && recognized.text.isBlank()) {
                        // Page rendered but contained no recognizable text — still kept, image-only
                    }

                    // --- rebuild the page: image + invisible text overlay ---
                    val info = PdfDocument.PageInfo.Builder(pageW, pageH, index + 1).create()
                    val outPage = outputDoc.startPage(info)
                    val canvas = outPage.canvas
                    canvas.drawBitmap(
                        bitmap,
                        null,
                        android.graphics.RectF(0f, 0f, pageW.toFloat(), pageH.toFloat()),
                        null
                    )
                    recognized?.let { drawInvisibleTextLayer(canvas, it, scale) }
                    outputDoc.finishPage(outPage)
                    bitmap.recycle()
                    pagesWritten += 1
                }
            }
        } finally {
            recognizer.close()
            renderer.close()
            descriptor.close()
        }

        if (pagesWritten == 0) {
            outputDoc.close()
            throw IllegalStateException("OCR could not process any page of this PDF.")
        }
        if (ocrFailures == pagesWritten) {
            outputDoc.close()
            throw IllegalStateException(
                "OCR could not run. Check that Google Play services is available and online for the first run (the model downloads once)."
            )
        }

        FileOutputStream(outFile).use { outputDoc.writeTo(it) }
        outputDoc.close()
        return pagesWritten
    }

    private fun drawInvisibleTextLayer(canvas: Canvas, recognized: Text, scale: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(0, 0, 0, 0) // fully transparent glyphs
        }
        for (block in recognized.textBlocks) {
            for (line in block.lines) {
                val box = line.boundingBox ?: continue
                val heightPts = box.height() / scale
                if (heightPts <= 0f) continue
                paint.textSize = (heightPts * 0.82f).coerceIn(2f, 200f)
                canvas.drawText(
                    line.text,
                    box.left / scale,
                    box.bottom / scale,
                    paint
                )
            }
        }
    }
}
