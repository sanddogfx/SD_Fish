package com.sdfish.app.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.sdfish.app.Formats
import com.sdfish.app.R
import com.sdfish.app.data.CatchEntry
import com.sdfish.app.data.RouteMath
import com.sdfish.app.data.TrackPoint
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.roundToInt

/** The GPS route of a trip on a map, with its fish, and the distance covered. */
@Composable
fun RouteSection(points: List<TrackPoint>, catches: List<CatchEntry>, active: Boolean) {
    SectionTitle(stringResource(R.string.section_route))
    if (points.size < 2) {
        Text(
            stringResource(if (active) R.string.route_waiting else R.string.route_none),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    RouteMap(points, catches)
    Text(
        stringResource(R.string.route_distance, formatDistance(RouteMath.lengthMeters(points))),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.testTag("route_distance"),
    )
}

@Composable
fun formatDistance(meters: Double): String =
    if (meters < 1000) stringResource(R.string.distance_m, meters.roundToInt())
    else stringResource(R.string.distance_km, Formats.number((meters / 10).roundToInt() / 100.0))

@Composable
private fun RouteMap(points: List<TrackPoint>, catches: List<CatchEntry>) {
    val context = LocalContext.current
    val mapView = rememberMapView()
    val lineColor = MaterialTheme.colorScheme.primary.toArgb()
    val line = remember {
        Polyline(mapView).apply {
            outlinePaint.strokeWidth = 10f
            outlinePaint.color = lineColor
            setOnClickListener { _, _, _ -> true }
        }
    }
    var framed by remember { mutableStateOf(false) }

    LaunchedEffect(points, catches) {
        line.setPoints(points.map { GeoPoint(it.latitude, it.longitude) })
        mapView.overlays.clear()
        mapView.overlays.add(line)
        catches.filter { it.latitude != null && it.longitude != null }.forEach { c ->
            mapView.overlays.add(
                Marker(mapView).apply {
                    position = GeoPoint(c.latitude!!, c.longitude!!)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = ContextCompat.getDrawable(context, R.drawable.ic_map_pin)
                    setInfoWindow(null)
                }
            )
        }
        if (!framed) {
            framed = true
            val box = BoundingBox.fromGeoPoints(line.actualPoints)
            // Wait for the view to be laid out, then fit the whole route with a margin.
            mapView.post { mapView.zoomToBoundingBox(box.increaseByScale(1.3f), false) }
        }
        mapView.invalidate()
    }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(12.dp))
            .testTag("route_map"),
        factory = { mapView },
    )
}
