package com.sdfish.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** A picture of a trip: the spot, the gear, the weather. [path] is a file in app storage. */
@Entity(tableName = "trip_photos", indices = [Index("tripId")])
data class TripPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val path: String,
    val addedAt: Long,
)

@Dao
interface TripPhotoDao {
    @Insert
    suspend fun insert(photo: TripPhoto)

    @Delete
    suspend fun delete(photo: TripPhoto)

    @Query("SELECT * FROM trip_photos WHERE tripId = :tripId ORDER BY addedAt")
    fun observeByTrip(tripId: Long): Flow<List<TripPhoto>>

    @Query("SELECT * FROM trip_photos WHERE tripId = :tripId")
    suspend fun byTrip(tripId: Long): List<TripPhoto>
}
