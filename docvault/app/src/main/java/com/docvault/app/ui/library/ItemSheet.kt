package com.docvault.app.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.docvault.app.core.model.DocItem
import com.docvault.app.core.util.describe
import com.docvault.app.ui.components.TypeTile
import com.docvault.app.ui.theme.LocalAppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemSheet(
    item: DocItem,
    favourite: Boolean,
    onDismiss: () -> Unit,
    onFavourite: () -> Unit,
    onRename: () -> Unit,
    onShare: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = LocalAppColors.current
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = c.surfaceAlt) {
        Column(Modifier.padding(bottom = 28.dp)) {
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                TypeTile(item, 44)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(item.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                    Text(describe(item), style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
                }
            }
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = c.border)
            SheetRow(if (favourite) "Remove from favourites" else "Add to favourites",
                if (favourite) Icons.Rounded.Star else Icons.Rounded.StarBorder, onFavourite)
            SheetRow("Rename", Icons.Rounded.DriveFileRenameOutline, onRename)
            SheetRow("Move or copy", Icons.Rounded.DriveFileMove, onMove)
            SheetRow("Share", Icons.Rounded.Share, onShare)
            HorizontalDivider(color = c.border)
            SheetRow("Delete", Icons.Rounded.DeleteOutline, onDelete, tint = c.danger)
        }
    }
}

@Composable
private fun SheetRow(label: String, icon: ImageVector, onClick: () -> Unit, tint: androidx.compose.ui.graphics.Color? = null) {
    val c = LocalAppColors.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint ?: c.textSecondary, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(18.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = tint ?: c.textPrimary)
    }
}