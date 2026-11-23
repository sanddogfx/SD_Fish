package com.sdfish.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sdfish.app.Formats
import com.sdfish.app.R
import com.sdfish.app.data.CatchEntry
import com.sdfish.app.data.FishingMethods
import com.sdfish.app.data.StatsCalculator
import com.sdfish.app.data.StatsPeriod
import com.sdfish.app.data.TripWithCount
import java.util.Locale

@Composable
fun StatsScreen(trips: List<TripWithCount>, catches: List<CatchEntry>, onOpenCatch: (Long) -> Unit) {
    var periodName by rememberSaveable { mutableStateOf(StatsPeriod.MONTH.name) }
    val period = StatsPeriod.valueOf(periodName)
    val stats = remember(trips, catches, period) {
        val now = System.currentTimeMillis()
        StatsCalculator.compute(trips.map { it.trip }, catches, StatsCalculator.periodStart(period, now), now)
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("stats"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                StatsPeriod.MONTH to R.string.stats_month,
                StatsPeriod.YEAR to R.string.stats_year,
                StatsPeriod.ALL to R.string.stats_all,
            ).forEach { (p, label) ->
                FilterChip(
                    selected = period == p,
                    onClick = { periodName = p.name },
                    label = { Text(stringResource(label)) },
                    modifier = Modifier.testTag("stats_period_${p.name.lowercase()}"),
                )
            }
        }

        if (stats.isEmpty) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.stats_empty), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
            }
            return@Column
        }

        // Totals for the period.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(stringResource(R.string.stats_trips), stats.trips.toString(), Modifier.weight(1f))
            StatTile(stringResource(R.string.stats_hours), hours(stats.hours), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(stringResource(R.string.stats_fish), stats.fish.toString(), Modifier.weight(1f).testTag("stats_fish"))
            StatTile(
                stringResource(R.string.stats_weight),
                stats.weightKg?.let { stringResource(R.string.weight_kg, Formats.number(it)) } ?: "—",
                Modifier.weight(1f),
            )
        }
        stats.fishPerHour?.let {
            StatTile(stringResource(R.string.stats_per_hour), perHour(it), Modifier.fillMaxWidth())
        }

        // Records.
        if (stats.heaviest != null || stats.longest != null) {
            SectionTitle(stringResource(R.string.stats_records))
            stats.heaviest?.let { fish ->
                RecordRow(stringResource(R.string.stats_heaviest), fish, stringResource(R.string.weight_kg, Formats.number(fish.weightKg))) {
                    onOpenCatch(fish.id)
                }
            }
            stats.longest?.let { fish ->
                RecordRow(stringResource(R.string.stats_longest), fish, stringResource(R.string.length_cm, Formats.number(fish.lengthCm))) {
                    onOpenCatch(fish.id)
                }
            }
        }

        // By species.
        if (stats.bySpecies.isNotEmpty()) {
            SectionTitle(stringResource(R.string.stats_by_species))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    stats.bySpecies.forEach { s ->
                        val name = if (s.species.isBlank()) stringResource(R.string.species_unknown)
                        else com.sdfish.app.data.SpeciesCatalog.displayName(s.species)
                        StatLine(
                            name,
                            listOfNotNull(
                                stringResource(R.string.fish_count, s.count),
                                s.weightKg?.let { stringResource(R.string.weight_kg, Formats.number(it)) },
                            ).joinToString(" · "),
                        )
                    }
                }
            }
        }

        // By fishing method.
        if (stats.byMethod.isNotEmpty()) {
            SectionTitle(stringResource(R.string.stats_by_method))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    stats.byMethod.forEach { m ->
                        StatLine(
                            FishingMethods.displayName(m.method),
                            listOfNotNull(
                                stringResource(R.string.stats_trips_count, m.trips),
                                stringResource(R.string.fish_count, m.fish),
                                m.fishPerHour?.let { perHour(it) },
                            ).joinToString(" · "),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun hours(h: Double): String = stringResource(R.string.stats_hours_value, String.format(Locale.getDefault(), "%.1f", h))

@Composable
private fun perHour(v: Double): String = stringResource(R.string.stats_per_hour_value, String.format(Locale.getDefault(), "%.1f", v))

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun StatLine(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RecordRow(title: String, fish: CatchEntry, value: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CatchThumbnail(fish, size = 48.dp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${speciesLabel(fish)} · $value", style = MaterialTheme.typography.titleMedium)
                Text(Formats.dateTime(fish.timestamp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
