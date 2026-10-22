package com.docvault.app.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Camera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.docvault.app.core.pdf.PdfEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val executor = remember { Executors.newSingleThreadExecutor() }

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }

    val shots = remember { mutableStateListOf<File>() }
    val capture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }

    LaunchedEffect(granted) {
        if (!granted) return@LaunchedEffect
        val provider = ProcessCameraProvider.getInstance(ctx).get()
        val preview = androidx.camera.core.Preview.Builder().build()
        val selector = CameraSelector.DEFAULT_BACK_CAMERA
        runCatching {
            provider.unbindAll()
            provider.bindToLifecycle(owner, selector, preview, capture)
        }
    }

    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { out ->
        out ?: return@rememberLauncherForActivityResult
        scope.launch {
            withContext(Dispatchers.IO) {
                val bitmaps = shots.mapNotNull { f ->
                    BitmapFactory.decodeFile(f.absolutePath)?.let { rotate(it) }
                }
                ctx.contentResolver.openOutputStream(out)?.use { PdfEngine.fromImages(bitmaps, it) }
                shots.forEach { it.delete() }
            }
            onBack()
        }
    }

    Scaffold(
        containerColor = Color(0xFF15171C),
        topBar = {
            TopAppBar(
                title = { Text("Scan") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, null) } },
                actions = {
                    if (shots.isNotEmpty()) {
                        TextButton(onClick = { save.launch("Scan.pdf") }) { Text("Save PDF", color = Color.White) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF15171C),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                )
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            Box(Modifier.weight(1f)) {
                if (!granted) {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Camera permission needed", color = Color.White)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { askPermission.launch(Manifest.permission.CAMERA) }) { Text("Grant") }
                    }
                } else {
                    AndroidView(
                        factory = { c ->
                            PreviewView(c).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                                val providerFuture = ProcessCameraProvider.getInstance(c)
                                providerFuture.addListener({
                                    val provider = providerFuture.get()
                                    val preview = androidx.camera.core.Preview.Builder().build().also {
                                        it.surfaceProvider = surfaceProvider
                                    }
                                    runCatching {
                                        provider.unbindAll()
                                        provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                                    }
                                }, ContextCompat.getMainExecutor(c))
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            if (shots.isNotEmpty()) {
                LazyRow(
                    Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(shots) { f ->
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_gallery),
                            contentDescription = null,
                            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp))
                        )
                    }
                }
            }

            Box(Modifier.fillMaxWidth().padding(18.dp), Alignment.Center) {
                FloatingActionButton(
                    onClick = {
                        val file = File(ctx.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
                        val opts = ImageCapture.OutputFileOptions.Builder(file).build()
                        capture.takePicture(opts, executor, object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(results: ImageCapture.OutputFileResults) { shots.add(file) }
                            override fun onError(e: ImageCaptureException) { }
                        })
                    },
                    containerColor = Color.White,
                    contentColor = Color(0xFF15171C),
                    shape = RoundedCornerShape(20.dp)
                ) { Icon(Icons.Rounded.Camera, null) }
            }
        }
    }
}

private fun rotate(src: Bitmap): Bitmap {
    val m = Matrix().apply { postRotate(90f) }
    return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
}