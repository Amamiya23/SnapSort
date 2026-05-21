package com.snapsort.app.ui.scan

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.net.Uri
import com.snapsort.app.SnapSortDependencies

@Composable
fun ScanProgressScreen(
    folderUri: Uri?,
    onComplete: () -> Unit,
    onCancel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: ScanProgressViewModel = viewModel(
        factory = ScanProgressViewModel.Factory(
            scanner = SnapSortDependencies.folderScanner(context),
            taskRepository = SnapSortDependencies.taskRepository(context),
            userSettingsRepository = SnapSortDependencies.userSettingsRepository(context)
        )
    )
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(folderUri) {
        if (folderUri != null) {
            viewModel.scanFolder(folderUri)
        }
    }

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) {
            onComplete()
        }
    }

    val currentStage = state.errorMessage ?: state.currentStage
    val currentProgress = state.currentProgress
    val totalItems = state.totalItems
    val currentFileName = state.currentFileName
    val progress = if (totalItems > 0) currentProgress.toFloat() / totalItems.toFloat() else 0f

    Surface(
        modifier = modifier
            .fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(80.dp),
                strokeWidth = 6.dp
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = currentStage,
                style = MaterialTheme.typography.titleLarge,
                color = if (state.errorMessage == null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.error
                },
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "已处理 $currentProgress / $totalItems",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = currentFileName.ifBlank { "等待文件夹授权" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            OutlinedButton(
                onClick = {
                    viewModel.cancelScan()
                    onCancel()
                },
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Text("取消扫描")
            }
        }
    }
}
