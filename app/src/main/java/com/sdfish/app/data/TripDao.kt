package com.sdfish.app.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query(
        "SELECT trips.*, (SELECT COUNT(*) FROM catches WHERE catches.tripId = trips.id) AS catchCount " +
            "FROM trips ORDER BY startTime DESC"
    )
    fun observeWithCounts(): Flow<List<TripWithCount>>

    @Query("SELECT * FROM trips WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    fun observeActive(): Flow<FishingTrip?>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getById(id: Long): FishingTrip?

    @Upsert
    suspend fun upsert(trip: FishingTrip): Long

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteTrip(id: Long)

    @Query("UPDATE catches SET tripId = NULL WHERE tripId = :tripId")
    suspend fun detachCatches(tripId: Long)

    /** Deletes the trip; its catches stay as separate catches. */
    @Transaction
    suspend fun delete(id: Long) {
        detachCatches(id)
        deleteTrip(id)
    }
}
