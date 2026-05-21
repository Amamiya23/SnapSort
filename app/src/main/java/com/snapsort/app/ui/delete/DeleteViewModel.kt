package com.snapsort.app.ui.delete

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.snapsort.app.data.repository.DeleteCandidate
import com.snapsort.app.data.repository.DeleteCalibrationResult
import com.snapsort.app.data.repository.DeleteFailure
import com.snapsort.app.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeleteAuthorizationState(
    val pendingIntent: PendingIntent? = null,
    val errorMessage: String? = null,
    val result: DeleteCalibrationResult? = null,
    val deleteMessage: String? = null
)

class DeleteViewModel(
    private val context: Context,
    private val contentResolver: ContentResolver,
    private val taskRepository: TaskRepository
) : ViewModel() {
    private val _authorizationState = MutableStateFlow(DeleteAuthorizationState())
    val authorizationState: StateFlow<DeleteAuthorizationState> = _authorizationState.asStateFlow()

    private var pendingCandidates: List<DeleteCandidate> = emptyList()

    fun prepareDeleteRequest() {
        viewModelScope.launch {
            _authorizationState.value = DeleteAuthorizationState()
            pendingCandidates = taskRepository.getDeleteCandidates()
            val uris = pendingCandidates.flatMap { candidate ->
                buildList {
                    add(Uri.parse(candidate.jpgUri))
                    if (candidate.rawUri != null) add(Uri.parse(candidate.rawUri))
                }
            }
            if (uris.isEmpty()) {
                _authorizationState.value = DeleteAuthorizationState(errorMessage = "没有待删除照片")
                return@launch
            }
            _authorizationState.value = try {
                DeleteAuthorizationState(
                    pendingIntent = MediaStore.createDeleteRequest(contentResolver, uris)
                )
            } catch (exception: Exception) {
                Log.w(TAG, "MediaStore delete request failed, falling back to SAF delete", exception)
                deleteWithSafFallback()
                return@launch
            }
        }
    }

    fun onDeleteAccepted() {
        viewModelScope.launch {
            calibrateAfterDelete()
        }
    }

    fun onDeleteRejected() {
        _authorizationState.value = DeleteAuthorizationState(
            deleteMessage = "已取消删除"
        )
    }

    fun cancelRemainingMarks() {
        viewModelScope.launch {
            taskRepository.clearDeleteMarks(pendingCandidates.map { it.jpgUri })
        }
    }

    fun clearDeleteMessage() {
        _authorizationState.value = _authorizationState.value.copy(deleteMessage = null)
    }

    private fun uriCanOpen(uri: String): Boolean {
        return try {
            contentResolver.openInputStream(Uri.parse(uri))?.close()
            true
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun deleteWithSafFallback() {
        val deletedJpgUris = mutableListOf<String>()
        val failures = mutableListOf<DeleteFailure>()
        var successCount = 0

        pendingCandidates.forEach { candidate ->
            if (deleteDocumentUri(candidate.jpgUri)) {
                deletedJpgUris.add(candidate.jpgUri)
                successCount += 1
            } else {
                failures.add(DeleteFailure(candidate.jpgFileName, "无法通过已授权文件夹删除 JPG"))
            }

            val rawUri = candidate.rawUri
            if (rawUri != null) {
                if (deleteDocumentUri(rawUri)) {
                    successCount += 1
                } else {
                    failures.add(DeleteFailure(candidate.rawFileName ?: rawUri, "无法通过已授权文件夹删除 RAW"))
                }
            }
        }

        taskRepository.removeDeletedPhotos(deletedJpgUris)
        _authorizationState.value = DeleteAuthorizationState(
            deleteMessage = if (failures.isEmpty()) {
                "已删除 $successCount 个文件"
            } else {
                "已删除 $successCount 个文件，${failures.size} 个失败"
            }
        )
    }

    private suspend fun calibrateAfterDelete() {
        val deletedJpgUris = mutableListOf<String>()
        val failures = mutableListOf<DeleteFailure>()
        var successCount = 0

        pendingCandidates.forEach { candidate ->
            val jpgStillExists = uriCanOpen(candidate.jpgUri)
            val rawStillExists = candidate.rawUri?.let { uriCanOpen(it) } ?: false

            if (!jpgStillExists) {
                deletedJpgUris.add(candidate.jpgUri)
                successCount += 1
            } else {
                failures.add(DeleteFailure(candidate.jpgFileName, "系统删除后文件仍可访问"))
            }

            if (candidate.rawUri != null) {
                if (!rawStillExists) {
                    successCount += 1
                } else {
                    failures.add(DeleteFailure(candidate.rawFileName ?: candidate.rawUri, "系统删除后文件仍可访问"))
                }
            }
        }

        taskRepository.removeDeletedPhotos(deletedJpgUris)
        _authorizationState.value = DeleteAuthorizationState(
            deleteMessage = if (failures.isEmpty()) {
                "已删除 $successCount 个文件"
            } else {
                "已删除 $successCount 个文件，${failures.size} 个失败"
            }
        )
    }

    private fun deleteDocumentUri(uri: String): Boolean {
        return try {
            val document = DocumentFile.fromSingleUri(context, Uri.parse(uri))
            val deleted = document?.delete() == true
            Log.d(TAG, "SAF delete uri=$uri result=$deleted")
            deleted
        } catch (exception: Exception) {
            Log.w(TAG, "SAF delete failed uri=$uri", exception)
            false
        }
    }

    class Factory(
        private val context: Context,
        private val contentResolver: ContentResolver,
        private val taskRepository: TaskRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DeleteViewModel(context.applicationContext, contentResolver, taskRepository) as T
        }
    }

    companion object {
        private const val TAG = "SnapSortDelete"
    }
}
