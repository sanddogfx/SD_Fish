package com.sdfish.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sdfish.app.backup.Backup
import com.sdfish.app.data.AppDatabase
import com.sdfish.app.data.CatchEntry
import com.sdfish.app.data.CatchPhoto
import com.sdfish.app.data.FishingTrip
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

/** A backup brings trips, fish and photos back, also when the photo folder differs on the new phone. */
@RunWith(AndroidJUnit4::class)
class BackupTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun backupRoundTrip() = runBlocking {
        val oldName = "backup-old.db"
        val newName = "backup-new.db"
        listOf(oldName, newName).forEach { context.deleteDatabase(it) }
        val oldPhotos = File(context.cacheDir, "old-photos").apply { deleteRecursively(); mkdirs() }
        val newPhotos = File(context.cacheDir, "new-photos").apply { deleteRecursively() }

        val bytes = ByteArrayOutputStream()
        val old = AppDatabase.create(context, oldName)
        try {
            val photo = File(oldPhotos, "catch_1.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
            val tripId = old.tripDao().upsert(
                FishingTrip(startTime = 1_000, endTime = 2_000, latitude = 52.2, longitude = 21.0, method = "feeder", note = "trip")
            )
            val catchId = old.catchDao().upsert(
                CatchEntry(
                    timestamp = 1_500, latitude = 52.2, longitude = 21.0, accuracyMeters = null, species = "roach",
                    weightKg = 1.8, lengthCm = null, photoPath = photo.absolutePath, note = "", tripId = tripId, count = 12,
                )
            )
            old.catchPhotoDao().insertAll(listOf(CatchPhoto(catchId = catchId, path = photo.absolutePath, position = 0)))
            Backup.write(old, context.getDatabasePath(oldName), oldPhotos, bytes)
        } finally {
            old.close()
        }

        val staged = Backup.stage(ByteArrayInputStream(bytes.toByteArray()), File(context.cacheDir, "restore-test"))
        Backup.install(staged, context.getDatabasePath(newName), newPhotos)

        val restored = AppDatabase.create(context, newName)
        try {
            val catches = restored.catchDao().observeAll().first()
            assertEquals(1, catches.size)
            assertEquals(12, catches[0].count)
            assertEquals(File(newPhotos, "catch_1.jpg").absolutePath, catches[0].photoPath)
            assertEquals(listOf(File(newPhotos, "catch_1.jpg").absolutePath), restored.catchPhotoDao().pathsFor(catches[0].id))
            assertTrue(File(newPhotos, "catch_1.jpg").readBytes().contentEquals(byteArrayOf(1, 2, 3)))
            assertEquals("trip", restored.tripDao().observeWithCounts().first().single().trip.note)
        } finally {
            restored.close()
            listOf(oldName, newName).forEach { context.deleteDatabase(it) }
            oldPhotos.deleteRecursively()
            newPhotos.deleteRecursively()
        }
    }

    @Test
    fun otherFilesAreRefused() {
        try {
            Backup.stage(ByteArrayInputStream("not a zip".toByteArray()), File(context.cacheDir, "restore-bad"))
            fail("expected NotABackup")
        } catch (e: Backup.NotABackup) {
            // Expected: nothing was touched.
        }
    }
}
