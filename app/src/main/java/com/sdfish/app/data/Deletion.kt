package com.sdfish.app.data

import com.sdfish.app.PhotoStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Deleting a fish or a trip together with its photo files. */
object Deletion {
    suspend fun deleteCatch(db: AppDatabase, entry: CatchEntry) {
        val files = (db.catchPhotoDao().pathsFor(entry.id) + listOfNotNull(entry.photoPath)).distinct()
        db.catchDao().delete(entry)
        db.catchPhotoDao().deleteFor(entry.id)
        withContext(Dispatchers.IO) { files.forEach(PhotoStorage::delete) }
    }

    /** Deletes the trip, its route and photos; its fish stay as separate catches. */
    suspend fun deleteTrip(db: AppDatabase, tripId: Long) {
        val files = db.tripPhotoDao().byTrip(tripId).map { it.path }
        db.tripDao().delete(tripId)
        withContext(Dispatchers.IO) { files.forEach(PhotoStorage::delete) }
    }
}
