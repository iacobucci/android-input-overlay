package com.inputoverlay.model

data class SymbolItem(
    val lower: String,
    val upper: String = lower.uppercase(),
    val label: String? = null
) {
    fun getDisplay(isUppercase: Boolean): String {
        return if (isUppercase) upper else lower
    }
}

data class SymbolCategory(
    val id: String,
    val name: String,
    val shortCode: String,
    val items: List<SymbolItem>
)
