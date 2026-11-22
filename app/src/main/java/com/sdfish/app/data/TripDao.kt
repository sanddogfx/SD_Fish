package com.sdfish.app.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Transaction
    @Query(
        "SELECT trips.*, (SELECT COUNT(*) FROM catches WHERE catches.tripId = trips.id) AS catchCount, " +
            "(SELECT SUM(weightKg) FROM catches WHERE catches.tripId = trips.id) AS totalWeightKg " +
            "FROM trips ORDER BY startTime DESC"
    )
    fun observeWithCounts(): Flow<List<TripWithCount>>

    @Query("SELECT * FROM trips WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    fun observeActive(): Flow<FishingTrip?>

    @Query("SELECT * FROM trips WHERE latitude IS NOT NULL AND longitude IS NOT NULL ORDER BY startTime DESC")
    suspend fun withPlace(): List<FishingTrip>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getById(id: Long): FishingTrip?

    @Upsert
    suspend fun upsert(trip: FishingTrip): Long

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteTrip(id: Long)

    @Query("UPDATE catches SET tripId = NULL WHERE tripId = :tripId")
    suspend fun detachCatches(tripId: Long)

    @Query("DELETE FROM track_points WHERE tripId = :tripId")
    suspend fun deleteRoute(tripId: Long)

    @Query("DELETE FROM trip_photos WHERE tripId = :tripId")
    suspend fun deletePhotos(tripId: Long)

    @Query("DELETE FROM trip_weather WHERE tripId = :tripId")
    suspend fun deleteWeather(tripId: Long)

    /** Deletes the trip, its route, photo and weather records; its catches stay as separate catches. */
    @Transaction
    suspend fun delete(id: Long) {
        detachCatches(id)
        deleteRoute(id)
        deletePhotos(id)
        deleteWeather(id)
        deleteTrip(id)
    }
}
