package com.sdfish.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sdfish.app.data.FishingTrip
import com.sdfish.app.weather.WeatherSync
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Reads weather from an Open-Meteo answer recorded in the shape the service returns.
 * The live service is not called here: CI machines share addresses that Open-Meteo
 * often rate-limits, which would make the build fail for reasons outside the app.
 */
@RunWith(AndroidJUnit4::class)
class WeatherSyncTest {
    private val hour = 3600L
    /** 2026-11-21 00:00 UTC. */
    private val base = 1_795_219_200L

    private fun answer(): JSONObject {
        val times = (0 until 30).map { base + it * hour }
        val pressure = (0 until 30).map { 1020.0 - it * 0.5 }
        return JSONObject(
            mapOf(
                "latitude" to 52.23, "longitude" to 21.01,
                "hourly" to mapOf(
                    "time" to times,
                    "temperature_2m" to (0 until 30).map { 2.0 + it * 0.1 },
                    "pressure_msl" to pressure,
                    "wind_speed_10m" to (0 until 30).map { 3.5 },
                    "wind_direction_10m" to (0 until 30).map { 290.0 },
                    "cloud_cover" to (0 until 30).map { 75.0 },
                    "precipitation" to (0 until 30).map { 0.2 },
                    "weather_code" to (0 until 30).map { 61 },
                ),
            )
        )
    }

    private fun trip(startSec: Long) = FishingTrip(
        id = 7, startTime = startSec * 1000, endTime = startSec * 1000 + 3 * hour * 1000,
        latitude = 52.23, longitude = 21.01, method = "spinning", note = "",
    )

    @Test
    fun readsTheHourTheTripStarted() {
        // Trip starts at 10:40 UTC: the 10:00 values are used, the trend compares with 07:00.
        val w = WeatherSync.parse(answer(), trip(base + 10 * hour + 40 * 60), 52.23, 21.01, now = 0)
        assertNotNull(w)
        w!!
        assertEquals(7L, w.tripId)
        assertEquals(3.0, w.temperatureC!!, 1e-9)
        assertEquals(1015.0, w.pressureHpa!!, 1e-9)
        assertEquals(-1.5, w.pressureChangeHpa!!, 1e-9)
        assertEquals(3.5, w.windSpeedMs!!, 1e-9)
        assertEquals(290.0, w.windDirectionDeg!!, 1e-9)
        assertEquals(75.0, w.cloudCoverPct!!, 1e-9)
        assertEquals(0.2, w.precipitationMm!!, 1e-9)
        assertEquals(61, w.weatherCode)
    }

    @Test
    fun noTrendAtTheFirstHours() {
        val w = WeatherSync.parse(answer(), trip(base + hour), 52.23, 21.01, now = 0)
        assertNotNull(w?.pressureHpa)
        assertNull(w?.pressureChangeHpa)
    }

    @Test
    fun missingDataIsLeftForLater() {
        val json = answer()
        json.getJSONObject("hourly").put("temperature_2m", org.json.JSONArray((0 until 30).map { JSONObject.NULL }))
        assertNull(WeatherSync.parse(json, trip(base + 5 * hour), 52.23, 21.01, now = 0))
    }

    @Test
    fun recentTripsUseForecastOlderUseArchive() {
        val day = 24L * 3600 * 1000
        val now = System.currentTimeMillis()
        assertTrue(WeatherSync.endpointFor(now - 2 * day, now).contains("api.open-meteo.com/v1/forecast"))
        assertTrue(WeatherSync.endpointFor(now - 200 * day, now).contains("archive-api.open-meteo.com/v1/archive"))
    }
}
