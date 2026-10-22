package com.docvault.app.core.prefs

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.store by preferencesDataStore("docvault_prefs")

class Prefs(private val ctx: Context) {

    private object K {
        val ROOT = stringPreferencesKey("root_uri")
        val THEME = stringPreferencesKey("theme_mode")
        val GRID = booleanPreferencesKey("grid_view")
        val SORT = stringPreferencesKey("sort_key")
        val ASC = booleanPreferencesKey("sort_asc")
        val FAVS = stringSetPreferencesKey("favourites")
        val RECENTS = stringSetPreferencesKey("recents")
    }

    val rootUri: Flow<String?> = ctx.store.data.map { it[K.ROOT] }
    val theme: Flow<String> = ctx.store.data.map { it[K.THEME] ?: "system" }
    val grid: Flow<Boolean> = ctx.store.data.map { it[K.GRID] ?: false }
    val sortKey: Flow<String> = ctx.store.data.map { it[K.SORT] ?: "NAME" }
    val sortAsc: Flow<Boolean> = ctx.store.data.map { it[K.ASC] ?: true }
    val favourites: Flow<Set<String>> = ctx.store.data.map { it[K.FAVS] ?: emptySet() }
    val recents: Flow<List<String>> = ctx.store.data.map { it[K.RECENTS] ?: emptySet() }
        .map { set -> set.toList() }

    suspend fun setRoot(uri: String) = ctx.store.edit { it[K.ROOT] = uri }
    suspend fun setTheme(mode: String) = ctx.store.edit { it[K.THEME] = mode }
    suspend fun setGrid(v: Boolean) = ctx.store.edit { it[K.GRID] = v }
    suspend fun setSort(key: String, asc: Boolean) = ctx.store.edit { it[K.SORT] = key; it[K.ASC] = asc }

    suspend fun toggleFavourite(uri: String) = ctx.store.edit { p ->
        val cur = p[K.FAVS] ?: emptySet()
        p[K.FAVS] = if (uri in cur) cur - uri else cur + uri
    }

    suspend fun pushRecent(uri: String) = ctx.store.edit { p ->
        val cur = (p[K.RECENTS] ?: emptySet()).toMutableList()
        cur.remove(uri)
        cur.add(0, uri)
        p[K.RECENTS] = cur.take(80).toSet()
    }
}