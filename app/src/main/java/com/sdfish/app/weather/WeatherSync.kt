package com.sdfish.app.weather

import com.sdfish.app.data.AppDatabase
import com.sdfish.app.data.FishingTrip
import com.sdfish.app.data.TripWeather
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/**
 * Looks up the weather of trips that have a place but no weather yet, from Open-Meteo
 * (free, no key). Runs only while the app is open; with no network it stops and tries
 * again next time, so there is no background work draining the battery.
 */
object WeatherSync {
    private val mutex = Mutex()
    private const val HOUR = 60 * 60 * 1000L
    private const val DAY = 24 * HOUR
    /** The forecast service covers about the last 3 months; older trips use the archive. */
    private const val FORECAST_DAYS_BACK = 60L
    private const val MAX_PER_RUN = 20

    suspend fun syncMissing(db: AppDatabase) = withContext(Dispatchers.IO) {
        if (!mutex.tryLock()) return@withContext
        try {
            val dao = db.tripWeatherDao()
            val now = System.currentTimeMillis()
            db.tripDao().withPlace()
                .filter { it.startTime <= now }
                .filter { trip -> dao.get(trip.id)?.let { !it.matches(trip) } ?: true }
                .take(MAX_PER_RUN)
                .forEach { trip ->
                    val weather = try {
                        fetch(trip, now)
                    } catch (e: IOException) {
                        return@withContext // Offline or the service is down: try again later.
                    } catch (e: Exception) {
                        null // Odd answer for this trip: skip it.
                    }
                    if (weather != null) dao.upsert(weather)
                }
        } finally {
            mutex.unlock()
        }
    }

    private fun TripWeather.matches(trip: FishingTrip): Boolean =
        time == trip.startTime &&
            abs(latitude - (trip.latitude ?: 0.0)) < 1e-4 &&
            abs(longitude - (trip.longitude ?: 0.0)) < 1e-4

    private fun fetch(trip: FishingTrip, now: Long): TripWeather? {
        val lat = trip.latitude ?: return null
        val lon = trip.longitude ?: return null
        val start = trip.startTime
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val host = if (now - start < FORECAST_DAYS_BACK * DAY) "https://api.open-meteo.com/v1/forecast"
        else "https://archive-api.open-meteo.com/v1/archive"
        val url = "$host?latitude=${"%.4f".format(Locale.US, lat)}&longitude=${"%.4f".format(Locale.US, lon)}" +
            "&hourly=temperature_2m,pressure_msl,wind_speed_10m,wind_direction_10m,cloud_cover,precipitation,weather_code" +
            "&wind_speed_unit=ms&timezone=GMT&timeformat=unixtime" +
            "&start_date=${day.format(start - 3 * HOUR)}&end_date=${day.format(start)}"
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
        }
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                // A trip too recent for the archive, or too old for the forecast: try later.
                if (conn.responseCode >= 500) throw IOException("HTTP ${conn.responseCode}")
                return null
            }
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            return parse(json, trip, lat, lon, now)
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(json: JSONObject, trip: FishingTrip, lat: Double, lon: Double, now: Long): TripWeather? {
        val hourly = json.getJSONObject("hourly")
        val times = hourly.getJSONArray("time")
        val startSec = trip.startTime / 1000
        var i = -1
        for (k in 0 until times.length()) if (times.getLong(k) <= startSec) i = k
        if (i < 0) return null
        fun value(name: String, at: Int): Double? {
            val arr = hourly.optJSONArray(name) ?: return null
            if (at < 0 || at >= arr.length() || arr.isNull(at)) return null
            return arr.getDouble(at)
        }
        val temperature = value("temperature_2m", i) ?: return null // No data yet: try later.
        val pressure = value("pressure_msl", i)
        val before = value("pressure_msl", i - 3)
        return TripWeather(
            tripId = trip.id,
            latitude = lat,
            longitude = lon,
            time = trip.startTime,
            temperatureC = temperature,
            pressureHpa = pressure,
            pressureChangeHpa = if (pressure != null && before != null) pressure - before else null,
            windSpeedMs = value("wind_speed_10m", i),
            windDirectionDeg = value("wind_direction_10m", i),
            cloudCoverPct = value("cloud_cover", i),
            precipitationMm = value("precipitation", i),
            weatherCode = value("weather_code", i)?.toInt(),
            fetchedAt = now,
        )
    }
}
