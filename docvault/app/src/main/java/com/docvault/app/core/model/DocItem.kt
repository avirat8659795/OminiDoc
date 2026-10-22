package com.docvault.app.core.model

import android.net.Uri

data class DocItem(
    val uri: Uri,
    val name: String,
    val isDir: Boolean,
    val size: Long,
    val modified: Long,
    val mime: String?,
    val type: DocType,
) {
    val ext: String get() = name.substringAfterLast('.', "").lowercase()
}