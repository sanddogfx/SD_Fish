package com.sdfish.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.sdfish.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val versionCode: Int, val title: String, val apkUrl: String)

/**
 * Self-update for test builds: finds the newest APK on the repository's GitHub
 * Releases page and installs it over the current app.
 * Only in debug builds; Google Play builds update through the Play Store.
 */
object Updater {
    private const val LATEST_RELEASE_URL = "https://api.github.com/repos/sanddogfx/SD_Fish/releases/latest"

    val enabled: Boolean get() = BuildConfig.DEBUG

    /** Returns the newer release, or null if there is none or it cannot be reached. */
    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            try {
                if (conn.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
                val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                // Release tags are "build-N", where N is the build's versionCode.
                val code = json.getString("tag_name").removePrefix("build-").toIntOrNull()
                    ?: return@runCatching null
                if (code <= BuildConfig.VERSION_CODE) return@runCatching null
                val assets = json.getJSONArray("assets")
                val apk = (0 until assets.length())
                    .map { assets.getJSONObject(it) }
                    .firstOrNull { it.getString("name").endsWith(".apk") }
                    ?: return@runCatching null
                UpdateInfo(code, json.optString("name"), apk.getString("browser_download_url"))
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    suspend fun download(context: Context, info: UpdateInfo): File? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "updates").apply {
                mkdirs()
                listFiles()?.forEach { it.delete() }
            }
            val file = File(dir, "SD_Fish-${info.versionCode}.apk")
            val conn = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 15_000
                readTimeout = 30_000
            }
            try {
                conn.inputStream.use { input -> file.outputStream().use { input.copyTo(it) } }
            } finally {
                conn.disconnect()
            }
            file
        }.getOrNull()
    }

    /** Android asks once per app to allow installing other apps. */
    fun canInstall(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
