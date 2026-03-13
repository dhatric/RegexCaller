package com.regexcaller.callblocker.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlockRuleDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: BlockRuleDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.blockRuleDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertRule_andQueryAll_returnsInsertedRule() = runTest {
        val rule = BlockRule(label = "Spam", pattern = "98765*")
        val id = dao.insert(rule)

        assertTrue(id > 0)

        val allRules = dao.getAllRules().first()
        assertEquals(1, allRules.size)
        assertEquals("Spam", allRules[0].label)
        assertEquals("98765*", allRules[0].pattern)
    }

    @Test
    fun insertMultipleRules_returnsAllOrderedByCreatedAtDesc() = runTest {
        dao.insert(BlockRule(label = "First", pattern = "111*", createdAt = 1000))
        dao.insert(BlockRule(label = "Second", pattern = "222*", createdAt = 2000))
        dao.insert(BlockRule(label = "Third", pattern = "333*", createdAt = 3000))

        val allRules = dao.getAllRules().first()
        assertEquals(3, allRules.size)
        assertEquals("Third", allRules[0].label)   // Most recent first
        assertEquals("First", allRules[2].label)    // Oldest last
    }

    @Test
    fun updateRule_changesLabel() = runTest {
        val id = dao.insert(BlockRule(label = "Old Label", pattern = "98765*"))
        val inserted = dao.getAllRules().first().first()

        dao.update(inserted.copy(label = "New Label"))

        val updated = dao.getAllRules().first().first()
        assertEquals("New Label", updated.label)
        assertEquals(id, updated.id)
    }

    @Test
    fun deleteRule_removesFromDatabase() = runTest {
        val rule = BlockRule(label = "ToDelete", pattern = "111*")
        dao.insert(rule)
        val inserted = dao.getAllRules().first().first()

        dao.delete(inserted)

        val allRules = dao.getAllRules().first()
        assertTrue(allRules.isEmpty())
    }

    @Test
    fun getEnabledRules_excludesDisabledRules() = runTest {
        dao.insert(BlockRule(label = "Active", pattern = "111*", isEnabled = true))
        dao.insert(BlockRule(label = "Disabled", pattern = "222*", isEnabled = false))
        dao.insert(BlockRule(label = "Active2", pattern = "333*", isEnabled = true))

        val enabled = dao.getEnabledRules()
        assertEquals(2, enabled.size)
        assertTrue(enabled.all { it.isEnabled })
    }

    @Test
    fun incrementMatchCount_increasesCountByOne() = runTest {
        dao.insert(BlockRule(label = "Counter", pattern = "111*"))
        val rule = dao.getAllRules().first().first()
        assertEquals(0, rule.matchCount)

        dao.incrementMatchCount(rule.id)
        dao.incrementMatchCount(rule.id)
        dao.incrementMatchCount(rule.id)

        val updated = dao.getAllRules().first().first()
        assertEquals(3, updated.matchCount)
    }

    @Test
    fun setEnabled_togglesRuleStatus() = runTest {
        dao.insert(BlockRule(label = "Toggle", pattern = "111*", isEnabled = true))
        val rule = dao.getAllRules().first().first()
        assertTrue(rule.isEnabled)

        dao.setEnabled(rule.id, false)

        val updated = dao.getAllRules().first().first()
        assertFalse(updated.isEnabled)

        dao.setEnabled(rule.id, true)

        val reEnabled = dao.getAllRules().first().first()
        assertTrue(reEnabled.isEnabled)
    }
}
