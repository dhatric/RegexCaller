package com.regexcaller.callblocker.engine

import com.regexcaller.callblocker.data.db.BlockRule
import com.regexcaller.callblocker.data.model.BlockAction

/**
 * PatternMatcher core logic engine for handling rules against numbers.
 */
object PatternMatcher {

    /**
     * Converts a wildcard pattern into a standard regex.
     */
    fun buildWildcardRegex(pattern: String): String {
        val sb = StringBuilder("^")
        for (char in pattern) {
            when (char) {
                '*'  -> sb.append(".*")
                '?'  -> sb.append(".")
                '.', '+', '^', '$', '{', '}', '[', ']', '(', ')', '|', '\\' ->
                    sb.append("\\$char")
                else -> sb.append(char)
            }
        }
        sb.append("$")
        return sb.toString()
    }

    /**
     * Generates a plain English description of what the wildcard pattern does.
     */
    fun getPatternDescription(pattern: String): String {
        if (pattern.isBlank()) return ""
        
        val hasPrefixStar = pattern.startsWith("*")
        val hasSuffixStar = pattern.endsWith("*")
        val hasQuestionMark = pattern.contains("?")
        
        // Strip out the start/end stars for the message
        val coreContent = pattern.trim('*')
        
        if (hasPrefixStar && hasSuffixStar && coreContent.isNotEmpty()) {
            return "Matches any number containing \"$coreContent\""
        }
        
        if (hasSuffixStar && coreContent.isNotEmpty()) {
            return "Matches numbers starting with \"$coreContent\""
        }
        
        if (hasPrefixStar && coreContent.isNotEmpty()) {
            return "Matches numbers ending with \"$coreContent\""
        }
        
        if (hasQuestionMark) {
            return "Matches exact length, with '?' allowing any single digit"
        }
        
        if (pattern == "*") {
            return "Matches absolutely ALL numbers"
        }
        
        return "Matches this exact number"
    }

    /**
     * Matches using wildcard semantics (exact, whole number match bounded by pattern).
     */
    fun matchWildcard(number: String, pattern: String): Boolean {
        if (pattern.isBlank()) return false
        val regex = buildWildcardRegex(pattern)
        return try {
            Regex(regex).matches(number)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Matches raw regex. Allows un-anchored substrings depending on user input regex.
     */
    fun matchRegex(number: String, pattern: String): Boolean {
        if (pattern.isBlank()) return false
        return try {
            Regex(pattern).containsMatchIn(number)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Determines if a single [rule] matches a given [incomingRaw] number.
     * Takes care of checking all matching variants of the number.
     */
    fun matches(incomingRaw: String, rule: BlockRule): Boolean {
        if (!rule.isEnabled) return false
        if (incomingRaw.isBlank()) return false

        val variants = NumberNormalizer.allVariants(incomingRaw)

        return variants.any { number ->
            if (rule.isRegex) {
                matchRegex(number, rule.pattern)
            } else {
                matchWildcard(number, rule.pattern)
            }
        }
    }

    /**
     * Evaluates a number against an entire list of rules.
     * Yields ALLOW rules first, then preserving list order (which defaults to created DESC).
     */
    fun findMatchingRule(incomingRaw: String, rules: List<BlockRule>): BlockRule? {
        val sorted = rules.sortedByDescending { it.action == BlockAction.ALLOW }
        return sorted.firstOrNull { matches(incomingRaw, it) }
    }

    /**
     * Checks if a user's typed pattern is valid or going to crash regex compiler.
     */
    fun validatePattern(pattern: String, isRegex: Boolean): String? {
        if (pattern.isBlank()) return "Pattern cannot be empty"
        return try {
            val regex = if (isRegex) pattern else buildWildcardRegex(pattern)
            Regex(regex)
            null
        } catch (e: Exception) {
            "Invalid pattern: ${e.message}"
        }
    }
}
