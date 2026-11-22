package com.sdfish.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.sdfish.app.Formats
import com.sdfish.app.LOCATION_PERMISSIONS
import com.sdfish.app.R
import com.sdfish.app.data.FishingMethods
import com.sdfish.app.hasLocationPermission
import kotlinx.coroutines.delay
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.util.Calendar
import kotlin.math.roundToInt

enum class LocationStatus { Idle, Locating, Failed, Denied }

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
fun ErrorText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
}

/**
 * Returns a function that asks for location permission if needed and then calls [onGranted],
 * or [onDenied] if the user refuses.
 */
@Composable
fun rememberLocationRequest(onGranted: () -> Unit, onDenied: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val granted by rememberUpdatedState(onGranted)
    val denied by rememberUpdatedState(onDenied)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) granted() else denied()
    }
    return remember(launcher) {
        { if (hasLocationPermission(context)) granted() else launcher.launch(LOCATION_PERMISSIONS) }
    }
}

/** Map with the chosen point, GPS status, coordinates and a "locate again" button. */
@Composable
fun PlaceSection(
    latitude: Double?,
    longitude: Double?,
    accuracy: Float?,
    status: LocationStatus,
    recenterToken: Int,
    onPick: (Double, Double) -> Unit,
    onRequestLocation: () -> Unit,
) {
    LocationPicker(latitude, longitude, recenterToken, onPick)
    when (status) {
        LocationStatus.Locating -> {
            Text(stringResource(R.string.locating))
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        LocationStatus.Failed -> ErrorText(stringResource(R.string.location_error))
        LocationStatus.Denied -> ErrorText(stringResource(R.string.location_denied))
        LocationStatus.Idle -> Unit
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            if (latitude != null && longitude != null) {
                Text(Formats.coords(latitude, longitude), style = MaterialTheme.typography.bodyMedium)
                accuracy?.let {
                    Text(
                        stringResource(R.string.location_accuracy, it.roundToInt()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                stringResource(R.string.location_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onRequestLocation, enabled = status != LocationStatus.Locating) {
            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.refresh_location))
        }
    }
}

/** Small map: shows the point; tap the map or drag the pin to move it. */
@Composable
private fun LocationPicker(latitude: Double?, longitude: Double?, recenterToken: Int, onPick: (Double, Double) -> Unit) {
    val context = LocalContext.current
    val mapView = rememberMapView()
    val pick by rememberUpdatedState(onPick)
    val marker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(context, R.drawable.ic_map_pin)
            setInfoWindow(null)
            isDraggable = true
            setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                override fun onMarkerDrag(marker: Marker) = Unit
                override fun onMarkerDragStart(marker: Marker) = Unit
                override fun onMarkerDragEnd(marker: Marker) {
                    pick(marker.position.latitude, marker.position.longitude)
                }
            })
        }
    }
    val tapOverlay = remember {
        MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                pick(p.latitude, p.longitude)
                return true
            }

            override fun longPressHelper(p: GeoPoint): Boolean = false
        })
    }

    LaunchedEffect(recenterToken) {
        if (recenterToken > 0 && latitude != null && longitude != null) {
            mapView.controller.setZoom(15.0)
            mapView.controller.setCenter(GeoPoint(latitude, longitude))
        }
    }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp)),
        factory = {
            mapView.apply {
                if (!overlays.contains(tapOverlay)) overlays.add(0, tapOverlay)
                if (!overlays.contains(marker)) overlays.add(marker)
            }
        },
        update = { map ->
            if (latitude != null && longitude != null) {
                marker.position = GeoPoint(latitude, longitude)
                marker.isEnabled = true
            } else {
                marker.isEnabled = false
            }
            map.invalidate()
        },
    )
}

/** Chips for picking a fishing method. With [allowNone], tapping the selected chip clears it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MethodPicker(selected: String?, allowNone: Boolean, onSelect: (String?) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FishingMethods.all.forEach { method ->
            val isSelected = method.id == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(if (isSelected && allowNone) null else method.id) },
                label = { Text(method.displayName()) },
                modifier = Modifier.testTag("method_${method.id}"),
            )
        }
    }
}

/** Ticking "1:23:45" since [start] (until [end], if given). */
@Composable
fun rememberElapsed(start: Long, end: Long? = null): String {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(start, end) {
        while (end == null) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    return Formats.duration((end ?: now) - start)
}

fun pickDateTime(context: Context, initial: Long, onPicked: (Long) -> Unit) {
    val cal = Calendar.getInstance().apply { timeInMillis = initial }
    DatePickerDialog(
        context,
        { _, year, month, day ->
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    cal.set(year, month, day, hour, minute, 0)
                    onPicked(cal.timeInMillis)
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true,
            ).show()
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH),
    ).show()
}
