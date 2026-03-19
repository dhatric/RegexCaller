package com.regexcaller.callblocker.ui.viewmodel

import com.regexcaller.callblocker.data.db.BlockRule
import com.regexcaller.callblocker.data.db.BlockRuleDao
import com.regexcaller.callblocker.data.repository.BlockRuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BlockRuleViewModelTest {

    private lateinit var fakeDao: FakeBlockRuleDao
    private lateinit var repository: BlockRuleRepository

    @Before
    fun setup() {
        fakeDao = FakeBlockRuleDao()
        repository = BlockRuleRepository(fakeDao)
    }

    @Test
    fun `allRules emits empty list initially`() = runTest {
        val rules = repository.allRules.first()
        assertTrue(rules.isEmpty())
    }

    @Test
    fun `insert adds rule and allRules updates`() = runTest {
        repository.insert(BlockRule(label = "Test", pattern = "98765*"))
        val rules = repository.allRules.first()
        assertEquals(1, rules.size)
    }

    @Test
    fun `delete removes rule`() = runTest {
        repository.insert(BlockRule(label = "ToDelete", pattern = "111*"))
        val inserted = repository.allRules.first().first()
        repository.delete(inserted)
        val rules = repository.allRules.first()
        assertTrue(rules.isEmpty())
    }

    @Test
    fun `setEnabled toggles rule enabled state`() = runTest {
        repository.insert(BlockRule(label = "Toggle", pattern = "111*"))
        val rule = repository.allRules.first().first()
        repository.setEnabled(rule.id, false)
        val updated = repository.allRules.first().first()
        assertFalse(updated.isEnabled)
    }

    @Test
    fun `update modifies rule label`() = runTest {
        repository.insert(BlockRule(label = "Old", pattern = "111*"))
        val rule = repository.allRules.first().first()
        repository.update(rule.copy(label = "New"))
        val updated = repository.allRules.first().first()
        assertEquals("New", updated.label)
    }

    @Test
    fun `importRules skips exact duplicates and inserts only new rules`() = runTest {
        repository.insert(
            BlockRule(
                label = "Spam",
                pattern = "111*",
                isRegex = false,
                action = "BLOCK",
                isEnabled = true
            )
        )

        val importStats = repository.importRules(
            listOf(
                BlockRule(
                    label = "Spam",
                    pattern = "111*",
                    isRegex = false,
                    action = "BLOCK",
                    isEnabled = true
                ),
                BlockRule(
                    label = "Allowlist",
                    pattern = "999",
                    isRegex = false,
                    action = "ALLOW",
                    isEnabled = true
                )
            )
        )

        val rules = repository.allRules.first()
        assertEquals(2, rules.size)
        assertEquals(1, importStats.importedCount)
        assertEquals(1, importStats.skippedCount)
    }
}

/**
 * Fake DAO for JVM unit testing — no Room or Android dependency.
 */
class FakeBlockRuleDao : BlockRuleDao {
    private val rules = mutableListOf<BlockRule>()
    private val flow = MutableStateFlow<List<BlockRule>>(emptyList())
    private var nextId = 1L

    override fun getAllRules(): Flow<List<BlockRule>> = flow

    override suspend fun getEnabledRules(): List<BlockRule> =
        rules.filter { it.isEnabled }

    override suspend fun getAllRulesSnapshot(): List<BlockRule> =
        rules.toList().sortedByDescending { it.createdAt }

    override suspend fun insert(rule: BlockRule): Long {
        val newRule = rule.copy(id = nextId++)
        rules.add(newRule)
        flow.value = rules.toList().sortedByDescending { it.createdAt }
        return newRule.id
    }

    override suspend fun insertAll(rules: List<BlockRule>) {
        rules.forEach { rule ->
            val newRule = rule.copy(id = nextId++)
            this.rules.add(newRule)
        }
        flow.value = this.rules.toList().sortedByDescending { it.createdAt }
    }

    override suspend fun getById(id: Long): BlockRule? = rules.firstOrNull { it.id == id }

    override suspend fun update(rule: BlockRule) {
        val index = rules.indexOfFirst { it.id == rule.id }
        if (index >= 0) {
            rules[index] = rule
            flow.value = rules.toList().sortedByDescending { it.createdAt }
        }
    }

    override suspend fun delete(rule: BlockRule) {
        rules.removeAll { it.id == rule.id }
        flow.value = rules.toList().sortedByDescending { it.createdAt }
    }

    override suspend fun incrementMatchCount(id: Long) {
        val index = rules.indexOfFirst { it.id == id }
        if (index >= 0) {
            rules[index] = rules[index].copy(matchCount = rules[index].matchCount + 1)
            flow.value = rules.toList()
        }
    }

    override suspend fun setEnabled(id: Long, enabled: Boolean) {
        val index = rules.indexOfFirst { it.id == id }
        if (index >= 0) {
            rules[index] = rules[index].copy(isEnabled = enabled)
            flow.value = rules.toList()
        }
    }
}
