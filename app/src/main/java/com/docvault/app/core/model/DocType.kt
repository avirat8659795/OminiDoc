package com.docvault.app.core.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.docvault.app.ui.theme.Tints

enum class DocType(
    val label: String,
    val icon: ImageVector,
    val fg: Color,
    val bg: Color,
) {
    FOLDER("Folder", Icons.Rounded.Folder, Tints.folder.first, Tints.folder.second),
    PDF("PDF", Icons.Rounded.PictureAsPdf, Tints.pdf.first, Tints.pdf.second),
    WORD("Document", Icons.Rounded.Description, Tints.word.first, Tints.word.second),
    SHEET("Spreadsheet", Icons.Rounded.TableChart, Tints.sheet.first, Tints.sheet.second),
    SLIDES("Presentation", Icons.Rounded.Slideshow, Tints.slides.first, Tints.slides.second),
    IMAGE("Image", Icons.Rounded.Image, Tints.image.first, Tints.image.second),
    TEXT("Text", Icons.Rounded.Article, Tints.text.first, Tints.text.second),
    EBOOK("Ebook", Icons.Rounded.MenuBook, Tints.ebook.first, Tints.ebook.second),
    ARCHIVE("Archive", Icons.Rounded.FolderZip, Tints.archive.first, Tints.archive.second),
    AUDIO("Audio", Icons.Rounded.Audiotrack, Tints.audio.first, Tints.audio.second),
    VIDEO("Video", Icons.Rounded.Movie, Tints.video.first, Tints.video.second),
    UNKNOWN("File", Icons.Rounded.InsertDriveFile, Tints.unknown.first, Tints.unknown.second);

    val isPreviewable: Boolean get() = this == PDF || this == IMAGE || this == TEXT
}

private val BY_EXT: Map<String, DocType> = buildMap {
    listOf("pdf").forEach { put(it, DocType.PDF) }
    listOf("doc", "docx", "odt", "rtf", "pages").forEach { put(it, DocType.WORD) }
    listOf("xls", "xlsx", "ods", "csv", "tsv").forEach { put(it, DocType.SHEET) }
    listOf("ppt", "pptx", "odp", "key").forEach { put(it, DocType.SLIDES) }
    listOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "avif", "svg").forEach { put(it, DocType.IMAGE) }
    listOf("txt", "md", "markdown", "json", "xml", "yaml", "yml", "log", "ini", "conf",
        "kt", "java", "py", "js", "ts", "c", "cpp", "h", "cs", "go", "rs", "sh", "sql",
        "gradle", "properties", "toml", "html", "htm", "css").forEach { put(it, DocType.TEXT) }
    listOf("epub", "mobi", "azw", "azw3", "fb2", "djvu").forEach { put(it, DocType.EBOOK) }
    listOf("zip", "rar", "7z", "tar", "gz", "bz2", "cbz", "cbr").forEach { put(it, DocType.ARCHIVE) }
    listOf("mp3", "wav", "m4a", "aac", "ogg", "flac", "opus").forEach { put(it, DocType.AUDIO) }
    listOf("mp4", "mkv", "webm", "mov", "avi", "3gp").forEach { put(it, DocType.VIDEO) }
}

fun detectType(name: String, mime: String?, isDir: Boolean): DocType {
    if (isDir) return DocType.FOLDER
    BY_EXT[name.substringAfterLast('.', "").lowercase()]?.let { return it }
    return when {
        mime == null -> DocType.UNKNOWN
        mime.startsWith("image/") -> DocType.IMAGE
        mime.startsWith("text/") -> DocType.TEXT
        mime.startsWith("audio/") -> DocType.AUDIO
        mime.startsWith("video/") -> DocType.VIDEO
        mime.contains("pdf") -> DocType.PDF
        mime.contains("word") -> DocType.WORD
        mime.contains("sheet") || mime.contains("excel") -> DocType.SHEET
        mime.contains("presentation") || mime.contains("powerpoint") -> DocType.SLIDES
        mime.contains("zip") || mime.contains("compressed") -> DocType.ARCHIVE
        else -> DocType.UNKNOWN
    }
}

enum class SortKey(val label: String) {
    NAME("Name"), MODIFIED("Last modified"), SIZE("Size"), TYPE("Type")
}
