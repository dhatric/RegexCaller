# 07-UI-LAYER

# Phase 7: UI Layer — Compose Screens

## Objective

Build all Jetpack Compose screens: HomeScreen (rule list), AddRuleScreen (create/edit), TestScreen (number tester), and navigation wiring. No TDD for UI — verified through manual testing and Compose previews.

**Prerequisites:** Phase 6 complete. ViewModel and Repository wired correctly.

---

## UI Testing Strategy

Compose UI is not tested via RED/GREEN TDD because:

- UI tests are slow and brittle
- Compose previews provide faster visual feedback
- The LOGIC behind the UI (ViewModel, PatternMatcher) is already fully tested

**Verification method:**

- Compose `@Preview` functions for each screen
- Manual device testing on Samsung S23
- Confirm navigation between all screens works

---

## Step 7.1: Create Theme

**File:** `app/src/main/java/com/regexcaller/callblocker/ui/theme/Theme.kt`

### Implementation Steps

1.  Create a `CallBlockerTheme` composable that wraps `MaterialTheme`
2.  Use Material 3 dynamic color on Android 12+ (Samsung S23 supports it)
3.  Fall back to a custom color scheme on older devices
4.  Support light and dark themes

```kotlin
package com.regexcaller.callblocker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun CallBlockerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Samsung S23 (Android 13+) supports dynamic color
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
```

**Verification:** Theme compiles. Compose preview renders.

---

## Step 7.2: Create Navigation in MainActivity

**File:** `app/src/main/java/com/regexcaller/callblocker/ui/MainActivity.kt`

### Implementation Steps

1.  Set up `NavController` with `rememberNavController()`
2.  Define routes: `"home"`, `"add_rule"`, `"edit_rule/{id}"`, `"test"`, `"onboarding"`
3.  Determine start destination based on `ROLE_CALL_SCREENING` status
4.  Wrap everything in `CallBlockerTheme`

```kotlin
package com.regexcaller.callblocker.ui

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.regexcaller.callblocker.ui.screens.*
import com.regexcaller.callblocker.ui.theme.CallBlockerTheme
import com.regexcaller.callblocker.util.hasCallScreeningRole

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CallBlockerTheme {
                val navController = rememberNavController()

                val startDestination = if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    !hasCallScreeningRole(this@MainActivity)
                ) "onboarding" else "home"

                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
                    composable("home") {
                        HomeScreen(navController)
                    }
                    composable("add_rule") {
                        AddRuleScreen(navController)
                    }
                    composable("edit_rule/{id}") { backStack ->
                        val id = backStack.arguments?.getString("id")?.toLongOrNull()
                        AddRuleScreen(navController, editRuleId = id)
                    }
                    composable("test") {
                        TestScreen(navController)
                    }
                    composable("onboarding") {
                        OnboardingScreen(navController)
                    }
                }
            }
        }
    }
}
```

**Note:** `hasCallScreeningRole` is implemented in Phase 8. For now, create a **stub** in `util/PermissionHelper.kt` so this code compiles:

```kotlin
// Temporary stub — replaced in Phase 8 with real implementation
fun hasCallScreeningRole(context: android.content.Context): Boolean = false
```

This makes the app default to the onboarding screen, which is the correct first-launch behavior. Phase 8 replaces this stub with the real `RoleManager` check.

**Verification:** App launches, shows the start destination screen.

---

## Step 7.3: HomeScreen — Rule List

**File:** `app/src/main/java/com/regexcaller/callblocker/ui/screens/HomeScreen.kt`

### Implementation Steps

1.  **Get ViewModel** via `viewModel()` default factory
2.  **Collect rules** as State via `viewModel.allRules.collectAsState(initial = emptyList())`
3.  **Empty state:** Show message when no rules exist with hint text
4.  **Rule list:** `LazyColumn` with `items(rules, key = { it.id })`
5.  **Each rule item shows:**
    - Label (headline)
    - Pattern (supporting text)
    - Match count badge
    - Enable/disable `Switch`
    - Edit button → navigates to `edit_rule/{id}`
    - Delete button → calls `viewModel.delete(rule)`
6.  **TopAppBar** with:
    - Title: "RegexCaller"
    - Test button → navigates to `"test"`
    - Settings button → navigates to `"onboarding"`
7.  **FAB** → navigates to `"add_rule"`

### Key Compose Components to Use

> **Design Decision — No Blocked-Call Notifications:** The app deliberately does NOT show a notification when a call is blocked. It operates as a completely silent filter. Users can check match counts on the HomeScreen to see how many calls each rule caught. If user feedback in v1.0 shows demand for notifications, this can be added in v1.1 via a `NotificationChannel` and user preference toggle.

| Component | Purpose |
| --- | --- |
| `Scaffold` | TopAppBar + FAB + content |
| `TopAppBar` | Title and action icons |
| `FloatingActionButton` | Add new rule |
| `LazyColumn` | Scrollable rule list |
| `ListItem` | Each rule row (Material 3) |
| `Switch` | Toggle rule enabled/disabled |
| `IconButton` + `Icon` | Edit and Delete actions |
| `HorizontalDivider` | Between list items |

### Implementation Note

Implement the HomeScreen using the Compose components listed above and the implementation steps at the top of this section. The ViewModel's `allRules` Flow provides the reactive data source. Use `collectAsState(initial = emptyList())` to observe it.

**Verification:**

- Empty state renders with "No rules yet" message
- After adding a rule (via AddRuleScreen), the rule appears in the list
- Switch toggles enabled/disabled
- Delete removes the rule
- Edit navigates to edit screen

---

## Step 7.4: AddRuleScreen — Create/Edit Rule

**File:** `app/src/main/java/com/regexcaller/callblocker/ui/screens/AddRuleScreen.kt`

### Implementation Steps

1.  **Accept optional** `editRuleId: Long?` — null for new rule, non-null for edit
2.  **State variables:**
    - `label: String` — user-visible name
    - `pattern: String` — wildcard or regex pattern
    - `isRegex: Boolean` — toggle for raw regex mode
    - `action: String` — one of `BlockAction.BLOCK`, `BlockAction.SILENCE`, or `BlockAction.ALLOW` (use the constants from `data.model.BlockAction`, not raw strings)
    - `patternError: String?` — validation error message
3.  **If editing:** `LaunchedEffect(editRuleId)` to load existing rule
4.  **Live validation:** `LaunchedEffect(pattern, isRegex)` runs `PatternMatcher.validatePattern()`
5.  **Pattern input field:**
    - Shows regex equivalent in supporting text when valid
    - Shows error message in red when invalid
    - Phone keyboard type
6.  **Regex toggle:** `Switch` for isRegex
7.  **Action selector:** Three `FilterChip` components for BLOCK/SILENCE/ALLOW
8.  **Hint card:** Shows pattern examples (98765\*, \*1234, 9876?0000)
9.  **Save button:**
    - Validates pattern before saving
    - Creates or updates `BlockRule`
    - Navigates back on success

### Key UX Details

| Element | Behavior |
| --- | --- |
| Label field | Optional — auto-fills with pattern if blank |
| Pattern field | Required — shows live regex preview below |
| Regex toggle | Off by default — most users use wildcards |
| Action chips | BLOCK selected by default |
| Save button | Disabled when pattern is empty or has validation error |
| Back navigation | Arrow icon in TopAppBar OR system back |

### Implementation Note

Implement the AddRuleScreen using the implementation steps and UX details listed above. Use `PatternMatcher.validatePattern()` for live validation and `PatternMatcher.buildWildcardRegex()` to show the regex equivalent in the supporting text.

**Verification:**

- Can add a new wildcard rule (e.g., "98765\*")
- Pattern validation shows regex equivalent
- Invalid regex shows red error
- BLOCK/SILENCE/ALLOW chips toggle correctly
- Save navigates back and rule appears in HomeScreen
- Edit mode loads existing rule and saves changes

---

## Step 7.5: TestScreen — Number Tester

**File:** `app/src/main/java/com/regexcaller/callblocker/ui/screens/TestScreen.kt`

### Implementation Steps

1.  **Phone number input** with phone keyboard type
2.  **Show normalized form** below the input using `NumberNormalizer.normalize()`
3.  **Run** `PatternMatcher.findMatchingRule()` against all rules (from ViewModel)
4.  **Display result:**
    - No match → Green card: "No rule matches — call would be allowed"
    - BLOCK match → Red card: Shows rule name, pattern, action
    - SILENCE match → Yellow/tertiary card: Shows rule name, pattern, action
    - ALLOW match → Blue/primary card: Shows rule name, pattern, action
5.  **Results update live** as user types

### Key Implementation Detail

```kotlin
val matchResult = remember(testNumber, rules) {
    if (testNumber.isBlank()) null
    else PatternMatcher.findMatchingRule(testNumber, rules)
}
```

This recomputes on every keystroke. Since `findMatchingRule` is O(n) over rules and fast (regex matching), this is fine for hundreds of rules.

### Implementation Note

Implement the TestScreen using the implementation steps above. The `remember(testNumber, rules)` pattern recomputes on every keystroke. Use `NumberNormalizer.normalize()` for the display and `PatternMatcher.findMatchingRule()` for matching.

**Verification:**

- Enter "9876500000" → shows "Normalized: 9876500000"
- Enter "+919876500000" → shows "Normalized: 9876500000"
- If a rule "98765\*" exists → shows BLOCK match
- If no rule matches → shows "call would be allowed"

---

## Step 7.6: Verify All Navigation Works

### Navigation Matrix

| From | To  | Trigger |
| --- | --- | --- |
| Home | AddRule | Tap FAB (+) |
| Home | EditRule | Tap Edit icon on a rule |
| Home | TestScreen | Tap Phone icon in TopAppBar |
| Home | Onboarding | Tap Settings icon in TopAppBar |
| AddRule | Home (pop) | Save button or Back arrow |
| EditRule | Home (pop) | Save button or Back arrow |
| TestScreen | Home (pop) | Back arrow |
| Onboarding | Home (pop) | "Start Adding Rules" button |

### Manual Test Checklist

| Test Case | Expected | Status |
| --- | --- | --- |
| Launch app with no rules | Shows empty state | \[ \] |
| Tap FAB → Add rule → Save → Back | Rule appears in list | \[ \] |
| Tap Edit on rule → Change label → Save | Label updated in list | \[ \] |
| Tap Delete on rule | Rule removed from list | \[ \] |
| Toggle Switch on rule | Rule enabled/disabled | \[ \] |
| Navigate to Test → Enter number | Shows match/no-match result | \[ \] |
| System Back button from each screen | Navigates back correctly | \[ \] |
| Rotate device on each screen | State preserved (Compose ViewModel) | \[ \] |

---

## Phase 7 Completion Checklist

| #   | Check | Status |
| --- | --- | --- |
| 7.1 | Theme created with dynamic color support | \[ \] |
| 7.2 | MainActivity navigation wired with all 5 routes | \[ \] |
| 7.3 | HomeScreen renders empty state and rule list | \[ \] |
| 7.4 | AddRuleScreen creates and edits rules with validation | \[ \] |
| 7.5 | TestScreen shows normalized number and match results | \[ \] |
| 7.6 | All navigation paths work correctly | \[ \] |
| \-  | **No regression:** `./gradlew test` **all GREEN** | \[ \] |
| \-  | **App runs on Samsung S23 with all screens functional** | \[ \] |

**STOP. Do not proceed to Phase 8 until all checks pass.**