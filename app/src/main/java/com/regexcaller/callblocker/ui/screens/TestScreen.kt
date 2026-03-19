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
import com.regexcaller.callblocker.ui.theme.ringBlockSectionCardColors
import com.regexcaller.callblocker.ui.theme.ringBlockTopAppBarColors
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
                colors = ringBlockTopAppBarColors(),
                title = { Text("Test Rule Matcher", color = MaterialTheme.colorScheme.onSurface) },
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
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = ringBlockSectionCardColors()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Try a number",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Verify if a specific phone number gets blocked by one of your rules.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (rawNumber.isNotBlank()) {
                Text(
                    text = "Result",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (matchResult == null) {
                    Card(
                        colors = ringBlockSectionCardColors(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Call Allowed",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "No rules match this number.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    val cardColor = when (matchResult.action) {
                        BlockAction.BLOCK -> MaterialTheme.colorScheme.errorContainer
                        BlockAction.SILENCE -> MaterialTheme.colorScheme.surfaceContainerHigh
                        else -> MaterialTheme.colorScheme.primaryContainer
                    }
                    val titleColor = when (matchResult.action) {
                        BlockAction.BLOCK -> MaterialTheme.colorScheme.error
                        BlockAction.SILENCE -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Action: ${matchResult.action}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = titleColor
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("Matched Rule: ${matchResult.label}")
                            Text(
                                "Pattern: ${matchResult.pattern}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
