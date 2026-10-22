package com.docvault.app.ui.tools

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.docvault.app.ui.theme.LocalAppColors

private data class Tool(val title: String, val sub: String, val icon: ImageVector)

private val TOOLS = listOf(
    Tool("Scan to PDF", "Camera → searchable document", Icons.Rounded.DocumentScanner),
    Tool("Images to PDF", "Combine photos into one file", Icons.Rounded.Collections),
    Tool("Merge PDFs", "Join several PDFs together", Icons.Rounded.Merge),
    Tool("Split PDF", "One file per page", Icons.Rounded.CallSplit),
    Tool("Compress PDF", "Shrink for upload limits", Icons.Rounded.Compress),
    Tool("Protect PDF", "Add a password", Icons.Rounded.Lock),
    Tool("Create from text", "Type it, export a PDF", Icons.Rounded.Notes),
)

@Composable
fun ToolsScreen(onOpenViewer: (Uri, String) -> Unit) {
    val c = LocalAppColors.current
    var active by remember { mutableStateOf<Tool?>(null) }

    if (active != null) {
        ToolRunner(active!!) { active = null }
        return
    }

    LazyColumn(
        Modifier.fillMaxSize().background(c.surface),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 24.dp)
    ) {
        item {
            Text("Tools", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = c.textPrimary)
            Text("Everything runs on-device", style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
            Spacer(Modifier.height(18.dp))
        }
        items(TOOLS) { tool ->
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(c.surfaceAlt)
                    .clickable { active = tool }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(c.brandGhost),
                    Alignment.Center
                ) { Icon(tool.icon, null, tint = c.brand, modifier = Modifier.size(22.dp)) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(tool.title, style = MaterialTheme.typography.titleSmall, color = c.textPrimary)
                    Text(tool.sub, style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = c.textTertiary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}