package com.snapsort.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Size
import kotlin.math.abs
import kotlinx.coroutines.launch

@Composable
fun ZoomableImage(
    model: Any?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    onTap: () -> Unit = {},
    onTransformActiveChange: (Boolean) -> Unit = {},
    onSwipeProgress: (Float) -> Unit = {},
    onSwipeEnd: (Float) -> Unit = {}
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    val swipeTranslation = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { imageSize = it }
            .pointerInput(Unit) {
                awaitEachGesture {
                    var lastTapTime = 0L
                    var lastTapPosition = Offset.Zero

                    while (true) {
                        val down = awaitFirstDown()

                        // Check double-tap before consuming
                        val now = System.nanoTime()
                        val isDoubleTap = lastTapTime != 0L &&
                                (now - lastTapTime) / 1_000_000 < 300 &&
                                (down.position - lastTapPosition).getDistance() < 50f

                        // Consume down to prevent pager from seeing it
                        down.consume()

                        if (isDoubleTap) {
                            // Double-tap: restore to original size only
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                                onTransformActiveChange(false)
                            }
                            lastTapTime = 0L
                            // Drain remaining events in this gesture
                            do {
                                val event = awaitPointerEvent()
                            } while (event.changes.any { it.pressed })
                            continue
                        }

                        // Wait for up, checking for drag
                        var dragOffsetY = 0f
                        var moved = false
                        do {
                            val event = awaitPointerEvent()
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            val isTransformGesture = event.changes.size > 1

                            val newScale = (scale * zoom).coerceIn(1f, 5f)

                            if (isTransformGesture || newScale > 1f) {
                                moved = true
                                onTransformActiveChange(true)
                                val maxX = (size.width * (newScale - 1)) / 2f
                                val maxY = (size.height * (newScale - 1)) / 2f
                                val newOffsetX = (offset.x + pan.x).coerceIn(-maxX, maxX)
                                val newOffsetY = (offset.y + pan.y).coerceIn(-maxY, maxY)
                                offset = Offset(newOffsetX, newOffsetY)
                                event.changes.forEach { it.consume() }
                            } else {
                                offset = Offset.Zero
                                if (event.changes.size == 1) {
                                    dragOffsetY += pan.y
                                    coroutineScope.launch { swipeTranslation.snapTo(dragOffsetY * 0.3f) }
                                    if (abs(dragOffsetY) > abs(pan.x) && abs(dragOffsetY) > 10f) {
                                        moved = true
                                        onSwipeProgress(dragOffsetY)
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            }

                            scale = newScale
                        } while (event.changes.any { it.pressed })

                        if (!moved) {
                            // Clean tap (no drag): record for double-tap detection
                            lastTapTime = now
                            lastTapPosition = down.position
                            onTap()
                        } else if (scale == 1f) {
                            onTransformActiveChange(false)
                            onSwipeEnd(dragOffsetY)
                        } else {
                            onTransformActiveChange(true)
                        }
                        coroutineScope.launch {
                            swipeTranslation.animateTo(0f, animationSpec = spring(stiffness = 800f))
                        }
                    }
                }
            }
    ) {
        val context = LocalContext.current
        val request = remember(model) {
            ImageRequest.Builder(context)
                .data(model)
                .size(Size.ORIGINAL)
                .allowHardware(false)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .build()
        }
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y + swipeTranslation.value
                )
        )
    }
}
