package com.docs.app.ui.history

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.docs.app.core.Files
import com.docs.app.data.HistoryItem
import com.docs.app.ui.components.EmptyState
import com.docs.app.ui.components.FileExtBadge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Recent outputs — everything the user produced, stored privately on-device,
 * ready to open, share, save to Downloads or delete.
 */
@Composable
fun HistoryScreen() {
    val viewModel: HistoryViewModel = viewModel()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val totalBytes by viewModel.totalBytes.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmClear by androidx.compose.runtime.remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // ---- top bar ----
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Recent", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    if (items.isEmpty()) "Outputs you save will appear here"
                    else "${items.size} file${if (items.size > 1) "s" else ""} · ${Files.formatFileSize(totalBytes)} stored on this device",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (items.isNotEmpty()) {
                IconButton(onClick = { confirmClear = true }) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear all", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (items.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.History,
                title = "Nothing here yet",
                subtitle = "Processed outputs are kept privately on this device — ready to open or share again."
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(items, key = { it.id }) { item ->
                HistoryRow(
                    item = item,
                    onShare = {
                        val file = viewModel.fileFor(item)
                        if (file.exists()) {
                            Files.shareFile(context, file, item.mimeType, "Share ${item.fileName}")
                        } else {
                            Toast.makeText(context, "This file no longer exists.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onOpen = {
                        val file = viewModel.fileFor(item)
                        if (file.exists()) {
                            if (!Files.openFile(context, file, item.mimeType)) {
                                Toast.makeText(context, "No app found to open this file type.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "This file no longer exists.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onSave = {
                        val file = viewModel.fileFor(item)
                        if (!file.exists()) {
                            Toast.makeText(context, "This file no longer exists.", Toast.LENGTH_SHORT).show()
                        } else {
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) {
                                    Files.saveToDownloads(context, file, item.fileName, item.mimeType)
                                }
                                Toast.makeText(
                                    context,
                                    if (ok) "Saved to Downloads/Docs" else "Could not save to Downloads.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    onDelete = { viewModel.delete(item) }
                )
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear recent files?") },
            text = { Text("This deletes the saved copies kept inside Docs. Your original files are not affected.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearAll(); confirmClear = false }) {
                    Text("Clear all", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Keep files") }
            }
        )
    }
}

@Composable
private fun HistoryRow(
    item: HistoryItem,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = scheme.surface,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FileExtBadge(item.fileName, tile = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${item.toolName} · ${Files.formatFileSize(item.sizeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    text = formatTimestamp(item.addedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = onOpen) {
                Icon(Icons.Filled.OpenInNew, contentDescription = "Open", tint = scheme.onSurfaceVariant)
            }
            IconButton(onClick = onSave) {
                Icon(Icons.Filled.Download, contentDescription = "Save to Downloads", tint = scheme.onSurfaceVariant)
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Filled.Share, contentDescription = "Share", tint = scheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = scheme.onSurfaceVariant.copy(alpha = 0.75f))
            }
        }
    }
}

private fun formatTimestamp(millis: Long): String {
    if (millis <= 0) return ""
    val now = System.currentTimeMillis()
    val twoDays = 2L * 24 * 60 * 60 * 1000
    val pattern = if (now - millis < twoDays) "MMM d · h:mm a" else "MMM d, yyyy · h:mm a"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
}
