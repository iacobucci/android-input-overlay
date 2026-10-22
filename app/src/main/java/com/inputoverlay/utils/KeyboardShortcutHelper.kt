package com.inputoverlay.utils

import android.view.KeyEvent

object KeyboardShortcutHelper {

    const val LAYOUT_QWERTY = "qwerty"
    const val LAYOUT_QWERTZ = "qwertz"
    const val LAYOUT_AZERTY = "azerty"
    const val LAYOUT_DVORAK = "dvorak"
    const val LAYOUT_COLEMAK = "colemak"
    const val LAYOUT_CUSTOM = "custom"

    val PRESET_QWERTY = listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P")
    val PRESET_QWERTZ = listOf("Q", "W", "E", "R", "T", "Z", "U", "I", "O", "P")
    val PRESET_AZERTY = listOf("A", "Z", "E", "R", "T", "Y", "U", "I", "O", "P")
    val PRESET_DVORAK = listOf("'", ",", ".", "P", "Y", "F", "G", "C", "R", "L")
    val PRESET_COLEMAK = listOf("Q", "W", "F", "P", "G", "J", "L", "U", "Y", ";")

    val DEFAULT_SHORTCUTS = PRESET_QWERTY
    val DEFAULT_SHORTCUTS_STRING = PRESET_QWERTY.joinToString(", ")

    fun getPresetShortcuts(layoutName: String?): List<String> {
        return when (layoutName?.trim()?.lowercase()) {
            LAYOUT_QWERTZ -> PRESET_QWERTZ
            LAYOUT_AZERTY -> PRESET_AZERTY
            LAYOUT_DVORAK -> PRESET_DVORAK
            LAYOUT_COLEMAK -> PRESET_COLEMAK
            LAYOUT_QWERTY -> PRESET_QWERTY
            else -> PRESET_QWERTY
        }
    }

    fun parseShortcutKeys(input: String?): List<String> {
        if (input.isNullOrBlank()) return DEFAULT_SHORTCUTS

        var cleaned = input.trim()
        if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
            cleaned = cleaned.substring(1, cleaned.length - 1).trim()
        }
        cleaned = cleaned.replace("\"", "").replace("'", "")

        val tokens = when {
            cleaned.contains(",") -> cleaned.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            cleaned.contains(" ") -> cleaned.split("\\s+".toRegex()).map { it.trim() }.filter { it.isNotEmpty() }
            else -> cleaned.map { it.toString() }
        }

        val result = tokens.map { it.uppercase() }
        return if (result.isEmpty()) DEFAULT_SHORTCUTS else result
    }

    fun getKeyCodeForShortcut(token: String): Int {
        val upper = token.trim().uppercase()
        if (upper.length == 1) {
            val ch = upper[0]
            if (ch in 'A'..'Z') {
                return KeyEvent.KEYCODE_A + (ch - 'A')
            }
            if (ch in '0'..'9') {
                return KeyEvent.KEYCODE_0 + (ch - '0')
            }
            when (ch) {
                ',' -> return KeyEvent.KEYCODE_COMMA
                '.' -> return KeyEvent.KEYCODE_PERIOD
                ';' -> return KeyEvent.KEYCODE_SEMICOLON
                '-' -> return KeyEvent.KEYCODE_MINUS
                '=' -> return KeyEvent.KEYCODE_EQUALS
                '[' -> return KeyEvent.KEYCODE_LEFT_BRACKET
                ']' -> return KeyEvent.KEYCODE_RIGHT_BRACKET
                '/' -> return KeyEvent.KEYCODE_SLASH
                '\\' -> return KeyEvent.KEYCODE_BACKSLASH
                '`' -> return KeyEvent.KEYCODE_GRAVE
                '\'' -> return KeyEvent.KEYCODE_APOSTROPHE
                ' ' -> return KeyEvent.KEYCODE_SPACE
            }
        }
        return when (upper) {
            "SPACE" -> KeyEvent.KEYCODE_SPACE
            "TAB" -> KeyEvent.KEYCODE_TAB
            "ENTER" -> KeyEvent.KEYCODE_ENTER
            "COMMA" -> KeyEvent.KEYCODE_COMMA
            "PERIOD", "DOT" -> KeyEvent.KEYCODE_PERIOD
            "SEMICOLON" -> KeyEvent.KEYCODE_SEMICOLON
            "MINUS" -> KeyEvent.KEYCODE_MINUS
            "PLUS" -> KeyEvent.KEYCODE_PLUS
            "SLASH" -> KeyEvent.KEYCODE_SLASH
            "BACKSLASH" -> KeyEvent.KEYCODE_BACKSLASH
            else -> {
                try {
                    val code = KeyEvent.keyCodeFromString("KEYCODE_$upper")
                    if (code != KeyEvent.KEYCODE_UNKNOWN) code else -1
                } catch (e: Throwable) {
                    -1
                }
            }
        }
    }

    fun getDefaultRowIndex(keyCode: Int): Int {
        return when (keyCode) {
            KeyEvent.KEYCODE_Q -> 0
            KeyEvent.KEYCODE_W -> 1
            KeyEvent.KEYCODE_E -> 2
            KeyEvent.KEYCODE_R -> 3
            KeyEvent.KEYCODE_T -> 4
            KeyEvent.KEYCODE_Y, KeyEvent.KEYCODE_Z -> 5
            KeyEvent.KEYCODE_U -> 6
            KeyEvent.KEYCODE_I -> 7
            KeyEvent.KEYCODE_O -> 8
            KeyEvent.KEYCODE_P -> 9
            else -> -1
        }
    }

    fun findShortcutIndex(event: KeyEvent, shortcuts: List<String>): Int {
        val keyCode = event.keyCode

        // 1. Direct keyCode match (supports QWERTZ / QWERTY interchangeability)
        val idxFromKeyCode = findShortcutIndex(keyCode, shortcuts)
        if (idxFromKeyCode != -1) {
            return idxFromKeyCode
        }

        // 2. Unshifted unicode character match
        try {
            val unshifted = event.getUnicodeChar(0).toChar().uppercaseChar()
            if (unshifted != 0.toChar()) {
                for (i in shortcuts.indices) {
                    val t = shortcuts[i].trim().uppercase()
                    if (t.length == 1 && t[0] == unshifted) {
                        return i
                    }
                }
            }
        } catch (_: Throwable) {
        }

        // 3. Match via KeyCharacterMap if available
        try {
            val kcm = event.keyCharacterMap
            if (kcm != null) {
                val chFromKcm = kcm.get(keyCode, 0).toChar().uppercaseChar()
                if (chFromKcm != 0.toChar()) {
                    for (i in shortcuts.indices) {
                        val t = shortcuts[i].trim().uppercase()
                        if (t.length == 1 && t[0] == chFromKcm) {
                            return i
                        }
                    }
                }
            }
        } catch (_: Throwable) {
        }

        // 4. Display label match
        try {
            val label = event.displayLabel.uppercaseChar()
            if (label != 0.toChar()) {
                for (i in shortcuts.indices) {
                    val t = shortcuts[i].trim().uppercase()
                    if (t.length == 1 && t[0] == label) {
                        return i
                    }
                }
            }
        } catch (_: Throwable) {
        }

        // 5. Fallback to standard top row keys (Q..P, mapping both Y and Z to index 5)
        val defaultRow = getDefaultRowIndex(keyCode)
        if (defaultRow != -1) {
            return defaultRow
        }

        return -1
    }

    fun findShortcutIndex(keyCode: Int, shortcuts: List<String>): Int {
        if (shortcuts.isEmpty()) {
            return getDefaultRowIndex(keyCode)
        }

        // Direct keyCode comparison (with QWERTZ / QWERTY interchangeability for the 6th row key)
        for (i in shortcuts.indices) {
            val token = shortcuts[i].trim().uppercase()
            val target = getKeyCodeForShortcut(token)
            if (target != -1 && target == keyCode) {
                return i
            }
            if (token == "Z" && keyCode == KeyEvent.KEYCODE_Y) {
                return i
            }
            if (token == "Y" && keyCode == KeyEvent.KEYCODE_Z) {
                return i
            }
        }

        // Fallback for A-Z
        if (keyCode in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z) {
            val ch = ('A'.code + (keyCode - KeyEvent.KEYCODE_A)).toChar()
            for (i in shortcuts.indices) {
                val t = shortcuts[i].trim().uppercase()
                if (t.length == 1 && t[0] == ch) {
                    return i
                }
            }
        }

        // Fallback for 0-9
        if (keyCode in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9) {
            val ch = ('0'.code + (keyCode - KeyEvent.KEYCODE_0)).toChar()
            for (i in shortcuts.indices) {
                val t = shortcuts[i].trim().uppercase()
                if (t.length == 1 && t[0] == ch) {
                    return i
                }
            }
        }

        return -1
    }

    fun formatMappingPreview(shortcuts: List<String>, characters: List<String>): String {
        val count = minOf(shortcuts.size, characters.size)
        if (count == 0) return "No active shortcuts"
        val pairs = mutableListOf<String>()
        for (i in 0 until count) {
            pairs.add("${shortcuts[i]} ➔ ${characters[i]}")
        }
        return pairs.joinToString("   ")
    }
}
