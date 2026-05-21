package com.snapsort.app

import android.content.Context
import com.snapsort.app.data.db.SnapSortDatabase
import com.snapsort.app.data.repository.TaskRepository
import com.snapsort.app.data.scanner.FolderScanner
import com.snapsort.app.data.settings.UserSettingsRepository

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
}
