# 08-ONBOARDING-PERMISSIONS

# Phase 8: Onboarding & Permissions

## Objective

Implement the permission flow that grants `ROLE_CALL_SCREENING` to the app. This is the CRITICAL step that enables the `CallBlockerService` to intercept calls. On Samsung S23, this shows a one-time system dialog — it does NOT replace the Samsung Phone app.

**Prerequisites:** Phase 7 complete. All screens functional.

---

## Key Principle: Add-On, NOT Replacement

```
┌─────────────────────────────────────────────────────┐
│  Samsung S23 Call Flow (After Setup)                │
│                                                      │
│  Incoming Call                                       │
│       │                                              │
│       ▼                                              │
│  ┌──────────────────┐                                │
│  │ Android Telecom  │                                │
│  │   Framework      │                                │
│  └────────┬─────────┘                                │
│           │                                          │
│           ▼                                          │
│  ┌──────────────────┐     ┌──────────────────┐      │
│  │ RegexCaller      │     │ Samsung Phone    │      │
│  │ (CallScreening   │     │ (Default Dialer) │      │
│  │  Service)        │     │ - UNCHANGED      │      │
│  └────────┬─────────┘     │ - Still default  │      │
│           │               │ - Still handles  │      │
│    Block? │               │   all calls      │      │
│    ┌──────┴──────┐        └──────────────────┘      │
│    │ Yes  │  No  │                                   │
│    │ Reject│ Pass │──────→ Samsung Phone rings       │
│    │      │ thru │                                   │
│    └──────┘      │                                   │
└──────────────────┘                                   │
```

---

## TDD for Permission Helper

The permission logic is simple branching — we can unit test the helper functions.

```
RED 8.1  → Test PermissionHelper functions exist         → FAILS
GREEN 8.1 → Create PermissionHelper                      → PASSES

RED 8.2  → Test OnboardingScreen role request flow       → Manual verification only
GREEN 8.2 → Implement OnboardingScreen                   → Verified on device
```

---

## Step 8.1: PermissionHelper

### RED — Write test stub

**File:** `app/src/test/java/com/regexcaller/callblocker/util/PermissionHelperTest.kt`

```kotlin
package com.regexcaller.callblocker.util

import org.junit.Assert.*
import org.junit.Test

class PermissionHelperTest {

    @Test
    fun `hasCallScreeningRole function exists and is callable`() {
        // Compilation check — the function signature must exist
        // Actual runtime testing requires Android context (instrumented test)
        assertNotNull(::hasCallScreeningRole)
    }
}
```

**Run:** RED — `hasCallScreeningRole` doesn’t exist yet (or was a stub from Phase 7).

### GREEN — Create PermissionHelper

**File:** `app/src/main/java/com/regexcaller/callblocker/util/PermissionHelper.kt`

```kotlin
package com.regexcaller.callblocker.util

import android.content.Context
import android.app.role.RoleManager
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Check if this app holds the ROLE_CALL_SCREENING role.
 *
 * On Samsung S23 (Android 13+), this role allows call screening WITHOUT
 * replacing Samsung Phone as the default dialer.
 *
 * @return true if role is held, false otherwise or if API < 29
 */
@RequiresApi(Build.VERSION_CODES.Q)
fun hasCallScreeningRole(context: Context): Boolean {
    val roleManager = context.getSystemService(RoleManager::class.java)
    return roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
}
```

> **Removed** `requestCallScreeningRole` **helper.** The `OnboardingScreen` (Step 8.2) uses Compose’s `rememberLauncherForActivityResult` to request the role, which is the modern approach. The old `Activity.startActivityForResult` pattern is deprecated since API 30. Having both creates confusion about which to use — the Compose launcher is the only correct path.

**Run:** GREEN — Test passes.

---

## Step 8.2: OnboardingScreen Implementation

**File:** `app/src/main/java/com/regexcaller/callblocker/ui/screens/OnboardingScreen.kt`

### Implementation Steps

1.  **Get the Activity** from `LocalContext.current as Activity`
2.  **Track role status** with `mutableStateOf(hasCallScreeningRole(context))`
3.  **Create ActivityResult launcher** to re-check role after system dialog returns
4.  **UI States:**
    - **Role NOT granted:** Show explanation + "Grant Permission" button
    - **Role granted:** Show success + "Start Adding Rules" button

### Detailed Implementation

```kotlin
package com.regexcaller.callblocker.ui.screens

import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.regexcaller.callblocker.util.hasCallScreeningRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = context as Activity

    var roleGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                hasCallScreeningRole(context)
            else false
        )
    }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            roleGranted = hasCallScreeningRole(context)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Enable Call Blocking") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (roleGranted)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        if (roleGranted) Icons.Default.CheckCircle
                        else Icons.Default.Warning,
                        contentDescription = null
                    )
                    Text(
                        if (roleGranted)
                            "Call screening is active. Samsung Phone is unchanged."
                        else
                            "Call screening permission not granted yet."
                    )
                }
            }

            if (!roleGranted) {
                Text(
                    "RegexCaller works as a silent background filter. " +
                    "Your Samsung Phone app will NOT be replaced.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    "Tap the button below. Android will show a one-time " +
                    "confirmation dialog.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val roleManager = activity.getSystemService(
                                android.app.role.RoleManager::class.java
                            )
                            val intent = roleManager.createRequestRoleIntent(
                                android.app.role.RoleManager.ROLE_CALL_SCREENING
                            )
                            roleLauncher.launch(intent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Call Screening Permission")
                }

                // Samsung-specific reassurance
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "What happens when you grant this permission:",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("• Samsung Phone stays as your default dialer")
                        Text("• RegexCaller silently checks each call")
                        Text("• Matched calls are blocked before they ring")
                        Text("• Unmatched calls ring normally through Samsung Phone")
                        Text("• You can revoke this anytime in Android Settings")
                    }
                }
            } else {
                Text(
                    "Everything is set up! Your call screening is active.",
                    style = MaterialTheme.typography.bodyLarge
                )

                Button(
                    onClick = { navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }},
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start Adding Rules")
                }
            }
        }
    }
}
```

**Verification:** Manual test on Samsung S23 — system dialog appears and role is granted.

---

## Step 8.3: Auto-Navigate to Onboarding on First Launch

Already handled in Step 7.2 (`MainActivity.kt`). The start destination checks `hasCallScreeningRole()` and routes to `"onboarding"` if not granted.

**Verification:**

1.  Fresh install → app opens to Onboarding screen
2.  Grant permission → "Start Adding Rules" appears
3.  Tap "Start Adding Rules" → navigates to Home
4.  Kill and relaunch app → goes directly to Home (role already granted)

---

## Step 8.4: Verify Role via ADB

After granting the role on Samsung S23:

```bash
# Check if our app holds the call screening role
adb shell cmd role list-holders android.app.role.CALL_SCREENING

# Expected output should include:
# com.regexcaller.callblocker

# Verify Samsung Phone is STILL the default dialer
adb shell cmd role list-holders android.app.role.DIALER

# Expected output should still be:
# com.samsung.android.dialer (or com.samsung.android.incallui)
```

**CRITICAL CHECK:** Samsung Phone MUST still be the default dialer. If our app appears as the dialer, something is wrong with the manifest.

---

## Step 8.5: Handle Permission Denial

If the user denies the role:

1.  The `roleLauncher` callback fires with `roleGranted = false`
2.  The UI stays on the "not granted" state
3.  User can tap the button again to retry
4.  No crash, no infinite loop

If the user navigates away from Onboarding before granting:

1.  The Home screen works normally (rules can still be added)
2.  But no calls will be screened until the role is granted
3.  The Settings icon in HomeScreen's TopAppBar takes them back to Onboarding

---

## Step 8.6: Samsung One UI 6.x System Dialog Behavior

On Samsung S23 with One UI 6.x, the `ROLE_CALL_SCREENING` system dialog:

| Aspect | Behavior |
| --- | --- |
| Dialog appearance | Standard Android role request dialog |
| Dialog text | "Allow RegexCaller to screen your calls?" |
| Buttons | "Allow" / "Deny" |
| After "Allow" | Role granted, dialog dismisses |
| After "Deny" | Dialog dismisses, no role change |
| Subsequent requests | Shows dialog again (user can change their mind) |
| Settings revocation | Settings → Apps → Default apps → Caller ID & spam |
| Samsung Smart Call | Continues to work alongside our service |

---

## Phase 8 Completion Checklist

| #   | Check | Status |
| --- | --- | --- |
| 8.1 | PermissionHelper created with hasCallScreeningRole + request helper | \[ \] |
| 8.2 | OnboardingScreen shows correct state for granted/not-granted | \[ \] |
| 8.3 | Auto-navigation to onboarding on first launch | \[ \] |
| 8.4 | ADB confirms role granted AND Samsung Phone still default dialer | \[ \] |
| 8.5 | Permission denial handled gracefully (retry available) | \[ \] |
| 8.6 | Samsung system dialog appears correctly | \[ \] |
| \-  | `./gradlew test` **— all unit tests GREEN** | \[ \] |
| \-  | **Role granted on Samsung S23 device** | \[ \] |
| \-  | **Samsung Phone app is unaffected (still default dialer)** | \[ \] |

**STOP. Do not proceed to Phase 9 until all checks pass.**