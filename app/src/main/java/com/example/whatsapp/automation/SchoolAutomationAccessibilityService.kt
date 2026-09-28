package com.example.whatsapp.automation

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Resilient Background Automation Service for Designated Administrator Devices.
 *
 * Runs as an Android AccessibilityService to automate WhatsApp message transmission
 * without requiring manual user interaction, touch inputs, or browser links.
 */
class SchoolAutomationAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName.contains("whatsapp", ignoreCase = true)) {
            val rootNode = rootInActiveWindow ?: return
            try {
                findAndClickSendButton(rootNode)
            } catch (_: Exception) {}
        }
    }

    /**
     * Traverses the view hierarchy on WhatsApp screen to locate the send button and perform click.
     */
    private fun findAndClickSendButton(rootNode: AccessibilityNodeInfo) {
        // Look for send button by view ID or content description
        val sendNodes = rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send")
        if (sendNodes.isNotEmpty()) {
            val sendButton = sendNodes[0]
            if (sendButton.isVisibleToUser && sendButton.isClickable) {
                sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return
            }
        }

        // Fallback by content description "Send"
        val byDesc = rootNode.findAccessibilityNodeInfosByText("Send")
        for (node in byDesc) {
            if (node.isClickable) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return
            }
        }
    }

    override fun onInterrupt() {
        // Service interrupted by OS
    }

    companion object {
        var isServiceActive: Boolean = false
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceActive = true
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceActive = false
    }
}
