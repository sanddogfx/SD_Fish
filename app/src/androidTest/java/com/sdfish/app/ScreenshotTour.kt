package com.sdfish.app

import android.Manifest
import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Walks through the main screens like a user would and saves a screenshot of each.
 * CI runs it on an emulator, so every build doubles as a "does the app start and
 * save a catch" check, and the screenshots are attached to the build.
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

    @Test
    fun tour() {
        shot("01_home")

        compose.onNodeWithTag("add_catch").performClick()
        shot("02_new_catch", settleMs = 6000) // time for the GPS fix and map tiles

        compose.onNodeWithTag("species").performTextInput("Szczu")
        shot("03_species_list")
        compose.onNodeWithTag("species_option_pike").performClick()

        compose.onNodeWithTag("weight").performTextInput("2,4")
        compose.onNodeWithTag("length").performTextInput("68")
        Espresso.closeSoftKeyboard()
        shot("04_new_catch_filled")

        compose.onNodeWithTag("note").performScrollTo()
        shot("05_new_catch_bottom", settleMs = 3000)

        compose.onNodeWithTag("save").performClick()
        shot("06_list_with_catch")

        compose.onNodeWithTag("tab_map").performClick()
        shot("07_map", settleMs = 6000)

        // Switch the app to Russian in settings; the screen is recreated in the new language.
        compose.onNodeWithTag("settings").performClick()
        shot("08_settings")
        compose.onNodeWithTag("language_ru").performClick()
        shot("09_map_russian", settleMs = 4000)
        compose.onNodeWithTag("add_catch").performClick()
        shot("10_new_catch_russian", settleMs = 4000)
    }
}
