package com.inputoverlay.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.Toast
import com.inputoverlay.service.InputOverlayAccessibilityService

object TextInjector {

    private const val TAG = "TextInjector"
    const val ACTION_CHARACTER_SELECTED = "com.inputoverlay.action.CHARACTER_SELECTED"
    const val EXTRA_CHARACTER = "character"
    const val EXTRA_CATEGORY = "category"

    fun injectOrCopy(context: Context, character: String, categoryId: String? = null, prefs: OverlayPreferences) {
        if (prefs.isHapticEnabled) {
            triggerHaptic(context)
        }

        // Broadcast character selection for KeyMapper / automation listeners
        broadcastCharacterSelected(context, character, categoryId)

        // Attempt direct injection via AccessibilityService
        val injected = InputOverlayAccessibilityService.instance?.injectText(character) ?: false
        if (!injected) {
            // Direct injection not possible; copy to clipboard and alert user
            copyToClipboard(context, character)
            val msg = if (InputOverlayAccessibilityService.isRunning) {
                "Copied '$character' to clipboard"
            } else {
                "Copied '$character' (Enable Accessibility in settings for auto-typing)"
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("OverlayInput", text)
        clipboard.setPrimaryClip(clip)
        Log.d(TAG, "Copied '$text' to system clipboard")
    }

    private fun broadcastCharacterSelected(context: Context, character: String, categoryId: String?) {
        val intent = Intent(ACTION_CHARACTER_SELECTED).apply {
            putExtra(EXTRA_CHARACTER, character)
            if (categoryId != null) {
                putExtra(EXTRA_CATEGORY, categoryId)
            }
            // Send broadcast to this package or explicitly for KeyMapper
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
