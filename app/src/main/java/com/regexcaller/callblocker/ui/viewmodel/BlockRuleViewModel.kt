package com.regexcaller.callblocker.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.regexcaller.callblocker.data.db.BlockRule
import com.regexcaller.callblocker.data.repository.BlockRuleRepository
import com.regexcaller.callblocker.data.transfer.RuleImportStats
import com.regexcaller.callblocker.data.transfer.RuleTransferManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing the UI state for Block Rules.
 */
class BlockRuleViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = BlockRuleRepository(app)
    private val ruleTransferManager = RuleTransferManager(
        app.applicationContext,
        repository
    )

    private val _transferUiState = MutableStateFlow(RuleTransferUiState())
    val transferUiState: StateFlow<RuleTransferUiState> = _transferUiState.asStateFlow()

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

    fun exportRules(uri: Uri) = runTransfer {
        val exportedCount = ruleTransferManager.exportRules(uri)
        _transferUiState.update {
            it.copy(
                lastImportSummary = null,
                message = "Exported $exportedCount rules."
            )
        }
    }

    fun importRules(uri: Uri) = runTransfer {
        val importStats = ruleTransferManager.importRules(uri)
        _transferUiState.update {
            it.copy(
                lastImportSummary = importStats,
                message = "Import finished."
            )
        }
    }

    fun consumeTransferMessage() {
        _transferUiState.update { it.copy(message = null) }
    }

    fun clearImportSummary() {
        _transferUiState.update { it.copy(lastImportSummary = null) }
    }

    private fun runTransfer(block: suspend () -> Unit) = viewModelScope.launch {
        if (_transferUiState.value.isBusy) return@launch

        _transferUiState.update {
            it.copy(
                isBusy = true,
                message = null
            )
        }

        try {
            block()
        } catch (e: Exception) {
            _transferUiState.update {
                it.copy(
                    message = e.message ?: "Transfer failed.",
                    lastImportSummary = null
                )
            }
        } finally {
            _transferUiState.update { it.copy(isBusy = false) }
        }
    }
}

data class RuleTransferUiState(
    val isBusy: Boolean = false,
    val message: String? = null,
    val lastImportSummary: RuleImportStats? = null
)
