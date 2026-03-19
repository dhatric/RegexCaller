# 01-PROJECT-SETUP

# Phase 1: Project Setup & Configuration

## Objective

Create a new Android project using the **standard Android Studio New Project wizard**, then add app-specific dependencies, configuration, and folder structure. No TDD in this phase — this is pure scaffolding.

---

## Prerequisites

Complete **all steps** in [00-PREREQUISITES.md](00-PREREQUISITES.md) first. Summary:

- **macOS with Apple Silicon** (M1/M2/M3/M4) — or Intel Mac
- **Homebrew** installed
- **JDK 17** installed (`java -version` → 17.x.x)
- **Android Studio** Ladybug (2024.2.1) or later — Apple Silicon build
- **Android SDK 35** + **SDK 29** installed via SDK Manager
- **Android Emulator** with an **arm64-v8a** AVD created and booting
- `adb devices` shows at least one device (emulator or physical)

> **Do not start Phase 1 until the** [**Prerequisites Checklist**](00-PREREQUISITES.md#prerequisites-checklist) **passes all 10 checks.**

---

## Step 1.1: Create the Android Project (Standard Wizard)

**Action:** Use the Android Studio **New Project** wizard — this is the standard way to scaffold an Android native app.

### Wizard Steps

1.  Open Android Studio → **File → New → New Project**
2.  Select template: **Empty Activity** (under "Phone and Tablet" — this generates Jetpack Compose by default in modern Android Studio)
3.  Configure the project:

| Setting | Value |
| --- | --- |
| Name | RegexCaller |
| Package name | `com.regexcaller.callblocker` |
| Save location | Your preferred project directory |
| Language | Kotlin |
| Minimum SDK | API 29 (Android 10) |
| Build configuration | Kotlin DSL (`build.gradle.kts`) |

4.  Click **Finish** and wait for the initial Gradle sync to complete.

### What the Wizard Auto-Generates

The wizard creates a complete, buildable project out of the box:

| Generated File/Folder | Contents |
| --- | --- |
| `build.gradle.kts` (project root) | Plugin declarations (Android, Kotlin, Compose) |
| `app/build.gradle.kts` | App config, default dependencies, Compose setup |
| `settings.gradle.kts` | Project name, repository configuration |
| `gradle/libs.versions.toml` | Version catalog (modern Gradle dependency management) |
| `app/src/main/AndroidManifest.xml` | Basic manifest with `MainActivity` |
| `app/src/main/java/.../MainActivity.kt` | Compose "Hello Android" activity |
| `app/src/main/java/.../ui/theme/` | `Color.kt`, `Theme.kt`, `Type.kt` |
| `app/src/main/res/` | Default resources (strings, themes, icons) |
| `app/proguard-rules.pro` | Empty ProGuard rules file |
| `gradle/wrapper/` | Gradle wrapper (no manual install needed) |

**Why Min SDK 29:** `ROLE_CALL_SCREENING` was introduced in Android 10 (API 29). This avoids needing default dialer replacement. Samsung S23 ships with Android 13+ so this is well within range.

**Verification:** Click **Run ▶** — the default "Hello Android" Compose activity launches on your emulator or Samsung S23.

---

## Step 1.2: Add KSP Plugin to Root `build.gradle.kts`

**Action:** The wizard already generates the root `build.gradle.kts` with Android, Kotlin, and Compose plugins. You only need to **add the KSP plugin** (required for Room database annotation processing).

Open the wizard-generated `build.gradle.kts` (project root) and add the KSP line:

```kotlin
// build.gradle.kts (project root)
// The first three plugins are already wizard-generated — just add KSP
plugins {
    id("com.android.application") version "8.7.0" apply false       // ← wizard-generated
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false  // ← wizard-generated
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false // ← wizard-generated
    id("com.google.devtools.ksp") version "2.0.20-1.0.25" apply false     // ← ADD THIS
}
```

> **Note:** If your wizard used a version catalog (`gradle/libs.versions.toml`), add KSP there instead. The versions above should match or be compatible with whatever the wizard generated.

**Verification:** Gradle sync succeeds with no errors.

---

## Step 1.3: Extend `app/build.gradle.kts` with App-Specific Dependencies

**Action:** The wizard already generates a working `app/build.gradle.kts` with Compose, core Android, and test dependencies. **Do NOT replace it entirely** — instead, add the missing plugins and dependencies that the app needs beyond the defaults.

### 1\. Add the KSP plugin (for Room)

In the `plugins` block at the top, add the KSP line:

```kotlin
plugins {
    id("com.android.application")               // ← wizard-generated ✓
    id("org.jetbrains.kotlin.android")           // ← wizard-generated ✓
    id("org.jetbrains.kotlin.plugin.compose")    // ← wizard-generated ✓
    id("com.google.devtools.ksp")                // ← ADD THIS
}
```

### 2\. Verify the `android` block

The wizard generates most of this. Just verify/adjust these values:

```kotlin
android {
    namespace = "com.regexcaller.callblocker"    // ← wizard-generated ✓
    compileSdk = 35                               // ← verify: must be 35

    defaultConfig {
        applicationId = "com.regexcaller.callblocker"  // ← wizard-generated ✓
        minSdk = 29                                     // ← verify: must be 29
        targetSdk = 35                                  // ← verify: must be 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true               // ← CHANGE: wizard defaults to false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17  // ← wizard-generated ✓
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"                              // ← wizard-generated ✓
    }

    buildFeatures {
        compose = true                                // ← wizard-generated ✓
    }
}
```

### 3\. Add dependencies beyond wizard defaults

The wizard already includes Compose, Material3, Activity Compose, Lifecycle, and basic test deps. **Add these extra dependencies** to the existing `dependencies` block:

```kotlin
dependencies {
    // ===== WIZARD-GENERATED (already present — do not duplicate) =====
    // Compose BOM, ui, material3, activity-compose, lifecycle, tooling, etc.
    // junit, espresso, compose-ui-test, etc.

    // ===== ADD THESE =====

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Room (local database)
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Coroutines (if not already present)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // DataStore (for onboarding preferences)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Google Truth (better test assertions)
    testImplementation("com.google.truth:truth:1.4.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")

    // Instrumented test additions
    androidTestImplementation("androidx.room:room-testing:$roomVersion")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
}
```

> **Version catalog note:** Modern Android Studio generates a `gradle/libs.versions.toml` file (Gradle version catalog) for dependency management. If your project has one, add the above dependencies using the catalog format instead of hardcoded strings. Here's how:
> 
> **In** `gradle/libs.versions.toml` — add to `[versions]` and `[libraries]`:
> 
> ```toml
> [versions]
> room = "2.6.1"
> navigationCompose = "2.7.7"
> datastore = "1.1.1"
> coroutines = "1.8.0"
> truth = "1.4.2"
> ksp = "2.0.20-1.0.25"
> 
> [libraries]
> room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
> room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
> room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
> room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
> navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
> datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
> kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
> kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
> truth = { group = "com.google.truth", name = "truth", version.ref = "truth" }
> 
> [plugins]
> ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
> ```
> 
> **In** `app/build.gradle.kts` — use catalog references:
> 
> ```kotlin
> plugins {
>     alias(libs.plugins.ksp)
> }
> dependencies {
>     implementation(libs.room.runtime)
>     implementation(libs.room.ktx)
>     ksp(libs.room.compiler)
>     implementation(libs.navigation.compose)
>     implementation(libs.datastore.preferences)
>     implementation(libs.kotlinx.coroutines.android)
>     testImplementation(libs.truth)
>     testImplementation(libs.kotlinx.coroutines.test)
>     androidTestImplementation(libs.room.testing)
>     androidTestImplementation(libs.kotlinx.coroutines.test)
> }
> ```
> 
> **Either format works.** Use whichever matches what the wizard generated. Do not mix both — pick one and be consistent.

**Verification:** Gradle sync succeeds. `./gradlew assembleDebug` completes without errors.

---

## Step 1.4: Create App-Specific Directory Structure

**Action:** The wizard already creates the base package (`com.regexcaller.callblocker/`) with `MainActivity.kt` and `ui/theme/`. You need to add the **app-specific sub-packages** for the features we'll build in later phases.

Create these additional directories:

```
app/src/main/java/com/regexcaller/callblocker/
├── MainActivity.kt              ← wizard-generated ✓
├── ui/theme/                    ← wizard-generated ✓
│
├── data/                        ← ADD: data layer
│   ├── db/                      ←   Room database classes
│   ├── model/                   ←   Enums and data models
│   └── repository/              ←   Repository pattern
├── engine/                      ← ADD: business logic
├── ui/                          
│   ├── screens/                 ← ADD: Compose screens
│   └── viewmodel/               ← ADD: ViewModels 
└── util/                        ← ADD: utilities

app/src/test/java/com/regexcaller/callblocker/
├── engine/                      ← ADD: unit tests for engine
└── data/                        ← ADD: unit tests for data

app/src/androidTest/java/com/regexcaller/callblocker/
└── data/
    └── db/                      ← ADD: instrumented Room tests
```

**Verification:** Folders exist in the project tree. Wizard-generated files (`MainActivity.kt`, `Theme.kt`, `Color.kt`, `Type.kt`) remain untouched.

---

## Step 1.5: Create Application Class Stub

**Action:** Create a minimal `CallBlockerApp.kt` Application class.

**File:** `app/src/main/java/com/regexcaller/callblocker/CallBlockerApp.kt`

```kotlin
package com.regexcaller.callblocker

import android.app.Application

class CallBlockerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Database and DI initialization will be added in Phase 2
    }
}
```

### Register in AndroidManifest.xml

Open `app/src/main/AndroidManifest.xml` (wizard-generated) and add `android:name` to the `<application>` tag:

```xml
<application
    android:name=".CallBlockerApp"
    android:allowBackup="false"
    android:dataExtractionRules="@xml/data_extraction_rules"
    android:fullBackupContent="@xml/backup_rules"
    android:icon="@mipmap/ic_launcher"
    ...
>
```

> Without this, Android will use the default `Application` class and `CallBlockerApp.onCreate()` will never run. This must be done NOW — Phase 2 relies on it for database initialization. The backup attributes are part of the current privacy posture and should be included from the start.

**Verification:** App compiles and launches (no crash on startup).

---

## Step 1.6: Create `BlockAction` Constants

**Action:** Create action constants and validation used throughout the app. The Room entity stores actions as `String` for simplicity (avoids TypeConverter boilerplate). These constants are the **single source of truth** for valid action values.

**File:** `app/src/main/java/com/regexcaller/callblocker/data/model/BlockAction.kt`

```kotlin
package com.regexcaller.callblocker.data.model

/**
 * Valid actions for a blocking rule.
 *
 * Stored as String in Room entity (BlockRule.action) to avoid TypeConverter complexity.
 * Use these constants everywhere instead of raw strings.
 */
object BlockAction {
    const val BLOCK = "BLOCK"      // Reject the call entirely
    const val SILENCE = "SILENCE"  // Let it ring silently (no audio, call still connects to voicemail)
    const val ALLOW = "ALLOW"      // Explicit allowlist — never block this number

    val ALL = listOf(BLOCK, SILENCE, ALLOW)

    fun isValid(action: String): Boolean = action in ALL
}
```

> **Design Note:** Using `object` with `const val` instead of `enum class` avoids needing a Room `TypeConverter`. The `BlockRule.action` field is a plain `String` that must always be one of these three values. The `isValid()` function should be called when saving rules from the UI.

**Verification:** App compiles.

---

## Step 1.7: Create Strings & Theme Resources

**Action:** Update `res/values/strings.xml`:

```xml
<resources>
    <string name="app_name">RingBlock</string>
    <string name="no_rules_title">No rules yet</string>
    <string name="no_rules_subtitle">Tap + to add a pattern like 98765* to block all matching numbers</string>
    <string name="add_rule">Add Rule</string>
    <string name="edit_rule">Edit Rule</string>
    <string name="save_changes">Save Changes</string>
    <string name="test_number">Test a Number</string>
    <string name="enable_call_blocking">Enable Call Blocking</string>
    <string name="grant_permission">Grant Call Screening Permission</string>
    <string name="screening_active">Call screening is active. Your phone app is unchanged.</string>
    <string name="screening_not_granted">Call screening permission not granted yet.</string>
    <string name="samsung_note">This app works as a silent background filter. Your Samsung Phone app will NOT be replaced.</string>
    <string name="privacy_policy_url">https://dhatric.github.io/RegexCaller/privacy-policy/</string>
    <string name="open_privacy_policy">Open Privacy Policy</string>
</resources>
```

**Verification:** App compiles, resources are resolvable.

---

## Step 1.8: Verify ProGuard Rules File Exists

**Action:** Create or update `app/proguard-rules.pro`:

```
# Room entity — needed for reflection
-keep class com.regexcaller.callblocker.data.db.BlockRule { *; }
-keep interface com.regexcaller.callblocker.data.db.BlockRuleDao { *; }

# Engine classes used by CallScreeningService (system-bound)
-keep class com.regexcaller.callblocker.engine.CallBlockerService { *; }
-keep class com.regexcaller.callblocker.engine.PatternMatcher { *; }
-keep class com.regexcaller.callblocker.engine.NumberNormalizer { *; }

# BlockAction constants
-keep class com.regexcaller.callblocker.data.model.BlockAction { *; }

# Coroutines
-dontwarn kotlinx.coroutines.**
```

> **Important:** This app intentionally does **not** include the `android.permission.INTERNET` permission. All data stays on-device. Do **not** add internet permission or any analytics/crash-reporting SDK that requires it (e.g., Firebase Crashlytics). If crash reporting is needed in the future, use an offline-first solution.

**Verification:** Release build (`./gradlew assembleRelease`) succeeds and the Play bundle (`./gradlew bundleRelease`) can be generated when signing is configured.

---

## Phase 1 Completion Checklist

| #   | Check | Status |
| --- | --- | --- |
| 1.1 | Project created via Android Studio wizard, runs on device | \[ \] |
| 1.2 | KSP plugin added to root Gradle, sync succeeds | \[ \] |
| 1.3 | App Gradle extended with Room, Nav, DataStore deps | \[ \] |
| 1.4 | App-specific directories added alongside wizard files | \[ \] |
| 1.5 | Application class exists and is declared in manifest | \[ \] |
| 1.6 | BlockAction enum created | \[ \] |
| 1.7 | String resources updated | \[ \] |
| 1.8 | ProGuard rules file populated | \[ \] |
| \-  | `./gradlew assembleDebug` **passes** | \[ \] |
| \-  | **Compose activity launches on emulator/device** | \[ \] |

**STOP. Do not proceed to Phase 2 until all checks pass.**
