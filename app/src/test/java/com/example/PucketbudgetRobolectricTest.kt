package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PucketbudgetRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appName_isPucketbudget() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Pucketbudget", context.getString(R.string.app_name))
    }

    @Test
    fun bottomNavigation_and_addExpenseFlow_works() {
        composeRule.waitForIdle()

        // Verify all 5 bottom navigation items and Home balance card
        composeRule.onNodeWithTag("home_balance_card").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_home").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_transactions").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_budget").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_savings").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_settings").assertIsDisplayed()

        // Scroll Home LazyColumn to reveal quick action button and click Add Expense
        composeRule.onNodeWithTag("home_list").performScrollToNode(hasTestTag("home_add_expense_btn"))
        composeRule.onNodeWithTag("home_add_expense_btn").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("tx_amount_input").performTextInput("45.50")
        composeRule.onNodeWithTag("tx_description_input").performTextInput("Coffee and Lunch")
        composeRule.onNodeWithTag("cat_chip_food").performScrollTo().performClick()
        composeRule.onNodeWithTag("tx_save_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Navigate to Transactions and verify record appears
        composeRule.onNodeWithTag("nav_transactions").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Coffee and Lunch").assertIsDisplayed()

        // Test search filter in Transactions
        composeRule.onNodeWithTag("tx_search_input").performTextInput("Coffee")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Coffee and Lunch").assertIsDisplayed()

        // Navigate to Budget and Reports
        composeRule.onNodeWithTag("nav_budget").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("segment_reports").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Expenses by Period").assertIsDisplayed()
        composeRule.onNodeWithText("Income vs Expenses").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Category Breakdown").performScrollTo().assertIsDisplayed()

        // Navigate to Savings
        composeRule.onNodeWithTag("nav_savings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("new_savings_goal_btn").assertIsDisplayed()

        // Navigate to Settings
        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_currency_row").assertIsDisplayed()
    }
}
