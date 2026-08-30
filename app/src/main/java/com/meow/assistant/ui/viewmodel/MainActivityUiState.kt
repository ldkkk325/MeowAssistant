package com.meow.assistant.ui.viewmodel

import androidx.compose.runtime.Immutable
import com.meow.assistant.ui.UiMode
import com.meow.assistant.ui.theme.AppSettings

@Immutable
data class MainActivityUiState(
    val appSettings: AppSettings,
    val pageScale: Float,
    val enableBlur: Boolean,
    val enableFloatingBottomBar: Boolean,
    val enableFloatingBottomBarBlur: Boolean,
    val enableNavigationBadge: Boolean,
    val uiMode: UiMode,
)
