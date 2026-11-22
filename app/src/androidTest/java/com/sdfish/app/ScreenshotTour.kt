package com.sdfish.app

import android.Manifest
import android.graphics.Bitmap
import android.os.Build
import com.sdfish.app.data.TrackPoint
import com.sdfish.app.data.TripPhoto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Walks through the main screens like a user would and saves a screenshot of each.
 * CI runs it on an emulator, so every build doubles as a "does the app start, run a
 * fishing trip and save a fish" check, and the screenshots are attached to the build.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotTour {

    @get:Rule(order = 0)
    val permissions: GrantPermissionRule = GrantPermissionRule.grant(
        *listOfNotNull(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            if (Build.VERSION.SDK_INT >= 33) Manifest.permission.POST_NOTIFICATIONS else null,
        ).toTypedArray()
    )

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    private fun shot(name: String, settleMs: Long = 1500) {
        compose.waitForIdle()
        Thread.sleep(settleMs)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        val dir = File(instrumentation.targetContext.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun hideKeyboard() {
        compose.runOnUiThread {
            val window = compose.activity.window
            WindowCompat.getInsetsController(window, window.decorView).hide(WindowInsetsCompat.Type.ime())
        }
    }

    private fun addSampleRoute() = runBlocking {
        val db = SdFishApp.from(InstrumentationRegistry.getInstrumentation().targetContext).database
        val trip = db.tripDao().observeActive().first() ?: return@runBlocking
        val now = System.currentTimeMillis()
        listOf(
            52.2297 to 21.0122, 52.2301 to 21.0131, 52.2306 to 21.0139, 52.2310 to 21.0150,
            52.2316 to 21.0157, 52.2321 to 21.0166, 52.2324 to 21.0178, 52.2330 to 21.0185,
        ).forEachIndexed { i, (lat, lon) ->
            db.trackDao().insert(TrackPoint(tripId = trip.id, time = now + i * 60_000L, latitude = lat, longitude = lon, accuracyMeters = 5f))
        }
    }

    /** A plain picture standing in for a photo of the spot, since the emulator camera shows nothing useful. */
    private fun addSamplePhoto() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = SdFishApp.from(context).database
        val trip = db.tripDao().observeActive().first() ?: return@runBlocking
        val bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.rgb(120, 170, 210))
        val paint = android.graphics.Paint().apply { color = android.graphics.Color.rgb(60, 120, 70) }
        canvas.drawRect(0f, 380f, 800f, 600f, paint)
        val file = PhotoStorage.newPhotoFile(context, "trip")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        db.tripPhotoDao().insert(TripPhoto(tripId = trip.id, path = file.absolutePath, addedAt = System.currentTimeMillis()))
    }

    private fun tap(tag: String) = compose.onNodeWithTag(tag).performClick()

    @Test
    fun tour() {
        shot("01_home_trips")

        // Start a trip: place from GPS, method feeder.
        tap("start_trip")
        shot("02_trip_new", settleMs = 6000)
        tap("method")
        shot("02b_method_list")
        tap("method_feeder")
        tap("trip_start")
        shot("03_home_active_trip", settleMs = 3000)

        // Add a fish during the trip: place and method come from the trip.
        tap("add_fish")
        shot("04_fish_in_trip", settleMs = 4000)
        compose.onNodeWithTag("species").performTextInput("Szczu")
        shot("05_species_list")
        tap("species_option_pike")
        compose.onNodeWithTag("weight").performTextInput("2,4")
        compose.onNodeWithTag("length").performTextInput("68")
        hideKeyboard()
        shot("06_fish_filled")
        compose.onNodeWithTag("note").performScrollTo()
        shot("07_fish_bottom", settleMs = 3000)
        tap("save")
        shot("08_home_after_fish")

        tap("tab_catches")
        shot("09_catch_list")

        // The emulator does not move, so add a short walk along the shore to show the route.
        addSampleRoute()
        addSamplePhoto()

        // Open the running trip, see its fish and route, end it.
        tap("active_trip")
        shot("10_trip_running", settleMs = 3000)
        compose.onNodeWithTag("route_map").performScrollTo()
        shot("10b_trip_route", settleMs = 5000)
        compose.onNodeWithTag("trip_photos").performScrollTo()
        shot("10c_trip_photos")
        compose.onNodeWithTag("trip_total_weight").performScrollTo()
        shot("10d_trip_weight")
        tap("trip_finish")
        tap("tab_trips")
        shot("11_trip_list")

        // Enter a trip that already happened: start and end are picked, no timer.
        tap("add_past_trip")
        shot("11a_past_trip", settleMs = 4000)
        tap("trip_add_past")
        shot("11a2_trip_list_with_past")

        // No trip running: adding a fish asks for a trip. Start a new one; the fish form opens right after.
        tap("tab_catches")
        tap("add_catch")
        shot("11b_choose_trip")
        tap("choose_new_trip")
        tap("trip_start")
        shot("11c_fish_after_new_trip", settleMs = 4000)
        tap("save")
        tap("active_trip")
        tap("trip_finish")

        tap("tab_map")
        shot("12_map", settleMs = 6000)

        // Switch the app to Russian in settings; the screen is recreated in the new language.
        tap("settings")
        shot("13_settings")
        tap("language_ru")
        shot("14_map_russian", settleMs = 4000)
        tap("tab_trips")
        shot("15_trips_russian")
        tap("tab_catches")
        shot("16_catches_russian")

        tap("settings")
        tap("language_be")
        shot("17_belarusian", settleMs = 4000)
        tap("tab_catches")
        shot("18_catches_belarusian")
    }
}
