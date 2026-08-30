package com.meow.assistant.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.meow.assistant.data.repository.SettingsRepository
import com.meow.assistant.data.repository.SettingsRepositoryImpl
import com.meow.assistant.ui.screen.settings.SettingsUiState
import com.meow.assistant.ui.theme.ColorMode

class SettingsViewModel(private val repo: SettingsRepository = SettingsRepositoryImpl()) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    init { refresh() }

    fun refresh() {
        _uiState.value = SettingsUiState(
            uiMode = repo.uiMode,
            checkUpdate = repo.checkUpdate,
            themeMode = repo.themeMode,
            miuixMonet = repo.miuixMonet,
            keyColor = repo.keyColor,
            colorStyle = repo.colorStyle,
            colorSpec = repo.colorSpec,
            enablePredictiveBack = repo.enablePredictiveBack,
            enableBlur = repo.enableBlur,
            enableFloatingBottomBar = repo.enableFloatingBottomBar,
            enableFloatingBottomBarBlur = repo.enableFloatingBottomBarBlur,
            enableNavigationBadge = repo.enableNavigationBadge,
            pageScale = repo.pageScale,
        )
    }
    fun setCheckUpdate(value: Boolean) { repo.checkUpdate = value; _uiState.update { it.copy(checkUpdate = value) } }
    fun setEnablePredictiveBack(value: Boolean) { repo.enablePredictiveBack = value; _uiState.update { it.copy(enablePredictiveBack = value) } }
    fun setUiMode(value: String) { repo.uiMode = value; _uiState.update { it.copy(uiMode = value) } }
    fun setEnableBlur(value: Boolean) { repo.enableBlur = value; _uiState.update { it.copy(enableBlur = value) } }
    fun setEnableFloatingBottomBar(value: Boolean) { repo.enableFloatingBottomBar = value; _uiState.update { it.copy(enableFloatingBottomBar = value) } }
    fun setEnableFloatingBottomBarBlur(value: Boolean) { repo.enableFloatingBottomBarBlur = value; _uiState.update { it.copy(enableFloatingBottomBarBlur = value) } }
    fun setEnableNavigationBadge(value: Boolean) { repo.enableNavigationBadge = value; _uiState.update { it.copy(enableNavigationBadge = value) } }
    fun setPageScale(value: Float) { repo.pageScale = value; _uiState.update { it.copy(pageScale = value) } }
    fun setThemeMode(value: Int) { repo.themeMode = value; _uiState.update { it.copy(themeMode = value) } }
    fun setColorMode(value: ColorMode) = setThemeMode(value.value)
    fun setMiuixMonet(value: Boolean) { repo.miuixMonet = value; _uiState.update { it.copy(miuixMonet = value) } }
    fun setKeyColor(value: Int) { repo.keyColor = value; _uiState.update { it.copy(keyColor = value) } }
    fun setColorStyle(value: String) { repo.colorStyle = value; _uiState.update { it.copy(colorStyle = value) } }
    fun setColorSpec(value: String) { repo.colorSpec = value; _uiState.update { it.copy(colorSpec = value) } }
}
