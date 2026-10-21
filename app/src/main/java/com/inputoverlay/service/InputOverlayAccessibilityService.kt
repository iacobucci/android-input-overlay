package com.inputoverlay.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class InputOverlayAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_FOCUSED or
                AccessibilityEvent.TYPE_VIEW_CLICKED or
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        serviceInfo = info
        Log.d(TAG, "InputOverlayAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No periodic processing needed; service is queried directly when injecting characters
    }

    override fun onInterrupt() {
        Log.d(TAG, "InputOverlayAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        Log.d(TAG, "InputOverlayAccessibilityService destroyed")
    }

    /**
     * Injects the provided text directly into the currently active/focused editable field.
     * Returns true if injection succeeded, false otherwise.
     */
    fun injectText(text: String): Boolean {
        val root = rootInActiveWindow
        val targetNode = findFocusedEditableNode(root)

        if (targetNode == null) {
            Log.w(TAG, "No focused editable node found in active window")
            return false
        }

        try {
            // First attempt: simulate PASTE action using ClipboardManager
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("OverlayInput", text)
            clipboard.setPrimaryClip(clip)

            val actions = targetNode.actionList.map { it.id }
            if (actions.contains(AccessibilityNodeInfo.ACTION_PASTE)) {
                val pasteSuccess = targetNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                if (pasteSuccess) {
                    Log.d(TAG, "Successfully injected '$text' via ACTION_PASTE")
                    return true
                }
            }

            // Fallback attempt: Manual text splicing via ACTION_SET_TEXT
            if (actions.contains(AccessibilityNodeInfo.ACTION_SET_TEXT)) {
                val currentText = targetNode.text?.toString() ?: ""
                val selStart = targetNode.textSelectionStart.coerceAtLeast(0)
                val selEnd = targetNode.textSelectionEnd.coerceAtLeast(selStart)

                val safeStart = selStart.coerceAtMost(currentText.length)
                val safeEnd = selEnd.coerceAtMost(currentText.length)

                val newText = currentText.substring(0, safeStart) + text + currentText.substring(safeEnd)
                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
                }
                val setSuccess = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                if (setSuccess) {
                    // Try to move cursor immediately after inserted text
                    val newCursorPos = safeStart + text.length
                    val cursorArgs = Bundle().apply {
                        putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, newCursorPos)
                        putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, newCursorPos)
                    }
                    targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, cursorArgs)
                    Log.d(TAG, "Successfully injected '$text' via ACTION_SET_TEXT")
                    return true
                }
            }

            Log.w(TAG, "Could not inject text: neither paste nor set_text succeeded")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error injecting text into node", e)
            return false
        } finally {
            targetNode.recycle()
            root?.recycle()
        }
    }

    private fun findFocusedEditableNode(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null

        // Direct input focus check
        root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.let { focused ->
            if (focused.isEditable || focused.isFocusable) {
                return focused
            }
        }

        // Accessibility focus check
        root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)?.let { focused ->
            if (focused.isEditable) {
                return focused
            }
        }

        // Tree traversal fallback for custom views / webviews
        return findFirstEditableInHierarchy(root)
    }

    private fun findFirstEditableInHierarchy(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isFocused && (node.isEditable || node.className?.contains("EditText", ignoreCase = true) == true)) {
            return AccessibilityNodeInfo.obtain(node)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findFirstEditableInHierarchy(child)
            if (found != null) {
                return found
            }
            child.recycle()
        }
        return null
    }

    companion object {
        private const val TAG = "OverlayAccessibility"
        @Volatile
        var instance: InputOverlayAccessibilityService? = null
            private set

        val isRunning: Boolean
            get() = instance != null
    }
}
