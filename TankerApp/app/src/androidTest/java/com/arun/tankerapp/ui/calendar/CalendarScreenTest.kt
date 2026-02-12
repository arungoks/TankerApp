package com.arun.tankerapp.ui.calendar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arun.tankerapp.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CalendarScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun calendar_displays_current_month() {
        val currentMonth = YearMonth.now()
        val monthName = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val year = currentMonth.year.toString()

        // Wait for Splash to finish (max 3000ms)
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText(monthName).fetchSemanticsNodes().isNotEmpty()
        }

        // Verify Header
        composeTestRule.onNodeWithText(monthName).assertIsDisplayed()
        composeTestRule.onNodeWithText(year).assertIsDisplayed()
    }

    @Test
    fun next_month_navigation_updates_header() {
        val currentMonth = YearMonth.now()
        val nextMonth = currentMonth.plusMonths(1)
        val nextMonthName = nextMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
        val currentMonthName = currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())

        // Wait for Splash
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText(currentMonthName).fetchSemanticsNodes().isNotEmpty()
        }

        // Click Next Arrow
        composeTestRule.onNodeWithContentDescription("Next Month").performClick()

        // Verify New Month Name
        composeTestRule.onNodeWithText(nextMonthName).assertIsDisplayed()
    }
}
