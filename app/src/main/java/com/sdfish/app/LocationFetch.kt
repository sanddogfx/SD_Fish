package com.sdfish.app

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

/** Current GPS position, or the last known one, or null. Call only with location permission. */
@SuppressLint("MissingPermission")
suspend fun fetchCurrentLocation(context: Context): Location? {
    val client = LocationServices.getFusedLocationProviderClient(context)
    return try {
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).await()
            ?: client.lastLocation.await()
    } catch (e: Exception) {
        null
    }
}
