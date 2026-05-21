package com.snapsort.app.ui.transition

import androidx.compose.ui.geometry.Rect

data class PhotoOpenTransitionSpec(
    val imageUri: String,
    val startBounds: Rect
)
