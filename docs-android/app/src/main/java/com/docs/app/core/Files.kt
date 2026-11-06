package com.docs.app.core

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

/** A user-selected file copied into the app cache for processing. */
data class PickedFile(
    val uri: String,
    val name: String,
    val size: Long,
    val mimeType: String
)

data class CachedInput(
    val file: File,
    val displayName: String,
    val mimeType: String
)

/**
 * File-system helpers: picking, caching, saving to Downloads,
 * opening and sharing outputs through a FileProvider.
 */
object Files {

    private const val TAG = "DocsFiles"
    const val MIME_PDF = "application/pdf"
    const val MIME_ZIP = "application/zip"
    const val MIME_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    const val MIME_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    const val MIME_PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation"

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val k = 1024.0
        val sizes = arrayOf("B", "KB", "MB", "GB")
        val i = (Math.log(bytes.toDouble()) / Math.log(k)).toInt().coerceAtMost(sizes.size - 1)
        val v = bytes / Math.pow(k, i.toDouble())
        return String.format(Locale.US, "%.1f %s", v, sizes[i])
    }

    /** Cache directory that holds intermediate inputs/outputs for the current session. */
    fun processingDir(context: Context): File =
        File(context.cacheDir, "processing").apply { mkdirs() }

    fun ocrDir(context: Context): File =
        File(context.cacheDir, "ocr").apply { mkdirs() }

    fun freshOutputFile(context: Context, suggestedName: String): File {
        val dir = processingDir(context)
        val safe = sanitizeFileName(suggestedName)
        val unique = "out_${System.currentTimeMillis()}_$safe"
        return File(dir, unique)
    }

    fun sanitizeFileName(name: String): String {
        val cleaned = name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        return if (cleaned.isEmpty()) "document" else cleaned.take(120)
    }

    fun stripExtension(name: String): String = name.substringBeforeLast('.', name)

    /** Read display name + size for a content Uri. Falls back gracefully. */
    fun queryDisplayName(context: Context, uri: Uri): String {
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) c.getString(idx)?.let { if (it.isNotBlank()) return it }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Display name query failed for $uri", e)
        }
        val fallback = uri.lastPathSegment ?: "document"
        return fallback.substringAfterLast('/').ifBlank { "document" }
    }

    fun querySize(context: Context, uri: Uri): Long {
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.SIZE)
                    if (idx >= 0 && !c.isNull(idx)) return c.getLong(idx)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Size query failed for $uri", e)
        }
        return -1L
    }

    fun describe(context: Context, uri: Uri): PickedFile =
        PickedFile(
            uri = uri.toString(),
            name = queryDisplayName(context, uri),
            size = querySize(context, uri),
            mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
        )

    /** Copies picked URIs into the app cache so engines can work on real files. */
    fun copyPickedInputs(context: Context, picks: List<PickedFile>): List<CachedInput> {
        val dir = processingDir(context)
        val out = mutableListOf<CachedInput>()
        picks.forEachIndexed { index, pick ->
            val uri = Uri.parse(pick.uri)
            val safeName = sanitizeFileName(pick.name)
            val target = File(dir, "in_${System.currentTimeMillis()}_${index}_$safeName")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(target).use { output -> input.copyTo(output) }
                } ?: throw IOException("Unable to open ${pick.name}")
                out += CachedInput(target, pick.name, pick.mimeType)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy picked file", e)
                throw IllegalStateException("Could not read “${pick.name}”. The file may have moved or permission was denied.")
            }
        }
        return out
    }

    /** Deletes every file under cacheDir/processing and cacheDir/ocr. Returns bytes freed. */
    fun clearProcessingCache(context: Context): Long {
        var freed = 0L
        listOf(processingDir(context), ocrDir(context)).forEach { dir ->
            dir.listFiles()?.forEach { f ->
                freed += dirSize(f)
                f.deleteRecursively()
            }
        }
        return freed
    }

    fun cacheSize(context: Context): Long =
        dirSize(processingDir(context)) + dirSize(ocrDir(context))

    private fun dirSize(file: File): Long =
        if (file.isFile) file.length() else file.listFiles()?.sumOf { dirSize(it) } ?: 0L

    /** Publishes [source] into the public Downloads/Docs folder. Returns true on success. */
    fun saveToDownloads(context: Context, source: File, displayName: String, mimeType: String): Boolean {
        val safeName = sanitizeFileName(displayName)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, safeName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Docs")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = try {
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            } catch (e: Exception) {
                Log.e(TAG, "MediaStore insert failed", e); null
            } ?: return false
            try {
                resolver.openOutputStream(uri)?.use { out ->
                    FileInputStream(source).use { it.copyTo(out) }
                } ?: return false
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                true
            } catch (e: Exception) {
                Log.e(TAG, "MediaStore write failed", e)
                resolver.delete(uri, null, null)
                false
            }
        } else {
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Docs")
            try {
                dir.mkdirs()
                source.copyTo(File(dir, safeName), overwrite = true)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Legacy Downloads copy failed", e)
                false
            }
        }
    }

    /** FileProvider URI that grants temporary read access to [file]. */
    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    private fun viewIntent(context: Context, file: File, mimeType: String): Intent {
        val uri = uriFor(context, file)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun openFile(context: Context, file: File, mimeType: String): Boolean = try {
        context.startActivity(viewIntent(context, file, mimeType)); true
    } catch (e: Exception) {
        Log.w(TAG, "No viewer for $mimeType", e); false
    }

    fun shareFile(context: Context, file: File, mimeType: String, title: String): Boolean {
        val uri = uriFor(context, file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return try {
            context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: Exception) {
            Log.w(TAG, "Share failed", e); false
        }
    }

    fun hasViewerFor(context: Context, mimeType: String): Boolean =
        Intent(Intent.ACTION_VIEW).setType(mimeType)
            .let { context.packageManager.queryIntentActivities(it, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty() }
}
