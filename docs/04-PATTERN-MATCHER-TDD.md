# 04-PATTERN-MATCHER-TDD

# Phase 4: Pattern Matching Engine (TDD)

## Objective

Build the `PatternMatcher` object that supports wildcard patterns (`*`, `?`) and raw regex patterns. Also includes pattern validation and rule priority logic (ALLOW > BLOCK). Pure Kotlin — all tests run on JVM.

**Prerequisites:** Phase 3 complete. NumberNormalizer tests all GREEN. Phase 2's `BlockRule` entity must also exist (imported by `matches()` and `findMatchingRule()`).

> **Design Decision — Match Semantics:**
> 
> - **Wildcard mode** (`matchWildcard`): Uses `Regex.matches()` — the pattern must match the **entire** number (anchored with `^...$`).
> - **Regex mode** (`matchRegex`): Uses `Regex.containsMatchIn()` — the pattern can match **any substring** of the number.
> 
> This is intentional: wildcard users expect `98765*` to match numbers _starting_ with 98765 (not containing it somewhere). Regex users expect standard regex behavior where `98765` matches anywhere unless they anchor with `^` and `$`.
> 
> **Important for UI:** The AddRuleScreen must clearly explain this difference. In wildcard mode, `98765` matches _only_ exactly `98765`. In regex mode, `98765` matches any number containing those digits.

---

## TDD Cycle Overview

```
RED 4.1  → Test buildWildcardRegex("98765*") == "^98765.*$"      → FAILS
GREEN 4.1 → Implement buildWildcardRegex                          → PASSES

RED 4.2  → Test matchWildcard prefix pattern                      → FAILS
GREEN 4.2 → Implement matchWildcard                               → PASSES

RED 4.3  → Test matchWildcard suffix pattern (*1234)              → FAILS / PASSES
GREEN 4.3 → Verify suffix matching works                          → PASSES

RED 4.4  → Test matchWildcard ? single-digit                      → FAILS / PASSES
GREEN 4.4 → Verify ? matching works                               → PASSES

RED 4.5  → Test matchRegex with raw regex                         → FAILS
GREEN 4.5 → Implement matchRegex                                  → PASSES

RED 4.6  → Test matches() integrates with NumberNormalizer        → FAILS
GREEN 4.6 → Implement matches() using allVariants                 → PASSES

RED 4.7  → Test findMatchingRule with ALLOW precedence            → FAILS
GREEN 4.7 → Implement findMatchingRule with ALLOW-first sorting   → PASSES

RED 4.8  → Test validatePattern returns null for valid            → FAILS
GREEN 4.8 → Implement validatePattern                             → PASSES

RED 4.9  → Test validatePattern returns error for invalid         → PASSES (with 4.8)
GREEN 4.9 → Verify invalid pattern detection                      → PASSES

RED 4.10 → Edge cases (malformed regex, disabled rules, empty)   → FAILS
GREEN 4.10 → Defensive error handling                             → PASSES

REFACTOR → Clean up, full test suite                              → ALL GREEN
```

---

## Step 4.1: Build Wildcard Regex

### RED — Write the test

**File:** `app/src/test/java/com/regexcaller/callblocker/engine/PatternMatcherTest.kt`

```kotlin
package com.regexcaller.callblocker.engine

import org.junit.Assert.*
import org.junit.Test

class PatternMatcherTest {

    // --- 4.1: buildWildcardRegex ---

    @Test
    fun `buildWildcardRegex converts star to dot-star`() {
        assertEquals("^98765.*$", PatternMatcher.buildWildcardRegex("98765*"))
    }

    @Test
    fun `buildWildcardRegex converts question mark to single dot`() {
        assertEquals("^9876.00000$", PatternMatcher.buildWildcardRegex("9876?00000"))
    }

    @Test
    fun `buildWildcardRegex escapes regex special characters`() {
        // Pattern with literal dots and plus signs should be escaped
        assertEquals("^\\+91\\..*$", PatternMatcher.buildWildcardRegex("+91.*"))
    }

    @Test
    fun `buildWildcardRegex handles plain number as exact match`() {
        assertEquals("^9876500000$", PatternMatcher.buildWildcardRegex("9876500000"))
    }
}
```

**Run:** `./gradlew test`  
**Expected:** RED — `PatternMatcher` does not exist.

### GREEN — Create PatternMatcher with buildWildcardRegex

**File:** `app/src/main/java/com/regexcaller/callblocker/engine/PatternMatcher.kt`

```kotlin
package com.regexcaller.callblocker.engine

object PatternMatcher {

    fun buildWildcardRegex(pattern: String): String {
        val sb = StringBuilder("^")
        for (char in pattern) {
            when (char) {
                '*'  -> sb.append(".*")
                '?'  -> sb.append(".")
                '.', '+', '^', '$', '{', '}', '[', ']', '(', ')', '|', '\\' ->
                    sb.append("\\$char")
                else -> sb.append(char)
            }
        }
        sb.append("$")
        return sb.toString()
    }
}
```

**Run:** GREEN — All 4 tests pass.

---

## Step 4.2: Match Wildcard — Prefix Pattern

### RED — Add tests

```kotlin
// --- 4.2: matchWildcard prefix ---

@Test
fun `matchWildcard matches prefix pattern`() {
    assertTrue(PatternMatcher.matchWildcard("9876500000", "98765*"))
    assertTrue(PatternMatcher.matchWildcard("9876512345", "98765*"))
}

@Test
fun `matchWildcard rejects non-matching prefix`() {
    assertFalse(PatternMatcher.matchWildcard("9876400000", "98765*"))
    assertFalse(PatternMatcher.matchWildcard("1234500000", "98765*"))
}
```

**Run:** RED — `matchWildcard` does not exist.

### GREEN — Implement matchWildcard

Add to `PatternMatcher`:

```kotlin
fun matchWildcard(number: String, pattern: String): Boolean {
    val regex = buildWildcardRegex(pattern)
    return try {
        Regex(regex).matches(number)
    } catch (e: Exception) {
        false
    }
}
```

**Run:** GREEN — Tests pass.

---

## Step 4.3: Match Wildcard — Suffix Pattern

### RED — Add tests

```kotlin
// --- 4.3: matchWildcard suffix ---

@Test
fun `matchWildcard matches suffix pattern`() {
    assertTrue(PatternMatcher.matchWildcard("9876500000", "*00000"))
    assertTrue(PatternMatcher.matchWildcard("1111100000", "*00000"))
}

@Test
fun `matchWildcard rejects non-matching suffix`() {
    assertFalse(PatternMatcher.matchWildcard("9876500001", "*00000"))
}
```

**Run:** Should be GREEN already (suffix `*` is handled by `.*` prefix in regex).

---

## Step 4.4: Match Wildcard — Single Digit `?`

### RED — Add tests

```kotlin
// --- 4.4: matchWildcard single-digit ? ---

@Test
fun `matchWildcard question mark matches exactly one digit`() {
    assertTrue(PatternMatcher.matchWildcard("9876500000", "9876?00000"))
    assertTrue(PatternMatcher.matchWildcard("9876100000", "9876?00000"))
    assertTrue(PatternMatcher.matchWildcard("9876900000", "9876?00000"))
}

@Test
fun `matchWildcard question mark does not match zero or multiple digits`() {
    // "9876?00000" has 10 chars total with ? as 1 → expects 10-digit number
    assertFalse(PatternMatcher.matchWildcard("987600000", "9876?00000"))   // 9 digits
    assertFalse(PatternMatcher.matchWildcard("98765500000", "9876?00000")) // 11 digits
}
```

**Run:** Should be GREEN already.

---

## Step 4.5: Match Raw Regex

### RED — Add tests

```kotlin
// --- 4.5: matchRegex ---

@Test
fun `matchRegex matches valid regex pattern`() {
    assertTrue(PatternMatcher.matchRegex("+919876500000", "^\\+9198765.*"))
}

@Test
fun `matchRegex uses containsMatchIn (partial match)`() {
    assertTrue(PatternMatcher.matchRegex("9876500000", "98765"))
}

@Test
fun `matchRegex returns false for non-match`() {
    assertFalse(PatternMatcher.matchRegex("9876500000", "^12345.*"))
}

@Test
fun `matchRegex returns false for invalid regex (no crash)`() {
    assertFalse(PatternMatcher.matchRegex("9876500000", "[invalid"))
}
```

**Run:** RED — `matchRegex` does not exist.

### GREEN — Implement matchRegex

Add to `PatternMatcher`:

```kotlin
fun matchRegex(number: String, pattern: String): Boolean {
    return try {
        Regex(pattern).containsMatchIn(number)
    } catch (e: Exception) {
        false
    }
}
```

**Run:** GREEN — All tests pass.

---

## Step 4.6: Full `matches()` with NumberNormalizer Integration

### RED — Add tests

```kotlin
// --- 4.6: matches() with normalization ---

@Test
fun `matches returns true when normalized number matches wildcard rule`() {
    val rule = BlockRule(label = "Test", pattern = "98765*", isEnabled = true)
    assertTrue(PatternMatcher.matches("+919876500000", rule))  // +91 format
    assertTrue(PatternMatcher.matches("09876500000", rule))    // trunk prefix
    assertTrue(PatternMatcher.matches("9876500000", rule))     // bare 10-digit
}

@Test
fun `matches returns false for disabled rule`() {
    val rule = BlockRule(label = "Disabled", pattern = "98765*", isEnabled = false)
    assertFalse(PatternMatcher.matches("9876500000", rule))
}

@Test
fun `matches works with regex rule across variants`() {
    val rule = BlockRule(label = "Regex", pattern = "^\\+9198765.*", isRegex = true, isEnabled = true)
    // The +91 variant should match
    assertTrue(PatternMatcher.matches("+919876500000", rule))
    assertTrue(PatternMatcher.matches("9876500000", rule))  // allVariants includes +91 prefix
}
```

**Note:** This step requires importing `BlockRule`. Add at top of test file:

```kotlin
import com.regexcaller.callblocker.data.db.BlockRule
```

**Run:** RED — `matches` does not exist.

### GREEN — Implement matches

Add to `PatternMatcher`:

```kotlin
import com.regexcaller.callblocker.data.db.BlockRule

fun matches(incomingRaw: String, rule: BlockRule): Boolean {
    if (!rule.isEnabled) return false
    if (incomingRaw.isBlank()) return false

    val variants = NumberNormalizer.allVariants(incomingRaw)

    return variants.any { number ->
        if (rule.isRegex) {
            matchRegex(number, rule.pattern)
        } else {
            matchWildcard(number, rule.pattern)
        }
    }
}
```

**Run:** GREEN — All tests pass.

---

## Step 4.7: Find Matching Rule with ALLOW Priority

### RED — Add tests

```kotlin
// --- 4.7: findMatchingRule with ALLOW precedence ---

@Test
fun `findMatchingRule returns first matching BLOCK rule`() {
    val rules = listOf(
        BlockRule(id = 1, label = "Spam", pattern = "98765*", action = "BLOCK", isEnabled = true),
        BlockRule(id = 2, label = "Other", pattern = "11111*", action = "BLOCK", isEnabled = true)
    )
    val match = PatternMatcher.findMatchingRule("9876500000", rules)
    assertNotNull(match)
    assertEquals("Spam", match!!.label)
}

@Test
fun `findMatchingRule returns null when no rules match`() {
    val rules = listOf(
        BlockRule(id = 1, label = "Spam", pattern = "98765*", action = "BLOCK", isEnabled = true)
    )
    val match = PatternMatcher.findMatchingRule("1111100000", rules)
    assertNull(match)
}

@Test
fun `findMatchingRule ALLOW takes precedence over BLOCK`() {
    val rules = listOf(
        BlockRule(id = 1, label = "Block All", pattern = "98765*", action = "BLOCK", isEnabled = true),
        BlockRule(id = 2, label = "Allow Bank", pattern = "9876500000", action = "ALLOW", isEnabled = true)
    )
    // Even though BLOCK rule matches, ALLOW should win
    val match = PatternMatcher.findMatchingRule("9876500000", rules)
    assertNotNull(match)
    assertEquals("ALLOW", match!!.action)
    assertEquals("Allow Bank", match.label)
}

@Test
fun `findMatchingRule skips disabled rules`() {
    val rules = listOf(
        BlockRule(id = 1, label = "Disabled", pattern = "98765*", action = "BLOCK", isEnabled = false),
        BlockRule(id = 2, label = "Active", pattern = "98765*", action = "SILENCE", isEnabled = true)
    )
    val match = PatternMatcher.findMatchingRule("9876500000", rules)
    assertNotNull(match)
    assertEquals("SILENCE", match!!.action)
}

@Test
fun `findMatchingRule returns ALLOW even when BLOCK is listed first`() {
    // Testing that ALLOW rules are sorted to the front
    val rules = listOf(
        BlockRule(id = 1, label = "Block", pattern = "98765*", action = "BLOCK", isEnabled = true),
        BlockRule(id = 2, label = "Allow", pattern = "98765*", action = "ALLOW", isEnabled = true)
    )
    val match = PatternMatcher.findMatchingRule("9876500000", rules)
    assertNotNull(match)
    assertEquals("ALLOW", match!!.action)
}
```

**Run:** RED — `findMatchingRule` does not exist.

### GREEN — Implement findMatchingRule

Add to `PatternMatcher`:

```kotlin
fun findMatchingRule(incomingRaw: String, rules: List<BlockRule>): BlockRule? {
    // ALLOW rules get priority: sorted to front so they are checked first
    // Among same-action rules, order is preserved (typically createdAt DESC from DAO)
    val sorted = rules.sortedByDescending { it.action == BlockAction.ALLOW }
    return sorted.firstOrNull { matches(incomingRaw, it) }
}
```

> **Import** `BlockAction`**:** Add `import com.regexcaller.callblocker.data.model.BlockAction` at the top of `PatternMatcher.kt`.

**Run:** GREEN — All tests pass.

---

## Step 4.8: Validate Pattern — Valid Cases

### RED — Add tests

```kotlin
// --- 4.8: validatePattern ---

@Test
fun `validatePattern returns null for valid wildcard pattern`() {
    assertNull(PatternMatcher.validatePattern("98765*", isRegex = false))
    assertNull(PatternMatcher.validatePattern("*1234", isRegex = false))
    assertNull(PatternMatcher.validatePattern("9876?00000", isRegex = false))
}

@Test
fun `validatePattern returns null for valid regex pattern`() {
    assertNull(PatternMatcher.validatePattern("^\\+9198765.*", isRegex = true))
    assertNull(PatternMatcher.validatePattern("[0-9]+", isRegex = true))
}
```

**Run:** RED — `validatePattern` does not exist.

### GREEN — Implement validatePattern

Add to `PatternMatcher`:

```kotlin
fun validatePattern(pattern: String, isRegex: Boolean): String? {
    if (pattern.isBlank()) return "Pattern cannot be empty"
    return try {
        val regex = if (isRegex) pattern else buildWildcardRegex(pattern)
        Regex(regex)
        null
    } catch (e: Exception) {
        "Invalid pattern: ${e.message}"
    }
}
```

**Run:** GREEN — Valid pattern tests pass.

---

## Step 4.9: Validate Pattern — Invalid Cases

### RED — Add tests

```kotlin
@Test
fun `validatePattern returns error for empty pattern`() {
    val error = PatternMatcher.validatePattern("", isRegex = false)
    assertNotNull(error)
    assertTrue(error!!.contains("empty"))
}

@Test
fun `validatePattern returns error for blank pattern`() {
    val error = PatternMatcher.validatePattern("   ", isRegex = false)
    assertNotNull(error)
}

@Test
fun `validatePattern returns error for invalid regex`() {
    val error = PatternMatcher.validatePattern("[unclosed", isRegex = true)
    assertNotNull(error)
    assertTrue(error!!.contains("Invalid pattern"))
}
```

**Run:** Should be GREEN because `validatePattern` already handles these cases.

---

## Step 4.10: Edge Cases

### RED — Add tests

```kotlin
// --- 4.10: Edge cases ---

@Test
fun `matchWildcard returns false for malformed internal pattern`() {
    // Even if somehow a bad pattern gets through validation
    assertFalse(PatternMatcher.matchWildcard("123", ""))
}

@Test
fun `matches handles empty incoming number gracefully`() {
    val rule = BlockRule(label = "Test", pattern = "98765*", isEnabled = true)
    assertFalse(PatternMatcher.matches("", rule))
}

@Test
fun `findMatchingRule handles empty rule list`() {
    val match = PatternMatcher.findMatchingRule("9876500000", emptyList())
    assertNull(match)
}

@Test
fun `matchWildcard exact number match`() {
    assertTrue(PatternMatcher.matchWildcard("9876500000", "9876500000"))
    assertFalse(PatternMatcher.matchWildcard("9876500001", "9876500000"))
}

@Test
fun `matches with SILENCE action rule`() {
    val rule = BlockRule(label = "Silence", pattern = "98765*", action = "SILENCE", isEnabled = true)
    assertTrue(PatternMatcher.matches("9876500000", rule))
}
```

**Run:** GREEN — All edge cases pass.

---

## Step 4.11: REFACTOR

**Actions:**

1.  Add KDoc comments to all public functions in `PatternMatcher`
2.  Ensure `matchRegex` uses `containsMatchIn` (not `matches`) — deliberate design choice
3.  Ensure `matchWildcard` uses `matches` (anchored) — deliberate design choice
4.  Verify error handling never throws — only returns `false` or error strings
5.  Run ALL tests: `./gradlew test`

**Expected:** ALL GREEN — All 25+ tests pass across NumberNormalizer + PatternMatcher.

---

## Phase 4 Completion Checklist

| #   | Check | Status |
| --- | --- | --- |
| 4.1 | buildWildcardRegex — 4 tests pass | \[ \] |
| 4.2 | matchWildcard prefix — 2 tests pass | \[ \] |
| 4.3 | matchWildcard suffix — 2 tests pass | \[ \] |
| 4.4 | matchWildcard ? — 2 tests pass | \[ \] |
| 4.5 | matchRegex — 4 tests pass | \[ \] |
| 4.6 | matches() with normalization — 3 tests pass | \[ \] |
| 4.7 | findMatchingRule with ALLOW priority — 5 tests pass | \[ \] |
| 4.8 | validatePattern valid — 2 tests pass | \[ \] |
| 4.9 | validatePattern invalid — 3 tests pass | \[ \] |
| 4.10 | Edge cases — 5 tests pass | \[ \] |
| 4.11 | Refactor complete, all tests still pass | \[ \] |
| \-  | **Total: 32 unit tests GREEN on JVM (Phase 4 only)** | \[ \] |
| \-  | **Combined Phase 3 + 4: 50 unit tests GREEN on JVM** | \[ \] |

**STOP. Do not proceed to Phase 5 until all checks pass.**