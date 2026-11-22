package com.sdfish.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sdfish.app.R
import com.sdfish.app.data.MoonPhase
import com.sdfish.app.data.PressureTrend
import com.sdfish.app.data.TripWeather
import com.sdfish.app.data.WeatherKind
import com.sdfish.app.data.WeatherMath
import kotlin.math.roundToInt

/** Russian and Belarusian anglers read pressure in mmHg; others in hPa. */
@Composable
private fun pressureText(hpa: Double): String {
    val lang = LocalConfiguration.current.locales[0].language
    return if (lang == "ru" || lang == "be") stringResource(R.string.pressure_mmhg, WeatherMath.hpaToMmHg(hpa).roundToInt().toString())
    else stringResource(R.string.pressure_hpa, hpa.roundToInt().toString())
}

@Composable
private fun temperatureText(c: Double): String {
    val t = c.roundToInt()
    return (if (t > 0) "+$t" else "$t") + "°C"
}

@Composable
private fun windText(w: TripWeather): String? {
    val speed = w.windSpeedMs ?: return null
    val dir = w.windDirectionDeg?.let { stringArrayResource(R.array.compass_points)[WeatherMath.compassIndex(it)] }
    return listOfNotNull(stringResource(R.string.wind_ms, speed.roundToInt().toString()), dir).joinToString(" ")
}

@Composable
private fun kindName(kind: WeatherKind): String = stringResource(
    when (kind) {
        WeatherKind.CLEAR -> R.string.weather_clear
        WeatherKind.PARTLY -> R.string.weather_partly
        WeatherKind.OVERCAST -> R.string.weather_overcast
        WeatherKind.FOG -> R.string.weather_fog
        WeatherKind.DRIZZLE -> R.string.weather_drizzle
        WeatherKind.RAIN -> R.string.weather_rain
        WeatherKind.SNOW -> R.string.weather_snow
        WeatherKind.SHOWERS -> R.string.weather_showers
        WeatherKind.THUNDER -> R.string.weather_thunder
    }
)

/** One short line for a trip card: sky, temperature, pressure with its trend, wind, moon. */
@Composable
fun WeatherLine(weather: TripWeather, modifier: Modifier = Modifier) {
    val parts = listOfNotNull(
        listOfNotNull(
            weather.weatherCode?.let { WeatherKind.of(it).emoji },
            weather.temperatureC?.let { temperatureText(it) },
        ).joinToString(" ").ifEmpty { null },
        weather.pressureHpa?.let { p ->
            pressureText(p) + (weather.pressureChangeHpa?.let { " " + PressureTrend.of(it).arrow } ?: "")
        },
        windText(weather),
        MoonPhase.emoji[MoonPhase.index(weather.time)],
    )
    Text(
        parts.joinToString(" · "),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** The weather section of a trip. Without weather it says why. */
@Composable
fun WeatherSection(weather: TripWeather?, startTime: Long, hasPlace: Boolean) {
    SectionTitle(stringResource(R.string.section_weather))
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().testTag("trip_weather")) {
        if (weather == null) {
            Text(
                stringResource(if (hasPlace) R.string.weather_pending else R.string.weather_no_place),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val headline = listOfNotNull(
                weather.weatherCode?.let { WeatherKind.of(it) }?.let { "${it.emoji} ${kindName(it)}" },
                weather.temperatureC?.let { temperatureText(it) },
            ).joinToString(", ")
            if (headline.isNotEmpty()) Text(headline, style = MaterialTheme.typography.titleMedium)
            weather.pressureHpa?.let { p ->
                val trend = weather.pressureChangeHpa?.let { change ->
                    val t = PressureTrend.of(change)
                    t.arrow + " " + stringResource(
                        when (t) {
                            PressureTrend.RISING -> R.string.pressure_rising
                            PressureTrend.FALLING -> R.string.pressure_falling
                            PressureTrend.STEADY -> R.string.pressure_steady
                        }
                    )
                }
                WeatherRow(stringResource(R.string.weather_pressure), listOfNotNull(pressureText(p), trend).joinToString(", "))
            }
            windText(weather)?.let { WeatherRow(stringResource(R.string.weather_wind), it) }
            weather.cloudCoverPct?.let {
                WeatherRow(stringResource(R.string.weather_clouds), stringResource(R.string.percent, it.roundToInt().toString()))
            }
            weather.precipitationMm?.let {
                WeatherRow(stringResource(R.string.weather_precip), stringResource(R.string.precip_mm, com.sdfish.app.Formats.number(it)))
            }
        }
        val moon = MoonPhase.index(weather?.time ?: startTime)
        WeatherRow(stringResource(R.string.weather_moon), "${MoonPhase.emoji[moon]} ${stringArrayResource(R.array.moon_phases)[moon]}")
    }
}

@Composable
private fun WeatherRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1.4f))
    }
}
