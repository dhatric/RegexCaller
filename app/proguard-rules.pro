# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

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

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile