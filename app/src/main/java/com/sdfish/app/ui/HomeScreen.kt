package com.sdfish.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.rememberCoroutineScope
import com.sdfish.app.data.FishingMethods
import com.sdfish.app.data.FishingTrip
import com.sdfish.app.data.TripWithCount
import kotlinx.coroutines.launch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.runtime.mutableStateOf
import com.sdfish.app.AppLanguage
import com.sdfish.app.AppTheme
import com.sdfish.app.ui.theme.GradientFab
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sdfish.app.BuildConfig
import com.sdfish.app.Formats
import com.sdfish.app.route.RouteService
import com.sdfish.app.R
import com.sdfish.app.SdFishApp
import com.sdfish.app.data.CatchEntry
import com.sdfish.app.data.SpeciesCatalog
import com.sdfish.app.hasLocationPermission
import com.sdfish.app.update.UpdateBanner
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpen: (Long) -> Unit,
    onStartTrip: () -> Unit,
    /** Starts a new trip and then adds a fish to it. */
    onStartTripForFish: () -> Unit,
    /** Enters a trip that already happened. */
    onAddPastTrip: () -> Unit,
    onOpenTrip: (Long) -> Unit,
    onAddFishToTrip: (Long) -> Unit,
) {
    val context = LocalContext.current
    val db = remember { SdFishApp.from(context).database }
    val catchesFlow = remember { db.catchDao().observeAll() }
    val catches by catchesFlow.collectAsStateWithLifecycle(initialValue = null)
    val tripsFlow = remember { db.tripDao().observeWithCounts() }
    val trips by tripsFlow.collectAsStateWithLifecycle(initialValue = null)
    val activeFlow = remember { db.tripDao().observeActive() }
    val activeTrip by activeFlow.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var chooseTrip by rememberSaveable { mutableStateOf(false) }

    // Keep recording the route of a running trip, also after the phone restarted the app.
    val activeId = activeTrip?.id
    LaunchedEffect(activeId) {
        if (activeId != null) RouteService.start(context, activeId)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text(stringResource(R.string.app_name))
                    Text(
                        stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }, actions = {
                IconButton(onClick = { showSettings = true }, modifier = Modifier.testTag("settings")) {
                    Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                }
            })
        },
        bottomBar = {
            val navColors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    colors = navColors,
                    modifier = Modifier.testTag("tab_trips"),
                    icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_trips)) },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    colors = navColors,
                    modifier = Modifier.testTag("tab_catches"),
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_catches)) },
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    colors = navColors,
                    modifier = Modifier.testTag("tab_map"),
                    icon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_map)) },
                )
            }
        },
        floatingActionButton = {
            val active = activeTrip
            when {
                active != null -> GradientFab(
                    text = stringResource(R.string.add_fish),
                    icon = Icons.Filled.Add,
                    onClick = { onAddFishToTrip(active.id) },
                    modifier = Modifier.testTag("add_fish"),
                )
                tab == 1 -> GradientFab(
                    // Every fish belongs to a trip: ask which one, or start a new one.
                    text = stringResource(R.string.add_catch),
                    icon = Icons.Filled.Add,
                    onClick = { chooseTrip = true },
                    modifier = Modifier.testTag("add_catch"),
                )
                else -> Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ExtendedFloatingActionButton(
                        onClick = onAddPastTrip,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.testTag("add_past_trip"),
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text(stringResource(R.string.add_trip)) },
                    )
                    GradientFab(
                        text = stringResource(R.string.start_fishing),
                        icon = Icons.Filled.PlayArrow,
                        onClick = onStartTrip,
                        modifier = Modifier.testTag("start_trip"),
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            UpdateBanner()
            activeTrip?.let { trip ->
                ActiveTripCard(
                    trip = trip,
                    onOpen = { onOpenTrip(trip.id) },
                    onFinish = {
                        RouteService.stop(context)
                        scope.launch { db.tripDao().upsert(trip.copy(endTime = System.currentTimeMillis())) }
                    },
                )
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                val list = catches
                val tripList = trips
                when {
                    list == null || tripList == null -> Unit
                    tab == 0 -> TripList(tripList, onOpenTrip)
                    tab == 1 -> CatchList(list, onOpen)
                    else -> CatchMap(list, onOpen)
                }
            }
        }
    }

    if (showSettings) SettingsDialog(onDismiss = { showSettings = false })
    if (chooseTrip) {
        ChooseTripDialog(
            trips = trips.orEmpty().map { it.trip },
            onNewTrip = { chooseTrip = false; onStartTripForFish() },
            onPick = { id -> chooseTrip = false; onAddFishToTrip(id) },
            onDismiss = { chooseTrip = false },
        )
    }
}

/** Asks which trip a new fish belongs to: a new trip or one of the recent ones. */
@Composable
private fun ChooseTripDialog(
    trips: List<FishingTrip>,
    onNewTrip: () -> Unit,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_trip_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Button(onClick = onNewTrip, modifier = Modifier.fillMaxWidth().testTag("choose_new_trip")) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.trip_title_new))
                }
                if (trips.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.choose_trip_existing), style = MaterialTheme.typography.titleSmall)
                    trips.take(RECENT_TRIPS).forEach { trip ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .testTag("choose_trip_${trip.id}")
                                .clickable { onPick(trip.id) }
                                .padding(vertical = 10.dp),
                        ) {
                            Text(Formats.dateTime(trip.startTime), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                FishingMethods.displayName(trip.method),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

private const val RECENT_TRIPS = 10

/** App settings: language and dark / light theme. */
@Composable
private fun SettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val selected = AppLanguage.selectedTag()
    val options = listOf("") + AppLanguage.supported
    val selectedTheme = AppTheme.selected(context)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.theme), style = MaterialTheme.typography.titleSmall)
                AppTheme.Mode.entries.forEach { mode ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("theme_${mode.key}")
                            .selectable(selected = mode == selectedTheme, role = Role.RadioButton) {
                                onDismiss()
                                // Changing the theme recreates the screen in the new colors.
                                if (mode != selectedTheme) AppTheme.select(context, mode)
                            }
                            .padding(vertical = 8.dp),
                    ) {
                        RadioButton(selected = mode == selectedTheme, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            stringResource(
                                when (mode) {
                                    AppTheme.Mode.SYSTEM -> R.string.language_system
                                    AppTheme.Mode.DARK -> R.string.theme_dark
                                    AppTheme.Mode.LIGHT -> R.string.theme_light
                                }
                            )
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.language), style = MaterialTheme.typography.titleSmall)
                options.forEach { tag ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("language_${tag.ifEmpty { "system" }}")
                            .selectable(selected = tag == selected, role = Role.RadioButton) {
                                onDismiss()
                                // Changing the language recreates the screen in the new language.
                                if (tag != selected) AppLanguage.select(tag)
                            }
                            .padding(vertical = 8.dp),
                    ) {
                        RadioButton(selected = tag == selected, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(if (tag.isEmpty()) stringResource(R.string.language_system) else AppLanguage.nativeName(tag))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

@Composable
private fun ActiveTripCard(trip: FishingTrip, onOpen: () -> Unit, onFinish: () -> Unit) {
    Card(
        onClick = onOpen,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("active_trip"),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.active_trip), style = MaterialTheme.typography.titleMedium)
                Text(FishingMethods.displayName(trip.method), style = MaterialTheme.typography.bodyMedium)
                Text(
                    rememberElapsed(trip.startTime),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.testTag("trip_timer"),
                )
            }
            Button(onClick = onFinish, modifier = Modifier.testTag("finish_trip")) {
                Icon(Icons.Filled.Done, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.end_fishing))
            }
        }
    }
}

@Composable
private fun TripList(trips: List<TripWithCount>, onOpen: (Long) -> Unit) {
    if (trips.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.trips_empty),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(trips, key = { it.trip.id }) { item ->
            TripCard(item, onClick = { onOpen(item.trip.id) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripCard(item: TripWithCount, onClick: () -> Unit) {
    val trip = item.trip
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(Formats.dateTime(trip.startTime), style = MaterialTheme.typography.titleMedium)
            Text(
                FishingMethods.displayName(trip.method),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val duration = if (trip.isActive) stringResource(R.string.trip_in_progress)
            else Formats.duration(trip.endTime!! - trip.startTime)
            Text(
                listOfNotNull(
                    duration,
                    stringResource(R.string.fish_count, item.catchCount),
                    item.totalWeightKg?.let { stringResource(R.string.weight_kg, Formats.number(it)) },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun speciesLabel(entry: CatchEntry): String =
    if (entry.species.isBlank()) stringResource(R.string.species_unknown)
    else SpeciesCatalog.displayName(entry.species)

@Composable
private fun CatchList(catches: List<CatchEntry>, onOpen: (Long) -> Unit) {
    if (catches.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.empty_list),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(catches, key = { it.id }) { entry ->
            CatchCard(entry, onClick = { onOpen(entry.id) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatchCard(entry: CatchEntry, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            val thumb = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
            if (entry.photoPath != null) {
                AsyncImage(
                    model = File(entry.photoPath),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = thumb,
                )
            } else {
                Box(
                    thumb.background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) { Text("🐟", fontSize = 28.sp) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(speciesLabel(entry), style = MaterialTheme.typography.titleMedium)
                Text(
                    Formats.dateTime(entry.timestamp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val details = listOfNotNull(
                    entry.weightKg?.let { stringResource(R.string.weight_kg, Formats.number(it)) },
                    entry.lengthCm?.let { stringResource(R.string.length_cm, Formats.number(it)) },
                ).joinToString(" · ")
                if (details.isNotEmpty()) {
                    Text(details, style = MaterialTheme.typography.bodyMedium)
                }
                if (entry.latitude == null) {
                    Text(
                        stringResource(R.string.no_coords),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun CatchMap(catches: List<CatchEntry>, onOpen: (Long) -> Unit) {
    val context = LocalContext.current
    val mapView = rememberMapView()
    val centered = remember { booleanArrayOf(false) }
    val tapAgain = stringResource(R.string.tap_again_to_open)
    val labels = catches.associate { it.id to speciesLabel(it) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            mapView.apply {
                if (hasLocationPermission(context) && overlays.none { it is MyLocationNewOverlay }) {
                    overlays.add(
                        MyLocationNewOverlay(GpsMyLocationProvider(context), this).apply { enableMyLocation() }
                    )
                }
            }
        },
        update = { map ->
            map.overlays.removeAll { it is Marker }
            catches.forEach { entry ->
                val lat = entry.latitude ?: return@forEach
                val lon = entry.longitude ?: return@forEach
                map.overlays.add(
                    Marker(map).apply {
                        position = GeoPoint(lat, lon)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        icon = ContextCompat.getDrawable(context, R.drawable.ic_map_pin)
                        title = labels[entry.id]
                        snippet = Formats.dateTime(entry.timestamp)
                        subDescription = tapAgain
                        setOnMarkerClickListener { marker, mv ->
                            if (marker.isInfoWindowShown) {
                                onOpen(entry.id)
                            } else {
                                InfoWindow.closeAllInfoWindowsOn(mv)
                                marker.showInfoWindow()
                            }
                            true
                        }
                    }
                )
            }
            if (!centered[0]) {
                // Start at the latest catch with coordinates.
                catches.firstOrNull { it.latitude != null && it.longitude != null }?.let {
                    map.controller.setZoom(12.0)
                    map.controller.setCenter(GeoPoint(it.latitude!!, it.longitude!!))
                }
                centered[0] = true
            }
            map.invalidate()
        },
    )
}
