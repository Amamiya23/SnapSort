package com.snapsort.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.snapsort.app.core.PhotoGroupKind
import com.snapsort.app.data.db.PhotoEntity
import com.snapsort.app.data.db.PhotoGroupEntity
import com.snapsort.app.data.db.TaskEntity
import com.snapsort.app.data.repository.TaskRepository
import com.snapsort.app.ui.components.DeleteFilePreview
import com.snapsort.app.ui.copy.formatLocalTimeRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

data class PhotoGroup(
    val id: String,
    val type: GroupType,
    val count: Int,
    val markedForDeletionCount: Int,
    val timeRange: String,
    val coverPhotoUri: String?
)

enum class GroupType {
    BURST,
    SINGLE
}

sealed class HomeUiState {
    data object Loading : HomeUiState()

    data class Empty(
        val hasRecentTask: Boolean = false,
        val recentFolderName: String = ""
    ) : HomeUiState()

    data class Active(
        val folderName: String,
        val folderUri: String,
        val statusText: String,
        val groups: List<PhotoGroup>
    ) : HomeUiState() {
        val totalPhotoCount: Int = groups.sumOf { it.count }
        val markedForDeletionCount: Int = groups.sumOf { it.markedForDeletionCount }
    }
}

class HomeViewModel(
    private val taskRepository: TaskRepository
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(
        taskRepository.observeTask(),
        taskRepository.observeGroups(),
        taskRepository.observePhotos()
    ) { task, groups, photos ->
        if (task == null) {
            HomeUiState.Empty()
        } else {
            task.toActiveState(groups, photos)
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState.Loading
    )

    fun showActiveTask() = Unit

    fun toggleState() = Unit

    suspend fun getDeletePreviewFiles(): List<DeleteFilePreview> {
        return withContext(Dispatchers.Default) {
            taskRepository.getDeleteCandidates().flatMap { candidate ->
                buildList {
                    add(DeleteFilePreview(candidate.jpgFileName, candidate.jpgUri))
                    if (candidate.rawUri != null && candidate.rawFileName != null) {
                        add(DeleteFilePreview(candidate.rawFileName, candidate.rawUri))
                    }
                }
            }
        }
    }

    class Factory(
        private val taskRepository: TaskRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(taskRepository) as T
        }
    }
}

private fun TaskEntity.toActiveState(
    groups: List<PhotoGroupEntity>,
    photos: List<PhotoEntity>
): HomeUiState.Active {
    val photosByGroup = photos.groupBy { it.groupId }
    val uiGroups = groups.map { group ->
        val groupPhotos = photosByGroup[group.id].orEmpty()
        PhotoGroup(
            id = group.id,
            type = if (group.kind == PhotoGroupKind.BURST.name) GroupType.BURST else GroupType.SINGLE,
            count = groupPhotos.size,
            markedForDeletionCount = groupPhotos.count { it.markedForDeletion },
            timeRange = formatLocalTimeRange(group.startMillis, group.endMillis),
            coverPhotoUri = group.coverPhotoUri
        )
    }
    val markedCount = photos.count { it.markedForDeletion }
    return HomeUiState.Active(
        folderName = folderName,
        folderUri = folderUri,
        statusText = "${photos.size} 张照片，${groups.size} 组，$markedCount 张已标记",
        groups = uiGroups
    )
}
