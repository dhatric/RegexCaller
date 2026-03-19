package com.regexcaller.callblocker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.regexcaller.callblocker.data.model.BlockAction
import com.regexcaller.callblocker.engine.NumberNormalizer
import com.regexcaller.callblocker.engine.PatternMatcher
import com.regexcaller.callblocker.ui.viewmodel.BlockRuleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreen(
    navController: NavController,
    viewModel: BlockRuleViewModel = viewModel()
) {
    var rawNumber by remember { mutableStateOf("") }
    val rules by viewModel.allRules.collectAsState(initial = emptyList())

    val matchResult = remember(rawNumber, rules) {
        if (rawNumber.isBlank()) null
        else PatternMatcher.findMatchingRule(rawNumber, rules)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test Rule Matcher") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Verify if a specific phone number gets blocked by one of your rules.",
                style = MaterialTheme.typography.bodyLarge
            )

            // Number input
            OutlinedTextField(
                value = rawNumber,
                onValueChange = { rawNumber = it },
                label = { Text("Enter a phone number to test") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (rawNumber.isNotBlank()) {
                val normalized = NumberNormalizer.normalize(rawNumber)
                Text(
                    text = "Normalized Form: $normalized",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display results
            if (rawNumber.isNotBlank()) {
                if (matchResult == null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Call Allowed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("No rules match this number.")
                        }
                    }
                } else {
                    val cardColor = when (matchResult.action) {
                        BlockAction.BLOCK -> MaterialTheme.colorScheme.errorContainer
                        BlockAction.SILENCE -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.tertiaryContainer
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Action: ${matchResult.action}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("Matched Rule: ${matchResult.label}")
                            Text("Pattern: ${matchResult.pattern}")
                        }
                    }
                }
            }
        }
    }
}
