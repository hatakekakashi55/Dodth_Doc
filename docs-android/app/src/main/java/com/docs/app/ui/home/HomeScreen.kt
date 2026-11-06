package com.docs.app.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docs.app.R
import com.docs.app.data.ToolCatalog
import com.docs.app.ui.components.EmptyState

/**
 * Tools dashboard — brand header, live search, hero banner and the full
 * catalogue grouped into the four Docs categories.
 */
@Composable
fun HomeScreen(
    onToolClick: (String) -> Unit,
    isDark: Boolean,
    onToggleTheme: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) { ToolCatalog.filter(query) }
    val scheme = MaterialTheme.colorScheme

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background),
        state = rememberLazyListState(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 6.dp, bottom = 28.dp)
    ) {
        item { HomeHeader(isDark = isDark, onToggleTheme = onToggleTheme) }
        item { SearchField(query = query, onQueryChange = { query = it }) }
        if (query.isBlank()) {
            item {
                Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                    HeroBanner()
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Filled.SearchOff,
                    title = "No tools found",
                    subtitle = "Try a different keyword — for example “merge”, “compress” or “ocr”."
                )
            }
        } else {
            filtered.forEach { category ->
                item(key = "section-${category.title}") {
                    SectionHeader(title = category.title, count = category.tools.size)
                }
                val rows = category.tools.chunked(2)
                items(rows, key = { row -> "row-${row.joinToString("-") { it.id }}" }) { row ->
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { tool ->
                            ToolCard(tool = tool, modifier = Modifier.weight(1f)) { onToolClick(tool.id) }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Text(
                text = "Docs  ·  Document Utility Suite  ·  v1.0.0",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 26.dp, bottom = 8.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
private fun HomeHeader(isDark: Boolean, onToggleTheme: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 10.dp, top = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo tile — same mark as the launcher icon
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(
                    Brush.linearGradient(listOf(Color(0xFF818CF8), Color(0xFF4F46E5)))
                ),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ic_logo_doc),
                contentDescription = "Docs logo",
                modifier = Modifier.size(30.dp),
                contentScale = ContentScale.Fit
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = "Docs",
                style = MaterialTheme.typography.headlineSmall,
                color = scheme.onSurface,
                fontSize = 21.sp,
                letterSpacing = (-0.4).sp
            )
            Text(
                text = "DOCUMENT UTILITY SUITE",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                letterSpacing = 1.6.sp,
                fontSize = 8.5.sp
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = onToggleTheme,
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(scheme.surfaceVariant)
        ) {
            Icon(
                imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                contentDescription = if (isDark) "Switch to light theme" else "Switch to dark theme",
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(21.dp)
            )
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        singleLine = true,
        leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = null, tint = scheme.onSurfaceVariant)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = scheme.onSurfaceVariant)
                }
            }
        },
        placeholder = {
            Text("Search tools — try “merge”", color = scheme.onSurfaceVariant.copy(alpha = 0.8f))
        },
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = scheme.primary.copy(alpha = 0.55f),
            unfocusedBorderColor = scheme.outlineVariant,
            focusedContainerColor = scheme.surface,
            unfocusedContainerColor = scheme.surface
        )
    )
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant,
            letterSpacing = 1.7.sp
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(scheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant
            )
        }
    }
}
