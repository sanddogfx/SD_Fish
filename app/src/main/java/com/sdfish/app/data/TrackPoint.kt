package com.sdfish.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** One GPS point of the route walked or sailed during a trip. */
@Entity(tableName = "track_points", indices = [Index("tripId")])
data class TrackPoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val time: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
)

@Dao
interface TrackDao {
    @Insert
    suspend fun insert(point: TrackPoint)

    @Query("SELECT * FROM track_points WHERE tripId = :tripId ORDER BY time")
    fun observeByTrip(tripId: Long): Flow<List<TrackPoint>>

    @Query("SELECT * FROM track_points WHERE tripId = :tripId ORDER BY time DESC LIMIT 1")
    suspend fun last(tripId: Long): TrackPoint?
}

object RouteMath {
    private const val EARTH_RADIUS_M = 6_371_000.0

    /** Distance between two points in metres (haversine). */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_M * asin(sqrt(a))
    }

    /** Length of the route in metres. */
    fun lengthMeters(points: List<TrackPoint>): Double =
        points.zipWithNext { a, b -> distanceMeters(a.latitude, a.longitude, b.latitude, b.longitude) }.sum()
}
