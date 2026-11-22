package com.sdfish.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sdfish.app.data.AppDatabase
import com.sdfish.app.data.FishingTrip
import com.sdfish.app.weather.WeatherSync
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/** Looks up real weather from Open-Meteo for a recent trip (forecast service) and an old one (archive). */
@RunWith(AndroidJUnit4::class)
class WeatherSyncTest {
    @Test
    fun recentAndOldTripsGetWeather() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "weather-test.db"
        context.deleteDatabase(name)
        val db = AppDatabase.create(context, name)
        try {
            val day = 24L * 60 * 60 * 1000
            val now = System.currentTimeMillis()
            fun trip(daysAgo: Long) = FishingTrip(
                startTime = now - daysAgo * day, endTime = now - daysAgo * day + 3 * 60 * 60 * 1000,
                latitude = 52.23, longitude = 21.01, method = "spinning", note = "",
            )
            val recent = db.tripDao().upsert(trip(2))
            val old = db.tripDao().upsert(trip(200))
            WeatherSync.syncMissing(db)
            val recentWeather = db.tripWeatherDao().get(recent)
            val oldWeather = db.tripWeatherDao().get(old)
            assertNotNull("recent trip weather", recentWeather?.temperatureC)
            assertNotNull("recent trip pressure", recentWeather?.pressureHpa)
            assertNotNull("recent trip pressure trend", recentWeather?.pressureChangeHpa)
            assertNotNull("old trip weather", oldWeather?.temperatureC)
            assertNotNull("old trip wind", oldWeather?.windSpeedMs)
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}
