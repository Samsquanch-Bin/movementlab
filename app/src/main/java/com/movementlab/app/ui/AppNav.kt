package com.movementlab.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.movementlab.app.data.AppViewModel
import com.movementlab.app.ui.screens.ExerciseDetailScreen
import com.movementlab.app.ui.screens.HistoryScreen
import com.movementlab.app.ui.screens.HomeScreen
import com.movementlab.app.ui.screens.LibraryScreen
import com.movementlab.app.ui.screens.OnboardingScreen
import com.movementlab.app.ui.screens.ProgressScreen
import com.movementlab.app.ui.screens.RoutineEditorScreen
import com.movementlab.app.ui.screens.RoutinesScreen
import com.movementlab.app.ui.screens.SettingsScreen
import com.movementlab.app.ui.screens.WorkoutScreen

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("home", "Home", Icons.Filled.Home),
    Tab("routines", "Routines", Icons.AutoMirrored.Filled.List),
    Tab("library", "Exercises", Icons.Filled.Search),
    Tab("history", "History", Icons.Filled.DateRange),
    Tab("progress", "Progress", Icons.Filled.Star),
)

/** Navigation helpers passed to screens. */
class Nav(private val c: NavHostController) {
    fun back() { c.popBackStack() }
    fun exercise(id: String) = c.navigate("exercise/$id")
    fun editRoutine(id: String) = c.navigate("routine/$id")
    fun workout() = c.navigate("workout") { launchSingleTop = true }
    fun settings() = c.navigate("settings")
    fun home() = c.navigate("home") {
        popUpTo(c.graph.id) { inclusive = true }
        launchSingleTop = true
    }
    fun tab(route: String) = c.navigate(route) {
        popUpTo("home") { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun AppNav(vm: AppViewModel) {
    val controller = rememberNavController()
    val nav = Nav(controller)
    val data by vm.data.collectAsStateWithLifecycle()
    val backStack by controller.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBar = tabs.any { it.route == route }
    val start = remember { if (data.settings.level == null) "onboarding" else "home" }

    Scaffold(
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    tabs.forEach { t ->
                        NavigationBarItem(
                            selected = route == t.route,
                            onClick = { nav.tab(t.route) },
                            icon = { Icon(t.icon, contentDescription = t.label) },
                            label = { Text(t.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(controller, startDestination = start, modifier = Modifier.padding(padding)) {
            composable("onboarding") { OnboardingScreen(vm, onDone = { nav.home() }) }
            composable("home") { HomeScreen(vm, nav) }
            composable("routines") { RoutinesScreen(vm, nav) }
            composable("routine/{id}") { RoutineEditorScreen(vm, nav, it.arguments?.getString("id") ?: "new") }
            composable("library") { LibraryScreen(vm, nav) }
            composable("exercise/{id}") { ExerciseDetailScreen(vm, nav, it.arguments?.getString("id") ?: "") }
            composable("history") { HistoryScreen(vm, nav) }
            composable("progress") { ProgressScreen(vm) }
            composable("settings") { SettingsScreen(vm, nav) }
            composable("workout") { WorkoutScreen(vm, nav) }
        }
    }
}
