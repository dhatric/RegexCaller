package com.regexcaller.callblocker.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class AddRuleScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun defaultModeShowsSimplePatternHelpers() {
        composeTestRule.setContent {
            SectionCard(title = "Rule Type", subtitle = "Choose the easiest matching mode for this rule.") {}
            ExampleChips(isRegex = false, onExampleSelected = {})
        }

        composeTestRule.onNodeWithText("98765*").assertIsDisplayed()
        composeTestRule.onNodeWithText("*1234").assertIsDisplayed()
    }

    @Test
    fun regexModeShowsRegexExampleChip() {
        composeTestRule.setContent {
            ExampleChips(isRegex = true, onExampleSelected = {})
        }

        composeTestRule.onNodeWithText("^\\+91.*00$").assertIsDisplayed()
    }

    @Test
    fun actionSectionShowsFriendlyLabels() {
        composeTestRule.setContent {
            ActionSection(
                selectedAction = "BLOCK",
                onActionSelected = {}
            )
        }

        composeTestRule.onNodeWithText("Block").assertIsDisplayed()
        composeTestRule.onNodeWithText("Silence").assertIsDisplayed()
        composeTestRule.onNodeWithText("Allow").assertIsDisplayed()
    }

    @Test
    fun previewSectionShowsEmptyStateWhenPatternBlank() {
        composeTestRule.setContent {
            PreviewSection(
                pattern = "",
                patternError = null,
                previewText = null,
                isRegex = false,
                actionDescription = "Reject matching calls before they ring."
            )
        }

        composeTestRule.onNodeWithText("Start by entering a phone number, pattern, or regex to preview this rule.").assertIsDisplayed()
    }

    @Test
    fun previewSectionShowsValidationError() {
        composeTestRule.setContent {
            PreviewSection(
                pattern = "[abc",
                patternError = "Invalid pattern",
                previewText = null,
                isRegex = true,
                actionDescription = "Reject matching calls before they ring."
            )
        }

        composeTestRule.onNodeWithText("Invalid pattern").assertIsDisplayed()
    }
}
