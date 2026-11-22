package com.sdfish.app

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/** Dark / light choice from the settings. Dark is the default: it saves battery on OLED screens. */
object AppTheme {
    enum class Mode(val key: String, val nightMode: Int) {
        SYSTEM("system", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
        DARK("dark", AppCompatDelegate.MODE_NIGHT_YES),
        LIGHT("light", AppCompatDelegate.MODE_NIGHT_NO),
    }

    private const val PREFS = "settings"
    private const val KEY = "theme"

    fun selected(context: Context): Mode {
        val key = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        return Mode.entries.firstOrNull { it.key == key } ?: Mode.DARK
    }

    /** Saves the choice and recreates open screens in it. */
    fun select(context: Context, mode: Mode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, mode.key).apply()
        AppCompatDelegate.setDefaultNightMode(mode.nightMode)
    }

    /** Called when the app starts. */
    fun apply(context: Context) {
        AppCompatDelegate.setDefaultNightMode(selected(context).nightMode)
    }
}
