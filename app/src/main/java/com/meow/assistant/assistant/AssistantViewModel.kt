package com.meow.assistant.assistant

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.meow.assistant.assistantApp

data class SelectableApp(
    val packageName: String,
    val label: String,
    val applicationInfo: ApplicationInfo,
)

fun loadSelectableApps(context: Context): List<SelectableApp> {
    val packageManager = context.packageManager
    return packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        .mapNotNull { applicationInfo ->
            val packageName = applicationInfo.packageName ?: return@mapNotNull null
            if (packageName == context.packageName) return@mapNotNull null
            SelectableApp(
                packageName = packageName,
                label = applicationInfo.loadLabel(packageManager).toString().ifBlank { packageName },
                applicationInfo = applicationInfo,
            )
        }
        .distinctBy { it.packageName }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
}

class AssistantViewModel : ViewModel() {
    private val _config = MutableStateFlow(AssistantConfig.load(assistantApp))
    val config: StateFlow<AssistantConfig> = _config.asStateFlow()
    private val _apps = MutableStateFlow<List<SelectableApp>>(emptyList())
    val apps: StateFlow<List<SelectableApp>> = _apps.asStateFlow()
    private val _appsLoaded = MutableStateFlow(false)
    val appsLoaded: StateFlow<Boolean> = _appsLoaded.asStateFlow()

    init { refreshApps(assistantApp) }

    fun refreshApps(context: Context) {
        viewModelScope.launch(Dispatchers.Default) {
            _apps.value = loadSelectableApps(context)
            _appsLoaded.value = true
        }
    }

    fun update(transform: (AssistantConfig) -> AssistantConfig) {
        val updated = transform(AssistantConfig.load(assistantApp))
        _config.value = updated
        updated.save(assistantApp)
    }

    fun setEnabled(enabled: Boolean) = update { it.copy(enabled = enabled) }
    fun setMode(mode: ProcessingMode) {
        update { it.copy(processingMode = mode) }
        val serviceIntent = Intent(assistantApp, AssistantFloatBallService::class.java)
        if (mode == ProcessingMode.FLOATING && Settings.canDrawOverlays(assistantApp)) {
            assistantApp.startService(serviceIntent)
        } else if (mode != ProcessingMode.FLOATING) {
            assistantApp.stopService(serviceIntent)
        }
    }
    fun setAppendEnabled(enabled: Boolean) = update { it.copy(enableAppend = enabled) }
    fun setAppendText(text: String) = update { it.copy(appendText = text) }
    fun setAppendProbability(probability: Int) = update { it.copy(appendProbability = probability.coerceIn(0, 100)) }
    fun setEmoticonEnabled(enabled: Boolean) = update { it.copy(enableEmoticon = enabled) }
    fun setEmoticonProbability(probability: Int) = update { it.copy(emoticonProbability = probability.coerceIn(0, 100)) }
    fun setSmartEmoticonEnabled(enabled: Boolean) = update { it.copy(enableSmartEmoticon = enabled) }
    fun setRandomTextEnabled(enabled: Boolean) = update { it.copy(enableRandomText = enabled) }
    fun setRandomTextProbability(probability: Int) = update { it.copy(randomTextProbability = probability.coerceIn(0, 100)) }
    fun setProtectInputMethods(enabled: Boolean) = update { it.copy(protectInputMethods = enabled) }
    fun setProtectPasswords(enabled: Boolean) = update { it.copy(protectPasswords = enabled) }
    fun importConfig(json: String) = update { AssistantConfig.fromJson(json, it) }
    fun exportConfig(): String = AssistantConfig.toJson(_config.value)
    fun setFloatBallSize(size: Int) = update {
        it.copy(floatBallSize = size.coerceIn(AssistantConfig.MIN_FLOAT_BALL_SIZE, AssistantConfig.MAX_FLOAT_BALL_SIZE))
    }
    fun setFloatBallAlpha(alpha: Float) = update {
        it.copy(floatBallAlpha = alpha.coerceIn(AssistantConfig.MIN_FLOAT_BALL_ALPHA, AssistantConfig.MAX_FLOAT_BALL_ALPHA))
    }
    fun setFloatBallPosition(x: Int, y: Int) = update { it.copy(floatBallX = x, floatBallY = y) }
    fun setSelectedPackages(packages: Set<String>) = update { it.copy(selectedPackages = packages) }
    fun setRules(text: String) = update { it.copy(rules = AssistantConfig.parseRules(text)) }
    fun setCustomEmoticons(text: String) = update { it.copy(customEmoticons = text.lines().map(String::trim).filter(String::isNotEmpty)) }
    fun setCustomTexts(text: String) = update { it.copy(customTexts = text.lines().map(String::trim).filter(String::isNotEmpty)) }
}
