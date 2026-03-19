package com.regexcaller.callblocker.ui.screens

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.regexcaller.callblocker.data.transfer.RuleImportStats
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsImportExportActions() {
        composeTestRule.setContent {
            SettingsScreenContent(
                isBusy = false,
                snackbarHostState = SnackbarHostState(),
                onBack = {},
                onExportClick = {},
                onImportClick = {},
                onOpenPermissionsClick = {},
                onOpenRuleTesterClick = {}
            )
        }

        composeTestRule.onNodeWithText("Export Rules").assertIsDisplayed()
        composeTestRule.onNodeWithText("Import Rules").assertIsDisplayed()
        composeTestRule.onNodeWithText("Open Permissions Setup").assertIsDisplayed()
        composeTestRule.onNodeWithText("Open Rule Matcher").assertIsDisplayed()
    }

    @Test
    fun disablesActionsWhileBusy() {
        composeTestRule.setContent {
            SettingsScreenContent(
                isBusy = true,
                snackbarHostState = SnackbarHostState(),
                onBack = {},
                onExportClick = {},
                onImportClick = {},
                onOpenPermissionsClick = {},
                onOpenRuleTesterClick = {}
            )
        }

        composeTestRule.onNodeWithText("Export Rules").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Import Rules").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Open Permissions Setup").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Open Rule Matcher").assertIsNotEnabled()
    }

    @Test
    fun showsImportSummaryDialog() {
        composeTestRule.setContent {
            SettingsScreenContent(
                isBusy = false,
                snackbarHostState = SnackbarHostState(),
                onBack = {},
                onExportClick = {},
                onImportClick = {},
                onOpenPermissionsClick = {},
                onOpenRuleTesterClick = {}
            )
            ImportSummaryDialog(
                summary = RuleImportStats(importedCount = 2, skippedCount = 1),
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Import Summary").assertIsDisplayed()
        composeTestRule.onNodeWithText("Imported 2 rules and skipped 1 duplicates.").assertIsDisplayed()
    }
}
