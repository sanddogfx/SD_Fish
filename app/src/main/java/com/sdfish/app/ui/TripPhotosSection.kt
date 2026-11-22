package com.sdfish.app.ui

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.sdfish.app.PhotoStorage
import com.sdfish.app.R
import com.sdfish.app.data.TripPhoto
import java.io.File

/** Photos of a trip: a row of thumbnails, camera and gallery buttons, tap a photo to view or delete it. */
@Composable
fun TripPhotosSection(
    photos: List<TripPhoto>,
    onTaken: (String) -> Unit,
    onPicked: (List<android.net.Uri>) -> Unit,
    onDelete: (TripPhoto) -> Unit,
) {
    val context = LocalContext.current
    var pendingPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    var viewing by remember { mutableStateOf<TripPhoto?>(null) }
    val noCamera = stringResource(R.string.no_camera)
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        pendingPhoto?.let { path -> if (ok) onTaken(path) else File(path).delete() }
        pendingPhoto = null
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_PICK)) { uris ->
        if (uris.isNotEmpty()) onPicked(uris)
    }

    SectionTitle(stringResource(R.string.trip_photos, photos.size))
    if (photos.isNotEmpty()) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("trip_photos")) {
            items(photos, key = { it.id }) { photo ->
                AsyncImage(
                    model = File(photo.path),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(112.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { viewing = photo },
                )
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = {
            val file = PhotoStorage.newPhotoFile(context, "trip")
            pendingPhoto = file.absolutePath
            try {
                cameraLauncher.launch(PhotoStorage.uriFor(context, file))
            } catch (e: ActivityNotFoundException) {
                pendingPhoto = null
                Toast.makeText(context, noCamera, Toast.LENGTH_SHORT).show()
            }
        }) { Text(stringResource(R.string.take_photo)) }
        OutlinedButton(onClick = {
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }) { Text(stringResource(R.string.pick_photo)) }
    }

    viewing?.let { photo ->
        Dialog(onDismissRequest = { viewing = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(16.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    AsyncImage(
                        model = File(photo.path),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp),
                    )
                    Row(Modifier.padding(8.dp)) {
                        TextButton(onClick = { onDelete(photo); viewing = null }) {
                            Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                        }
                        TextButton(onClick = { viewing = null }) { Text(stringResource(R.string.close)) }
                    }
                }
            }
        }
    }
}

private const val MAX_PICK = 10
