package com.regexcaller.callblocker.data.repository

import android.content.Context
import com.regexcaller.callblocker.data.db.AppDatabase
import com.regexcaller.callblocker.data.db.BlockRule
import com.regexcaller.callblocker.data.db.BlockRuleDao

/**
 * Repository for managing block rules, wrapping the DAO.
 */
class BlockRuleRepository(private val dao: BlockRuleDao) {

    /**
     * Secondary constructor for production use — gets DAO from singleton database.
     */
    constructor(context: Context) : this(
        AppDatabase.getInstance(context).blockRuleDao()
    )

    val allRules = dao.getAllRules()

    suspend fun insert(rule: BlockRule) = dao.insert(rule)
    suspend fun update(rule: BlockRule) = dao.update(rule)
    suspend fun delete(rule: BlockRule) = dao.delete(rule)
    suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)

    suspend fun getById(id: Long): BlockRule? = dao.getById(id)
}
