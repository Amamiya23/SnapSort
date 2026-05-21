package com.snapsort.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TaskEntity::class,
        PhotoGroupEntity::class,
        PhotoEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class SnapSortDatabase : RoomDatabase() {
    abstract fun snapSortDao(): SnapSortDao

    companion object {
        @Volatile
        private var instance: SnapSortDatabase? = null

        fun getInstance(context: Context): SnapSortDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SnapSortDatabase::class.java,
                    "snapsort.db"
                ).build().also { instance = it }
            }
        }
    }
}
