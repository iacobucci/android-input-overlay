package com.inputoverlay

import android.view.KeyEvent
import com.inputoverlay.utils.KeyboardShortcutHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardShortcutHelperTest {

    @Test
    fun testParseShortcutKeysCommaSeparated() {
        val input = "q, w, e, r, t, z"
        val parsed = KeyboardShortcutHelper.parseShortcutKeys(input)
        assertEquals(listOf("Q", "W", "E", "R", "T", "Z"), parsed)
    }

    @Test
    fun testParseShortcutKeysJsonArray() {
        val input = "[\"q\",\"w\",\"e\",\"r\",\"t\",\"z\"]"
        val parsed = KeyboardShortcutHelper.parseShortcutKeys(input)
        assertEquals(listOf("Q", "W", "E", "R", "T", "Z"), parsed)
    }

    @Test
    fun testParseShortcutKeysSingleQuotes() {
        val input = "['q', 'w', 'e', 'r', 't', 'z']"
        val parsed = KeyboardShortcutHelper.parseShortcutKeys(input)
        assertEquals(listOf("Q", "W", "E", "R", "T", "Z"), parsed)
    }

    @Test
    fun testParseShortcutKeysContinuousString() {
        val input = "qwertz"
        val parsed = KeyboardShortcutHelper.parseShortcutKeys(input)
        assertEquals(listOf("Q", "W", "E", "R", "T", "Z"), parsed)
    }

    @Test
    fun testParseShortcutKeysSpaceSeparated() {
        val input = "q w e r t z"
        val parsed = KeyboardShortcutHelper.parseShortcutKeys(input)
        assertEquals(listOf("Q", "W", "E", "R", "T", "Z"), parsed)
    }

    @Test
    fun testParseShortcutKeysBlankFallback() {
        val parsedEmpty = KeyboardShortcutHelper.parseShortcutKeys("")
        assertEquals(KeyboardShortcutHelper.DEFAULT_SHORTCUTS, parsedEmpty)

        val parsedNull = KeyboardShortcutHelper.parseShortcutKeys(null)
        assertEquals(KeyboardShortcutHelper.DEFAULT_SHORTCUTS, parsedNull)
    }

    @Test
    fun testPresetLayouts() {
        val qwertz = KeyboardShortcutHelper.getPresetShortcuts("qwertz")
        assertEquals("Z", qwertz[5])
        assertEquals("T", qwertz[4])

        val qwerty = KeyboardShortcutHelper.getPresetShortcuts("qwerty")
        assertEquals("Y", qwerty[5])

        val azerty = KeyboardShortcutHelper.getPresetShortcuts("azerty")
        assertEquals("A", azerty[0])
        assertEquals("Z", azerty[1])
        assertEquals("E", azerty[2])
    }

    @Test
    fun testFormatMappingPreview() {
        val shortcuts = listOf("Q", "W", "E", "R", "T", "Z")
        val characters = listOf("à", "è", "é", "ì", "ò", "ù")
        val preview = KeyboardShortcutHelper.formatMappingPreview(shortcuts, characters)
        assertEquals("Q ➔ à   W ➔ è   E ➔ é   R ➔ ì   T ➔ ò   Z ➔ ù", preview)
    }

    @Test
    fun testFindShortcutIndexByKeyCode() {
        val shortcuts = listOf("Q", "W", "E", "R", "T", "Z")
        assertEquals(0, KeyboardShortcutHelper.findShortcutIndex(KeyEvent.KEYCODE_Q, shortcuts))
        assertEquals(1, KeyboardShortcutHelper.findShortcutIndex(KeyEvent.KEYCODE_W, shortcuts))
        assertEquals(2, KeyboardShortcutHelper.findShortcutIndex(KeyEvent.KEYCODE_E, shortcuts))
        assertEquals(3, KeyboardShortcutHelper.findShortcutIndex(KeyEvent.KEYCODE_R, shortcuts))
        assertEquals(4, KeyboardShortcutHelper.findShortcutIndex(KeyEvent.KEYCODE_T, shortcuts))
        assertEquals(5, KeyboardShortcutHelper.findShortcutIndex(KeyEvent.KEYCODE_Z, shortcuts))
        assertEquals(-1, KeyboardShortcutHelper.findShortcutIndex(KeyEvent.KEYCODE_Y, shortcuts))
    }

    @Test
    fun testFunctionMappingQwertzToAccents() {
        // Issue #1: ["q","w","e","r","t","z"] -> ["à","è","é","ì","ò","ù"]
        val keys = KeyboardShortcutHelper.parseShortcutKeys("[\"q\",\"w\",\"e\",\"r\",\"t\",\"z\"]")
        val accents = listOf("à", "è", "é", "ì", "ò", "ù")
        
        // Test function f: key k -> character c
        fun f(keyCode: Int): String? {
            val idx = KeyboardShortcutHelper.findShortcutIndex(keyCode, keys)
            return if (idx in accents.indices) accents[idx] else null
        }

        assertEquals("à", f(KeyEvent.KEYCODE_Q))
        assertEquals("è", f(KeyEvent.KEYCODE_W))
        assertEquals("é", f(KeyEvent.KEYCODE_E))
        assertEquals("ì", f(KeyEvent.KEYCODE_R))
        assertEquals("ò", f(KeyEvent.KEYCODE_T))
        assertEquals("ù", f(KeyEvent.KEYCODE_Z))
        assertEquals(null, f(KeyEvent.KEYCODE_Y))
    }

    @Test
    fun testNumericAndPunctuationShortcuts() {
        val numShortcuts = KeyboardShortcutHelper.parseShortcutKeys("1, 2, 3")
        assertEquals(0, KeyboardShortcutHelper.findShortcutIndex(KeyEvent.KEYCODE_1, numShortcuts))
        assertEquals(1, KeyboardShortcutHelper.findShortcutIndex(KeyEvent.KEYCODE_2, numShortcuts))
        assertEquals(2, KeyboardShortcutHelper.findShortcutIndex(KeyEvent.KEYCODE_3, numShortcuts))

        val punctShortcuts = KeyboardShortcutHelper.parseShortcutKeys("; - .")
        assertEquals(0, KeyboardShortcutHelper.findShortcutIndex(KeyEvent.KEYCODE_SEMICOLON, punctShortcuts))
        assertEquals(1, KeyboardShortcutHelper.findShortcutIndex(KeyEvent.KEYCODE_MINUS, punctShortcuts))
        assertEquals(2, KeyboardShortcutHelper.findShortcutIndex(KeyEvent.KEYCODE_PERIOD, punctShortcuts))

        val namedShortcuts = KeyboardShortcutHelper.parseShortcutKeys("SEMICOLON, COMMA, PERIOD")
        assertEquals(0, KeyboardShortcutHelper.findShortcutIndex(KeyEvent.KEYCODE_SEMICOLON, namedShortcuts))
        assertEquals(1, KeyboardShortcutHelper.findShortcutIndex(KeyEvent.KEYCODE_COMMA, namedShortcuts))
        assertEquals(2, KeyboardShortcutHelper.findShortcutIndex(KeyEvent.KEYCODE_PERIOD, namedShortcuts))
    }
}
