package com.docvault.app.ui.theme

import androidx.compose.ui.graphics.Color

/** Per-type foreground + container tint. Kept here so core stays compose-free-ish. */
object Tints {
    val pdf = Rose to Color(0xFFFCE8E6)
    val word = Color(0xFF1967D2) to Color(0xFFE8F0FE)
    val sheet = Color(0xFF188038) to Color(0xFFE6F4EA)
    val slides = Color(0xFFE37400) to Color(0xFFFDF7E0)
    val image = Color(0xFFB8860B) to Color(0xFFFEF7E0)
    val text = Color(0xFF5F6368) to Color(0xFFF1F3F4)
    val ebook = Color(0xFF7B1FA2) to Color(0xFFF3E5F5)
    val archive = Color(0xFF795548) to Color(0xFFEFEBE9)
    val audio = Color(0xFF00796B) to Color(0xFFE0F2F1)
    val video = Color(0xFFC2185B) to Color(0xFFFCE4EC)
    val folder = Color(0xFF2C6BED) to Color(0xFFE8F0FE)
    val unknown = Color(0xFF5F6368) to Color(0xFFF1F3F4)
}