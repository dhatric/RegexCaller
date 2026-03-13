package com.regexcaller.callblocker.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberNormalizerTest {

    @Test
    fun `normalize keeps leading plus and strips non-digits`() {
        assertEquals("+919876500000", NumberNormalizer.normalize("+91 98765-00000"))
    }

    @Test
    fun `normalize keeps country code without plus intact`() {
        assertEquals("919876500000", NumberNormalizer.normalize("919876500000"))
    }

    @Test
    fun `normalize keeps leading zero intact`() {
        assertEquals("09876500000", NumberNormalizer.normalize("09876500000"))
    }

    @Test
    fun `normalize returns 10-digit number stripped of formatting`() {
        assertEquals("9876500000", NumberNormalizer.normalize("(987) 650-0000"))
    }

    @Test
    fun `normalize handles spaces and dashes`() {
        assertEquals("+18005550100", NumberNormalizer.normalize("+1-800-555-0100"))
    }

    @Test
    fun `normalize handles parentheses`() {
        assertEquals("+447911123456", NumberNormalizer.normalize("+44 (7911) 123456"))
    }

    @Test
    fun `normalize handles dots`() {
        assertEquals("9876500000", NumberNormalizer.normalize("987.650.0000"))
    }

    @Test
    fun `allVariants returns both normalized and original strings`() {
        val variants = NumberNormalizer.allVariants("+91 98765-00000")
        assertTrue(variants.contains("+919876500000"))      // normalized
        assertTrue(variants.contains("+91 98765-00000"))    // original
        assertEquals(2, variants.size)
    }

    @Test
    fun `allVariants deduplicates results`() {
        val variants = NumberNormalizer.allVariants("9876500000")
        // "9876500000" appears as both normalized and original -> should be deduped
        assertEquals(variants.size, variants.distinct().size)
        assertEquals(1, variants.size)
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
        assertEquals("+", NumberNormalizer.normalize("+"))
    }

    @Test
    fun `normalize handles very short number`() {
        // 4-digit number -> should pass through unchanged
        assertEquals("1234", NumberNormalizer.normalize("1234"))
    }

    @Test
    fun `normalize handles very long number`() {
        // 15-digit number -> should pass through unchanged
        assertEquals("+123456789012345", NumberNormalizer.normalize("+123456789012345"))
    }

    @Test
    fun `allVariants works with empty string`() {
        val variants = NumberNormalizer.allVariants("")
        assertTrue(variants.isEmpty())
    }
}
