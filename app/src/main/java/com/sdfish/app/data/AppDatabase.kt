package com.sdfish.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CatchEntry::class, FishingTrip::class, TrackPoint::class, TripPhoto::class, CatchPhoto::class], version = 5, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun catchDao(): CatchDao
    abstract fun tripDao(): TripDao
    abstract fun trackDao(): TrackDao
    abstract fun tripPhotoDao(): TripPhotoDao
    abstract fun catchPhotoDao(): CatchPhotoDao

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

        /** Version 3 adds GPS route points of trips. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `track_points` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`tripId` INTEGER NOT NULL, `time` INTEGER NOT NULL, `latitude` REAL NOT NULL, " +
                        "`longitude` REAL NOT NULL, `accuracyMeters` REAL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_track_points_tripId` ON `track_points` (`tripId`)")
            }
        }

        /** Version 4 adds photos of trips. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `trip_photos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`tripId` INTEGER NOT NULL, `path` TEXT NOT NULL, `addedAt` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_trip_photos_tripId` ON `trip_photos` (`tripId`)")
            }
        }

        /** Version 5 lets a fish have several photos; the existing photo becomes the first one. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `catch_photos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`catchId` INTEGER NOT NULL, `path` TEXT NOT NULL, `position` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_catch_photos_catchId` ON `catch_photos` (`catchId`)")
                db.execSQL(
                    "INSERT INTO catch_photos (catchId, path, position) " +
                        "SELECT id, photoPath, 0 FROM catches WHERE photoPath IS NOT NULL"
                )
            }
        }

        fun create(context: Context, name: String = NAME): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
    }
}
