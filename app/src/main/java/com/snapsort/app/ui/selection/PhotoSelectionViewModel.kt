package com.snapsort.app.ui.selection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.snapsort.app.data.db.PhotoEntity
import com.snapsort.app.data.repository.TaskRepository
import com.snapsort.app.data.settings.UserSettingsRepository
import com.snapsort.app.ui.copy.formatLocalPhotoTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PhotoSelectionItem(
    val id: String,
    val uri: String,
    val timestamp: String,
    val hasRaw: Boolean,
    val markedForDeletion: Boolean
)

data class PhotoSelectionGroup(
    val id: String,
    val title: String,
    val photos: List<PhotoSelectionItem>
)

data class PhotoSelectionState(
    val currentGroup: PhotoSelectionGroup? = null,
    val currentIndex: Int = 0,
    val gestureShortcutEnabled: Boolean = true
)

class PhotoSelectionViewModel(
    private val groupId: String,
    private val taskRepository: TaskRepository,
    userSettingsRepository: UserSettingsRepository
) : ViewModel() {
    val state: StateFlow<PhotoSelectionState> = combine(
        taskRepository.observePhotosForGroup(groupId),
        userSettingsRepository.settings
    ) { photos, settings ->
        PhotoSelectionState(
            currentGroup = PhotoSelectionGroup(
                id = groupId,
                title = if (groupId.startsWith("burst")) "连拍" else "散片",
                photos = photos.map { it.toUiPhoto() }
            ),
            gestureShortcutEnabled = settings.gestureShortcutEnabled
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PhotoSelectionState()
    )

    fun markForDeletion(photoId: String) {
        viewModelScope.launch { taskRepository.setMarkedForDeletion(photoId, true) }
    }

    fun cancelDeleteMarker(photoId: String) {
        viewModelScope.launch { taskRepository.setMarkedForDeletion(photoId, false) }
    }

    class Factory(
        private val groupId: String,
        private val taskRepository: TaskRepository,
        private val userSettingsRepository: UserSettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PhotoSelectionViewModel(groupId, taskRepository, userSettingsRepository) as T
        }
    }
}

private fun PhotoEntity.toUiPhoto(): PhotoSelectionItem {
    return PhotoSelectionItem(
        id = jpgUri,
        uri = jpgUri,
        timestamp = formatLocalPhotoTime(capturedAtMillis),
        hasRaw = rawUri != null,
        markedForDeletion = markedForDeletion
    )
}
