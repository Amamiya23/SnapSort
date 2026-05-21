package com.snapsort.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SnapSortDao {
    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    fun observeTask(taskId: String = RECENT_TASK_ID): Flow<TaskEntity?>

    @Query("SELECT * FROM photo_groups WHERE taskId = :taskId ORDER BY position ASC")
    fun observeGroups(taskId: String = RECENT_TASK_ID): Flow<List<PhotoGroupEntity>>

    @Query("SELECT * FROM photos WHERE taskId = :taskId ORDER BY groupId ASC, position ASC")
    fun observePhotos(taskId: String = RECENT_TASK_ID): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE groupId = :groupId ORDER BY position ASC")
    fun observePhotosForGroup(groupId: String): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE markedForDeletion = 1 ORDER BY groupId ASC, position ASC")
    suspend fun getMarkedPhotos(): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE taskId = :taskId")
    suspend fun getPhotos(taskId: String = RECENT_TASK_ID): List<PhotoEntity>

    @Query("SELECT COUNT(*) FROM photos WHERE taskId = :taskId")
    suspend fun getPhotoCount(taskId: String = RECENT_TASK_ID): Int

    @Query("UPDATE photos SET markedForDeletion = :marked WHERE jpgUri = :jpgUri")
    suspend fun setMarkedForDeletion(jpgUri: String, marked: Boolean)

    @Query("DELETE FROM photos WHERE jpgUri IN (:jpgUris)")
    suspend fun deletePhotos(jpgUris: List<String>)

    @Query(
        """
        DELETE FROM photo_groups
        WHERE id NOT IN (SELECT DISTINCT groupId FROM photos)
        """
    )
    suspend fun deleteEmptyGroups()

    @Query("SELECT jpgUri FROM photos WHERE groupId = :groupId ORDER BY position ASC LIMIT 1")
    suspend fun getFirstPhotoUri(groupId: String): String?

    @Query("SELECT DISTINCT groupId FROM photos WHERE jpgUri IN (:jpgUris)")
    suspend fun getGroupIdsForPhotos(jpgUris: List<String>): List<String>

    @Query("UPDATE photo_groups SET coverPhotoUri = :coverUri WHERE id = :groupId")
    suspend fun updateGroupCoverPhoto(groupId: String, coverUri: String)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String = RECENT_TASK_ID)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<PhotoGroupEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<PhotoEntity>)

    @Transaction
    suspend fun replaceRecentTask(
        task: TaskEntity,
        groups: List<PhotoGroupEntity>,
        photos: List<PhotoEntity>
    ) {
        deleteTask(task.id)
        insertTask(task)
        insertGroups(groups)
        insertPhotos(photos)
    }
}
