package com.sdfish.app.backup

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.sdfish.app.BuildConfig
import com.sdfish.app.SdFishApp
import com.sdfish.app.data.AppDatabase
import com.sdfish.app.route.RouteService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * A backup is one zip file: manifest.json, the database and every photo. The person
 * saves it wherever they like through the system file picker (Google Drive included),
 * so the app needs no account, key or server of its own.
 */
object Backup {
    const val FORMAT = 1
    private const val MANIFEST = "manifest.json"
    private const val DB_ENTRY = "db/sdfish.db"
    private const val PHOTOS = "photos/"

    class NotABackup(message: String) : Exception(message)

    fun suggestedName(now: Long = System.currentTimeMillis()): String =
        "Loopcast-backup-" + SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now)) + ".zip"

    fun photosDir(context: Context) = File(context.filesDir, "photos")

    suspend fun export(context: Context, out: Uri) = withContext(Dispatchers.IO) {
        val db = SdFishApp.from(context).database
        context.contentResolver.openOutputStream(out)!!.use {
            write(db, context.getDatabasePath(AppDatabase.NAME), photosDir(context), it)
        }
    }

    /** Writes the backup of [db] (stored in [dbFile]) and the photos in [photosDir]. */
    fun write(db: AppDatabase, dbFile: File, photosDir: File, out: OutputStream) {
        // Move everything from the write-ahead log into the main file, so one file holds all data.
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
        val manifest = JSONObject()
            .put("format", FORMAT)
            .put("app", "LoopcastFishing")
            .put("appVersion", BuildConfig.VERSION_NAME)
            .put("dbVersion", db.openHelper.readableDatabase.version)
            .put("createdAt", System.currentTimeMillis())
            .put("photosDir", photosDir.absolutePath)
        ZipOutputStream(out.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST))
            zip.write(manifest.toString(2).toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry(DB_ENTRY))
            dbFile.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
            photosDir.listFiles()?.filter { it.isFile }?.forEach { photo ->
                zip.putNextEntry(ZipEntry(PHOTOS + photo.name))
                photo.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    /**
     * Replaces all data on this phone with the backup in [input], then restarts the app.
     * Throws [NotABackup] before touching anything if the file is not a usable backup.
     */
    suspend fun restore(context: Context, input: Uri) = withContext(Dispatchers.IO) {
        val app = SdFishApp.from(context)
        val staged = context.contentResolver.openInputStream(input)!!.use { stage(it, File(context.cacheDir, "restore")) }
        if (staged.dbVersion > app.database.openHelper.readableDatabase.version) {
            staged.dir.deleteRecursively()
            throw NotABackup("made by a newer version")
        }
        RouteService.stop(context)
        app.database.close()
        install(staged, context.getDatabasePath(AppDatabase.NAME), photosDir(context))
        restartApp(context)
    }

    class Staged(val dir: File, val dbVersion: Int, val oldPhotosDir: String)

    /** Unpacks [input] into [dir] and checks it is a backup of this app. */
    fun stage(input: InputStream, dir: File): Staged {
        dir.deleteRecursively()
        File(dir, "photos").mkdirs()
        var manifest: JSONObject? = null
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name
                if (entry.isDirectory || name.contains("..")) continue
                when {
                    name == MANIFEST -> manifest = JSONObject(zip.readBytes().decodeToString())
                    name == DB_ENTRY -> File(dir, "db").outputStream().use { zip.copyTo(it) }
                    name.startsWith(PHOTOS) && !name.removePrefix(PHOTOS).contains('/') ->
                        File(dir, "photos/" + name.removePrefix(PHOTOS)).outputStream().use { zip.copyTo(it) }
                }
            }
        }
        val m = manifest
        if (m == null || m.optInt("format") != FORMAT || m.optString("app") != "LoopcastFishing" || !File(dir, "db").isFile) {
            dir.deleteRecursively()
            throw NotABackup("not a Loopcast Fishing backup")
        }
        return Staged(dir, m.optInt("dbVersion"), m.optString("photosDir"))
    }

    /** Puts a staged backup in place of the database [dbFile] and the photos in [photosDir]. */
    fun install(staged: Staged, dbFile: File, photosDir: File) {
        listOf("", "-wal", "-shm", "-journal").forEach { File(dbFile.path + it).delete() }
        File(staged.dir, "db").copyTo(dbFile, overwrite = true)
        photosDir.deleteRecursively()
        photosDir.mkdirs()
        File(staged.dir, "photos").listFiles()?.forEach { it.copyTo(File(photosDir, it.name), overwrite = true) }
        // Photos are stored by full path; on another phone the folder may differ.
        val old = staged.oldPhotosDir
        val new = photosDir.absolutePath
        if (old.isNotEmpty() && old != new) {
            SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READWRITE).use { d ->
                listOf("catches" to "photoPath", "catch_photos" to "path", "trip_photos" to "path").forEach { (table, column) ->
                    runCatching {
                        d.execSQL(
                            "UPDATE $table SET $column = ? || substr($column, ?) WHERE $column LIKE ?",
                            arrayOf<Any>(new, old.length + 1, "$old%"),
                        )
                    }
                }
            }
        }
        staged.dir.deleteRecursively()
    }

    /** Starts the app afresh so it opens the restored database. */
    private fun restartApp(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)!!
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        // The app is in front, so it may start its screen; the system brings it up in a new process.
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }
}
