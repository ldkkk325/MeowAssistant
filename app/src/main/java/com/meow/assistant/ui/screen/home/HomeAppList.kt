package com.meow.assistant.ui.screen.home

import android.content.pm.ApplicationInfo
import com.meow.assistant.assistant.SelectableApp

internal enum class HomeAppCategory {
    User,
    System,
}

internal fun List<SelectableApp>.filterForHomeList(category: HomeAppCategory, query: String): List<SelectableApp> {
    val normalizedQuery = query.trim()
    return asSequence()
        .filter { app ->
            when (category) {
                HomeAppCategory.User -> !app.applicationInfo.isSystemApp()
                HomeAppCategory.System -> app.applicationInfo.isSystemApp()
            }
        }
        .filter { app ->
            normalizedQuery.isEmpty() ||
                app.label.contains(normalizedQuery, ignoreCase = true) ||
                app.packageName.contains(normalizedQuery, ignoreCase = true)
        }
        .toList()
}

internal fun ApplicationInfo.isSystemApp(): Boolean =
    flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
