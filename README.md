# RegexCaller - Pattern-Based Call Blocker

**RegexCaller** is a robust, lightweight Android call screening application tailored to quickly drop telemarketing and VoIP spam calls based on user-defined Wildcard prefixes, exact numbers, or complex explicit Regex matching. 

Unlike traditional "Dialer Replacement" apps, RegexCaller takes advantage of Android's modern `ROLE_CALL_SCREENING` permission. It runs entirely silently in the background filtering incoming calls without displacing your system's default dialer application. 

This project was built from scratch utilizing strict **Test-Driven Development (TDD)** and hardened to support deep-sleeping functionality on One UI (Samsung Galaxy S23) devices running Android 15.

## Key Features

- **Wildcard Blocking:** Instantly block entire area codes or sequential spammers.
  - Prefix (`98765*` -> matches numbers starting with 98765)
  - Suffix (`*1234` -> matches numbers ending with 1234)
  - Length Substitutes (`9876?00000` -> matched exactly one character)
- **Regex Blocking:** Formulate explicit expressions (e.g. `^\+91.*00$`) for granular filtering.
- **Rule Priorities:** Apply `BLOCK`, `SILENCE`, or `ALLOW` actions. Allow rules are strictly evaluated first allowing you to easily whitelist priority numbers against broad blocking sweeps.
- **Safe Evaluation:** Built-in Test tool lets you pass raw numbers through your database to determine what Rule catches it.
- **Privacy First Approach:** Requires zero location, analytics, or Internet permissions. Local Room Database. 

## Technical Architecture
- **Language:** Kotlin
- **UI Toolkit:** Jetpack Compose (Material 3 Dynamic Theming)
- **Data Layer:** Room (SQLite via Kotlin Symbol Processing)
- **Testing Layers:** 60+ JVM Unit Tests (`Junit4`) + Android Instrumented DAOs. 

## Requirements
To verify and compile this project locally:

- Open JDK 17
- Android Studio Ladybug (or newer recommended)
- A physical Android device running SDK 29 (Android 10) or higher.
- `adb` configured in your `$PATH`

---

## 🚀 Getting Started

### 1. Automated Testing Setup
Because RegexCaller leverages a strict TDD foundation, you can confirm all 60 of the project's logic behaviors without needing a physical device. Run all isolated JVM testing routines via terminal:

```bash
# Evaluate NumberNormalizer, Pattern Matcher, and ViewModel logic 
./gradlew testDebugUnitTest
```

To run the full suite of Instrumented Android tests (evaluating SQLite SQL migrations on actual hardware instances), attach a physical device or emulator and run:
```bash
./gradlew connectedAndroidTest
```

### 2. Building for Production

Compile a signed, Proguard-minified release APK to test natively on your Android Device:

```bash
# Generate the Minified Release Build
./gradlew assembleRelease
```

### 3. Installing via ADB

To push your compiled `.apk` to your phone directly from your terminal, assure your phone is plugged in with "USB Debugging" enabled under Developer Settings:

```bash
# Push the release build to your connected device
# Note: For MacOS, adb is usually installed to ~/Library/Android/sdk/platform-tools/adb
adb install app/build/outputs/apk/release/app-release.apk

# If the app was previously installed, you can use the replace flag
adb install -r app/build/outputs/apk/release/app-release.apk
```

### 4. Running the App & Onboarding
1. Open RegexCaller from your App Drawer.
2. The `OnboardingScreen` will prompt you to apply "Call Screening" permissions. **If the Android System Dialog misleadingly asks to "Set RegexCaller as your default caller ID app" — click Allow.** It will not replace your stock phone dialer.
3. Click the `+` Floating Action Button to generate an `ALLOW` or `BLOCK` rule.
4. Have a friend or secondary device trigger an incoming call to observe the filtering. 

---

*This application was developed via AI-assisted TDD processes tailored to resolve quirks historically seen on aggressively background-restricted Android distributions.*
