package com.docs.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * File-manager style extension badge — tinted tile with the uppercased
 * extension, e.g. PDF, DOCX, XLSX. Professional and iconographic.
 */
@Composable
fun FileExtBadge(
    fileName: String,
    modifier: Modifier = Modifier,
    tile: Dp = 46.dp
) {
    val raw = fileName.substringAfterLast('.', "")
    val ext = raw.uppercase().take(4).ifBlank { "FILE" }
    val color = when (raw.lowercase()) {
        "pdf" -> Color(0xFFDC2626)
        "doc", "docx" -> Color(0xFF2563EB)
        "xls", "xlsx" -> Color(0xFF059669)
        "ppt", "pptx" -> Color(0xFFD97706)
        "jpg", "jpeg", "png", "webp", "bmp" -> Color(0xFF7C3AED)
        "html", "htm" -> Color(0xFF6366F1)
        "zip" -> Color(0xFFF59E0B)
        else -> Color(0xFF4B5563)
    }
    Box(
        modifier = modifier
            .width(tile)
            .height(tile)
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(color)
                .width(tile - 12.dp)
                .height(tile - 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = ext,
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
    }
}

/** A hairline divider row used across cards and sections. */
@Composable
fun HairlineDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}
