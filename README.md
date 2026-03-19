# RingBlock - Pattern Call Blocker

**RingBlock** is a lightweight Android call screening app that blocks spam and unwanted calls using custom number patterns, exact matches, or explicit regex rules.

Unlike dialer replacement apps, RingBlock uses Android's `ROLE_CALL_SCREENING` permission. It runs silently in the background and filters incoming calls without replacing your default phone app.

This project is built with Kotlin, Jetpack Compose, and a test-first workflow, with special care for Samsung / One UI behavior on modern Android versions.

## Key Features

- **Pattern-Based Blocking:** Instantly block entire area codes, repeating spam ranges, or number shapes.
  - Prefix (`98765*` -> matches numbers starting with 98765)
  - Suffix (`*1234` -> matches numbers ending with 1234)
  - Single-character wildcard (`9876?00000` -> matches exactly one digit in place of `?`)
- **Regex Rules:** Use explicit regex patterns (for example `^\+91.*00$`) for more advanced filtering.
- **Rule Actions:** Apply `BLOCK`, `SILENCE`, or `ALLOW`. Allow rules are evaluated first so you can safely whitelist important numbers.
- **Rule Import / Export:** Back up your rules to JSON and restore them later from the Settings screen.
- **Built-in Rule Matcher:** Test a number against your saved rules from Settings to see which rule would apply.
- **Privacy First:** No analytics, no Internet permission, and local-only rule storage through Room. Android cloud backup is disabled so rules stay on-device.

## Technical Architecture
- **Language:** Kotlin
- **UI Toolkit:** Jetpack Compose (Material 3 Dynamic Theming)
- **Data Layer:** Room (SQLite via Kotlin Symbol Processing)
- **Testing Layers:** JVM unit tests plus Android instrumented database and UI tests.

## Requirements
To verify and compile this project locally:

- Open JDK 17
- Android Studio Ladybug (or newer recommended)
- A physical Android device running SDK 29 (Android 10) or higher.
- `adb` configured in your `$PATH`

---

## 🚀 Getting Started

### 1. Automated Testing Setup
You can verify the core logic without needing a physical device:

```bash
# Evaluate matcher, repository, transfer, and ViewModel logic
./gradlew testDebugUnitTest
```

To compile or run Android instrumented tests, attach a device or emulator:
```bash
# Compile Android test sources
./gradlew compileDebugAndroidTestKotlin

# Run instrumented tests on a connected device/emulator
./gradlew connectedAndroidTest
```

### 2. Building for Production

Compile a signed, Proguard-minified release APK:

**Note:** You must provide your own signing keystore via environment variables or `gradle.properties`:
- `RELEASE_STORE_FILE`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

```bash
# Generate the minified release build
./gradlew assembleRelease
```

### 3. Installing via ADB

To push the compiled APK to your phone, enable USB debugging and connect the device:

```bash
# Push the release build to your connected device
# Note: For MacOS, adb is usually installed to ~/Library/Android/sdk/platform-tools/adb
adb install app/build/outputs/apk/release/app-release.apk

# If the app was previously installed, you can use the replace flag
adb install -r app/build/outputs/apk/release/app-release.apk
```

### 4. Running the App & Onboarding
1. Open RingBlock from your app drawer.
2. The onboarding flow will prompt you to grant Call Screening permission. If Android phrases this as setting RingBlock as the default caller ID or screening app, allow it. It does not replace your normal phone dialer.
3. Add your rules from the Home screen with the `+` action.
4. Use Settings to test rule matching or import/export your rule set.
5. Trigger a real call from another device to confirm filtering behavior.

---

*This application was developed with AI-assisted implementation and a test-first workflow, with extra attention paid to behavior on aggressively background-restricted Android builds.*
