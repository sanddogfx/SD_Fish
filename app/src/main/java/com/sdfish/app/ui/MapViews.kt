package com.sdfish.app.ui

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView

/** Centre of Poland, used before we know where the user is. */
val DEFAULT_MAP_CENTER = GeoPoint(52.07, 19.48)
const val DEFAULT_MAP_ZOOM = 6.0

/**
 * A map that keeps its touch gestures when placed inside a scrolling screen,
 * so dragging the map moves the map, not the page.
 */
@SuppressLint("ViewConstructor")
class NestedMapView(context: Context) : MapView(context) {
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            parent?.requestDisallowInterceptTouchEvent(true)
        }
        return super.dispatchTouchEvent(event)
    }
}

/** Creates an OpenStreetMap view tied to the screen's lifecycle. */
@Composable
fun rememberMapView(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        NestedMapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            minZoomLevel = 3.0
            controller.setZoom(DEFAULT_MAP_ZOOM)
            controller.setCenter(DEFAULT_MAP_CENTER)
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }
    return mapView
}
