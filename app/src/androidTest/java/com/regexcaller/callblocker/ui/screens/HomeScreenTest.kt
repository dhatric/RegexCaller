package com.regexcaller.callblocker.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.regexcaller.callblocker.data.db.BlockRule
import com.regexcaller.callblocker.data.model.BlockAction
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyStateShowsAddRuleCtaAndExamples() {
        composeTestRule.setContent {
            EmptyStateCard(onAddRule = {})
        }

        composeTestRule.onNodeWithText("Start blocking smarter").assertIsDisplayed()
        composeTestRule.onNodeWithText("Add Rule").assertIsDisplayed()
        composeTestRule.onNodeWithText("98765*").assertIsDisplayed()
    }

    @Test
    fun summaryCardShowsCounts() {
        composeTestRule.setContent {
            HomeSummaryCard(
                totalRules = 5,
                enabledRules = 3,
                totalBlockedCalls = 12
            )
        }

        composeTestRule.onNodeWithText("Protection active").assertIsDisplayed()
        composeTestRule.onNodeWithText("5").assertIsDisplayed()
        composeTestRule.onNodeWithText("3").assertIsDisplayed()
        composeTestRule.onNodeWithText("12").assertIsDisplayed()
    }

    @Test
    fun ruleCardShowsFriendlyActionLabels() {
        composeTestRule.setContent {
            RuleCard(
                rule = BlockRule(
                    label = "Spam Prefix",
                    pattern = "98765*",
                    action = BlockAction.BLOCK,
                    isEnabled = true,
                    matchCount = 4
                ),
                onToggle = {},
                onEdit = {},
                onDelete = {}
            )
        }

        composeTestRule.onNodeWithText("Name: Spam Prefix").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pattern: 98765*").assertIsDisplayed()
        composeTestRule.onNodeWithText("Block").assertIsDisplayed()
    }
}
