package com.sdfish.app

import android.Manifest
import android.graphics.Bitmap
import android.os.Build
import com.sdfish.app.data.TrackPoint
import com.sdfish.app.data.TripPhoto
import com.sdfish.app.data.TripWeather
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextClearance
import com.sdfish.app.data.FishingTrip
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
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

    /** Fixed weather for the running trip, so the screenshot does not depend on the network. */
    private fun addSampleWeather() = runBlocking {
        val db = SdFishApp.from(InstrumentationRegistry.getInstrumentation().targetContext).database
        val trip = db.tripDao().observeActive().first() ?: return@runBlocking
        db.tripWeatherDao().upsert(
            TripWeather(
                tripId = trip.id, latitude = trip.latitude ?: 52.2, longitude = trip.longitude ?: 21.0, time = trip.startTime,
                temperatureC = 6.4, pressureHpa = 1012.6, pressureChangeHpa = -1.8, windSpeedMs = 3.6, windDirectionDeg = 300.0,
                cloudCoverPct = 70.0, precipitationMm = 0.0, weatherCode = 2, fetchedAt = System.currentTimeMillis(),
            )
        )
    }

    /** Two plain pictures standing in for photos of the fish just saved. */
    private fun addSampleFishPhotos(): Long = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = SdFishApp.from(context).database
        val fish = db.catchDao().observeAll().first().first()
        val paths = listOf(android.graphics.Color.rgb(90, 140, 90), android.graphics.Color.rgb(200, 170, 60)).map { color ->
            val bitmap = Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888)
            android.graphics.Canvas(bitmap).drawColor(color)
            val file = PhotoStorage.newPhotoFile(context, "catch")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            file.absolutePath
        }
        db.catchDao().upsert(fish.copy(photoPath = paths.first()))
        db.catchPhotoDao().replace(fish.id, paths)
        fish.id
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
        // Tapping the empty species field lists all species.
        tap("species")
        compose.waitForIdle()
        compose.onNodeWithTag("species_option_perch").assertExists()
        shot("04b_species_all")
        compose.onNodeWithTag("species").performTextInput("Szczu")
        shot("05_species_list")
        tap("species_option_pike")
        // With a species chosen, tapping again offers the whole list to change it.
        tap("species")
        compose.waitForIdle()
        compose.onNodeWithTag("species_option_perch").assertExists()
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
        val fishId = addSampleFishPhotos()
        shot("09_catch_list")
        // Opening the fish shows all its photos.
        compose.waitForIdle()
        compose.onNodeWithTag("swipe_catch_$fishId").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("catch_photos").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("catch_photos").performScrollTo()
        shot("09b_fish_photos", settleMs = 3000)
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }

        // Many small fish of one species go in one entry: count and total weight.
        tap("add_fish")
        compose.onNodeWithTag("species").performTextInput("Płoć")
        tap("species_option_roach")
        compose.onNodeWithTag("count").performTextClearance()
        compose.onNodeWithTag("count").performTextInput("12")
        compose.onNodeWithTag("weight").performTextInput("1,8")
        hideKeyboard()
        shot("09c_group_fish")
        tap("save")
        compose.onNodeWithText("× 12", substring = true, useUnmergedTree = true).assertExists()
        shot("09d_catch_list_group")

        // The emulator does not move, so add a short walk along the shore to show the route.
        addSampleRoute()
        addSamplePhoto()
        addSampleWeather()

        // Open the running trip, see its weather, fish and route, end it.
        tap("active_trip")
        shot("10_trip_running", settleMs = 3000)
        compose.onNodeWithTag("trip_weather").performScrollTo()
        shot("10a_trip_weather")
        compose.onNodeWithTag("route_map").performScrollTo()
        shot("10b_trip_route", settleMs = 5000)
        compose.onNodeWithTag("trip_photos").performScrollTo()
        shot("10c_trip_photos")
        compose.onNodeWithTag("trip_total_weight").performScrollTo()
        shot("10d_trip_weight")
        // Changing the place opens a full-screen map; Cancel keeps the old place.
        compose.onNodeWithTag("edit_place").performScrollTo().performClick()
        compose.onNodeWithTag("place_map_full").assertExists()
        shot("10e_place_full", settleMs = 4000)
        tap("place_cancel")
        tap("trip_finish")
        tap("tab_trips")
        shot("11_trip_list")

        // Enter a trip that already happened: start and end are picked, no timer.
        tap("add_past_trip")
        shot("11a_past_trip", settleMs = 4000)
        tap("trip_add_past")
        shot("11a2_trip_list_with_past")

        // Swipe a trip to the left: it asks before deleting. Keep it.
        val pastTripId = runBlocking {
            SdFishApp.from(InstrumentationRegistry.getInstrumentation().targetContext).database
                .tripDao().observeWithCounts().first().first { it.catchCount == 0 }.trip.id
        }
        // A short quick flick does nothing; only a long swipe asks to delete.
        compose.onNodeWithTag("swipe_trip_$pastTripId").performTouchInput {
            swipeLeft(startX = right - 10f, endX = right - 10f - width * 0.2f, durationMillis = 80)
        }
        compose.waitForIdle()
        compose.onNodeWithTag("confirm_delete").assertDoesNotExist()
        compose.onNodeWithTag("swipe_trip_$pastTripId").performTouchInput { swipeLeft() }
        shot("11a3_swipe_delete_trip")
        tap("cancel_delete")

        // A long list scrolled to the end: the last card stays clear of the buttons.
        val oldestTripId = runBlocking {
            val dao = SdFishApp.from(InstrumentationRegistry.getInstrumentation().targetContext).database.tripDao()
            val day = 24L * 60 * 60 * 1000
            val now = System.currentTimeMillis()
            (30..37).map { d ->
                dao.upsert(FishingTrip(startTime = now - d * day, endTime = now - d * day + 2 * 60 * 60 * 1000,
                    latitude = null, longitude = null, method = "spinning", note = ""))
            }.last()
        }
        compose.onNodeWithTag("trip_list").performScrollToNode(hasTestTag("swipe_trip_$oldestTripId"))
        compose.onNodeWithTag("trip_list").performTouchInput { swipeUp() }
        shot("11a4_long_list_end")
        val lastCard = compose.onNodeWithTag("swipe_trip_$oldestTripId").getUnclippedBoundsInRoot()
        val buttons = compose.onNodeWithTag("add_past_trip").getUnclippedBoundsInRoot()
        check(lastCard.bottom <= buttons.top) { "last card ${lastCard.bottom} overlaps buttons ${buttons.top}" }

        // Statistics: totals, records, species and methods for a period.
        tap("tab_stats")
        shot("11a5_stats_month")
        compose.onNodeWithTag("stats_fish").assertExists()
        tap("stats_period_all")
        compose.onNodeWithTag("stats").performTouchInput { swipeUp() }
        shot("11a6_stats_all_bottom")

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
        tap("tab_stats")
        shot("16b_stats_russian")

        tap("settings")
        tap("language_be")
        shot("17_belarusian", settleMs = 4000)
        tap("tab_catches")
        shot("18_catches_belarusian")

        // Dark is the default; switch to the light theme and back to Russian.
        tap("settings")
        tap("language_ru")
        compose.waitForIdle()
        Thread.sleep(3000)
        tap("settings")
        shot("19_settings_theme")
        tap("theme_light")
        shot("20_light_catches", settleMs = 4000)
        tap("tab_trips")
        shot("21_light_trips")
        tap("tab_map")
        shot("22_light_map", settleMs = 5000)
    }
}
