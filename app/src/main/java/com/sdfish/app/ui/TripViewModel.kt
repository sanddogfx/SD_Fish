package com.sdfish.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sdfish.app.SdFishApp
import com.sdfish.app.data.CatchEntry
import com.sdfish.app.data.FishingTrip
import com.sdfish.app.fetchCurrentLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

data class TripState(
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy: Float? = null,
    val method: String = "spinning",
    val note: String = "",
    val locationStatus: LocationStatus = LocationStatus.Idle,
    val recenterToken: Int = 0,
    val timeError: TripTimeError? = null,
)

/** Why Save was refused because of the trip times. */
sealed interface TripTimeError {
    data object EndBeforeStart : TripTimeError

    /** Fish of this trip were caught between [first] and [last]; the trip must cover that span. */
    data class OutsideCatches(val first: Long, val last: Long) : TripTimeError
}

class TripViewModel(app: Application, private val tripId: Long) : AndroidViewModel(app) {

    private val db = (app as SdFishApp).database
    private val tripDao = db.tripDao()

    var state by mutableStateOf(TripState())
        private set

    var locationRequested = false

    val isNew: Boolean get() = tripId == 0L

    val catches: Flow<List<CatchEntry>> =
        if (tripId == 0L) flowOf(emptyList()) else db.catchDao().observeByTrip(tripId)

    init {
        if (tripId != 0L) {
            viewModelScope.launch {
                tripDao.getById(tripId)?.let { t ->
                    state = TripState(
                        startTime = t.startTime,
                        endTime = t.endTime,
                        latitude = t.latitude,
                        longitude = t.longitude,
                        method = t.method,
                        note = t.note,
                        recenterToken = 1,
                    )
                }
            }
        }
    }

    fun update(transform: (TripState) -> TripState) {
        state = transform(state)
    }

    fun setStart(time: Long) {
        state = state.copy(startTime = time, timeError = null)
    }

    fun setEnd(time: Long) {
        state = state.copy(endTime = time, timeError = null)
    }

    /** Checks that the trip times are in order and cover all its fish. Times are compared to the minute. */
    private suspend fun checkTimes(endTime: Long?): TripTimeError? {
        if (isNew) return null
        val start = state.startTime / 60_000
        val end = endTime?.let { it / 60_000 }
        if (end != null && end < start) return TripTimeError.EndBeforeStart
        val times = db.catchDao().timesInTrip(tripId)
        if (times.isEmpty()) return null
        val first = times.min()
        val last = times.max()
        return if (first / 60_000 < start || (end != null && last / 60_000 > end)) {
            TripTimeError.OutsideCatches(first, last)
        } else {
            null
        }
    }

    fun setPosition(lat: Double, lon: Double) {
        state = state.copy(latitude = lat, longitude = lon, accuracy = null, locationStatus = LocationStatus.Idle)
    }

    fun markLocationDenied() {
        state = state.copy(locationStatus = LocationStatus.Denied)
    }

    fun fetchLocation() {
        state = state.copy(locationStatus = LocationStatus.Locating)
        viewModelScope.launch {
            val location = fetchCurrentLocation(getApplication())
            state = if (location != null) {
                state.copy(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = if (location.hasAccuracy()) location.accuracy else null,
                    locationStatus = LocationStatus.Idle,
                    recenterToken = state.recenterToken + 1,
                )
            } else {
                state.copy(locationStatus = LocationStatus.Failed)
            }
        }
    }

    private fun toTrip(endTime: Long?) = FishingTrip(
        id = tripId,
        startTime = if (isNew) System.currentTimeMillis() else state.startTime,
        endTime = endTime,
        latitude = state.latitude,
        longitude = state.longitude,
        method = state.method,
        note = state.note.trim(),
    )

    /** Starts a new trip, or saves changes to an existing one. [onDone] gets the trip id. */
    fun save(onDone: (Long) -> Unit) = store(state.endTime, onDone)

    fun finish(onDone: (Long) -> Unit) = store(System.currentTimeMillis(), onDone)

    private fun store(endTime: Long?, onDone: (Long) -> Unit) {
        viewModelScope.launch {
            val error = checkTimes(endTime)
            if (error != null) {
                state = state.copy(timeError = error)
                return@launch
            }
            val newId = tripDao.upsert(toTrip(endTime))
            onDone(if (isNew) newId else tripId)
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            tripDao.delete(tripId)
            onDone()
        }
    }

    companion object {
        fun factory(tripId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer { TripViewModel(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!, tripId) }
        }
    }
}
