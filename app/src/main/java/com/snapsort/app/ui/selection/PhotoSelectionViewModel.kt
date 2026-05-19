package com.snapsort.app.ui.selection

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PhotoMock(
    val id: String,
    val timestamp: String,
    val isRaw: Boolean,
    val color: Long // Hex color for placeholder
)

data class PhotoGroupMock(
    val id: String,
    val title: String,
    val photos: List<PhotoMock>
)

data class PhotoSelectionState(
    val currentGroup: PhotoGroupMock? = null,
    val markedForDeletionIds: Set<String> = emptySet(),
    val currentIndex: Int = 0
)

class PhotoSelectionViewModel : ViewModel() {

    private val _state = MutableStateFlow(PhotoSelectionState())
    val state: StateFlow<PhotoSelectionState> = _state.asStateFlow()

    init {
        // Load mock data
        val mockPhotos = listOf(
            PhotoMock("1", "10:42 AM", true, 0xFFE57373),
            PhotoMock("2", "10:42 AM", true, 0xFF81C784),
            PhotoMock("3", "10:43 AM", false, 0xFF64B5F6),
            PhotoMock("4", "10:43 AM", false, 0xFFFFD54F),
            PhotoMock("5", "10:44 AM", true, 0xFFBA68C8)
        )
        val mockGroup = PhotoGroupMock("g1", "Burst", mockPhotos)
        _state.value = PhotoSelectionState(currentGroup = mockGroup)
    }

    fun toggleDeleteMarker(photoId: String) {
        _state.update { currentState ->
            val newMarkers = currentState.markedForDeletionIds.toMutableSet()
            if (newMarkers.contains(photoId)) {
                newMarkers.remove(photoId)
            } else {
                newMarkers.add(photoId)
            }
            currentState.copy(markedForDeletionIds = newMarkers)
        }
    }

    fun setPage(index: Int) {
        _state.update { it.copy(currentIndex = index) }
    }
}