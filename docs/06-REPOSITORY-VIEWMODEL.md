# 06-REPOSITORY-VIEWMODEL

# Phase 6: Repository & ViewModel

## Objective

Wire the data layer to the UI layer through `BlockRuleRepository` and `BlockRuleViewModel` following MVVM pattern. The repository wraps the DAO, and the ViewModel exposes reactive Flows for Compose.

**Prerequisites:** Phase 5 complete. Service registered, all prior tests GREEN.

---

## TDD Approach for This Phase

The Repository is a thin wrapper — unit testing it directly requires mocking Room, which is heavyweight. Instead:

- **Repository:** Verified through existing DAO instrumented tests + new integration tests
- **ViewModel:** Tested via a local unit test with a fake repository

```
RED 6.1  → Test Repository.allRules emits data               → FAILS
GREEN 6.1 → Create BlockRuleRepository                        → PASSES

RED 6.2  → Test ViewModel exposes allRules Flow               → FAILS
GREEN 6.2 → Create BlockRuleViewModel                         → PASSES

RED 6.3  → Test ViewModel.insert adds a rule                  → FAILS
GREEN 6.3 → Implement insert in ViewModel                     → PASSES

RED 6.4  → Test ViewModel.delete removes a rule               → FAILS
GREEN 6.4 → Implement delete                                  → PASSES

RED 6.5  → Test ViewModel.setEnabled toggles rule             → FAILS
GREEN 6.5 → Implement setEnabled                              → PASSES

RED 6.6  → Test ViewModel.getRuleById returns correct rule    → FAILS
GREEN 6.6 → Implement getRuleById                             → PASSES

REFACTOR → Finalize both classes                              → ALL GREEN
```

---

## Step 6.1: BlockRuleRepository

### RED — Write instrumented test

**File:** `app/src/androidTest/java/com/regexcaller/callblocker/data/repository/BlockRuleRepositoryTest.kt`

```kotlin
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
        // Repository needs context but we need to inject our test DB
        // For this test, we'll create a test-friendly repository constructor
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
}
```

**Run:** RED — `BlockRuleRepository` does not exist.

### GREEN — Create BlockRuleRepository

**File:** `app/src/main/java/com/regexcaller/callblocker/data/repository/BlockRuleRepository.kt`

```kotlin
package com.regexcaller.callblocker.data.repository

import android.content.Context
import com.regexcaller.callblocker.data.db.AppDatabase
import com.regexcaller.callblocker.data.db.BlockRule
import com.regexcaller.callblocker.data.db.BlockRuleDao

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
```

> `getById` **uses the DAO’s** `@Query` **directly** — not filtered from `getEnabledRules()`. This ensures disabled rules can also be loaded for editing.

**Run:** GREEN — Test passes.

---

## Step 6.2: BlockRuleViewModel — Expose allRules

### RED — Write a unit test

**File:** `app/src/test/java/com/regexcaller/callblocker/ui/viewmodel/BlockRuleViewModelTest.kt`

Since `AndroidViewModel` needs an `Application`, we'll test the ViewModel logic through a simpler approach — verify the ViewModel class compiles and wires correctly. For JVM tests, we use a fake DAO.

```kotlin
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

    override suspend fun insert(rule: BlockRule): Long {
        val newRule = rule.copy(id = nextId++)
        rules.add(newRule)
        flow.value = rules.toList().sortedByDescending { it.createdAt }
        return newRule.id
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
```

**Run:** RED — Tests compile but `BlockRuleDao` interface doesn't yet have the right signature if `FakeBlockRuleDao` has issues. Fix any compilation errors.

### GREEN — Verify repository works with fake DAO

Tests should pass with the existing `BlockRuleRepository` and `FakeBlockRuleDao`.

**Run:** GREEN — Both tests pass.

---

## Step 6.3–6.5: Repository CRUD Operations

### RED — Add remaining tests (batch)

Add to `BlockRuleViewModelTest.kt`:

```kotlin
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
```

**Run:** GREEN — All pass with existing implementation.

---

## Step 6.6: ViewModel Implementation

### RED — Verify ViewModel class doesn't exist

The actual `BlockRuleViewModel` extends `AndroidViewModel` — needs `Application` parameter. This is tested via instrumented tests or manually.

### GREEN — Create the ViewModel

**File:** `app/src/main/java/com/regexcaller/callblocker/ui/viewmodel/BlockRuleViewModel.kt`

```kotlin
package com.regexcaller.callblocker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.regexcaller.callblocker.data.db.BlockRule
import com.regexcaller.callblocker.data.repository.BlockRuleRepository
import kotlinx.coroutines.launch

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
```

**Verification:** Project compiles. ViewModel can be instantiated by Compose via `viewModel()`.

---

## Step 6.7: REFACTOR

**Actions:**

1.  Ensure `BlockRuleRepository` has both constructors (DAO-direct + Context)
2.  Ensure `FakeBlockRuleDao` is in the test source set, not main
3.  Add KDoc comments to Repository and ViewModel public APIs
4.  Run ALL tests: `./gradlew test` AND `./gradlew connectedAndroidTest`

**Expected:** ALL GREEN.

---

## Phase 6 Completion Checklist

| #   | Check | Status |
| --- | --- | --- |
| 6.1 | Repository created with dual constructors | \[ \] |
| 6.2 | Repository.allRules test passes | \[ \] |
| 6.3 | Repository.delete test passes | \[ \] |
| 6.4 | Repository.setEnabled test passes | \[ \] |
| 6.5 | Repository.update test passes | \[ \] |
| 6.6 | ViewModel created, compiles, exposes correct API | \[ \] |
| 6.7 | Refactor complete, all tests still pass | \[ \] |
| \-  | **FakeBlockRuleDao created for JVM testing** | \[ \] |
| \-  | `./gradlew test` **— all unit tests GREEN** | \[ \] |
| \-  | `./gradlew connectedAndroidTest` **— all instrumented GREEN** | \[ \] |

**STOP. Do not proceed to Phase 7 until all checks pass.**