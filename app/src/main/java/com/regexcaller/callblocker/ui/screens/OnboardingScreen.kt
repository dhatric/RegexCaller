package com.regexcaller.callblocker.ui.screens

import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.regexcaller.callblocker.util.hasCallScreeningRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = context as? Activity

    var roleGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                hasCallScreeningRole(context)
            else false
        )
    }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            roleGranted = hasCallScreeningRole(context)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Enable Call Blocking") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (roleGranted)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        if (roleGranted) Icons.Default.Warning // Using warning as a placeholder for check circle to avoid extra imports if checkcircle doesn't exist
                        else Icons.Default.Warning,
                        contentDescription = null
                    )
                    Text(
                        if (roleGranted)
                            "Call screening is active. Samsung Phone is unchanged."
                        else
                            "Call screening permission not granted yet."
                    )
                }
            }

            if (!roleGranted) {
                Text(
                    "RegexCaller works as a silent background filter. " +
                    "Your Samsung Phone app will NOT be replaced.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    "Tap the button below. Android will show a one-time " +
                    "confirmation dialog.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && activity != null) {
                            val roleManager = activity.getSystemService(
                                android.app.role.RoleManager::class.java
                            )
                            val intent = roleManager.createRequestRoleIntent(
                                android.app.role.RoleManager.ROLE_CALL_SCREENING
                            )
                            roleLauncher.launch(intent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Call Screening Permission")
                }

                // Samsung-specific reassurance
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "What happens when you grant this permission:",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("• Samsung Phone stays as your default dialer")
                        Text("• RegexCaller silently checks each call")
                        Text("• Matched calls are blocked before they ring")
                        Text("• Unmatched calls ring normally through Samsung Phone")
                        Text("• You can revoke this anytime in Android Settings")
                    }
                }
            } else {
                Text(
                    "Everything is set up! Your call screening is active.",
                    style = MaterialTheme.typography.bodyLarge
                )

                Button(
                    onClick = { navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }},
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start Adding Rules")
                }
            }
        }
    }
}
