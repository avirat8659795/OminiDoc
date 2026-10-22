package com.docvault.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.docvault.app.core.model.DocItem
import com.docvault.app.core.model.DocType
import com.docvault.app.core.util.describe
import com.docvault.app.ui.theme.LocalAppColors

@Composable
fun TypeTile(item: DocItem, size: Int = 44) {
    val shape = RoundedCornerShape((size * 0.26f).dp)
    Box(
        Modifier.size(size.dp).clip(shape).background(item.type.bg),
        Alignment.Center
    ) {
        if (item.type == DocType.IMAGE) {
            AsyncImage(
                model = item.uri, contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(shape)
            )
        } else {
            Icon(item.type.icon, null, tint = item.type.fg, modifier = Modifier.size((size * 0.5f).dp))
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = LocalAppColors.current.textTertiary,
        modifier = modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
fun FileRow(
    item: DocItem,
    selected: Boolean,
    favourite: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMenu: () -> Unit,
) {
    val c = LocalAppColors.current
    val bg by animateColorAsState(if (selected) c.brand.copy(alpha = 0.10f) else Color.Transparent, label = "rowbg")

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TypeTile(item)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.name,
                style = MaterialTheme.typography.bodyLarge,
                color = c.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(describe(item), style = MaterialTheme.typography.labelSmall, color = c.textTertiary, maxLines = 1)
        }
        if (favourite) {
            Icon(Icons.Rounded.Star, null, tint = c.brand, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        if (selected) {
            Box(
                Modifier.size(20.dp).clip(CircleShape).background(c.brand),
                Alignment.Center
            ) { Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(14.dp)) }
        } else {
            IconButtonTiny(onMenu)
        }
    }
}

@Composable
private fun IconButtonTiny(onClick: () -> Unit) {
    Box(Modifier.size(30.dp).clip(CircleShape).clickable(onClick = onClick), Alignment.Center) {
        Icon(
            androidx.compose.material.icons.Icons.Rounded.MoreVert,
            null,
            tint = LocalAppColors.current.textTertiary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun GradientSummaryCard(
    folders: Int,
    files: Int,
    onScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent
    ) {
        Box(
            Modifier.background(
                Brush.linearGradient(
                    listOf(Color(0xFF2C6BED), Color(0xFF7C5CFC))
                )
            ).padding(20.dp)
        ) {
            Column {
                Text("Your library", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = .8f))
                Spacer(Modifier.height(6.dp))
                Text(
                    "$folders folders · $files files",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PillButton("Scan a document", onScan)
                    Spacer(Modifier.width(10.dp))
                    Text("100% offline", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .8f))
                }
            }
        }
    }
}

@Composable
private fun PillButton(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.18f),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val c = LocalAppColors.current
    Column(
        Modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(84.dp).clip(RoundedCornerShape(26.dp)).background(c.brandGhost),
            Alignment.Center
        ) {
            Icon(
                androidx.compose.material.icons.Icons.Rounded.FolderOpen,
                null, tint = c.brand, modifier = Modifier.size(40.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
        Spacer(Modifier.height(6.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodySmall,
            color = c.textSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = c.brand,
                modifier = Modifier.clickable(onClick = onAction)
            ) {
                Text(
                    actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 11.dp)
                )
            }
        }
    }
}

@Composable
fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) c.brand else c.surfaceAlt,
        border = if (selected) null else BorderStroke(1.dp, c.border),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else c.textSecondary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

@Composable
fun GridCell(
    item: DocItem,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val c = LocalAppColors.current
    val shape = RoundedCornerShape(14.dp)
    val bg = if (selected) c.brand.copy(alpha = 0.10f) else c.surfaceAlt
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.78f)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(10.dp),
        color = bg,
        shape = shape
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                TypeTile(item, 58)
            }
            Spacer(Modifier.height(8.dp))
            Text(item.name, maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium)
            Text(if (item.isDir) "Folder" else describe(item),
                style = MaterialTheme.typography.labelSmall,
                color = c.textTertiary)
        }
    }
}