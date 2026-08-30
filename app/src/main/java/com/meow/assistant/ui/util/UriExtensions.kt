package com.meow.assistant.ui.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

fun Uri.getFileName(context: Context): String? {
    if (scheme.equals("file", ignoreCase = true)) {
        return path?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
    }

    return runCatching {
        context.contentResolver.query(this, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0)?.takeIf { it.isNotBlank() } else null
            }
    }.getOrNull() ?: lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
}

