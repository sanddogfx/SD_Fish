package com.sdfish.app.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.net.Uri
import com.sdfish.app.R
import com.sdfish.app.backup.Backup
import kotlinx.coroutines.launch

/** Save all data to one file (Google Drive or anywhere) and bring it back on a new phone. */
@Composable
fun BackupSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var working by remember { mutableStateOf(false) }
    var toRestore by remember { mutableStateOf<Uri?>(null) }
    val saved = stringResource(R.string.backup_saved)
    val saveFailed = stringResource(R.string.backup_failed)
    val restoreFailed = stringResource(R.string.restore_failed)

    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        working = true
        scope.launch {
            val ok = runCatching { Backup.export(context, uri) }.isSuccess
            working = false
            Toast.makeText(context, if (ok) saved else saveFailed, Toast.LENGTH_LONG).show()
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) toRestore = uri
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.backup_title), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.backup_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = { save.launch(Backup.suggestedName()) },
            enabled = !working,
            modifier = Modifier.fillMaxWidth().testTag("backup_save"),
        ) { Text(stringResource(R.string.backup_save)) }
        OutlinedButton(
            onClick = { open.launch(arrayOf("application/zip", "application/octet-stream", "application/x-zip-compressed")) },
            enabled = !working,
            modifier = Modifier.fillMaxWidth().testTag("backup_restore"),
        ) { Text(stringResource(R.string.backup_restore)) }
        if (working) LinearProgressIndicator(Modifier.fillMaxWidth())
    }

    toRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { toRestore = null },
            title = { Text(stringResource(R.string.restore_confirm_title)) },
            text = { Text(stringResource(R.string.restore_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    toRestore = null
                    working = true
                    scope.launch {
                        // On success the app restarts with the restored data and never gets here.
                        runCatching { Backup.restore(context, uri) }
                        working = false
                        Toast.makeText(context, restoreFailed, Toast.LENGTH_LONG).show()
                    }
                }) { Text(stringResource(R.string.restore_action)) }
            },
            dismissButton = { TextButton(onClick = { toRestore = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
