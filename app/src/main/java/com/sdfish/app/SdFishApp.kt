package com.sdfish.app

import android.app.Application
import android.content.Context
import com.sdfish.app.data.AppDatabase
import org.osmdroid.config.Configuration
import java.io.File

class SdFishApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.create(this) }

    override fun onCreate() {
        super.onCreate()
        AppTheme.apply(this)
        // OpenStreetMap tiles: identify the app and keep the tile cache in app storage,
        // so no storage permission is needed and viewed areas work offline later.
        val osm = Configuration.getInstance()
        osm.load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        osm.userAgentValue = "LoopcastFishing/${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})"
        val base = File(filesDir, "osmdroid")
        osm.osmdroidBasePath = base
        osm.osmdroidTileCache = File(base, "tiles")
    }

    companion object {
        fun from(context: Context): SdFishApp = context.applicationContext as SdFishApp
    }
}
