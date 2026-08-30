package com.meow.assistant.ui.screen.home

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meow.assistant.R
import com.meow.assistant.assistant.AssistantConfig
import com.meow.assistant.assistant.SelectableApp
import com.meow.assistant.ui.component.AppIconImage
import com.meow.assistant.ui.component.material.ExpressiveScaffold
import com.meow.assistant.ui.component.material.expressiveTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePagerMaterial(
    config: AssistantConfig,
    apps: List<SelectableApp>,
    onToggle: (Boolean) -> Unit,
    onSelectionChange: (Set<String>) -> Unit,
    bottomInnerPadding: Dp,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val listState = rememberLazyListState()
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(HomeAppCategory.User) }
    val filteredApps = remember(apps, category, query) { apps.filterForHomeList(category, query) }

    LaunchedEffect(expanded) {
        if (expanded) listState.animateScrollToItem(2)
    }

    ExpressiveScaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = expressiveTopAppBarColors(),
                scrollBehavior = scrollBehavior,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = bottomInnerPadding + 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item("assistant_status") { AssistantStatusCard(config, onToggle, Modifier.animateItem()) }
            item("system_info") { SystemInfoCard(Modifier.animateItem()) }
            item("apps_header") {
                AssistantAppsHeaderCard(
                    selectedCount = config.selectedPackages.size,
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.animateItem(),
                )
            }
            item("apps_controls") {
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn(tween(160)) +
                        slideInVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)) { -it / 5 } +
                        expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)),
                    exit = fadeOut(tween(120)) +
                        slideOutVertically(animationSpec = tween(160)) { -it / 6 } +
                        shrinkVertically(animationSpec = tween(180)),
                ) {
                    AssistantAppsControlsCard(
                        query = query,
                        onQueryChange = { query = it },
                        category = category,
                        onCategoryChange = { category = it },
                    )
                }
            }
            if (expanded) {
                if (filteredApps.isEmpty()) {
                    item("apps_empty") { EmptyAppsCard(Modifier.animateItem()) }
                } else {
                    items(filteredApps, key = { "app_${it.packageName}" }) { app ->
                        AppRowCard(
                            app = app,
                            checked = app.packageName in config.selectedPackages,
                            onCheckedChange = { checked ->
                                onSelectionChange(config.selectedPackages.toMutableSet().apply {
                                    if (checked) add(app.packageName) else remove(app.packageName)
                                })
                            },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
            item("bottom_spacer") { Spacer(Modifier.height(1.dp)) }
        }
    }
}

@Composable
private fun SystemInfoCard(modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth().smoothSize(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.home_system_info), style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.home_android_version), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Build.VERSION.RELEASE ?: Build.VERSION.SDK_INT.toString(), color = MaterialTheme.colorScheme.onSurface)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.home_device_model), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Build.MODEL.orEmpty().ifBlank { "—" }, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun AssistantStatusCard(config: AssistantConfig, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val iconScale by animateFloatAsState(
        targetValue = if (config.enabled) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "assistant_status_icon_scale",
    )
    Surface(modifier.fillMaxWidth().smoothSize(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Pets, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp).graphicsLayer {
                scaleX = iconScale
                scaleY = iconScale
            })
            Text(
                text = if (config.enabled) stringResource(R.string.home_assistant_enabled) else stringResource(R.string.home_assistant_disabled),
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                style = MaterialTheme.typography.titleMedium,
            )
            Switch(checked = config.enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun AssistantAppsHeaderCard(
    selectedCount: Int,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "apps_expand_rotation",
    )
    Surface(modifier.fillMaxWidth().smoothSize(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange(!expanded) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_assistant_apps), style = MaterialTheme.typography.titleMedium)
                Text(
                    if (selectedCount == 0) stringResource(R.string.home_assistant_apps_none) else stringResource(R.string.home_assistant_apps_count, selectedCount),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AnimatedContent(
                targetState = expanded,
                transitionSpec = {
                    fadeIn(tween(120)) togetherWith fadeOut(tween(90))
                },
                label = "apps_expand_icon",
            ) { isExpanded ->
                Icon(
                    if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.rotate(iconRotation),
                )
            }
        }
    }
}

@Composable
private fun AssistantAppsControlsCard(
    query: String,
    onQueryChange: (String) -> Unit,
    category: HomeAppCategory,
    onCategoryChange: (HomeAppCategory) -> Unit,
) {
    Surface(Modifier.fillMaxWidth().smoothSize(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.assistant_apps_search)) },
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryChip(stringResource(R.string.assistant_apps_user), category == HomeAppCategory.User) {
                    onCategoryChange(HomeAppCategory.User)
                }
                CategoryChip(stringResource(R.string.assistant_apps_system), category == HomeAppCategory.System) {
                    onCategoryChange(HomeAppCategory.System)
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(text) })
}

@Composable
private fun EmptyAppsCard(modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth().smoothSize(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Text(
            stringResource(R.string.assistant_apps_empty),
            Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AppRowCard(
    app: SelectableApp,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowScale by animateFloatAsState(
        targetValue = if (checked) 0.985f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "app_row_scale",
    )
    val containerColor = androidx.compose.animation.animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(220),
        label = "app_row_container",
    )
    Surface(
        modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = rowScale
                scaleY = rowScale
            },
        shape = MaterialTheme.shapes.medium,
        color = containerColor.value,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIconImage(Modifier.size(40.dp), app.applicationInfo, app.label)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(app.packageName, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

private fun Modifier.smoothSize(): Modifier =
    animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow))
