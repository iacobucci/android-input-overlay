package com.inputoverlay

import com.inputoverlay.model.PresetData
import com.inputoverlay.model.SymbolItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SymbolItemTest {

    @Test
    fun testSymbolDisplay() {
        val item = SymbolItem(lower = "à", upper = "À")
        assertEquals("à", item.getDisplay(isUppercase = false))
        assertEquals("À", item.getDisplay(isUppercase = true))
    }

    @Test
    fun testDefaultUppercaseFallback() {
        val item = SymbolItem(lower = "è")
        assertEquals("è", item.getDisplay(isUppercase = false))
        assertEquals("È", item.getDisplay(isUppercase = true))
    }
}

class PresetDataTest {

    @Test
    fun testItalianAccents() {
        val italian = PresetData.ITALIAN_ACCENTS
        assertEquals("italian", italian.id)
        assertEquals("IT", italian.shortCode)

        val expectedChars = listOf("à", "è", "é", "ì", "ò", "ù")
        val actualChars = italian.items.map { it.lower }
        assertEquals(expectedChars, actualChars)

        val expectedUpper = listOf("À", "È", "É", "Ì", "Ò", "Ù")
        val actualUpper = italian.items.map { it.upper }
        assertEquals(expectedUpper, actualUpper)
    }

    @Test
    fun testFindCategoryById() {
        assertEquals(PresetData.ITALIAN_ACCENTS, PresetData.findCategoryById("italian"))
        assertEquals(PresetData.ITALIAN_ACCENTS, PresetData.findCategoryById("IT"))
        assertEquals(PresetData.ITALIAN_ACCENTS, PresetData.findCategoryById(null))
        assertEquals(PresetData.SPANISH_ACCENTS, PresetData.findCategoryById("spanish"))
        assertEquals(PresetData.FRENCH_ACCENTS, PresetData.findCategoryById("FR"))
        assertEquals(PresetData.GERMAN_ACCENTS, PresetData.findCategoryById("german"))
        assertEquals(PresetData.VOWEL_E, PresetData.findCategoryById("e"))
        assertEquals(PresetData.TYPOGRAPHY, PresetData.findCategoryById("symbols"))
    }

    @Test
    fun testParseCustomCharactersCommaSeparated() {
        val input = "à, è, é, ì"
        val items = PresetData.parseCustomCharacters(input)
        assertEquals(4, items.size)
        assertEquals("à", items[0].lower)
        assertEquals("è", items[1].lower)
        assertEquals("é", items[2].lower)
        assertEquals("ì", items[3].lower)
        assertEquals("À", items[0].upper)
    }

    @Test
    fun testParseCustomCharactersContinuousString() {
        val input = "àèéìòù"
        val items = PresetData.parseCustomCharacters(input)
        assertEquals(6, items.size)
        assertEquals("à", items[0].lower)
        assertEquals("ù", items[5].lower)
    }
}
