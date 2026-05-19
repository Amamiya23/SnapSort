package com.snapsort.app.ui.selection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PhotoSelectionScreen(
    viewModel: PhotoSelectionViewModel = viewModel(),
    onDone: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val group = state.currentGroup ?: return

    val pagerState = rememberPagerState(
        initialPage = state.currentIndex,
        pageCount = { group.photos.size }
    )
    
    val coroutineScope = rememberCoroutineScope()

    // Sync pager state with view model
    LaunchedEffect(pagerState.currentPage) {
        viewModel.setPage(pagerState.currentPage)
    }
    LaunchedEffect(state.currentIndex) {
        if (state.currentIndex != pagerState.currentPage) {
            pagerState.animateScrollToPage(state.currentIndex)
        }
    }

    val currentPhoto = group.photos.getOrNull(pagerState.currentPage)
    val isMarkedForDeletion = currentPhoto?.id?.let { state.markedForDeletionIds.contains(it) } ?: false

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
                                text = "${currentPhoto.timestamp} • RAW: ${if (currentPhoto.isRaw) "Yes" else "No"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous")
                    }

                    Button(
                        onClick = {
                            currentPhoto?.id?.let { viewModel.toggleDeleteMarker(it) }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isMarkedForDeletion) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                            contentColor = if (isMarkedForDeletion) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = if (isMarkedForDeletion) Icons.Default.Check else Icons.Default.Delete,
                            contentDescription = if (isMarkedForDeletion) "Keep" else "Mark Delete"
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (isMarkedForDeletion) "Marked (Undo)" else "Delete")
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
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next")
                    }

                    TextButton(onClick = onDone) {
                        Text("Done")
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
                val isDeleted = state.markedForDeletionIds.contains(photo.id)
                
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .background(Color(photo.color)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDeleted) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Deleted",
                                    tint = Color.Red,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Marked for Deletion",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Photo ${photo.id}",
                            color = Color.White,
                            style = MaterialTheme.typography.displayMedium
                        )
                    }
                }
            }
        }
    }
}