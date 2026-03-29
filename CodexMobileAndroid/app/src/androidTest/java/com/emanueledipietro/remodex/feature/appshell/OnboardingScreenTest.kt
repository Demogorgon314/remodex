package com.emanueledipietro.remodex.feature.appshell

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.emanueledipietro.remodex.feature.onboarding.OnboardingScreen
import com.emanueledipietro.remodex.ui.theme.RemodexTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class OnboardingScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun onboardingAdvancesThroughIosStylePagesAndFinishesOnFinalCta() {
        var continueCalls = 0

        composeRule.setContent {
            RemodexTheme {
                OnboardingScreen(onContinue = { continueCalls += 1 })
            }
        }

        composeRule.onNodeWithText("Control Codex from your Android phone.").assertIsDisplayed()
        composeRule.onNodeWithText("Get Started").assertIsDisplayed().performClick()

        composeRule.onNodeWithText("What you get").assertIsDisplayed()
        composeRule.onNodeWithText("Set Up").assertIsDisplayed().performClick()

        composeRule.onNodeWithText("Install Codex CLI").assertIsDisplayed()
        composeRule.onNodeWithText("Continue").assertIsDisplayed().performClick()

        composeRule.onNodeWithText("Install the Bridge").assertIsDisplayed()
        composeRule.onNodeWithText("Continue").assertIsDisplayed().performClick()

        composeRule.onNodeWithText("Start Pairing").assertIsDisplayed()
        composeRule.onNodeWithText("Scan QR Code").assertIsDisplayed().performClick()

        composeRule.runOnIdle {
            assertEquals(1, continueCalls)
        }
    }
}
