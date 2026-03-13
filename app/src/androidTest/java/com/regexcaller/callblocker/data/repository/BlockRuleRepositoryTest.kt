package com.regexcaller.callblocker.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.regexcaller.callblocker.data.db.AppDatabase
import com.regexcaller.callblocker.data.db.BlockRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlockRuleRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: BlockRuleRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = BlockRuleRepository(database.blockRuleDao())
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun allRules_emitsInsertedRules() = runTest {
        repository.insert(BlockRule(label = "Test", pattern = "98765*"))
        val rules = repository.allRules.first()
        assertEquals(1, rules.size)
        assertEquals("Test", rules[0].label)
    }

    @Test
    fun delete_removesRule() = runTest {
        repository.insert(BlockRule(label = "ToDelete", pattern = "111*"))
        val inserted = repository.allRules.first().first()
        repository.delete(inserted)
        val rules = repository.allRules.first()
        assertTrue(rules.isEmpty())
    }

    @Test
    fun setEnabled_togglesRuleEnabledState() = runTest {
        repository.insert(BlockRule(label = "Toggle", pattern = "111*"))
        val rule = repository.allRules.first().first()
        repository.setEnabled(rule.id, false)
        val updated = repository.allRules.first().first()
        assertFalse(updated.isEnabled)
    }

    @Test
    fun update_modifiesRuleLabel() = runTest {
        repository.insert(BlockRule(label = "Old", pattern = "111*"))
        val rule = repository.allRules.first().first()
        repository.update(rule.copy(label = "New"))
        val updated = repository.allRules.first().first()
        assertEquals("New", updated.label)
    }
}
