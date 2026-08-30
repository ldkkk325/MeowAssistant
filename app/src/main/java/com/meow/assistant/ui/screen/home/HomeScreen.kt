package com.meow.assistant.ui.screen.home

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meow.assistant.assistant.AssistantAccessibilityService
import com.meow.assistant.assistant.AssistantViewModel
import com.meow.assistant.ui.LocalUiMode
import com.meow.assistant.ui.UiMode
import com.meow.assistant.ui.navigation3.Navigator
import com.google.android.accessibility.selecttospeak.SelectToSpeakService

@Composable
fun HomePager(navigator: Navigator, bottomInnerPadding: Dp, isCurrentPage: Boolean = true) {
    val assistantViewModel = viewModel<AssistantViewModel>()
    val config by assistantViewModel.config.collectAsStateWithLifecycle()
    val apps by assistantViewModel.apps.collectAsStateWithLifecycle()
    val appsLoaded by assistantViewModel.appsLoaded.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var accessibilityEnabled by remember { mutableStateOf(false) }
    var permissionSettingsOpened by rememberSaveable { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        assistantViewModel.refreshApps(context)
        val serviceEnabled = isAssistantAccessibilityEnabled(context)
        accessibilityEnabled = serviceEnabled
        when {
            serviceEnabled && !config.enabled -> assistantViewModel.setEnabled(true)
            !serviceEnabled && config.enabled -> assistantViewModel.setEnabled(false)
        }
        onPauseOrDispose { }
    }
    LaunchedEffect(isCurrentPage) { if (isCurrentPage) assistantViewModel.refreshApps(context) }
    LaunchedEffect(isCurrentPage, appsLoaded, apps) {
        if (!isCurrentPage || !appsLoaded) return@LaunchedEffect
        if (apps.isNotEmpty()) {
            permissionSettingsOpened = false
            return@LaunchedEffect
        }
        if (permissionSettingsOpened) return@LaunchedEffect
        permissionSettingsOpened = true
        openApplicationPermissionSettings(context)
    }
    val onToggle: (Boolean) -> Unit = { enabled ->
        if (enabled && !accessibilityEnabled) context.startActivity(android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
        else assistantViewModel.setEnabled(enabled)
    }
    when (LocalUiMode.current) {
        UiMode.Miuix -> HomePagerMiuix(config, apps, onToggle, assistantViewModel::setSelectedPackages, bottomInnerPadding)
        UiMode.Material -> HomePagerMaterial(config, apps, onToggle, assistantViewModel::setSelectedPackages, bottomInnerPadding)
    }
}

private fun openApplicationPermissionSettings(context: Context) {
    val packageName = context.packageName
    val packageUri = Uri.parse("package:$packageName")
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri)
        .takeIf { it.resolveActivity(context.packageManager) != null }
        ?.let(context::startActivity)
}

internal fun isAssistantAccessibilityEnabled(context: Context): Boolean {
    val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
    val serviceClassNames = setOf(
        AssistantAccessibilityService::class.java.name,
        SelectToSpeakService::class.java.name,
    )
    return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any { info ->
        val serviceInfo = info.resolveInfo?.serviceInfo
        serviceInfo?.packageName == context.packageName && serviceInfo.name in serviceClassNames
    }
}
