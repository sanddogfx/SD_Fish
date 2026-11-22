package com.sdfish.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sdfish.app.Formats
import com.sdfish.app.LOCATION_PERMISSIONS
import com.sdfish.app.PhotoStorage
import com.sdfish.app.R
import com.sdfish.app.data.SpeciesCatalog
import com.sdfish.app.hasLocationPermission
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.util.Calendar
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCatchScreen(catchId: Long, onDone: () -> Unit) {
    val context = LocalContext.current
    val vm: EditCatchViewModel = viewModel(factory = EditCatchViewModel.factory(catchId))
    val s = vm.state
    val isNew = catchId == 0L
    var confirmDelete by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) vm.fetchLocation() else vm.markLocationDenied()
    }
    val requestLocation = {
        if (hasLocationPermission(context)) vm.fetchLocation() else permissionLauncher.launch(LOCATION_PERMISSIONS)
    }
    LaunchedEffect(Unit) {
        if (isNew && !vm.locationRequested) {
            vm.locationRequested = true
            requestLocation()
        }
    }

    var pendingPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        pendingPhoto?.let { path -> if (ok) vm.setPhoto(path) else File(path).delete() }
        pendingPhoto = null
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.importPhoto(uri)
    }
    val noCamera = stringResource(R.string.no_camera)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (isNew) R.string.new_catch else R.string.edit_catch)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = { vm.save(onDone) },
                    modifier = Modifier
                        .testTag("save")
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(16.dp),
                ) { Text(stringResource(R.string.save)) }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // --- Fish ---
            SectionTitle(stringResource(R.string.section_fish))
            SpeciesField(s.speciesText) { text -> vm.update { it.copy(speciesText = text) } }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = s.weightText,
                    onValueChange = { v -> vm.update { it.copy(weightText = v) } },
                    label = { Text(stringResource(R.string.weight_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("weight"),
                )
                OutlinedTextField(
                    value = s.lengthText,
                    onValueChange = { v -> vm.update { it.copy(lengthText = v) } },
                    label = { Text(stringResource(R.string.length_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("length"),
                )
            }

            // --- Photo ---
            SectionTitle(stringResource(R.string.section_photo))
            s.photoPath?.let { path ->
                AsyncImage(
                    model = File(path),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    val file = PhotoStorage.newPhotoFile(context)
                    pendingPhoto = file.absolutePath
                    try {
                        cameraLauncher.launch(PhotoStorage.uriFor(context, file))
                    } catch (e: ActivityNotFoundException) {
                        pendingPhoto = null
                        Toast.makeText(context, noCamera, Toast.LENGTH_SHORT).show()
                    }
                }) { Text(stringResource(R.string.take_photo)) }
                OutlinedButton(onClick = {
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text(stringResource(R.string.pick_photo)) }
                if (s.photoPath != null) {
                    TextButton(onClick = { vm.setPhoto(null) }) { Text(stringResource(R.string.remove_photo)) }
                }
            }

            // --- Place and time ---
            SectionTitle(stringResource(R.string.section_place))
            OutlinedButton(onClick = { pickDateTime(context, s.timestamp) { t -> vm.update { it.copy(timestamp = t) } } }) {
                Icon(Icons.Filled.DateRange, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(Formats.dateTime(s.timestamp))
            }
            LocationPicker(vm, s)
            when (s.locationStatus) {
                LocationStatus.Locating -> {
                    Text(stringResource(R.string.locating))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                LocationStatus.Failed -> ErrorText(stringResource(R.string.location_error))
                LocationStatus.Denied -> ErrorText(stringResource(R.string.location_denied))
                LocationStatus.Idle -> Unit
            }
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    if (s.latitude != null && s.longitude != null) {
                        Text(Formats.coords(s.latitude, s.longitude), style = MaterialTheme.typography.bodyMedium)
                        s.accuracy?.let {
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
                TextButton(onClick = requestLocation, enabled = s.locationStatus != LocationStatus.Locating) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.refresh_location))
                }
            }

            // --- Note ---
            OutlinedTextField(
                value = s.note,
                onValueChange = { v -> vm.update { it.copy(note = v) } },
                label = { Text(stringResource(R.string.note_label)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth().testTag("note"),
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_text)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; vm.delete(onDone) }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun ErrorText(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
}

/** Small map: shows the catch point; tap the map or drag the pin to move it. */
@Composable
private fun LocationPicker(vm: EditCatchViewModel, s: EditState) {
    val mapView = rememberMapView()
    val marker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            setInfoWindow(null)
            isDraggable = true
            setOnMarkerDragListener(object : Marker.OnMarkerDragListener {
                override fun onMarkerDrag(marker: Marker) = Unit
                override fun onMarkerDragStart(marker: Marker) = Unit
                override fun onMarkerDragEnd(marker: Marker) {
                    vm.setPosition(marker.position.latitude, marker.position.longitude)
                }
            })
        }
    }
    val tapOverlay = remember {
        MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                vm.setPosition(p.latitude, p.longitude)
                return true
            }

            override fun longPressHelper(p: GeoPoint): Boolean = false
        })
    }

    LaunchedEffect(s.recenterToken) {
        if (s.recenterToken > 0 && s.latitude != null && s.longitude != null) {
            mapView.controller.setZoom(15.0)
            mapView.controller.setCenter(GeoPoint(s.latitude, s.longitude))
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
        update = { map: MapView ->
            val lat = s.latitude
            val lon = s.longitude
            if (lat != null && lon != null) {
                marker.position = GeoPoint(lat, lon)
                marker.isEnabled = true
            } else {
                marker.isEnabled = false
            }
            map.invalidate()
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeciesField(text: String, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = remember(text) { SpeciesCatalog.search(text) }
    val showMenu = expanded && options.isNotEmpty()
    ExposedDropdownMenuBox(expanded = showMenu, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = text,
            onValueChange = {
                onChange(it)
                expanded = true
            },
            label = { Text(stringResource(R.string.species_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showMenu) },
            singleLine = true,
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryEditable)
                .fillMaxWidth()
                .testTag("species"),
        )
        ExposedDropdownMenu(expanded = showMenu, onDismissRequest = { expanded = false }) {
            options.forEach { species ->
                DropdownMenuItem(
                    text = { Text(species.displayName) },
                    onClick = {
                        onChange(species.displayName)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun pickDateTime(context: Context, initial: Long, onPicked: (Long) -> Unit) {
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
