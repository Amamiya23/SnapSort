package com.snapsort.app.ui.selection

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.snapsort.app.SnapSortDependencies

import kotlin.math.abs
import kotlinx.coroutines.launch

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PhotoSelectionScreen(
    groupId: String,
    viewModel: PhotoSelectionViewModel = viewModel(
        factory = PhotoSelectionViewModel.Factory(
            groupId = groupId,
            taskRepository = SnapSortDependencies.taskRepository(LocalContext.current),
            photoExifReader = SnapSortDependencies.photoExifReader(LocalContext.current),
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
    LaunchedEffect(currentPhoto?.id) {
        currentPhoto?.let(viewModel::refreshExposureIfMissing)
    }

    var showOverlay by remember { mutableStateOf(true) }
    var transformActive by remember { mutableStateOf(false) }
    val previewBackgroundColor = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(previewBackgroundColor)
    ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = !transformActive
            ) { page ->
                val photo = group.photos[page]
                val isDeleted = photo.markedForDeletion
                
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    com.snapsort.app.ui.components.ZoomableImage(
                        model = photo.uri,
                        modifier = Modifier.fillMaxSize(),
                        contentDescription = "当前照片 ${page + 1}/${group.photos.size}",
                        onTap = { showOverlay = !showOverlay },
                        onTransformActiveChange = { transformActive = it },
                        onSwipeProgress = { if (state.gestureShortcutEnabled) dragOffsetY = it },
                        onSwipeEnd = {
                            if (!state.gestureShortcutEnabled) return@ZoomableImage
                            val shouldTrigger = abs(it) >= gestureThreshold
                            if (shouldTrigger) {
                                if (it > 0f) {
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
                        }
                    )

                    if (page == pagerState.currentPage && abs(dragOffsetY) > 4f) {
                        val progress = (abs(dragOffsetY) / gestureThreshold).coerceIn(0f, 1f)
                        val isDownward = dragOffsetY > 0f

                        Box(modifier = Modifier.fillMaxSize()) {
                            // Colored wash at bottom edge, grows upward with progress
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(progress * 0.28f)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        if (isDownward) MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                    )
                            )

                            // Thin progress bar at bottom edge
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(progress)
                                        .background(
                                            if (isDownward) MaterialTheme.colorScheme.error
                                            else MaterialTheme.colorScheme.primary
                                        )
                                )
                            }

                            // Direction hint label near the progress bar
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 16.dp)
                                    .alpha(progress.coerceIn(0.4f, 1f)),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.inverseSurface
                            ) {
                                Text(
                                    text = if (isDownward) "↓ 松手标记删除" else "↑ 松手取消标记",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.inverseOnSurface,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }

                    if (isDeleted) {
                        MarkedForDeleteOverlay()
                    }
                }
            }

            if (showOverlay) {
                SelectionTopOverlay(
                    groupTitle = group.title,
                    currentIndex = pagerState.currentPage + 1,
                    totalCount = group.photos.size,
                    overlayVisible = showOverlay,
                    onBack = onDone,
                    onToggleOverlay = { showOverlay = !showOverlay }
                )

                SelectionDotBar(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = 58.dp),
                    photos = group.photos,
                    currentIndex = pagerState.currentPage,
                    onSelect = { index ->
                        if (index != pagerState.currentPage) {
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        }
                    }
                )

                currentPhoto?.let { photo ->
                    PhotoMetadataFloat(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = SelectionBottomBarHeight + 6.dp),
                        photo = photo
                    )
                }

                SelectionBottomBar(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    canGoPrevious = pagerState.currentPage > 0,
                    canGoNext = pagerState.currentPage < group.photos.size - 1,
                    isMarkedForDeletion = isMarkedForDeletion,
                    onPrevious = {
                        if (pagerState.currentPage > 0) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    },
                    onToggleDelete = {
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
                    onNext = {
                        if (pagerState.currentPage < group.photos.size - 1) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    }
                )
            }

            if (!showOverlay) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .height(56.dp)
                        .padding(end = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    OverlayVisibilityButton(
                        overlayVisible = showOverlay,
                        onClick = { showOverlay = !showOverlay }
                    )
                }
            }
    }
}

@Composable
private fun OverlayVisibilityButton(
    modifier: Modifier = Modifier,
    overlayVisible: Boolean,
    onClick: () -> Unit
) {
    val label = if (overlayVisible) "隐藏照片信息" else "显示照片信息"
    IconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp)
    ) {
        Icon(
            imageVector = if (overlayVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun SelectionTopOverlay(
    modifier: Modifier = Modifier,
    groupTitle: String,
    currentIndex: Int,
    totalCount: Int,
    overlayVisible: Boolean,
    onBack: () -> Unit,
    onToggleOverlay: () -> Unit
) {
    val scrimColor = MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(132.dp)
            .background(
                Brush.verticalGradient(
                    0f to scrimColor.copy(alpha = 0.96f),
                    0.48f to scrimColor.copy(alpha = 0.62f),
                    1f to scrimColor.copy(alpha = 0f)
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(56.dp)
                .padding(start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回工作台",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = "$groupTitle $currentIndex/$totalCount",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            OverlayVisibilityButton(
                overlayVisible = overlayVisible,
                onClick = onToggleOverlay
            )
        }
    }
}

@Composable
private fun SelectionDotBar(
    modifier: Modifier = Modifier,
    photos: List<PhotoSelectionItem>,
    currentIndex: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 88.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        photos.forEachIndexed { index, photo ->
            val isCurrent = index == currentIndex
            val dotWidth by animateDpAsState(
                targetValue = if (isCurrent) 22.dp else 6.dp,
                animationSpec = tween(durationMillis = 180),
                label = "selection-dot-width"
            )
            val dotColor = when {
                photo.markedForDeletion -> MaterialTheme.colorScheme.error
                isCurrent -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
            }
            val label = buildString {
                append("跳转到第 ${index + 1} 张")
                if (photo.markedForDeletion) append("，已标记删除")
                if (isCurrent) append("，当前照片")
            }
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(width = dotWidth, height = 28.dp)
                    .semantics {
                        contentDescription = label
                        role = Role.Button
                    }
                    .clickable(role = Role.Button) { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = dotWidth, height = 6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(dotColor)
                )
            }
        }
    }
}

@Composable
private fun PhotoMetadataFloat(
    modifier: Modifier = Modifier,
    photo: PhotoSelectionItem
) {
    val metadataShadow = Shadow(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.72f),
        offset = Offset(0f, 1.4f),
        blurRadius = 3f
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(0.92f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = photo.fileName,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium.merge(
                    TextStyle(
                        fontFamily = FontFamily.Serif,
                        shadow = metadataShadow
                    )
                ),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            photo.rawExtension?.let { extension ->
                Spacer(Modifier.width(8.dp))
                RawBadge(extension)
            }
        }

        if (photo.exposureLine.isNotEmpty()) {
            Text(
                text = photo.exposureLine,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall.merge(
                    TextStyle(
                        fontFamily = FontFamily.Serif,
                        shadow = metadataShadow
                    )
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RawBadge(extension: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        Text(
            text = extension.uppercase(),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun SelectionBottomBar(
    modifier: Modifier = Modifier,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    isMarkedForDeletion: Boolean,
    onPrevious: () -> Unit,
    onToggleDelete: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .height(SelectionBottomBarHeight)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SelectionBarAction(
                modifier = Modifier.weight(1f),
                label = "上一张",
                enabled = canGoPrevious,
                onClick = onPrevious
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
            SelectionBarAction(
                modifier = Modifier.weight(1f),
                label = if (isMarkedForDeletion) "恢复保留" else "标记删除",
                enabled = true,
                danger = true,
                active = isMarkedForDeletion,
                onClick = onToggleDelete
            ) {
                Icon(
                    imageVector = if (isMarkedForDeletion) Icons.Default.Check else Icons.Outlined.DeleteOutline,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
            SelectionBarAction(
                modifier = Modifier.weight(1f),
                label = "下一张",
                enabled = canGoNext,
                onClick = onNext
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun SelectionBarAction(
    modifier: Modifier = Modifier,
    label: String,
    enabled: Boolean,
    danger: Boolean = false,
    active: Boolean = false,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
        danger && active -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    val labelColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.34f)
        danger && active -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = label
                role = Role.Button
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            icon()
        }
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            color = labelColor,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MarkedForDeleteOverlay() {
    val danger = MaterialTheme.colorScheme.error
    Box(
        modifier = Modifier
            .fillMaxSize()
            .border(3.dp, danger, RoundedCornerShape(2.dp))
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(top = DeleteBadgeTopPadding, end = 12.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = danger,
            contentColor = MaterialTheme.colorScheme.onError,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    "已标记",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private val DeleteBadgeTopPadding = 88.dp
private val SelectionBottomBarHeight = 76.dp
