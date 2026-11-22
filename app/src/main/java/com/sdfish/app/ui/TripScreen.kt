package com.sdfish.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sdfish.app.Formats
import com.sdfish.app.R
import com.sdfish.app.data.CatchEntry

/** Start a new trip (tripId = 0) or view and edit an existing one with its fish. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripScreen(
    tripId: Long,
    /** For a new trip: enter a trip that already happened instead of starting one now. */
    isPast: Boolean = false,
    onDone: () -> Unit,
    /** Called instead of [onDone] after a new trip is started. */
    onStarted: (tripId: Long) -> Unit,
    onAddFish: (tripId: Long) -> Unit,
    onOpenCatch: (Long) -> Unit,
) {
    val context = LocalContext.current
    val vm: TripViewModel = viewModel(factory = TripViewModel.factory(tripId, isPast))
    val s = vm.state
    val catches by vm.catches.collectAsStateWithLifecycle(initialValue = emptyList())
    val route by vm.route.collectAsStateWithLifecycle(initialValue = emptyList())
    val photos by vm.photos.collectAsStateWithLifecycle(initialValue = emptyList())
    var confirmDelete by remember { mutableStateOf(false) }

    // The route is recorded with a notification; ask once to show it (Android 13+), then start either way.
    val notificationRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.save(onStarted)
    }
    val startTrip = {
        val needsAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsAsk) notificationRequest.launch(Manifest.permission.POST_NOTIFICATIONS) else vm.save(onStarted)
    }

    val requestLocation = rememberLocationRequest(onGranted = vm::fetchLocation, onDenied = vm::markLocationDenied)
    LaunchedEffect(Unit) {
        if (vm.isNew && !vm.locationRequested) {
            vm.locationRequested = true
            requestLocation()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (vm.isNew) R.string.trip_title_new else R.string.trip_title)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (!vm.isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when {
                        vm.isNew && vm.isPast -> Button(
                            onClick = { vm.save(onStarted) },
                            modifier = Modifier.weight(1f).testTag("trip_add_past"),
                        ) { Text(stringResource(R.string.add_trip)) }
                        vm.isNew -> Button(
                            onClick = startTrip,
                            modifier = Modifier.weight(1f).testTag("trip_start"),
                        ) { Text(stringResource(R.string.start_fishing)) }
                        s.endTime == null -> {
                            OutlinedButton(onClick = { vm.save { onDone() } }, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.save))
                            }
                            Button(
                                onClick = { vm.finish { onDone() } },
                                modifier = Modifier.weight(1f).testTag("trip_finish"),
                            ) { Text(stringResource(R.string.end_fishing)) }
                        }
                        else -> Button(
                            onClick = { vm.save { onDone() } },
                            modifier = Modifier.weight(1f).testTag("trip_save"),
                        ) { Text(stringResource(R.string.save)) }
                    }
                }
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
            SectionTitle(stringResource(R.string.section_method))
            MethodPicker(selected = s.method, allowNone = false) { m -> if (m != null) vm.update { it.copy(method = m) } }

            SectionTitle(stringResource(R.string.section_place_only))
            PlaceSection(
                latitude = s.latitude,
                longitude = s.longitude,
                accuracy = s.accuracy,
                status = s.locationStatus,
                recenterToken = s.recenterToken,
                onPick = vm::setPosition,
                onRequestLocation = requestLocation,
                startLocked = !vm.isNew,
            )

            if (!vm.isNew || vm.isPast) {
                SectionTitle(stringResource(R.string.section_time))
                TimeRow(stringResource(R.string.trip_started), s.startTime) { t ->
                    pickDateTime(context, s.startTime, vm::setStart)
                }
                val end = s.endTime
                if (end != null) {
                    TimeRow(stringResource(R.string.trip_ended), end) {
                        pickDateTime(context, end, vm::setEnd)
                    }
                }
                Text(
                    stringResource(R.string.trip_duration, rememberElapsed(s.startTime, s.endTime)),
                    style = MaterialTheme.typography.bodyLarge,
                )
                when (val error = s.timeError) {
                    TripTimeError.EndBeforeStart -> ErrorText(stringResource(R.string.trip_end_before_start))
                    is TripTimeError.OutsideCatches -> ErrorText(
                        stringResource(R.string.trip_must_cover_catches, Formats.dateTime(error.first), Formats.dateTime(error.last))
                    )
                    null -> Unit
                }
            }

            if (!vm.isNew) {
                RouteSection(points = route, catches = catches, active = s.endTime == null)

                TripPhotosSection(
                    photos = photos,
                    onTaken = vm::addPhoto,
                    onPicked = vm::importPhotos,
                    onDelete = vm::deletePhoto,
                )
            }

            OutlinedTextField(
                value = s.note,
                onValueChange = { v -> vm.update { it.copy(note = v) } },
                label = { Text(stringResource(R.string.note_label)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            if (!vm.isNew) {
                SectionTitle(stringResource(R.string.trip_catches, catches.size))
                val totalWeight = catches.mapNotNull { it.weightKg }.takeIf { it.isNotEmpty() }?.sum()
                if (totalWeight != null) {
                    Text(
                        stringResource(R.string.trip_total_weight, Formats.number(totalWeight)),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.testTag("trip_total_weight"),
                    )
                }
                if (catches.isEmpty()) {
                    Text(
                        stringResource(R.string.trip_no_catches),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                catches.forEach { entry ->
                    TripCatchRow(entry) { onOpenCatch(entry.id) }
                    HorizontalDivider()
                }
                OutlinedButton(onClick = { onAddFish(tripId) }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.add_fish))
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_trip_title)) },
            text = { Text(stringResource(R.string.delete_trip_text)) },
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
private fun TimeRow(label: String, time: Long, onClick: (Long) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(96.dp), style = MaterialTheme.typography.bodyLarge)
        OutlinedButton(onClick = { onClick(time) }) {
            Icon(Icons.Filled.DateRange, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(Formats.dateTime(time))
        }
    }
}

@Composable
private fun TripCatchRow(entry: CatchEntry, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(speciesLabel(entry), style = MaterialTheme.typography.bodyLarge)
            val details = listOfNotNull(
                entry.weightKg?.let { stringResource(R.string.weight_kg, Formats.number(it)) },
                entry.lengthCm?.let { stringResource(R.string.length_cm, Formats.number(it)) },
            ).joinToString(" · ")
            if (details.isNotEmpty()) Text(details, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            Formats.time(entry.timestamp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
