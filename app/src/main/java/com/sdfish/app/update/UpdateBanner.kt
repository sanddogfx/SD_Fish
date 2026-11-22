package com.sdfish.app.update

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sdfish.app.R
import kotlinx.coroutines.launch

private enum class Step { Ready, Downloading, Failed }

/** Shows a card when a newer test build is available; one tap downloads and installs it. */
@Composable
fun UpdateBanner() {
    if (!Updater.enabled) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var update by remember { mutableStateOf<UpdateInfo?>(null) }
    var dismissed by remember { mutableStateOf(false) }
    var step by remember { mutableStateOf(Step.Ready) }
    val allowInstallHint = stringResource(R.string.update_allow_install)

    LaunchedEffect(Unit) { update = Updater.check() }

    val info = update
    if (info == null || dismissed) return

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.update_available, info.versionCode), style = MaterialTheme.typography.titleMedium)
            when (step) {
                Step.Downloading -> {
                    Text(stringResource(R.string.update_downloading), style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
                }
                else -> {
                    if (step == Step.Failed) {
                        Text(
                            stringResource(R.string.update_failed),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { dismissed = true }) { Text(stringResource(R.string.update_later)) }
                        Button(
                            onClick = {
                                if (!Updater.canInstall(context)) {
                                    Toast.makeText(context, allowInstallHint, Toast.LENGTH_LONG).show()
                                    Updater.openInstallPermissionSettings(context)
                                    return@Button
                                }
                                step = Step.Downloading
                                scope.launch {
                                    val apk = Updater.download(context, info)
                                    if (apk != null) {
                                        step = Step.Ready
                                        Updater.install(context, apk)
                                    } else {
                                        step = Step.Failed
                                    }
                                }
                            },
                        ) { Text(stringResource(R.string.update_now)) }
                    }
                }
            }
        }
    }
}
