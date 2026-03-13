package com.regexcaller.callblocker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.regexcaller.callblocker.data.db.BlockRule
import com.regexcaller.callblocker.data.model.BlockAction
import com.regexcaller.callblocker.engine.PatternMatcher
import com.regexcaller.callblocker.ui.viewmodel.BlockRuleViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRuleScreen(
    navController: NavController,
    editRuleId: Long? = null,
    viewModel: BlockRuleViewModel = viewModel()
) {
    var label by remember { mutableStateOf("") }
    var pattern by remember { mutableStateOf("") }
    var isRegex by remember { mutableStateOf(false) }
    var action by remember { mutableStateOf(BlockAction.BLOCK) }
    var patternError by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    var existingRule by remember { mutableStateOf<BlockRule?>(null) }

    // Load existing rule if editing
    LaunchedEffect(editRuleId) {
        if (editRuleId != null) {
            val rule = viewModel.getRuleById(editRuleId)
            if (rule != null) {
                existingRule = rule
                label = rule.label
                pattern = rule.pattern
                isRegex = rule.isRegex
                action = rule.action
            }
        }
    }

    // Live validation
    LaunchedEffect(pattern, isRegex) {
        patternError = if (pattern.isNotBlank()) PatternMatcher.validatePattern(pattern, isRegex) else null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editRuleId == null) "Add Rule" else "Edit Rule") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 4.dp, shadowElevation = 8.dp) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(16.dp)) {
                    Button(
                        onClick = {
                            val finalLabel = if (label.isBlank()) pattern else label
                            val newRule = BlockRule(
                                id = existingRule?.id ?: 0L,
                                label = finalLabel,
                                pattern = pattern,
                                isRegex = isRegex,
                                action = action,
                                isEnabled = existingRule?.isEnabled ?: true,
                                matchCount = existingRule?.matchCount ?: 0,
                                createdAt = existingRule?.createdAt ?: System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                            
                            if (editRuleId == null) {
                                viewModel.insert(newRule)
                            } else {
                                viewModel.update(newRule)
                            }
                            navController.popBackStack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = pattern.isNotBlank() && patternError == null
                    ) {
                        Text("Save Rule")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Label Input
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Label (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            // Pattern Input
            OutlinedTextField(
                value = pattern,
                onValueChange = { pattern = it },
                label = { Text(if (isRegex) "Regex explicitly" else "Pattern or Number") },
                modifier = Modifier.fillMaxWidth(),
                isError = patternError != null,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text
                ),
                supportingText = {
                    if (patternError != null) {
                        Text(patternError!!, color = MaterialTheme.colorScheme.error)
                    } else if (pattern.isNotBlank() && !isRegex) {
                        Text(PatternMatcher.getPatternDescription(pattern))
                    }
                }
            )

            // Regex Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = "Use explicit Regex",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = isRegex,
                    onCheckedChange = { isRegex = it }
                )
            }

            // Action Selection
            Text("Action", style = MaterialTheme.typography.titleMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(BlockAction.BLOCK, BlockAction.SILENCE, BlockAction.ALLOW).forEach { act ->
                    FilterChip(
                        selected = action == act,
                        onClick = { action = act },
                        label = { Text(act) }
                    )
                }
            }

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Pattern Types:", style = MaterialTheme.typography.titleSmall)
                    Text("• Valid Phone Number (Exact matching)", style = MaterialTheme.typography.bodyMedium)
                    Text("• Prefix Wildcard: 98765* (Matches numbers starting with 98765)", style = MaterialTheme.typography.bodyMedium)
                    Text("• Suffix Wildcard: *1234 (Matches numbers ending with 1234)", style = MaterialTheme.typography.bodyMedium)
                    Text("• Length Wildcard: 9876?00000 (? dictates exactly 1 digit substitution)", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
