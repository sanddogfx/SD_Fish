package com.sdfish.app

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/** Catch and trip photos live in the app's private storage, under files/photos. */
object PhotoStorage {
    private fun dir(context: Context) = File(context.filesDir, "photos").apply { mkdirs() }

    fun newPhotoFile(context: Context, prefix: String = "catch"): File =
        // The random part keeps names unique when several pictures are copied in the same millisecond.
        File(dir(context), "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg")

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** Copies a picture picked from the gallery into app storage. */
    fun copyFromUri(context: Context, uri: Uri, prefix: String = "catch"): File? = runCatching {
        val file = newPhotoFile(context, prefix)
        context.contentResolver.openInputStream(uri)!!.use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        file
    }.getOrNull()

    fun delete(path: String?) {
        if (path != null) File(path).delete()
    }
}
