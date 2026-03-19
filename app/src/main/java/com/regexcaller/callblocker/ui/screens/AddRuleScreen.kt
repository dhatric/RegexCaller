package com.regexcaller.callblocker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.regexcaller.callblocker.data.db.BlockRule
import com.regexcaller.callblocker.data.model.BlockAction
import com.regexcaller.callblocker.engine.PatternMatcher
import com.regexcaller.callblocker.ui.theme.ringBlockFilterChipColors
import com.regexcaller.callblocker.ui.theme.ringBlockPrimaryButtonColors
import com.regexcaller.callblocker.ui.theme.ringBlockSectionCardColors
import com.regexcaller.callblocker.ui.theme.ringBlockTopAppBarColors
import com.regexcaller.callblocker.ui.viewmodel.BlockRuleViewModel

private enum class RuleMode {
    SIMPLE,
    REGEX
}

private data class ActionUiModel(
    val action: String,
    val label: String,
    val description: String
)

private val actionUiModels = listOf(
    ActionUiModel(
        action = BlockAction.BLOCK,
        label = "Block",
        description = "Reject matching calls before they ring."
    ),
    ActionUiModel(
        action = BlockAction.SILENCE,
        label = "Silence",
        description = "Let matching calls arrive silently."
    ),
    ActionUiModel(
        action = BlockAction.ALLOW,
        label = "Allow",
        description = "Never block matching calls."
    )
)

private val simpleExamples = listOf("98765*", "*1234", "9876?00000", "9876543210")
private const val regexExample = "^\\+91.*00$"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRuleScreen(
    navController: NavController,
    editRuleId: Long? = null,
    viewModel: BlockRuleViewModel = viewModel()
) {
    var label by remember { mutableStateOf("") }
    var pattern by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(RuleMode.SIMPLE) }
    var action by remember { mutableStateOf(BlockAction.BLOCK) }
    var patternError by remember { mutableStateOf<String?>(null) }
    var existingRule by remember { mutableStateOf<BlockRule?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val isEditing = editRuleId != null
    val isRegex = mode == RuleMode.REGEX
    val selectedAction = actionUiModels.first { it.action == action }
    val previewText = remember(pattern, mode, action) {
        buildPreviewText(
            pattern = pattern,
            isRegex = isRegex,
            actionLabel = selectedAction.label
        )
    }

    LaunchedEffect(editRuleId) {
        if (editRuleId != null) {
            val rule = viewModel.getRuleById(editRuleId)
            if (rule != null) {
                existingRule = rule
                label = rule.label
                pattern = rule.pattern
                mode = if (rule.isRegex) RuleMode.REGEX else RuleMode.SIMPLE
                action = rule.action
            }
        }
    }

    LaunchedEffect(pattern, mode) {
        patternError = if (pattern.isNotBlank()) {
            PatternMatcher.validatePattern(pattern, isRegex)
        } else {
            null
        }
    }

    if (showDeleteConfirmation && existingRule != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete rule?") },
            text = { Text("This will permanently remove this rule.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(existingRule!!)
                        showDeleteConfirmation = false
                        navController.popBackStack()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = ringBlockTopAppBarColors(),
                title = {
                    Text(
                        text = if (isEditing) "Edit Rule" else "Add Rule",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditing && existingRule != null) {
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Rule")
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(16.dp)
                ) {
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

                            if (isEditing) {
                                viewModel.update(newRule)
                            } else {
                                viewModel.insert(newRule)
                            }
                            navController.popBackStack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = pattern.isNotBlank() && patternError == null,
                        colors = ringBlockPrimaryButtonColors()
                    ) {
                        Text(if (isEditing) "Save Changes" else "Add Rule")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionCard(
                title = "Rule",
                subtitle = null
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(RuleMode.SIMPLE, RuleMode.REGEX).forEachIndexed { index, ruleMode ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = RuleMode.entries.size
                            ),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                activeBorderColor = MaterialTheme.colorScheme.primary,
                                inactiveBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            selected = mode == ruleMode,
                            onClick = { mode = ruleMode },
                            label = {
                                Text(
                                    if (ruleMode == RuleMode.SIMPLE) {
                                        "Simple"
                                    } else {
                                        "Regex"
                                    }
                                )
                            }
                        )
                    }
                }
                ActionSection(
                    selectedAction = action,
                    onActionSelected = { action = it }
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Name (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    supportingText = {
                        Text("Blank uses the pattern.")
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = {
                        Text(if (isRegex) "Regex pattern" else "Number or pattern")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    isError = patternError != null,
                    placeholder = {
                        Text(if (isRegex) regexExample else "98765*")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text
                    ),
                    supportingText = {
                        when {
                            patternError != null -> {
                                Text(
                                    text = patternError!!,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            isRegex -> {
                                Text(regexExample)
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExampleChips(
                    isRegex = isRegex,
                    onExampleSelected = { pattern = it }
                )
            }

            if (pattern.isNotBlank() || patternError != null) {
                SectionCard(
                    title = "Preview",
                    subtitle = null
                ) {
                    PreviewSection(
                        pattern = pattern,
                        patternError = patternError,
                        previewText = previewText,
                        isRegex = isRegex,
                        actionDescription = selectedAction.description
                    )
                }
            }
        }
    }
}

@Composable
internal fun SectionCard(
    title: String,
    subtitle: String?,
    content: @Composable () -> Unit
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
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider()
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ExampleChips(
    isRegex: Boolean,
    onExampleSelected: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val examples = if (isRegex) listOf(regexExample) else simpleExamples
        examples.forEach { example ->
            FilterChip(
                selected = false,
                onClick = { onExampleSelected(example) },
                colors = ringBlockFilterChipColors(),
                label = { Text(example) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ActionSection(
    selectedAction: String,
    onActionSelected: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actionUiModels.forEach { actionModel ->
            FilterChip(
                selected = selectedAction == actionModel.action,
                onClick = { onActionSelected(actionModel.action) },
                colors = ringBlockFilterChipColors(),
                label = { Text(actionModel.label) }
            )
        }
    }
    Text(
        text = actionUiModels.first { it.action == selectedAction }.description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
internal fun PreviewSection(
    pattern: String,
    patternError: String?,
    previewText: String?,
    isRegex: Boolean,
    actionDescription: String
) {
    when {
        patternError != null -> {
            Text(
                text = patternError,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        else -> {
            Text(
                text = previewText ?: "",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = actionDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun buildPreviewText(
    pattern: String,
    isRegex: Boolean,
    actionLabel: String
): String? {
    if (pattern.isBlank()) return null

    return if (isRegex) {
        "This advanced rule will $actionLabel when the regex matches an incoming number."
    } else {
        val actionText = when (actionLabel) {
            "Block" -> "block"
            "Silence" -> "silence"
            else -> "always allow"
        }
        "This rule will $actionText calls that ${PatternMatcher.getPatternDescription(pattern).removePrefix("Matches ").replaceFirstChar { it.lowercase() }}."
    }
}
