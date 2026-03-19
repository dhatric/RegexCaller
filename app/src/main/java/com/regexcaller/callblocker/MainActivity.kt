package com.regexcaller.callblocker

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.regexcaller.callblocker.ui.screens.*
import com.regexcaller.callblocker.ui.theme.CallBlockerTheme
import com.regexcaller.callblocker.util.hasCallScreeningRole

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CallBlockerTheme {
                val navController = rememberNavController()

                val startDestination = if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    !hasCallScreeningRole(this@MainActivity)
                ) "onboarding" else "home"

                NavHost(
                    navController = navController,
                    startDestination = startDestination
                ) {
                    composable("home") {
                        HomeScreen(navController)
                    }
                    composable("add_rule") {
                        AddRuleScreen(navController)
                    }
                    composable("edit_rule/{id}") { backStack ->
                        val id = backStack.arguments?.getString("id")?.toLongOrNull()
                        AddRuleScreen(navController, editRuleId = id)
                    }
                    composable("test") {
                        TestScreen(navController)
                    }
                    composable("settings") {
                        SettingsScreen(navController)
                    }
                    composable("onboarding") {
                        OnboardingScreen(navController)
                    }
                }
            }
        }
    }
}
