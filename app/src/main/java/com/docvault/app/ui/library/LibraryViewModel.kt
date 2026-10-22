package com.docvault.app.ui.library

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.docvault.app.core.fs.FileRepo
import com.docvault.app.core.model.*
import com.docvault.app.core.prefs.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class LibraryTab { FILES, RECENTS, FAVOURITES }

data class LibraryState(
    val root: Uri? = null,
    val trail: List<Pair<Uri, String>> = emptyList(),
    val items: List<DocItem> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val query: String = "",
    val filter: DocType? = null,
    val sort: SortKey = SortKey.NAME,
    val ascending: Boolean = true,
    val grid: Boolean = false,
    val selected: Set<String> = emptySet(),
    val favourites: Set<String> = emptySet(),
    val recents: List<String> = emptyList(),
) {
    val dir: Uri? get() = trail.lastOrNull()?.first ?: root
    val title: String get() = trail.lastOrNull()?.second ?: "My Documents"
    val selecting: Boolean get() = selected.isNotEmpty()
}

class LibraryViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = FileRepo(app)
    private val prefs = Prefs(app)

    private val _state = MutableStateFlow(LibraryState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.rootUri.collect { raw ->
                _state.update { it.copy(root = raw?.let(Uri::parse)) }
                reload()
            }
        }
        viewModelScope.launch { prefs.grid.collect { g -> _state.update { it.copy(grid = g) } } }
        viewModelScope.launch {
            prefs.sortKey.collect { k ->
                _state.update { it.copy(sort = runCatching { SortKey.valueOf(k) }.getOrDefault(SortKey.NAME)) }
            }
        }
        viewModelScope.launch { prefs.sortAsc.collect { a -> _state.update { it.copy(ascending = a) } } }
        viewModelScope.launch { prefs.favourites.collect { f -> _state.update { it.copy(favourites = f) } } }
        viewModelScope.launch { prefs.recents.collect { r -> _state.update { it.copy(recents = r) } } }
    }

    fun grantRoot(uri: Uri) = viewModelScope.launch {
        prefs.setRoot(uri.toString())
        _state.update { it.copy(trail = emptyList()) }
        reload()
    }

    fun openFolder(item: DocItem) {
        _state.update { it.copy(trail = it.trail + (item.uri to item.name), selected = emptySet()) }
        reload()
    }

    fun jumpTo(index: Int) {
        _state.update { it.copy(trail = it.trail.take(index + 1), selected = emptySet()) }
        reload()
    }

    fun goUp() {
        val t = _state.value.trail
        if (t.isNotEmpty()) jumpTo(t.size - 2)
    }

    fun setQuery(q: String) = _state.update { it.copy(query = q) }
    fun setFilter(t: DocType?) = _state.update { it.copy(filter = t) }

    fun cycleSort(key: SortKey) = viewModelScope.launch {
        val s = _state.value
        val asc = if (s.sort == key) !s.ascending else true
        prefs.setSort(key.name, asc)
        reload()
    }

    fun toggleGrid() = viewModelScope.launch { prefs.setGrid(!_state.value.grid) }

    fun toggleSelect(uri: String) = _state.update {
        it.copy(selected = if (uri in it.selected) it.selected - uri else it.selected + uri)
    }

    fun clearSelection() = _state.update { it.copy(selected = emptySet()) }

    fun toggleFavourite(uri: String) = viewModelScope.launch { prefs.toggleFavourite(uri) }

    fun markOpened(uri: String) = viewModelScope.launch { prefs.pushRecent(uri) }

    fun reload() = viewModelScope.launch {
        val s = _state.value
        val dir = s.dir
        if (dir == null) {
            _state.update { it.copy(items = emptyList(), loading = false, refreshing = false) }
            return@launch
        }
        _state.update { it.copy(loading = it.items.isEmpty(), refreshing = it.items.isNotEmpty()) }
        val raw = withContext(Dispatchers.IO) { runCatching { repo.children(dir) }.getOrDefault(emptyList()) }
        val visible = raw
            .filter { s.query.isBlank() || it.name.contains(s.query, ignoreCase = true) }
            .filter { s.filter == null || it.type == s.filter }
            .sortedWith(orderBy(s.sort, s.ascending))
        _state.update { it.copy(items = visible, loading = false, refreshing = false) }
    }

    private fun orderBy(key: SortKey, asc: Boolean): Comparator<DocItem> {
        val c: Comparator<DocItem> = when (key) {
            SortKey.NAME -> compareBy { it.name.lowercase() }
            SortKey.MODIFIED -> compareByDescending { it.modified }
            SortKey.SIZE -> compareByDescending { it.size }
            SortKey.TYPE -> compareBy<DocItem> { it.type.ordinal }.thenBy { it.name.lowercase() }
        }
        val dirFirst = compareByDescending<DocItem> { it.isDir }
        return if (asc) dirFirst.then(c) else dirFirst.then(c.reversed())
    }

    fun createFolder(name: String) = viewModelScope.launch {
        _state.value.dir?.let { withContext(Dispatchers.IO) { repo.createFolder(it, name) } }
        reload()
    }

    fun createTextFile(name: String, body: String) = viewModelScope.launch {
        val dir = _state.value.dir ?: return@launch
        withContext(Dispatchers.IO) {
            repo.createFile(dir, name, "text/plain")?.let { repo.writeText(it, body) }
        }
        reload()
    }

    fun rename(uri: Uri, newName: String) = viewModelScope.launch {
        withContext(Dispatchers.IO) { repo.rename(uri, newName) }
        reload()
    }

    fun delete(uris: Collection<String>) = viewModelScope.launch {
        withContext(Dispatchers.IO) { uris.forEach { repo.delete(Uri.parse(it)) } }
        clearSelection()
        reload()
    }

    fun transfer(uris: Collection<String>, dest: Uri, move: Boolean) = viewModelScope.launch {
        withContext(Dispatchers.IO) { uris.forEach { repo.transfer(Uri.parse(it), dest, move) } }
        clearSelection()
        reload()
    }

    /** Resolve URIs from Recents/Favourites into displayable items. */
    fun collection(tab: LibraryTab): List<DocItem> {
        val s = _state.value
        val uris = when (tab) {
            LibraryTab.RECENTS -> s.recents
            LibraryTab.FAVOURITES -> s.favourites.toList()
            LibraryTab.FILES -> return s.items
        }
        return uris.mapNotNull { raw ->
            runCatching {
                val uri = Uri.parse(raw)
                val name = repo.displayName(uri)
                val mime = getApplication<Application>().contentResolver.getType(uri)
                DocItem(uri, name, false, repo.sizeOf(uri), 0L, mime, detectType(name, mime, false))
            }.getOrNull()
        }
    }
}