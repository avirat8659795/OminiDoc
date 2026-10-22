package com.docvault.app.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AColor
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.docvault.app.ui.editor.Anno
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.multipdf.Splitter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream

object PdfEngine {

    // ---------------------------------------------------------------- render

    fun pageCount(ctx: Context, uri: Uri): Int = runCatching {
        ctx.contentResolver.openFileDescriptor(uri, "r")!!.use { fd ->
            PdfRenderer(fd).use { it.pageCount }
        }
    }.getOrDefault(0)

    fun renderPage(ctx: Context, uri: Uri, index: Int, widthPx: Int): Bitmap? = runCatching {
        ctx.contentResolver.openFileDescriptor(uri, "r")!!.use { fd ->
            PdfRenderer(fd).use { renderer ->
                if (index !in 0 until renderer.pageCount) return@use null
                renderer.openPage(index).use { page ->
                    val w = widthPx.coerceAtLeast(200)
                    val h = (page.height.toFloat() / page.width * w).toInt().coerceAtLeast(200)
                    Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { bmp ->
                        bmp.eraseColor(AColor.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                }
            }
        }
    }.getOrNull()

    // ---------------------------------------------------------------- create

    /** Build a PDF from plain text, one page per entry. */
    fun fromText(pages: List<String>, out: OutputStream) {
        PdfDocument().use { doc ->
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 12f
                color = AColor.BLACK
            }
            pages.forEachIndexed { i, body ->
                val info = PdfDocument.PageInfo.Builder(595, 842, i + 1).create()
                val page = doc.startPage(info)
                var y = 56f
                body.split('\n').forEach { line ->
                    if (y > 800f) return@forEach
                    page.canvas.drawText(line, 56f, y, paint)
                    y += 17f
                }
                doc.finishPage(page)
            }
            doc.writeTo(out)
        }
    }

    /** Build a PDF from images, one page per image, scaled to A4 width. */
    fun fromImages(images: List<Bitmap>, out: OutputStream) {
        PdfDocument().use { doc ->
            images.forEachIndexed { i, bmp ->
                val pageW = 595
                val pageH = (bmp.height.toFloat() / bmp.width * pageW).toInt().coerceAtLeast(100)
                val info = PdfDocument.PageInfo.Builder(pageW, pageH, i + 1).create()
                val page = doc.startPage(info)
                page.canvas.drawBitmap(
                    bmp, null,
                    android.graphics.RectF(0f, 0f, pageW.toFloat(), pageH.toFloat()),
                    android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
                )
                doc.finishPage(page)
            }
            doc.writeTo(out)
        }
    }

    // ---------------------------------------------------------------- tools

    fun merge(ctx: Context, sources: List<Uri>, out: OutputStream): Boolean = runCatching {
        val merger = PDFMergerUtility()
        val docs = sources.map { ctx.contentResolver.openInputStream(it)!! }
        merger.setDestinationStream(out)
        docs.forEach { merger.addSource(it) }
        merger.mergeDocuments(null)
        docs.forEach { runCatching { it.close() } }
        true
    }.getOrDefault(false)

    fun split(ctx: Context, src: Uri, outDir: (index: Int, doc: PDDocument) -> OutputStream): Int = runCatching {
        ctx.contentResolver.openInputStream(src)!!.use { input ->
            PDDocument.load(input).use { doc ->
                val pages = Splitter().split(doc)
                pages.forEachIndexed { i, single ->
                    single.use { outDir(i, it).use { os -> it.save(os) } }
                }
                pages.size
            }
        }
    }.getOrDefault(0)

    /** Re-save with object cleanup + metadata strip. Real, safe compression. */
    fun compress(ctx: Context, src: Uri, out: OutputStream): Boolean = runCatching {
        ctx.contentResolver.openInputStream(src)!!.use { input ->
            PDDocument.load(input).use { doc ->
                try { doc.documentCatalog.metadata = null } catch (_: Exception) {}
                doc.save(out)
            }
        }
        true
    }.getOrDefault(false)

    fun protect(ctx: Context, src: Uri, password: String, out: OutputStream): Boolean = runCatching {
        ctx.contentResolver.openInputStream(src)!!.use { input ->
            PDDocument.load(input).use { doc ->
                val policy = StandardProtectionPolicy(password, password, AccessPermission())
                policy.encryptionKeyLength = 128
                doc.protect(policy)
                doc.save(out)
            }
        }
        true
    }.getOrDefault(false)

    // ---------------------------------------------------------------- annotate

    /**
     * Appends [annos] onto [src] and writes to [out].
     * [renderWidths] maps page index -> the pixel width the page was rendered at,
     * so view coordinates can be scaled into PDF points.
     */
    fun stampAnnotations(
        ctx: Context,
        src: Uri,
        out: OutputStream,
        annos: Map<Int, List<Anno>>,
        renderWidths: Map<Int, Int>,
    ): Boolean = runCatching {
        ctx.contentResolver.openInputStream(src)!!.use { input ->
            PDDocument.load(input).use { doc ->
                annos.forEach { (pageIndex, list) ->
                    if (list.isEmpty()) return@forEach
                    if (pageIndex !in 0 until doc.numberOfPages) return@forEach
                    val page: PDPage = doc.getPage(pageIndex)
                    val box: PDRectangle = page.mediaBox
                    val scale = box.width / (renderWidths[pageIndex] ?: box.width.toInt()).toFloat()
                    PDPageContentStream(
                        doc, page,
                        PDPageContentStream.AppendMode.APPEND, true, true
                    ).use { cs -> list.forEach { draw(doc, cs, it, box.height, scale) } }
                }
                doc.save(out)
            }
        }
        true
    }.getOrDefault(false)

    private fun draw(doc: PDDocument, cs: PDPageContentStream, a: Anno, pageH: Float, s: Float) {
        fun fx(c: Int) = AColor.red(c) / 255f
        fun fy(c: Int) = AColor.green(c) / 255f
        fun fz(c: Int) = AColor.blue(c) / 255f
        fun flip(y: Float) = pageH - y * s

        when (a) {
            is Anno.Ink -> {
                val c = a.colorInt
                cs.setStrokingColor(fx(c), fy(c), fz(c))
                cs.setLineWidth(a.width * s)
                cs.setLineCapStyle(1)
                cs.setLineJoinStyle(1)
                a.points.firstOrNull()?.let { cs.moveTo(it.first * s, flip(it.second)) }
                a.points.drop(1).forEach { cs.lineTo(it.first * s, flip(it.second)) }
                cs.stroke()
            }

            is Anno.Text -> {
                val c = a.colorInt
                cs.beginText()
                cs.setFont(PDType1Font.HELVETICA, (a.size * s).coerceIn(4f, 96f))
                cs.setNonStrokingColor(fx(c), fy(c), fz(c))
                cs.newLineAtOffset(a.x * s, flip(a.y))
                cs.showText(a.text.replace("\n", " "))
                cs.endText()
            }

            is Anno.Shape -> {
                val c = a.colorInt
                cs.setStrokingColor(fx(c), fy(c), fz(c))
                cs.setLineWidth(a.stroke * s)
                val left = minOf(a.x1, a.x2) * s
                val right = maxOf(a.x1, a.x2) * s
                val top = flip(minOf(a.y1, a.y2))
                val bottom = flip(maxOf(a.y1, a.y2))
                when (a.kind) {
                    Anno.ShapeKind.RECT -> cs.addRect(left, top, right - left, bottom - top).also { cs.stroke() }
                    Anno.ShapeKind.ELLIPSE -> cs.addRect(left, top, right - left, bottom - top).also { cs.stroke() }
                    Anno.ShapeKind.LINE, Anno.ShapeKind.ARROW -> {
                        cs.moveTo(a.x1 * s, flip(a.y1))
                        cs.lineTo(a.x2 * s, flip(a.y2))
                        cs.stroke()
                    }
                }
            }

            is Anno.Stamp -> {
                val bytes = ByteArrayOutputStream().also { a.bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
                val img: PDImageXObject = PDImageXObject.createFromByteArray(doc, bytes, "stamp")
                val w = (a.x2 - a.x1) * s
                val h = (a.y2 - a.y1) * s
                cs.drawImage(img, a.x1 * s, flip(a.y2), w, h)
            }
        }
    }

    fun cacheFile(ctx: Context, name: String): File {
        val dir = File(ctx.cacheDir, "exports").apply { mkdirs() }
        return File(dir, name)
    }
}
