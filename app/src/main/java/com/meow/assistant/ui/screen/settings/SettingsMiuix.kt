package com.meow.assistant.ui.screen.settings

import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meow.assistant.R
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun SettingPagerMiuix(state: SettingsUiState, actions: SettingsScreenActions, bottomInnerPadding: Dp) {
    val behavior = MiuixScrollBehavior()
    Scaffold(
        topBar = { TopAppBar(title = stringResource(R.string.settings), scrollBehavior = behavior) },
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal),
    ) { padding ->
        Column(
            Modifier.fillMaxSize()
                .padding(padding)
                .nestedScroll(behavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
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
                SwitchPreference(
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
