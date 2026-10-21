package com.inputoverlay.model

object PresetData {

    val ITALIAN_ACCENTS = SymbolCategory(
        id = "italian",
        name = "Italian Accents",
        shortCode = "IT",
        items = listOf(
            SymbolItem("à", "À"),
            SymbolItem("è", "È"),
            SymbolItem("é", "É"),
            SymbolItem("ì", "Ì"),
            SymbolItem("ò", "Ò"),
            SymbolItem("ù", "Ù")
        )
    )

    val VOWEL_E = SymbolCategory(
        id = "e",
        name = "E Accents",
        shortCode = "E",
        items = listOf(
            SymbolItem("è", "È"),
            SymbolItem("é", "É"),
            SymbolItem("ê", "Ê"),
            SymbolItem("ë", "Ë")
        )
    )

    val VOWEL_A = SymbolCategory(
        id = "a",
        name = "A Accents",
        shortCode = "A",
        items = listOf(
            SymbolItem("à", "À"),
            SymbolItem("á", "Á"),
            SymbolItem("â", "Â"),
            SymbolItem("ä", "Ä"),
            SymbolItem("ã", "Ã")
        )
    )

    val VOWEL_I = SymbolCategory(
        id = "i",
        name = "I Accents",
        shortCode = "I",
        items = listOf(
            SymbolItem("ì", "Ì"),
            SymbolItem("í", "Í"),
            SymbolItem("î", "Î"),
            SymbolItem("ï", "Ï")
        )
    )

    val VOWEL_O = SymbolCategory(
        id = "o",
        name = "O Accents",
        shortCode = "O",
        items = listOf(
            SymbolItem("ò", "Ò"),
            SymbolItem("ó", "Ó"),
            SymbolItem("ô", "Ô"),
            SymbolItem("ö", "Ö"),
            SymbolItem("õ", "Õ")
        )
    )

    val VOWEL_U = SymbolCategory(
        id = "u",
        name = "U Accents",
        shortCode = "U",
        items = listOf(
            SymbolItem("ù", "Ù"),
            SymbolItem("ú", "Ú"),
            SymbolItem("û", "Û"),
            SymbolItem("ü", "Ü")
        )
    )

    val SPANISH_ACCENTS = SymbolCategory(
        id = "spanish",
        name = "Spanish Accents",
        shortCode = "ES",
        items = listOf(
            SymbolItem("á", "Á"),
            SymbolItem("é", "É"),
            SymbolItem("í", "Í"),
            SymbolItem("ó", "Ó"),
            SymbolItem("ú", "Ú"),
            SymbolItem("ü", "Ü"),
            SymbolItem("ñ", "Ñ"),
            SymbolItem("¿", "¿"),
            SymbolItem("¡", "¡")
        )
    )

    val FRENCH_ACCENTS = SymbolCategory(
        id = "french",
        name = "French Accents",
        shortCode = "FR",
        items = listOf(
            SymbolItem("à", "À"),
            SymbolItem("â", "Â"),
            SymbolItem("æ", "Æ"),
            SymbolItem("ç", "Ç"),
            SymbolItem("é", "É"),
            SymbolItem("è", "È"),
            SymbolItem("ê", "Ê"),
            SymbolItem("ë", "Ë"),
            SymbolItem("î", "Î"),
            SymbolItem("ï", "Ï"),
            SymbolItem("ô", "Ô"),
            SymbolItem("œ", "Œ"),
            SymbolItem("ù", "Ù"),
            SymbolItem("û", "Û"),
            SymbolItem("ü", "Ü")
        )
    )

    val GERMAN_ACCENTS = SymbolCategory(
        id = "german",
        name = "German Accents",
        shortCode = "DE",
        items = listOf(
            SymbolItem("ä", "Ä"),
            SymbolItem("ö", "Ö"),
            SymbolItem("ü", "Ü"),
            SymbolItem("ß", "ẞ")
        )
    )

    val TYPOGRAPHY = SymbolCategory(
        id = "typography",
        name = "Typography & Quotes",
        shortCode = "«»",
        items = listOf(
            SymbolItem("«", "«"),
            SymbolItem("»", "»"),
            SymbolItem("“", "“"),
            SymbolItem("”", "”"),
            SymbolItem("‘", "‘"),
            SymbolItem("’", "’"),
            SymbolItem("—", "—"),
            SymbolItem("–", "–"),
            SymbolItem("…", "…"),
            SymbolItem("•", "•"),
            SymbolItem("°", "°")
        )
    )

    val MATH = SymbolCategory(
        id = "math",
        name = "Math",
        shortCode = "±",
        items = listOf(
            SymbolItem("×", "×"),
            SymbolItem("÷", "÷"),
            SymbolItem("±", "±"),
            SymbolItem("≠", "≠"),
            SymbolItem("≈", "≈"),
            SymbolItem("≤", "≤"),
            SymbolItem("≥", "≥"),
            SymbolItem("√", "√"),
            SymbolItem("∞", "∞"),
            SymbolItem("π", "π")
        )
    )

    val CURRENCY = SymbolCategory(
        id = "currency",
        name = "Currency",
        shortCode = "€",
        items = listOf(
            SymbolItem("€", "€"),
            SymbolItem("$", "$"),
            SymbolItem("£", "£"),
            SymbolItem("¥", "¥"),
            SymbolItem("¢", "¢"),
            SymbolItem("₹", "₹"),
            SymbolItem("₿", "₿")
        )
    )

    val ALL_PRESETS = listOf(
        ITALIAN_ACCENTS,
        SPANISH_ACCENTS,
        FRENCH_ACCENTS,
        GERMAN_ACCENTS,
        TYPOGRAPHY,
        MATH,
        CURRENCY
    )

    fun findCategoryById(id: String?): SymbolCategory {
        if (id == null) return ITALIAN_ACCENTS
        val normalized = id.trim().lowercase()
        return when (normalized) {
            "italian", "it" -> ITALIAN_ACCENTS
            "e" -> VOWEL_E
            "a" -> VOWEL_A
            "i" -> VOWEL_I
            "o" -> VOWEL_O
            "u" -> VOWEL_U
            "spanish", "es" -> SPANISH_ACCENTS
            "french", "fr" -> FRENCH_ACCENTS
            "german", "de" -> GERMAN_ACCENTS
            "typography", "quotes", "symbols" -> TYPOGRAPHY
            "math" -> MATH
            "currency" -> CURRENCY
            else -> ALL_PRESETS.find { it.id.equals(normalized, ignoreCase = true) } ?: ITALIAN_ACCENTS
        }
    }

    fun parseCustomCharacters(input: String): List<SymbolItem> {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ITALIAN_ACCENTS.items

        val tokens = if (trimmed.contains(",")) {
            trimmed.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else if (trimmed.contains(" ")) {
            trimmed.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        } else {
            // Sequence of characters
            trimmed.map { it.toString() }
        }

        return tokens.map { char ->
            SymbolItem(lower = char.lowercase(), upper = char.uppercase())
        }
    }
}
