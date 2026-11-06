package com.docs.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docs.app.data.DocsTool

/**
 * Premium tool card: hairline top accent stripe, tinted icon tile,
 * name and two-line description. Tapping opens the tool workspace.
 */
@Composable
fun ToolCard(
    tool: DocsTool,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val accent = Color(tool.accent)
    val scheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surface)
            .border(1.dp, scheme.outlineVariant, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(bounded = true),
                onClick = onClick
            )
            .drawBehind {
                // 3dp accent stripe across the top edge
                drawRect(
                    color = accent.copy(alpha = 0.9f),
                    size = androidx.compose.ui.geometry.Size(size.width, 3.dp.toPx())
                )
            }
            .padding(top = 3.dp)
            .height(132.dp)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.14f)),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(23.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = tool.name,
            style = MaterialTheme.typography.titleMedium,
            color = scheme.onSurface,
            maxLines = 1
        )
        Text(
            text = tool.description,
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            maxLines = 2,
            lineHeight = 14.5.sp
        )
    }
}
