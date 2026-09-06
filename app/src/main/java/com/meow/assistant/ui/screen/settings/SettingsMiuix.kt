package com.meow.assistant.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meow.assistant.R
import com.meow.assistant.ui.theme.LocalEnableBlur
import com.meow.assistant.ui.util.BlurredBar
import com.meow.assistant.ui.util.rememberBlurBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Notes
import com.meow.assistant.ui.component.GlassSwitchPreference
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun SettingPagerMiuix(state: SettingsUiState, actions: SettingsScreenActions, bottomInnerPadding: Dp) {
    val behavior = MiuixScrollBehavior()
    // 顶栏毛玻璃：和主题设置页同一套（采样整页内容 + surface 染色），内容必须能滚到顶栏底下
    // 才有东西可糊，所以顶部内边距挪到滚动容器里面。
    val backdrop = rememberBlurBackdrop(LocalEnableBlur.current)
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else colorScheme.surface
    Scaffold(
        topBar = {
            BlurredBar(backdrop) {
                TopAppBar(
                    title = stringResource(R.string.settings),
                    color = barColor,
                    scrollBehavior = behavior,
                )
            }
        },
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal),
    ) { padding ->
        // 同主页：layerBackdrop 挂在可滚动节点上录不到内容，必须用普通 Box 包一层再采样
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .nestedScroll(behavior.nestedScrollConnection)
                    .verticalScroll(rememberScrollState())
                    .padding(top = padding.calculateTopPadding())
                    .padding(horizontal = 12.dp)
                    .padding(bottom = bottomInnerPadding),
            ) {
                Card(Modifier.fillMaxWidth()) {
                    ArrowPreference(
                        title = stringResource(R.string.settings_theme),
                        summary = stringResource(R.string.settings_theme_summary),
                        onClick = actions.onOpenTheme,
                        startAction = { Icon(MiuixIcons.Notes, null, tint = colorScheme.primary) },
                    )
                    GlassSwitchPreference(
                        title = stringResource(R.string.settings_check_update),
                        summary = stringResource(R.string.settings_check_update_summary),
                        checked = state.checkUpdate,
                        onCheckedChange = actions.onCheckUpdateChanged,
                    )
                    ArrowPreference(
                        title = stringResource(R.string.about),
                        summary = stringResource(R.string.about_summary),
                        onClick = actions.onOpenAbout,
                    )
                }
            }
        }
    }
}
