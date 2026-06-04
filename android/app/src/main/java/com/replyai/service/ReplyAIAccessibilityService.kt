package com.replyai.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ClipData
import android.content.ClipboardManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.replyai.utils.messengerFromPackage
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.CopyOnWriteArrayList

@AndroidEntryPoint
class ReplyAIAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return
        if (!MESSENGER_PACKAGES.any { packageName.startsWith(it) }) return

        currentPackage = packageName
        currentMessenger = packageName.messengerFromPackage(packageName)

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) {
            readChatMessages()
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    /** Синхронно перечитывает все видимые сообщения из активного окна мессенджера. */
    fun forceRefreshMessages(): Int {
        return readChatMessages()
    }

    private fun readChatMessages(): Int {
        val root = rootInActiveWindow ?: return 0
        val texts = linkedSetOf<String>()
        collectMessageTexts(root, texts)
        root.recycle()

        val filtered = texts
            .map { it.trim() }
            .filter { isLikelyChatMessage(it) }
            .toList()

        if (filtered.isNotEmpty()) {
            lastMessages.clear()
            lastMessages.addAll(filtered.takeLast(30))
        }
        return lastMessages.size
    }

    private fun isLikelyChatMessage(text: String): Boolean {
        if (text.length !in 2..800) return false
        if (text.matches(Regex("^\\d{1,2}:\\d{2}$"))) return false
        if (text.matches(Regex("^\\d+$"))) return false
        val lower = text.lowercase()
        val uiNoise = listOf(
            "telegram", "whatsapp", "instagram", "online", "last seen",
            "typing", "печатает", "в сети", "был(а)", "photo", "video",
            "voice message", "стикер", "gif"
        )
        if (uiNoise.any { lower == it || lower.startsWith("$it ") }) return false
        return true
    }

    private fun collectMessageTexts(node: AccessibilityNodeInfo, out: MutableSet<String>) {
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        listOf(text, desc).forEach { value ->
            if (!value.isNullOrBlank() && isLikelyChatMessage(value)) {
                out.add(value)
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectMessageTexts(child, out)
            child.recycle()
        }
    }

    fun insertIntoInput(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val input = findBestEditableNode(root)
        if (input == null) {
            root.recycle()
            return false
        }

        val args = android.os.Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text
            )
        }
        var ok = input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)

        if (!ok) {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("reply_ai", text))
            input.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            ok = input.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        }

        if (!ok) {
            input.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            ok = input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }

        input.recycle()
        root.recycle()
        return ok
    }

    private fun findBestEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var best: AccessibilityNodeInfo? = null
        fun walk(n: AccessibilityNodeInfo) {
            if (n.isEditable && n.isEnabled && n.isVisibleToUser) {
                val hint = n.hintText?.toString().orEmpty().lowercase()
                val isMessageField = hint.contains("message") || hint.contains("сообщ") ||
                    hint.contains("text") || n.className?.toString()?.contains("EditText") == true
                if (isMessageField || best == null) {
                    best?.recycle()
                    best = AccessibilityNodeInfo.obtain(n)
                }
            }
            for (i in 0 until n.childCount) {
                n.getChild(i)?.let { child ->
                    walk(child)
                    child.recycle()
                }
            }
        }
        walk(node)
        return best
    }

    companion object {
        private val MESSENGER_PACKAGES = listOf(
            "org.telegram.messenger",
            "org.telegram.messenger.web",
            "com.telegram.messenger",
            "com.instagram.android",
            "com.whatsapp"
        )

        @Volatile
        var instance: ReplyAIAccessibilityService? = null
            private set

        @Volatile
        var currentPackage: String? = null

        @Volatile
        var currentMessenger: String = "telegram"

        val lastMessages = CopyOnWriteArrayList<String>()

        fun getMessagesSnapshot(): List<String> = lastMessages.toList()

        fun forceRefreshMessages(): Int = instance?.forceRefreshMessages() ?: 0

        fun insertText(text: String): Boolean = instance?.insertIntoInput(text) == true
    }
}
