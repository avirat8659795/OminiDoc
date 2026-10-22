package com.docvault.app.ui.editor

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
sealed interface Anno {
    val colorInt: Int

    @Immutable
    data class Ink(
        val points: List<Pair<Float, Float>>,
        val color: Color,
        val width: Float,
        val highlight: Boolean = false,
    ) : Anno {
        override val colorInt: Int get() = color.toArgbInt()
    }

    @Immutable
    data class Text(
        val text: String,
        val x: Float,
        val y: Float,
        val color: Color,
        val size: Float,
    ) : Anno {
        override val colorInt: Int get() = color.toArgbInt()
    }

    @Immutable
    data class Shape(
        val kind: ShapeKind,
        val x1: Float, val y1: Float,
        val x2: Float, val y2: Float,
        val color: Color,
        val stroke: Float,
    ) : Anno {
        override val colorInt: Int get() = color.toArgbInt()
    }

    @Immutable
    data class Stamp(
        val bitmap: Bitmap,
        val x1: Float, val y1: Float,
        val x2: Float, val y2: Float,
    ) : Anno {
        override val colorInt: Int get() = 0xFF000000.toInt()
    }

    enum class ShapeKind { RECT, ELLIPSE, LINE, ARROW }
}

fun Color.toArgbInt(): Int =
    android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt(),
    )

enum class Tool { PEN, HIGHLIGHT, TEXT, SHAPE, SIGN, ERASE }