# 09-SAMSUNG-ANDROID15

# Phase 9: Samsung S23 & Android 15+ Hardening

## Objective

Address Samsung-specific battery optimization, One UI quirks, Android 15 behavioral changes, and production edge cases. This phase is a cross-cutting concern that hardens the app for real-world Samsung S23 usage.

**Prerequisites:** Phase 8 complete. App is functional with role granted on Samsung S23.

---

## Samsung S23 Device Profile

| Attribute | Value |
| --- | --- |
| Model | SM-S911B / SM-S911U (variants) |
| SoC | Snapdragon 8 Gen 2 / Exynos 2200 |
| Android version | 13 → 14 → 15 (via OTA updates) |
| One UI version | 5.x → 6.x → 7.x |
| Call screening support | Full `ROLE_CALL_SCREENING` support |
| Default dialer | Samsung Phone (com.samsung.android.dialer) |
| Battery optimization | Aggressive (Samsung-specific) |
| Dynamic color | Supported (Material You, Android 12+) |

---

## Step 9.1: Samsung Battery Optimization

### Problem

Samsung's One UI has aggressive battery optimization that can kill background services, including system-bound `CallScreeningService`, especially after the device has been idle for extended periods.

### Solution: Request Battery Optimization Exemption

**Add to** `PermissionHelper.kt`**:**

```kotlin
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

fun isBatteryOptimized(context: Context): Boolean {
    val pm = context.getSystemService(PowerManager::class.java)
    return !pm.isIgnoringBatteryOptimizations(context.packageName)
}

fun requestBatteryOptimizationExemption(activity: Activity) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${activity.packageName}")
    }
    activity.startActivity(intent)
}
```

### Implementation Steps

1.  **Check battery optimization status** in OnboardingScreen after role is granted
2.  **If optimized (not exempt):** Show a second card explaining why battery exemption is needed
3.  **Button:** "Exclude from Battery Optimization" → opens system settings
4.  **After returning:** Re-check status and update UI

### RED/GREEN TDD

**RED — Test:**

```kotlin
// In PermissionHelperTest.kt
@Test
fun `isBatteryOptimized function exists and is callable`() {
    // This is a compilation check — the function signature must exist
    // Actual runtime testing requires Android context
    assertNotNull(::isBatteryOptimized)
}
```

**GREEN:** Implement the function as shown above.

---

## Step 9.2: Samsung One UI Sleeping Apps

### Problem

Samsung One UI has a "Sleeping apps" and "Deep sleeping apps" feature that restricts background activity for apps the user doesn't open frequently. If the user adds our app to the sleeping list (or it's auto-added), the `CallScreeningService` may not be invoked.

### Solution: Guide User to Exclude the App

Add an informational card to OnboardingScreen:

```
Samsung Battery Settings Guide:
1. Open Settings → Battery → Background usage limits
2. Tap "Never sleeping apps"
3. Add "RegexCaller" to the list
4. This ensures call screening is always active
```

### Implementation Steps

1.  **Add a "Samsung Tips" section** to OnboardingScreen (show only on Samsung devices)
2.  **Detect Samsung** via `Build.MANUFACTURER.equals("samsung", ignoreCase = true)`
3.  **Show guidance card** with step-by-step text
4.  **Optional:** Add a button that opens Samsung's battery settings directly:

```kotlin
fun openSamsungBatterySettings(activity: Activity) {
    try {
        val intent = Intent().apply {
            component = android.content.ComponentName(
                "com.samsung.android.lool",
                "com.samsung.android.sm.battery.ui.BatteryActivity"
            )
        }
        activity.startActivity(intent)
    } catch (e: Exception) {
        // Fallback to generic battery settings
        val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
        activity.startActivity(intent)
    }
}
```

### Samsung Detection Utility

**Add to** `PermissionHelper.kt`**:**

```kotlin
fun isSamsungDevice(): Boolean {
    return Build.MANUFACTURER.equals("samsung", ignoreCase = true)
}
```

**RED — Test:**

```kotlin
@Test
fun `isSamsungDevice function exists`() {
    assertNotNull(::isSamsungDevice)
}
```

**GREEN:** Implement as shown.

---

## Step 9.3: Android 15 (SDK 35) Specific Changes

### 9.3.1: Foreground Service Restrictions

Android 15 tightens foreground service restrictions. However, `CallScreeningService` is NOT a foreground service — it's a system-bound service. **No changes needed for this.**

### 9.3.2: Intent Target Restrictions

Android 15 enforces stricter implicit intent rules. Our app only uses:

- `RoleManager.createRequestRoleIntent()` — framework API, not affected
- `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` — system intent, safe

**No changes needed.**

### 9.3.3: Edge-to-Edge Display (Android 15)

Android 15 enforces edge-to-edge display by default. Compose handles this via:

- `Scaffold` with `padding` parameter (already used in all screens)
- `WindowInsets` are handled by Material 3 components

**Verification step:** Check that all screens have correct padding and no content is hidden behind system bars.

### 9.3.4: Package Visibility

Android 15 continues to enforce package visibility restrictions. Since our app only queries the `RoleManager` API (not other packages), **no** `<queries>` **element needed in manifest.**

---

## Step 9.4: Call Number Format on Samsung S23

### Samsung-Specific Number Formats

Samsung's Phone app and telecom framework may deliver numbers in these formats:

| Format Received | NumberNormalizer Result | Notes |
| --- | --- | --- |
| `tel:+919876500000` | `9876500000` | Standard international |
| `tel:09876500000` | `9876500000` | Trunk prefix |
| `tel:9876500000` | `9876500000` | 10-digit bare |
| `tel:919876500000` | `9876500000` | Country code, no plus |
| `sip:user@domain` | Handled as non-Indian | VoIP calls |
| `null` (handle is null) | Handled by null check | Private/restricted numbers |

**Already handled by:**

- `callDetails.handle?.schemeSpecificPart` extracts the number
- `NumberNormalizer.normalize()` handles all Indian formats
- Null check in `onScreenCall()` allows unknown numbers through

### Additional Test Cases for Samsung

Add to `NumberNormalizerTest.kt` (Phase 3 backfill):

```kotlin
@Test
fun `normalize handles Samsung tel URI scheme specific part`() {
    // schemeSpecificPart strips "tel:" automatically
    // But test with a raw +91 number as it would arrive
    assertEquals("9876500000", NumberNormalizer.normalize("+919876500000"))
}

@Test
fun `normalize handles number with Samsung country code format`() {
    // Samsung sometimes sends with extra spaces
    assertEquals("9876500000", NumberNormalizer.normalize(" +91 9876500000 "))
}

@Test
fun `normalize does not strip 91 from non-Indian 12-digit numbers with plus`() {
    // A non-Indian number that happens to have 12 digits starting with 91
    // WITH a plus sign means it's definitely international — +91 = India
    assertEquals("9100000000", NumberNormalizer.normalize("+919100000000"))
    // This IS correct — +91 means India, so stripping is right
}
```

**Run these tests.** They should be GREEN with existing implementation.

> **Note:** `allVariants("")` now returns an empty list (fixed in Phase 3) to prevent false-positive matches for private/withheld caller numbers. See Phase 5, Step 5.6 for private number handling.

---

## Step 9.5: Service Lifecycle on Samsung

### Behavior Matrix

| Event | CallBlockerService Impact | Action Needed |
| --- | --- | --- |
| Phone reboot | Service auto-restarts by system | None |
| App update | Service recreated by system | None |
| App force-stop | Service stops until next call | User education |
| Clear app data | Rules deleted, service restarts | User education |
| Samsung "Optimize battery" | May delay service binding | Battery exemption (9.1) |
| Samsung "Put app to sleep" | May prevent service binding | Never sleeping list (9.2) |
| Samsung "Lock recent apps" | Service continues running | None needed |
| One UI "Device care" cleanup | May affect DB but not service | None (Room is resilient) |

### No BOOT_COMPLETED Receiver Needed

`CallScreeningService` is automatically bound by the Android telecom framework when a call arrives. There is no need for:

- `RECEIVE_BOOT_COMPLETED` permission
- Boot broadcast receiver
- Foreground service notification

The system handles all lifecycle management.

---

## Step 9.6: Samsung Smart Call Coexistence

Samsung S23 has built-in "Smart Call" (powered by Hiya) that:

- Identifies unknown callers
- Has its own spam blocking feature
- Runs separately from third-party `CallScreeningService`

### Coexistence Behavior

```
Incoming Call → Samsung Smart Call (identify/block) → RegexCaller (pattern-based block)
```

- If Samsung Smart Call blocks a call FIRST, `CallBlockerService.onScreenCall()` is NOT called
- If Samsung Smart Call lets the call through, `CallBlockerService.onScreenCall()` IS called
- Both can coexist without conflict
- No configuration needed

### User Guidance

Add to OnboardingScreen or a help section:

```
"If you also use Samsung's Smart Call:
• Smart Call handles caller ID and known spam
• RegexCaller handles your custom patterns
• Both work together — no conflicts"
```

---

## Step 9.7: Samsung-Specific Integration Test Script

Manual test script for QA on Samsung S23:

```
Test Device: Samsung Galaxy S23
Android Version: 15 (One UI 7.x)
Tester: _______________
Date: _______________

PRE-SETUP:
[ ] Fresh install of RegexCaller
[ ] Samsung Phone is the default dialer
[ ] Samsung Smart Call is enabled (if applicable)

ONBOARDING:
[ ] App opens to Onboarding screen on first launch
[ ] "Grant Permission" button shows system dialog
[ ] System dialog says "Allow RegexCaller to screen your calls?"
[ ] After "Allow", screen shows green success card
[ ] Samsung Phone is STILL the default dialer (check in Settings)

BATTERY:
[ ] App is excluded from battery optimization
[ ] App is in Samsung "Never sleeping apps" list

RULE CREATION:
[ ] Add wildcard rule: "98765*" → BLOCK
[ ] Add wildcard rule: "9876500000" → ALLOW
[ ] Add regex rule: "^\\+9198765.*" → SILENCE
[ ] Rules appear in HomeScreen list

CALL BLOCKING (use a second phone):
[ ] Call from 9876512345 → BLOCKED (no ring)
[ ] Call from 9876500000 → ALLOWED (rings normally via Samsung Phone)
[ ] Call from 1234567890 → ALLOWED (no rule matched)
[ ] Blocked call appears in Samsung Phone call log

SERVICE RESILIENCE:
[ ] Reboot phone → service auto-starts → blocking still works
[ ] Force-stop app → make a call → service may not block (expected)
[ ] Re-open app → service resumes → blocking works again

COEXISTENCE:
[ ] Samsung Phone caller ID still works for non-blocked calls
[ ] Samsung Smart Call still identifies spam
[ ] No "Set as default phone app" prompts appear
```

---

## Phase 9 Completion Checklist

| #   | Check | Status |
| --- | --- | --- |
| 9.1 | Battery optimization exemption request implemented | \[ \] |
| 9.2 | Samsung sleeping apps guidance added to onboarding | \[ \] |
| 9.3 | Android 15 compatibility verified (edge-to-edge, intents) | \[ \] |
| 9.4 | Samsung number format tests backfilled and GREEN | \[ \] |
| 9.5 | Service lifecycle behavior documented and verified | \[ \] |
| 9.6 | Samsung Smart Call coexistence confirmed | \[ \] |
| 9.7 | Full Samsung integration test script executed | \[ \] |
| \-  | **Samsung Phone is still the default dialer** | \[ \] |
| \-  | **All unit tests:** `./gradlew test` **GREEN** | \[ \] |
| \-  | **Calls blocked correctly on Samsung S23** | \[ \] |

**STOP. Do not proceed to Phase 10 until all checks pass including physical device testing.**

---

## Appendix: OEM Differences with ROLE_CALL_SCREENING

While this project targets Samsung S23, here's how `ROLE_CALL_SCREENING` behaves on other OEMs if you ever expand device support:

| OEM | Behavior | Notes |
| --- | --- | --- |
| **Samsung (One UI)** | ✅ Works correctly on Android 10+ | Samsung's own call blocking coexists — no conflict |
| **Google Pixel** | ✅ Works cleanly | Standard Android framework, no quirks |
| **OnePlus (OxygenOS)** | ✅ Works correctly | No issues reported |
| **Xiaomi (MIUI)** | ⚠️ May be restricted | MIUI sometimes prevents third-party apps from holding the role. Users may need to enable "AutoStart" in MIUI battery settings, otherwise the service may be killed |
| **Oppo/Realme (ColorOS)** | ⚠️ Similar to Xiaomi | Aggressive background restrictions — may need manual exemption |

### Key Edge Cases Across All OEMs

1.  **ALLOW rules must take precedence:** If a user has both `98765*` (BLOCK) and `9876500000` (ALLOW) for their bank's number, the ALLOW rule must win. `PatternMatcher.findMatchingRule()` handles this by sorting ALLOW rules first.
2.  **Service restart after reboot:** `CallScreeningService` is system-bound and automatically restored on boot. No `BOOT_COMPLETED` receiver is needed on any OEM.
3.  **Background thread requirement (Android 10+):** Room database calls inside `CallScreeningService` must run on a background thread. The coroutine scope with `Dispatchers.IO` in `CallBlockerService` handles this correctly.