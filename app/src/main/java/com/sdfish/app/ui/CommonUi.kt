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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.layout.onSizeChanged
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
    var fullScreen by rememberSaveable { mutableStateOf(false) }
    LocationPicker(latitude, longitude, recenterToken, editable, onPick)
    if (fullScreen) {
        FullScreenPlacePicker(
            latitude = latitude,
            longitude = longitude,
            recenterToken = recenterToken,
            locating = status == LocationStatus.Locating,
            onPick = onPick,
            onRequestLocation = onRequestLocation,
            onDone = { fullScreen = false; editable = true },
        )
    }
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
            // Changing the place is easier on a big map.
            TextButton(onClick = { fullScreen = true }, modifier = Modifier.testTag("edit_place")) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.change_place))
            }
        }
    }
    if (editable) {
        TextButton(onClick = { fullScreen = true }, modifier = Modifier.testTag("open_place_map")) {
            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.open_big_map))
        }
    }
}

/** The whole screen as a map for moving the place; Cancel puts the place back. */
@Composable
private fun FullScreenPlacePicker(
    latitude: Double?,
    longitude: Double?,
    recenterToken: Int,
    locating: Boolean,
    onPick: (Double, Double) -> Unit,
    onRequestLocation: () -> Unit,
    onDone: () -> Unit,
) {
    val original = remember { if (latitude != null && longitude != null) latitude to longitude else null }
    val cancel = {
        original?.let { (lat, lon) -> onPick(lat, lon) }
        onDone()
    }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = cancel,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        androidx.compose.material3.Surface(Modifier.fillMaxSize().testTag("place_map_full")) {
            Column(Modifier.fillMaxSize().systemBarsPadding()) {
                androidx.compose.foundation.layout.Box(Modifier.weight(1f).fillMaxWidth()) {
                    LocationPicker(
                        latitude, longitude, recenterToken, editable = true, onPick = onPick,
                        modifier = Modifier.fillMaxSize(), centerOnStart = true,
                    )
                }
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.location_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = onRequestLocation, enabled = !locating) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.refresh_location))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = cancel, modifier = Modifier.testTag("place_cancel")) { Text(stringResource(R.string.cancel)) }
                        androidx.compose.material3.Button(onClick = onDone, modifier = Modifier.testTag("place_done")) {
                            Text(stringResource(R.string.done))
                        }
                    }
                }
            }
        }
    }
}

/** Map with the place: tap the map or drag the pin to move it when [editable]. */
@Composable
private fun LocationPicker(
    latitude: Double?,
    longitude: Double?,
    recenterToken: Int,
    editable: Boolean,
    onPick: (Double, Double) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)),
    centerOnStart: Boolean = false,
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

    LaunchedEffect(Unit) {
        if (centerOnStart && latitude != null && longitude != null) {
            mapView.controller.setZoom(16.0)
            mapView.controller.setCenter(GeoPoint(latitude, longitude))
        }
    }

    LaunchedEffect(recenterToken) {
        if (recenterToken > 0 && latitude != null && longitude != null) {
            mapView.controller.setZoom(15.0)
            mapView.controller.setCenter(GeoPoint(latitude, longitude))
        }
    }

    AndroidView(
        modifier = modifier,
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

/** How far (part of the card width) a swipe must go to count. */
private const val SWIPE_FRACTION = 0.4f

/**
 * A list item that can be swiped: left to right opens it for editing,
 * right to left asks to delete it. The item springs back either way.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeActions(onEdit: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val edit by rememberUpdatedState(onEdit)
    val delete by rememberUpdatedState(onDelete)
    // Act only on a deliberate swipe: the card must be dragged across a large part
    // of its width. A short quick flick (often a sloppy vertical scroll) does nothing.
    var width by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val holder = remember { arrayOfNulls<androidx.compose.material3.SwipeToDismissBoxState>(1) }
    val state = androidx.compose.material3.rememberSwipeToDismissBoxState(
        positionalThreshold = { total -> total * SWIPE_FRACTION },
        confirmValueChange = { value ->
            val dragged = holder[0]?.let { runCatching { kotlin.math.abs(it.requireOffset()) }.getOrNull() } ?: 0f
            if (value != androidx.compose.material3.SwipeToDismissBoxValue.Settled && width > 0f && dragged < width * SWIPE_FRACTION) return@rememberSwipeToDismissBoxState false
            when (value) {
                androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd -> edit()
                androidx.compose.material3.SwipeToDismissBoxValue.EndToStart -> delete()
                androidx.compose.material3.SwipeToDismissBoxValue.Settled -> Unit
            }
            false
        },
    )
    holder[0] = state
    androidx.compose.material3.SwipeToDismissBox(
        state = state,
        modifier = modifier.onSizeChanged { width = it.width.toFloat() },
        backgroundContent = {
            val toDelete = state.dismissDirection == androidx.compose.material3.SwipeToDismissBoxValue.EndToStart
            val toEdit = state.dismissDirection == androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd
            androidx.compose.foundation.layout.Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            toDelete -> MaterialTheme.colorScheme.errorContainer
                            toEdit -> MaterialTheme.colorScheme.primaryContainer
                            else -> androidx.compose.ui.graphics.Color.Transparent
                        }
                    )
                    .padding(horizontal = 24.dp),
                contentAlignment = if (toDelete) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                when {
                    toDelete -> Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    toEdit -> Icon(
                        Icons.Filled.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        },
        content = { content() },
    )
}

/** "Delete …?" confirmation used by swipe-to-delete. */
@Composable
fun ConfirmDeleteDialog(title: String, text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm, modifier = Modifier.testTag("confirm_delete")) { Text(stringResource(R.string.delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_delete")) { Text(stringResource(R.string.cancel)) }
        },
    )
}
