package com.docvault.app.ui.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.docvault.app.core.model.DocType
import com.docvault.app.core.pdf.PdfEngine
import com.docvault.app.ui.theme.LocalAppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun EditorScreen(uri: Uri, name: String, onBack: () -> Unit, type: DocType = DocType.PENDING) {
    val ctx = LocalContext.current
    val c = LocalAppColors.current
    val text = remember(uri) { withContext(Dispatchers.IO) { com.docvault.app.core.fs.FileRepo(ctx).readText(uri) ?: "" } }

    Scaffold(
        containerColor = if (type == DocType.PDF) Color(0xFF1A1C20) else c.surface,
        topBar = {
            TopAppBar(
                title = { Text(name, maxLines = 1, style = MaterialTheme.typography.titleSmall) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null) } },
                actions = {
                    if (type == DocType.PDF) {
                        Menu {
                            Button(onClick = { /* share */ }) { Text("Share") }
                            Button(onClick = { /* print */ }) { Text("Print") }
                        } { Icon(MoreVert, null) }
                    } else {
                        IconButton(onClick = { /* TODO export */ }) { Text("Export") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (type == DocType.PDF) Color(0xFF1A1C20) else c.surface,
                    titleContentColor = if (type == DocType.PDF) Color.White else c.textPrimary,
                    navigationIconContentColor = if (type == DocType.PDF) Color.White else c.textPrimary,
                    actionIconContentColor = if (type == DocType.PDF) Color.White else c.textPrimary,
                )
            )
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when (type) {
                DocType.PDF -> {
                    val pageCount = remember(uri) { PdfEngine.pageCount(ctx, uri) }
                    HorizontalPager(state = rememberPagerState(pageCount = { pageCount.coerceAtLeast(1) })) { index ->
                        PdfViewPage(uri = uri, index = index)
                    }
                }
                DocType.TEXT -> {
                    Text(
                        text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textPrimary,
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)
                    )
                }
                else -> Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(type.icon, null, Modifier.size(72.dp), tint = type.fg)
                    Spacer(Modifier.height(14.dp))
                    Text(name, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
                    Text("${type.label} · preview",
                        style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
                }
            }
        }
    }
}

@Composable
private fun PdfViewPage(uri: Uri, index: Int) {
    val ctx = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { androidx.compose.ui.geometry.Offset.Zero }
    val target = (widthDp * 2).coerceIn(600, 2400)

    val bmp = produceState<ImageBitmap?>(index, target) {
        value = withContext(Dispatchers.IO) { PdfEngine.renderPage(ctx, uri, index, target)?.asImageBitmap() }
    }

    Box(Modifier.fillMaxSize(), Alignment.Center) {
        bmp?.let { image ->
            androidx.compose.foundation.Image(
                bitmap = image,
                contentDescription = "Page ${index + 1}",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer(
                        scaleX = scale, scaleY = scale,
                        translationX = offset.x, translationY = offset.y
                    )
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 6f)
                            offset = if (scale > 1f) offset + pan else androidx.compose.ui.geometry.Offset.Zero
                        }
                    }
            )
        } ?: CircularProgressIndicator(color = Color.White)
    }
}