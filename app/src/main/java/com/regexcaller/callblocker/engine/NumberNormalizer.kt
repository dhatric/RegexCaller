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
        val variants = mutableListOf<String>()
        
        if (normalized.isEmpty()) return listOf(raw.trim()).filter { it.isNotEmpty() }
        
        // 1. Normalized
        variants.add(normalized)
        // 2. Original Raw
        variants.add(raw.trim())
        
        val digits = raw.filter { it.isDigit() }
        if (digits.isNotEmpty()) {
            // 3. Just digits (no leading +) e.g., "917096346999"
            variants.add(digits)
            
            // 4. Force a "+" prefix e.g., "+917096346999" (useful if carrier omits +)
            if (!normalized.startsWith("+")) {
                variants.add("+$digits")
            }
            
            // 5. Last 10 digits (common local number length). By generating this, 
            // rules like "7096346*" will match "+917096346999".
            if (digits.length > 10) {
                variants.add(digits.takeLast(10))
            }
        }
        
        return variants.distinct()
    }
}
