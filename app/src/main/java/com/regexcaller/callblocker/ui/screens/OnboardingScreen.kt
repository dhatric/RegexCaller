package com.regexcaller.callblocker.ui.screens

import android.app.Activity
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.navigation.NavController
import com.regexcaller.callblocker.ui.theme.ringBlockPrimaryButtonColors
import com.regexcaller.callblocker.ui.theme.ringBlockSectionCardColors
import com.regexcaller.callblocker.ui.theme.ringBlockTopAppBarColors
import com.regexcaller.callblocker.util.hasCallScreeningRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = context as? Activity
    val canNavigateBack = navController.previousBackStackEntry != null

    var roleGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                hasCallScreeningRole(context)
            else false
        )
    }

    var contactsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            roleGranted = hasCallScreeningRole(context)
        }
    }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        contactsGranted = granted
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = ringBlockTopAppBarColors(),
                title = { Text("Enable Call Blocking", color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    if (canNavigateBack) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 18.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (roleGranted) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                    Text(
                        if (roleGranted)
                            "Call screening is active."
                        else
                            "Call screening permission not granted yet.",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (roleGranted) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
            }

            if (!roleGranted) {
                Text(
                    "RingBlock works as a silent background filter. " +
                    "Your Phone app will NOT be replaced.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    "Tap the button below. Android will show a one-time " +
                    "confirmation dialog.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    modifier = Modifier.fillMaxWidth(),
                    colors = ringBlockPrimaryButtonColors()
                ) {
                    Text("Grant Call Screening Permission")
                }

                // Samsung-specific reassurance
                Card(
                    colors = ringBlockSectionCardColors(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "What happens when you grant this permission:",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("• Current Phone stays as your default dialer", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("• RingBlock silently checks each call", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("• Matched calls are blocked before they ring", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("• Unmatched calls ring normally through Phone", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("• You can revoke this anytime in Android Settings", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Text(
                    "Everything is set up! Your call screening is active.",
                    style = MaterialTheme.typography.titleSmall
                )

                if (!contactsGranted) {
                    Card(
                        colors = ringBlockSectionCardColors(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Optional but recommended: Allow Contacts permission",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Without this, Android may skip screening for calls from saved contacts.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ringBlockPrimaryButtonColors()
                            ) {
                                Text("Grant Contacts Permission")
                            }
                        }
                    }
                }

                Button(
                    onClick = { navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }},
                    modifier = Modifier.fillMaxWidth(),
                    colors = ringBlockPrimaryButtonColors()
                ) {
                    Text("Start Adding Rules")
                }
            }
        }
    }
}
