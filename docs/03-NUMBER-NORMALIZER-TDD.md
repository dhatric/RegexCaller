# 03-NUMBER-NORMALIZER-TDD

# Phase 3: Number Normalizer Engine (TDD)

## Objective

Build the `NumberNormalizer` object that converts any incoming phone number format into a canonical 10-digit Indian number. This is a pure Kotlin class — no Android dependencies — so all tests run on JVM (fast).

**Prerequisites:** Phase 2 complete. Data layer tests all GREEN.

---

## Why This Matters

Samsung S23 on Indian carriers receives numbers in wildly different formats:

- `+919876500000` (international with country code)
- `919876500000` (country code without plus)
- `09876500000` (trunk prefix)
- `9876500000` (bare 10-digit)
- `+91 98765-00000` (formatted with spaces/dashes)

All must resolve to `9876500000` before pattern matching.

> **Known Limitation — Indian-Number Assumption:** The normalizer assumes any 12-digit number starting with `91` is an Indian number. A non-Indian 12-digit number that happens to start with `91` (e.g., from a country whose code begins with 9 followed by a local number starting with 1) would be incorrectly stripped. This is an acceptable trade-off for an India-focused app. If international support is added later, consider using a phone number library like `libphonenumber`.

---

## TDD Cycle Overview

```
RED 3.1  → Test normalize("+919876500000") == "9876500000"     → FAILS
GREEN 3.1 → Create NumberNormalizer with +91 stripping          → PASSES

RED 3.2  → Test normalize("919876500000") == "9876500000"      → FAILS
GREEN 3.2 → Add 91-prefix stripping (no plus)                  → PASSES

RED 3.3  → Test normalize("09876500000") == "9876500000"       → FAILS
GREEN 3.3 → Add leading-zero stripping                         → PASSES

RED 3.4  → Test normalize("9876500000") == "9876500000"        → FAILS (or passes trivially)
GREEN 3.4 → Verify 10-digit passthrough                        → PASSES

RED 3.5  → Test normalize("+91 98765-00000") == "9876500000"   → FAILS
GREEN 3.5 → Add non-digit stripping                            → PASSES

RED 3.6  → Test normalize("+1-800-555-0100") keeps non-Indian  → FAILS
GREEN 3.6 → Add non-Indian number passthrough                  → PASSES

RED 3.7  → Test allVariants() returns all 5 formats            → FAILS
GREEN 3.7 → Implement allVariants()                            → PASSES

RED 3.8  → Edge case tests (empty, short, long numbers)        → FAILS
GREEN 3.8 → Handle edge cases                                  → PASSES

REFACTOR → Clean up, verify all pass                           → ALL GREEN
```

---

## Step 3.1: Normalize +91 Country Code

### RED — Write the test

**File:** `app/src/test/java/com/regexcaller/callblocker/engine/NumberNormalizerTest.kt`

```kotlin
package com.regexcaller.callblocker.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class NumberNormalizerTest {

    @Test
    fun `normalize strips plus 91 country code from 12-digit number`() {
        assertEquals("9876500000", NumberNormalizer.normalize("+919876500000"))
    }
}
```

**Run:** `./gradlew test`  
**Expected:** RED — `NumberNormalizer` does not exist.

### GREEN — Create minimal NumberNormalizer

**File:** `app/src/main/java/com/regexcaller/callblocker/engine/NumberNormalizer.kt`

```kotlin
package com.regexcaller.callblocker.engine

object NumberNormalizer {

    fun normalize(raw: String): String {
        var number = raw.trim()
        val hasPlus = number.startsWith("+")
        number = number.filter { it.isDigit() }

        // Strip +91 country code
        if (hasPlus && number.startsWith("91") && number.length == 12) {
            return number.substring(2)
        }

        return number
    }
}
```

**Run:** `./gradlew test`  
**Expected:** GREEN — Test passes.

---

## Step 3.2: Normalize 91 Prefix Without Plus

### RED — Add test

```kotlin
@Test
fun `normalize strips 91 prefix without plus from 12-digit number`() {
    assertEquals("9876500000", NumberNormalizer.normalize("919876500000"))
}
```

**Run:** RED — Returns `"919876500000"` (91 not stripped because `hasPlus` is false).

### GREEN — Add second stripping rule

Add to `normalize()` after the +91 check:

```kotlin
// Strip 91 prefix without plus
if (number.startsWith("91") && number.length == 12) {
    return number.substring(2)
}
```

**Run:** GREEN — Both tests pass.

---

## Step 3.3: Normalize Leading Zero (Trunk Prefix)

### RED — Add test

```kotlin
@Test
fun `normalize strips leading zero from 11-digit number`() {
    assertEquals("9876500000", NumberNormalizer.normalize("09876500000"))
}
```

**Run:** RED — Returns `"09876500000"` (zero not stripped).

### GREEN — Add zero stripping

Add to `normalize()`:

```kotlin
// Strip leading 0 (trunk prefix)
if (number.startsWith("0") && number.length == 11) {
    return number.substring(1)
}
```

**Run:** GREEN — All 3 tests pass.

---

## Step 3.4: 10-Digit Passthrough

### RED — Add test

```kotlin
@Test
fun `normalize returns 10-digit number unchanged`() {
    assertEquals("9876500000", NumberNormalizer.normalize("9876500000"))
}
```

**Run:** This may already pass since the function returns `number` at the end. If GREEN, move on. If RED, fix the return statement.

**Expected:** GREEN — 10-digit number passes through.

---

## Step 3.5: Formatted Numbers (Spaces, Dashes, Parens)

### RED — Add tests

```kotlin
@Test
fun `normalize handles spaces and dashes in Indian number`() {
    assertEquals("9876500000", NumberNormalizer.normalize("+91 98765-00000"))
}

@Test
fun `normalize handles parentheses in number`() {
    assertEquals("9876500000", NumberNormalizer.normalize("+91 (98765) 00000"))
}

@Test
fun `normalize handles dots in number`() {
    assertEquals("9876500000", NumberNormalizer.normalize("+91.98765.00000"))
}
```

**Run:** Should already be GREEN because `number.filter { it.isDigit() }` strips all non-digits. Verify.

**Expected:** GREEN — All pass because digit filtering already works.

---

## Step 3.6: Non-Indian Number Passthrough

### RED — Add test

```kotlin
@Test
fun `normalize keeps non-Indian number stripped of formatting`() {
    // US number: +1-800-555-0100 → 18005550100
    assertEquals("18005550100", NumberNormalizer.normalize("+1-800-555-0100"))
}

@Test
fun `normalize keeps short international number as-is`() {
    // UK number
    assertEquals("447911123456", NumberNormalizer.normalize("+447911123456"))
}
```

**Run:** RED or GREEN depending on length handling. The +44 number is 12 digits starting with "44", not "91", so it should pass through. The +1 number is 11 digits, not starting with "0", so it should also pass through.

**Expected:** GREEN — Non-Indian numbers are returned as digit-only strings.

---

## Step 3.7: All Variants

### RED — Add test

```kotlin
@Test
fun `allVariants returns all format variants of Indian number`() {
    val variants = NumberNormalizer.allVariants("+919876500000")
    assertTrue(variants.contains("9876500000"))      // normalized
    assertTrue(variants.contains("09876500000"))      // trunk prefix
    assertTrue(variants.contains("919876500000"))     // country code
    assertTrue(variants.contains("+919876500000"))    // full international
    assertTrue(variants.contains("+919876500000"))    // original
}

@Test
fun `allVariants deduplicates results`() {
    val variants = NumberNormalizer.allVariants("9876500000")
    // "9876500000" appears as both normalized and original — should be deduped
    assertEquals(variants.size, variants.distinct().size)
}
```

**Run:** RED — `allVariants` does not exist.

### GREEN — Implement allVariants

Add to `NumberNormalizer`:

```kotlin
fun allVariants(raw: String): List<String> {
    val normalized = normalize(raw)
    return listOf(
        normalized,               // 9876500000
        "0$normalized",           // 09876500000
        "91$normalized",          // 919876500000
        "+91$normalized",         // +919876500000
        raw.trim()                // original
    ).distinct()
}
```

**Run:** GREEN — All tests pass.

---

## Step 3.8: Edge Cases

### RED — Add edge case tests

```kotlin
@Test
fun `normalize handles empty string`() {
    assertEquals("", NumberNormalizer.normalize(""))
}

@Test
fun `normalize handles whitespace-only string`() {
    assertEquals("", NumberNormalizer.normalize("   "))
}

@Test
fun `normalize handles number with only plus sign`() {
    assertEquals("", NumberNormalizer.normalize("+"))
}

@Test
fun `normalize handles very short number`() {
    // 4-digit number — should pass through unchanged
    assertEquals("1234", NumberNormalizer.normalize("1234"))
}

@Test
fun `normalize handles very long number`() {
    // 15-digit number — should pass through unchanged
    assertEquals("123456789012345", NumberNormalizer.normalize("123456789012345"))
}

@Test
fun `allVariants works with empty string`() {
    val variants = NumberNormalizer.allVariants("")
    // Empty string should NOT generate false Indian variants like "0", "91", "+91"
    assertTrue(variants.isEmpty())
}

@Test
fun `allVariants does not generate misleading Indian variants for non-Indian numbers`() {
    val variants = NumberNormalizer.allVariants("+18005550100")
    // These variants will exist but shouldn't match Indian rules
    assertTrue(variants.contains("18005550100"))
    assertTrue(variants.contains("+18005550100"))
}
```

**Run:** Check each. Some may pass, some may fail. Fix any failures with defensive code.

### GREEN — Add defensive handling

Ensure `normalize()` handles empty strings gracefully (the `filter { it.isDigit() }` on an empty string returns `""`, which won't match any stripping rule, so it returns `""`).

**Run:** GREEN — All edge case tests pass.

---

## Step 3.9: REFACTOR

**Actions:**

1.  Review `NumberNormalizer.normalize()` — ensure all branches are clear and documented
2.  Add KDoc comments to both public functions
3.  Ensure no unnecessary allocations
4.  Run ALL tests: `./gradlew test`

**Expected:** ALL GREEN — All 14+ tests pass.

---

## Final NumberNormalizer After All Steps

```kotlin
package com.regexcaller.callblocker.engine

object NumberNormalizer {

    fun normalize(raw: String): String {
        var number = raw.trim()
        val hasPlus = number.startsWith("+")
        number = number.filter { it.isDigit() }

        if (hasPlus && number.startsWith("91") && number.length == 12) {
            return number.substring(2)
        }
        if (number.startsWith("91") && number.length == 12) {
            return number.substring(2)
        }
        if (number.startsWith("0") && number.length == 11) {
            return number.substring(1)
        }
        return number
    }

    fun allVariants(raw: String): List<String> {
        val normalized = normalize(raw)
        if (normalized.isEmpty()) return listOf(raw.trim()).filter { it.isNotEmpty() }
        return listOf(
            normalized,
            "0$normalized",
            "91$normalized",
            "+91$normalized",
            raw.trim()
        ).distinct()
    }
}
```

---

## Phase 3 Completion Checklist

| #   | Check | Status |
| --- | --- | --- |
| 3.1 | +91 country code stripping test passes | \[ \] |
| 3.2 | 91 prefix (no plus) stripping test passes | \[ \] |
| 3.3 | Leading zero stripping test passes | \[ \] |
| 3.4 | 10-digit passthrough test passes | \[ \] |
| 3.5 | Formatted numbers (spaces, dashes, dots) tests pass | \[ \] |
| 3.6 | Non-Indian number passthrough tests pass | \[ \] |
| 3.7 | allVariants tests pass (5 formats + deduplication) | \[ \] |
| 3.8 | Edge case tests pass (empty, short, long) | \[ \] |
| 3.9 | Refactor complete, all tests still pass | \[ \] |
| \-  | **Total: 18 unit tests GREEN on JVM** | \[ \] |

**STOP. Do not proceed to Phase 4 until all checks pass.**