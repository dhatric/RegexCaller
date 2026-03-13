package com.regexcaller.callblocker.engine

import com.regexcaller.callblocker.data.db.BlockRule
import com.regexcaller.callblocker.data.model.BlockAction
import org.junit.Assert.*
import org.junit.Test

class PatternMatcherTest {

    // --- 4.1: buildWildcardRegex ---
    @Test
    fun `buildWildcardRegex converts star to dot-star`() {
        assertEquals("^98765.*$", PatternMatcher.buildWildcardRegex("98765*"))
    }

    @Test
    fun `buildWildcardRegex converts question mark to single dot`() {
        assertEquals("^9876.00000$", PatternMatcher.buildWildcardRegex("9876?00000"))
    }

    @Test
    fun `buildWildcardRegex escapes regex special characters`() {
        // Pattern with literal dots and plus signs should be escaped
        assertEquals("^\\+91\\..*$", PatternMatcher.buildWildcardRegex("+91.*"))
    }

    @Test
    fun `buildWildcardRegex handles plain number as exact match`() {
        assertEquals("^9876500000$", PatternMatcher.buildWildcardRegex("9876500000"))
    }

    // --- 4.2: matchWildcard prefix ---
    @Test
    fun `matchWildcard matches prefix pattern`() {
        assertTrue(PatternMatcher.matchWildcard("9876500000", "98765*"))
        assertTrue(PatternMatcher.matchWildcard("9876512345", "98765*"))
    }

    @Test
    fun `matchWildcard rejects non-matching prefix`() {
        assertFalse(PatternMatcher.matchWildcard("9876400000", "98765*"))
        assertFalse(PatternMatcher.matchWildcard("1234500000", "98765*"))
    }

    // --- 4.3: matchWildcard suffix ---
    @Test
    fun `matchWildcard matches suffix pattern`() {
        assertTrue(PatternMatcher.matchWildcard("9876500000", "*00000"))
        assertTrue(PatternMatcher.matchWildcard("1111100000", "*00000"))
    }

    @Test
    fun `matchWildcard rejects non-matching suffix`() {
        assertFalse(PatternMatcher.matchWildcard("9876500001", "*00000"))
    }

    // --- 4.4: matchWildcard single-digit ? ---
    @Test
    fun `matchWildcard question mark matches exactly one digit`() {
        assertTrue(PatternMatcher.matchWildcard("9876500000", "9876?00000"))
        assertTrue(PatternMatcher.matchWildcard("9876100000", "9876?00000"))
        assertTrue(PatternMatcher.matchWildcard("9876900000", "9876?00000"))
    }

    @Test
    fun `matchWildcard question mark does not match zero or multiple digits`() {
        // "9876?00000" has 10 chars total with ? as 1 -> expects 10-digit number
        assertFalse(PatternMatcher.matchWildcard("987600000", "9876?00000"))   // 9 digits
        assertFalse(PatternMatcher.matchWildcard("98765500000", "9876?00000")) // 11 digits
    }

    // --- 4.5: matchRegex ---
    @Test
    fun `matchRegex matches valid regex pattern`() {
        assertTrue(PatternMatcher.matchRegex("+919876500000", "^\\+9198765.*"))
    }

    @Test
    fun `matchRegex uses containsMatchIn (partial match)`() {
        assertTrue(PatternMatcher.matchRegex("9876500000", "98765"))
    }

    @Test
    fun `matchRegex returns false for non-match`() {
        assertFalse(PatternMatcher.matchRegex("9876500000", "^12345.*"))
    }

    @Test
    fun `matchRegex returns false for invalid regex (no crash)`() {
        assertFalse(PatternMatcher.matchRegex("9876500000", "[invalid"))
    }

    // --- 4.6: matches() with normalization ---
    @Test
    fun `matches returns true when normalized number matches wildcard rule`() {
        val rule = BlockRule(label = "Test", pattern = "98765*", isEnabled = true)
        assertTrue(PatternMatcher.matches("9876500000", rule))
        assertTrue(PatternMatcher.matches("(987) 650-0000", rule))
    }

    @Test
    fun `matches returns false for disabled rule`() {
        val rule = BlockRule(label = "Disabled", pattern = "98765*", isEnabled = false)
        assertFalse(PatternMatcher.matches("9876500000", rule))
    }

    @Test
    fun `matches works with regex rule across variants`() {
        val rule = BlockRule(label = "Regex", pattern = "^\\+9198765.*", isRegex = true, isEnabled = true)
        // The +91 variant should match
        assertTrue(PatternMatcher.matches("+919876500000", rule))
        // "9876500000" won't match anymore because allVariants doesn't prepend +91
        assertFalse(PatternMatcher.matches("9876500000", rule)) 
    }

    // --- 4.7: findMatchingRule with ALLOW precedence ---
    @Test
    fun `findMatchingRule returns first matching BLOCK rule`() {
        val rules = listOf(
            BlockRule(id = 1, label = "Spam", pattern = "98765*", action = BlockAction.BLOCK, isEnabled = true),
            BlockRule(id = 2, label = "Other", pattern = "11111*", action = BlockAction.BLOCK, isEnabled = true)
        )
        val match = PatternMatcher.findMatchingRule("9876500000", rules)
        assertNotNull(match)
        assertEquals("Spam", match!!.label)
    }

    @Test
    fun `findMatchingRule returns null when no rules match`() {
        val rules = listOf(
            BlockRule(id = 1, label = "Spam", pattern = "98765*", action = BlockAction.BLOCK, isEnabled = true)
        )
        val match = PatternMatcher.findMatchingRule("1111100000", rules)
        assertNull(match)
    }

    @Test
    fun `findMatchingRule ALLOW takes precedence over BLOCK`() {
        val rules = listOf(
            BlockRule(id = 1, label = "Block All", pattern = "98765*", action = BlockAction.BLOCK, isEnabled = true),
            BlockRule(id = 2, label = "Allow Bank", pattern = "9876500000", action = BlockAction.ALLOW, isEnabled = true)
        )
        // Even though BLOCK rule matches, ALLOW should win
        val match = PatternMatcher.findMatchingRule("9876500000", rules)
        assertNotNull(match)
        assertEquals(BlockAction.ALLOW, match!!.action)
        assertEquals("Allow Bank", match.label)
    }

    @Test
    fun `findMatchingRule skips disabled rules`() {
        val rules = listOf(
            BlockRule(id = 1, label = "Disabled", pattern = "98765*", action = BlockAction.BLOCK, isEnabled = false),
            BlockRule(id = 2, label = "Active", pattern = "98765*", action = BlockAction.SILENCE, isEnabled = true)
        )
        val match = PatternMatcher.findMatchingRule("9876500000", rules)
        assertNotNull(match)
        assertEquals(BlockAction.SILENCE, match!!.action)
    }

    @Test
    fun `findMatchingRule returns ALLOW even when BLOCK is listed first`() {
        // Testing that ALLOW rules are sorted to the front
        val rules = listOf(
            BlockRule(id = 1, label = "Block", pattern = "98765*", action = BlockAction.BLOCK, isEnabled = true),
            BlockRule(id = 2, label = "Allow", pattern = "98765*", action = BlockAction.ALLOW, isEnabled = true)
        )
        val match = PatternMatcher.findMatchingRule("9876500000", rules)
        assertNotNull(match)
        assertEquals(BlockAction.ALLOW, match!!.action)
    }

    // --- 4.8: validatePattern ---
    @Test
    fun `validatePattern returns null for valid wildcard pattern`() {
        assertNull(PatternMatcher.validatePattern("98765*", isRegex = false))
        assertNull(PatternMatcher.validatePattern("*1234", isRegex = false))
        assertNull(PatternMatcher.validatePattern("9876?00000", isRegex = false))
    }

    @Test
    fun `validatePattern returns null for valid regex pattern`() {
        assertNull(PatternMatcher.validatePattern("^\\+9198765.*", isRegex = true))
        assertNull(PatternMatcher.validatePattern("[0-9]+", isRegex = true))
    }

    @Test
    fun `validatePattern returns error for empty pattern`() {
        val error = PatternMatcher.validatePattern("", isRegex = false)
        assertNotNull(error)
        assertTrue(error!!.contains("empty"))
    }

    @Test
    fun `validatePattern returns error for blank pattern`() {
        val error = PatternMatcher.validatePattern("   ", isRegex = false)
        assertNotNull(error)
    }

    @Test
    fun `validatePattern returns error for invalid regex`() {
        val error = PatternMatcher.validatePattern("[unclosed", isRegex = true)
        assertNotNull(error)
        assertTrue(error!!.contains("Invalid pattern"))
    }

    // --- 4.10: Edge cases ---
    @Test
    fun `matchWildcard returns false for malformed internal pattern`() {
        // Even if somehow a bad pattern gets through validation
        assertFalse(PatternMatcher.matchWildcard("123", ""))
    }

    @Test
    fun `matches handles empty incoming number gracefully`() {
        val rule = BlockRule(label = "Test", pattern = "98765*", isEnabled = true)
        assertFalse(PatternMatcher.matches("", rule))
    }

    @Test
    fun `findMatchingRule handles empty rule list`() {
        val match = PatternMatcher.findMatchingRule("9876500000", emptyList())
        assertNull(match)
    }

    @Test
    fun `matchWildcard exact number match`() {
        assertTrue(PatternMatcher.matchWildcard("9876500000", "9876500000"))
        assertFalse(PatternMatcher.matchWildcard("9876500001", "9876500000"))
    }

    @Test
    fun `matches with SILENCE action rule`() {
        val rule = BlockRule(label = "Silence", pattern = "98765*", action = BlockAction.SILENCE, isEnabled = true)
        assertTrue(PatternMatcher.matches("9876500000", rule))
    }
}
