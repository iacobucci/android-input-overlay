package com.inputoverlay.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class InputOverlayAccessibilityService : AccessibilityService() {

    private var lastFocusedNode: AccessibilityNodeInfo? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_VIEW_FOCUSED or
                AccessibilityEvent.TYPE_VIEW_CLICKED or
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.flags = info.flags or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        serviceInfo = info
        Log.d(TAG, "InputOverlayAccessibilityService connected with key event filtering enabled")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            val source = event.source
            if (source != null && (source.isEditable || source.className?.contains("EditText", ignoreCase = true) == true)) {
                lastFocusedNode = source
            }
        }
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

    private var lastInterceptedKeyCode: Int = KeyEvent.KEYCODE_UNKNOWN

    override fun onKeyEvent(event: KeyEvent): Boolean {
        Log.d(TAG, "onKeyEvent: keyCode=${event.keyCode}, action=${event.action}, overlayVisible=${OverlayService.instance?.isOverlayVisible}")
        val overlay = OverlayService.instance
        if (overlay != null && overlay.isOverlayVisible) {
            // Dismiss on ESCAPE key
            if (event.keyCode == KeyEvent.KEYCODE_ESCAPE) {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    Log.d(TAG, "Intercepted ESCAPE -> hiding overlay")
                    overlay.hideOverlay()
                }
                return true
            }

            // Toggle Shift / Uppercase on hardware Shift key
            if (event.keyCode == KeyEvent.KEYCODE_SHIFT_LEFT || event.keyCode == KeyEvent.KEYCODE_SHIFT_RIGHT) {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    overlay.toggleShift()
                }
                return true
            }

            // Configured keyboard shortcuts (supports layouts like QWERTZ, QWERTY, AZERTY, custom, and SHIFT for uppercase)
            var charIndex = overlay.getShortcutIndex(event)
            if (charIndex == -1) {
                // Direct fallback to standard keyboard row (Q..P, mapping both Y and Z to index 5)
                charIndex = com.inputoverlay.utils.KeyboardShortcutHelper.getDefaultRowIndex(event.keyCode)
            }

            if (charIndex != -1) {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    lastInterceptedKeyCode = event.keyCode
                    val isShift = event.isShiftPressed || (event.metaState and KeyEvent.META_CAPS_LOCK_ON) != 0
                    overlay.selectCharacterByIndex(charIndex, forceUppercase = if (isShift) true else null)
                }
                return true
            }
        } else if (event.action == KeyEvent.ACTION_UP && lastInterceptedKeyCode != KeyEvent.KEYCODE_UNKNOWN && event.keyCode == lastInterceptedKeyCode) {
            lastInterceptedKeyCode = KeyEvent.KEYCODE_UNKNOWN
            return true
        }
        return super.onKeyEvent(event)
    }

    /**
     * Injects the provided text directly into the currently active/focused editable field.
     * Uses direct text splicing (ACTION_SET_TEXT) without touching the clipboard.
     * Falls back to ACTION_PASTE with automatic clipboard restoration if necessary.
     */
    fun injectText(text: String): Boolean {
        var targetNode: AccessibilityNodeInfo? = null

        if (lastFocusedNode != null && (lastFocusedNode?.isEditable == true || lastFocusedNode?.className?.contains("EditText", ignoreCase = true) == true)) {
            targetNode = lastFocusedNode
        }

        if (targetNode == null) {
            targetNode = findFocusedEditableNode(rootInActiveWindow)
        }

        // If active window didn't have the focused node, search across all open windows
        if (targetNode == null) {
            try {
                for (window in windows) {
                    val root = window.root ?: continue
                    targetNode = findFocusedEditableNode(root)
                    if (targetNode != null) break
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error searching windows for focus", e)
            }
        }

        if (targetNode == null) {
            Log.w(TAG, "No focused editable node found in any interactive window")
            return false
        }

        try {
            targetNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)

            // Direct text splicing via ACTION_SET_TEXT without touching clipboard
            val currentText = targetNode.text?.toString() ?: ""
            val selStart = targetNode.textSelectionStart
            val selEnd = targetNode.textSelectionEnd

            if (selStart >= 0 && selEnd >= selStart) {
                val safeStart = selStart.coerceAtMost(currentText.length)
                val safeEnd = selEnd.coerceAtMost(currentText.length)
                val newText = currentText.substring(0, safeStart) + text + currentText.substring(safeEnd)

                val args = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
                }
                val setSuccess = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                if (setSuccess) {
                    val newCursorPos = safeStart + text.length
                    val cursorArgs = Bundle().apply {
                        putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, newCursorPos)
                        putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, newCursorPos)
                    }
                    targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, cursorArgs)
                    Log.d(TAG, "Successfully injected '$text' directly via ACTION_SET_TEXT")
                    return true
                }
            }

            Log.w(TAG, "Could not inject text via ACTION_SET_TEXT on target node")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error injecting text into node", e)
            return false
        }
    }

    private fun findFocusedEditableNode(root: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (root == null) return null

        root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?.let { focused ->
            if (focused.isEditable || focused.isFocusable) {
                return focused
            }
        }

        root.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)?.let { focused ->
            if (focused.isEditable) {
                return focused
            }
        }

        return findFirstEditableInHierarchy(root)
    }

    private fun findFirstEditableInHierarchy(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null
        if (node.isFocused && (node.isEditable || node.className?.contains("EditText", ignoreCase = true) == true)) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findFirstEditableInHierarchy(child)
            if (found != null) {
                return found
            }
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
