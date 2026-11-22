package com.sdfish.app.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sdfish.app.Formats
import com.sdfish.app.PhotoStorage
import com.sdfish.app.SdFishApp
import com.sdfish.app.fetchCurrentLocation
import com.sdfish.app.data.CatchEntry
import com.sdfish.app.data.SpeciesCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EditState(
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracy: Float? = null,
    val speciesText: String = "",
    val weightText: String = "",
    val lengthText: String = "",
    val photoPath: String? = null,
    val note: String = "",
    val method: String? = null,
    val tripId: Long? = null,
    /** Start time of the trip this fish belongs to, for display. */
    val tripStart: Long? = null,
    /** End time of that trip; null while the trip is still running. */
    val tripEnd: Long? = null,
    /** Set when Save was refused because the time is outside the trip: the allowed span. */
    val timeOutOfTrip: Pair<Long, Long>? = null,
    val locationStatus: LocationStatus = LocationStatus.Idle,
    /** Changes whenever the map should jump to the current point. */
    val recenterToken: Int = 0,
)

class EditCatchViewModel(
    app: Application,
    private val catchId: Long,
    /** For a new fish added during a trip: take place and method from that trip. */
    private val newInTripId: Long,
) : AndroidViewModel(app) {

    private val dao = (app as SdFishApp).database.catchDao()
    private val tripDao = (app as SdFishApp).database.tripDao()
    private var originalPhotoPath: String? = null

    var state by mutableStateOf(EditState())
        private set

    /** Set once the automatic location request for a new catch has been made. */
    var locationRequested = false

    /** A new fish outside a trip gets its place from GPS automatically. */
    val wantsAutoLocation: Boolean get() = catchId == 0L && newInTripId == 0L

    init {
        if (catchId == 0L && newInTripId != 0L) {
            viewModelScope.launch {
                tripDao.getById(newInTripId)?.let { t ->
                    state = state.copy(
                        // A fish added later to a finished trip gets a time inside that trip.
                        timestamp = state.timestamp.coerceIn(t.startTime, maxOf(t.startTime, t.endTime ?: Long.MAX_VALUE)),
                        latitude = t.latitude,
                        longitude = t.longitude,
                        method = t.method,
                        tripId = t.id,
                        tripStart = t.startTime,
                        tripEnd = t.endTime,
                        recenterToken = state.recenterToken + 1,
                    )
                }
            }
        }
        if (catchId != 0L) {
            viewModelScope.launch {
                dao.getById(catchId)?.let { e ->
                    originalPhotoPath = e.photoPath
                    val trip = e.tripId?.let { id -> tripDao.getById(id) }
                    state = EditState(
                        timestamp = e.timestamp,
                        latitude = e.latitude,
                        longitude = e.longitude,
                        accuracy = e.accuracyMeters,
                        speciesText = if (e.species.isBlank()) "" else SpeciesCatalog.displayName(e.species),
                        weightText = Formats.number(e.weightKg),
                        lengthText = Formats.number(e.lengthCm),
                        photoPath = e.photoPath,
                        note = e.note,
                        method = e.method,
                        tripId = e.tripId,
                        tripStart = trip?.startTime,
                        tripEnd = trip?.endTime,
                        recenterToken = 1,
                    )
                }
            }
        }
    }

    fun update(transform: (EditState) -> EditState) {
        state = transform(state)
    }

    fun setTime(time: Long) {
        state = state.copy(timestamp = time, timeOutOfTrip = null)
    }

    /** The time span a fish of this trip may have, or null when it is not in a trip. */
    private fun allowedSpan(): Pair<Long, Long>? {
        val start = state.tripStart ?: return null
        return start to (state.tripEnd ?: System.currentTimeMillis())
    }

    fun setPosition(lat: Double, lon: Double) {
        // A point placed by hand has no GPS accuracy figure.
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

    fun setPhoto(path: String?) {
        val previous = state.photoPath
        // Remove a photo taken in this session that is being replaced; the saved one is kept until Save.
        if (previous != null && previous != originalPhotoPath && previous != path) PhotoStorage.delete(previous)
        state = state.copy(photoPath = path)
    }

    fun importPhoto(uri: Uri) {
        viewModelScope.launch {
            val file = withContext(Dispatchers.IO) { PhotoStorage.copyFromUri(getApplication(), uri) }
            if (file != null) setPhoto(file.absolutePath)
        }
    }

    private fun toEntry() = CatchEntry(
        id = catchId,
        timestamp = state.timestamp,
        latitude = state.latitude,
        longitude = state.longitude,
        accuracyMeters = state.accuracy,
        species = SpeciesCatalog.toStored(state.speciesText),
        weightKg = Formats.parseNumber(state.weightText),
        lengthCm = Formats.parseNumber(state.lengthText),
        photoPath = state.photoPath,
        note = state.note.trim(),
        tripId = state.tripId,
        method = state.method,
    )

    fun save(onSaved: () -> Unit) {
        val span = allowedSpan()
        // Times are picked to the minute, so compare whole minutes.
        if (span != null && state.timestamp / 60_000 !in span.first / 60_000..span.second / 60_000) {
            state = state.copy(timeOutOfTrip = span)
            return
        }
        viewModelScope.launch {
            dao.upsert(toEntry())
            if (originalPhotoPath != null && originalPhotoPath != state.photoPath) {
                withContext(Dispatchers.IO) { PhotoStorage.delete(originalPhotoPath) }
            }
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            dao.delete(toEntry())
            withContext(Dispatchers.IO) {
                PhotoStorage.delete(state.photoPath)
                if (originalPhotoPath != state.photoPath) PhotoStorage.delete(originalPhotoPath)
            }
            onDeleted()
        }
    }

    companion object {
        fun factory(catchId: Long, newInTripId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                EditCatchViewModel(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!, catchId, newInTripId)
            }
        }
    }
}
