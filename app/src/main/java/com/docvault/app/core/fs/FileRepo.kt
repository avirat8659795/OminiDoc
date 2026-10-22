package com.docvault.app.core.fs

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import com.docvault.app.core.model.DocItem
import com.docvault.app.core.model.detectType

class FileRepo(private val ctx: Context) {

    private fun tree(uri: Uri): DocumentFile? = DocumentFile.fromTreeUri(ctx, uri)
    private fun single(uri: Uri): DocumentFile? =
        DocumentFile.fromSingleUri(ctx, uri) ?: DocumentFile.fromTreeUri(ctx, uri)

    fun children(dir: Uri): List<DocItem> {
        val df = tree(dir) ?: return emptyList()
        return df.listFiles()
            .map { f ->
                val name = f.name ?: return@map null
                DocItem(
                    uri = f.uri,
                    name = name,
                    isDir = f.isDirectory,
                    size = if (f.isDirectory) 0L else f.length(),
                    modified = f.lastModified(),
                    mime = f.type,
                    type = detectType(name, f.type, f.isDirectory),
                )
            }
            .filterNotNull()
    }

    fun displayName(uri: Uri): String {
        runCatching {
            ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0 && c.moveToFirst()) return c.getString(i)
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "file"
    }

    fun sizeOf(uri: Uri): Long = single(uri)?.length() ?: 0L

    fun createFolder(parent: Uri, name: String): Uri? =
        tree(parent)?.createDirectory(name)?.uri

    fun createFile(parent: Uri, name: String, mime: String): Uri? =
        tree(parent)?.createFile(mime, name)?.uri

    fun rename(uri: Uri, newName: String): Boolean =
        single(uri)?.renameTo(newName) ?: false

    fun delete(uri: Uri): Boolean =
        runCatching { single(uri)?.delete() == true }.getOrDefault(false)

    fun uniqueName(dir: Uri, wanted: String): String {
        val existing = children(dir).map { it.name }.toHashSet()
        if (wanted !in existing) return wanted
        val base = wanted.substringBeforeLast('.')
        val ext = wanted.substringAfterLast('.', "")
        var n = 1
        while (true) {
            val candidate = if (ext.isBlank()) "$base ($n)" else "$base ($n).$ext"
            if (candidate !in existing) return candidate
            n++
        }
    }

    /** Copy (or move) [src] into [destDir]. */
    fun transfer(src: Uri, destDir: Uri, move: Boolean): Boolean {
        val name = displayName(src)
        val mime = ctx.contentResolver.getType(src) ?: "application/octet-stream"
        val dir = tree(destDir) ?: return false
        val out = dir.createFile(mime, uniqueName(destDir, name)) ?: return false
        val ok = runCatching {
            ctx.contentResolver.openInputStream(src)!!.use { input ->
                ctx.contentResolver.openOutputStream(out.uri)!!.use { output -> input.copyTo(output) }
            }
            true
        }.getOrDefault(false)
        if (ok && move) delete(src)
        return ok
    }

    /** Read a whole document into memory (text only). */
    fun readText(uri: Uri): String = runCatching {
        ctx.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
    }.getOrDefault("")

    fun writeText(uri: Uri, text: String): Boolean = runCatching {
        ctx.contentResolver.openOutputStream(uri, "wt")!!.bufferedWriter().use { it.write(text) }
        true
    }.getOrDefault(false)

    fun newReadFd(uri: Uri) = ctx.contentResolver.openFileDescriptor(uri, "r")
}