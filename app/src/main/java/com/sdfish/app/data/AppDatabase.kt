package com.sdfish.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CatchEntry::class, FishingTrip::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun catchDao(): CatchDao
    abstract fun tripDao(): TripDao

    companion object {
        const val NAME = "sdfish.db"

        /** Version 2 adds fishing trips; existing catches are kept. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `trips` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`startTime` INTEGER NOT NULL, `endTime` INTEGER, `latitude` REAL, `longitude` REAL, " +
                        "`method` TEXT NOT NULL, `note` TEXT NOT NULL)"
                )
                db.execSQL("ALTER TABLE `catches` ADD COLUMN `tripId` INTEGER")
                db.execSQL("ALTER TABLE `catches` ADD COLUMN `method` TEXT")
            }
        }

        fun create(context: Context, name: String = NAME): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
