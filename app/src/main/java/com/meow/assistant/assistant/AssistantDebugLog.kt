package com.meow.assistant.assistant

import android.content.Context
import android.os.SystemClock
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

object AssistantDebugLog {
    private const val FILE_NAME = "assistant-debug.log"
    private const val MAX_BYTES = 128 * 1024L
    private const val KEEP_BYTES = 96 * 1024
    private const val MAX_TEXT_LENGTH = 160
    private const val TRIM_INTERVAL_MS = 15_000L

    private val lock = Any()
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "AssistantDebugLog").apply { isDaemon = true }
    }
    @Volatile private var lastTrimUptime = 0L

    fun record(context: Context, action: String, details: Map<String, Any?> = emptyMap()) {
        val appContext = context.applicationContext
        val line = buildString {
            append(timestampFormat.format(Date()))
            append(" action=")
            append(sanitize(action))
            details.forEach { (key, value) ->
                append(' ')
                append(sanitize(key))
                append('=')
                append(sanitize(value?.toString().orEmpty()))
            }
            append('\n')
        }
        executor.execute {
            synchronized(lock) {
                val file = logFile(appContext)
                file.parentFile?.mkdirs()
                file.appendText(line)
                trimIfNeeded(file)
            }
        }
    }

    fun export(context: Context): String = synchronized(lock) {
        val file = logFile(context.applicationContext)
        if (!file.exists()) return@synchronized ""
        file.readText()
    }

    private fun trimIfNeeded(file: File) {
        if (file.length() <= MAX_BYTES) return
        val now = SystemClock.uptimeMillis()
        if (now - lastTrimUptime < TRIM_INTERVAL_MS) return
        lastTrimUptime = now
        val bytes = file.readBytes()
        val fromIndex = (bytes.size - KEEP_BYTES).coerceAtLeast(0)
        val kept = bytes.copyOfRange(fromIndex, bytes.size)
        val firstNewline = kept.indexOf('\n'.code.toByte())
        file.writeBytes(if (firstNewline >= 0 && firstNewline + 1 < kept.size) kept.copyOfRange(firstNewline + 1, kept.size) else kept)
    }

    private fun logFile(context: Context): File = File(context.filesDir, FILE_NAME)

    private fun sanitize(value: String): String {
        val cleaned = value
            .replace("\u200B", "")
            .replace("\u200C", "")
            .replace("\u200D", "")
            .replace("\uFEFF", "")
            .replace("\r\n", "\\n")
            .replace("\n", "\\n")
            .replace("\r", "\\n")
            .replace("\t", " ")
        return if (cleaned.length <= MAX_TEXT_LENGTH) cleaned else cleaned.take(MAX_TEXT_LENGTH) + "…"
    }
}
