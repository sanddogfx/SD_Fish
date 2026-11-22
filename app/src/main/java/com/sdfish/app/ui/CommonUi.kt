package com.sdfish.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
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

/**
 * Map with the chosen point, GPS status, coordinates and a "locate again" button.
 * With [startLocked] (a saved trip or fish) the place can't be changed by an accidental
 * tap on the map: the user first presses "Change place".
 */
@Composable
fun PlaceSection(
    latitude: Double?,
    longitude: Double?,
    accuracy: Float?,
    status: LocationStatus,
    recenterToken: Int,
    onPick: (Double, Double) -> Unit,
    onRequestLocation: () -> Unit,
    startLocked: Boolean = false,
) {
    var editable by rememberSaveable { mutableStateOf(!startLocked) }
    LocationPicker(latitude, longitude, recenterToken, editable, onPick)
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
            if (editable) {
                Text(
                    stringResource(R.string.location_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (editable) {
            TextButton(onClick = onRequestLocation, enabled = status != LocationStatus.Locating) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.refresh_location))
            }
        } else {
            TextButton(onClick = { editable = true }, modifier = Modifier.testTag("edit_place")) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.change_place))
            }
        }
    }
}

/** Small map: shows the point; when [editable], tap the map or drag the pin to move it. */
@Composable
private fun LocationPicker(
    latitude: Double?,
    longitude: Double?,
    recenterToken: Int,
    editable: Boolean,
    onPick: (Double, Double) -> Unit,
) {
    val context = LocalContext.current
    val mapView = rememberMapView()
    val pick by rememberUpdatedState(onPick)
    val canEdit by rememberUpdatedState(editable)
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
                if (!canEdit) return false
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
            marker.isDraggable = editable
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

/** Drop-down list for picking a fishing method. With [allowNone], "not set" is also offered. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MethodPicker(selected: String?, allowNone: Boolean, onSelect: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val notSet = stringResource(R.string.method_not_set)
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = if (selected == null) notSet else FishingMethods.displayName(selected),
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .testTag("method"),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (allowNone) {
                DropdownMenuItem(
                    text = { Text(notSet) },
                    modifier = Modifier.testTag("method_none"),
                    onClick = {
                        onSelect(null)
                        expanded = false
                    },
                )
            }
            FishingMethods.all.forEach { method ->
                DropdownMenuItem(
                    text = { Text(method.displayName()) },
                    modifier = Modifier.testTag("method_${method.id}"),
                    onClick = {
                        onSelect(method.id)
                        expanded = false
                    },
                )
            }
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

/** The fish's first photo, or a fish sign when it has none. */
@Composable
fun CatchThumbnail(entry: com.sdfish.app.data.CatchEntry, size: androidx.compose.ui.unit.Dp) {
    val thumb = Modifier.size(size).clip(RoundedCornerShape(8.dp))
    val path = entry.photoPath
    if (path != null) {
        coil.compose.AsyncImage(
            model = java.io.File(path),
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = thumb.testTag("catch_thumb_${entry.id}"),
        )
    } else {
        androidx.compose.foundation.layout.Box(
            thumb.background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) { Text("🐟", fontSize = (size.value * 0.44f).sp) }
    }
}
