package com.arun.tankerapp.ui.calendar

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class VacancyEntryTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun clicking_date_opens_vacancy_sheet() {
        // Handle Splash Screen delay
        val today = LocalDate.now()
        val formattedDate = today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
        val dayStr = today.dayOfMonth.toString()

        // Wait for Calendar
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText(dayStr).fetchSemanticsNodes().isNotEmpty()
        }
        
        // Find Today's date cell and Click it
        // Note: There might be multiple cells with same day number (from prev/next month if shown? No, grid implementation logic uses nulls for padding).
        // But day number exists once.
        composeTestRule.onNodeWithText(dayStr).performClick()
        
        // Check for Sheet Header "Vacancies for <Date>"
        composeTestRule.waitUntil(timeoutMillis = 2000) {
             composeTestRule.onAllNodesWithText("Vacancies for $formattedDate").fetchSemanticsNodes().isNotEmpty()
        }
        
        composeTestRule.onNodeWithText("Vacancies for $formattedDate").assertIsDisplayed()
        
        // Verify list contains sample apartment (e.g. 101)
        composeTestRule.onNodeWithText("Apartment 101").assertIsDisplayed()
    }
}
