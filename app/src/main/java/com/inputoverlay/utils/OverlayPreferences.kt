package com.inputoverlay.utils

import android.content.Context
import android.content.SharedPreferences

class OverlayPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var isAutoDismissEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_DISMISS, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_DISMISS, value).apply()

    var isHapticEnabled: Boolean
        get() = prefs.getBoolean(KEY_HAPTIC, true)
        set(value) = prefs.edit().putBoolean(KEY_HAPTIC, value).apply()

    var isKeyboardShortcutsEnabled: Boolean
        get() = prefs.getBoolean(KEY_SHORTCUTS, true)
        set(value) = prefs.edit().putBoolean(KEY_SHORTCUTS, value).apply()

    var isDefaultUppercase: Boolean
        get() = prefs.getBoolean(KEY_DEFAULT_UPPERCASE, false)
        set(value) = prefs.edit().putBoolean(KEY_DEFAULT_UPPERCASE, value).apply()

    var shortcutLayout: String
        get() = prefs.getString(KEY_SHORTCUT_LAYOUT, KeyboardShortcutHelper.LAYOUT_QWERTY) ?: KeyboardShortcutHelper.LAYOUT_QWERTY
        set(value) = prefs.edit().putString(KEY_SHORTCUT_LAYOUT, value).apply()

    var shortcutKeys: String
        get() = prefs.getString(KEY_SHORTCUT_KEYS, KeyboardShortcutHelper.DEFAULT_SHORTCUTS_STRING) ?: KeyboardShortcutHelper.DEFAULT_SHORTCUTS_STRING
        set(value) = prefs.edit().putString(KEY_SHORTCUT_KEYS, value).apply()

    fun getShortcutList(): List<String> {
        return KeyboardShortcutHelper.parseShortcutKeys(shortcutKeys)
    }

    var defaultPresetId: String
        get() = prefs.getString(KEY_DEFAULT_PRESET, "italian") ?: "italian"
        set(value) = prefs.edit().putString(KEY_DEFAULT_PRESET, value).apply()

    var customSymbols: String
        get() = prefs.getString(KEY_CUSTOM_SYMBOLS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_SYMBOLS, value).apply()

    var overlayPosX: Int
        get() = prefs.getInt(KEY_POS_X, -1)
        set(value) = prefs.edit().putInt(KEY_POS_X, value).apply()

    var overlayPosY: Int
        get() = prefs.getInt(KEY_POS_Y, -1)
        set(value) = prefs.edit().putInt(KEY_POS_Y, value).apply()

    companion object {
        private const val PREF_NAME = "input_overlay_prefs"
        private const val KEY_AUTO_DISMISS = "key_auto_dismiss"
        private const val KEY_HAPTIC = "key_haptic"
        private const val KEY_SHORTCUTS = "key_shortcuts"
        private const val KEY_DEFAULT_UPPERCASE = "key_default_uppercase"
        private const val KEY_SHORTCUT_LAYOUT = "key_shortcut_layout"
        private const val KEY_SHORTCUT_KEYS = "key_shortcut_keys"
        private const val KEY_DEFAULT_PRESET = "key_default_preset"
        private const val KEY_CUSTOM_SYMBOLS = "key_custom_symbols"
        private const val KEY_POS_X = "key_pos_x"
        private const val KEY_POS_Y = "key_pos_y"
    }
}
