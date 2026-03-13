package com.regexcaller.callblocker.engine

object NumberNormalizer {

    fun normalize(raw: String): String {
        // Strip out non-digit characters, except an optional leading plus
        val hasPlus = raw.trim().startsWith("+")
        val digits = raw.filter { it.isDigit() }
        return if (hasPlus) "+$digits" else digits
    }

    fun allVariants(raw: String): List<String> {
        val normalized = normalize(raw)
        if (normalized.isEmpty()) return listOf(raw.trim()).filter { it.isNotEmpty() }
        
        // Remove trailing variants like "+91" that are India-specific, and just keep raw and normalized
        return listOf(
            normalized,
            raw.trim()
        ).distinct()
    }
}
