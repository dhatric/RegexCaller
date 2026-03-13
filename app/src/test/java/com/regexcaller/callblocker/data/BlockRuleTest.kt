package com.regexcaller.callblocker.data

import com.regexcaller.callblocker.data.db.BlockRule
import org.junit.Assert.*
import org.junit.Test

class BlockRuleTest {

    @Test
    fun `default values are correct`() {
        val rule = BlockRule(
            label = "Test Rule",
            pattern = "98765*"
        )
        assertEquals(0L, rule.id)
        assertEquals("Test Rule", rule.label)
        assertEquals("98765*", rule.pattern)
        assertFalse(rule.isRegex)
        assertEquals("BLOCK", rule.action)
        assertTrue(rule.isEnabled)
        assertEquals(0, rule.matchCount)
        assertTrue(rule.createdAt > 0)
        assertTrue(rule.updatedAt > 0)
        assertTrue(rule.updatedAt >= rule.createdAt)
    }

    @Test
    fun `regex rule can be created`() {
        val rule = BlockRule(
            label = "Regex Rule",
            pattern = "^\\+9198765.*",
            isRegex = true,
            action = "SILENCE"
        )
        assertTrue(rule.isRegex)
        assertEquals("SILENCE", rule.action)
    }

    @Test
    fun `allow action is valid`() {
        val rule = BlockRule(
            label = "Allowlist",
            pattern = "9876500000",
            action = "ALLOW"
        )
        assertEquals("ALLOW", rule.action)
    }
}
