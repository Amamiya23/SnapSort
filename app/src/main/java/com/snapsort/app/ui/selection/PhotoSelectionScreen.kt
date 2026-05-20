package com.snapsort.app.ui.selection

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.snapsort.app.SnapSortDependencies
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PhotoSelectionScreen(
    groupId: String,
    viewModel: PhotoSelectionViewModel = viewModel(
        factory = PhotoSelectionViewModel.Factory(
            groupId = groupId,
            taskRepository = SnapSortDependencies.taskRepository(LocalContext.current),
            userSettingsRepository = SnapSortDependencies.userSettingsRepository(LocalContext.current)
        )
    ),
    onDone: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val group = state.currentGroup ?: return
    if (group.photos.isEmpty()) return

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { group.photos.size }
    )
    
    val coroutineScope = rememberCoroutineScope()
    var dragOffsetY by remember { mutableStateOf(0f) }
    val gestureThreshold = 120f

    val currentPhoto = group.photos.getOrNull(pagerState.currentPage)
    val isMarkedForDeletion = currentPhoto?.markedForDeletion ?: false

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "${group.title} ${pagerState.currentPage + 1}/${group.photos.size}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (currentPhoto != null) {
                            Text(
                                text = "${currentPhoto.timestamp} · RAW: ${if (currentPhoto.hasRaw) "有" else "无"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回工作台")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (pagerState.currentPage > 0) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        },
                        enabled = pagerState.currentPage > 0
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上一张")
                    }

                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Button(
                            onClick = {
                                currentPhoto?.id?.let { photoId ->
                                    if (isMarkedForDeletion) {
                                        viewModel.cancelDeleteMarker(photoId)
                                    } else {
                                        viewModel.markForDeletion(photoId)
                                        if (pagerState.currentPage < group.photos.size - 1) {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                            }
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isMarkedForDeletion) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.errorContainer,
                                contentColor = if (isMarkedForDeletion) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onErrorContainer
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.height(48.dp).fillMaxWidth(0.8f)
                        ) {
                            Icon(
                                imageVector = if (isMarkedForDeletion) Icons.Default.Check else Icons.Default.Delete,
                                contentDescription = if (isMarkedForDeletion) "取消标记" else "标记删除"
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(if (isMarkedForDeletion) "保留照片" else "标记删除")
                        }
                    }

                    IconButton(
                        onClick = {
                            if (pagerState.currentPage < group.photos.size - 1) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        enabled = pagerState.currentPage < group.photos.size - 1
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下一张")
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val photo = group.photos[page]
                val isDeleted = photo.markedForDeletion
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .pointerInput(photo.id, state.gestureShortcutEnabled) {
                            if (!state.gestureShortcutEnabled) return@pointerInput
                            detectVerticalDragGestures(
                                onDragStart = { dragOffsetY = 0f },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetY += dragAmount
                                },
                                onDragEnd = {
                                    val shouldTrigger = abs(dragOffsetY) >= gestureThreshold
                                    if (shouldTrigger) {
                                        if (dragOffsetY > 0f) {
                                            viewModel.markForDeletion(photo.id)
                                            if (pagerState.currentPage < group.photos.size - 1) {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                                }
                                            }
                                        } else {
                                            viewModel.cancelDeleteMarker(photo.id)
                                        }
                                    }
                                    dragOffsetY = 0f
                                },
                                onDragCancel = { dragOffsetY = 0f }
                            )
                        }
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = photo.uri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    val gestureText = when {
                        dragOffsetY > 24f -> "松手标记删除"
                        dragOffsetY < -24f -> "松手取消标记"
                        else -> null
                    }

                    if (gestureText != null && page == pagerState.currentPage) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 18.dp)
                                .alpha((abs(dragOffsetY) / gestureThreshold).coerceIn(0.35f, 1f)),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.inverseSurface
                        ) {
                            Text(
                                text = gestureText,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    if (isDeleted) {
                        // High-fidelity non-blocking delete overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Red.copy(alpha = 0.08f)) // very subtle red tint
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.error,
                                shadowElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onError,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "已标记删除",
                                        color = MaterialTheme.colorScheme.onError,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
