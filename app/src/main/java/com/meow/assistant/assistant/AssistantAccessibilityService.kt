package com.meow.assistant.assistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque

open class AssistantAccessibilityService : AccessibilityService() {
    private var previousOriginal = ""
    private var lastResult: ProcessResult? = null
    private var lastWritten = ""
    private var lastObservedText = ""
    private var lastNodeKey = ""
    private var lastEmoticon: String? = null
    private var lastActionText: String? = null
    private var lastFloatingNodeKey = ""
    private var lastFloatingInput = ""
    private var lastFloatingOutput = ""
    private var inputMethodPackage: String? = null
    private var activeWindowPackage: String? = null
    private var activeWindowId = -1
    private val eventHandler = Handler(Looper.getMainLooper())
    private var pendingProcess: Runnable? = null
    private var pendingNode: AccessibilityNodeInfo? = null
    private var pendingDeletionEvent = false
    private var pendingInsertionEvent = false
    private var deletionSessionActive = false
    private val recentWrittenTexts = ArrayDeque<String>()

    override fun onServiceConnected() {
        activeInstance = this
        inputMethodPackage = defaultInputMethodPackage()
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_CLICKED or
                AccessibilityEvent.TYPE_VIEW_FOCUSED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = 1 or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 80
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val eventPackage = normalizePackageName(event.packageName?.toString().orEmpty())
        if (eventPackage == packageName) return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val currentInputMethod = inputMethodPackage
            if (eventPackage != currentInputMethod && eventPackage != "com.android.systemui") {
                val windowChanged = eventPackage != activeWindowPackage || event.windowId != activeWindowId
                if (windowChanged) {
                    cancelPendingProcessing(clearDeletion = true)
                    resetState()
                    activeWindowPackage = eventPackage
                    activeWindowId = event.windowId
                    inputMethodPackage = defaultInputMethodPackage()
                }
            }
            return
        }
        val config = AssistantConfig.load(this)
        if (!config.enabled || eventPackage !in config.selectedPackages) return
        if (config.protectInputMethods && eventPackage == inputMethodPackage) return
        if (config.processingMode == ProcessingMode.FLOATING) return
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED && !isSendAction(event)) return
        val root = getBestRoot(eventPackage)
        val node = findFocusedEditableNode(root)
            ?: findEditableNode(event.source)
            ?: findEditableNode(root)
            ?: return
        val beforeLength = event.beforeText?.length ?: -1
        val eventTextLength = event.text.firstOrNull()?.length ?: -1
        val textChanged = event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        val deletionEvent = textChanged && (
            event.removedCount > 0 && event.addedCount <= 0 ||
                beforeLength >= 0 && eventTextLength >= 0 && beforeLength > eventTextLength
            )
        val insertionEvent = textChanged && (
            event.addedCount > 0 && event.removedCount <= 0 ||
                beforeLength >= 0 && eventTextLength >= 0 && eventTextLength > beforeLength
            )
        scheduleTextProcessing(
            eventPackage = eventPackage,
            eventType = event.eventType,
            node = node,
            eventText = event.text.firstOrNull()?.toString(),
            delayMs = eventProcessDelay(eventPackage, config, deletionEvent, insertionEvent),
            deletionEvent = deletionEvent,
            insertionEvent = insertionEvent,
        )
    }

    private fun scheduleTextProcessing(
        eventPackage: String,
        eventType: Int,
        node: AccessibilityNodeInfo,
        eventText: String?,
        delayMs: Long,
        deletionEvent: Boolean,
        insertionEvent: Boolean,
    ) {
        pendingDeletionEvent = pendingDeletionEvent || deletionEvent
        pendingInsertionEvent = pendingInsertionEvent || insertionEvent
        cancelPendingProcessing()
        val snapshot = AccessibilityNodeInfo.obtain(node)
        pendingNode = snapshot
        lateinit var task: Runnable
        task = Runnable {
            if (pendingNode !== snapshot) {
                snapshot.recycle()
                return@Runnable
            }
            pendingNode = null
            pendingProcess = null
            val wasDeletion = pendingDeletionEvent
            val wasInsertion = pendingInsertionEvent
            pendingDeletionEvent = false
            pendingInsertionEvent = false
            try {
                val currentNode = findCurrentEditableNode(eventPackage, snapshot)
                processTextEvent(eventPackage, eventType, currentNode, eventText, wasDeletion, wasInsertion)
            } finally {
                snapshot.recycle()
            }
        }
        pendingProcess = task
        eventHandler.postDelayed(task, delayMs)
    }

    private fun processTextEvent(
        eventPackage: String,
        eventType: Int,
        node: AccessibilityNodeInfo,
        eventText: String?,
        deletionEvent: Boolean,
        insertionEvent: Boolean,
    ) {
        val config = AssistantConfig.load(this)
        if (!config.enabled || eventPackage !in config.selectedPackages) return
        if (config.protectInputMethods && eventPackage == inputMethodPackage) return
        if (config.processingMode == ProcessingMode.FLOATING) return
        if (isProtectedInput(node, config)) return
        runCatching { node.refresh() }
        val current = readCurrentText(eventPackage, eventType, node, eventText, config)
        val nodeKey = nodeKey(eventPackage, node)
        if (nodeKey != lastNodeKey) {
            resetState(clearDeletionSession = false)
            lastNodeKey = nodeKey
        }
        val previousObservedText = lastObservedText
        val currentCursor = node.textSelectionStart
            .takeIf { it >= 0 }
            ?.coerceIn(0, current.length)
        val lengthDropped = current.length < previousObservedText.length || current.length < lastWritten.length
        val realtimeMode = config.processingMode == ProcessingMode.REALTIME
        val deleting = realtimeMode && (lengthDropped || deletionEvent && !insertionEvent)
        val recovered = if (config.processingMode == ProcessingMode.REALTIME && lastResult != null && previousOriginal.isNotEmpty()) {
            TextProcessor.recoverEdited(
                previousOriginal = previousOriginal,
                previousResult = lastResult!!,
                editedText = current,
                editedCursorIndex = currentCursor,
            )
        } else {
            TextProcessor.RecoveredEdit(current, currentCursor ?: current.length)
        }
        val original = recovered.text
        if (deleting) {
            handleRealtimeDeletion(node, current, recovered)
            return
        }
        if (realtimeMode && deletionSessionActive && !isTextInsertion(previousObservedText, current, insertionEvent)) {
            preservePlainEditedText(current, currentCursor ?: current.length)
            return
        }
        if (deletionSessionActive && isTextInsertion(previousObservedText, current, insertionEvent)) {
            deletionSessionActive = false
        }
        lastObservedText = current
        if (current == lastWritten) return
        if (current in recentWrittenTexts) return
        val hint = node.hintText?.toString()?.trim().orEmpty()
        if (current.isBlank() || current.trim() in knownPlaceholders || hint.isNotEmpty() && current.trim() == hint) {
            resetTextState()
            return
        }
        if (config.processingMode == ProcessingMode.PUNCTUATION && eventType != AccessibilityEvent.TYPE_VIEW_CLICKED && current.lastOrNull()?.let { it !in charArrayOf('。', '！', '!', '？', '?', '.', ',', '，', '\n') } == true) return

        val previousEmoticon = lastEmoticon?.takeIf { current.contains(it) }
        val previousActionText = lastActionText?.takeIf { current.contains("($it)") }
        val result = TextProcessor.process(
            original,
            config,
            previousEmoticon = previousEmoticon.takeIf { config.processingMode == ProcessingMode.REALTIME },
            previousActionText = previousActionText.takeIf { config.processingMode == ProcessingMode.REALTIME },
        )
        if (result.text == current || result.text.isBlank()) {
            previousOriginal = original
            lastWritten = current
            lastResult = result
            lastEmoticon = result.emoticon
            lastActionText = result.actionText
            return
        }
        val selectionIndex = TextProcessor.processedCursorIndex(result, recovered.cursorIndex)
        writeResult(node, result, original, selectionIndex)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        cancelPendingProcessing(clearDeletion = true)
        if (activeInstance === this) activeInstance = null
        super.onDestroy()
    }

    private fun cancelPendingProcessing(clearDeletion: Boolean = false) {
        pendingProcess?.let(eventHandler::removeCallbacks)
        pendingProcess = null
        pendingNode?.recycle()
        pendingNode = null
        if (clearDeletion) {
            pendingDeletionEvent = false
            pendingInsertionEvent = false
        }
    }

    private fun isTextInsertion(previous: String, current: String, insertionEvent: Boolean): Boolean {
        if (insertionEvent) return true
        if (current.length > previous.length) return true
        if (previous.isEmpty() && current.isNotEmpty()) return true
        return false
    }

    private fun eventProcessDelay(
        eventPackage: String,
        config: AssistantConfig,
        deletionEvent: Boolean,
        insertionEvent: Boolean,
    ): Long = when {
        config.processingMode == ProcessingMode.REALTIME && deletionEvent && !insertionEvent -> REALTIME_DELETION_PROCESS_DELAY_MS
        eventPackage == WECHAT_PACKAGE && config.processingMode == ProcessingMode.REALTIME -> WECHAT_TEXT_PROCESS_DELAY_MS
        else -> TEXT_PROCESS_DELAY_MS
    }

    private fun replaceFocusedTextForFloating() {
        val config = AssistantConfig.load(this)
        if (!config.enabled || config.processingMode != ProcessingMode.FLOATING) return
        val root = getBestRoot(null) ?: return
        val eventPackage = normalizePackageName(root.packageName?.toString().orEmpty())
        if (eventPackage.isEmpty() || eventPackage == packageName || eventPackage !in config.selectedPackages || config.protectInputMethods && eventPackage == inputMethodPackage) return
        val node = findFocusedEditableNode(root) ?: findEditableNode(root) ?: return
        if (isProtectedInput(node, config)) return
        runCatching { node.refresh() }
        val current = node.text?.toString().orEmpty()
        val currentNodeKey = nodeKey(eventPackage, node)
        if (current == lastWritten || current in recentWrittenTexts || current == lastFloatingOutput) return
        if (currentNodeKey == lastFloatingNodeKey && current == lastFloatingInput) return
        if (current.isBlank() || current.trim() in knownPlaceholders) return
        val result = TextProcessor.process(current, config)
        if (result.text == current || result.text.isBlank()) return
        if (writeResult(node, result, current)) {
            lastFloatingNodeKey = currentNodeKey
            lastFloatingInput = current
            lastFloatingOutput = result.text
        }
    }

    private fun writeResult(
        node: AccessibilityNodeInfo,
        result: ProcessResult,
        original: String,
        selectionIndex: Int = result.userEndIndex,
    ): Boolean {
        val arguments = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, result.text) }
        if (!node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)) return false
        node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, Bundle().apply {
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, selectionIndex.coerceIn(0, result.text.length))
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, selectionIndex.coerceIn(0, result.text.length))
        })
        previousOriginal = original
        lastWritten = result.text
        lastObservedText = result.text
        lastResult = result
        lastEmoticon = result.emoticon
        lastActionText = result.actionText
        deletionSessionActive = false
        recentWrittenTexts.remove(result.text)
        recentWrittenTexts.addLast(result.text)
        while (recentWrittenTexts.size > MAX_RECENT_WRITTEN_TEXTS) recentWrittenTexts.removeFirst()
        return true
    }

    private fun handleRealtimeDeletion(
        node: AccessibilityNodeInfo,
        current: String,
        recovered: TextProcessor.RecoveredEdit,
    ) {
        deletionSessionActive = true
        val plainText = recovered.text
        val cursor = recovered.cursorIndex.coerceIn(0, plainText.length)
        if (current != plainText) {
            writePlainText(node, plainText, cursor)
        } else {
            preservePlainEditedText(plainText, cursor)
        }
    }

    private fun writePlainText(node: AccessibilityNodeInfo, text: String, selectionIndex: Int): Boolean {
        val arguments = Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) }
        if (!node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)) return false
        node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, Bundle().apply {
            val cursor = selectionIndex.coerceIn(0, text.length)
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, cursor)
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, cursor)
        })
        preservePlainEditedText(text, selectionIndex)
        return true
    }

    private fun preservePlainEditedText(text: String, cursorIndex: Int) {
        val cursor = cursorIndex.coerceIn(0, text.length)
        previousOriginal = text
        lastWritten = text
        lastObservedText = text
        lastResult = ProcessResult(
            text = text,
            userEndIndex = cursor,
            processedBoundaryToOriginal = IntArray(text.length + 1) { it },
        )
        lastEmoticon = null
        lastActionText = null
    }

    private fun isProtectedInput(node: AccessibilityNodeInfo, config: AssistantConfig): Boolean {
        val inputType = node.inputType
        val inputClass = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        val password = node.isPassword || inputClass == InputType.TYPE_CLASS_TEXT && variation in setOf(InputType.TYPE_TEXT_VARIATION_PASSWORD, InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD, InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)
        val numeric = inputClass == InputType.TYPE_CLASS_NUMBER || inputClass == InputType.TYPE_CLASS_PHONE || inputClass == InputType.TYPE_CLASS_DATETIME
        return config.protectPasswords && password || numeric
    }

    private fun nodeKey(eventPackage: String, node: AccessibilityNodeInfo): String = "$eventPackage:${node.windowId}:${node.viewIdResourceName}:${node.className}"
    private fun defaultInputMethodPackage(): String? = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        ?.substringBefore('/')
        ?.let(::normalizePackageName)
        ?.takeIf { it.isNotBlank() }

    private fun normalizePackageName(value: String): String = value.substringBefore(':')

    private fun resetTextState(clearDeletionSession: Boolean = true) {
        previousOriginal = ""
        lastResult = null
        lastWritten = ""
        lastObservedText = ""
        lastEmoticon = null
        lastActionText = null
        if (clearDeletionSession) deletionSessionActive = false
    }

    private fun resetState(clearDeletionSession: Boolean = true) {
        resetTextState(clearDeletionSession)
        recentWrittenTexts.clear()
        lastFloatingNodeKey = ""
        lastFloatingInput = ""
        lastFloatingOutput = ""
        lastNodeKey = ""
    }

    private fun getBestRoot(eventPackage: String?): AccessibilityNodeInfo? {
        val activeRoot = rootInActiveWindow
        if (activeRoot != null && (eventPackage.isNullOrEmpty() || packageMatches(activeRoot, eventPackage))) {
            return activeRoot
        }
        val matchingWindow = runCatching {
            windows.firstOrNull { window ->
                val windowRoot = window.root
                windowRoot != null && (eventPackage.isNullOrEmpty() || packageMatches(windowRoot, eventPackage)) &&
                    (window.isFocused || window.isActive)
            } ?: windows.firstOrNull { window ->
                val windowRoot = window.root
                windowRoot != null && (eventPackage.isNullOrEmpty() || packageMatches(windowRoot, eventPackage))
            }
        }.getOrNull()
        return matchingWindow?.root ?: activeRoot
    }

    private fun packageMatches(node: AccessibilityNodeInfo, expectedPackage: String): Boolean =
        normalizePackageName(node.packageName?.toString().orEmpty()) == expectedPackage

    private fun isEditableNode(node: AccessibilityNodeInfo): Boolean {
        if (node.isEditable) return true
        val className = node.className?.toString().orEmpty()
        return node.isEnabled && className.endsWith("EditText", ignoreCase = true)
    }

    private fun findEditableNode(source: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (source == null) return null
        if (isEditableNode(source)) return source
        for (index in 0 until source.childCount) findEditableNode(source.getChild(index))?.let { return it }
        return null
    }

    private fun findFocusedEditableNode(source: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (source == null) return null
        if (isEditableNode(source) && source.isFocused) return source
        for (index in 0 until source.childCount) findFocusedEditableNode(source.getChild(index))?.let { return it }
        return null
    }

    private fun findCurrentEditableNode(eventPackage: String, fallback: AccessibilityNodeInfo): AccessibilityNodeInfo {
        val root = getBestRoot(eventPackage)
        return findFocusedEditableNode(root)
            ?: findEditableNode(fallback)
            ?: findEditableNode(root)
            ?: fallback
    }

    private fun readCurrentText(
        eventPackage: String,
        eventType: Int,
        node: AccessibilityNodeInfo,
        eventText: String?,
        config: AssistantConfig,
    ): String {
        val nodeText = node.text?.toString().orEmpty()
        val eventCandidate = eventText.orEmpty()
        return if (
            eventPackage == WECHAT_PACKAGE &&
            config.processingMode == ProcessingMode.REALTIME &&
            eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED &&
            eventCandidate.isNotBlank()
        ) {
            eventCandidate
        } else {
            nodeText
        }
    }

    private fun isSendAction(event: AccessibilityEvent): Boolean {
        val eventLabel = event.text.joinToString(" ") + " " + event.contentDescription?.toString().orEmpty()
        if (eventLabel.contains("发送", ignoreCase = true) || eventLabel.contains("send", ignoreCase = true)) return true
        return isSendAction(event.source)
    }

    private fun isSendAction(source: AccessibilityNodeInfo?): Boolean {
        if (source == null) return false
        val label = listOf(source.text, source.contentDescription).joinToString(" ") { it?.toString().orEmpty() }
        val resourceId = source.viewIdResourceName.orEmpty()
        if (label.contains("发送", ignoreCase = true) || label.contains("send", ignoreCase = true) ||
            resourceId.contains("send", ignoreCase = true) || resourceId.contains("emoji_send", ignoreCase = true)) return true
        for (index in 0 until source.childCount) if (isSendAction(source.getChild(index))) return true
        return false
    }

    companion object {
        @Volatile private var activeInstance: AssistantAccessibilityService? = null
        private const val WECHAT_PACKAGE = "com.tencent.mm"
        private const val TEXT_PROCESS_DELAY_MS = 100L
        private const val WECHAT_TEXT_PROCESS_DELAY_MS = 180L
        private const val REALTIME_DELETION_PROCESS_DELAY_MS = 0L
        private const val MAX_RECENT_WRITTEN_TEXTS = 12
        fun requestFloatingReplacement() { activeInstance?.replaceFocusedTextForFloating() }
        val knownPlaceholders = setOf("发消息", "发消息或按住说话", "发送消息", "说点什么", "发个评论", "友善评论", "友善评论一下", "输入内容", "输入消息", "写下你的评论", "短信")
    }
}
