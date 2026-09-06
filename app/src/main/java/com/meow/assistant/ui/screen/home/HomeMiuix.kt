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
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
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
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.meow.assistant.ui.component.GlassSwitch
import com.meow.assistant.ui.theme.LocalEnableBlur
import com.meow.assistant.ui.theme.LocalEnableGlassSwitch
import com.meow.assistant.ui.util.BlurredBar
import com.meow.assistant.ui.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ExpandLess
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun HomePagerMiuix(
    config: AssistantConfig,
    apps: List<SelectableApp>,
    onToggle: (Boolean) -> Unit,
    onSelectionChange: (Set<String>) -> Unit,
    bottomInnerPadding: Dp,
) {
    val scrollBehavior = MiuixScrollBehavior()
    // 顶栏毛玻璃：和主题设置页同一套（采样整页内容 + surface 染色）。顶部内边距必须放进
    // contentPadding，让列表能滚到顶栏底下，否则玻璃底下永远是空的纯色。
    val backdrop = rememberBlurBackdrop(LocalEnableBlur.current)
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else colorScheme.surface
    val listState = rememberLazyListState()
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(HomeAppCategory.User) }
    val filteredApps = remember(apps, category, query) { apps.filterForHomeList(category, query) }

    LaunchedEffect(expanded) {
        if (expanded) listState.animateScrollToItem(2)
    }

    Scaffold(
        topBar = {
            BlurredBar(backdrop) {
                TopAppBar(
                    title = stringResource(R.string.app_name),
                    color = barColor,
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal),
    ) { padding ->
        // layerBackdrop 只能挂在普通容器上：挂在 LazyColumn 这类可滚动节点上时录制层是空的
        // （库在 record 里会重跑一次 drawContent，滚动容器不认），顶栏就退化成一块纯色。
        // 所以和主题设置页/功能页保持一致，用一个 Box 把整页内容包起来供玻璃采样。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                state = listState,
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = padding.calculateTopPadding(),
                    bottom = bottomInnerPadding + 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item("assistant_status") { AssistantStatusCardMiuix(config, onToggle, Modifier.animateItem()) }
                item("system_info") { SystemInfoCardMiuix(Modifier.animateItem()) }
                item("apps_header") {
                    AssistantAppsHeaderMiuix(
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
                        AssistantAppsControlsMiuix(
                            query = query,
                            onQueryChange = { query = it },
                            category = category,
                            onCategoryChange = { category = it },
                        )
                    }
                }
                if (expanded) {
                    if (filteredApps.isEmpty()) {
                        item("apps_empty") { EmptyAppsCardMiuix(Modifier.animateItem()) }
                    } else {
                        items(filteredApps, key = { "app_${it.packageName}" }) { app ->
                            AppRowCardMiuix(
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
}

@Composable
private fun AssistantStatusCardMiuix(config: AssistantConfig, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val iconScale by animateFloatAsState(
        targetValue = if (config.enabled) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "assistant_status_icon_scale_miuix",
    )
    val enableGlassSwitch = LocalEnableGlassSwitch.current
    // 玻璃要透出背景：把「图标 + 文案」这一整块录成捕获层喂给玻璃，滑过去就能看到
    // 背景被折射。开关本体刻意留在这棵子树之外，否则玻璃折射自己会在录制阶段递归。
    val cardBackdrop = rememberLayerBackdrop()
    Card(modifier.fillMaxWidth().smoothSize()) {
        Box(Modifier.fillMaxWidth()) {
            Row(
                // layerBackdrop 在 padding 之前：整行（含开关那一格空白）都被录进捕获层，
                // 玻璃停在原位时也有完整背景可采样，滑到文字上就能看到背景被折射。
                Modifier
                    .fillMaxWidth()
                    .layerBackdrop(cardBackdrop)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Pets, null, tint = colorScheme.primary, modifier = Modifier.size(28.dp).graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                })
                Text(
                    if (config.enabled) stringResource(R.string.home_assistant_enabled) else stringResource(R.string.home_assistant_disabled),
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    color = colorScheme.onSurface,
                )
                // 开关原位占位：排版和以前一模一样，玻璃叠在这格之上。
                Spacer(Modifier.size(width = 49.dp, height = 28.dp))
            }
            GlassSwitch(
                checked = config.enabled,
                onCheckedChange = onToggle,
                glass = enableGlassSwitch,
                backdrop = cardBackdrop,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp),
            )
        }
    }
}

@Composable
private fun SystemInfoCardMiuix(modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(stringResource(R.string.home_system_info), color = colorScheme.onSurface)
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.home_android_version), color = colorScheme.onSurfaceVariantSummary)
                Text(Build.VERSION.RELEASE ?: Build.VERSION.SDK_INT.toString(), color = colorScheme.onSurface)
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.home_device_model), color = colorScheme.onSurfaceVariantSummary)
                Text(Build.MODEL.orEmpty().ifBlank { "—" }, color = colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun AssistantAppsHeaderMiuix(
    selectedCount: Int,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "apps_expand_rotation_miuix",
    )
    Card(modifier.fillMaxWidth().smoothSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange(!expanded) }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.home_assistant_apps), color = colorScheme.onSurface)
                Text(
                    if (selectedCount == 0) stringResource(R.string.home_assistant_apps_none) else stringResource(R.string.home_assistant_apps_count, selectedCount),
                    color = colorScheme.onSurfaceVariantSummary,
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
                    if (isExpanded) MiuixIcons.ExpandLess else MiuixIcons.ExpandMore,
                    null,
                    tint = colorScheme.primary,
                    modifier = Modifier.rotate(iconRotation),
                )
            }
        }
    }
}

@Composable
private fun AssistantAppsControlsMiuix(
    query: String,
    onQueryChange: (String) -> Unit,
    category: HomeAppCategory,
    onCategoryChange: (HomeAppCategory) -> Unit,
) {
    Card(Modifier.fillMaxWidth().smoothSize()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = stringResource(R.string.assistant_apps_search),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryButtonMiuix(stringResource(R.string.assistant_apps_user), category == HomeAppCategory.User) {
                    onCategoryChange(HomeAppCategory.User)
                }
                CategoryButtonMiuix(stringResource(R.string.assistant_apps_system), category == HomeAppCategory.System) {
                    onCategoryChange(HomeAppCategory.System)
                }
            }
        }
    }
}

@Composable
private fun CategoryButtonMiuix(text: String, selected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.04f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "category_scale_miuix",
    )
    val textColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) colorScheme.primary else colorScheme.onSurfaceVariantSummary,
        animationSpec = tween(180),
        label = "category_color_miuix",
    )
    Text(
        text,
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = textColor,
    )
}

@Composable
private fun EmptyAppsCardMiuix(modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth().smoothSize()) {
        Text(
            stringResource(R.string.assistant_apps_empty),
            color = colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun AppRowCardMiuix(
    app: SelectableApp,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowScale by animateFloatAsState(
        targetValue = if (checked) 0.985f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "app_row_scale_miuix",
    )
    Card(modifier.fillMaxWidth().graphicsLayer {
        scaleX = rowScale
        scaleY = rowScale
    }) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIconImage(Modifier.size(40.dp), app.applicationInfo, app.label)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(app.label, color = colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(app.packageName, color = colorScheme.onSurfaceVariantSummary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

private fun Modifier.smoothSize(): Modifier =
    animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow))
