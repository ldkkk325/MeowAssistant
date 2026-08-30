package com.meow.assistant.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meow.assistant.R
import com.meow.assistant.ui.component.material.ExpressiveScaffold
import com.meow.assistant.ui.component.material.SegmentedColumn
import com.meow.assistant.ui.component.material.SegmentedListItem
import com.meow.assistant.ui.component.material.SegmentedSwitchItem
import com.meow.assistant.ui.component.material.expressiveTopAppBarColors

@Composable
fun SettingPagerMaterial(state: SettingsUiState, actions: SettingsScreenActions, bottomInnerPadding: Dp) {
    val behavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    ExpressiveScaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                colors = expressiveTopAppBarColors(),
                scrollBehavior = behavior,
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    ) { padding ->
        Column(
            Modifier.padding(padding)
                .nestedScroll(behavior.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = bottomInnerPadding),
        ) {
            SegmentedColumn {
                item {
                    SegmentedListItem(
                        onClick = actions.onOpenTheme,
                        leadingContent = { Icon(Icons.Rounded.Palette, null) },
                        headlineContent = { Text(stringResource(R.string.settings_theme)) },
                        supportingContent = { Text(stringResource(R.string.settings_theme_summary)) },
                    )
                }
                item {
                    SegmentedSwitchItem(
                        icon = Icons.Rounded.SystemUpdate,
                        title = stringResource(R.string.settings_check_update),
                        summary = stringResource(R.string.settings_check_update_summary),
                        checked = state.checkUpdate,
                        onCheckedChange = actions.onCheckUpdateChanged,
                    )
                }
                item {
                    SegmentedListItem(
                        onClick = actions.onOpenAbout,
                        leadingContent = { Icon(Icons.Rounded.Info, null) },
                        headlineContent = { Text(stringResource(R.string.about)) },
                        supportingContent = { Text(stringResource(R.string.about_summary)) },
                    )
                }
            }
        }
    }
}
