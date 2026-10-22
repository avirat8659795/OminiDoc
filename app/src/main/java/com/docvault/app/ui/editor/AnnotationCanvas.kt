package com.docvault.app.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs

@Composable
fun AnnotationCanvas(
    pageBitmap: ImageBitmap?,
    annotations: List<Anno>,
    draft: Anno?,
    tool: Tool,
    color: Color,
    stroke: Float,
    shapeKind: Anno.ShapeKind,
    onCommit: (Anno) -> Unit,
    onDraft: (Anno?) -> Unit,
    onTap: (Offset, IntSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var points = remember { mutableStateListOf<Pair<Float, Float>>() }
    var origin by remember { mutableStateOf<Offset?>(null) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { size = it }
            .pointerInput(tool, color, stroke, shapeKind) {
                detectDragGestures(
                    onDragStart = { p ->
                        points = mutableStateListOf(p.x to p.y)
                        origin = p
                        when (tool) {
                            Tool.PEN, Tool.HIGHLIGHT ->
                                onDraft(Anno.Ink(listOf(p.x to p.y), color, stroke, tool == Tool.HIGHLIGHT))
                            Tool.SHAPE ->
                                onDraft(Anno.Shape(shapeKind, p.x, p.y, p.x, p.y, color, stroke))
                            else -> onDraft(null)
                        }
                    },
                    onDrag = { change, _ ->
                        val p = change.position
                        when (tool) {
                            Tool.PEN, Tool.HIGHLIGHT -> {
                                points.add(p.x to p.y)
                                onDraft(Anno.Ink(points.toList(), color, stroke, tool == Tool.HIGHLIGHT))
                            }
                            Tool.SHAPE -> origin?.let { s ->
                                onDraft(Anno.Shape(shapeKind, s.x, s.y, p.x, p.y, color, stroke))
                            }
                            else -> Unit
                        }
                    },
                    onDragEnd = {
                        when (tool) {
                            Tool.PEN, Tool.HIGHLIGHT ->
                                if (points.size > 1) onCommit(Anno.Ink(points.toList(), color, stroke, tool == Tool.HIGHLIGHT))
                            Tool.SHAPE -> origin?.let { s ->
                                onCommit(Anno.Shape(shapeKind, s.x, s.y, s.x + 2f, s.y + 2f, color, stroke))
                            }
                            else -> Unit
                        }
                        onDraft(null)
                        points = mutableStateListOf()
                        origin = null
                    }
                )
            }
            .pointerInput(tool) { detectTapGestures { p -> onTap(p, size) } }
    ) {
        pageBitmap?.let { bmp ->
            drawImage(
                image = bmp,
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.coerceAtLeast(1), size.height.coerceAtLeast(1))
            )
        }
        annotations.forEach { drawAnno(it) }
        draft?.let { drawAnno(it) }
    }
}

private fun DrawScope.drawAnno(a: Anno) {
    when (a) {
        is Anno.Ink -> {
            val path = Path().apply {
                a.points.firstOrNull()?.let { moveTo(it.first, it.second) }
                a.points.drop(1).forEach { lineTo(it.first, it.second) }
            }
            drawPath(
                path = path,
                color = if (a.highlight) a.color.copy(alpha = 0.35f) else a.color,
                style = Stroke(width = if (a.highlight) a.width * 5f else a.width)
            )
        }

        is Anno.Text -> drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawText(
                a.text, a.x, a.y,
                android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = a.colorInt
                    textSize = a.size
                }
            )
        }

        is Anno.Shape -> {
            val tl = Offset(minOf(a.x1, a.x2), minOf(a.y1, a.y2))
            val sz = Size(abs(a.x2 - a.x1), abs(a.y2 - a.y1))
            when (a.kind) {
                Anno.ShapeKind.RECT -> drawRect(a.color, tl, sz, style = Stroke(a.stroke))
                Anno.ShapeKind.ELLIPSE -> drawOval(a.color, tl, sz, style = Stroke(a.stroke))
                Anno.ShapeKind.LINE, Anno.ShapeKind.ARROW ->
                    drawLine(a.color, Offset(a.x1, a.y1), Offset(a.x2, a.y2), strokeWidth = a.stroke)
            }
        }

        is Anno.Stamp -> drawImage(
            image = a.bitmap.asImageBitmap(),
            dstOffset = IntOffset(a.x1.toInt(), a.y1.toInt()),
            dstSize = IntSize((a.x2 - a.x1).toInt().coerceAtLeast(1), (a.y2 - a.y1).toInt().coerceAtLeast(1))
        )
    }
}