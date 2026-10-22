package com.docvault.app.ui.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset

@Composable
fun SignatureDialog(onCancel: () -> Unit, onConfirm: (Bitmap) -> Unit) {
    val paths = remember { mutableStateListOf<Path>() }
    var current by remember { mutableStateOf<Path?>(null) }
    val paint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 5f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Draw your signature") },
        text = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .padding(2.dp)
            ) {
                androidx.compose.foundation.Canvas(
                    Modifier
                        .fillMaxSize()
                        .then(
                            Modifier.pointerInputCompat(
                                onStart = { p ->
                                    Path().also { it.moveTo(p.x, p.y); current = it; paths.add(it) }
                                },
                                onMove = { p -> current?.lineTo(p.x, p.y) }
                            )
                        )
                ) {
                    paths.forEach { p ->
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawPath(p, paint)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val bmp = Bitmap.createBitmap(600, 240, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                canvas.drawColor(android.graphics.Color.TRANSPARENT)
                paths.forEach { canvas.drawPath(it, paint) }
                onConfirm(crop(bmp))
            }) { Text("Use") }
        },
        dismissButton = {
            TextButton(onClick = { paths.clear(); current = null }) { Text("Clear") }
        }
    )
}

private fun crop(src: Bitmap): Bitmap {
    var minX = src.width; var minY = src.height; var maxX = 0; var maxY = 0
    var found = false
    for (y in 0 until src.height) {
        for (x in 0 until src.width) {
            if (android.graphics.Color.alpha(src.getPixel(x, y)) > 10) {
                found = true
                if (x < minX) minX = x; if (x > maxX) maxX = x
                if (y < minY) minY = y; if (y > maxY) maxY = y
            }
        }
    }
    if (!found) return src
    val w = (maxX - minX + 20).coerceAtMost(src.width - minX)
    val h = (maxY - minY + 20).coerceAtMost(src.height - minY)
    return Bitmap.createBitmap(src, (minX - 10).coerceAtLeast(0), (minY - 10).coerceAtLeast(0), w, h)
}

private fun Modifier.pointerInputCompat(onStart: (androidx.compose.ui.geometry.Offset) -> Unit, onMove: (androidx.compose.ui.geometry.Offset) -> Unit): Modifier =
    this.then(
        Modifier.pointerInput(Unit) {
            androidx.compose.foundation.gestures.detectDragGestures(
                onDragStart = onStart,
                onDrag = { change, _ -> onMove(change.position) }
            )
        }
    )
