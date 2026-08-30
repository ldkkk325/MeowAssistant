package com.meow.assistant.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.meow.assistant.R
import com.meow.assistant.ui.LocalUiMode
import com.meow.assistant.ui.UiMode
import com.meow.assistant.update.ReleaseInfo
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog

@Composable
fun UpdatePrompt(
    release: ReleaseInfo,
    downloading: Boolean,
    error: String?,
    onDownload: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (LocalUiMode.current) {
        UiMode.Material -> AlertDialog(
            onDismissRequest = { if (!downloading) onDismiss() },
            icon = { Icon(Icons.Rounded.SystemUpdate, contentDescription = null) },
            title = { Text(stringResource(R.string.update_available_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.update_available_message, release.title, release.tagName))
                    error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = onDownload, enabled = !downloading) {
                    Text(stringResource(if (downloading) R.string.update_downloading else R.string.update_download))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss, enabled = !downloading) {
                    Text(stringResource(R.string.update_later))
                }
            },
        )

        UiMode.Miuix -> OverlayDialog(
            show = true,
            title = stringResource(R.string.update_available_title),
            summary = release.tagName,
            onDismissRequest = { if (!downloading) onDismiss() },
            content = {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    Text(stringResource(R.string.update_available_message, release.title, release.tagName))
                    error?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        top.yukonga.miuix.kmp.basic.TextButton(
                            text = stringResource(R.string.update_later),
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            enabled = !downloading,
                        )
                        top.yukonga.miuix.kmp.basic.TextButton(
                            text = stringResource(if (downloading) R.string.update_downloading else R.string.update_download),
                            onClick = onDownload,
                            modifier = Modifier.weight(1f),
                            enabled = !downloading,
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                        )
                    }
                }
            },
        )
    }
}
