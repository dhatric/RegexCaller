package com.regexcaller.callblocker.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.regexcaller.callblocker.data.transfer.RuleImportStats
import com.regexcaller.callblocker.ui.theme.ringBlockPrimaryButtonColors
import com.regexcaller.callblocker.ui.theme.ringBlockSectionCardColors
import com.regexcaller.callblocker.ui.theme.ringBlockTopAppBarColors
import com.regexcaller.callblocker.ui.viewmodel.BlockRuleViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: BlockRuleViewModel = viewModel()
) {
    val uiState by viewModel.transferUiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportRules(uri)
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importRules(uri)
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.consumeTransferMessage()
        }
    }

    val importSummary = uiState.lastImportSummary
    if (importSummary != null) {
        ImportSummaryDialog(
            summary = importSummary,
            onDismiss = viewModel::clearImportSummary
        )
    }

    SettingsScreenContent(
        isBusy = uiState.isBusy,
        snackbarHostState = snackbarHostState,
        onBack = { navController.popBackStack() },
        onExportClick = {
            exportLauncher.launch("regexcaller-rules-${LocalDate.now()}.json")
        },
        onImportClick = {
            importLauncher.launch(arrayOf("application/json"))
        },
        onOpenPermissionsClick = {
            navController.navigate("onboarding")
        },
        onOpenRuleTesterClick = {
            navController.navigate("test")
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    isBusy: Boolean,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onOpenPermissionsClick: () -> Unit,
    onOpenRuleTesterClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                colors = ringBlockTopAppBarColors(),
                title = { Text("Settings", color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSectionCard(
                title = "Permissions & Setup",
                details = "Review onboarding steps and system permissions if call screening needs to be reconfigured."
            ) {
                Button(
                    onClick = onOpenPermissionsClick,
                    enabled = !isBusy,
                    colors = ringBlockPrimaryButtonColors(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Permissions Setup")
                }
            }
            SettingsSectionCard(
                title = "Rule Backup",
                details = "Export your blocking rules to a JSON backup file or import a RingBlock backup. Imports skip exact duplicate rules."
            ) {
                Button(
                    onClick = onExportClick,
                    enabled = !isBusy,
                    colors = ringBlockPrimaryButtonColors(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export Rules")
                }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onImportClick,
                    enabled = !isBusy,
                    colors = ringBlockPrimaryButtonColors(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Import Rules")
                }
            }

            SettingsSectionCard(
                title = "Tools",
                details = "Use the built-in matcher to check whether a number would be blocked by your current rules."
            ) {
                Button(
                    onClick = onOpenRuleTesterClick,
                    enabled = !isBusy,
                    colors = ringBlockPrimaryButtonColors(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Rule Matcher")
                }
            }



            if (isBusy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    details: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = ringBlockSectionCardColors()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                SectionInfoMenu(
                    title = title,
                    details = details
                )
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
private fun SectionInfoMenu(
    title: String,
    details: String
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "$title details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = details,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                onClick = { expanded = false }
            )
        }
    }
}

@Composable
internal fun ImportSummaryDialog(
    summary: RuleImportStats,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Summary") },
        text = {
            Box {
                Text(
                    "Imported ${summary.importedCount} rules and skipped ${summary.skippedCount} duplicates."
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
