package com.snapsort.app

import android.content.Context
import com.snapsort.app.data.db.SnapSortDatabase
import com.snapsort.app.data.repository.TaskRepository
import com.snapsort.app.data.scanner.FolderScanner
import com.snapsort.app.data.scanner.PhotoExifReader
import com.snapsort.app.data.settings.UserSettingsRepository
import com.snapsort.app.data.update.UpdateRepository

object SnapSortDependencies {
    fun taskRepository(context: Context): TaskRepository {
        return TaskRepository(SnapSortDatabase.getInstance(context).snapSortDao())
    }

    fun userSettingsRepository(context: Context): UserSettingsRepository {
        return UserSettingsRepository(context.applicationContext)
    }

    fun folderScanner(context: Context): FolderScanner {
        return FolderScanner(context.applicationContext)
    }

    fun photoExifReader(context: Context): PhotoExifReader {
        return PhotoExifReader(context.applicationContext)
    }

    fun updateRepository(): UpdateRepository {
        return UpdateRepository()
    }
}
