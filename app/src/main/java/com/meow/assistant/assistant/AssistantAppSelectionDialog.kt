package com.meow.assistant.assistant

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.meow.assistant.R
import com.meow.assistant.ui.component.AppIconImage
import com.meow.assistant.ui.LocalUiMode
import com.meow.assistant.ui.UiMode

@Composable
fun AssistantAppSelectionDialog(
    show: Boolean,
    apps: List<SelectableApp>,
    selectedPackages: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!show) return
    if (LocalUiMode.current == UiMode.Miuix) {
        AssistantAppSelectionDialogMiuix(show, apps, selectedPackages, onSelectionChange, onDismiss)
        return
    }
    var query by remember { mutableStateOf("") }
    val filteredApps = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter {
            it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.assistant_apps_title), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.assistant_apps_selected, selectedPackages.size), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = null) }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text(stringResource(R.string.assistant_apps_search)) },
            )
            if (filteredApps.isEmpty()) {
                Text(stringResource(R.string.assistant_apps_empty), modifier = Modifier.padding(vertical = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val checked = app.packageName in selectedPackages
                        val rowScale by animateFloatAsState(
                            targetValue = if (checked) 0.985f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                            label = "dialog_app_row_scale",
                        )
                        val labelColor by animateColorAsState(
                            targetValue = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            animationSpec = tween(180),
                            label = "dialog_app_label_color",
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .graphicsLayer {
                                    scaleX = rowScale
                                    scaleY = rowScale
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppIconImage(
                                modifier = Modifier.size(44.dp).padding(end = 10.dp),
                                applicationInfo = app.applicationInfo,
                                label = app.label,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(app.label, color = labelColor)
                                Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { checked ->
                                    val updated = selectedPackages.toMutableSet()
                                    if (checked) updated += app.packageName else updated -= app.packageName
                                    onSelectionChange(updated)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
