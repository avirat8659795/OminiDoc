package com.docvault.app.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun humanSize(bytes: Long): String {
    if (bytes <= 0L) return "—"
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var v = bytes.toDouble() / 1024
    var i = 0
    while (v >= 1024 && i < units.lastIndex) { v /= 1024; i++ }
    return String.format(Locale.US, if (v >= 100) "%.0f %s" else "%.1f %s", v, units[i])
}

fun humanTime(millis: Long): String {
    if (millis <= 0L) return "—"
    val diff = System.currentTimeMillis() - millis
    val mins = diff / 60_000
    return when {
        mins < 1 -> "just now"
        mins < 60 -> "${mins}m ago"
        mins < 1_440 -> "${mins / 60}h ago"
        mins < 10_080 -> "${mins / 1_440}d ago"
        else -> SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(millis))
    }
}

fun describe(item: com.docvault.app.core.model.DocItem): String =
    if (item.isDir) "Folder" else "${item.type.label} · ${humanSize(item.size)} · ${humanTime(item.modified)}"