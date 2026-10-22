package com.docvault.app.ui.tools

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.docvault.app.core.pdf.PdfEngine
import com.docvault.app.ui.theme.LocalAppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolRunner(tool: Tool, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val c = LocalAppColors.current
    val scope = rememberCoroutineScope()

    var pdfs by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var images by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var text by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }

    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u ->
        u?.let { pdfs = listOf(it) }
    }
    val pickManyPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { list ->
        pdfs = list
    }
    val pickImages = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { list ->
        images = list
    }

    val saveAs = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { out ->
        out ?: return@rememberLauncherForActivityResult
        scope.launch {
            status = "Working…"
            val ok = withContext(Dispatchers.IO) {
                ctx.contentResolver.openOutputStream(out)?.use { os ->
                    when (tool.title) {
                        "Merge PDFs" -> PdfEngine.merge(ctx, pdfs, os)
                        "Compress PDF" -> PdfEngine.compress(ctx, pdfs.first(), os)
                        "Protect PDF" -> PdfEngine.protect(ctx, pdfs.first(), password.ifBlank { "1234" }, os)
                        "Create from text" -> { PdfEngine.fromText(text.split("\f"), os); true }
                        "Images to PDF" -> {
                            val bitmaps = images.mapNotNull { u ->
                                ctx.contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it) }
                            }
                            PdfEngine.fromImages(bitmaps, os); true
                        }
                        else -> false
                    }
                } ?: false
            }
            status = if (ok) "Saved" else "Failed"
            if (ok) onDone()
        }
    }

    Scaffold(
        containerColor = c.surface,
        topBar = {
            TopAppBar(
                title = { Text(tool.title) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(androidx.compose.material.icons.Icons.Rounded.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = c.surface)
            )
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(20.dp)) {

            when (tool.title) {
                "Merge PDFs" -> {
                    Button(onClick = { pickManyPdf.launch(arrayOf("application/pdf")) }) {
                        Text("Choose PDFs (${pdfs.size})")
                    }
                }
                "Compress PDF", "Protect PDF" -> {
                    Button(onClick = { pickPdf.launch(arrayOf("application/pdf")) }) {
                        Text(if (pdfs.isEmpty()) "Choose a PDF" else "PDF selected")
                    }
                }
                "Create from text" -> {
                    OutlinedTextField(
                        value = text, onValueChange = { text = it },
                        label = { Text("Type your document") },
                        modifier = Modifier.fillMaxWidth().height(240.dp),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
                "Images to PDF" -> {
                    Button(onClick = { pickImages.launch(arrayOf("image/*")) }) {
                        Text("Choose images (${images.size})")
                    }
                }
            }

            if (tool.title == "Protect PDF") {
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(Modifier.height(22.dp))
            Button(
                onClick = { saveAs.launch("output.pdf") },
                enabled = when (tool.title) {
                    "Merge PDFs" -> pdfs.size >= 2
                    "Compress PDF", "Protect PDF" -> pdfs.isNotEmpty()
                    "Create from text" -> text.isNotBlank()
                    "Images to PDF" -> images.isNotEmpty()
                    else -> false
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Run and save") }

            status?.let {
                Spacer(Modifier.height(14.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
            }
        }
    }
}