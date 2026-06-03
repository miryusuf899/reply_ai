package com.replyai.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.replyai.R

class ReplyAIAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        val isMessenger = MESSENGER_PACKAGES.any { packageName.startsWith(it) }

        if (isMessenger) {
            if (!overlayRunning) {
                FloatingOverlayService.start(this)
                overlayRunning = true
            }
        } else if (overlayRunning && !packageName.startsWith(packageName)) {
            // Keep overlay running once started — user controls via Settings
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    companion object {
        private val MESSENGER_PACKAGES = listOf(
            "org.telegram.messenger",
            "com.telegram.messenger",
            "com.instagram.android",
            "com.whatsapp"
        )

        @Volatile
        var instance: ReplyAIAccessibilityService? = null
            private set

        @Volatile
        private var overlayRunning = false

        fun insertText(text: String) {
            val service = instance
            if (service == null) {
                Toast.makeText(
                    com.replyai.ReplyAIApp.instance,
                    "Enable ReplyAI accessibility service",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
            service.performInsert(text)
        }
    }

    private fun performInsert(text: String) {
        val root = rootInActiveWindow ?: run {
            Toast.makeText(this, "No active window", Toast.LENGTH_SHORT).show()
            return
        }

        val inputNode = findEditableNode(root)
        if (inputNode == null) {
            Toast.makeText(this, R.string.accessibility_no_input, Toast.LENGTH_SHORT).show()
            root.recycle()
            return
        }

        val arguments = android.os.Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text
            )
        }

        val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            inputNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        } else {
            false
        }

        if (!success) {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("reply", text))
            inputNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        }

        inputNode.recycle()
        root.recycle()
        Toast.makeText(this, "Reply inserted", Toast.LENGTH_SHORT).show()
    }

    private fun findEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable && node.isEnabled) return node

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findEditableNode(child)
            child.recycle()
            if (found != null) return found
        }
        return null
    }
}
