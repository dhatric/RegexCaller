# 02-DATA-LAYER-TDD

# Phase 2: Data Layer — Room Database (TDD)

## Objective

Build the Room database layer (Entity, DAO, Database) using RED/GREEN TDD with Android instrumented tests.

**Prerequisites:** Phase 1 complete. Project compiles and launches.

---

## TDD Cycle Overview for This Phase

```
RED 2.1  → Write BlockRule entity validation test       → FAILS (class doesn't exist)
GREEN 2.1 → Create BlockRule entity                      → PASSES

RED 2.2  → Write DAO insert + query test                → FAILS (DAO doesn't exist)
GREEN 2.2 → Create BlockRuleDao interface                → PASSES

RED 2.3  → Write DAO update test                        → FAILS (method doesn't exist)
GREEN 2.3 → Add update method to DAO                    → PASSES

RED 2.4  → Write DAO delete test                        → FAILS
GREEN 2.4 → Add delete method to DAO                    → PASSES

RED 2.5  → Write DAO getEnabledRules test               → FAILS
GREEN 2.5 → Add getEnabledRules query                   → PASSES

RED 2.6  → Write DAO incrementMatchCount test           → FAILS
GREEN 2.6 → Add incrementMatchCount query               → PASSES

RED 2.7  → Write DAO setEnabled test                    → FAILS
GREEN 2.7 → Add setEnabled query                        → PASSES

REFACTOR → Clean up, add AppDatabase singleton           → ALL PASS
```

---

## Step 2.1: BlockRule Entity

### RED — Write the test first

**File:** `app/src/test/java/com/regexcaller/callblocker/data/BlockRuleTest.kt`

```kotlin
package com.regexcaller.callblocker.data

import com.regexcaller.callblocker.data.db.BlockRule
import org.junit.Assert.*
import org.junit.Test

class BlockRuleTest {

    @Test
    fun `default values are correct`() {
        val rule = BlockRule(
            label = "Test Rule",
            pattern = "98765*"
        )
        assertEquals(0L, rule.id)
        assertEquals("Test Rule", rule.label)
        assertEquals("98765*", rule.pattern)
        assertFalse(rule.isRegex)
        assertEquals("BLOCK", rule.action)
        assertTrue(rule.isEnabled)
        assertEquals(0, rule.matchCount)
        assertTrue(rule.createdAt > 0)
        assertTrue(rule.updatedAt > 0)
        assertTrue(rule.updatedAt >= rule.createdAt)
    }

    @Test
    fun `regex rule can be created`() {
        val rule = BlockRule(
            label = "Regex Rule",
            pattern = "^\\+9198765.*",
            isRegex = true,
            action = "SILENCE"
        )
        assertTrue(rule.isRegex)
        assertEquals("SILENCE", rule.action)
    }

    @Test
    fun `allow action is valid`() {
        val rule = BlockRule(
            label = "Allowlist",
            pattern = "9876500000",
            action = "ALLOW"
        )
        assertEquals("ALLOW", rule.action)
    }
}
```

**Run:** `./gradlew test`  
**Expected:** RED — Compilation fails because `BlockRule` class does not exist.

### GREEN — Create the entity

**File:** `app/src/main/java/com/regexcaller/callblocker/data/db/BlockRule.kt`

```kotlin
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
    val createdAt: Long = System.currentTimeMillis()
)
```

**Run:** `./gradlew test`  
**Expected:** GREEN — All 3 tests pass.

---

## Step 2.2: DAO — Insert & Query All

### RED — Write the instrumented test

**File:** `app/src/androidTest/java/com/regexcaller/callblocker/data/db/BlockRuleDaoTest.kt`

```kotlin
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
}
```

**Run:** `./gradlew connectedAndroidTest`  
**Expected:** RED — Compilation fails because `BlockRuleDao` and `AppDatabase` do not exist.

### GREEN — Create DAO and Database

**File:** `app/src/main/java/com/regexcaller/callblocker/data/db/BlockRuleDao.kt`

```kotlin
package com.regexcaller.callblocker.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockRuleDao {

    @Query("SELECT * FROM block_rules ORDER BY createdAt DESC")
    fun getAllRules(): Flow<List<BlockRule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: BlockRule): Long
}
```

**File:** `app/src/main/java/com/regexcaller/callblocker/data/db/AppDatabase.kt`

```kotlin
package com.regexcaller.callblocker.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(entities = [BlockRule::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun blockRuleDao(): BlockRuleDao
```

> **Schema Export:** `exportSchema = true` writes the Room schema JSON to `app/schemas/`. This is essential for writing migration tests when the schema changes in future versions. Add this to `app/build.gradle.kts`:
> 
> ```kotlin
> ksp {
>     arg("room.schemaLocation", "$projectDir/schemas")
> }
> ```
> 
> Also add `app/schemas/` to version control so migration tests can reference previous schema versions.

```
companion object {
    @Volatile private var INSTANCE: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase =
        INSTANCE ?: synchronized(this) {
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "callblocker.db"
            ).build().also { INSTANCE = it }
        }
}
```

}

````
**Run:** `./gradlew connectedAndroidTest`
**Expected:** GREEN — Both insert tests pass.

---

## Step 2.3: DAO — Update

### RED — Add test to `BlockRuleDaoTest.kt`

```kotlin
@Test
fun updateRule_changesLabel() = runTest {
    val id = dao.insert(BlockRule(label = "Old Label", pattern = "98765*"))
    val inserted = dao.getAllRules().first().first()

    dao.update(inserted.copy(label = "New Label"))

    val updated = dao.getAllRules().first().first()
    assertEquals("New Label", updated.label)
    assertEquals(id, updated.id)
}
````

**Run:** `./gradlew connectedAndroidTest`  
**Expected:** RED — `update` method does not exist on DAO.

### GREEN — Add update to DAO

Add to `BlockRuleDao.kt`:

```kotlin
@Update
suspend fun update(rule: BlockRule)
```

**Run:** `./gradlew connectedAndroidTest`  
**Expected:** GREEN — All tests pass.

---

## Step 2.4: DAO — Delete

### RED — Add test

```kotlin
@Test
fun deleteRule_removesFromDatabase() = runTest {
    val rule = BlockRule(label = "ToDelete", pattern = "111*")
    dao.insert(rule)
    val inserted = dao.getAllRules().first().first()

    dao.delete(inserted)

    val allRules = dao.getAllRules().first()
    assertTrue(allRules.isEmpty())
}
```

**Run:** RED — `delete` does not exist.

### GREEN — Add delete to DAO

```kotlin
@Delete
suspend fun delete(rule: BlockRule)
```

**Run:** GREEN — All tests pass.

---

## Step 2.5: DAO — Get Enabled Rules Only

### RED — Add test

```kotlin
@Test
fun getEnabledRules_excludesDisabledRules() = runTest {
    dao.insert(BlockRule(label = "Active", pattern = "111*", isEnabled = true))
    dao.insert(BlockRule(label = "Disabled", pattern = "222*", isEnabled = false))
    dao.insert(BlockRule(label = "Active2", pattern = "333*", isEnabled = true))

    val enabled = dao.getEnabledRules()
    assertEquals(2, enabled.size)
    assertTrue(enabled.all { it.isEnabled })
}
```

**Run:** RED — `getEnabledRules` does not exist.

### GREEN — Add query to DAO

```kotlin
@Query("SELECT * FROM block_rules WHERE isEnabled = 1")
suspend fun getEnabledRules(): List<BlockRule>
```

**Run:** GREEN — All tests pass.

---

## Step 2.6: DAO — Increment Match Count

### RED — Add test

```kotlin
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
```

**Run:** RED — `incrementMatchCount` does not exist.

### GREEN — Add query to DAO

```kotlin
@Query("UPDATE block_rules SET matchCount = matchCount + 1 WHERE id = :id")
suspend fun incrementMatchCount(id: Long)
```

**Run:** GREEN — All tests pass.

---

## Step 2.7: DAO — Set Enabled/Disabled

### RED — Add test

```kotlin
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
```

**Run:** RED — `setEnabled` does not exist.

### GREEN — Add query to DAO

```kotlin
@Query("UPDATE block_rules SET isEnabled = :enabled WHERE id = :id")
suspend fun setEnabled(id: Long, enabled: Boolean)
```

**Run:** GREEN — All tests pass.

---

## Step 2.8: REFACTOR

**Actions:**

1.  Review `AppDatabase.kt` — ensure singleton pattern is thread-safe (already done with `@Volatile` + `synchronized`)
2.  Review `BlockRuleDao.kt` — ensure all queries are correct
3.  Ensure `AppDatabase` is declared in `CallBlockerApp.kt` for lazy initialization
4.  Run all tests one final time

**Run:** `./gradlew test` AND `./gradlew connectedAndroidTest`  
**Expected:** ALL GREEN — Every test passes.

---

## Final DAO Interface After All Steps

```kotlin
@Dao
interface BlockRuleDao {
    @Query("SELECT * FROM block_rules ORDER BY createdAt DESC")
    fun getAllRules(): Flow<List<BlockRule>>

    @Query("SELECT * FROM block_rules WHERE isEnabled = 1")
    suspend fun getEnabledRules(): List<BlockRule>

    @Query("SELECT * FROM block_rules WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): BlockRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: BlockRule): Long

    @Update
    suspend fun update(rule: BlockRule)

    @Delete
    suspend fun delete(rule: BlockRule)

    @Query("UPDATE block_rules SET matchCount = matchCount + 1 WHERE id = :id")
    suspend fun incrementMatchCount(id: Long)

    @Query("UPDATE block_rules SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)
}
```

> **Added** `getById`**:** Required by Phase 6 (Repository) for the edit-rule flow. Without this, editing disabled rules would fail since `getEnabledRules()` only returns active rules.

---

### Room Migration Strategy (For Future Versions)

The database starts at `version = 1`. When the schema changes in a future release:

1.  **Increment the version** in `@Database(version = 2)`
2.  **Add a Migration object:**
    
    ```kotlin
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE block_rules ADD COLUMN newField TEXT DEFAULT ''")
        }
    }
    ```
    
3.  **Register it:** `.addMigrations(MIGRATION_1_2)` in the database builder
4.  **Never use** `fallbackToDestructiveMigration()` in production — it deletes all user data
5.  **Write a migration test** using the exported schema JSON

---

## Phase 2 Completion Checklist

| #   | Check | Status |
| --- | --- | --- |
| 2.1 | `BlockRuleTest` — 3 unit tests pass (entity defaults) | \[ \] |
| 2.2 | DAO insert + query all — 2 instrumented tests pass | \[ \] |
| 2.3 | DAO update — 1 instrumented test passes | \[ \] |
| 2.4 | DAO delete — 1 instrumented test passes | \[ \] |
| 2.5 | DAO getEnabledRules — 1 instrumented test passes | \[ \] |
| 2.6 | DAO incrementMatchCount — 1 instrumented test passes | \[ \] |
| 2.7 | DAO setEnabled — 1 instrumented test passes | \[ \] |
| 2.8 | Refactor complete, ALL tests still pass | \[ \] |
| \-  | **Total: 3 unit + 8 instrumented tests = 11 tests GREEN** | \[ \] |

**STOP. Do not proceed to Phase 3 until all checks pass.**