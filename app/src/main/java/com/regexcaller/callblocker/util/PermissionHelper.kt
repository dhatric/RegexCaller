package com.regexcaller.callblocker.util

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
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

fun isSamsungDevice(): Boolean {
    return Build.MANUFACTURER.equals("samsung", ignoreCase = true)
}

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
