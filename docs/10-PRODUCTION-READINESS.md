# 10-PRODUCTION-READINESS

# Phase 10: Production & Play Store Readiness

## Objective

Finalize the app for production release — ProGuard, signing, privacy policy, Play Store metadata, and final regression testing.

**Prerequisites:** Phase 9 complete. Samsung S23 integration tests passed.

---

## Step 10.1: ProGuard / R8 Configuration

### Verify `app/proguard-rules.pro`

```proguard
# ===== Room Database Entity =====
# Room uses reflection to map database rows to Kotlin data classes.
# Keep only the entity class and its fields — not the entire db package.
-keep class com.regexcaller.callblocker.data.db.BlockRule { *; }

# ===== DAO Interface =====
# Room generates the DAO implementation at compile time.
# The interface itself doesn't strictly need keeping, but keeping it
# avoids potential issues with R8's aggressive optimization.
-keep interface com.regexcaller.callblocker.data.db.BlockRuleDao { *; }

# ===== Engine Classes =====
# CallBlockerService is bound by the Android system framework via its
# class name in AndroidManifest.xml. The system uses reflection to bind.
-keep class com.regexcaller.callblocker.engine.CallBlockerService { *; }
# PatternMatcher and NumberNormalizer are called from the service.
-keep class com.regexcaller.callblocker.engine.PatternMatcher { *; }
-keep class com.regexcaller.callblocker.engine.NumberNormalizer { *; }

# ===== BlockAction Constants =====
-keep class com.regexcaller.callblocker.data.model.BlockAction { *; }

# ===== Coroutines =====
-dontwarn kotlinx.coroutines.**

# ===== Room =====
-dontwarn androidx.room.**

# ===== Compose =====
# Compose uses code generation — usually handled by the Compose compiler plugin.
# These rules prevent false positives from R8 analysis.
-dontwarn androidx.compose.**
```

> **Targeted keep rules:** Instead of `-keep class ...data.db.** { *; }` which keeps everything in the package (including generated `_Impl` classes that R8 already handles), keep only the specific classes that need reflection or system binding.

### RED — Verify ProGuard works

```bash
# Build release APK with minification enabled
./gradlew assembleRelease

# If build fails with R8 errors, add missing keep rules
# Common issue: Room entity fields renamed → "no such column" crash
```

### GREEN — Release APK builds successfully

Verify:

1.  `./gradlew assembleRelease` completes without errors
2.  APK size is smaller than debug build (R8 removed unused code)
3.  Install release APK on Samsung S23 and verify:
    - App launches
    - Rules can be created
    - Call blocking still works

---

## Step 10.2: App Signing

### Debug Signing (Development)

Already handled by default Android Studio debug keystore. No action needed.

### Release Signing (Production)

**Create a release keystore:**

```bash
keytool -genkey -v -keystore regexcaller-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias regexcaller
```

**Configure in** `app/build.gradle.kts`**:**

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../regexcaller-release.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = "regexcaller"
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

**CRITICAL:** Never commit the keystore password. Use environment variables or a local `signing.properties` file (add to `.gitignore`).

---

## Step 10.3: Privacy Policy

### Why Required

For Google Play submission, publish:

1.  A privacy policy URL
2.  An in-app privacy policy entry point
3.  Data Safety declarations that match the shipped manifest and behavior

### Privacy Policy Content (Template)

Create a privacy policy page (host on GitHub Pages, your website, or Google Sites). Must include:

```
Privacy Policy for RegexCaller

Last updated: [date]

1. DATA COLLECTED
RingBlock screens incoming calls locally using Android's call screening role.
Your blocking rules and match counts are stored locally on your device in the
app's private database.

2. DATA NOT COLLECTED OR SHARED
- We do NOT collect phone numbers
- We do NOT transmit any data off your device
- We do NOT use analytics or tracking
- We do NOT share any data with third parties
- We do NOT store incoming call numbers

3. PERMISSIONS USED
- READ_CONTACTS: Used to support contact-aware call screening behavior
- ROLE_CALL_SCREENING: Used to screen incoming calls against user-defined patterns

4. DATA STORAGE
All data (blocking rules, match counts) is stored locally on your device
in a private SQLite database accessible only to this app. No cloud storage
is used. The database is stored in the app's private data directory, which
is protected by Android's application sandbox.

Note: The database is NOT encrypted at rest. If device-level encryption
is important, users should enable full-disk encryption in Android Settings
(enabled by default on Samsung S23).

5. DATA DELETION
Uninstalling the app deletes all stored data immediately.

6. CONTACT
[Your email address]
```

### Implementation Step

Add a Settings/About entry that opens the published privacy policy URL.

---

## Step 10.4: Play Store Metadata

### App Listing Content

| Field | Value |
| --- | --- |
| **App name** | RegexCaller - Pattern Call Blocker |
| **Short description** | Block spam calls using wildcard and regex patterns. Samsung-friendly add-on. |
| **Category** | Tools |
| **Content rating** | Everyone |
| **Price** | Free |

### Full Description (4000 chars max)

```
RegexCaller blocks unwanted calls using pattern matching — just like email spam filters.

HOW IT WORKS:
• Create rules using simple wildcard patterns (e.g., 98765* blocks all numbers starting with 98765)
• Or use powerful regex patterns for advanced matching
• Test any phone number against your rules before they go live
• Works silently in the background — no interruption to your daily phone use

KEY FEATURES:
✓ Wildcard patterns: 98765* (prefix), *1234 (suffix), 9876?0000 (single digit)
✓ Full regex support for power users
✓ BLOCK, SILENCE, or ALLOW actions per rule
✓ Allowlist to protect important numbers from accidental blocking
✓ Real-time match counter shows how many calls each rule caught
✓ Test screen to verify rules before activating

SAMSUNG FRIENDLY:
• Does NOT replace your Samsung Phone app
• Works alongside Samsung Smart Call
• Your existing dialer, contacts, and call log remain untouched
• One-time permission grant — no complex setup

PRIVACY FOCUSED:
• All data stays on your device
• No internet permission — nothing leaves your phone
• No analytics, no tracking, no ads

PERFECT FOR:
• Blocking telemarketing call campaigns that share a common number prefix
• Silencing VoIP spam from sequential numbers
• Creating allowlists for important contacts
• Pattern-based call management
```

### Screenshots Required (Minimum 2)

1.  **HomeScreen** with 3-4 rules showing (enabled, disabled, different actions)
2.  **AddRuleScreen** with a pattern entered and validation showing
3.  **TestScreen** showing a blocked number result
4.  **OnboardingScreen** showing "Samsung Phone unchanged" message

### App Icon

- 512×512 PNG, no transparency
- Suggest: Phone icon with regex slash pattern overlay
- Must meet Google Play icon guidelines

---

## Step 10.5: Play Store Data Safety Declaration

In Google Play Console → App Content → Data Safety:

### Data Types

| Data Type | Collected? | Shared? | Purpose |
| --- | --- | --- | --- |
| Phone numbers | No  | No  | —   |
| Contacts | No  | No  | Optional on-device screening context |
| App interactions | No  | No  | —   |
| Device info | No  | No  | —   |

### Sensitive Permissions Justification

In Play Console → App Content → Permissions:

```
Permission: READ_CONTACTS
Core feature: Supports contact-aware call screening behavior on device.
No contact data is transmitted off-device.
```

---

## Step 10.6: Final Regression Test Suite

### Run ALL Automated Tests

```bash
# Unit tests (JVM) — NumberNormalizer, PatternMatcher, Repository
./gradlew test

# Instrumented tests (device) — Room DAO
./gradlew connectedAndroidTest
```

**Expected:** ALL GREEN. Zero failures.

### Run Full Samsung S23 Integration Test

Execute the complete test script from Phase 9, Step 9.7.

### Edge Case Manual Tests

| Test Case | Expected Behavior | Status |
| --- | --- | --- |
| 100+ rules in database — call screening still fast | < 100ms per call screening | \[ \] |
| Rule with very long regex pattern (500+ chars) | Validation passes, matching works | \[ \] |
| Multiple simultaneous incoming calls (call waiting) | Each call screened independently | \[ \] |
| Airplane mode toggled — rules persist | Database unaffected | \[ \] |
| Low storage warning — app still functions | Room DB operations still work | \[ \] |
| App updated from previous version | Existing rules preserved | \[ \] |
| Samsung firmware update | Role retained, service still works | \[ \] |

---

## Step 10.7: Release Checklist

### Pre-Release

| #   | Item | Status |
| --- | --- | --- |
| 1   | All unit tests pass (`./gradlew test`) | \[ \] |
| 2   | All instrumented tests pass (`connectedAndroidTest`) | \[ \] |
| 3   | Release APK builds with R8 (`assembleRelease`) | \[ \] |
| 3a  | Play app bundle builds successfully (`bundleRelease`) | \[ \] |
| 4   | Release APK tested on Samsung S23 | \[ \] |
| 5   | Samsung Phone still default dialer after install | \[ \] |
| 6   | Call blocking works on release build | \[ \] |
| 7   | Privacy policy URL live and accessible | \[ \] |
| 8   | App icon (512×512) created | \[ \] |
| 9   | Screenshots (2+) captured | \[ \] |
| 10  | Short description written (≤80 chars) | \[ \] |
| 11  | Full description written (≤4000 chars) | \[ \] |
| 12  | ProGuard rules verified on release build | \[ \] |
| 13  | Keystore created and backed up securely | \[ \] |
| 14  | Version code set to 1, version name to "1.0.0" | \[ \] |

### Play Store Submission

| #   | Item | Status |
| --- | --- | --- |
| 1   | App content declarations filled (Data Safety) | \[ \] |
| 2   | Sensitive permission disclosures match shipped manifest | \[ \] |
| 3   | Content rating questionnaire completed | \[ \] |
| 4   | Target audience set (general) | \[ \] |
| 5   | Privacy policy URL entered in Play Console | \[ \] |
| 6   | App bundle (.aab) uploaded to Production track | \[ \] |
| 7   | Release notes written: "Initial release" | \[ \] |
| 8   | Submitted for review | \[ \] |

### Post-Release Monitoring

| #   | Item | Status |
| --- | --- | --- |
| 1   | Monitor Play Console for review results | \[ \] |
| 2   | Check Play Console’s Android Vitals for crash reports | \[ \] |
| 3   | Monitor user reviews for Samsung-specific issues | \[ \] |
| 4   | Plan v1.1: export/import rules, call log integration | \[ \] |

> **No Firebase Crashlytics.** The app intentionally has no INTERNET permission. Use Play Console’s Android Vitals (crash reports, ANRs) for monitoring instead. If offline crash reporting is needed in the future, evaluate solutions that batch reports locally and require explicit user opt-in to upload.

---

## Phase 10 Completion Checklist

| #   | Check | Status |
| --- | --- | --- |
| 10.1 | ProGuard/R8 config verified, release build works | \[ \] |
| 10.2 | App signing configured with release keystore | \[ \] |
| 10.3 | Privacy policy created and published | \[ \] |
| 10.4 | Play Store metadata prepared (title, description, screenshots) | \[ \] |
| 10.5 | Data Safety declarations ready | \[ \] |
| 10.6 | Full regression test suite passed | \[ \] |
| 10.7 | Release checklist completed | \[ \] |
| \-  | **ALL 50+ unit tests GREEN** | \[ \] |
| \-  | **ALL 12+ instrumented tests GREEN** | \[ \] |
| \-  | **Release APK works on Samsung S23** | \[ \] |
| \-  | **Samsung Phone is still the default dialer** | \[ \] |

---

## PROJECT COMPLETE

Total test count at project completion:

- ~18 unit tests: NumberNormalizer (Phase 3)
- ~32 unit tests: PatternMatcher (Phase 4)
- ~5 unit tests: Repository/ViewModel (Phase 6)
- ~1 unit tests: PermissionHelper (Phase 8)
- ~11 instrumented tests: Room DAO + getById (Phase 2)
- ~1 instrumented test: Repository (Phase 6)
- **Total: ~68 automated tests**

All tests follow RED/GREEN TDD — every test was written before the code it validates.
