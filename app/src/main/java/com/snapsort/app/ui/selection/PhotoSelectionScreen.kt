package com.snapsort.app.ui.selection

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
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
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.lifecycle.viewmodel.compose.viewModel
import com.snapsort.app.SnapSortDependencies
import com.snapsort.app.ui.transition.PhotoOpenTransitionSpec

import kotlin.math.abs
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PhotoSelectionScreen(
    groupId: String,
    openTransitionSpec: PhotoOpenTransitionSpec? = null,
    onOpenTransitionFinished: () -> Unit = {},
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

    var showOverlay by remember { mutableStateOf(true) }
    var transformActive by remember { mutableStateOf(false) }
    var openingTransitionActive by remember(openTransitionSpec) { mutableStateOf(openTransitionSpec != null) }
    var controlsVisible by remember(openTransitionSpec) { mutableStateOf(openTransitionSpec == null) }
    val controlsAlpha by animateFloatAsState(
        targetValue = if (controlsVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 140, delayMillis = if (controlsVisible) 40 else 0),
        label = "selection-controls-alpha"
    )
    val previewBackgroundColor = MaterialTheme.colorScheme.surface
    val overlayColor = previewBackgroundColor.copy(alpha = 0.88f)
    val overlayContentColor = MaterialTheme.colorScheme.onSurface

    Scaffold(
        containerColor = previewBackgroundColor,
        topBar = {
            if (showOverlay) {
            TopAppBar(
                modifier = Modifier.alpha(controlsAlpha),
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
                    containerColor = overlayColor,
                    titleContentColor = overlayContentColor,
                    navigationIconContentColor = overlayContentColor,
                    actionIconContentColor = overlayContentColor
                )
            )
            }
        },
        bottomBar = {
            if (showOverlay) {
            BottomAppBar(
                modifier = Modifier.alpha(controlsAlpha),
                containerColor = overlayColor
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                if (openingTransitionActive) return@FilledTonalIconButton
                                if (pagerState.currentPage > 0) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                }
                            },
                            enabled = controlsVisible && pagerState.currentPage > 0,
                            modifier = Modifier.size(44.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                contentColor = overlayContentColor,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "上一张",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                if (openingTransitionActive) return@Button
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
                            border = BorderStroke(
                                1.dp,
                                if (isMarkedForDeletion) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.height(48.dp),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp)
                        ) {
                            Icon(
                                imageVector = if (isMarkedForDeletion) Icons.Default.Check else Icons.Outlined.DeleteOutline,
                                contentDescription = if (isMarkedForDeletion) "取消标记" else "标记删除",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(if (isMarkedForDeletion) "保留照片" else "标记删除")
                        }
                    }

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                if (openingTransitionActive) return@FilledTonalIconButton
                                if (pagerState.currentPage < group.photos.size - 1) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                }
                            },
                            enabled = controlsVisible && pagerState.currentPage < group.photos.size - 1,
                            modifier = Modifier.size(44.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                contentColor = overlayContentColor,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "下一张",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
            }
        }
    ) { contentPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .consumeWindowInsets(contentPadding)
            .background(previewBackgroundColor)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = !transformActive && !openingTransitionActive
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
                        onTap = { if (!openingTransitionActive) showOverlay = !showOverlay },
                        onTransformActiveChange = { if (!openingTransitionActive) transformActive = it },
                        onSwipeProgress = { if (!openingTransitionActive && state.gestureShortcutEnabled) dragOffsetY = it },
                        onSwipeEnd = {
                            if (openingTransitionActive) return@ZoomableImage
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
                        // High-fidelity non-blocking delete overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.18f))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .windowInsetsPadding(WindowInsets.systemBars)
                                .padding(top = 64.dp, end = 16.dp),
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
                                        contentDescription = "已标记删除",
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

            if (openingTransitionActive) {
                openTransitionSpec?.let { transitionSpec ->
                    PhotoOpenTransitionOverlay(
                        transitionSpec = transitionSpec,
                        onAnimationFinished = {
                            openingTransitionActive = false
                            controlsVisible = true
                            onOpenTransitionFinished()
                        }
                    )
                }
                InputBlocker()
            }
        }
    }
}

@Composable
private fun PhotoOpenTransitionOverlay(
    transitionSpec: PhotoOpenTransitionSpec,
    onAnimationFinished: () -> Unit
) {
    val progress = remember(transitionSpec) { Animatable(0f) }
    var rootOffset by remember { mutableStateOf(Offset.Zero) }
    val transitionDurationMillis = if (ValueAnimator.areAnimatorsEnabled()) 260 else 0
    val density = LocalDensity.current

    LaunchedEffect(transitionSpec) {
        progress.snapTo(0f)
        if (transitionDurationMillis == 0) {
            progress.snapTo(1f)
        } else {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = transitionDurationMillis,
                    easing = OpenPhotoEasing
                )
            )
        }
        onAnimationFinished()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootOffset = it.positionInWindow() }
    ) {
        val startBounds = transitionSpec.startBounds.translate(-rootOffset.x, -rootOffset.y)
        val endBounds = Rect(
            left = 0f,
            top = 0f,
            right = constraints.maxWidth.toFloat(),
            bottom = constraints.maxHeight.toFloat()
        )
        val animatedBounds = lerp(startBounds, endBounds, progress.value)
        val fitAlpha = progress.value.coerceIn(0f, 1f)
        val cropAlpha = (1f - progress.value * 1.35f).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationX = animatedBounds.left
                    translationY = animatedBounds.top
                }
                .size(
                    width = with(density) { animatedBounds.width.toDp() },
                    height = with(density) { animatedBounds.height.toDp() }
                )
        ) {
            AsyncImage(
                model = transitionSpec.imageUri,
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(cropAlpha),
                contentScale = ContentScale.Crop
            )
            AsyncImage(
                model = transitionSpec.imageUri,
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(fitAlpha),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun InputBlocker() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                }
            }
    )
}

private val OpenPhotoEasing = Easing { fraction ->
    1f - (1f - fraction) * (1f - fraction) * (1f - fraction) * (1f - fraction)
}

private fun lerp(start: Rect, stop: Rect, fraction: Float): Rect {
    return Rect(
        left = lerp(start.left, stop.left, fraction),
        top = lerp(start.top, stop.top, fraction),
        right = lerp(start.right, stop.right, fraction),
        bottom = lerp(start.bottom, stop.bottom, fraction)
    )
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}
