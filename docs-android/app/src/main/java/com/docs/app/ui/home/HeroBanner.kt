package com.docs.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docs.app.data.ToolCatalog

/**
 * Hero banner — the brand moment of the app. Deep indigo gradient with a
 * soft document motif, the value proposition and three trust signals.
 */
@Composable
fun HeroBanner(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF4338CA), Color(0xFF6366F1), Color(0xFF818CF8)),
                    start = Offset(0f, 0f),
                    end = Offset(900f, 520f)
                )
            )
    ) {
        // Decorative oversized glyph, bleeding off the right edge
        Icon(
            imageVector = Icons.Filled.PictureAsPdf,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.16f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 6.dp)
                .size(104.dp)
        )
        Column(modifier = Modifier.padding(20.dp)) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.16f)
            ) {
                Text(
                    text = "100% ON-DEVICE",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 9.sp,
                    letterSpacing = 1.8.sp
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Every document, handled.",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 23.sp,
                letterSpacing = (-0.4).sp,
                lineHeight = 28.sp
            )
            Text(
                text = "Convert, merge, compress and protect your files — privately, right on this device.",
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 6.dp, end = 64.dp)
            )
            Spacer(Modifier.height(14.dp))
            Row {
                StatChip(Icons.Filled.Shield, "Private by design")
                Spacer(Modifier.size(8.dp))
                StatChip(Icons.Filled.Bolt, "${ToolCatalog.toolCount} pro tools")
            }
        }
    }
}

@Composable
private fun StatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.Black.copy(alpha = 0.18f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.95f),
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.size(5.dp))
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
