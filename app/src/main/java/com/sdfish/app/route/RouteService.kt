package com.sdfish.app.route

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.sdfish.app.MainActivity
import com.sdfish.app.R
import com.sdfish.app.SdFishApp
import com.sdfish.app.data.RouteMath
import com.sdfish.app.data.TrackPoint
import com.sdfish.app.hasLocationPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Records the GPS route of the running trip. Runs as a foreground service with a
 * notification, so recording goes on with the screen off or the app in the background.
 */
class RouteService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private var tripId = 0L
    private var callback: LocationCallback? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val requested = intent?.getLongExtra(EXTRA_TRIP_ID, 0L) ?: 0L
        if (!showNotification()) return START_NOT_STICKY
        if (requested != 0L) {
            begin(requested)
        } else {
            // Restarted by the system after it stopped the app: carry on with the running trip.
            scope.launch {
                val active = SdFishApp.from(this@RouteService).database.tripDao().observeActive().first()
                if (active == null) stopRecording() else launch(Dispatchers.Main) { begin(active.id) }
            }
        }
        return START_STICKY
    }

    private fun showNotification(): Boolean {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, getString(R.string.route_channel), NotificationManager.IMPORTANCE_LOW)
            )
        }
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.active_trip))
            .setContentText(getString(R.string.route_recording))
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .build()
        return try {
            ServiceCompat.startForeground(
                this, NOTIFICATION_ID, notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0,
            )
            true
        } catch (e: Exception) {
            // Not allowed right now (for example no location permission): skip recording.
            stopSelf()
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun begin(id: Long) {
        if (callback != null && tripId == id) return
        if (!hasLocationPermission(this)) {
            stopRecording()
            return
        }
        callback?.let { client.removeLocationUpdates(it) }
        tripId = id
        val dao = SdFishApp.from(this).database.trackDao()
        val cb = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val points = result.locations.filter { !it.hasAccuracy() || it.accuracy <= MAX_ACCURACY_M }
                if (points.isEmpty()) return
                scope.launch {
                    var last = dao.last(id)
                    for (loc in points) {
                        // Skip jitter while standing still on the shore.
                        val moved = last == null ||
                            RouteMath.distanceMeters(last.latitude, last.longitude, loc.latitude, loc.longitude) >= MIN_STEP_M
                        if (!moved) continue
                        val point = TrackPoint(
                            tripId = id,
                            time = loc.time,
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            accuracyMeters = if (loc.hasAccuracy()) loc.accuracy else null,
                        )
                        dao.insert(point)
                        last = point
                    }
                }
            }
        }
        callback = cb
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVAL_MS)
            .setMinUpdateDistanceMeters(MIN_STEP_M.toFloat())
            .build()
        try {
            client.requestLocationUpdates(request, cb, Looper.getMainLooper())
        } catch (e: SecurityException) {
            stopRecording()
        }
    }

    private fun stopRecording() {
        callback?.let { client.removeLocationUpdates(it) }
        callback = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        callback?.let { client.removeLocationUpdates(it) }
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_TRIP_ID = "tripId"
        private const val CHANNEL_ID = "route"
        private const val NOTIFICATION_ID = 1
        private const val INTERVAL_MS = 15_000L
        private const val MIN_STEP_M = 10.0
        private const val MAX_ACCURACY_M = 50f

        /** Starts recording the route of [tripId]. Does nothing without location permission. */
        fun start(context: Context, tripId: Long) {
            if (!hasLocationPermission(context)) return
            val intent = Intent(context, RouteService::class.java).putExtra(EXTRA_TRIP_ID, tripId)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                // Starting from the background is not allowed; the app retries when it is opened.
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, RouteService::class.java))
        }
    }
}
