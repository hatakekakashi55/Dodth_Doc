package com.docs.app.data

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * App-private store of recently produced outputs.
 *
 * Successful results are copied into filesDir/history/files and indexed in
 * index.json (Org-JSON, no extra dependencies). The history survives cache
 * cleanups and is included in Auto Backup.
 */
class HistoryRepository(context: Context) {

    private val rootDir = File(context.filesDir, "history")
    private val filesDir = File(rootDir, "files")
    private val indexFile = File(rootDir, "index.json")

    init {
        filesDir.mkdirs()
    }

    fun list(): List<HistoryItem> =
        loadAll().sortedByDescending { it.addedAt }

    fun find(id: String): HistoryItem? = loadAll().firstOrNull { it.id == id }

    fun fileFor(item: HistoryItem): File =
        File(filesDir, "${item.id}_${item.fileName}")

    fun totalBytes(): Long = loadAll().sumOf { it.sizeBytes }

    /** Copies [source] into history storage and records the item. */
    fun add(source: File, fileName: String, mimeType: String, toolId: String, toolName: String): HistoryItem {
        val item = HistoryItem(
            id = UUID.randomUUID().toString().substringBefore('-'),
            fileName = fileName,
            mimeType = mimeType,
            sizeBytes = source.length(),
            toolId = toolId,
            toolName = toolName,
            addedAt = System.currentTimeMillis()
        )
        source.copyTo(fileFor(item), overwrite = true)
        val all = loadAll().toMutableList()
        all.add(item)
        // Keep the store bounded — newest 50 outputs are retained.
        val sorted = all.sortedByDescending { it.addedAt }
        sorted.drop(50).forEach { deletePhysical(it) }
        saveAll(sorted.take(50))
        return item
    }

    fun delete(id: String) {
        val all = loadAll()
        all.firstOrNull { it.id == id }?.let { deletePhysical(it) }
        saveAll(all.filterNot { it.id == id })
    }

    fun clearAll() {
        loadAll().forEach { deletePhysical(it) }
        saveAll(emptyList())
    }

    private fun deletePhysical(item: HistoryItem) {
        try {
            fileFor(item).delete()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete history file for ${item.fileName}", e)
        }
    }

    private fun loadAll(): List<HistoryItem> {
        if (!indexFile.exists()) return emptyList()
        return try {
            val arr = JSONArray(indexFile.readText())
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        HistoryItem(
                            id = o.getString("id"),
                            fileName = o.getString("fileName"),
                            mimeType = o.optString("mimeType", "application/octet-stream"),
                            sizeBytes = o.optLong("sizeBytes", 0L),
                            toolId = o.optString("toolId", ""),
                            toolName = o.optString("toolName", "Docs"),
                            addedAt = o.optLong("addedAt", 0L)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "History index corrupted, resetting", e)
            emptyList()
        }
    }

    private fun saveAll(items: List<HistoryItem>) {
        try {
            val arr = JSONArray()
            items.forEach { item ->
                arr.put(
                    JSONObject().apply {
                        put("id", item.id)
                        put("fileName", item.fileName)
                        put("mimeType", item.mimeType)
                        put("sizeBytes", item.sizeBytes)
                        put("toolId", item.toolId)
                        put("toolName", item.toolName)
                        put("addedAt", item.addedAt)
                    }
                )
            }
            indexFile.writeText(arr.toString(2))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist history index", e)
        }
    }

    companion object {
        private const val TAG = "HistoryRepository"
    }
}
