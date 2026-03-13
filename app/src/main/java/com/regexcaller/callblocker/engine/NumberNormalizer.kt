package com.regexcaller.callblocker.engine

object NumberNormalizer {

    fun normalize(raw: String): String {
        var number = raw.trim()
        val hasPlus = number.startsWith("+")
        number = number.filter { it.isDigit() }

        if (hasPlus && number.startsWith("91") && number.length == 12) {
            return number.substring(2)
        }
        if (number.startsWith("91") && number.length == 12) {
            return number.substring(2)
        }
        if (number.startsWith("0") && number.length == 11) {
            return number.substring(1)
        }
        return number
    }

    fun allVariants(raw: String): List<String> {
        val normalized = normalize(raw)
        if (normalized.isEmpty()) return listOf(raw.trim()).filter { it.isNotEmpty() }
        return listOf(
            normalized,
            "0$normalized",
            "91$normalized",
            "+91$normalized",
            raw.trim()
        ).distinct()
    }
}
