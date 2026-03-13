package com.regexcaller.callblocker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.regexcaller.callblocker.data.db.BlockRule
import com.regexcaller.callblocker.data.repository.BlockRuleRepository
import kotlinx.coroutines.launch

/**
 * ViewModel managing the UI state for Block Rules.
 */
class BlockRuleViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = BlockRuleRepository(app)

    val allRules = repository.allRules

    fun insert(rule: BlockRule) = viewModelScope.launch {
        repository.insert(rule)
    }

    fun update(rule: BlockRule) = viewModelScope.launch {
        repository.update(rule)
    }

    fun delete(rule: BlockRule) = viewModelScope.launch {
        repository.delete(rule)
    }

    fun setEnabled(id: Long, enabled: Boolean) = viewModelScope.launch {
        repository.setEnabled(id, enabled)
    }

    suspend fun getRuleById(id: Long): BlockRule? = repository.getById(id)
}
