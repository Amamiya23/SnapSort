package com.snapsort.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TaskEntity::class,
        PhotoGroupEntity::class,
        PhotoEntity::class
    ],
    version = 2,
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
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE photos ADD COLUMN aperture REAL")
                db.execSQL("ALTER TABLE photos ADD COLUMN shutterSpeedSeconds REAL")
                db.execSQL("ALTER TABLE photos ADD COLUMN iso INTEGER")
            }
        }
    }
}
