package com.meow.assistant.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.meow.assistant.update.UpdateManager
import com.meow.assistant.ui.LocalUiMode
import com.meow.assistant.ui.UiMode
import com.meow.assistant.ui.navigation3.Navigator
import com.meow.assistant.ui.navigation3.Route
import com.meow.assistant.ui.viewmodel.SettingsViewModel

@Composable
fun SettingPager(navigator: Navigator, bottomInnerPadding: Dp) {
    val context = LocalContext.current
    val viewModel = viewModel<SettingsViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) { viewModel.refresh(); onPauseOrDispose { } }
    val actions = SettingsScreenActions(
        onOpenTheme = { navigator.push(Route.ColorPalette) },
        onOpenAbout = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(UpdateManager.REPOSITORY_URL)))
        },
        onCheckUpdateChanged = viewModel::setCheckUpdate,
    )
    when (LocalUiMode.current) {
        UiMode.Miuix -> SettingPagerMiuix(state, actions, bottomInnerPadding)
        UiMode.Material -> SettingPagerMaterial(state, actions, bottomInnerPadding)
    }
}
