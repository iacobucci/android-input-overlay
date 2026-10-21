package com.inputoverlay.service

import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.View

class InputOverlayImeService : InputMethodService() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "InputOverlayImeService created")
    }

    override fun onCreateInputView(): View? {
        // Return null: no soft keyboard needed on physical keyboard devices (BlackBerry KEY2)
        return null
    }

    override fun onDestroy() {
        if (instance == this) {
            instance = null
        }
        super.onDestroy()
        Log.d(TAG, "InputOverlayImeService destroyed")
    }

    /**
     * Commits text directly into the active InputConnection.
     * Works natively in Termux, TerminalView, webviews, and all text fields.
     */
    fun commitChar(text: String): Boolean {
        val ic = currentInputConnection ?: return false
        val success = ic.commitText(text, 1)
        Log.d(TAG, "commitText('$text') result: $success")
        return success
    }

    /**
     * Simulates a key event combination (e.g. with Alt, Ctrl, Shift modifiers) directly via InputConnection.
     */
    fun sendKeyCombination(keyCode: Int, metaState: Int = 0): Boolean {
        val ic = currentInputConnection ?: return false
        val now = SystemClock.uptimeMillis()
        val eventDown = KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, metaState)
        val eventUp = KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, metaState)
        val downSuccess = ic.sendKeyEvent(eventDown)
        val upSuccess = ic.sendKeyEvent(eventUp)
        Log.d(TAG, "sendKeyCombination(keyCode=$keyCode, meta=$metaState) down=$downSuccess, up=$upSuccess")
        return downSuccess && upSuccess
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Pass through all hardware keys to the focused application
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return super.onKeyUp(keyCode, event)
    }

    companion object {
        private const val TAG = "InputOverlayIme"

        @Volatile
        var instance: InputOverlayImeService? = null
            private set

        val isImeActive: Boolean
            get() = instance?.currentInputConnection != null
    }
}
