package com.sdfish.app

import android.Manifest
import android.graphics.Bitmap
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
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
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

        // Open the running trip, see its fish, end it.
        tap("active_trip")
        shot("10_trip_running", settleMs = 3000)
        tap("trip_finish")
        tap("tab_trips")
        shot("11_trip_list")

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
