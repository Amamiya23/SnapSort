package com.snapsort.app.ui.navigation

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.snapsort.app.SnapSortDependencies
import com.snapsort.app.ui.delete.DeleteViewModel
import com.snapsort.app.ui.home.HomeScreen
import com.snapsort.app.ui.scan.ScanProgressScreen
import com.snapsort.app.ui.selection.PhotoSelectionScreen
import com.snapsort.app.ui.settings.SettingsScreen

private const val HomeRoute = "home"
private const val GroupSelectionRoute = "group_selection/{groupId}"
private const val GroupSelectionTransitionMillis = 260
private const val GroupSelectionFadeMillis = 160

@Composable
fun SnapSortApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    var pendingFolderUri by remember { mutableStateOf<Uri?>(null) }
    var deleteRequestLaunched by remember { mutableStateOf(false) }
    val deleteViewModel: DeleteViewModel = viewModel(
        factory = DeleteViewModel.Factory(
            context = context,
            contentResolver = context.contentResolver,
            taskRepository = SnapSortDependencies.taskRepository(context)
        )
    )
    val deleteAuthorizationState by deleteViewModel.authorizationState.collectAsState()
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        deleteRequestLaunched = false
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            deleteViewModel.onDeleteAccepted()
        } else {
            deleteViewModel.onDeleteRejected()
        }
    }

    LaunchedEffect(deleteAuthorizationState.pendingIntent) {
        val pendingIntent = deleteAuthorizationState.pendingIntent
        if (pendingIntent != null && !deleteRequestLaunched) {
            deleteRequestLaunched = true
            deleteLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
        }
    }

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
            pendingFolderUri = uri
            navController.navigate("scan_progress")
        }
    }

    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable(HomeRoute) {
            HomeScreen(
                onSelectFolder = { folderLauncher.launch(null) },
                onRescan = { folderUri ->
                    pendingFolderUri = Uri.parse(folderUri)
                    navController.navigate("scan_progress")
                },
                onOpenGroup = { groupId ->
                    navController.navigate("group_selection/$groupId")
                },
                onOpenSettings = { navController.navigate("settings") },
                onDeleteConfirmed = { deleteViewModel.prepareDeleteRequest() },
                deleteMessage = deleteAuthorizationState.deleteMessage,
                onDeleteMessageShown = { deleteViewModel.clearDeleteMessage() }
            )
        }
        composable("scan_progress") {
            ScanProgressScreen(
                folderUri = pendingFolderUri,
                onComplete = {
                    pendingFolderUri = null
                    navController.popBackStack("home", inclusive = false)
                },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(
            route = GroupSelectionRoute,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(durationMillis = GroupSelectionTransitionMillis)
                ) + fadeIn(animationSpec = tween(durationMillis = GroupSelectionFadeMillis))
            },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(durationMillis = GroupSelectionTransitionMillis)
                ) + fadeOut(animationSpec = tween(durationMillis = GroupSelectionFadeMillis))
            }
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId").orEmpty()
            PhotoSelectionScreen(
                groupId = groupId,
                onDone = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
