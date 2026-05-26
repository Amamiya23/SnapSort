package com.snapsort.app.data.repository

import com.snapsort.app.core.PhotoGroup
import com.snapsort.app.core.ScannedPhoto
import com.snapsort.app.core.SortDirection
import com.snapsort.app.data.db.PhotoEntity
import com.snapsort.app.data.db.PhotoGroupEntity
import com.snapsort.app.data.db.RECENT_TASK_ID
import com.snapsort.app.data.db.SnapSortDao
import com.snapsort.app.data.db.TaskEntity
import kotlinx.coroutines.flow.Flow

data class DeleteCandidate(
    val jpgUri: String,
    val jpgFileName: String,
    val rawUri: String?,
    val rawFileName: String?
)

data class DeleteCalibrationResult(
    val successCount: Int,
    val failures: List<DeleteFailure>
)

data class DeleteFailure(
    val fileName: String,
    val reason: String
)

class TaskRepository(
    private val dao: SnapSortDao
) {
    fun observeTask(): Flow<TaskEntity?> = dao.observeTask()

    fun observeGroups(): Flow<List<PhotoGroupEntity>> = dao.observeGroups()

    fun observePhotos(): Flow<List<PhotoEntity>> = dao.observePhotos()

    fun observePhotosForGroup(groupId: String): Flow<List<PhotoEntity>> {
        return dao.observePhotosForGroup(groupId)
    }

    suspend fun saveRecentTask(
        folderUri: String,
        folderName: String,
        scannedAtMillis: Long,
        burstThresholdMillis: Long,
        sortDirection: SortDirection,
        groups: List<PhotoGroup>
    ) {
        val task = TaskEntity(
            folderUri = folderUri,
            folderName = folderName,
            scannedAtMillis = scannedAtMillis,
            burstThresholdMillis = burstThresholdMillis,
            sortDirection = sortDirection.name
        )
        val groupEntities = groups.mapIndexed { index, group ->
            PhotoGroupEntity(
                id = group.id,
                kind = group.kind.name,
                position = index,
                startMillis = group.startMillis,
                endMillis = group.endMillis,
                coverPhotoUri = group.photos.first().jpgUri
            )
        }
        val previousMarks = dao.getMarkedPhotoUris().toSet()
        val photoEntities = groups.flatMap { group ->
            group.photos.mapIndexed { index, photo ->
                photo.toEntity(
                    groupId = group.id,
                    position = index,
                    markedForDeletion = photo.jpgUri in previousMarks
                )
            }
        }

        dao.replaceRecentTask(task, groupEntities, photoEntities)
    }

    suspend fun setMarkedForDeletion(jpgUri: String, marked: Boolean) {
        dao.setMarkedForDeletion(jpgUri, marked)
    }

    suspend fun getDeleteCandidates(): List<DeleteCandidate> {
        return dao.getMarkedPhotos().map { photo ->
            DeleteCandidate(
                jpgUri = photo.jpgUri,
                jpgFileName = photo.fileName,
                rawUri = photo.rawUri,
                rawFileName = photo.rawFileName
            )
        }
    }

    suspend fun clearRecentTask() {
        dao.deleteTask(RECENT_TASK_ID)
    }

    suspend fun clearDeleteMarks(jpgUris: List<String>) {
        if (jpgUris.isNotEmpty()) {
            dao.clearDeleteMarks(jpgUris)
        }
    }

    suspend fun removeDeletedPhotos(jpgUris: List<String>) {
        if (jpgUris.isNotEmpty()) {
            val affectedGroupIds = dao.getGroupIdsForPhotos(jpgUris)
            dao.deletePhotos(jpgUris)
            for (groupId in affectedGroupIds) {
                val newCoverUri = dao.getFirstPhotoUri(groupId)
                if (newCoverUri != null) {
                    dao.updateGroupCoverPhoto(groupId, newCoverUri)
                }
            }
            dao.deleteEmptyGroups()
            if (dao.getPhotoCount() == 0) {
                dao.deleteTask()
            }
        }
    }
}

private fun ScannedPhoto.toEntity(
    groupId: String,
    position: Int,
    markedForDeletion: Boolean
): PhotoEntity {
    return PhotoEntity(
        jpgUri = jpgUri,
        groupId = groupId,
        position = position,
        fileName = fileName,
        baseName = baseName,
        extension = extension,
        capturedAtMillis = capturedAtMillis,
        captureTimeSource = captureTimeSource.name,
        modifiedAtMillis = modifiedAtMillis,
        rawUri = rawMatch?.uri,
        rawFileName = rawMatch?.fileName,
        rawExtension = rawMatch?.extension,
        markedForDeletion = markedForDeletion
    )
}
