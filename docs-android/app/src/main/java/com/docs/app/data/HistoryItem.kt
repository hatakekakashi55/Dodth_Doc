package com.docs.app.data

/** One processed output, persisted in the app-private history store. */
data class HistoryItem(
    val id: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val toolId: String,
    val toolName: String,
    val addedAt: Long
)
