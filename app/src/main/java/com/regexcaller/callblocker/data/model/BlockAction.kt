package com.regexcaller.callblocker.data.model

/**
 * Valid actions for a blocking rule.
 *
 * Stored as String in Room entity (BlockRule.action) to avoid TypeConverter complexity.
 * Use these constants everywhere instead of raw strings.
 */
object BlockAction {
    const val BLOCK = "BLOCK"      // Reject the call entirely
    const val SILENCE = "SILENCE"  // Let it ring silently (no audio, call still connects to voicemail)
    const val ALLOW = "ALLOW"      // Explicit allowlist — never block this number

    val ALL = listOf(BLOCK, SILENCE, ALLOW)

    fun isValid(action: String): Boolean = action in ALL
}
