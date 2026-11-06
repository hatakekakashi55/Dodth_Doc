package com.docs.app.ui.tool

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.docs.app.core.Files
import com.docs.app.data.DocsTool
import com.docs.app.data.ToolCatalog
import com.docs.app.ui.components.FileExtBadge
import com.docs.app.ui.components.HairlineDivider
import com.docs.app.ui.theme.Success
import com.docs.app.ui.theme.Danger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The universal tool workspace. One screen drives every tool in the catalogue:
 * pick files → tweak options → process → open / share / save.
 */
@Composable
fun ToolScreen(toolId: String?, onBack: () -> Unit) {
    val tool = remember(toolId) { ToolCatalog.findById(toolId) }
    val viewModel: ToolViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // SAF pickers must be declared before any conditional branch
    val pickSingle = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && tool != null) viewModel.setPicked(listOf(uri), tool)
    }
    val pickMultiple = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty() && tool != null) viewModel.setPicked(uris, tool)
    }
    fun launchPicker() {
        val mimes = tool?.accepts?.toTypedArray() ?: arrayOf("*/*")
        if (tool?.multiple == true) pickMultiple.launch(mimes) else pickSingle.launch(mimes)
    }

    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(bottom = 36.dp)
    ) {
        // ---- top bar ----
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = tool?.name ?: "Tool",
                style = MaterialTheme.typography.titleLarge,
                color = scheme.onSurface
            )
        }

        if (tool == null) {
            Text(
                text = "This tool could not be found. It may have been removed in a newer version.",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
            return@Column
        }

        ToolHeader(tool = tool)
        OnDeviceNote()
        Spacer(Modifier.height(12.dp))
        PickerCard(tool = tool, onBrowse = ::launchPicker)

        if (state.files.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            SelectedFiles(
                files = state.files,
                onRemove = viewModel::removeFile,
                onClear = viewModel::clearFiles
            )
        }

        ToolOptions(
            tool = tool,
            state = state,
            viewModel = viewModel
        )

        Spacer(Modifier.height(18.dp))

        AnimatedContent(
            targetState = state.phase,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tool-phase"
        ) { phase ->
            when (phase) {
                WorkPhase.IDLE -> ProcessButton(tool = tool, state = state, viewModel = viewModel)
                WorkPhase.PROCESSING -> ProcessingCard(progress = state.progress, viewModel = viewModel)
                WorkPhase.SUCCESS -> SuccessCard(
                    state = state,
                    onOpen = {
                        state.output?.let { out ->
                            if (!Files.openFile(context, out.file, out.mimeType)) {
                                Toast.makeText(context, "No app found to open this file type.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onShare = {
                        state.output?.let { out ->
                            Files.shareFile(context, out.file, out.mimeType, "Share ${out.fileName}")
                        }
                    },
                    onSave = {
                        state.output?.let { out ->
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) {
                                    Files.saveToDownloads(context, out.file, out.fileName, out.mimeType)
                                }
                                Toast.makeText(
                                    context,
                                    if (ok) "Saved to Downloads/Docs" else "Could not save to Downloads.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    onReset = viewModel::reset
                )
                WorkPhase.ERROR -> ErrorCard(message = state.error, onRetry = viewModel::reset)
            }
        }
    }
}

// ------------------------------------------------------------------ sections

@Composable
private fun ToolHeader(tool: DocsTool) {
    val scheme = MaterialTheme.colorScheme
    val accent = androidx.compose.ui.graphics.Color(tool.accent)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(tool.icon, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(tool.name, style = MaterialTheme.typography.headlineSmall, color = scheme.onSurface, fontSize = 19.sp)
            Text(tool.description, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun OnDeviceNote() {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(50),
        color = scheme.surfaceVariant.copy(alpha = 0.75f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(12.dp), tint = scheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            Text(
                "On-device · files never leave this phone",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PickerCard(tool: DocsTool, onBrowse: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.5.dp, scheme.outlineVariant, RoundedCornerShape(18.dp))
            .clickable(onClick = onBrowse),
        color = scheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 26.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.FolderOpen,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(44.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = if (tool.multiple) "Tap to add files" else "Tap to browse files",
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Accepts:  ${tool.acceptLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
            tool.footnote?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.75f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun SelectedFiles(
    files: List<com.docs.app.core.PickedFile>,
    onRemove: (Int) -> Unit,
    onClear: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = scheme.surface,
        modifier = Modifier.fillMaxWidth().border(1.dp, scheme.outlineVariant, RoundedCornerShape(16.dp))
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${files.size} file${if (files.size > 1) "s" else ""} selected",
                    style = MaterialTheme.typography.titleSmall,
                    color = scheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onClear) { Text("Clear all") }
            }
            files.forEachIndexed { index, file ->
                if (index > 0) HairlineDivider(Modifier.padding(vertical = 2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FileExtBadge(file.name, tile = 42.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = scheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (file.size >= 0) Files.formatFileSize(file.size) else "Size unknown",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onRemove(index) }) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove ${file.name}", tint = scheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolOptions(tool: DocsTool, state: ToolUiState, viewModel: ToolViewModel) {
    if (!tool.needsPassword && !tool.needsPageRange && !tool.needsAngle && !tool.needsWatermark) return
    val scheme = MaterialTheme.colorScheme
    var showPassword by remember { mutableStateOf(false) }

    Spacer(Modifier.height(12.dp))
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = scheme.surface,
        modifier = Modifier.fillMaxWidth().border(1.dp, scheme.outlineVariant, RoundedCornerShape(16.dp))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "OPTIONS",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
                letterSpacing = 1.6.sp
            )

            if (tool.needsPassword) {
                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::setPassword,
                    label = { Text("Password") },
                    placeholder = { Text("Enter password") },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (showPassword) "Hide password" else "Show password"
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedFieldColors()
                )
                Text(
                    text = when (tool.id) {
                        "protect" -> "The PDF will require this password to open · 256-bit AES encryption"
                        else -> "Enter the password that currently protects this file"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }

            if (tool.needsPageRange) {
                OutlinedTextField(
                    value = state.pageRange,
                    onValueChange = viewModel::setPageRange,
                    label = { Text("Pages") },
                    placeholder = { Text(if (tool.id == "split") "all or 1,3,5-7" else "2,4-6") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedFieldColors()
                )
                Text(
                    text = when (tool.id) {
                        "split" -> "“all” exports every page · ranges like 1,3,5-7 supported"
                        else -> "These pages will be removed · ranges like 1,3,5-7 supported"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }

            if (tool.needsAngle) {
                Text("Rotation angle", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(90, 180, 270).forEach { angle ->
                        FilterChip(
                            selected = state.angle == angle,
                            onClick = { viewModel.setAngle(angle) },
                            label = { Text("$angle°") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = scheme.primaryContainer,
                                selectedLabelColor = scheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            if (tool.needsWatermark) {
                OutlinedTextField(
                    value = state.watermarkText,
                    onValueChange = viewModel::setWatermarkText,
                    label = { Text("Watermark text") },
                    placeholder = { Text("CONFIDENTIAL") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = outlinedFieldColors()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Opacity", style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
                    androidx.compose.material3.Slider(
                        value = state.watermarkOpacity,
                        onValueChange = viewModel::setWatermarkOpacity,
                        valueRange = 0.1f..1f,
                        steps = 8,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                    )
                    Text(
                        text = String.format(java.util.Locale.US, "%.1f", state.watermarkOpacity),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.width(30.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface
)

// -------------------------------------------------------------------- phases

@Composable
private fun ProcessButton(tool: DocsTool, state: ToolUiState, viewModel: ToolViewModel) {
    val ready = state.files.isNotEmpty() &&
        (!tool.needsPassword || state.password.isNotBlank()) &&
        (!tool.needsWatermark || state.watermarkText.isNotBlank())
    val scheme = MaterialTheme.colorScheme
    Button(
        onClick = { viewModel.process(tool) },
        enabled = ready,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = androidx.compose.ui.graphics.Color(0xFF6366F1),
            contentColor = androidx.compose.ui.graphics.Color.White,
            disabledContainerColor = scheme.surfaceVariant,
            disabledContentColor = scheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Icon(tool.icon, contentDescription = null, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(10.dp))
        Text("Process ${tool.name}", style = MaterialTheme.typography.labelLarge, fontSize = 13.5.sp)
    }
    if (state.files.isEmpty()) {
        Text(
            "Add files to continue",
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProcessingCard(progress: Float, viewModel: ToolViewModel) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = scheme.surface,
        modifier = Modifier.fillMaxWidth().border(1.dp, scheme.outlineVariant, RoundedCornerShape(18.dp))
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    strokeWidth = 3.5.dp,
                    modifier = Modifier.size(38.dp),
                    color = scheme.primary
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Working on it…", style = MaterialTheme.typography.titleMedium, color = scheme.onSurface)
                    Text(
                        viewModel.stageText(progress),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = scheme.primary
                )
            }
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                color = scheme.primary,
                trackColor = scheme.surfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Keep Docs open — processing runs privately on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
        }
    }
}

@Composable
private fun SuccessCard(
    state: ToolUiState,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val out = state.output
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = scheme.surface,
        modifier = Modifier.fillMaxWidth().border(1.dp, Success.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(22.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Success, modifier = Modifier.size(52.dp))
            Spacer(Modifier.height(8.dp))
            Text("Processing complete", style = MaterialTheme.typography.headlineSmall, color = scheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(
                text = out?.fileName.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOfNotNull(
                    out?.let { Files.formatFileSize(it.file.length()) },
                    out?.summary
                ).joinToString("  ·  "),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpen, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open")
                }
                OutlinedButton(onClick = onShare, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Share")
                }
                Button(
                    onClick = onSave,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFF6366F1),
                        contentColor = androidx.compose.ui.graphics.Color.White
                    )
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save")
                }
            }
            TextButton(onClick = onReset, modifier = Modifier.padding(top = 8.dp)) {
                Text("Process another file")
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String?, onRetry: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = scheme.surface,
        modifier = Modifier.fillMaxWidth().border(1.dp, Danger.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(22.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = Danger, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(8.dp))
            Text("Processing failed", style = MaterialTheme.typography.headlineSmall, color = scheme.onSurface)
            Text(
                text = message ?: "Something went wrong.",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
            )
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = scheme.surfaceVariant, contentColor = scheme.onSurface)
            ) {
                Text("Try again")
            }
        }
    }
}
