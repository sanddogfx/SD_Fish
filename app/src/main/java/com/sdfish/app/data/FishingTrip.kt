package com.sdfish.app.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

/** One fishing session: a place, a method and a time span. [endTime] is null while it is running. */
@Entity(tableName = "trips")
data class FishingTrip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long?,
    val latitude: Double?,
    val longitude: Double?,
    /** A [FishingMethod.id]. */
    val method: String,
    val note: String,
) {
    val isActive: Boolean get() = endTime == null
}

data class TripWithCount(
    @Embedded val trip: FishingTrip,
    val catchCount: Int,
    /** Sum of the weights entered for the trip's fish; null when none has a weight. */
    val totalWeightKg: Double?,
    @Relation(parentColumn = "id", entityColumn = "tripId")
    val weather: TripWeather?,
)
