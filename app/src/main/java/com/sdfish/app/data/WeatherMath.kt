package com.sdfish.app.data

import kotlin.math.floor
import kotlin.math.roundToInt

/** Kinds of weather, grouped from WMO weather codes. */
enum class WeatherKind(val emoji: String) {
    CLEAR("☀️"), PARTLY("⛅"), OVERCAST("☁️"), FOG("🌫️"), DRIZZLE("🌦️"),
    RAIN("🌧️"), SNOW("🌨️"), SHOWERS("🌦️"), THUNDER("⛈️");

    companion object {
        fun of(code: Int): WeatherKind = when (code) {
            0 -> CLEAR
            1, 2 -> PARTLY
            3 -> OVERCAST
            45, 48 -> FOG
            in 51..57 -> DRIZZLE
            in 61..67 -> RAIN
            in 71..77, 85, 86 -> SNOW
            in 80..82 -> SHOWERS
            in 95..99 -> THUNDER
            else -> OVERCAST
        }
    }
}

enum class PressureTrend(val arrow: String) {
    RISING("↑"), FALLING("↓"), STEADY("→");

    companion object {
        /** A change of at least 1 hPa in 3 hours counts as rising or falling. */
        fun of(changeHpa: Double): PressureTrend = when {
            changeHpa >= 1.0 -> RISING
            changeHpa <= -1.0 -> FALLING
            else -> STEADY
        }
    }
}

object WeatherMath {
    fun hpaToMmHg(hpa: Double): Double = hpa * 0.750062

    /** 0 = N, 1 = NE, ... 7 = NW. */
    fun compassIndex(degrees: Double): Int = (((degrees % 360 + 360) % 360) / 45.0).roundToInt() % 8
}

object MoonPhase {
    private const val SYNODIC_DAYS = 29.530588853
    /** A known new moon: 6 January 2000, 18:14 UTC. */
    private const val NEW_MOON_MILLIS = 947_182_440_000L

    /** Age of the moon in days since the last new moon, 0 until 29.53. */
    fun ageDays(timeMillis: Long): Double {
        val days = (timeMillis - NEW_MOON_MILLIS) / 86_400_000.0
        return days - SYNODIC_DAYS * floor(days / SYNODIC_DAYS)
    }

    /** 0 new moon, 1 waxing crescent, 2 first quarter, 3 waxing gibbous, 4 full moon, 5 waning gibbous, 6 last quarter, 7 waning crescent. */
    fun index(timeMillis: Long): Int = ((ageDays(timeMillis) / SYNODIC_DAYS * 8).roundToInt()) % 8

    val emoji = listOf("🌑", "🌒", "🌓", "🌔", "🌕", "🌖", "🌗", "🌘")
}
