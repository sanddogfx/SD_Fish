package com.sdfish.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * The weather at a trip's place when it started. [latitude], [longitude] and [time]
 * record what it was looked up for, so a moved or re-timed trip gets fresh weather.
 */
@Entity(tableName = "trip_weather")
data class TripWeather(
    @PrimaryKey val tripId: Long,
    val latitude: Double,
    val longitude: Double,
    val time: Long,
    val temperatureC: Double?,
    /** Sea-level pressure. */
    val pressureHpa: Double?,
    /** How the pressure changed over the 3 hours before [time]. */
    val pressureChangeHpa: Double?,
    val windSpeedMs: Double?,
    /** Where the wind blows from, degrees clockwise from north. */
    val windDirectionDeg: Double?,
    val cloudCoverPct: Double?,
    val precipitationMm: Double?,
    /** WMO weather code. */
    val weatherCode: Int?,
    val fetchedAt: Long,
)

@Dao
interface TripWeatherDao {
    @Upsert
    suspend fun upsert(weather: TripWeather)

    @Query("SELECT * FROM trip_weather WHERE tripId = :tripId")
    suspend fun get(tripId: Long): TripWeather?

    @Query("SELECT * FROM trip_weather WHERE tripId = :tripId")
    fun observe(tripId: Long): Flow<TripWeather?>
}
