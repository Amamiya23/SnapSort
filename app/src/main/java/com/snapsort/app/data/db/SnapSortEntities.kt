package com.snapsort.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String = RECENT_TASK_ID,
    val folderUri: String,
    val folderName: String,
    val scannedAtMillis: Long,
    val burstThresholdMillis: Long,
    val sortDirection: String
)

@Entity(
    tableName = "photo_groups",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId")]
)
data class PhotoGroupEntity(
    @PrimaryKey val id: String,
    val taskId: String = RECENT_TASK_ID,
    val kind: String,
    val position: Int,
    val startMillis: Long,
    val endMillis: Long,
    val coverPhotoUri: String
)

@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PhotoGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId"), Index("groupId")]
)
data class PhotoEntity(
    @PrimaryKey val jpgUri: String,
    val taskId: String = RECENT_TASK_ID,
    val groupId: String,
    val position: Int,
    val fileName: String,
    val baseName: String,
    val extension: String,
    val capturedAtMillis: Long,
    val captureTimeSource: String,
    val modifiedAtMillis: Long,
    val rawUri: String?,
    val rawFileName: String?,
    val rawExtension: String?,
    val markedForDeletion: Boolean
)

const val RECENT_TASK_ID = "recent"
