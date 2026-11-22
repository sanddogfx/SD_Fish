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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
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
fun HomeScreen(onAdd: () -> Unit, onOpen: (Long) -> Unit) {
    val context = LocalContext.current
    val catchesFlow = remember { SdFishApp.from(context).database.catchDao().observeAll() }
    val catches by catchesFlow.collectAsStateWithLifecycle(initialValue = null)
    var tab by rememberSaveable { mutableIntStateOf(0) }

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
            })
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_catches)) },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    modifier = Modifier.testTag("tab_map"),
                    icon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                    label = { Text(stringResource(R.string.tab_map)) },
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                modifier = Modifier.testTag("add_catch"),
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_catch)) },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            UpdateBanner()
            Box(Modifier.weight(1f).fillMaxWidth()) {
                val list = catches
                when {
                    list == null -> Unit
                    tab == 0 -> CatchList(list, onOpen)
                    else -> CatchMap(list, onOpen)
                }
            }
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
