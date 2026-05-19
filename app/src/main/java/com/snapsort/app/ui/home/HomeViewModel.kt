package com.snapsort.app.ui.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Mock Data Models
data class PhotoGroup(
    val id: String,
    val type: GroupType,
    val count: Int,
    val markedForDeletionCount: Int,
    val timeRange: String,
    val coverImageColor: Long // Mocking an image with a color hex
)

enum class GroupType {
    BURST, SINGLE
}

// UI State
sealed class HomeUiState {
    object Empty : HomeUiState()
    data class Active(
        val folderName: String,
        val statusText: String,
        val groups: List<PhotoGroup>
    ) : HomeUiState()
}

class HomeViewModel : ViewModel() {

    private val mockGroups = listOf(
        PhotoGroup(
            id = "1",
            type = GroupType.BURST,
            count = 15,
            markedForDeletionCount = 12,
            timeRange = "10:02 AM - 10:03 AM",
            coverImageColor = 0xFFEF5350 // Red-ish
        ),
        PhotoGroup(
            id = "2",
            type = GroupType.SINGLE,
            count = 1,
            markedForDeletionCount = 0,
            timeRange = "10:05 AM",
            coverImageColor = 0xFF42A5F5 // Blue-ish
        ),
        PhotoGroup(
            id = "3",
            type = GroupType.BURST,
            count = 8,
            markedForDeletionCount = 2,
            timeRange = "10:15 AM - 10:16 AM",
            coverImageColor = 0xFF66BB6A // Green-ish
        )
    )

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Empty)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun toggleState() {
        _uiState.update { currentState ->
            if (currentState is HomeUiState.Empty) {
                HomeUiState.Active(
                    folderName = "DCIM/100CANON",
                    statusText = "Screening in progress",
                    groups = mockGroups
                )
            } else {
                HomeUiState.Empty
            }
        }
    }
}
