package com.regexcaller.callblocker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "block_rules")
data class BlockRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val pattern: String,
    val isRegex: Boolean = false,
    val action: String = "BLOCK",
    val isEnabled: Boolean = true,
    val matchCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis() // Added to satisfy test requirement
)
