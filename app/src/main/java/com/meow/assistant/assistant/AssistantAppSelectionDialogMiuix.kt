package com.meow.assistant.assistant

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.meow.assistant.R
import com.meow.assistant.ui.component.AppIconImage
import androidx.compose.ui.res.stringResource
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.CheckboxPreference

@Composable
internal fun AssistantAppSelectionDialogMiuix(
    show: Boolean,
    apps: List<SelectableApp>,
    selectedPackages: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filteredApps = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter {
            it.label.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true)
        }
    }
    OverlayDialog(
        show = show,
        title = stringResource(R.string.assistant_apps_title),
        summary = stringResource(R.string.assistant_apps_selected, selectedPackages.size),
        onDismissRequest = onDismiss,
        content = {
            Column(modifier = Modifier.heightIn(max = 560.dp)) {
                top.yukonga.miuix.kmp.basic.TextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true,
                    label = stringResource(R.string.assistant_apps_search),
                )
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        val checked = app.packageName in selectedPackages
                        val rowScale by animateFloatAsState(
                            targetValue = if (checked) 0.985f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                            label = "dialog_app_row_scale_miuix",
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .graphicsLayer {
                                    scaleX = rowScale
                                    scaleY = rowScale
                                }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppIconImage(
                                modifier = Modifier.size(42.dp).padding(end = 8.dp),
                                applicationInfo = app.applicationInfo,
                                label = app.label,
                            )
                            CheckboxPreference(
                                title = app.label,
                                summary = app.packageName,
                                checked = checked,
                                insideMargin = PaddingValues(vertical = 8.dp),
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
        },
    )
}
