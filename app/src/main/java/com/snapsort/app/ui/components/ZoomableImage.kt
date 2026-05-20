package com.snapsort.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import coil.compose.AsyncImage
import kotlin.math.abs

@Composable
fun ZoomableImage(
    model: Any?,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {},
    onSwipeProgress: (Float) -> Unit = {},
    onSwipeEnd: (Float) -> Unit = {}
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { imageSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { tapOffset ->
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                            val maxX = (imageSize.width * (2.5f - 1)) / 2f
                            val maxY = (imageSize.height * (2.5f - 1)) / 2f
                            val x = (imageSize.width / 2f - tapOffset.x) * 2.5f
                            val y = (imageSize.height / 2f - tapOffset.y) * 2.5f
                            offset = Offset(x.coerceIn(-maxX, maxX), y.coerceIn(-maxY, maxY))
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    var dragOffsetY = 0f
                    do {
                        val event = awaitPointerEvent()
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        
                        val newScale = (scale * zoom).coerceIn(1f, 5f)
                        
                        if (newScale > 1f) {
                            val maxX = (size.width * (newScale - 1)) / 2f
                            val maxY = (size.height * (newScale - 1)) / 2f
                            val newOffsetX = (offset.x + pan.x).coerceIn(-maxX, maxX)
                            val newOffsetY = (offset.y + pan.y).coerceIn(-maxY, maxY)
                            offset = Offset(newOffsetX, newOffsetY)
                            
                            // Consume to prevent pager swipe
                            event.changes.forEach { it.consume() }
                        } else {
                            offset = Offset.Zero
                            
                            // If scale is 1f, calculate pure vertical swipe if horizontal pan is minimal
                            if (event.changes.size == 1) {
                                dragOffsetY += pan.y
                                if (abs(dragOffsetY) > abs(pan.x) && abs(dragOffsetY) > 10f) {
                                    onSwipeProgress(dragOffsetY)
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                        
                        scale = newScale
                    } while (event.changes.any { it.pressed })
                    
                    if (scale == 1f) {
                        onSwipeEnd(dragOffsetY)
                    }
                }
            }
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )
    }
}
