package com.regexcaller.callblocker.data.transfer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleTransferCodecTest {

    @Test
    fun encodeDecode_roundTripsValidBackup() {
        val backup = RuleBackupFile(
            version = RuleTransferCodec.VERSION,
            exportedAt = 1234L,
            rules = listOf(
                ExportedRule(
                    label = "Spam",
                    pattern = "98765*",
                    isRegex = false,
                    action = "BLOCK",
                    isEnabled = true
                )
            )
        )

        val encoded = RuleTransferCodec.encode(backup)
        val decoded = RuleTransferCodec.decode(encoded)

        assertEquals(backup, decoded)
    }

    @Test(expected = org.json.JSONException::class)
    fun decode_rejectsMalformedJson() {
        RuleTransferCodec.decode("{bad json")
    }

    @Test
    fun validate_rejectsUnsupportedVersion() {
        val backup = RuleBackupFile(
            version = 99,
            exportedAt = 1234L,
            rules = emptyList()
        )

        try {
            RuleTransferValidator.validate(backup)
            assertFalse("Expected unsupported version error", true)
        } catch (e: RuleTransferException) {
            assertTrue(e.message!!.contains("Unsupported backup version"))
        }
    }

    @Test
    fun validate_rejectsInvalidAction() {
        val backup = RuleBackupFile(
            version = RuleTransferCodec.VERSION,
            exportedAt = 1234L,
            rules = listOf(
                ExportedRule(
                    label = "Spam",
                    pattern = "123*",
                    isRegex = false,
                    action = "DELETE",
                    isEnabled = true
                )
            )
        )

        try {
            RuleTransferValidator.validate(backup)
            assertFalse("Expected invalid action error", true)
        } catch (e: RuleTransferException) {
            assertTrue(e.message!!.contains("invalid action"))
        }
    }

    @Test
    fun validate_rejectsBlankPattern() {
        val backup = RuleBackupFile(
            version = RuleTransferCodec.VERSION,
            exportedAt = 1234L,
            rules = listOf(
                ExportedRule(
                    label = "Spam",
                    pattern = "",
                    isRegex = false,
                    action = "BLOCK",
                    isEnabled = true
                )
            )
        )

        try {
            RuleTransferValidator.validate(backup)
            assertFalse("Expected empty pattern error", true)
        } catch (e: RuleTransferException) {
            assertTrue(e.message!!.contains("empty pattern"))
        }
    }

    @Test
    fun validate_rejectsInvalidRegex() {
        val backup = RuleBackupFile(
            version = RuleTransferCodec.VERSION,
            exportedAt = 1234L,
            rules = listOf(
                ExportedRule(
                    label = "Spam",
                    pattern = "[abc",
                    isRegex = true,
                    action = "BLOCK",
                    isEnabled = true
                )
            )
        )

        try {
            RuleTransferValidator.validate(backup)
            assertFalse("Expected invalid regex error", true)
        } catch (e: RuleTransferException) {
            assertTrue(e.message!!.contains("invalid"))
        }
    }

    @Test
    fun encode_omitsInternalFields() {
        val backup = RuleBackupFile(
            version = RuleTransferCodec.VERSION,
            exportedAt = 1234L,
            rules = listOf(
                ExportedRule(
                    label = "Spam",
                    pattern = "123*",
                    isRegex = false,
                    action = "BLOCK",
                    isEnabled = true
                )
            )
        )

        val encoded = RuleTransferCodec.encode(backup)

        assertFalse(encoded.contains("matchCount"))
        assertFalse(encoded.contains("createdAt"))
        assertFalse(encoded.contains("updatedAt"))
        assertFalse(encoded.contains("\"id\""))
    }
}
