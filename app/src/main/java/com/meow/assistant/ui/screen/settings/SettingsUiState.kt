package com.meow.assistant.ui.screen.settings

import androidx.compose.runtime.Immutable
import com.meow.assistant.ui.UiMode

@Immutable
data class SettingsUiState(
    val uiMode: String = UiMode.DEFAULT_VALUE,
    val checkUpdate: Boolean = true,
    val themeMode: Int = 0,
    val miuixMonet: Boolean = false,
    val keyColor: Int = 0,
    val colorStyle: String = "TonalSpot",
    val colorSpec: String = "SPEC_2025",
    val enablePredictiveBack: Boolean = false,
    val enableBlur: Boolean = true,
    val enableFloatingBottomBar: Boolean = false,
    val enableFloatingBottomBarBlur: Boolean = false,
    val enableNavigationBadge: Boolean = true,
    val pageScale: Float = 1f,
)

@Immutable
data class SettingsScreenActions(
    val onOpenTheme: () -> Unit,
    val onOpenAbout: () -> Unit,
    val onCheckUpdateChanged: (Boolean) -> Unit,
)
