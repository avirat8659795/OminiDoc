package com.docvault.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.docvault.app.core.prefs.Prefs
import com.docvault.app.ui.theme.LocalAppColors
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    val c = LocalAppColors.current
    val prefs = remember { Prefs(ctx) }
    val scope = rememberCoroutineScope()
    val theme by prefs.theme.collectAsState(initial = "system")

    Column(
        Modifier.fillMaxSize().background(c.surface).padding(20.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = c.textPrimary)
        Spacer(Modifier.height(20.dp))

        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = c.surfaceAlt)) {
            Column(Modifier.padding(6.dp)) {
                Text(
                    "APPEARANCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textTertiary,
                    modifier = Modifier.padding(start = 14.dp, top = 12.dp, bottom = 6.dp)
                )
                listOf("system" to "Follow system", "light" to "Light", "dark" to "Dark").forEach { (key, label) ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .clickable { scope.launch { prefs.setTheme(key) } }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = theme == key, onClick = { scope.launch { prefs.setTheme(key) } })
                        Spacer(Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge, color = c.textPrimary)
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = c.surfaceAlt)) {
            Column(Modifier.padding(18.dp)) {
                Text("ABOUT", style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
                Spacer(Modifier.height(8.dp))
                Text("DocVault 1.0.0", style = MaterialTheme.typography.titleSmall, color = c.textPrimary)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Opens every document type and edits PDFs — entirely offline. " +
                        "No INTERNET permission, no account, no ads, no watermark.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary
                )
            }
        }
    }
}