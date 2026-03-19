package com.regexcaller.callblocker.data.repository

import android.content.Context
import com.regexcaller.callblocker.data.db.AppDatabase
import com.regexcaller.callblocker.data.db.BlockRule
import com.regexcaller.callblocker.data.db.BlockRuleDao
import com.regexcaller.callblocker.data.transfer.RuleImportStats
import androidx.room.withTransaction

/**
 * Repository for managing block rules, wrapping the DAO.
 */
class BlockRuleRepository private constructor(
    private val dao: BlockRuleDao,
    private val database: AppDatabase? = null
) {

    constructor(dao: BlockRuleDao) : this(dao, null)

    /**
     * Secondary constructor for production use — gets DAO from singleton database.
     */
    constructor(database: AppDatabase) : this(database.blockRuleDao(), database)

    constructor(context: Context) : this(AppDatabase.getInstance(context))

    val allRules = dao.getAllRules()

    suspend fun insert(rule: BlockRule) = dao.insert(rule)
    suspend fun update(rule: BlockRule) = dao.update(rule)
    suspend fun delete(rule: BlockRule) = dao.delete(rule)
    suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)

    suspend fun getById(id: Long): BlockRule? = dao.getById(id)

    suspend fun getAllRulesSnapshot(): List<BlockRule> = dao.getAllRulesSnapshot()

    suspend fun importRules(rules: List<BlockRule>): RuleImportStats {
        val importBlock: suspend () -> RuleImportStats = {
            val seenKeys = dao.getAllRulesSnapshot()
                .mapTo(mutableSetOf()) { rule -> rule.toTransferKey() }
            val newRules = mutableListOf<BlockRule>()
            var skippedCount = 0

            for (rule in rules) {
                if (seenKeys.add(rule.toTransferKey())) {
                    newRules += rule
                } else {
                    skippedCount++
                }
            }

            if (newRules.isNotEmpty()) {
                dao.insertAll(newRules)
            }

            RuleImportStats(
                importedCount = newRules.size,
                skippedCount = skippedCount
            )
        }

        return if (database != null) {
            database.withTransaction { importBlock() }
        } else {
            importBlock()
        }
    }

    private data class RuleTransferKey(
        val label: String,
        val pattern: String,
        val isRegex: Boolean,
        val action: String,
        val isEnabled: Boolean
    )

    private fun BlockRule.toTransferKey(): RuleTransferKey =
        RuleTransferKey(
            label = label,
            pattern = pattern,
            isRegex = isRegex,
            action = action,
            isEnabled = isEnabled
        )
}
