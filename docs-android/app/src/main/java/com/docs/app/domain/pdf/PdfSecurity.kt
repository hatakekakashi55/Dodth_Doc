package com.docs.app.domain.pdf

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Security operations: 256-bit AES protection, password removal and
 * diagonal text watermarking — all on-device.
 */
object PdfSecurity {

    /** Encrypts [input] with 256-bit AES (user == owner password). */
    fun protect(input: File, out: File, password: String) {
        if (password.isBlank()) throw IllegalArgumentException("A password is required to protect the PDF.")
        val document = try {
            PDDocument.load(input)
        } catch (e: InvalidPasswordException) {
            throw IllegalStateException("This PDF is already password-protected.")
        } catch (e: Exception) {
            throw IllegalStateException("Could not read this PDF. It may be corrupted or not a real PDF file.")
        }
        document.use { doc ->
            val permissions = AccessPermission() // defaults: allow print/copy, keep structure locked
            val policy = StandardProtectionPolicy(password, password, permissions)
            policy.encryptionKeyLength = 256
            doc.protect(policy)
            doc.documentInformation.producer = "Docs 1.0 (protected)"
            FileOutputStream(out).use { doc.save(it) }
        }
    }

    /** Removes password protection. Throws a friendly error on a wrong password. */
    fun unlock(input: File, out: File, password: String) {
        if (password.isBlank()) throw IllegalArgumentException("Enter the current password to unlock the PDF.")
        val document = try {
            PDDocument.load(input, password)
        } catch (e: InvalidPasswordException) {
            throw IllegalStateException("Incorrect password — the PDF was not unlocked.")
        } catch (e: Exception) {
            throw IllegalStateException("Could not read this PDF. It may be corrupted or already unlocked.")
        }
        document.use { doc ->
            doc.setAllSecurityToBeRemoved(true)
            doc.documentInformation.producer = "Docs 1.0 (unlocked)"
            FileOutputStream(out).use { doc.save(it) }
        }
    }

    /**
     * Stamps [text] diagonally across every page.
     * [opacity] in 0.0..1.0 — mapped to a light neutral gray (true alpha text
     * is unreliable across PDF viewers, so contrast is tuned instead).
     */
    fun watermark(input: File, out: File, text: String, opacity: Float) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) throw IllegalArgumentException("Watermark text cannot be empty.")
        val o = opacity.coerceIn(0.05f, 1.0f)
        // More opacity => darker gray. Ranges roughly #DEDEDE .. #6B6B6B.
        val component = (1f - 0.58f * o).coerceIn(0.42f, 0.93f)
        val document = try {
            PDDocument.load(input)
        } catch (e: InvalidPasswordException) {
            throw IllegalStateException("This PDF is password-protected. Unlock it before watermarking.")
        }
        document.use { doc ->
            val font = PDType1Font.HELVETICA_BOLD
            for (page in doc.pages) {
                val width = page.mediaBox.width
                val height = page.mediaBox.height
                // Fit the stamp to ~80% of the page diagonal
                val diagonal = Math.hypot(width.toDouble(), height.toDouble()).toFloat()
                val sizeFromWidth = diagonal * 0.055f
                val sizeFromText = diagonal * 0.8f / (cleanText.length * 0.58f)
                val fontSize = min(sizeFromWidth, max(14f, sizeFromText))

                val textWidth = font.getStringWidth(cleanText) / 1000f * fontSize
                val angle = Math.toRadians(45.0)
                PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { stream ->
                    stream.saveGraphicsState()
                    // Rotate 45° around the page center; text is drawn half its width left of origin
                    stream.transform(
                        com.tom_roush.pdfbox.util.Matrix.getRotateInstance(angle, width / 2f, height / 2f)
                    )
                    stream.beginText()
                    stream.setFont(font, fontSize)
                    stream.setNonStrokingColor(component, component, component)
                    stream.newLineAtOffset(-textWidth / 2f, 0f)
                    stream.showText(cleanText)
                    stream.endText()
                    stream.restoreGraphicsState()
                }
            }
            doc.documentInformation.producer = "Docs 1.0 (watermarked)"
            FileOutputStream(out).use { doc.save(it) }
        }
    }
}
