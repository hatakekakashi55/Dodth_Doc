package com.docs.app.ui.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.ShieldCheck
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.docs.app.R
import com.docs.app.core.Files
import com.docs.app.data.ThemeRepository
import com.docs.app.ui.components.HairlineDivider

/**
 * Settings — appearance (system / light / dark), storage management,
 * privacy posture and licensing. No tracking, no accounts, no ads.
 */
@Composable
fun SettingsScreen() {
    val viewModel: SettingsViewModel = viewModel()
    val cacheBytes by viewModel.cacheBytes.collectAsStateWithLifecycle()
    val historyBytes by viewModel.historyBytes.collectAsStateWithLifecycle()
    val mode by ThemeRepository.mode.collectAsStateWithLifecycle()
    var showLicenses by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(bottom = 40.dp)
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 18.dp, bottom = 14.dp)
        )

        // ---------- identity card ----------
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF818CF8), Color(0xFF4F46E5)))),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(R.drawable.ic_logo_doc),
                        contentDescription = "Docs logo",
                        modifier = Modifier.size(36.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Docs", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "Version 1.0.0 (build 1)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Document Utility Suite",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.2.sp
                    )
                }
            }
        }

        SectionLabel("APPEARANCE")
        SettingsCard {
            ThemeRow(
                icon = Icons.Filled.BrightnessAuto,
                title = "System default",
                subtitle = "Follow the device appearance",
                selected = mode == ThemeRepository.MODE_SYSTEM,
                onClick = { ThemeRepository.setMode(ThemeRepository.MODE_SYSTEM) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ThemeRow(
                icon = Icons.Filled.LightMode,
                title = "Light",
                subtitle = "Paper surfaces, high legibility",
                selected = mode == ThemeRepository.MODE_LIGHT,
                onClick = { ThemeRepository.setMode(ThemeRepository.MODE_LIGHT) }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ThemeRow(
                icon = Icons.Filled.DarkMode,
                title = "Dark",
                subtitle = "Enterprise slate — easier at night",
                selected = mode == ThemeRepository.MODE_DARK,
                onClick = { ThemeRepository.setMode(ThemeRepository.MODE_DARK) }
            )
        }

        SectionLabel("STORAGE")
        SettingsCard {
            ActionRow(
                icon = Icons.Filled.DeleteSweep,
                title = "Output cache",
                subtitle = "${Files.formatFileSize(cacheBytes)} of staged files",
                actionLabel = "Clear",
                onAction = viewModel::clearCache
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ActionRow(
                icon = Icons.Filled.History,
                title = "Saved outputs",
                subtitle = "${Files.formatFileSize(historyBytes)} kept in Recents",
                actionLabel = "Clear",
                onAction = viewModel::clearHistory
            )
        }

        SectionLabel("PRIVACY & ENGINES")
        SettingsCard {
            InfoRow(
                icon = Icons.Filled.ShieldCheck,
                title = "On-device processing",
                subtitle = "Every conversion runs locally. No file is ever uploaded, logged or shared."
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            InfoRow(
                icon = Icons.Filled.Memory,
                title = "Processing engines",
                subtitle = "Apache PDFBox · Android PdfDocument & PdfRenderer · ML Kit OCR"
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ActionRow(
                icon = Icons.Filled.Description,
                title = "Open-source licenses",
                subtitle = "PDFBox (Apache-2.0) · ML Kit Terms · AOSP",
                actionLabel = "View",
                onAction = { showLicenses = true }
            )
        }

        Text(
            text = "© 2026 Docs — Document Utility Suite\nNothing leaves your device. Not now, not ever.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp)
        )
    }

    if (showLicenses) {
        LicensesDialog(onDismiss = { showLicenses = false })
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.7.sp,
        modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
    ) {
        Column { content() }
    }
}

@Composable
private fun ThemeRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        RadioButton(selected = selected, onClick = onClick)
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun ActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onAction) {
            Text(actionLabel, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun LicensesDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Open-source licenses") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LicenseEntry("Apache PDFBox for Android", "Apache License 2.0 — © The Apache Software Foundation. Used for merge, split, rotate, compress, page numbers, encryption, watermarking and text extraction.")
                LicenseEntry("Google ML Kit — Text Recognition", "Google ML Kit Terms of Service. The OCR model is downloaded once via Play services and runs fully on-device afterwards.")
                LicenseEntry("Android framework", "PdfDocument, PdfRenderer and BitmapFactory are part of the Android Open Source Project (Apache-2.0).")
                LicenseEntry("Docs brand assets", "The Docs logo and icon artwork are original works created for this application.")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun LicenseEntry(name: String, body: String) {
    Column {
        Text(name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
    }
}
