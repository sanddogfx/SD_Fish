package com.sdfish.app.data

import java.util.Calendar
import java.util.TimeZone

enum class StatsPeriod { MONTH, YEAR, ALL }

data class SpeciesStats(val species: String, val count: Int, val weightKg: Double?)

data class MethodStats(val method: String, val trips: Int, val hours: Double, val fish: Int) {
    val fishPerHour: Double? get() = if (hours >= StatsCalculator.MIN_HOURS_FOR_RATE) fish / hours else null
}

data class Stats(
    val trips: Int,
    val hours: Double,
    val fish: Int,
    val weightKg: Double?,
    /** Fish caught on trips divided by hours on trips. */
    val fishPerHour: Double?,
    val bySpecies: List<SpeciesStats>,
    val byMethod: List<MethodStats>,
    /** The heaviest single fish (entries of several fish do not count). */
    val heaviest: CatchEntry?,
    val longest: CatchEntry?,
) {
    val isEmpty: Boolean get() = trips == 0 && fish == 0
}

/** Statistics over trips and catches; plain Kotlin so it can move to shared code later. */
object StatsCalculator {
    private const val HOUR = 3_600_000.0
    /** Below this much time on the water a "fish per hour" figure is meaningless. */
    const val MIN_HOURS_FOR_RATE = 0.5

    /** Start of the period containing [now], or null for all time. */
    fun periodStart(period: StatsPeriod, now: Long, zone: TimeZone = TimeZone.getDefault()): Long? {
        if (period == StatsPeriod.ALL) return null
        val c = Calendar.getInstance(zone).apply {
            timeInMillis = now
            if (period == StatsPeriod.YEAR) set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return c.timeInMillis
    }

    fun compute(trips: List<FishingTrip>, catches: List<CatchEntry>, from: Long?, now: Long): Stats {
        val t = trips.filter { from == null || it.startTime >= from }
        val c = catches.filter { from == null || it.timestamp >= from }
        fun hoursOf(trip: FishingTrip) = ((trip.endTime ?: now) - trip.startTime).coerceAtLeast(0) / HOUR
        val hours = t.sumOf { hoursOf(it) }
        val tripIds = t.map { it.id }.toSet()
        val fishOnTrips = c.filter { it.tripId in tripIds }.sumOf { it.count }
        val weights = c.mapNotNull { it.weightKg }

        val bySpecies = c.groupBy { it.species }
            .map { (species, list) ->
                SpeciesStats(
                    species = species,
                    count = list.sumOf { it.count },
                    weightKg = list.mapNotNull { it.weightKg }.takeIf { it.isNotEmpty() }?.sum(),
                )
            }
            .sortedWith(compareByDescending<SpeciesStats> { it.count }.thenByDescending { it.weightKg ?: 0.0 })

        val tripById = t.associateBy { it.id }
        val byMethod = t.groupBy { it.method }
            .map { (method, list) ->
                val ids = list.map { it.id }.toSet()
                MethodStats(
                    method = method,
                    trips = list.size,
                    hours = list.sumOf { hoursOf(it) },
                    fish = c.filter { it.tripId in ids }.sumOf { it.count },
                )
            }
            .sortedWith(compareByDescending<MethodStats> { it.fish }.thenByDescending { it.trips })

        val singles = c.filter { it.count <= 1 }
        return Stats(
            trips = t.size,
            hours = hours,
            fish = c.sumOf { it.count },
            weightKg = weights.takeIf { it.isNotEmpty() }?.sum(),
            fishPerHour = if (hours >= MIN_HOURS_FOR_RATE && tripById.isNotEmpty()) fishOnTrips / hours else null,
            bySpecies = bySpecies,
            byMethod = byMethod,
            heaviest = singles.filter { it.weightKg != null }.maxByOrNull { it.weightKg!! },
            longest = singles.filter { it.lengthCm != null }.maxByOrNull { it.lengthCm!! },
        )
    }
}
