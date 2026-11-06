package com.docs.app.data

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Declarative description of a single document tool.
 * Mirrors the catalogue the Docs web suite ships, ported 1:1 for Android.
 */
data class DocsTool(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    /** Accent color used for the tool's icon chip and card stripe (0xFFRRGGBB). */
    val accent: Long,
    /** MIME types passed to the system file picker. */
    val accepts: List<String>,
    /** Human-readable accept hint shown in the picker card. */
    val acceptLabel: String,
    val multiple: Boolean,
    val needsPassword: Boolean = false,
    val needsPageRange: Boolean = false,
    val needsAngle: Boolean = false,
    val needsWatermark: Boolean = false,
    /** Extra caption rendered under the picker (e.g. "2 or more PDFs required"). */
    val footnote: String? = null,
    /** Space-separated keywords used by the search box. */
    val keywords: String = ""
)

data class ToolCategory(
    val title: String,
    val tools: List<DocsTool>
)
