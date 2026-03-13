# 05-CALL-SCREENING-SERVICE

# Phase 5: CallScreeningService

## Objective

Implement `CallBlockerService` — the Android `CallScreeningService` subclass that intercepts incoming calls, runs them through the PatternMatcher, and responds with block/silence/allow.

**Prerequisites:** Phase 4 complete. NumberNormalizer and PatternMatcher fully tested.

---

## Important: No Unit TDD for This Phase

`CallScreeningService` is a system-bound Android component. It cannot be meaningfully unit-tested on JVM because:

- It's instantiated by the Android telecom framework
- `onScreenCall()` receives framework objects (`Call.Details`)
- `respondToCall()` is a framework callback

**Testing strategy:**

- The LOGIC (PatternMatcher, NumberNormalizer) is already fully tested in Phase 3 & 4
- The SERVICE is tested via manual integration testing on a Samsung S23 device
- We verify the service registration via `adb` commands

---

## Step 5.1: Create the Service Class

**File:** `app/src/main/java/com/regexcaller/callblocker/engine/CallBlockerService.kt`

### Implementation Steps

1.  **Create the class** extending `CallScreeningService`
2.  **Create a coroutine scope** with `SupervisorJob` + `Dispatchers.IO`
3.  **Override** `onScreenCall()` — this is the entry point when a call arrives
4.  **Extract the incoming number** from `callDetails.handle?.schemeSpecificPart`
5.  **Query Room** for enabled rules (on IO thread via coroutine)
6.  **Run PatternMatcher.findMatchingRule()** against the enabled rules
7.  **Build a CallResponse** based on the matched rule's action
8.  **Call respondToCall()** with the response
9.  **Override** `onDestroy()` to cancel the coroutine scope

### Full Code to Implement

```kotlin
package com.regexcaller.callblocker.engine

import android.telecom.Call
import android.telecom.CallScreeningService
import com.regexcaller.callblocker.data.db.AppDatabase
import com.regexcaller.callblocker.data.model.BlockAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CallBlockerService : CallScreeningService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onScreenCall(callDetails: Call.Details) {
        val incomingNumber = callDetails.handle?.schemeSpecificPart ?: run {
            respondToCall(callDetails, buildResponse(block = false, silence = false))
            return
        }

        serviceScope.launch {
            try {
                val dao = AppDatabase.getInstance(applicationContext).blockRuleDao()
                val enabledRules = dao.getEnabledRules()
                val matchingRule = PatternMatcher.findMatchingRule(incomingNumber, enabledRules)

                val response = when (matchingRule?.action) {
                    BlockAction.BLOCK -> {
                        dao.incrementMatchCount(matchingRule.id)
                        buildResponse(block = true, silence = true)
                    }
                    BlockAction.SILENCE -> {
                        dao.incrementMatchCount(matchingRule.id)
                        buildResponse(block = false, silence = true)
                    }
                    BlockAction.ALLOW -> {
                        buildResponse(block = false, silence = false)
                    }
                    else -> {
                        buildResponse(block = false, silence = false)
                    }
                }

                respondToCall(callDetails, response)
            } catch (e: Exception) {
                // Safety net INSIDE the coroutine — must be here, not outside launch{}
                // A try-catch outside serviceScope.launch{} would NOT catch async exceptions
                android.util.Log.e("CallBlockerService", "Error screening call", e)
                respondToCall(callDetails, buildResponse(block = false, silence = false))
            }
        }
    }

    private fun buildResponse(block: Boolean, silence: Boolean): CallResponse {
        return CallResponse.Builder()
            .setDisallowCall(block)
            .setRejectCall(block)
            .setSilenceCall(silence)
            .setSkipCallLog(false)          // Always log — user can review
            .setSkipNotification(block)     // Hide notification only for blocked
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
```

**Verification:** Project compiles with no errors.

---

## Step 5.2: Register Service in AndroidManifest.xml

**File:** `app/src/main/AndroidManifest.xml`

### Exact Additions Required

1.  **Add permissions** (inside `<manifest>`, before `<application>`):

```xml
<uses-permission android:name="android.permission.READ_CALL_LOG" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.ANSWER_PHONE_CALLS" />
```

> **No INTERNET permission.** This is intentional — the app never communicates over the network. Do not add `android.permission.INTERNET` for any reason, including analytics or crash reporting.

2.  **Register the Application class** (add `android:name` to `<application>`):

```xml
<application
    android:name=".CallBlockerApp"
    ... >
```

3.  **Register the service** (inside `<application>`, after the `<activity>`):

```xml
<service
    android:name=".engine.CallBlockerService"
    android:exported="true"
    android:permission="android.permission.BIND_SCREENING_SERVICE">
    <intent-filter>
        <action android:name="android.telecom.CallScreeningService" />
    </intent-filter>
</service>
```

> **Note on** `android:exported`**:** Starting from Android 12 (API 31), all components with `<intent-filter>` MUST explicitly declare `android:exported`. The wizard-generated `MainActivity` should already have `android:exported="true"` (since it has the `MAIN`/`LAUNCHER` intent filter). If not, add it manually or the app will fail to install on API 31+ devices.

### Critical Notes for Samsung S23

- `android:exported="true"` is REQUIRED — the system must be able to bind to this service
- `android:permission="android.permission.BIND_SCREENING_SERVICE"` ensures ONLY the system can bind
- The intent-filter action MUST be exactly `android.telecom.CallScreeningService`
- This does NOT make the app the default dialer — it only registers as a call screener

**Verification:** Project compiles. No manifest merge errors.

---

## Step 5.3: Verify Service Registration via ADB

**On a connected Samsung S23 device or emulator:**

```bash
# Build and install the app
./gradlew installDebug

# Verify the service is visible to the framework
adb shell dumpsys telecom | grep -i "call_screening"

# Check if our app is listed as a call screening service
adb shell cmd role list-holders android.app.role.CALL_SCREENING

# Verify the service component is registered
adb shell pm dump com.regexcaller.callblocker | grep -A5 "CallBlockerService"
```

**Expected output:**

- The service should be listed in telecom dump
- After granting the role (Phase 8), the app should appear as a role holder

---

## Step 5.4: Response Behavior Matrix

Document this for QA verification:

| Matched Action | `setDisallowCall` | `setRejectCall` | `setSilenceCall` | `setSkipCallLog` | `setSkipNotification` | User Experience |
| --- | --- | --- | --- | --- | --- | --- |
| **BLOCK** | `true` | `true` | `true` | `false` | `true` | Call rejected. No ring. Still in call log. |
| **SILENCE** | `false` | `false` | `true` | `false` | `false` | Phone doesn't ring but call still arrives. |
| **ALLOW** | `false` | `false` | `false` | `false` | `false` | Call rings normally. |
| **No match** | `false` | `false` | `false` | `false` | `false` | Call rings normally (default pass-through). |

---

## Step 5.5: Error Handling in the Service

The service MUST NEVER crash. A crash in `CallScreeningService` can cause ALL calls to be blocked or all calls to ring through unfiltered.

**Defensive measures already in place:**

1.  `callDetails.handle?.schemeSpecificPart ?: run { ... return }` — handles null/unknown numbers
2.  `PatternMatcher.matchWildcard` and `matchRegex` catch ALL exceptions and return `false`
3.  `SupervisorJob` ensures one failed coroutine doesn't cancel the scope
4.  Room queries are on `Dispatchers.IO` — never block the main thread

**Additional defensive measure to add:**

Wrap the entire `onScreenCall` body in a try-catch as a final safety net:

```kotlin
override fun onScreenCall(callDetails: Call.Details) {
    try {
        // ... existing logic ...
    } catch (e: Exception) {
        // Absolute last resort — allow the call through
        respondToCall(callDetails, buildResponse(block = false, silence = false))
    }
}
```

---

## Step 5.6: Samsung One UI Compatibility Notes

**Samsung S23 with One UI 6.x (Android 14/15) specifics:**

1.  **Samsung's built-in call blocking coexists** with third-party `CallScreeningService` — they don't conflict
2.  **Samsung Phone app remains the default dialer** — our app never touches it
3.  **Samsung Smart Call** (if enabled) runs alongside our screening — both can block independently
4.  **The service survives Samsung's battery optimization** because it's system-bound (but see Phase 9 for hardening)
5.  **Call.Details.handle** on Samsung always returns the number in `tel:` URI scheme — `schemeSpecificPart` works correctly
6.  **Service automatically restarts after reboot** — `CallScreeningService` is system-bound, so Android restores it on boot. No `BOOT_COMPLETED` receiver or `START_STICKY` is needed

### Private / Unknown / Withheld Caller Numbers

When the incoming number is null (private/restricted), `callDetails.handle` or `schemeSpecificPart` is null. The service immediately allows the call through (see the `?: run { ... return }` guard in Step 5.1). **Private numbers are never blocked.**

If you want to support blocking private numbers in the future, add a user-facing toggle and a special sentinel pattern (e.g., `PRIVATE`). Do not match empty strings against rules—this would cause false positives via `allVariants`.

---

## Phase 5 Completion Checklist

| #   | Check | Status |
| --- | --- | --- |
| 5.1 | `CallBlockerService.kt` created and compiles | \[ \] |
| 5.2 | AndroidManifest.xml has permissions + service registered | \[ \] |
| 5.3 | ADB dump shows service registered on device | \[ \] |
| 5.4 | Response behavior matrix documented and understood | \[ \] |
| 5.5 | Try-catch safety net INSIDE coroutine launch in onScreenCall | \[ \] |
| 5.6 | Samsung One UI compatibility notes reviewed | \[ \] |
| \-  | **App installs and launches on Samsung S23 without crash** | \[ \] |
| \-  | **No new test failures from Phase 3 & 4:** `./gradlew test` **GREEN** | \[ \] |

**STOP. Do not proceed to Phase 6 until all checks pass.**