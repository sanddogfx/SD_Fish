package com.sdfish.app.ui

import android.annotation.SuppressLint
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
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.sdfish.app.Formats
import com.sdfish.app.PhotoStorage
import com.sdfish.app.SdFishApp
import com.sdfish.app.data.CatchEntry
import com.sdfish.app.data.SpeciesCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

enum class LocationStatus { Idle, Locating, Failed, Denied }

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
    val locationStatus: LocationStatus = LocationStatus.Idle,
    /** Changes whenever the map should jump to the current point. */
    val recenterToken: Int = 0,
)

class EditCatchViewModel(app: Application, private val catchId: Long) : AndroidViewModel(app) {

    private val dao = (app as SdFishApp).database.catchDao()
    private var originalPhotoPath: String? = null

    var state by mutableStateOf(EditState())
        private set

    /** Set once the automatic location request for a new catch has been made. */
    var locationRequested = false

    init {
        if (catchId != 0L) {
            viewModelScope.launch {
                dao.getById(catchId)?.let { e ->
                    originalPhotoPath = e.photoPath
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
                        recenterToken = 1,
                    )
                }
            }
        }
    }

    fun update(transform: (EditState) -> EditState) {
        state = transform(state)
    }

    fun setPosition(lat: Double, lon: Double) {
        // A point placed by hand has no GPS accuracy figure.
        state = state.copy(latitude = lat, longitude = lon, accuracy = null, locationStatus = LocationStatus.Idle)
    }

    fun markLocationDenied() {
        state = state.copy(locationStatus = LocationStatus.Denied)
    }

    @SuppressLint("MissingPermission") // The screen checks the permission before calling.
    fun fetchLocation() {
        state = state.copy(locationStatus = LocationStatus.Locating)
        viewModelScope.launch {
            val client = LocationServices.getFusedLocationProviderClient(getApplication<Application>())
            val location = try {
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).await()
                    ?: client.lastLocation.await()
            } catch (e: Exception) {
                null
            }
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
    )

    fun save(onSaved: () -> Unit) {
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
        fun factory(catchId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                EditCatchViewModel(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!, catchId)
            }
        }
    }
}
