package com.docvault.app.ui.library

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.docvault.app.core.model.DocItem
import com.docvault.app.core.model.DocType
import com.docvault.app.core.model.SortKey
import com.docvault.app.ui.components.*
import com.docvault.app.ui.theme.LocalAppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    tab: LibraryTab,
    onOpenFile: (android.net.Uri, String) -> Unit,
    vm: LibraryViewModel = viewModel(),
) {
    val ctx = LocalContext.current
    val c = LocalAppColors.current
    val s by vm.state.collectAsStateWithLifecycle()

    var searchOpen by remember { mutableStateOf(false) }
    var sortOpen by remember { mutableStateOf(false) }
    var fabOpen by remember { mutableStateOf(false) }
    var newFolder by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<DocItem?>(null) }
    var menuTarget by remember { mutableStateOf<DocItem?>(null) }
    var moveMode by remember { mutableStateOf<Boolean?>(null) }

    val pickRoot = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            ctx.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        vm.grantRoot(uri)
    }

    val pickDest = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val move = moveMode ?: false
        vm.transfer(s.selected, uri, move)
        moveMode = null
    }

    LaunchedEffect(tab) { if (tab != LibraryTab.FILES) vm.clearSelection() }

    Scaffold(
        containerColor = c.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (s.selecting) SelectionBar(vm) else HeaderBar(
                tab = tab, s = s, vm = vm,
                searchOpen = searchOpen,
                onToggleSearch = { searchOpen = !searchOpen },
                onSort = { sortOpen = true }
            )
        },
        floatingActionButton = {
            if (tab == LibraryTab.FILES && s.root != null && !s.selecting) {
                Column(horizontalAlignment = Alignment.End) {
                    AnimatedVisibility(fabOpen) {
                        Column(horizontalAlignment = Alignment.End) {
                            MiniFab("New folder", Icons.Rounded.CreateNewFolder) { fabOpen = false; newFolder = true }
                            MiniFab("New text file", Icons.Rounded.NoteAdd) {
                                fabOpen = false
                                vm.createTextFile("Note.txt", "")
                            }
                            MiniFab("New PDF", Icons.Rounded.PictureAsPdf) {
                                fabOpen = false
                                vm.createTextFile("Blank.pdf", "")
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    FloatingActionButton(
                        onClick = { fabOpen = !fabOpen },
                        containerColor = c.brand,
                        contentColor = androidx.compose.ui.graphics.Color.White,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(if (fabOpen) Icons.Rounded.Close else Icons.Rounded.Add, null)
                    }
                }
            }
        }
    ) { pad -

        Box(Modifier.fillMaxSize().padding(pad)) {
            when {
                s.root == null -> EmptyState(
                    title = "No folder selected",
                    body = "Pick a folder and DocVault will manage everything inside it. No permissions, no cloud.",
                    actionLabel = "Choose folder",
                    onAction = { pickRoot.launch(null) }
                )

                s.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = c.brand) }

                else -> {
                    val list = if (tab == LibraryTab.FILES) s.items else vm.collection(tab)
                    val folders = list.filter { it.isDir }
                    val files = list.filterNot { it.isDir }

                    if (list.isEmpty()) {
                        EmptyState(
                            title = if (s.query.isNotBlank()) "No matches" else "This folder is empty",
                            body = if (s.query.isNotBlank()) "Try a different search."
                                else "Add files here or use the + button to create one."
                        )
                    } else if (s.grid) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(108.dp),
                            contentPadding = PaddingValues(12.dp, 8.dp, 12.dp, 100.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(list, key = { it.uri.toString() }) { item ->
                                GridCell(
                                    item = item,
                                    selected = item.uri.toString() in s.selected,
                                    onClick = { onTap(item, s, vm, onOpenFile) },
                                    onLongClick = { vm.toggleSelect(item.uri.toString()) }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(top = 4.dp, bottom = 100.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (tab == LibraryTab.FILES && s.trail.isEmpty() && s.query.isBlank()) {
                                item("summary") {
                                    Spacer(Modifier.height(8.dp))
                                    GradientSummaryCard(
                                        folders = folders.size,
                                        files = files.size,
                                        onScan = { }
                                    )
                                    Spacer(Modifier.height(4.dp))
                                }
                            }
                            if (folders.isNotEmpty()) {
                                item("h-folders") { SectionLabel("Folders") }
                                items(folders, key = { it.uri.toString() }) { item ->
                                    FileRow(
                                        item = item,
                                        selected = item.uri.toString() in s.selected,
                                        favourite = item.uri.toString() in s.favourites,
                                        onClick = { onTap(item, s, vm, onOpenFile) },
                                        onLongClick = { vm.toggleSelect(item.uri.toString()) },
                                        onMenu = { menuTarget = item }
                                    )
                                }
                            }
                            if (files.isNotEmpty()) {
                                item("h-files") { SectionLabel(if (folders.isEmpty()) "Files" else "Documents") }
                                items(files, key = { it.uri.toString() }) { item ->
                                    FileRow(
                                        item = item,
                                        selected = item.uri.toString() in s.selected,
                                        favourite = item.uri.toString() in s.favourites,
                                        onClick = { onTap(item, s, vm, onOpenFile) },
                                        onLongClick = { vm.toggleSelect(item.uri.toString()) },
                                        onMenu = { menuTarget = item }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (sortOpen) SortSheet(s.sort, s.ascending, vm) { sortOpen = false }

    if (newFolder) NameDialog("New folder", "Untitled folder", onCancel = { newFolder = false }) {
        vm.createFolder(it); newFolder = false
    }

    renameTarget?.let { item ->
        NameDialog("Rename", item.name, onCancel = { renameTarget = null }) {
            vm.rename(item.uri, it); renameTarget = null
        }
    }

    menuTarget?.let { item ->
        ItemSheet(
            item = item,
            favourite = item.uri.toString() in s.favourites,
            onDismiss = { menuTarget = null },
            onFavourite = { vm.toggleFavourite(item.uri.toString()); menuTarget = null },
            onRename = { renameTarget = item; menuTarget = null },
            onShare = { menuTarget = null; shareFile(ctx, item) },
            onMove = { vm.clearSelection(); vm.toggleSelect(item.uri.toString()); moveMode = true; menuTarget = null; pickDest.launch(null) },
            onDelete = { vm.delete(listOf(item.uri.toString())); menuTarget = null },
        )
    }
}

private fun onTap(item: DocItem, s: LibraryState, vm: LibraryViewModel, open: (Uri, String) -> Unit) {
    when {
        s.selecting -> vm.toggleSelect(item.uri.toString())
        item.isDir -> vm.openFolder(item)
        else -> { vm.markOpened(item.uri.toString()); open(item.uri, item.name) }
    }
}

private fun shareFile(ctx: android.content.Context, item: DocItem) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = item.mime ?: "*/*"
        putExtra(Intent.EXTRA_STREAM, item.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    ctx.startActivity(Intent.createChooser(intent, item.name))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeaderBar(
    tab: LibraryTab,
    s: LibraryState,
    vm: LibraryViewModel,
    searchOpen: Boolean,
    onToggleSearch: () -> Unit,
    onSort: () -> Unit,
) {
    val c = LocalAppColors.current
    Column(Modifier.background(c.surface).padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (s.trail.isNotEmpty() && tab == LibraryTab.FILES) {
                        Icon(
                            Icons.Rounded.ArrowBack, "Up",
                            tint = c.textSecondary,
                            modifier = Modifier.size(20.dp).padding(end = 4.dp)
                                .let { it }
                        )
                    }
                    Text(
                        when (tab) {
                            LibraryTab.FILES -> s.title
                            LibraryTab.RECENTS -> "Recents"
                            LibraryTab.FAVOURITES -> "Favourites"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        color = c.textPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                Text("Offline · nothing leaves your phone",
                    style = MaterialTheme.typography.labelSmall, color = c.textTertiary)
            }
            HeaderIcon(Icons.Rounded.Search, onToggleSearch)
            HeaderIcon(Icons.Rounded.SwapVert, onSort)
            HeaderIcon(if (s.grid) Icons.Rounded.ViewList else Icons.Rounded.GridView) { vm.toggleGrid() }
        }

        AnimatedVisibility(searchOpen) {
            OutlinedTextField(
                value = s.query,
                onValueChange = vm::setQuery,
                placeholder = { Text("Search documents…") },
                leadingIcon = { Icon(Icons.Rounded.Search, null, Modifier.size(18.dp)) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.brand,
                    unfocusedBorderColor = c.border,
                )
            )
        }

        if (s.trail.isNotEmpty() && tab == LibraryTab.FILES) {
            TextButton(onClick = vm::goUp, contentPadding = PaddingValues(0.dp)) {
                Icon(Icons.Rounded.NorthWest, null, Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Up one level", style = MaterialTheme.typography.labelMedium)
            }
        }

        Row(
            Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip("All", s.filter == null) { vm.setFilter(null) }
            listOf(DocType.PDF, DocType.WORD, DocType.SHEET, DocType.SLIDES, DocType.IMAGE, DocType.TEXT)
                .forEach { t -> FilterChip(t.label + "s", s.filter == t) { vm.setFilter(t) } }
        }
    }
}

@Composable
private fun HeaderIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(38.dp).padding(start = 2.dp),
        Alignment.Center
    ) {
        IconButton(onClick = onClick) {
            Icon(icon, null, tint = LocalAppColors.current.textSecondary, modifier = Modifier.size(21.dp))
        }
    }
}

@Composable
private fun MiniFab(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = c.surfaceAlt,
        shadowElevation = 2.dp,
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = c.textSecondary, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(10.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = c.textPrimary)
        }
    }
}

@Composable
private fun SelectionBar(vm: LibraryViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    val c = LocalAppColors.current
    TopAppBar(
        title = { Text("${s.selected.size} selected", fontWeight = FontWeight.SemiBold) },
        navigationIcon = {
            IconButton(onClick = vm::clearSelection) { Icon(Icons.Rounded.Close, null) }
        },
        actions = {
            IconButton(onClick = { vm.delete(s.selected) }) { Icon(Icons.Rounded.DeleteOutline, null) }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = c.surface)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortSheet(current: SortKey, asc: Boolean, vm: LibraryViewModel, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = LocalAppColors.current.surfaceAlt) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            Text("Sort by", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            SortKey.entries.forEach { key ->
                Row(
                    Modifier.fillMaxWidth().clickableRow { vm.cycleSort(key) }.padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = key == current, onClick = { vm.cycleSort(key) })
                    Spacer(Modifier.width(6.dp))
                    Text(key.label, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.weight(1f))
                    if (key == current) {
                        Icon(
                            if (asc) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                            null, tint = LocalAppColors.current.brand, modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit) =
    androidx.compose.foundation.clickable(onClick = onClick).let { this.then(it) }

@Composable
fun NameDialog(title: String, initial: String, onCancel: () -> Unit, onConfirm: (String) -> Unit) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            TextButton(onClick = { if (value.isNotBlank()) onConfirm(value.trim()) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } }
    )
}