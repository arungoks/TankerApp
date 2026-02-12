package com.arun.tankerapp.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.arun.tankerapp.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AboutScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun navigateToAboutScreen_displaysCorrectInfo() {
        // Start at Home Screen
        composeTestRule.onNodeWithText("Tanker App Home").assertIsDisplayed()
        
        // Click "Go to About"
        composeTestRule.onNodeWithText("Go to About").performClick()
        
        // Verify About Screen Content
        composeTestRule.onNodeWithText("TankerApp").assertIsDisplayed()
        composeTestRule.onNodeWithText("Developed by Arun").assertIsDisplayed()
        
        // Check for Version text (partial match or accurate)
        // Since version name is dynamic, checking for prefix logic or full string "Version: 1.0"
        composeTestRule.onNodeWithText("Version: 1.0").assertIsDisplayed()
    }
}
