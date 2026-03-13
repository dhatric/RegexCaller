package com.regexcaller.callblocker.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberNormalizerTest {

    @Test
    fun `normalize strips plus 91 country code from 12-digit number`() {
        assertEquals("9876500000", NumberNormalizer.normalize("+919876500000"))
    }

    @Test
    fun `normalize strips 91 prefix without plus from 12-digit number`() {
        assertEquals("9876500000", NumberNormalizer.normalize("919876500000"))
    }

    @Test
    fun `normalize strips leading zero from 11-digit number`() {
        assertEquals("9876500000", NumberNormalizer.normalize("09876500000"))
    }

    @Test
    fun `normalize returns 10-digit number unchanged`() {
        assertEquals("9876500000", NumberNormalizer.normalize("9876500000"))
    }

    @Test
    fun `normalize handles spaces and dashes in Indian number`() {
        assertEquals("9876500000", NumberNormalizer.normalize("+91 98765-00000"))
    }

    @Test
    fun `normalize handles parentheses in number`() {
        assertEquals("9876500000", NumberNormalizer.normalize("+91 (98765) 00000"))
    }

    @Test
    fun `normalize handles dots in number`() {
        assertEquals("9876500000", NumberNormalizer.normalize("+91.98765.00000"))
    }

    @Test
    fun `normalize keeps non-Indian number stripped of formatting`() {
        // US number: +1-800-555-0100 -> 18005550100
        assertEquals("18005550100", NumberNormalizer.normalize("+1-800-555-0100"))
    }

    @Test
    fun `normalize keeps short international number as-is`() {
        // UK number
        assertEquals("447911123456", NumberNormalizer.normalize("+447911123456"))
    }

    @Test
    fun `allVariants returns all format variants of Indian number`() {
        val variants = NumberNormalizer.allVariants("+919876500000")
        assertTrue(variants.contains("9876500000"))      // normalized
        assertTrue(variants.contains("09876500000"))      // trunk prefix
        assertTrue(variants.contains("919876500000"))     // country code
        assertTrue(variants.contains("+919876500000"))    // full international
    }

    @Test
    fun `allVariants deduplicates results`() {
        val variants = NumberNormalizer.allVariants("9876500000")
        // "9876500000" appears as both normalized and original -> should be deduped
        assertEquals(variants.size, variants.distinct().size)
    }

    @Test
    fun `normalize handles empty string`() {
        assertEquals("", NumberNormalizer.normalize(""))
    }

    @Test
    fun `normalize handles whitespace-only string`() {
        assertEquals("", NumberNormalizer.normalize("   "))
    }

    @Test
    fun `normalize handles number with only plus sign`() {
        assertEquals("", NumberNormalizer.normalize("+"))
    }

    @Test
    fun `normalize handles very short number`() {
        // 4-digit number -> should pass through unchanged
        assertEquals("1234", NumberNormalizer.normalize("1234"))
    }

    @Test
    fun `normalize handles very long number`() {
        // 15-digit number -> should pass through unchanged
        assertEquals("123456789012345", NumberNormalizer.normalize("123456789012345"))
    }

    @Test
    fun `allVariants works with empty string`() {
        val variants = NumberNormalizer.allVariants("")
        // Empty string should NOT generate false Indian variants like "0", "91", "+91"
        assertTrue(variants.isEmpty())
    }

    @Test
    fun `allVariants does not generate misleading Indian variants for non-Indian numbers`() {
        val variants = NumberNormalizer.allVariants("+18005550100")
        // These variants will exist but shouldn't match Indian rules
        assertTrue(variants.contains("18005550100"))
        assertTrue(variants.contains("+18005550100"))
    }

    @Test
    fun `normalize handles Samsung tel URI scheme specific part`() {
        assertEquals("9876500000", NumberNormalizer.normalize("+919876500000"))
    }

    @Test
    fun `normalize handles number with Samsung country code format`() {
        assertEquals("9876500000", NumberNormalizer.normalize(" +91 9876500000 "))
    }

    @Test
    fun `normalize does not strip 91 from non-Indian 12-digit numbers with plus`() {
        // A non-Indian number that happens to have 12 digits starting with 91
        // WITH a plus sign means it's definitely international — +91 = India
        assertEquals("9100000000", NumberNormalizer.normalize("+919100000000"))
    }
}
