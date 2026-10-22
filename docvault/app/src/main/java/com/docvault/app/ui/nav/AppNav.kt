package com.docvault.app.ui.nav

import android.net.Uri
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.docvault.app.ui.editor.EditorScreen
import com.docvault.app.ui.library.LibraryScreen
import com.docvault.app.ui.library.LibraryTab
import com.docvault.app.ui.scan.ScanScreen
import com.docvault.app.ui.settings.SettingsScreen
import com.docvault.app.ui.tools.ToolsScreen
import com.docvault.app.ui.viewer.ViewerScreen

private const val ROUTE_FILES = "files"
private const val ROUTE_RECENTS = "recents"
private const val ROUTE_FAVES = "faves"
private const val ROUTE_TOOLS = "tools"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_VIEWER = "viewer"
private const val ROUTE_EDITOR = "editor"
private const val ROUTE_SCAN = "scan"

private val BAR_ROUTES = setOf(ROUTE_FILES, ROUTE_RECENTS, ROUTE_FAVES, ROUTE_TOOLS, ROUTE_SETTINGS)

@Composable
fun DocVaultNav(incoming: Uri? = null, onIncomingConsumed: () -> Unit = {}) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = route in BAR_ROUTES

    LaunchedEffect(incoming) {
        val uri = incoming ?: return@LaunchedEffect
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: "Document"
        nav.navigate("$ROUTE_VIEWER?u=${Uri.encode(uri.toString())}&n=${Uri.encode(name)}")
        onIncomingConsumed()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    NavTab(nav, route, ROUTE_FILES, "Files", Icons.Rounded.FolderOpen)
                    NavTab(nav, route, ROUTE_RECENTS, "Recents", Icons.Rounded.History)
                    NavTab(nav, route, ROUTE_FAVES, "Favourites", Icons.Rounded.Star)
                    NavTab(nav, route, ROUTE_TOOLS, "Tools", Icons.Rounded.Handyman)
                    NavTab(nav, route, ROUTE_SETTINGS, "Settings", Icons.Rounded.Settings)
                }
            }
        }
    ) { _ ->
        NavHost(
            navController = nav,
            startDestination = ROUTE_FILES,
            enterTransition = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(180)) },
            exitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(150)) },
        ) {
            composable(ROUTE_FILES) { LibraryScreen(LibraryTab.FILES, nav::openViewer) }
            composable(ROUTE_RECENTS) { LibraryScreen(LibraryTab.RECENTS, nav::openViewer) }
            composable(ROUTE_FAVES) { LibraryScreen(LibraryTab.FAVOURITES, nav::openViewer) }
            composable(ROUTE_TOOLS) { ToolsScreen(nav::openViewer) }
            composable(ROUTE_SETTINGS) { SettingsScreen() }
            composable(ROUTE_SCAN) { ScanScreen(onBack = { nav.popBackStack() }) }

            composable(
                "$ROUTE_VIEWER?u={u}&n={n}",
                arguments = listOf(
                    navArgument("u") { type = NavType.StringType },
                    navArgument("n") { type = NavType.StringType },
                )
            ) { entry ->
                val u = Uri.parse(entry.arguments?.getString("u").orEmpty())
                val n = entry.arguments?.getString("n").orEmpty()
                ViewerScreen(
                    uri = u,
                    name = n,
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate("$ROUTE_EDITOR?u=${Uri.encode(u.toString())}&n=${Uri.encode(n)}") }
                )
            }

            composable(
                "$ROUTE_EDITOR?u={u}&n={n}",
                arguments = listOf(
                    navArgument("u") { type = NavType.StringType },
                    navArgument("n") { type = NavType.StringType },
                )
            ) { entry ->
                EditorScreen(
                    uri = Uri.parse(entry.arguments?.getString("u").orEmpty()),
                    name = entry.arguments?.getString("n").orEmpty(),
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }
}

private fun NavHostController.openViewer(uri: Uri, name: String) {
    navigate("$ROUTE_VIEWER?u=${Uri.encode(uri.toString())}&n=${Uri.encode(name)}")
}

@Composable
private fun RowScope.NavTab(
    nav: NavHostController,
    current: String?,
    route: String,
    label: String,
    icon: ImageVector,
) {
    NavigationBarItem(
        selected = current == route,
        onClick = {
            if (current != route) {
                nav.navigate(route) { launchSingleTop = true; popUpTo(ROUTE_FILES) { inclusive = false } }
            }
        },
        icon = { Icon(icon, label, modifier = Modifier.size(21.dp)) },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        )
    )
}