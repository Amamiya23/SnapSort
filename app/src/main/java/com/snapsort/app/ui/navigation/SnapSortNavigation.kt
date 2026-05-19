package com.snapsort.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.snapsort.app.ui.home.HomeScreen
import com.snapsort.app.ui.scan.ScanProgressScreen
import com.snapsort.app.ui.selection.PhotoSelectionScreen
import com.snapsort.app.ui.settings.SettingsScreen

@Composable
fun SnapSortApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen() }
        composable("scan_progress") { ScanProgressScreen() }
        composable("group_selection/{groupId}") { PhotoSelectionScreen() }
        composable("settings") { SettingsScreen() }
    }
}
