# 00-PREREQUISITES

# Prerequisites — Development Environment Setup

## Overview

This document covers **everything you need to install and configure** before starting Phase 1. Targeted at **macOS with Apple Silicon** (M1/M2/M3/M4), but also applicable to Intel Macs with minor differences noted.

> **Windows / Linux Users:** The concepts are identical — JDK 17, Android Studio, SDK 35, emulator. Replace Homebrew commands with your platform’s equivalents:
> 
> - **Windows:** Use the Android Studio installer from https://developer.android.com/studio, install JDK 17 via https://adoptium.net, and use PowerShell or `cmd` for `adb`/`sdkmanager` commands. Set `ANDROID_HOME` and `JAVA_HOME` as Windows environment variables.
> - **Linux:** Use your distro’s package manager for JDK 17 (`sudo apt install openjdk-17-jdk`) and download Android Studio as a `.tar.gz`. Follow the same SDK/emulator setup.  
>     All verification commands (`adb devices`, `java -version`, etc.) work identically across platforms.

Complete all steps below. Each has a verification command — **do not proceed to Phase 1 until all verifications pass.**

---

## 1\. Homebrew (Package Manager)

If you don't have Homebrew installed:

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

After installation, add Homebrew to your PATH (Apple Silicon default location):

```bash
echo 'eval "$(/opt/homebrew/bin/brew shellenv)"' >> ~/.zprofile
eval "$(/opt/homebrew/bin/brew shellenv)"
```

**Verification:**

```bash
brew --version
# Expected: Homebrew 4.x.x
```

---

## 2\. JDK 17

Android Studio bundles its own JDK, but having a system JDK is useful for command-line Gradle builds.

```bash
brew install openjdk@17
```

Add to your shell profile (`~/.zshrc`):

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"
```

Reload:

```bash
source ~/.zshrc
```

**Verification:**

```bash
java -version
# Expected: openjdk version "17.x.x"

echo $JAVA_HOME
# Expected: /Library/Java/JavaVirtualMachines/openjdk-17.jdk/Contents/Home (or similar)
```

> **Note:** If Android Studio's bundled JDK conflicts, you can skip this and use Android Studio's JDK exclusively. Android Studio → Settings → Build → Gradle → Gradle JDK should show "jbr-17" (bundled).

---

## 3\. Android Studio

### Install

```bash
brew install --cask android-studio
```

Or download directly from: https://developer.android.com/studio

> **Apple Silicon:** Android Studio has native ARM64 support since Arctic Fox. The download page auto-detects your chip — make sure you get the **Apple Silicon (arm64)** build, not the Intel one.

### First Launch Setup

1.  Open Android Studio from `/Applications/Android Studio.app`
2.  The **Setup Wizard** will run on first launch:
    - Select **Standard** installation type
    - Accept all SDK license agreements
    - Wait for the initial SDK download to complete (~2-3 GB)

This installs:

- Android SDK (latest stable)
- Android SDK Build-Tools
- Android SDK Platform-Tools (`adb`, `fastboot`)
- Android Emulator
- Intel/ARM system images

**Verification:**

```bash
# Android Studio installed
ls /Applications/Android\ Studio.app
# Should exist

# After first-launch setup completes:
# Open Android Studio → Settings → Languages & Frameworks → Android SDK
# "Android SDK Location" should show: /Users/<you>/Library/Android/sdk
```

---

## 4\. Android SDK Components

After Android Studio's initial setup, open **SDK Manager** (Settings → Languages & Frameworks → Android SDK) and ensure these are installed:

### SDK Platforms Tab

| Component | Required | Why |
| --- | --- | --- |
| Android 15.0 (API 35) | ✅   | Target SDK for this project |
| Android 10.0 (API 29) | ✅   | Min SDK — needed for emulator testing |

### SDK Tools Tab

| Component | Required | Why |
| --- | --- | --- |
| Android SDK Build-Tools 35.x | ✅   | Compiles the app |
| Android SDK Platform-Tools | ✅   | `adb` for device communication |
| Android Emulator | ✅   | Run/test without a physical device |
| Android SDK Command-line Tools | ✅   | `sdkmanager`, `avdmanager` CLI access |
| Google Play services | Optional | Not required for this app |

### Install via Command Line (alternative)

Set up the `ANDROID_HOME` environment variable first (add to `~/.zshrc`):

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/emulator:$PATH"
```

Then reload and install:

```bash
source ~/.zshrc

sdkmanager "platforms;android-35" "platforms;android-29"
sdkmanager "build-tools;35.0.0"
sdkmanager "platform-tools"
sdkmanager "emulator"
sdkmanager "cmdline-tools;latest"
```

**Verification:**

```bash
adb --version
# Expected: Android Debug Bridge version 1.0.41 (or later)

sdkmanager --list | grep "platforms;android-35"
# Expected: platforms;android-35 | 2 | Android SDK Platform 35

echo $ANDROID_HOME
# Expected: /Users/<you>/Library/Android/sdk
```

---

## 5\. Android Emulator (Virtual Device)

Create an emulator to test the app without a physical Samsung S23.

### Via Android Studio (recommended)

1.  Open **Device Manager** (Tools → Device Manager)
2.  Click **Create Virtual Device**
3.  Select hardware: **Pixel 7** or **Galaxy S23** (if Samsung skin available)
4.  Select system image: **API 35 — arm64-v8a** (Apple Silicon native — do NOT pick x86_64)
5.  Name it (e.g., `Pixel_7_API_35`)
6.  Finish

> **Apple Silicon critical:** Always select **arm64-v8a** system images. x86_64 images require translation and are extremely slow on Apple Silicon.

### Via Command Line

```bash
# List available system images
sdkmanager --list | grep "system-images;android-35"

# Install ARM64 image
sdkmanager "system-images;android-35;google_apis;arm64-v8a"

# Create AVD
avdmanager create avd -n Pixel_7_API_35 -k "system-images;android-35;google_apis;arm64-v8a" -d "pixel_7"
```

**Verification:**

```bash
# List created AVDs
emulator -list-avds
# Expected: Pixel_7_API_35

# Boot the emulator (first boot takes a few minutes)
emulator -avd Pixel_7_API_35 &

# Check device is connected
adb devices
# Expected: emulator-5554   device
```

---

## 6\. Physical Device Setup (Samsung S23 — Optional but Recommended)

For final testing on the actual target device:

### Enable Developer Options

1.  Go to **Settings → About phone → Software information**
2.  Tap **Build number** 7 times → "Developer mode enabled"
3.  Go back to **Settings → Developer options**
4.  Enable **USB debugging**
5.  (Optional) Enable **Wireless debugging** for cable-free deployment

### Connect via USB

```bash
# Plug in Samsung S23 via USB-C
adb devices
# Expected: XXXXXXXXX   device
# First connection: tap "Allow" on the phone's USB debugging prompt
```

### Connect via Wi-Fi (Android 11+)

```bash
# Both Mac and phone must be on the same Wi-Fi network
# On phone: Developer options → Wireless debugging → Pair device with pairing code
adb pair <ip>:<port>
# Enter the pairing code shown on phone

adb connect <ip>:<port>
adb devices
# Expected: <ip>:<port>   device
```

**Verification:**

```bash
adb devices
# At least one device listed (emulator or physical)

adb shell getprop ro.build.version.sdk
# Expected: 35 (or 34 depending on Samsung firmware)
```

---

## 7\. Git (Version Control)

```bash
# Usually pre-installed on macOS. Verify:
git --version
# Expected: git version 2.x.x

# If not installed:
xcode-select --install
# OR
brew install git
```

Configure:

```bash
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"
```

---

## 8\. Gradle (Build System)

**You do NOT need to install Gradle globally.** The project uses the Gradle Wrapper (`gradlew`), which downloads the correct Gradle version automatically.

After creating the project in Phase 1, verify it works:

```bash
cd /path/to/RegexCaller
./gradlew --version
# Expected: Gradle 8.x.x (downloaded automatically)
```

> **Note on Apple Silicon:** Gradle runs inside the JVM, so it uses the JDK you configured. No special ARM config needed.

---

## Environment Variables Summary

Add all of these to `~/.zshrc` (create the file if it doesn't exist):

```bash
# Java
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH="$JAVA_HOME/bin:$PATH"

# Android SDK
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
export PATH="$ANDROID_HOME/emulator:$PATH"
```

Reload after editing:

```bash
source ~/.zshrc
```

---

## Prerequisites Checklist

Run each command and confirm the expected output:

| #   | Check | Command | Expected |
| --- | --- | --- | --- |
| 1   | Homebrew installed | `brew --version` | `Homebrew 4.x.x` |
| 2   | JDK 17 installed | `java -version` | `openjdk version "17.x.x"` |
| 3   | JAVA_HOME set | `echo $JAVA_HOME` | Path to JDK 17 |
| 4   | Android Studio installed | `ls /Applications/Android\ Studio.app` | Exists |
| 5   | ANDROID_HOME set | `echo $ANDROID_HOME` | `~/Library/Android/sdk` |
| 6   | Android SDK 35 installed | `sdkmanager --list \| grep "android-35"` | `platforms;android-35` |
| 7   | adb available | `adb --version` | `Android Debug Bridge 1.0.x` |
| 8   | Emulator created | `emulator -list-avds` | At least one AVD listed |
| 9   | Emulator boots (or physical device seen) | `adb devices` | Device listed as `device` |
| 10  | Git installed | `git --version` | `git version 2.x.x` |

**STOP. Do not proceed to Phase 1 until all 10 checks pass.**

---

## Troubleshooting

### Android Studio won't start on Apple Silicon

- Make sure you downloaded the **Apple Silicon** build (check with: `file /Applications/Android\ Studio.app/Contents/MacOS/studio` → should say `arm64`)
- If you accidentally installed the Intel build, it will run via Rosetta but will be slow

### `sdkmanager: command not found`

- The command-line tools may not be installed yet. Install via:
    - Android Studio → Settings → SDK Tools → "Android SDK Command-line Tools"
    - Or: download from https://developer.android.com/studio#command-tools

### Emulator crashes or is extremely slow

- **Apple Silicon users:** You MUST use `arm64-v8a` system images, not `x86_64`
- Allocate at least 2 GB RAM to the AVD (Device Manager → Edit → Show Advanced Settings → RAM)
- Close other heavy apps to free memory

### `adb devices` shows `unauthorized`

- Check your phone screen — there should be an "Allow USB debugging?" prompt
- If no prompt: revoke USB debugging authorizations (Developer Options) and reconnect

### Gradle sync fails with JDK errors

- Android Studio → Settings → Build → Gradle → Gradle JDK → Select "jbr-17" (bundled)
- Alternatively, point it to your Homebrew-installed JDK 17