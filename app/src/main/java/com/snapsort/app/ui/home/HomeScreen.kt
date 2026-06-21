package com.snapsort.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.snapsort.app.SnapSortDependencies
import com.snapsort.app.ui.components.DeleteConfirmationSheet
import com.snapsort.app.ui.components.DeleteFilePreview
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSelectFolder: () -> Unit,
    onRescan: (String) -> Unit,
    onOpenGroup: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    deleteMessage: String? = null,
    onDeleteMessageShown: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(
            SnapSortDependencies.taskRepository(LocalContext.current)
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showDeleteSheet by remember { mutableStateOf(false) }
    var deletePreviewFiles by remember { mutableStateOf<List<DeleteFilePreview>>(emptyList()) }
    val activeState = uiState as? HomeUiState.Active

    LaunchedEffect(deleteMessage) {
        if (deleteMessage != null) {
            snackbarHostState.showSnackbar(deleteMessage)
            onDeleteMessageShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            when (uiState) {
                is HomeUiState.Loading -> {
                    TopAppBar(title = { Text("SnapSort") })
                }
                is HomeUiState.Empty -> {
                    TopAppBar(
                        title = { Text("SnapSort") },
                        actions = {
                            IconButton(onClick = onOpenSettings) {
                                Icon(imageVector = Icons.Default.Settings, contentDescription = "设置")
                            }
                        }
                    )
                }
                is HomeUiState.Active -> {
                    val topBarState = uiState as HomeUiState.Active
                    TopAppBar(
                        title = {
                            Column {
                                Text(text = topBarState.folderName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = topBarState.statusText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { onRescan(topBarState.folderUri) }) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "重新扫描")
                            }
                            IconButton(onClick = onSelectFolder) {
                                Icon(imageVector = Icons.Default.FolderOpen, contentDescription = "选择其他文件夹")
                            }
                            IconButton(onClick = onOpenSettings) {
                                Icon(imageVector = Icons.Default.Settings, contentDescription = "设置")
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is HomeUiState.Loading -> { /* 等待数据，不显示内容 */ }
                is HomeUiState.Empty -> EmptyStateContent(
                    state = state,
                    onSelectFolder = onSelectFolder,
                    onContinueTask = viewModel::showActiveTask
                )
                is HomeUiState.Active -> ActiveStateContent(
                    state = state,
                    onOpenGroup = onOpenGroup,
                    onFinish = {
                        if (state.markedForDeletionCount > 0) {
                            coroutineScope.launch {
                                deletePreviewFiles = viewModel.getDeletePreviewFiles()
                                showDeleteSheet = true
                            }
                        }
                    }
                )
            }
        }
    }

    if (showDeleteSheet && activeState != null) {
        DeleteConfirmationSheet(
            jpgCount = activeState.markedForDeletionCount,
            rawCount = (deletePreviewFiles.size - activeState.markedForDeletionCount).coerceAtLeast(0),
            files = deletePreviewFiles,
            onConfirm = {
                showDeleteSheet = false
                onDeleteConfirmed()
            },
            onDismiss = { showDeleteSheet = false }
        )
    }
}

@Composable
fun EmptyStateContent(
    state: HomeUiState.Empty,
    onSelectFolder: () -> Unit,
    onContinueTask: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.FolderOpen,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "选择相机导出文件夹开始筛选",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "支持 JPG/JPEG 预览，并在删除前显示同名 RAW 的影响范围。",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onSelectFolder,
            modifier = Modifier.fillMaxWidth(0.8f).height(56.dp)
        ) {
            Text("选择照片文件夹")
        }
        if (state.hasRecentTask) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onContinueTask,
                modifier = Modifier.fillMaxWidth(0.8f).height(56.dp)
            ) {
                Text("继续上次筛选")
            }
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(onClick = onSelectFolder) {
                Text(
                    text = "重新扫描 ${state.recentFolderName}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ActiveStateContent(
    state: HomeUiState.Active,
    onOpenGroup: (String) -> Unit,
    onFinish: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TaskSummaryCard(state = state, onFinish = onFinish)
        }
        items(state.groups, key = { it.id }) { group ->
            GroupItemCard(group = group, onClick = {
                onOpenGroup(group.id)
            })
        }
    }
}

@Composable
fun TaskSummaryCard(
    state: HomeUiState.Active,
    onFinish: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "当前任务",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                if (state.markedForDeletionCount > 0) {
                    Text(
                        text = "已标记 ${state.markedForDeletionCount} 张待删除",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.errorContainer,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "${state.totalPhotoCount} 张照片",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "共 ${state.groups.size} 组",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onFinish,
                    enabled = state.markedForDeletionCount > 0,
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("完成")
                }
            }
        }
    }
}

@Composable
fun GroupItemCard(
    group: PhotoGroup,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = group.coverPhotoUri,
                contentDescription = "${if (group.type == GroupType.BURST) "连拍" else "散片"}分组封面",
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${group.count} 张",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (group.type == GroupType.BURST) "连拍" else "散片",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = group.timeRange,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                if (group.markedForDeletionCount > 0) {
                    Text(
                        text = "${group.markedForDeletionCount} 张已标记删除",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
