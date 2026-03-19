package com.regexcaller.callblocker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.regexcaller.callblocker.data.db.BlockRule
import com.regexcaller.callblocker.data.model.BlockAction
import com.regexcaller.callblocker.ui.viewmodel.BlockRuleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: BlockRuleViewModel = viewModel()
) {
    val rules by viewModel.allRules.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RegexCaller") },
                actions = {
                    IconButton(onClick = { navController.navigate("test") }) {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = "Test Number")
                    }
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("add_rule") }) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Rule")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (rules.isEmpty()) {
                Text(
                    text = "No rules yet.\nTap + to add a call block rule.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(rules, key = { it.id }) { rule ->
                        RuleItem(
                            rule = rule,
                            onToggle = { enabled -> viewModel.setEnabled(rule.id, enabled) },
                            onEdit = { navController.navigate("edit_rule/${rule.id}") },
                            onDelete = { viewModel.delete(rule) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun RuleItem(
    rule: BlockRule,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = rule.label.ifBlank { "Unnamed Rule" },
                fontWeight = FontWeight.Bold
            )
        },
        supportingContent = {
            Column {
                Text(
                    text = "Pattern: ${rule.pattern}" + if (rule.isRegex) " (Regex)" else " (Wildcard)",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Action: ${rule.action} • Matches: ${rule.matchCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = when (rule.action) {
                        BlockAction.BLOCK -> MaterialTheme.colorScheme.error
                        BlockAction.SILENCE -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Rule")
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Rule")
                }
                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = onToggle
                )
            }
        }
    )
}
