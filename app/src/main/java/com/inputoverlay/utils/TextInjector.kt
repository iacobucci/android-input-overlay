package com.inputoverlay.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.inputoverlay.service.InputOverlayAccessibilityService
import com.inputoverlay.service.InputOverlayImeService

object TextInjector {

    private const val TAG = "TextInjector"
    const val ACTION_CHARACTER_SELECTED = "com.inputoverlay.action.CHARACTER_SELECTED"
    const val EXTRA_CHARACTER = "character"
    const val EXTRA_CATEGORY = "category"

    /**
     * Injects the character directly without ever touching or modifying the system clipboard.
     * 1. Priority 1: InputConnection via InputOverlayImeService (works in Termux, TerminalView, webviews, and all apps)
     * 2. Priority 2: Direct text splicing via AccessibilityNodeInfo (ACTION_SET_TEXT) on focused EditTexts
     */
    fun injectDirectly(context: Context, character: String, categoryId: String? = null, prefs: OverlayPreferences) {
        if (prefs.isHapticEnabled) {
            triggerHaptic(context)
        }

        // Broadcast character selection for KeyMapper / automation listeners
        broadcastCharacterSelected(context, character, categoryId)

        // Priority 1: Native InputMethod InputConnection (ideal for Termux & any terminal or custom view)
        val imeService = InputOverlayImeService.instance
        if (imeService != null && imeService.commitChar(character)) {
            Log.d(TAG, "Successfully injected '$character' directly via InputConnection")
            return
        }

        // Priority 2: Direct Accessibility text insertion (for standard native input fields)
        val a11yService = InputOverlayAccessibilityService.instance
        if (a11yService != null && a11yService.injectText(character)) {
            Log.d(TAG, "Successfully injected '$character' directly via AccessibilityService ACTION_SET_TEXT")
            return
        }

        Log.w(TAG, "Could not inject '$character': InputOverlay Keyboard or Accessibility service not active on focused field")
    }

    private fun broadcastCharacterSelected(context: Context, character: String, categoryId: String?) {
        val intent = Intent(ACTION_CHARACTER_SELECTED).apply {
            putExtra(EXTRA_CHARACTER, character)
            if (categoryId != null) {
                putExtra(EXTRA_CATEGORY, categoryId)
            }
            setPackage(context.packageName)
        }
        context.sendBroadcast(intent)
    }

    @Suppress("DEPRECATION")
    private fun triggerHaptic(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                )
            } else {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator?.vibrate(20)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to perform haptic feedback", e)
        }
    }
}
