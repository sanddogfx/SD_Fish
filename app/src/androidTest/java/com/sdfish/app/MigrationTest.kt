package com.sdfish.app

import android.database.sqlite.SQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sdfish.app.data.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** Catches saved by version 1 of the database must survive the updates to the latest version. */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @Test
    fun catchesSurviveUpgradeToTrips() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration-test.db"
        context.deleteDatabase(name)

        // A database exactly as version 1 of the app created it, with one catch.
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null).use { db ->
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `catches` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`timestamp` INTEGER NOT NULL, `latitude` REAL, `longitude` REAL, `accuracyMeters` REAL, " +
                    "`species` TEXT NOT NULL, `weightKg` REAL, `lengthCm` REAL, `photoPath` TEXT, `note` TEXT NOT NULL)"
            )
            db.execSQL(
                "INSERT INTO catches (timestamp, latitude, longitude, accuracyMeters, species, weightKg, lengthCm, photoPath, note) " +
                    "VALUES (1700000000000, 52.2, 21.0, 5.0, 'pike', 2.4, 68.0, '/old/pike.jpg', 'old catch')"
            )
            db.version = 1
        }

        val room = AppDatabase.create(context, name)
        try {
            val catches = runBlocking { room.catchDao().observeAll().first() }
            assertEquals(1, catches.size)
            assertEquals("pike", catches[0].species)
            assertEquals("old catch", catches[0].note)
            assertNull(catches[0].tripId)
            assertEquals(0, runBlocking { room.tripDao().observeWithCounts().first() }.size)
            assertEquals(0, runBlocking { room.trackDao().observeByTrip(1).first() }.size)
            assertEquals(0, runBlocking { room.tripPhotoDao().byTrip(1) }.size)
            assertNull(runBlocking { room.tripWeatherDao().get(1) })
            // The old single photo became the first of the fish's photos.
            assertEquals(listOf("/old/pike.jpg"), runBlocking { room.catchPhotoDao().pathsFor(catches[0].id) })
        } finally {
            room.close()
            context.deleteDatabase(name)
        }
    }
}
